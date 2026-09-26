package uk.ewancroft.inkwell.deeplink

import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for the unverified HTTPS hand-off entry point
 * (`https://inkwell.ewancroft.uk/open?uri=<AT-URI>`), distinct from both the trusted
 * `inkwell://document` custom scheme ([uk.ewancroft.inkwell.ContentDeepLinkPolicy], covered
 * by `OAuthCallbackPolicyTest`) and the OAuth callback. Covers percent-encoded Unicode,
 * extra path segments, and interleaved/extra query parameters per issue #97's acceptance
 * criteria — [HttpsDeepLinkResolver] (not tested here, since it hits the network) is
 * responsible for actually verifying whatever candidate this extracts before routing it
 * into the Reader.
 */
@RunWith(RobolectricTestRunner::class)
class HttpsDeepLinkPolicyTest {

    @Test
    fun `accepts open path with a document AT-URI`() {
        val uri = Uri.parse(
            "https://inkwell.ewancroft.uk/open?uri=at%3A%2F%2Fdid%3Aplc%3Aexample%2Fsite.standard.document%2Fabc"
        )
        assertEquals(
            "at://did:plc:example/site.standard.document/abc",
            HttpsDeepLinkPolicy.candidateDocumentUri(Intent(Intent.ACTION_VIEW, uri)),
        )
    }

    @Test
    fun `accepts a nested open path segment`() {
        val uri = Uri.parse(
            "https://inkwell.ewancroft.uk/open/document?uri=at%3A%2F%2Fdid%3Aplc%3Aexample%2Fsite.standard.document%2Fabc"
        )
        assertEquals(
            "at://did:plc:example/site.standard.document/abc",
            HttpsDeepLinkPolicy.candidateDocumentUri(uri),
        )
    }

    @Test
    fun `decodes percent-encoded unicode in the record key`() {
        // "日記" (diary), percent-encoded in the query value.
        val uri = Uri.parse(
            "https://inkwell.ewancroft.uk/open?uri=at%3A%2F%2Fdid%3Aplc%3Aexample%2Fsite.standard.document%2F%E6%97%A5%E8%A8%98"
        )
        assertEquals(
            "at://did:plc:example/site.standard.document/日記",
            HttpsDeepLinkPolicy.candidateDocumentUri(uri),
        )
    }

    @Test
    fun `ignores extra and interleaved query parameters`() {
        val uri = Uri.parse(
            "https://inkwell.ewancroft.uk/open" +
                "?url=https%3A%2F%2Fexample.com%2Farticle" +
                "&uri=at%3A%2F%2Fdid%3Aplc%3Aexample%2Fsite.standard.document%2Fabc" +
                "&ref=email"
        )
        assertEquals(
            "at://did:plc:example/site.standard.document/abc",
            HttpsDeepLinkPolicy.candidateDocumentUri(uri),
        )
    }

    @Test
    fun `mixed-case scheme and host still match`() {
        val uri = Uri.parse(
            "HTTPS://INKWELL.EWANCROFT.UK/open?uri=at%3A%2F%2Fdid%3Aplc%3Aexample%2Fsite.standard.document%2Fabc"
        )
        assertEquals(
            "at://did:plc:example/site.standard.document/abc",
            HttpsDeepLinkPolicy.candidateDocumentUri(uri),
        )
    }

    @Test
    fun `rejects a different host`() {
        val uri = Uri.parse(
            "https://evil.example.com/open?uri=at%3A%2F%2Fdid%3Aplc%3Aexample%2Fsite.standard.document%2Fabc"
        )
        assertNull(HttpsDeepLinkPolicy.candidateDocumentUri(uri))
    }

    @Test
    fun `rejects a non-open path`() {
        val uri = Uri.parse(
            "https://inkwell.ewancroft.uk/privacy?uri=at%3A%2F%2Fdid%3Aplc%3Aexample%2Fsite.standard.document%2Fabc"
        )
        assertNull(HttpsDeepLinkPolicy.candidateDocumentUri(uri))
    }

    @Test
    fun `rejects plain http scheme`() {
        val uri = Uri.parse(
            "http://inkwell.ewancroft.uk/open?uri=at%3A%2F%2Fdid%3Aplc%3Aexample%2Fsite.standard.document%2Fabc"
        )
        assertNull(HttpsDeepLinkPolicy.candidateDocumentUri(uri))
    }

    @Test
    fun `rejects a non-document collection`() {
        val uri = Uri.parse(
            "https://inkwell.ewancroft.uk/open?uri=at%3A%2F%2Fdid%3Aplc%3Aexample%2Fsite.standard.publication%2Fabc"
        )
        assertNull(HttpsDeepLinkPolicy.candidateDocumentUri(uri))
    }

    @Test
    fun `rejects a missing uri parameter`() {
        val uri = Uri.parse("https://inkwell.ewancroft.uk/open?url=https%3A%2F%2Fexample.com%2Farticle")
        assertNull(HttpsDeepLinkPolicy.candidateDocumentUri(uri))
    }

    @Test
    fun `rejects a malformed AT-URI`() {
        val uri = Uri.parse("https://inkwell.ewancroft.uk/open?uri=not-an-at-uri")
        assertNull(HttpsDeepLinkPolicy.candidateDocumentUri(uri))
    }

    @Test
    fun `returns null for an intent with no data`() {
        assertNull(HttpsDeepLinkPolicy.candidateDocumentUri(Intent(Intent.ACTION_MAIN)))
    }
}

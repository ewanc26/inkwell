package uk.ewancroft.inkwell.data.remote

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Test
import okhttp3.HttpUrl.Companion.toHttpUrl
import uk.ewancroft.inkwell.data.model.atproto.DocumentRecord
import uk.ewancroft.inkwell.data.model.atproto.PublicationRecord
import uk.ewancroft.inkwell.shared.verification.VerificationFailure
import uk.ewancroft.inkwell.shared.verification.VerificationResult
import uk.ewancroft.inkwell.shared.verification.VerificationUrls

/**
 * Hermetic unit tests for URL construction and verification outcomes. Network I/O is
 * supplied by a small fake transport so this suite is safe to run offline.
 */
class StandardSiteVerifierTest {

    @Test
    fun `verification target policy rejects local hosts`() {
        assertTrue(!VerificationTargetPolicy.isSafe("https://localhost/document".toHttpUrl()))
        assertTrue(!VerificationTargetPolicy.isSafe("https://127.0.0.1/document".toHttpUrl()))
        assertTrue(!VerificationTargetPolicy.isSafe("https://192.168.1.10/document".toHttpUrl()))
    }

    // ── publicationVerificationUrl ──────────────────────────────────────

    @Test
    fun `non-root publication verification endpoint appends publication path`() {
        val url = VerificationUrls.publicationVerificationUrl("https://example.com/writing/")
        assertEquals(
            "https://example.com/.well-known/site.standard.publication/writing",
            url,
        )
    }

    @Test
    fun `root publication verification endpoint has no trailing path`() {
        val url = VerificationUrls.publicationVerificationUrl("https://example.com")
        assertEquals(
            "https://example.com/.well-known/site.standard.publication",
            url,
        )
    }

    @Test
    fun `non-https publication url is rejected`() {
        assertNull(VerificationUrls.publicationVerificationUrl("http://example.com"))
    }

    @Test
    fun `unparseable publication url is rejected`() {
        assertNull(VerificationUrls.publicationVerificationUrl("not a url"))
    }

    // ── documentCanonicalUrl ────────────────────────────────────────────

    @Test
    fun `document canonical url uses publication url for at-uri site`() {
        val publication = PublicationRecord(url = "https://example.com/writing", name = "Example")
        val document = DocumentRecord(
            site = "at://did:plc:alice/site.standard.publication/3pub",
            title = "Hello",
            publishedAt = "2026-01-01T00:00:00Z",
            path = "/posts/hello",
        )

        val url = StandardSiteVerifier.documentCanonicalUrl(document, publication)
        assertEquals("https://example.com/writing/posts/hello", url?.toString())
    }

    @Test
    fun `document canonical url is null for at-uri site without a resolved publication`() {
        val document = DocumentRecord(
            site = "at://did:plc:alice/site.standard.publication/3pub",
            title = "Hello",
            publishedAt = "2026-01-01T00:00:00Z",
        )

        assertNull(StandardSiteVerifier.documentCanonicalUrl(document, publication = null))
    }

    @Test
    fun `document canonical url uses direct https site for standalone documents`() {
        val document = DocumentRecord(
            site = "https://example.com",
            title = "Hello",
            publishedAt = "2026-01-01T00:00:00Z",
            path = "hello",
        )

        val url = StandardSiteVerifier.documentCanonicalUrl(document, publication = null)
        assertEquals("https://example.com/hello", url?.toString())
    }

    // ── verifyPublication / verifyDocument: failure taxonomy (offline) ──

    @Test
    fun `verifyPublication fails with InvalidPublicationURL for an unparseable url`() = runBlocking {
        val publication = PublicationRecord(url = "not a url", name = "Example")
        val result = StandardSiteVerifier.verifyPublication(
            publicationURI = "at://did:plc:alice/site.standard.publication/3pub",
            publication = publication,
        )
        assertTrue(result is VerificationResult.Failed)
        assertTrue((result as VerificationResult.Failed).failure is VerificationFailure.InvalidPublicationURL)
    }

    @Test
    fun `verifyDocument fails with InvalidDocumentURL when publication is unresolved`() = runBlocking {
        val document = DocumentRecord(
            site = "at://did:plc:alice/site.standard.publication/3pub",
            title = "Hello",
            publishedAt = "2026-01-01T00:00:00Z",
        )
        val result = StandardSiteVerifier.verifyDocument(
            documentURI = "at://did:plc:alice/site.standard.document/3doc",
            document = document,
            publication = null,
        )
        assertTrue(result is VerificationResult.Failed)
        assertTrue((result as VerificationResult.Failed).failure is VerificationFailure.InvalidDocumentURL)
    }

    @Test
    fun `verifyPublication maps a non-success response`() = runBlocking {
        val publication = PublicationRecord(
            url = "https://example.com",
            name = "Example",
        )
        val verifier = SiteVerifier { VerificationHttpResponse(503, "busy") }
        val result = verifier.verifyPublication(
            publicationURI = "at://did:plc:alice/site.standard.publication/3pub",
            publication = publication,
        )
        assertTrue(result is VerificationResult.Failed)
        assertTrue((result as VerificationResult.Failed).failure is VerificationFailure.EndpointUnreachable)
    }

    @Test
    fun `verifyPublication accepts an exact endpoint response`() = runBlocking {
        val publicationURI = "at://did:plc:ofrbh253gwicbkc5nktqepol/site.standard.publication/3m3x4bgbsh22k"
        val publication = PublicationRecord(url = "https://example.com", name = "Example")
        val verifier = SiteVerifier { VerificationHttpResponse(200, publicationURI) }

        val result = verifier.verifyPublication(publicationURI, publication)
        assertEquals(VerificationResult.Verified, result)
    }

    @Test
    fun `verifyPublication reports a mismatched endpoint response`() = runBlocking {
        val publication = PublicationRecord(url = "https://example.com", name = "Example")
        val verifier = SiteVerifier { VerificationHttpResponse(200, "at://wrong") }

        val result = verifier.verifyPublication(
            publicationURI = "at://did:plc:someoneelse/site.standard.publication/notreal",
            publication = publication,
        )
        assertTrue(result is VerificationResult.Failed)
        assertTrue((result as VerificationResult.Failed).failure is VerificationFailure.MismatchedURI)
    }

    @Test
    fun `verifyDocument accepts a matching discovery link`() = runBlocking {
        val publication = PublicationRecord(url = "https://example.com", name = "Example")
        val documentURI = "at://did:plc:ofrbh253gwicbkc5nktqepol/site.standard.document/3msjlh4nqfc2l"
        val document = DocumentRecord(
            site = "at://did:plc:ofrbh253gwicbkc5nktqepol/site.standard.publication/3m3x4bgbsh22k",
            title = "The Whole Industry Is Doing This.",
            publishedAt = "2026-01-01T00:00:00Z",
            path = "/3msjlh4nqfc2l",
        )

        val html = "<link rel=\"site.standard.document\" href=\"$documentURI\">"
        val verifier = SiteVerifier { VerificationHttpResponse(200, html) }
        val result = verifier.verifyDocument(documentURI, document, publication)
        assertEquals(VerificationResult.Verified, result)
    }

    @Test
    fun `document cache does not reuse verification for changed record inputs`() = runBlocking {
        val documentURI = "at://did:plc:alice/site.standard.document/same"
        val first = DocumentRecord(
            site = "https://example.com",
            title = "First",
            publishedAt = "2026-01-01T00:00:00Z",
            path = "/first",
        )
        val second = first.copy(path = "/second")
        val calls = AtomicInteger(0)
        val verifier = SiteVerifier {
            calls.incrementAndGet()
            VerificationHttpResponse(200, "<link rel=\"site.standard.document\" href=\"$documentURI\">")
        }

        assertEquals(VerificationResult.Verified, verifier.verifyDocument(documentURI, first))
        assertEquals(VerificationResult.Verified, verifier.verifyDocument(documentURI, second))
        assertEquals(2, calls.get())
    }
}

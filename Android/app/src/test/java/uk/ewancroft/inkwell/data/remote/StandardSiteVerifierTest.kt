package uk.ewancroft.inkwell.data.remote

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.ewancroft.inkwell.data.model.atproto.DocumentRecord
import uk.ewancroft.inkwell.data.model.atproto.PublicationRecord
import uk.ewancroft.inkwell.shared.verification.VerificationUrls

/**
 * Unit and (network-dependent) integration tests for [StandardSiteVerifier].
 *
 * The `live*` tests hit `https://blog.ewancroft.uk`, a real standard.site publication
 * (Ewan Croft's blog — the app author's own site) confirmed by hand to serve a working
 * `.well-known/site.standard.publication` endpoint and per-document `<link
 * rel="site.standard.document">` tags. They require network access; everything else here
 * is pure logic and needs none.
 */
class StandardSiteVerifierTest {

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
    fun `verifyPublication fails with EndpointUnreachable for a domain that does not exist`() = runBlocking {
        val publication = PublicationRecord(
            url = "https://this-domain-should-not-resolve-inkwell-verify-test.invalid",
            name = "Example",
        )
        val result = StandardSiteVerifier.verifyPublication(
            publicationURI = "at://did:plc:alice/site.standard.publication/3pub",
            publication = publication,
        )
        assertTrue(result is VerificationResult.Failed)
        assertTrue((result as VerificationResult.Failed).failure is VerificationFailure.EndpointUnreachable)
    }

    // ── Live network checks against a real standard.site publication ───

    @Test
    fun `live verifyPublication succeeds for the real blog-ewancroft-uk publication`() = runBlocking {
        // Confirmed by hand: https://blog.ewancroft.uk/.well-known/site.standard.publication
        // returns exactly this AT-URI.
        val publicationURI = "at://did:plc:ofrbh253gwicbkc5nktqepol/site.standard.publication/3m3x4bgbsh22k"
        val publication = PublicationRecord(url = "https://blog.ewancroft.uk", name = "Ewan's Blog")

        val result = StandardSiteVerifier.verifyPublication(publicationURI, publication)
        assertEquals(VerificationResult.Verified, result)
    }

    @Test
    fun `live verifyPublication reports MismatchedURI for the wrong AT-URI`() = runBlocking {
        val publication = PublicationRecord(url = "https://blog.ewancroft.uk", name = "Ewan's Blog")

        val result = StandardSiteVerifier.verifyPublication(
            publicationURI = "at://did:plc:someoneelse/site.standard.publication/notreal",
            publication = publication,
        )
        assertTrue(result is VerificationResult.Failed)
        assertTrue((result as VerificationResult.Failed).failure is VerificationFailure.MismatchedURI)
    }

    @Test
    fun `live verifyDocument succeeds for a real published document`() = runBlocking {
        val publication = PublicationRecord(url = "https://blog.ewancroft.uk", name = "Ewan's Blog")
        val documentURI = "at://did:plc:ofrbh253gwicbkc5nktqepol/site.standard.document/3msjlh4nqfc2l"
        val document = DocumentRecord(
            site = "at://did:plc:ofrbh253gwicbkc5nktqepol/site.standard.publication/3m3x4bgbsh22k",
            title = "The Whole Industry Is Doing This.",
            publishedAt = "2026-01-01T00:00:00Z",
            path = "/3msjlh4nqfc2l",
        )

        val result = StandardSiteVerifier.verifyDocument(documentURI, document, publication)
        assertEquals(VerificationResult.Verified, result)
    }
}

package uk.ewancroft.inkwell.shared.verification

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VerificationUrlsTest {
    @Test
    fun publicationUrlUsesParsedPathAndPort() {
        assertEquals(
            "https://example.com:8443/.well-known/site.standard.publication/writing",
            VerificationUrls.publicationVerificationUrl("HTTPS://EXAMPLE.COM:8443/writing/?ignored=1#fragment"),
        )
    }

    @Test
    fun rejectsCredentialsAndNonHttpsUrls() {
        assertNull(VerificationUrls.publicationVerificationUrl("http://example.com/writing"))
        assertNull(VerificationUrls.publicationVerificationUrl("https://user:pass@example.com/writing"))
        assertNull(VerificationUrls.publicationVerificationUrl("https:///writing"))
    }

    @Test
    fun canonicalDocumentUrlPreservesParsedAuthority() {
        assertEquals(
            "https://example.com:8443/writing/post",
            VerificationUrls.documentCanonicalUrl(
                documentSite = "https://example.com:8443/writing?draft=true",
                documentPath = "/post",
                publicationUrl = null,
            ),
        )
    }
}

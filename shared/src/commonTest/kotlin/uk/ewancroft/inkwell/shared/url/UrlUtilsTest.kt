package uk.ewancroft.inkwell.shared.url

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UrlUtilsTest {

    @Test
    fun canonicalUrlForBareHostWithNoPathOrTrailingSlash() {
        // Regression: a site with no '/', '?', or '#' after the host (e.g.
        // "https://example.com") left hostEnd at -1, and
        // afterScheme.substring(0, -1) crashed with an uncaught
        // StringIndexOutOfBoundsException on Kotlin/Native (iOS), taken down
        // the whole app when opening a document whose publication had this
        // shape.
        assertEquals(
            "https://example.com",
            UrlUtils.canonicalUrl(site = "https://example.com", path = null),
        )
        assertEquals(
            "https://example.com/hello",
            UrlUtils.canonicalUrl(site = "https://example.com", path = "hello"),
        )
    }

    @Test
    fun canonicalUrlForBareHostWithTrailingSlash() {
        assertEquals(
            "https://example.com",
            UrlUtils.canonicalUrl(site = "https://example.com/", path = null),
        )
    }

    @Test
    fun canonicalUrlForHostWithPath() {
        assertEquals(
            "https://example.com/writing/posts/hello",
            UrlUtils.canonicalUrl(site = "https://example.com/writing", path = "/posts/hello"),
        )
    }

    @Test
    fun canonicalUrlRejectsNonHttps() {
        assertNull(UrlUtils.canonicalUrl(site = "http://example.com", path = null))
    }

    @Test
    fun canonicalUrlRequiresPublicationUrlForAtUriSite() {
        assertNull(
            UrlUtils.canonicalUrl(
                site = "at://did:plc:alice/site.standard.publication/3pub",
                path = "/hello",
                publicationUrl = null,
            )
        )
        assertEquals(
            "https://example.com/hello",
            UrlUtils.canonicalUrl(
                site = "at://did:plc:alice/site.standard.publication/3pub",
                path = "/hello",
                publicationUrl = "https://example.com",
            ),
        )
    }

    @Test
    fun normalizedSiteForBareHostWithNoTrailingSlash() {
        assertEquals("https://example.com", UrlUtils.normalizedSite("https://example.com"))
    }
}

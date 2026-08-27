package uk.ewancroft.inkwell.shared.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SearchResultClassifierTest {

    @Test
    fun `isPublication only matches the publication type`() {
        assertTrue(SearchResultClassifier.isPublication("publication"))
        assertFalse(SearchResultClassifier.isPublication("article"))
        assertFalse(SearchResultClassifier.isPublication("site.standard.document"))
    }

    @Test
    fun `isStandardSiteDocument inspects the collection`() {
        assertTrue(
            SearchResultClassifier.isStandardSiteDocument(
                "at://did:plc:abc/site.standard.document/hello"
            )
        )
        assertFalse(
            SearchResultClassifier.isStandardSiteDocument(
                "at://did:plc:abc/site.standard.publication/pub"
            )
        )
        assertFalse(
            SearchResultClassifier.isStandardSiteDocument("https://example.com/hello")
        )
    }

    @Test
    fun `webURL returns origin for publications`() {
        assertEquals(
            "https://example.com",
            SearchResultClassifier.webURL(
                basePath = "example.com",
                path = null,
                rkey = null,
                platform = null,
                isPublication = true,
            )
        )
    }

    @Test
    fun `webURL appends path for documents`() {
        assertEquals(
            "https://example.com/hello",
            SearchResultClassifier.webURL(
                basePath = "example.com",
                path = "/hello",
                rkey = "hello",
                platform = "leaflet",
                isPublication = false,
            )
        )
    }

    @Test
    fun `webURL falls back to rkey for leaflet documents without a path`() {
        assertEquals(
            "https://example.com/abc123",
            SearchResultClassifier.webURL(
                basePath = "example.com",
                path = null,
                rkey = "abc123",
                platform = "leaflet",
                isPublication = false,
            )
        )
    }

    @Test
    fun `webURL is null without a base path`() {
        assertNull(
            SearchResultClassifier.webURL(
                basePath = null,
                path = "/hello",
                rkey = null,
                platform = null,
                isPublication = false,
            )
        )
    }
}

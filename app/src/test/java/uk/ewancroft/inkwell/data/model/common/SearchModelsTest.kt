package uk.ewancroft.inkwell.data.model.common

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Decoding tests for the pub search (leaflet-search-backend.fly.dev) wire
 * format. The endpoint returns camelCase fields and a `null` `total` even
 * when `format=v2` is requested, so the models must tolerate both.
 */
class SearchModelsTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `v2 search response with null total decodes`() {
        val body = """
            {
              "results": [
                {
                  "type": "article",
                  "uri": "at://did:plc:abc123/site.standard.document/hello",
                  "did": "did:plc:abc123",
                  "title": "Hello world",
                  "snippet": "A snippet",
                  "createdAt": "2026-06-01T12:00:00Z",
                  "rkey": "hello",
                  "basePath": "example.com",
                  "platform": "leaflet",
                  "path": "/hello",
                  "coverImage": "https://example.com/cover.png",
                  "handle": "alice.example",
                  "source": "atproto",
                  "publicationName": "Example",
                  "url": "https://example.com/hello"
                }
              ],
              "total": null,
              "hasMore": true,
              "nextOffset": 40,
              "query": "hello",
              "mode": "keyword"
            }
        """.trimIndent()

        val response = json.decodeFromString<SearchResponse>(body)

        assertEquals(1, response.results.size)
        assertNull(response.total)
        assertTrue(response.hasMore)

        val result = response.results.first()
        assertEquals("article", result.type)
        assertEquals("at://did:plc:abc123/site.standard.document/hello", result.uri)
        assertEquals("Hello world", result.title)
        assertEquals("A snippet", result.snippet)
        assertEquals("2026-06-01T12:00:00Z", result.createdAt)
        assertEquals("example.com", result.basePath)
        assertEquals("https://example.com/cover.png", result.coverImage)
        assertEquals("alice.example", result.handle)
        assertTrue(result.isStandardSiteDocument)
        assertFalse(result.isPublication)
    }

    @Test
    fun `publication result is flagged`() {
        val body = """
            [{"type":"publication","uri":"at://did:plc:abc123/app.bsky.graph.list/pub",
              "did":"did:plc:abc123","title":"My Publication"}]
        """.trimIndent()

        val results = json.decodeFromString<List<SearchResult>>(body)

        assertTrue(results.first().isPublication)
        assertFalse(results.first().isStandardSiteDocument)
    }
}

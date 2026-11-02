package uk.ewancroft.inkwell.data.model.bluesky

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Verifies [BlueskyProfile] decodes the `links` carried by a
 * `app.bsky.actor.defs#profileViewDetailed` (`app.bsky.actor.getProfile`).
 * List subjects decode as `profileView`, which has no `links` — so the field
 * must also tolerate being absent entirely.
 */
class BlueskyProfileLinksTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val fixture = """
        {
          "did": "did:plc:example",
          "handle": "example.bsky.social",
          "displayName": "Example",
          "description": "Testing links",
          "pronouns": "they/them",
          "website": "https://example.com/old",
          "avatar": "https://cdn.bsky.app/img/avatar/plain/did:plc:example/bafkreiexample",
          "followersCount": 42,
          "links": [
            {
              "uri": "at://did:plc:example/app.bsky.actor.link/3mexample1",
              "cid": "bafyreexample1",
              "url": "https://example.com",
              "title": "Example site",
              "icon": "https://cdn.bsky.app/img/feed_thumbnail/plain/did:plc:example/bafkreiexample",
              "labels": []
            },
            {
              "uri": "at://did:plc:example/app.bsky.actor.link/3mexample2",
              "cid": "bafyreexample2",
              "url": "https://untitled.example"
            }
          ],
          "labels": [],
          "createdAt": "2023-04-26T05:22:14.855Z",
          "indexedAt": "2026-07-22T10:12:57.160Z"
        }
    """.trimIndent()

    @Test
    fun `decodes profile links in display order`() {
        val decoded = json.decodeFromString<BlueskyProfile>(fixture)

        val links = decoded.links
        assertEquals(2, links?.size)
        assertEquals("https://example.com", links?.get(0)?.url)
        assertEquals("Example site", links?.get(0)?.title)
        assertEquals("https://untitled.example", links?.get(1)?.url)
        assertNull(links?.get(1)?.title)
    }

    @Test
    fun `missing links decodes as null for profileView subjects`() {
        val decoded = json.decodeFromString<BlueskyProfile>(
            """{"did":"did:plc:example","handle":"example.bsky.social"}""",
        )

        assertNull(decoded.links)
    }
}

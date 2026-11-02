package uk.ewancroft.inkwell.data.model.bluesky

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies [BSkyEmbed] decodes on the wire `$type` — always a `#view`
 * variant, never a bare record ID. The old sealed hierarchy matched bare
 * IDs under the default `type` discriminator, so *every* post carrying an
 * embed failed wholesale and the Reader showed "post unavailable".
 *
 * Unknown present and future embed types must decode as [BSkyEmbed.Unknown]
 * without sinking the post.
 */
class BlueskyPostEmbedsTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun postWithEmbed(embedJson: String?) = """
        {
          "posts": [
            {
              "uri": "at://did:plc:example/app.bsky.feed.post/3mexample",
              "cid": "bafyreexample",
              "author": {"did": "did:plc:example", "handle": "example.bsky.social", "displayName": "Example"},
              "record": {"text": "hi", "createdAt": "2026-01-01T00:00:00.000Z"},
              "replyCount": 1, "repostCount": 2, "likeCount": 3${if (embedJson != null) ", \"embed\": $embedJson" else ""}
            }
          ]
        }
    """.trimIndent()

    private fun decodeEmbed(embedJson: String?): BSkyEmbed? =
        json.decodeFromString<GetPostsResponse>(postWithEmbed(embedJson)).posts.firstOrNull()?.embed

    @Test
    fun `decodes images view embed`() {
        val embed = decodeEmbed(
            """{"${'$'}type":"app.bsky.embed.images#view","images":[{"thumb":"https://example.com/t","fullsize":"https://example.com/f","alt":"A red kite"}]}""",
        ) as? BSkyEmbed.Images

        assertNotNull(embed)
        assertEquals("A red kite", embed?.images?.firstOrNull()?.alt)
    }

    @Test
    fun `decodes external view embed`() {
        val embed = decodeEmbed(
            """{"${'$'}type":"app.bsky.embed.external#view","external":{"uri":"https://example.com/story","title":"A story"}}""",
        ) as? BSkyEmbed.External

        assertNotNull(embed)
        assertEquals("A story", embed?.external?.title)
    }

    @Test
    fun `decodes quoted record view embed`() {
        val embed = decodeEmbed(
            """{"${'$'}type":"app.bsky.embed.record#view","record":{"${'$'}type":"app.bsky.embed.record#viewRecord","uri":"at://did:plc:author/app.bsky.feed.post/abc","author":{"did":"did:plc:author","handle":"author.example"},"value":{"text":"Quoted"}}}""",
        ) as? BSkyEmbed.Record

        assertNotNull(embed)
        assertEquals("author.example", embed?.record?.author?.handle)
    }

    @Test
    fun `tolerates a taken-down quoted record`() {
        val embed = decodeEmbed(
            """{"${'$'}type":"app.bsky.embed.record#view","record":{"${'$'}type":"app.bsky.embed.record#viewNotFound","uri":"at://did:plc:author/app.bsky.feed.post/abc"}}""",
        ) as? BSkyEmbed.Record

        assertNotNull(embed)
        assertNull(embed?.record?.author)
    }

    @Test
    fun `decodes video view embed`() {
        val embed = decodeEmbed(
            """{"${'$'}type":"app.bsky.embed.video#view","cid":"bafyrevideo","playlist":"https://example.com/list.m3u8","thumbnail":"https://example.com/t","alt":"A clip"}""",
        ) as? BSkyEmbed.Video

        assertNotNull(embed)
        assertEquals("https://example.com/list.m3u8", embed?.playlist)
    }

    @Test
    fun `decodes record with media embed`() {
        val embed = decodeEmbed(
            """{"${'$'}type":"app.bsky.embed.recordWithMedia#view","record":{"record":{"uri":"at://did:plc:author/app.bsky.feed.post/abc"}},"media":{"${'$'}type":"app.bsky.embed.images#view","images":[{"thumb":"https://example.com/t"}]}}""",
        ) as? BSkyEmbed.RecordWithMedia

        assertNotNull(embed)
        assertEquals("at://did:plc:author/app.bsky.feed.post/abc", embed?.record?.record?.uri)
        assertTrue(embed?.media is BSkyEmbedMedia.Images)
    }

    @Test
    fun `unknown future embed type decodes as Unknown without sinking the post`() {
        val decoded = json.decodeFromString<GetPostsResponse>(
            postWithEmbed("""{"${'$'}type":"app.bsky.embed.audio#view","audio":"https://example.com/a"}"""),
        )
        val post = decoded.posts.firstOrNull()

        assertNotNull(post)
        assertTrue(post?.embed is BSkyEmbed.Unknown)
    }

    @Test
    fun `post without embed decodes with null embed`() {
        val decoded = json.decodeFromString<GetPostsResponse>(postWithEmbed(null))

        assertNull(decoded.posts.firstOrNull()?.embed)
    }
}

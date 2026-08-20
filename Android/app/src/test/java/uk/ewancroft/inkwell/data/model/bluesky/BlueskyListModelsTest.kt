package uk.ewancroft.inkwell.data.model.bluesky

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Verifies [GetListResponse] decodes a real `app.bsky.graph.getList` response.
 *
 * Fixture captured from Inkwell's own supporters list
 * (at://did:plc:ofrbh253gwicbkc5nktqepol/app.bsky.graph.list/3mtjkyzm3nx27,
 * `limit=2`) — trimmed of fields [BlueskyListItem]/[BlueskyProfile] don't
 * declare, which `ignoreUnknownKeys` must tolerate since the real payload
 * carries far more (pronouns, associated, labels, ...) than the app needs.
 */
class BlueskyListModelsTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val fixture = """
        {
          "items": [
            {
              "uri": "at://did:plc:ofrbh253gwicbkc5nktqepol/app.bsky.graph.listitem/3mtjl3hfwhp27",
              "subject": {
                "did": "did:plc:jbeaa5kdaladzwq3r7f5xgwe",
                "handle": "danielroe.dev",
                "displayName": "danielroe",
                "pronouns": "he/him",
                "avatar": "https://cdn.bsky.app/img/avatar/plain/did:plc:jbeaa5kdaladzwq3r7f5xgwe/bafkreif4d7wtmzqppbpnwhjulf3d36ltbeg5wzu3i2mhq6wxb4f6nh5uo4",
                "associated": { "chat": { "allowIncoming": "all" } },
                "labels": [],
                "createdAt": "2023-04-26T05:22:14.855Z",
                "description": "building @nuxt.com",
                "indexedAt": "2026-07-22T10:12:57.160Z"
              }
            },
            {
              "uri": "at://did:plc:ofrbh253gwicbkc5nktqepol/app.bsky.graph.listitem/3mtjl2op6qx27",
              "subject": {
                "did": "did:plc:hu2jmpvtlecuwqnosnloplx6",
                "handle": "captaincalliope.at",
                "displayName": "Lyre Calliope 🧭✨",
                "avatar": "https://cdn.bsky.app/img/avatar/plain/did:plc:hu2jmpvtlecuwqnosnloplx6/bafkreibmf27a7ju4bdfamv4xxszl5w7kqgapnz237y53f2xgoolcwjrnwy",
                "labels": [],
                "createdAt": "2023-11-13T03:20:03.535Z",
                "indexedAt": "2026-05-25T19:43:38.264Z"
              }
            }
          ],
          "cursor": "3mtjl2op6qx27"
        }
    """.trimIndent()

    @Test
    fun `decodes real getList response, unknown keys and all`() {
        val decoded = json.decodeFromString<GetListResponse>(fixture)

        assertEquals(2, decoded.items.size)
        assertEquals("3mtjl2op6qx27", decoded.cursor)

        val first = decoded.items[0].subject
        assertEquals("danielroe.dev", first.handle)
        assertEquals("danielroe", first.displayName)
        assertNotNull(first.avatar)

        val second = decoded.items[1].subject
        assertEquals("captaincalliope.at", second.handle)
        assertEquals("Lyre Calliope 🧭✨", second.displayName)
    }

    @Test
    fun `missing cursor decodes as null, ending pagination`() {
        val decoded = json.decodeFromString<GetListResponse>("""{"items":[]}""")

        assertEquals(0, decoded.items.size)
        assertEquals(null, decoded.cursor)
    }
}

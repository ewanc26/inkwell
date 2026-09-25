package uk.ewancroft.inkwell.data.model.bluesky

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class BlueskyPostModelsTest {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun `decodes enriched Standard site external embed fields`() {
        val payload = """
        {
            "uri": "https://example.com/doc",
            "title": "Standard.site Post",
            "description": "Longform post",
            "readingTime": 5,
            "createdAt": "2026-09-21T00:00:00Z",
            "associatedRefs": [
                {
                    "uri": "at://did:plc:author/site.standard.document/123",
                    "cid": "bafyabc"
                }
            ]
        }
        """

        val external = json.decodeFromString<BSkyExternal>(payload)

        assertEquals("Standard.site Post", external.title)
        assertEquals(5, external.readingTime)
        assertEquals("2026-09-21T00:00:00Z", external.createdAt)
        assertNotNull(external.associatedRefs)
        assertEquals(1, external.associatedRefs?.size)
        assertEquals("at://did:plc:author/site.standard.document/123", external.associatedRefs?.first()?.uri)
        assertEquals("bafyabc", external.associatedRefs?.first()?.cid)
    }
}

package uk.ewancroft.inkwell.data.model.content

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The Leaflet lexicons store `aspectRatio` as `{ width, height }` on image,
 * gallery, iframe and HTML blocks. Decoding it as a string dropped the whole
 * document, so these pin both the canonical object and the legacy `16:9` form.
 */
class LeafletBlockAspectRatioTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun `decodes the canonical image aspect ratio object`() {
        val block = json.decodeFromString<LeafletBlock>(
            """
            {
              "${'$'}type": "pub.leaflet.blocks.image",
              "image": {
                "${'$'}type": "blob",
                "ref": {"${'$'}link": "bafyimage"},
                "mimeType": "image/jpeg",
                "size": 12
              },
              "alt": "A picture",
              "aspectRatio": {"width": 1200, "height": 800}
            }
            """.trimIndent(),
        )

        assertEquals(1200, block.aspectRatio?.width)
        assertEquals(800, block.aspectRatio?.height)
    }

    @Test
    fun `decodes a gallery with per-image aspect ratios`() {
        val block = json.decodeFromString<LeafletBlock>(
            """
            {
              "${'$'}type": "pub.leaflet.blocks.imageGallery",
              "format": "grid",
              "images": [
                {
                  "${'$'}type": "pub.leaflet.blocks.imageGallery#image",
                  "image": {
                    "${'$'}type": "blob",
                    "ref": {"${'$'}link": "bafyone"},
                    "mimeType": "image/jpeg",
                    "size": 1
                  },
                  "alt": "One",
                  "aspectRatio": {"width": 4, "height": 3}
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals("grid", block.format)
        assertEquals(1, block.images?.size)
        assertEquals(4, block.images?.first()?.aspectRatio?.width)
        assertEquals(3, block.images?.first()?.aspectRatio?.height)
    }

    @Test
    fun `decodes a legacy string aspect ratio`() {
        val block = json.decodeFromString<LeafletBlock>(
            """{"${'$'}type":"pub.leaflet.blocks.iframe","url":"https://example.com/embed","aspectRatio":"16:9"}""",
        )

        assertEquals(16, block.aspectRatio?.width)
        assertEquals(9, block.aspectRatio?.height)
    }
}

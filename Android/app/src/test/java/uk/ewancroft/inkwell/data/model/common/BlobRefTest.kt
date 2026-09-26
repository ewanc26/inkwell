package uk.ewancroft.inkwell.data.model.common

import kotlinx.serialization.json.Json
import uk.ewancroft.inkwell.data.model.content.LeafletContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * The writer publishes blob-backed content using the canonical AT Protocol blob
 * shape (`{"ref": {"$link": …}}`). These cover the reader decoding both that and
 * the legacy top-level `$link` form so neither silently drops a document body.
 */
class BlobRefTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun `decodes the canonical nested ref shape`() {
        val blob = json.decodeFromString<BlobRef>(
            """{"${'$'}type":"blob","ref":{"${'$'}link":"bafyreicanonical"},"mimeType":"application/json","size":42}""",
        )

        assertEquals("bafyreicanonical", blob.link)
        assertEquals("application/json", blob.mimeType)
        assertEquals(42, blob.size)
    }

    @Test
    fun `decodes the legacy top-level link shape`() {
        val blob = json.decodeFromString<BlobRef>(
            """{"${'$'}type":"blob","${'$'}link":"bafyreilegacy","size":7}""",
        )

        assertEquals("bafyreilegacy", blob.link)
        assertEquals(7, blob.size)
    }

    @Test
    fun `decodes blob-backed leaflet pages`() {
        val content = json.decodeFromString<LeafletContent>(
            """
            {
              "${'$'}type":"pub.leaflet.content",
              "blobPages":{"${'$'}type":"blob","ref":{"${'$'}link":"bafyreipages"},"mimeType":"application/json","size":1024}
            }
            """.trimIndent(),
        )

        assertNotNull(content.blobPages)
        assertEquals("bafyreipages", content.blobPages.link)
        assertEquals(1024, content.blobPages.size)
    }
}

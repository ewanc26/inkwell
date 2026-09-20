package uk.ewancroft.inkwell.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.ewancroft.inkwell.data.model.content.ByteSlice
import uk.ewancroft.inkwell.data.model.content.FacetFeature
import uk.ewancroft.inkwell.data.model.content.LeafletFacet
import uk.ewancroft.inkwell.shared.facets.FacetSchema

class LeafletFacetRenderingTest {
    @Test
    fun `preserves multiple unicode link facets and leaves surrounding text unannotated`() {
        val text = "Read café at https://one.example and 東京 at https://two.example."
        val firstStart = text.indexOf("https://one.example")
        val secondStart = text.indexOf("https://two.example")
        val facets = listOf(
            facet(text, firstStart, "https://one.example"),
            facet(text, secondStart, "https://two.example"),
        )

        val annotated = buildAnnotatedString(text, facets)
        val links = annotated.getStringAnnotations("URL", 0, annotated.length)

        assertEquals(text, annotated.text)
        assertEquals(2, links.size)
        assertEquals("https://one.example", links[0].item)
        assertEquals("https://two.example", links[1].item)
        assertEquals("https://one.example", annotated.text.substring(links[0].start, links[0].end))
        assertEquals("https://two.example", annotated.text.substring(links[1].start, links[1].end))
        assertTrue(links[0].start > 0)
        assertTrue(links[1].end < annotated.length)
    }

    private fun facet(text: String, start: Int, uri: String): LeafletFacet {
        val byteStart = text.substring(0, start).toByteArray(Charsets.UTF_8).size
        val byteEnd = byteStart + uri.toByteArray(Charsets.UTF_8).size
        return LeafletFacet(
            index = ByteSlice(byteStart, byteEnd),
            features = listOf(FacetFeature(FacetSchema.leaflet.link, uri = uri)),
        )
    }
}

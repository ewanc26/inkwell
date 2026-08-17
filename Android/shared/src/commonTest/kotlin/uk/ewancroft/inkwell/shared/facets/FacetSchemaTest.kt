package uk.ewancroft.inkwell.shared.facets

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FacetSchemaTest {

    @Test
    fun leafletSchemaHasExpectedTypes() {
        val schema = FacetSchema.leaflet
        assertEquals("pub.leaflet.richtext.facet", schema.facet)
        assertEquals("pub.leaflet.richtext.facet#bold", schema.bold)
        assertEquals("pub.leaflet.richtext.facet#italic", schema.italic)
        assertEquals("pub.leaflet.richtext.facet#code", schema.code)
        assertEquals("pub.leaflet.richtext.facet#strikethrough", schema.strike)
        assertEquals("pub.leaflet.richtext.facet#link", schema.link)
        assertEquals("pub.leaflet.richtext.facet#byteSlice", schema.byteSlice)
    }

    @Test
    fun pcktSchemaUsesBlogNamespace() {
        val schema = FacetSchema.pckt
        assertEquals("blog.pckt.richtext.facet", schema.facet)
        assertEquals("blog.pckt.richtext.facet#bold", schema.bold)
    }

    @Test
    fun offprintSchemaUsesAppNamespace() {
        val schema = FacetSchema.offprint
        assertEquals("app.offprint.richtext.facet", schema.facet)
        assertEquals("app.offprint.richtext.facet#bold", schema.bold)
    }

    @Test
    fun allSchemasShareCoreMarkTypes() {
        for (schema in listOf(FacetSchema.leaflet, FacetSchema.pckt, FacetSchema.offprint)) {
            assertTrue(schema.bold.endsWith("#bold"), "$schema.bold should end with #bold")
            assertTrue(schema.italic.endsWith("#italic"), "$schema.italic should end with #italic")
            assertTrue(schema.code.endsWith("#code"), "$schema.code should end with #code")
            assertTrue(schema.strike.endsWith("#strikethrough"), "$schema.strike should end with #strikethrough")
            assertTrue(schema.link.endsWith("#link"), "$schema.link should end with #link")
        }
    }

    @Test
    fun leafletLossyContainsExpectedLabels() {
        val lossy = FacetSchema.leaflet.lossy
        assertEquals("highlight", lossy["pub.leaflet.richtext.facet#highlight"])
        assertEquals("underline", lossy["pub.leaflet.richtext.facet#underline"])
        assertEquals("mentions", lossy["pub.leaflet.richtext.facet#atMention"])
        assertEquals("footnotes", lossy["pub.leaflet.richtext.facet#footnote"])
    }

    @Test
    fun pcktLossyContainsExpectedLabels() {
        val lossy = FacetSchema.pckt.lossy
        assertEquals("highlight", lossy["pub.leaflet.richtext.facet#highlight"])
        assertEquals("underline", lossy["pub.leaflet.richtext.facet#underline"])
        assertEquals("mentions", lossy["pub.leaflet.richtext.facet#atMention"])
        assertEquals("anchors", lossy["pub.leaflet.richtext.facet#id"])
    }

    @Test
    fun offprintLossyContainsExpectedLabels() {
        val lossy = FacetSchema.offprint.lossy
        assertEquals("mentions", lossy["app.offprint.richtext.facet#mention"])
        assertEquals("mentions", lossy["app.offprint.richtext.facet#webMention"])
    }

    @Test
    fun lossyMapsAreImmutable() {
        // Each schema's lossy map should be a distinct instance
        assertTrue(FacetSchema.leaflet.lossy !== FacetSchema.pckt.lossy)
        assertTrue(FacetSchema.pckt.lossy !== FacetSchema.offprint.lossy)
    }
}

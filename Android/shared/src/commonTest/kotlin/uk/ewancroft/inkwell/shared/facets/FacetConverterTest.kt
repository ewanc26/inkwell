package uk.ewancroft.inkwell.shared.facets

import kotlin.test.Test
import kotlin.test.assertEquals

class FacetConverterTest {

    private val leafletBold = FacetSchema.leaflet.bold
    private val leafletItalic = FacetSchema.leaflet.italic
    private val leafletCode = FacetSchema.leaflet.code
    private val leafletStrike = FacetSchema.leaflet.strike
    private val leafletLink = FacetSchema.leaflet.link
    private val leafletLossy = FacetSchema.leaflet.lossy

    // ── No facets ───────────────────────────────────────────────────────

    @Test
    fun plainTextWithNoFacets() {
        val result = facetsToMarkdown("Hello world", null, leafletBold, leafletItalic, leafletCode, leafletStrike, leafletLink, leafletLossy)
        assertEquals("Hello world", result)
    }

    @Test
    fun emptyFacetsList() {
        val result = facetsToMarkdown("Hello", emptyList(), leafletBold, leafletItalic, leafletCode, leafletStrike, leafletLink, leafletLossy)
        assertEquals("Hello", result)
    }

    // ── Bold ────────────────────────────────────────────────────────────

    @Test
    fun boldFacet() {
        val facets = listOf(
            RichTextFacet(byteStart = 0, byteEnd = 5, features = listOf(RichTextFeature(leafletBold)))
        )
        val result = facetsToMarkdown("Hello", facets, leafletBold, leafletItalic, leafletCode, leafletStrike, leafletLink, leafletLossy)
        assertEquals("**Hello**", result)
    }

    // ── Italic ──────────────────────────────────────────────────────────

    @Test
    fun italicFacet() {
        val facets = listOf(
            RichTextFacet(byteStart = 0, byteEnd = 5, features = listOf(RichTextFeature(leafletItalic)))
        )
        val result = facetsToMarkdown("Hello", facets, leafletBold, leafletItalic, leafletCode, leafletStrike, leafletLink, leafletLossy)
        assertEquals("*Hello*", result)
    }

    // ── Code ────────────────────────────────────────────────────────────

    @Test
    fun codeFacet() {
        val facets = listOf(
            RichTextFacet(byteStart = 0, byteEnd = 5, features = listOf(RichTextFeature(leafletCode)))
        )
        val result = facetsToMarkdown("Hello", facets, leafletBold, leafletItalic, leafletCode, leafletStrike, leafletLink, leafletLossy)
        assertEquals("`Hello`", result)
    }

    // ── Strikethrough ───────────────────────────────────────────────────

    @Test
    fun strikeFacet() {
        val facets = listOf(
            RichTextFacet(byteStart = 0, byteEnd = 5, features = listOf(RichTextFeature(leafletStrike)))
        )
        val result = facetsToMarkdown("Hello", facets, leafletBold, leafletItalic, leafletCode, leafletStrike, leafletLink, leafletLossy)
        assertEquals("~~Hello~~", result)
    }

    // ── Link ────────────────────────────────────────────────────────────

    @Test
    fun linkFacet() {
        val facets = listOf(
            RichTextFacet(byteStart = 0, byteEnd = 5, features = listOf(RichTextFeature(leafletLink, uri = "https://example.com")))
        )
        val result = facetsToMarkdown("Hello", facets, leafletBold, leafletItalic, leafletCode, leafletStrike, leafletLink, leafletLossy)
        assertEquals("[Hello](https://example.com)", result)
    }

    // ── Combined marks ──────────────────────────────────────────────────

    @Test
    fun boldAndItalicCombined() {
        val facets = listOf(
            RichTextFacet(byteStart = 0, byteEnd = 5, features = listOf(
                RichTextFeature(leafletBold),
                RichTextFeature(leafletItalic),
            ))
        )
        val result = facetsToMarkdown("Hello", facets, leafletBold, leafletItalic, leafletCode, leafletStrike, leafletLink, leafletLossy)
        assertEquals("***Hello***", result)
    }

    @Test
    fun boldAndItalicInSameSentence() {
        val facets = listOf(
            RichTextFacet(byteStart = 0, byteEnd = 3, features = listOf(RichTextFeature(leafletBold))),
            RichTextFacet(byteStart = 7, byteEnd = 10, features = listOf(RichTextFeature(leafletItalic))),
        )
        val result = facetsToMarkdown("abc defghi", facets, leafletBold, leafletItalic, leafletCode, leafletStrike, leafletLink, leafletLossy)
        assertEquals("**abc** def*ghi*", result)
    }

    // ── Segment merging ─────────────────────────────────────────────────

    @Test
    fun adjacentSegmentsWithSameMarksMerge() {
        val facets = listOf(
            RichTextFacet(byteStart = 0, byteEnd = 3, features = listOf(RichTextFeature(leafletBold))),
            RichTextFacet(byteStart = 3, byteEnd = 6, features = listOf(RichTextFeature(leafletBold))),
        )
        val result = facetsToMarkdown("abcdef", facets, leafletBold, leafletItalic, leafletCode, leafletStrike, leafletLink, leafletLossy)
        assertEquals("**abcdef**", result)
    }

    // ── Code takes precedence over other marks ───────────────────────────

    @Test
    fun codeFacetWrapsOnlyBackticks() {
        val facets = listOf(
            RichTextFacet(byteStart = 0, byteEnd = 5, features = listOf(
                RichTextFeature(leafletCode),
                RichTextFeature(leafletBold),
            ))
        )
        val result = facetsToMarkdown("Hello", facets, leafletBold, leafletItalic, leafletCode, leafletStrike, leafletLink, leafletLossy)
        assertEquals("`Hello`", result)
    }

    // ── UTF-8 multibyte characters ──────────────────────────────────────

    @Test
    fun utf8MultibyteCharacters() {
        val text = "Hello 🎉 World"
        val facets = listOf(
            RichTextFacet(byteStart = 6, byteEnd = 10, features = listOf(RichTextFeature(leafletBold)))
        )
        val result = facetsToMarkdown(text, facets, leafletBold, leafletItalic, leafletCode, leafletStrike, leafletLink, leafletLossy)
        assertEquals("Hello **🎉** World", result)
    }

    // ── Lossy features ──────────────────────────────────────────────────

    @Test
    fun lossyFeatureTracked() {
        val lost = mutableSetOf<String>()
        val facets = listOf(
            RichTextFacet(byteStart = 0, byteEnd = 5, features = listOf(
                RichTextFeature("pub.leaflet.richtext.facet#highlight")
            ))
        )
        facetsToMarkdown("Hello", facets, leafletBold, leafletItalic, leafletCode, leafletStrike, leafletLink, leafletLossy, lost)
        assertEquals(setOf("highlight"), lost)
    }
}

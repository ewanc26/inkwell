package uk.ewancroft.inkwell.shared.facets

/**
 * Maps each content format's facet `$type` strings to the markdown marks
 * Inkwell supports, plus human-readable labels for features that can't be
 * represented in markdown.
 *
 * Mirrors iOS `FacetSchema` in ContentProvider.swift and the hard-coded
 * NSID strings duplicated across Android's MarkdownConverter.kt,
 * PcktOffprintConverter.kt, and LeafletBlockRenderer.kt.
 */
object FacetSchema {

    val leaflet = FacetDefinition(
        facet = "pub.leaflet.richtext.facet",
        byteSlice = "pub.leaflet.richtext.facet#byteSlice",
        bold = "pub.leaflet.richtext.facet#bold",
        italic = "pub.leaflet.richtext.facet#italic",
        code = "pub.leaflet.richtext.facet#code",
        strike = "pub.leaflet.richtext.facet#strikethrough",
        link = "pub.leaflet.richtext.facet#link",
        lossy = mapOf(
            "pub.leaflet.richtext.facet#highlight" to "highlight",
            "pub.leaflet.richtext.facet#underline" to "underline",
            "pub.leaflet.richtext.facet#atMention" to "mentions",
            "pub.leaflet.richtext.facet#didMention" to "mentions",
            "pub.leaflet.richtext.facet#footnote" to "footnotes",
        )
    )

    val pckt = FacetDefinition(
        facet = "blog.pckt.richtext.facet",
        byteSlice = "blog.pckt.richtext.facet#byteSlice",
        bold = "blog.pckt.richtext.facet#bold",
        italic = "blog.pckt.richtext.facet#italic",
        code = "blog.pckt.richtext.facet#code",
        strike = "blog.pckt.richtext.facet#strikethrough",
        link = "blog.pckt.richtext.facet#link",
        lossy = mapOf(
            "pub.leaflet.richtext.facet#highlight" to "highlight",
            "pub.leaflet.richtext.facet#underline" to "underline",
            "pub.leaflet.richtext.facet#atMention" to "mentions",
            "pub.leaflet.richtext.facet#didMention" to "mentions",
            "pub.leaflet.richtext.facet#id" to "anchors",
        )
    )

    val offprint = FacetDefinition(
        facet = "app.offprint.richtext.facet",
        byteSlice = "app.offprint.richtext.facet#byteSlice",
        bold = "app.offprint.richtext.facet#bold",
        italic = "app.offprint.richtext.facet#italic",
        code = "app.offprint.richtext.facet#code",
        strike = "app.offprint.richtext.facet#strikethrough",
        link = "app.offprint.richtext.facet#link",
        lossy = mapOf(
            "app.offprint.richtext.facet#highlight" to "highlight",
            "app.offprint.richtext.facet#underline" to "underline",
            "app.offprint.richtext.facet#mention" to "mentions",
            "app.offprint.richtext.facet#webMention" to "mentions",
        )
    )
}

data class FacetDefinition(
    val facet: String,
    val byteSlice: String,
    val bold: String,
    val italic: String,
    val code: String,
    val strike: String,
    val link: String,
    val lossy: Map<String, String>,
)

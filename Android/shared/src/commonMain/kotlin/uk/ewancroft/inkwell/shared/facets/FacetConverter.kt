package uk.ewancroft.inkwell.shared.facets

/**
 * Pure byte-range → markdown conversion algorithm, shared between
 * Android and iOS.
 *
 * The algorithm is identical on both platforms:
 * 1. Convert plaintext to UTF-8 bytes
 * 2. Collect all byte boundaries from facet byteStart/byteEnd
 * 3. For each segment between boundaries, determine active marks
 * 4. Merge adjacent segments with identical marks
 * 5. Wrap each segment with markdown syntax
 *
 * Platform-specific JSON parsing stays in the app modules; this
 * class operates on the minimal [RichTextFacet] representation.
 */

/**
 * Minimal facet representation for the shared converter.
 *
 * Mirrors the relevant fields from Android's `LeafletFacet` and
 * iOS's `LeafletFacet` — just the byte-range index and feature list
 * needed for markdown conversion.
 */
data class RichTextFacet(
    val byteStart: Int,
    val byteEnd: Int,
    val features: List<RichTextFeature>,
)

data class RichTextFeature(
    val type: String,
    val uri: String? = null,
)

/**
 * Converts rich-text facets (plaintext + byte-range features) to
 * markdown inline syntax.
 *
 * @param plaintext The plain text content
 * @param facets List of byte-range facets, or null for plain text
 * @param boldType The facet $type string for bold (e.g. "pub.leaflet.richtext.facet#bold")
 * @param italicType The facet $type string for italic
 * @param codeType The facet $type string for code
 * @param strikeType The facet $type string for strikethrough
 * @param linkType The facet $type string for links
 * @param lost Set to collect labels for unsupported feature types
 * @return The markdown-formatted string
 */
fun facetsToMarkdown(
    plaintext: String,
    facets: List<RichTextFacet>?,
    boldType: String,
    italicType: String,
    codeType: String,
    strikeType: String,
    linkType: String,
    lossy: Map<String, String>,
    lost: MutableSet<String>? = null,
): String {
    if (facets.isNullOrEmpty()) return plaintext

    val utf8Bytes = plaintext.toByteArray(Charsets.UTF_8)
    val totalBytes = utf8Bytes.size

    val boundaries = mutableSetOf(0, totalBytes)
    for (facet in facets) {
        boundaries.add(facet.byteStart)
        boundaries.add(facet.byteEnd)
    }
    val sortedBounds = boundaries.sorted()

    data class Segment(
        val text: String,
        val bold: Boolean,
        val italic: Boolean,
        val code: Boolean,
        val strike: Boolean,
        val link: String?,
    )

    val segments = mutableListOf<Segment>()
    for (idx in 0 until sortedBounds.size - 1) {
        val start = sortedBounds[idx]
        val end = sortedBounds[idx + 1]
        if (start >= end || start >= totalBytes) continue
        val clampedEnd = minOf(end, totalBytes)

        val text = String(utf8Bytes, start, clampedEnd - start, Charsets.UTF_8)
        if (text.isEmpty()) continue

        var bold = false
        var italic = false
        var code = false
        var strike = false
        var link: String? = null

        for (facet in facets) {
            if (start >= facet.byteStart && start < facet.byteEnd) {
                for (feature in facet.features) {
                    when (feature.type) {
                        boldType -> bold = true
                        italicType -> italic = true
                        codeType -> code = true
                        strikeType -> strike = true
                        linkType -> link = feature.uri
                        else -> {
                            val label = lossy[feature.type]
                            if (label != null && lost != null) {
                                lost.add(label)
                            }
                        }
                    }
                }
            }
        }

        val seg = Segment(text, bold, italic, code, strike, link)
        val last = segments.lastOrNull()
        if (last != null &&
            last.bold == seg.bold &&
            last.italic == seg.italic &&
            last.code == seg.code &&
            last.strike == seg.strike &&
            last.link == seg.link
        ) {
            segments[segments.size - 1] = last.copy(text = last.text + seg.text)
        } else {
            segments.add(seg)
        }
    }

    return segments.joinToString("") { seg ->
        var wrapped = seg.text
        if (seg.code) {
            wrapped = "`$wrapped`"
        } else {
            if (seg.strike) wrapped = "~~$wrapped~~"
            if (seg.italic) wrapped = "*$wrapped*"
            if (seg.bold) wrapped = "**$wrapped**"
        }
        if (seg.link != null) {
            wrapped = "[$wrapped](${seg.link})"
        }
        wrapped
    }
}

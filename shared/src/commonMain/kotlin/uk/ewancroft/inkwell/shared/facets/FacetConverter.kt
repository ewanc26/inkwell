package uk.ewancroft.inkwell.shared.facets

data class RichTextFacet(
    val byteStart: Int,
    val byteEnd: Int,
    val features: List<RichTextFeature>,
)

data class RichTextFeature(
    val type: String,
    val uri: String? = null,
)

object FacetConverter {

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

        val utf8Bytes = plaintext.encodeToByteArray()
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

            val text = utf8Bytes.sliceArray(start until clampedEnd).decodeToString()
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
}

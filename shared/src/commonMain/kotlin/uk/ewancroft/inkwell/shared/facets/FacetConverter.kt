package uk.ewancroft.inkwell.shared.facets

import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

@OptIn(ExperimentalObjCName::class)
@ObjCName("RichTextFacet", exact = true)
data class RichTextFacet(
    val byteStart: Int,
    val byteEnd: Int,
    val features: List<RichTextFeature>,
)

@OptIn(ExperimentalObjCName::class)
@ObjCName("RichTextFeature", exact = true)
data class RichTextFeature(
    val type: String,
    val uri: String? = null,
)

@OptIn(ExperimentalObjCName::class)
@ObjCName("FacetConverter", exact = true)
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

    fun markdownToFacets(
        markdown: String,
        boldType: String,
        italicType: String,
        codeType: String,
        strikeType: String,
        linkType: String,
    ): Pair<String, List<RichTextFacet>> {
        val plaintext = StringBuilder()
        val facets = mutableListOf<RichTextFacet>()
        val chars = markdown.toCharArray()
        var i = 0

        val markStack = mutableListOf<Triple<Int, String, String?>>()

        while (i < chars.size) {
            // Bold: **text**
            if (i + 1 < chars.size && chars[i] == '*' && chars[i + 1] == '*') {
                val top = markStack.lastOrNull()
                if (top != null && top.second == boldType) {
                    val byteEnd = plaintext.toString().encodeToByteArray().size
                    if (byteEnd > top.first) {
                        facets.add(RichTextFacet(top.first, byteEnd, listOf(RichTextFeature(top.second))))
                    }
                    markStack.removeLast()
                    i += 2
                } else {
                    markStack.add(Triple(plaintext.toString().encodeToByteArray().size, boldType, null))
                    i += 2
                }
                continue
            }

            // Italic: *text*
            if (chars[i] == '*') {
                val top = markStack.lastOrNull()
                if (top != null && top.second == italicType) {
                    val byteEnd = plaintext.toString().encodeToByteArray().size
                    if (byteEnd > top.first) {
                        facets.add(RichTextFacet(top.first, byteEnd, listOf(RichTextFeature(top.second))))
                    }
                    markStack.removeLast()
                    i += 1
                } else {
                    markStack.add(Triple(plaintext.toString().encodeToByteArray().size, italicType, null))
                    i += 1
                }
                continue
            }

            // Strikethrough: ~~text~~
            if (i + 1 < chars.size && chars[i] == '~' && chars[i + 1] == '~') {
                val top = markStack.lastOrNull()
                if (top != null && top.second == strikeType) {
                    val byteEnd = plaintext.toString().encodeToByteArray().size
                    if (byteEnd > top.first) {
                        facets.add(RichTextFacet(top.first, byteEnd, listOf(RichTextFeature(top.second))))
                    }
                    markStack.removeLast()
                    i += 2
                } else {
                    markStack.add(Triple(plaintext.toString().encodeToByteArray().size, strikeType, null))
                    i += 2
                }
                continue
            }

            // Code: `text`
            if (chars[i] == '`') {
                val closeIdx = chars.drop(i + 1).indexOfFirst { it == '`' }.let { if (it >= 0) i + 1 + it else -1 }
                if (closeIdx >= 0) {
                    val content = chars.sliceArray(i + 1 until closeIdx).concatToString()
                    val byteStart = plaintext.toString().encodeToByteArray().size
                    plaintext.append(content)
                    val byteEnd = plaintext.toString().encodeToByteArray().size
                    facets.add(RichTextFacet(byteStart, byteEnd, listOf(RichTextFeature(codeType))))
                    i = closeIdx + 1
                    continue
                }
            }

            // Link: [text](url)
            if (chars[i] == '[') {
                val closeBracket = chars.drop(i + 1).indexOfFirst { it == ']' }.let { if (it >= 0) i + 1 + it else -1 }
                if (closeBracket >= 0 && closeBracket + 1 < chars.size && chars[closeBracket + 1] == '(') {
                    val openParen = closeBracket + 1
                    val closeParen = chars.drop(openParen + 1).indexOfFirst { it == ')' }.let { if (it >= 0) openParen + 1 + it else -1 }
                    if (closeParen >= 0) {
                        val text = chars.sliceArray(i + 1 until closeBracket).concatToString()
                        val url = chars.sliceArray(openParen + 1 until closeParen).concatToString()
                        val byteStart = plaintext.toString().encodeToByteArray().size
                        plaintext.append(text)
                        val byteEnd = plaintext.toString().encodeToByteArray().size
                        facets.add(RichTextFacet(byteStart, byteEnd, listOf(RichTextFeature(linkType, url))))
                        i = closeParen + 1
                        continue
                    }
                }
            }

            plaintext.append(chars[i])
            i += 1
        }

        return plaintext.toString() to facets
    }
}

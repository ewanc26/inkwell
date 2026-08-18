package uk.ewancroft.inkwell.shared.markdown

/**
 * Platform-agnostic inline markdown scanner.
 *
 * Parses inline markdown syntax (**bold**, *italic*, `code`, ~~strike~~,
 * [text](url)) into a list of [InlineSegment] items. Each platform maps
 * these to its own attributed-string type (SwiftUI AttributedString,
 * Compose AnnotatedString).
 *
 * The scanner strips markdown delimiters from the output text — segments
 * contain the visible text only. Byte offsets are not tracked here; the
 * caller already has the plaintext and can compute offsets if needed.
 */
object InlineMarkdownScanner {

    /**
     * Parses [text] for inline markdown and returns the list of segments.
     * Unmatched delimiters are treated as literal text.
     */
    fun scan(text: String): List<InlineSegment> {
        val chars = text.toCharArray()
        val segments = mutableListOf<InlineSegment>()
        val plain = StringBuilder()
        var i = 0

        fun flushPlain() {
            if (plain.isNotEmpty()) {
                segments.add(InlineSegment.Plain(plain.toString()))
                plain.clear()
            }
        }

        while (i < chars.size) {
            // Bold: **text**
            if (i + 1 < chars.size && chars[i] == '*' && chars[i + 1] == '*') {
                val end = findClosing(chars, i + 2, "**")
                if (end > 0) {
                    flushPlain()
                    segments.add(InlineSegment.Bold(text.substring(i + 2, end)))
                    i = end + 2
                    continue
                }
            }

            // Italic: *text*
            if (chars[i] == '*' && (i + 1 >= chars.size || chars[i + 1] != '*')) {
                val end = findClosing(chars, i + 1, "*")
                if (end > 0 && (end == 0 || chars[end - 1] != '*')) {
                    flushPlain()
                    segments.add(InlineSegment.Italic(text.substring(i + 1, end)))
                    i = end + 1
                    continue
                }
            }

            // Strikethrough: ~~text~~
            if (i + 1 < chars.size && chars[i] == '~' && chars[i + 1] == '~') {
                val end = findClosing(chars, i + 2, "~~")
                if (end > 0) {
                    flushPlain()
                    segments.add(InlineSegment.Strike(text.substring(i + 2, end)))
                    i = end + 2
                    continue
                }
            }

            // Inline code: `text`
            if (chars[i] == '`') {
                val end = findClosing(chars, i + 1, "`")
                if (end > 0) {
                    flushPlain()
                    segments.add(InlineSegment.Code(text.substring(i + 1, end)))
                    i = end + 1
                    continue
                }
            }

            // Link: [text](url)
            if (chars[i] == '[') {
                val closeBracket = text.indexOf(']', i + 1)
                if (closeBracket > 0 && closeBracket + 1 < chars.size && chars[closeBracket + 1] == '(') {
                    val closeParen = text.indexOf(')', closeBracket + 2)
                    if (closeParen > 0) {
                        val linkText = text.substring(i + 1, closeBracket)
                        val url = text.substring(closeBracket + 2, closeParen)
                        flushPlain()
                        segments.add(InlineSegment.Link(text = linkText, url = url))
                        i = closeParen + 1
                        continue
                    }
                }
            }

            plain.append(chars[i])
            i++
        }

        flushPlain()
        return segments
    }

    private fun findClosing(text: CharArray, start: Int, delimiter: String): Int {
        val delimChars = delimiter.toCharArray()
        var i = start
        while (i <= text.size - delimChars.size) {
            if (text[i] == delimChars[0] && (delimChars.size == 1 || text[i + 1] == delimChars[1])) {
                return i
            }
            i++
        }
        return -1
    }
}

/**
 * A single segment of inline-formatted text.
 */
sealed class InlineSegment {
    /** Plain text with no formatting. */
    data class Plain(val text: String) : InlineSegment()

    /** Bold text (was wrapped in `**`). */
    data class Bold(val text: String) : InlineSegment()

    /** Italic text (was wrapped in `*`). */
    data class Italic(val text: String) : InlineSegment()

    /** Inline code (was wrapped in `` ` ``). */
    data class Code(val text: String) : InlineSegment()

    /** Strikethrough text (was wrapped in `~~`). */
    data class Strike(val text: String) : InlineSegment()

    /** A hyperlink. [text] is the visible label, [url] is the destination. */
    data class Link(val text: String, val url: String) : InlineSegment()
}

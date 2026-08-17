package uk.ewancroft.inkwell.shared.markdown

/**
 * Line-by-line markdown parser handling the block types common to all
 * standard.site providers. Not a full CommonMark parser, but sufficient
 * for the reader's rendering and the writer's round-trip needs.
 *
 * Mirrors iOS `MarkdownParser` in ContentProvider.swift and Android
 * `MarkdownParser` in MarkdownParser.kt — near-identical logic.
 */
object MarkdownParser {

    fun parse(markdown: String): List<MarkdownBlock> {
        val blocks = mutableListOf<MarkdownBlock>()
        val lines = markdown.lines()
        var i = 0

        while (i < lines.size) {
            val trimmed = lines[i].trim()

            if (trimmed.isEmpty()) {
                i++
                continue
            }

            // Code block / math block
            if (trimmed.startsWith("```")) {
                val lang = trimmed.removePrefix("```")
                val codeLines = mutableListOf<String>()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) {
                    codeLines.add(lines[i])
                    i++
                }
                i++ // skip closing ```
                val content = codeLines.joinToString("\n")
                if (lang == "math") {
                    blocks.add(MarkdownBlock.Math(tex = content))
                } else {
                    blocks.add(MarkdownBlock.Code(language = lang.ifEmpty { null }, content = content))
                }
                continue
            }

            // Heading
            if (trimmed.startsWith("#")) {
                val level = trimmed.takeWhile { it == '#' }.length
                if (level in 1..6) {
                    val text = trimmed.drop(level).trim()
                    blocks.add(MarkdownBlock.Heading(level = level, text = text))
                    i++
                    continue
                }
            }

            // Horizontal rule
            if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
                blocks.add(MarkdownBlock.HorizontalRule)
                i++
                continue
            }

            // Blockquote
            if (trimmed.startsWith(">")) {
                val quoteLines = mutableListOf<String>()
                while (i < lines.size) {
                    val l = lines[i].trim()
                    if (l.startsWith(">")) {
                        quoteLines.add(l.removePrefix(">").trim())
                        i++
                    } else if (l.isEmpty()) {
                        i++
                        break
                    } else {
                        break
                    }
                }
                blocks.add(MarkdownBlock.Blockquote(text = quoteLines.joinToString("\n")))
                continue
            }

            // Image (on its own line)
            if (trimmed.startsWith("![")) {
                val closeBracket = trimmed.indexOf(']')
                if (closeBracket > 0 && closeBracket + 1 < trimmed.length && trimmed[closeBracket + 1] == '(') {
                    val closeParen = trimmed.indexOf(')', closeBracket + 2)
                    if (closeParen > 0) {
                        val alt = trimmed.substring(2, closeBracket)
                        val url = trimmed.substring(closeBracket + 2, closeParen)
                        blocks.add(MarkdownBlock.Image(alt = alt, url = url))
                        i++
                        continue
                    }
                }
            }

            // Unordered list / Task list
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                val (items, nextI) = parseList(lines, i, ordered = false)
                if (items.isNotEmpty() && items.all { it.checked != null }) {
                    blocks.add(MarkdownBlock.TaskList(items = items))
                } else {
                    blocks.add(MarkdownBlock.UnorderedList(items = items))
                }
                i = nextI
                continue
            }

            // Ordered list: "1. Item" or "1) Item"
            var numberStart = 0
            var numberStr = ""
            while (numberStart < trimmed.length && trimmed[numberStart].isDigit()) {
                numberStr += trimmed[numberStart]
                numberStart++
            }
            if (numberStr.isNotEmpty() && numberStart < trimmed.length &&
                (trimmed[numberStart] == '.' || trimmed[numberStart] == ')') &&
                numberStart + 1 < trimmed.length && trimmed[numberStart + 1].isWhitespace()
            ) {
                val number = numberStr.toIntOrNull()
                if (number != null) {
                    val (items, nextI) = parseList(lines, i, ordered = true)
                    blocks.add(MarkdownBlock.OrderedList(start = number, items = items))
                    i = nextI
                    continue
                }
            }

            // Paragraph (collect consecutive non-special lines)
            val paraLines = mutableListOf<String>()
            while (i < lines.size) {
                val l = lines[i].trim()
                if (l.isEmpty() || l.startsWith("#") || l.startsWith(">") || l.startsWith("```") ||
                    l.startsWith("- ") || l.startsWith("* ") || l == "---" || l == "***" || l == "___" ||
                    l.startsWith("![")
                ) {
                    break
                }
                paraLines.add(l)
                i++
            }
            if (paraLines.isNotEmpty()) {
                blocks.add(MarkdownBlock.Paragraph(text = paraLines.joinToString(" ")))
            }
        }

        return blocks
    }

    private fun parseList(
        lines: List<String>,
        start: Int,
        ordered: Boolean
    ): Pair<List<MarkdownListItem>, Int> {
        val items = mutableListOf<MarkdownListItem>()
        var i = start
        val baseIndent = lines[start].takeWhile { it == ' ' }.length

        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            if (trimmed.isEmpty()) {
                i++
                continue
            }

            val indent = line.takeWhile { it == ' ' }.length
            if (indent < baseIndent) break

            val isUnordered = trimmed.startsWith("- ") || trimmed.startsWith("* ")
            val isOrdered = ordered && trimmed.matches(Regex("^\\d+[.)]\\s.*"))

            if (!isUnordered && !isOrdered) break

            val itemText = if (isUnordered) {
                trimmed.drop(2)
            } else {
                val markerEnd = trimmed.indexOf(' ')
                if (markerEnd > 0) trimmed.substring(markerEnd + 1) else ""
            }

            var checked: Boolean? = null
            var cleanText = itemText
            when {
                cleanText.startsWith("[x] ") || cleanText.startsWith("[X] ") -> {
                    checked = true
                    cleanText = cleanText.drop(4)
                }
                cleanText.startsWith("[ ] ") -> {
                    checked = false
                    cleanText = cleanText.drop(4)
                }
            }

            var children: List<MarkdownListItem> = emptyList()
            if (i + 1 < lines.size) {
                val nextIndent = lines[i + 1].takeWhile { it == ' ' }.length
                if (nextIndent > baseIndent) {
                    val (nested, nextI) = parseList(lines, i + 1, ordered = false)
                    children = nested
                    i = nextI - 1
                }
            }

            items.add(MarkdownListItem(text = cleanText, checked = checked, children = children.ifEmpty { null }))
            i++
        }

        return items to i
    }
}

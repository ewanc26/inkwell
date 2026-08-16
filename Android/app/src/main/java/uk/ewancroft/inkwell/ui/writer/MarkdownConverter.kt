package uk.ewancroft.inkwell.ui.writer

import kotlinx.serialization.json.*
import uk.ewancroft.inkwell.data.model.content.*

object MarkdownConverter {

    fun convert(markdown: String, format: String): JsonObject {
        return when (format) {
            "Leaflet" -> buildLeafletContent(markdown)
            "Markpub" -> buildMarkpubContent(markdown)
            "pckt" -> buildPcktContent(markdown)
            "Offprint" -> buildOffprintContent(markdown)
            else -> buildLeafletContent(markdown)
        }
    }

    private fun buildLeafletContent(markdown: String): JsonObject = buildJsonObject {
        put("\$type", "pub.leaflet.content")
        val blocks = parseMarkdownToLeafletBlocks(markdown)
        put("pages", buildJsonArray {
            add(buildJsonObject {
                put("\$type", "pub.leaflet.pages.linearDocument")
                put("id", "page-1")
                put("blocks", buildJsonArray {
                    blocks.forEach { add(it) }
                })
            })
        })
    }

    private fun buildMarkpubContent(markdown: String): JsonObject = buildJsonObject {
        put("\$type", "at.markpub.markdown")
        put("text", buildJsonObject {
            put("\$type", "at.markpub.text")
            put("markdown", markdown)
        })
    }

    private fun buildPcktContent(markdown: String): JsonObject = buildJsonObject {
        put("\$type", "blog.pckt.content")
        val blocks = parseMarkdownToPcktBlocks(markdown)
        put("items", buildJsonArray {
            blocks.forEach { add(it) }
        })
    }

    private fun buildOffprintContent(markdown: String): JsonObject = buildJsonObject {
        put("\$type", "app.offprint.content")
        val blocks = parseMarkdownToOffprintBlocks(markdown)
        put("items", buildJsonArray {
            blocks.forEach { add(it) }
        })
    }

    // MARK: - Markdown Parsing

    private fun parseMarkdownToLeafletBlocks(markdown: String): List<JsonObject> {
        val lines = markdown.lines()
        val blocks = mutableListOf<JsonObject>()
        var i = 0

        while (i < lines.size) {
            val line = lines[i]
            when {
                line.startsWith("# ") -> {
                    val text = line.drop(2).trim()
                    val (plaintext, facets) = facetsFromMarkdown(text)
                    blocks.add(buildJsonObject {
                        put("\$type", "pub.leaflet.blocks.header")
                        put("plaintext", plaintext)
                        put("level", 1)
                        if (facets.isNotEmpty()) put("facets", buildJsonArray { facets.forEach { add(it) } })
                    })
                    i++
                }
                line.startsWith("## ") -> {
                    val text = line.drop(3).trim()
                    val (plaintext, facets) = facetsFromMarkdown(text)
                    blocks.add(buildJsonObject {
                        put("\$type", "pub.leaflet.blocks.header")
                        put("plaintext", plaintext)
                        put("level", 2)
                        if (facets.isNotEmpty()) put("facets", buildJsonArray { facets.forEach { add(it) } })
                    })
                    i++
                }
                line.startsWith("### ") -> {
                    val text = line.drop(4).trim()
                    val (plaintext, facets) = facetsFromMarkdown(text)
                    blocks.add(buildJsonObject {
                        put("\$type", "pub.leaflet.blocks.header")
                        put("plaintext", plaintext)
                        put("level", 3)
                        if (facets.isNotEmpty()) put("facets", buildJsonArray { facets.forEach { add(it) } })
                    })
                    i++
                }
                line.startsWith("```") -> {
                    val lang = line.drop(3).trim()
                    val codeLines = mutableListOf<String>()
                    i++
                    while (i < lines.size && !lines[i].startsWith("```")) {
                        codeLines.add(lines[i])
                        i++
                    }
                    i++ // skip closing ```
                    blocks.add(buildJsonObject {
                        put("\$type", "pub.leaflet.blocks.code")
                        put("plaintext", codeLines.joinToString("\n"))
                        if (lang.isNotBlank()) put("language", lang)
                    })
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    val (items, nextI) = parseList(lines, i, ordered = false)
                    blocks.add(buildJsonObject {
                        put("\$type", "pub.leaflet.blocks.unorderedList")
                        put("children", buildJsonArray {
                            items.forEach { item ->
                                add(buildJsonObject {
                                    put("\$type", "pub.leaflet.blocks.unorderedList#listItem")
                                    val (plaintext, facets) = facetsFromMarkdown(item.text)
                                    put("content", buildJsonObject {
                                        put("\$type", "pub.leaflet.blocks.text")
                                        put("plaintext", plaintext)
                                        if (facets.isNotEmpty()) put("facets", buildJsonArray { facets.forEach { add(it) } })
                                    })
                                    if (item.checked != null) put("checked", item.checked)
                                })
                            }
                        })
                    })
                    i = nextI
                }
                line.startsWith("> ") -> {
                    val quoteLines = mutableListOf<String>()
                    while (i < lines.size && (lines[i].startsWith("> ") || lines[i].isEmpty())) {
                        if (lines[i].startsWith("> ")) {
                            quoteLines.add(lines[i].drop(2).trim())
                        }
                        i++
                    }
                    blocks.add(buildJsonObject {
                        put("\$type", "pub.leaflet.blocks.blockquote")
                        put("plaintext", quoteLines.joinToString("\n"))
                    })
                }
                line == "---" || line == "***" -> {
                    blocks.add(buildJsonObject { put("\$type", "pub.leaflet.blocks.horizontalRule") })
                    i++
                }
                line.isEmpty() -> i++
                else -> {
                    val paraLines = mutableListOf<String>()
                    while (i < lines.size && lines[i].isNotBlank() &&
                        !lines[i].startsWith("#") && !lines[i].startsWith(">") &&
                        !lines[i].startsWith("```") && !lines[i].startsWith("- ") &&
                        !lines[i].startsWith("* ") && lines[i] != "---" && lines[i] != "***"
                    ) {
                        paraLines.add(lines[i])
                        i++
                    }
                    val text = paraLines.joinToString(" ")
                    val (plaintext, facets) = facetsFromMarkdown(text)
                    blocks.add(buildJsonObject {
                        put("\$type", "pub.leaflet.blocks.text")
                        put("plaintext", plaintext)
                        if (facets.isNotEmpty()) put("facets", buildJsonArray { facets.forEach { add(it) } })
                    })
                }
            }
        }
        return blocks
    }

    private fun parseMarkdownToPcktBlocks(markdown: String): List<JsonObject> {
        val lines = markdown.lines()
        val blocks = mutableListOf<JsonObject>()
        var i = 0

        while (i < lines.size) {
            val line = lines[i]
            when {
                line.startsWith("# ") -> {
                    val text = line.drop(2).trim()
                    val (plaintext, facets) = facetsFromMarkdown(text)
                    blocks.add(buildJsonObject {
                        put("\$type", "blog.pckt.block.heading")
                        put("plaintext", plaintext)
                        put("level", 1)
                        if (facets.isNotEmpty()) put("facets", buildJsonArray { facets.forEach { add(it) } })
                    })
                    i++
                }
                line.startsWith("## ") -> {
                    val text = line.drop(3).trim()
                    val (plaintext, facets) = facetsFromMarkdown(text)
                    blocks.add(buildJsonObject {
                        put("\$type", "blog.pckt.block.heading")
                        put("plaintext", plaintext)
                        put("level", 2)
                        if (facets.isNotEmpty()) put("facets", buildJsonArray { facets.forEach { add(it) } })
                    })
                    i++
                }
                line.startsWith("### ") -> {
                    val text = line.drop(4).trim()
                    val (plaintext, facets) = facetsFromMarkdown(text)
                    blocks.add(buildJsonObject {
                        put("\$type", "blog.pckt.block.heading")
                        put("plaintext", plaintext)
                        put("level", 3)
                        if (facets.isNotEmpty()) put("facets", buildJsonArray { facets.forEach { add(it) } })
                    })
                    i++
                }
                line.startsWith("```") -> {
                    val lang = line.drop(3).trim()
                    val codeLines = mutableListOf<String>()
                    i++
                    while (i < lines.size && !lines[i].startsWith("```")) {
                        codeLines.add(lines[i])
                        i++
                    }
                    i++
                    blocks.add(buildJsonObject {
                        put("\$type", "blog.pckt.block.codeBlock")
                        put("plaintext", codeLines.joinToString("\n"))
                        if (lang.isNotBlank()) put("language", lang)
                    })
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    val (items, nextI) = parseList(lines, i, ordered = false)
                    blocks.add(buildJsonObject {
                        put("\$type", "blog.pckt.block.bulletList")
                        put("listContent", buildJsonArray {
                            items.forEach { item ->
                                add(buildJsonObject {
                                    put("\$type", "blog.pckt.block.listItem")
                                    val (plaintext, facets) = facetsFromMarkdown(item.text)
                                    put("content", buildJsonArray {
                                        add(buildJsonObject {
                                            put("\$type", "blog.pckt.block.text")
                                            put("plaintext", plaintext)
                                            if (facets.isNotEmpty()) put("facets", buildJsonArray { facets.forEach { add(it) } })
                                        })
                                    })
                                    if (item.checked != null) put("checked", item.checked)
                                })
                            }
                        })
                    })
                    i = nextI
                }
                line.startsWith("> ") -> {
                    val quoteLines = mutableListOf<String>()
                    while (i < lines.size && (lines[i].startsWith("> ") || lines[i].isEmpty())) {
                        if (lines[i].startsWith("> ")) quoteLines.add(lines[i].drop(2).trim())
                        i++
                    }
                    blocks.add(buildJsonObject {
                        put("\$type", "blog.pckt.block.blockquote")
                        put("content", buildJsonArray {
                            add(buildJsonObject {
                                put("\$type", "blog.pckt.block.text")
                                put("plaintext", quoteLines.joinToString("\n"))
                            })
                        })
                    })
                }
                line == "---" || line == "***" -> {
                    blocks.add(buildJsonObject { put("\$type", "blog.pckt.block.horizontalRule") })
                    i++
                }
                line.isEmpty() -> i++
                else -> {
                    val paraLines = mutableListOf<String>()
                    while (i < lines.size && lines[i].isNotBlank() &&
                        !lines[i].startsWith("#") && !lines[i].startsWith(">") &&
                        !lines[i].startsWith("```") && !lines[i].startsWith("- ") &&
                        !lines[i].startsWith("* ") && lines[i] != "---" && lines[i] != "***"
                    ) {
                        paraLines.add(lines[i])
                        i++
                    }
                    val text = paraLines.joinToString(" ")
                    val (plaintext, facets) = facetsFromMarkdown(text)
                    blocks.add(buildJsonObject {
                        put("\$type", "blog.pckt.block.text")
                        put("plaintext", plaintext)
                        if (facets.isNotEmpty()) put("facets", buildJsonArray { facets.forEach { add(it) } })
                    })
                }
            }
        }
        return blocks
    }

    private fun parseMarkdownToOffprintBlocks(markdown: String): List<JsonObject> {
        val lines = markdown.lines()
        val blocks = mutableListOf<JsonObject>()
        var i = 0

        while (i < lines.size) {
            val line = lines[i]
            when {
                line.startsWith("# ") -> {
                    val text = line.drop(2).trim()
                    val (plaintext, facets) = facetsFromMarkdown(text)
                    blocks.add(buildJsonObject {
                        put("\$type", "app.offprint.block.heading")
                        put("plaintext", plaintext)
                        put("level", 1)
                        if (facets.isNotEmpty()) put("facets", buildJsonArray { facets.forEach { add(it) } })
                    })
                    i++
                }
                line.startsWith("## ") -> {
                    val text = line.drop(3).trim()
                    val (plaintext, facets) = facetsFromMarkdown(text)
                    blocks.add(buildJsonObject {
                        put("\$type", "app.offprint.block.heading")
                        put("plaintext", plaintext)
                        put("level", 2)
                        if (facets.isNotEmpty()) put("facets", buildJsonArray { facets.forEach { add(it) } })
                    })
                    i++
                }
                line.startsWith("### ") -> {
                    val text = line.drop(4).trim()
                    val (plaintext, facets) = facetsFromMarkdown(text)
                    blocks.add(buildJsonObject {
                        put("\$type", "app.offprint.block.heading")
                        put("plaintext", plaintext)
                        put("level", 3)
                        if (facets.isNotEmpty()) put("facets", buildJsonArray { facets.forEach { add(it) } })
                    })
                    i++
                }
                line.startsWith("```") -> {
                    val lang = line.drop(3).trim()
                    val codeLines = mutableListOf<String>()
                    i++
                    while (i < lines.size && !lines[i].startsWith("```")) {
                        codeLines.add(lines[i])
                        i++
                    }
                    i++
                    val blockType = if (lang == "math") "app.offprint.block.mathBlock" else "app.offprint.block.codeBlock"
                    blocks.add(buildJsonObject {
                        put("\$type", blockType)
                        put("plaintext", codeLines.joinToString("\n"))
                        if (lang.isNotBlank() && lang != "math") put("language", lang)
                    })
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    val (items, nextI) = parseList(lines, i, ordered = false)
                    blocks.add(buildJsonObject {
                        put("\$type", "app.offprint.block.bulletList")
                        put("children", buildJsonArray {
                            items.forEach { item ->
                                add(buildJsonObject {
                                    put("\$type", "app.offprint.block.text")
                                    val (plaintext, facets) = facetsFromMarkdown(item.text)
                                    put("plaintext", plaintext)
                                    if (facets.isNotEmpty()) put("facets", buildJsonArray { facets.forEach { add(it) } })
                                    if (item.checked != null) put("checked", item.checked)
                                })
                            }
                        })
                    })
                    i = nextI
                }
                line.startsWith("> ") -> {
                    val quoteLines = mutableListOf<String>()
                    while (i < lines.size && (lines[i].startsWith("> ") || lines[i].isEmpty())) {
                        if (lines[i].startsWith("> ")) quoteLines.add(lines[i].drop(2).trim())
                        i++
                    }
                    blocks.add(buildJsonObject {
                        put("\$type", "app.offprint.block.blockquote")
                        put("content", buildJsonArray {
                            add(buildJsonObject {
                                put("\$type", "app.offprint.block.text")
                                put("plaintext", quoteLines.joinToString("\n"))
                            })
                        })
                    })
                }
                line == "---" || line == "***" -> {
                    blocks.add(buildJsonObject { put("\$type", "app.offprint.block.horizontalRule") })
                    i++
                }
                line.isEmpty() -> i++
                else -> {
                    val paraLines = mutableListOf<String>()
                    while (i < lines.size && lines[i].isNotBlank() &&
                        !lines[i].startsWith("#") && !lines[i].startsWith(">") &&
                        !lines[i].startsWith("```") && !lines[i].startsWith("- ") &&
                        !lines[i].startsWith("* ") && lines[i] != "---" && lines[i] != "***"
                    ) {
                        paraLines.add(lines[i])
                        i++
                    }
                    val text = paraLines.joinToString(" ")
                    val (plaintext, facets) = facetsFromMarkdown(text)
                    blocks.add(buildJsonObject {
                        put("\$type", "app.offprint.block.text")
                        put("plaintext", plaintext)
                        if (facets.isNotEmpty()) put("facets", buildJsonArray { facets.forEach { add(it) } })
                    })
                }
            }
        }
        return blocks
    }

    private data class ListItem(val text: String, val checked: Boolean? = null)

    private fun parseList(lines: List<String>, start: Int, ordered: Boolean): Pair<List<ListItem>, Int> {
        val items = mutableListOf<ListItem>()
        val baseIndent = lines[start].indexOfFirst { it != ' ' }.coerceAtLeast(0)
        var i = start

        while (i < lines.size) {
            val line = lines[i]
            if (line.isEmpty()) { i++; continue }
            val indent = line.indexOfFirst { it != ' ' }.coerceAtLeast(0)
            if (indent < baseIndent) break

            val trimmed = line.trim()
            val isUnordered = trimmed.startsWith("- ") || trimmed.startsWith("* ")
            val isOrdered = ordered && trimmed.matches(Regex("^\\d+[\\)\\.]\\s.*"))

            if (!isUnordered && !isOrdered) break

            var itemText = when {
                isUnordered -> trimmed.drop(2)
                else -> {
                    val endOfMarker = trimmed.indexOfFirst { !it.isDigit() && it != '.' && it != ')' }
                    trimmed.drop(endOfMarker).trimStart().dropWhile { it == ' ' || it == '.' || it == ')' }
                }
            }

            var checked: Boolean? = null
            if (itemText.startsWith("[x] ") || itemText.startsWith("[X] ")) {
                checked = true
                itemText = itemText.drop(4)
            } else if (itemText.startsWith("[ ] ")) {
                checked = false
                itemText = itemText.drop(4)
            }

            items.add(ListItem(text = itemText, checked = checked))
            i++
        }
        return items to i
    }

    private fun facetsFromMarkdown(text: String): Pair<String, List<JsonObject>> {
        val boldRegex = Regex("\\*\\*(.+?)\\*\\*")
        val italicRegex = Regex("\\*(.+?)\\*")
        val codeRegex = Regex("`(.+?)`")
        val linkRegex = Regex("\\[(.+?)\\]\\((.+?)\\)")

        // Every format span is resolved against the original text with its
        // marker-strip ranges. Later matches skip any span already claimed so
        // nested/overlapping formatting can't double-strip or emit conflicting
        // facets (e.g. the italic pass inside `**bold**`).
        data class Span(
            val start: Int,
            val innerStart: Int,
            val innerEnd: Int,
            val end: Int,
            val type: String,
            val url: String? = null,
        )

        val spans = mutableListOf<Span>()

        fun claim(span: Span) {
            if (spans.any { span.innerStart < it.end && it.innerStart < span.end }) return
            spans.add(span)
        }

        for (match in boldRegex.findAll(text)) {
            val innerStart = match.range.first + 2
            claim(Span(match.range.first, innerStart, innerStart + match.groupValues[1].length, match.range.last + 1, "bold"))
        }
        for (match in italicRegex.findAll(text)) {
            if (match.value.startsWith("**")) continue
            val innerStart = match.range.first + 1
            claim(Span(match.range.first, innerStart, innerStart + match.groupValues[1].length, match.range.last + 1, "italic"))
        }
        for (match in codeRegex.findAll(text)) {
            val innerStart = match.range.first + 1
            claim(Span(match.range.first, innerStart, innerStart + match.groupValues[1].length, match.range.last + 1, "code"))
        }
        for (match in linkRegex.findAll(text)) {
            val innerStart = match.range.first + 1
            claim(Span(match.range.first, innerStart, innerStart + match.groupValues[1].length, match.range.last + 1, "link", match.groupValues[2]))
        }

        // Mark every character that belongs to a formatting marker.
        val removed = BooleanArray(text.length)
        for (span in spans) {
            for (i in span.start until span.innerStart) removed[i] = true
            for (i in span.innerEnd until span.end) removed[i] = true
        }

        // removedBefore[i] = markers stripped in text[0..i), mapping original
        // character indices to their position in the stripped plaintext.
        val removedBefore = IntArray(text.length + 1)
        for (i in text.indices) {
            removedBefore[i + 1] = removedBefore[i] + (if (removed[i]) 1 else 0)
        }

        val plaintext = buildString {
            for (i in text.indices) if (!removed[i]) append(text[i])
        }

        // Cumulative UTF-8 byte offset of each character index in the
        // plaintext. Facet byte ranges are byte offsets, not character
        // indices, and must index the *plaintext* — not the original text
        // (markers were stripped from it).
        val bytePrefix = IntArray(plaintext.length + 1)
        var byteCount = 0
        var ci = 0
        while (ci < plaintext.length) {
            val cp = plaintext.codePointAt(ci)
            val charCount = Character.charCount(cp)
            byteCount += when {
                cp < 0x80 -> 1
                cp < 0x800 -> 2
                cp < 0x10000 -> 3
                else -> 4
            }
            ci += charCount
            bytePrefix[ci] = byteCount
        }
        for (i in 1 until bytePrefix.size) {
            if (bytePrefix[i] == 0) bytePrefix[i] = bytePrefix[i - 1]
        }

        val facets = spans.map { span ->
            val pStart = span.innerStart - removedBefore[span.innerStart]
            val pEnd = span.innerEnd - removedBefore[span.innerEnd]
            buildJsonObject {
                put("\$type", "pub.leaflet.richtext.facet")
                put("index", buildJsonObject {
                    put("byteStart", bytePrefix[pStart])
                    put("byteEnd", bytePrefix[pEnd])
                })
                put("features", buildJsonArray {
                    add(buildJsonObject {
                        when (span.type) {
                            "bold" -> put("\$type", "pub.leaflet.richtext.facet#bold")
                            "italic" -> put("\$type", "pub.leaflet.richtext.facet#italic")
                            "code" -> put("\$type", "pub.leaflet.richtext.facet#code")
                            else -> {
                                put("\$type", "pub.leaflet.richtext.facet#link")
                                put("uri", span.url)
                            }
                        }
                    })
                })
            }
        }

        return plaintext to facets
    }
}

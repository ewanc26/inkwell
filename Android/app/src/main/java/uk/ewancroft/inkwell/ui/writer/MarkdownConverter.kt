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
        val facets = mutableListOf<JsonObject>()
        var plaintext = text
        var byteStart = 0

        val boldRegex = Regex("\\*\\*(.+?)\\*\\*")
        val italicRegex = Regex("\\*(.+?)\\*")
        val codeRegex = Regex("`(.+?)`")
        val linkRegex = Regex("\\[(.+?)\\]\\((.+?)\\)")

        val replacements = mutableListOf<Triple<Int, Int, String>>()

        for (match in boldRegex.findAll(text)) {
            val start = match.range.first
            val end = match.range.last + 1
            val inner = match.groupValues[1]
            val utf8Start = text.substring(0, start).toByteArray(Charsets.UTF_8).size
            val utf8End = text.substring(0, end).toByteArray(Charsets.UTF_8).size
            facets.add(buildJsonObject {
                put("\$type", "pub.leaflet.richtext.facet")
                put("index", buildJsonObject {
                    put("byteStart", utf8Start)
                    put("byteEnd", utf8End)
                })
                put("features", buildJsonArray {
                    add(buildJsonObject { put("\$type", "pub.leaflet.richtext.facet#bold") })
                })
            })
            replacements.add(Triple(start, end, inner))
        }

        for (match in italicRegex.findAll(text)) {
            if (match.value.startsWith("**")) continue
            val start = match.range.first
            val end = match.range.last + 1
            val inner = match.groupValues[1]
            val utf8Start = text.substring(0, start).toByteArray(Charsets.UTF_8).size
            val utf8End = text.substring(0, end).toByteArray(Charsets.UTF_8).size
            facets.add(buildJsonObject {
                put("\$type", "pub.leaflet.richtext.facet")
                put("index", buildJsonObject {
                    put("byteStart", utf8Start)
                    put("byteEnd", utf8End)
                })
                put("features", buildJsonArray {
                    add(buildJsonObject { put("\$type", "pub.leaflet.richtext.facet#italic") })
                })
            })
            replacements.add(Triple(start, end, inner))
        }

        for (match in codeRegex.findAll(text)) {
            val start = match.range.first
            val end = match.range.last + 1
            val inner = match.groupValues[1]
            val utf8Start = text.substring(0, start).toByteArray(Charsets.UTF_8).size
            val utf8End = text.substring(0, end).toByteArray(Charsets.UTF_8).size
            facets.add(buildJsonObject {
                put("\$type", "pub.leaflet.richtext.facet")
                put("index", buildJsonObject {
                    put("byteStart", utf8Start)
                    put("byteEnd", utf8End)
                })
                put("features", buildJsonArray {
                    add(buildJsonObject { put("\$type", "pub.leaflet.richtext.facet#code") })
                })
            })
            replacements.add(Triple(start, end, inner))
        }

        for (match in linkRegex.findAll(text)) {
            val start = match.range.first
            val end = match.range.last + 1
            val inner = match.groupValues[1]
            val url = match.groupValues[2]
            val utf8Start = text.substring(0, start).toByteArray(Charsets.UTF_8).size
            val utf8End = text.substring(0, end).toByteArray(Charsets.UTF_8).size
            facets.add(buildJsonObject {
                put("\$type", "pub.leaflet.richtext.facet")
                put("index", buildJsonObject {
                    put("byteStart", utf8Start)
                    put("byteEnd", utf8End)
                })
                put("features", buildJsonArray {
                    add(buildJsonObject {
                        put("\$type", "pub.leaflet.richtext.facet#link")
                        put("uri", url)
                    })
                })
            })
            replacements.add(Triple(start, end, inner))
        }

        var result = text
        replacements.sortByDescending { it.first }
        for ((start, end, replacement) in replacements) {
            result = result.substring(0, start) + replacement + result.substring(end)
        }

        return result to facets
    }
}

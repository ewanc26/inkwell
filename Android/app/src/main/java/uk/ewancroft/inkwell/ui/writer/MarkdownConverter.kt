package uk.ewancroft.inkwell.ui.writer

import kotlinx.serialization.json.*
import uk.ewancroft.inkwell.data.model.content.*
import uk.ewancroft.inkwell.shared.facets.FacetSchema
import uk.ewancroft.inkwell.shared.markdown.MarkdownBlock as SharedMarkdownBlock
import uk.ewancroft.inkwell.shared.markdown.MarkdownParser as SharedMarkdownParser
import uk.ewancroft.inkwell.shared.markdown.MarkdownListItem as SharedMarkdownListItem

object MarkdownConverter {

    fun convert(markdown: String, format: String, uploadedBlobs: Map<String, JsonObject> = emptyMap()): JsonObject {
        val blocks = SharedMarkdownParser.parse(markdown)
        return when (format) {
            "Leaflet" -> buildLeafletContent(blocks, uploadedBlobs)
            "Markpub" -> buildMarkpubContent(markdown)
            "pckt" -> buildPcktContent(blocks, uploadedBlobs)
            "Offprint" -> buildOffprintContent(blocks, uploadedBlobs)
            else -> buildLeafletContent(blocks, uploadedBlobs)
        }
    }

    private fun buildLeafletContent(blocks: List<SharedMarkdownBlock>, uploadedBlobs: Map<String, JsonObject>): JsonObject = buildJsonObject {
        put("\$type", "pub.leaflet.content")
        put("pages", buildJsonArray {
            add(buildJsonObject {
                put("\$type", "pub.leaflet.pages.linearDocument")
                put("id", "page-1")
                put("blocks", buildJsonArray {
                    blocks.forEach { block ->
                        leafletBlockToJson(block, uploadedBlobs)?.let { add(it) }
                    }
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

    private fun buildPcktContent(blocks: List<SharedMarkdownBlock>, uploadedBlobs: Map<String, JsonObject>): JsonObject = buildJsonObject {
        put("\$type", "blog.pckt.content")
        put("items", buildJsonArray {
            blocks.forEach { block ->
                pcktBlockToJson(block, uploadedBlobs)?.let { add(it) }
            }
        })
    }

    private fun buildOffprintContent(blocks: List<SharedMarkdownBlock>, uploadedBlobs: Map<String, JsonObject>): JsonObject = buildJsonObject {
        put("\$type", "app.offprint.content")
        put("items", buildJsonArray {
            blocks.forEach { block ->
                offprintBlockToJson(block, uploadedBlobs)?.let { add(it) }
            }
        })
    }

    // MARK: - Shared Block → Format JSON

    private fun leafletBlockToJson(block: SharedMarkdownBlock, uploadedBlobs: Map<String, JsonObject>): JsonObject? = when (block) {
        is SharedMarkdownBlock.Heading -> {
            val (plaintext, facets) = facetsFromMarkdown(block.text, "Leaflet")
            buildJsonObject {
                put("\$type", "pub.leaflet.blocks.header")
                put("plaintext", plaintext)
                put("level", block.level)
                if (facets.isNotEmpty()) put("facets", buildJsonArray { facets.forEach { add(it) } })
            }
        }
        is SharedMarkdownBlock.Paragraph -> {
            val (plaintext, facets) = facetsFromMarkdown(block.text, "Leaflet")
            buildJsonObject {
                put("\$type", "pub.leaflet.blocks.text")
                put("plaintext", plaintext)
                if (facets.isNotEmpty()) put("facets", buildJsonArray { facets.forEach { add(it) } })
            }
        }
        is SharedMarkdownBlock.Code -> buildJsonObject {
            put("\$type", "pub.leaflet.blocks.code")
            put("plaintext", block.content)
            if (block.language != null) put("language", block.language)
        }
        is SharedMarkdownBlock.Math -> buildJsonObject {
            put("\$type", "pub.leaflet.blocks.code")
            put("plaintext", block.tex)
            put("language", "math")
        }
        is SharedMarkdownBlock.Blockquote -> {
            val (plaintext, facets) = facetsFromMarkdown(block.text, "Leaflet")
            buildJsonObject {
                put("\$type", "pub.leaflet.blocks.blockquote")
                put("plaintext", plaintext)
                if (facets.isNotEmpty()) put("facets", buildJsonArray { facets.forEach { add(it) } })
            }
        }
        is SharedMarkdownBlock.Image -> {
            val blob = uploadedBlobs[block.url] ?: buildJsonObject { put("\$link", block.url) }
            buildJsonObject {
                put("\$type", "pub.leaflet.blocks.image")
                put("alt", block.alt)
                put("image", blob)
            }
        }
        SharedMarkdownBlock.HorizontalRule -> buildJsonObject {
            put("\$type", "pub.leaflet.blocks.horizontalRule")
        }
        is SharedMarkdownBlock.UnorderedList -> buildJsonObject {
            put("\$type", "pub.leaflet.blocks.unorderedList")
            put("children", buildJsonArray {
                block.items.forEach { item ->
                    add(leafletListItemToJson(item, ordered = false, uploadedBlobs))
                }
            })
        }
        is SharedMarkdownBlock.OrderedList -> buildJsonObject {
            put("\$type", "pub.leaflet.blocks.orderedList")
            put("children", buildJsonArray {
                block.items.forEach { item ->
                    add(leafletListItemToJson(item, ordered = true, uploadedBlobs))
                }
            })
        }
        is SharedMarkdownBlock.TaskList -> buildJsonObject {
            put("\$type", "pub.leaflet.blocks.unorderedList")
            put("children", buildJsonArray {
                block.items.forEach { item ->
                    add(leafletListItemToJson(item, ordered = false, uploadedBlobs, task = true))
                }
            })
        }
    }

    private fun leafletListItemToJson(
        item: SharedMarkdownListItem,
        ordered: Boolean,
        uploadedBlobs: Map<String, JsonObject>,
        task: Boolean = false
    ): JsonObject = buildJsonObject {
        val itemType = if (ordered) "pub.leaflet.blocks.orderedList#listItem" else "pub.leaflet.blocks.unorderedList#listItem"
        put("\$type", itemType)
        val (plaintext, facets) = facetsFromMarkdown(item.text, "Leaflet")
        put("content", buildJsonObject {
            put("\$type", "pub.leaflet.blocks.text")
            put("plaintext", plaintext)
            if (facets.isNotEmpty()) put("facets", buildJsonArray { facets.forEach { add(it) } })
        })
        if (task && item.checked != null) put("checked", item.checked)
    }

    private fun pcktBlockToJson(block: SharedMarkdownBlock, uploadedBlobs: Map<String, JsonObject>): JsonObject? = when (block) {
        is SharedMarkdownBlock.Heading -> {
            val (plaintext, facets) = facetsFromMarkdown(block.text, "pckt")
            buildJsonObject {
                put("\$type", "blog.pckt.block.heading")
                put("plaintext", plaintext)
                put("level", block.level)
                if (facets.isNotEmpty()) put("facets", buildJsonArray { facets.forEach { add(it) } })
            }
        }
        is SharedMarkdownBlock.Paragraph -> {
            val (plaintext, facets) = facetsFromMarkdown(block.text, "pckt")
            buildJsonObject {
                put("\$type", "blog.pckt.block.text")
                put("plaintext", plaintext)
                if (facets.isNotEmpty()) put("facets", buildJsonArray { facets.forEach { add(it) } })
            }
        }
        is SharedMarkdownBlock.Code -> buildJsonObject {
            put("\$type", "blog.pckt.block.codeBlock")
            put("plaintext", block.content)
            if (block.language != null) put("language", block.language)
        }
        is SharedMarkdownBlock.Math -> buildJsonObject {
            put("\$type", "blog.pckt.block.codeBlock")
            put("plaintext", block.tex)
            put("language", "math")
        }
        is SharedMarkdownBlock.Blockquote -> {
            val (plaintext, facets) = facetsFromMarkdown(block.text, "pckt")
            buildJsonObject {
                put("\$type", "blog.pckt.block.blockquote")
                put("content", buildJsonArray {
                    add(buildJsonObject {
                        put("\$type", "blog.pckt.block.text")
                        put("plaintext", plaintext)
                        if (facets.isNotEmpty()) put("facets", buildJsonArray { facets.forEach { add(it) } })
                    })
                })
            }
        }
        is SharedMarkdownBlock.Image -> {
            val blob = uploadedBlobs[block.url] ?: buildJsonObject { put("\$link", block.url) }
            buildJsonObject {
                put("\$type", "blog.pckt.block.image")
                put("alt", block.alt)
                put("attrs", buildJsonObject {
                    put("src", block.url)
                    put("blob", blob)
                })
            }
        }
        SharedMarkdownBlock.HorizontalRule -> buildJsonObject {
            put("\$type", "blog.pckt.block.horizontalRule")
        }
        is SharedMarkdownBlock.UnorderedList -> buildJsonObject {
            put("\$type", "blog.pckt.block.bulletList")
            put("listContent", buildJsonArray {
                block.items.forEach { item ->
                    add(pcktListItemToJson(item, uploadedBlobs))
                }
            })
        }
        is SharedMarkdownBlock.OrderedList -> buildJsonObject {
            put("\$type", "blog.pckt.block.bulletList")
            put("listContent", buildJsonArray {
                block.items.forEach { item ->
                    add(pcktListItemToJson(item, uploadedBlobs))
                }
            })
        }
        is SharedMarkdownBlock.TaskList -> buildJsonObject {
            put("\$type", "blog.pckt.block.bulletList")
            put("listContent", buildJsonArray {
                block.items.forEach { item ->
                    add(pcktListItemToJson(item, uploadedBlobs, task = true))
                }
            })
        }
    }

    private fun pcktListItemToJson(item: SharedMarkdownListItem, uploadedBlobs: Map<String, JsonObject>, task: Boolean = false): JsonObject = buildJsonObject {
        put("\$type", "blog.pckt.block.listItem")
        val (plaintext, facets) = facetsFromMarkdown(item.text, "pckt")
        put("content", buildJsonArray {
            add(buildJsonObject {
                put("\$type", "blog.pckt.block.text")
                put("plaintext", plaintext)
                if (facets.isNotEmpty()) put("facets", buildJsonArray { facets.forEach { add(it) } })
            })
        })
        if (task && item.checked != null) put("checked", item.checked)
    }

    private fun offprintBlockToJson(block: SharedMarkdownBlock, uploadedBlobs: Map<String, JsonObject>): JsonObject? = when (block) {
        is SharedMarkdownBlock.Heading -> {
            val (plaintext, facets) = facetsFromMarkdown(block.text, "Offprint")
            buildJsonObject {
                put("\$type", "app.offprint.block.heading")
                put("plaintext", plaintext)
                put("level", block.level)
                if (facets.isNotEmpty()) put("facets", buildJsonArray { facets.forEach { add(it) } })
            }
        }
        is SharedMarkdownBlock.Paragraph -> {
            val (plaintext, facets) = facetsFromMarkdown(block.text, "Offprint")
            buildJsonObject {
                put("\$type", "app.offprint.block.text")
                put("plaintext", plaintext)
                if (facets.isNotEmpty()) put("facets", buildJsonArray { facets.forEach { add(it) } })
            }
        }
        is SharedMarkdownBlock.Code -> buildJsonObject {
            put("\$type", "app.offprint.block.codeBlock")
            put("plaintext", block.content)
            if (block.language != null) put("language", block.language)
        }
        is SharedMarkdownBlock.Math -> buildJsonObject {
            put("\$type", "app.offprint.block.mathBlock")
            put("plaintext", block.tex)
        }
        is SharedMarkdownBlock.Blockquote -> {
            val (plaintext, facets) = facetsFromMarkdown(block.text, "Offprint")
            buildJsonObject {
                put("\$type", "app.offprint.block.blockquote")
                put("content", buildJsonArray {
                    add(buildJsonObject {
                        put("\$type", "app.offprint.block.text")
                        put("plaintext", plaintext)
                        if (facets.isNotEmpty()) put("facets", buildJsonArray { facets.forEach { add(it) } })
                    })
                })
            }
        }
        is SharedMarkdownBlock.Image -> {
            val blob = uploadedBlobs[block.url] ?: buildJsonObject { put("\$link", block.url) }
            buildJsonObject {
                put("\$type", "app.offprint.block.image")
                put("alt", block.alt)
                put("image", blob)
            }
        }
        SharedMarkdownBlock.HorizontalRule -> buildJsonObject {
            put("\$type", "app.offprint.block.horizontalRule")
        }
        is SharedMarkdownBlock.UnorderedList -> buildJsonObject {
            put("\$type", "app.offprint.block.bulletList")
            put("children", buildJsonArray {
                block.items.forEach { item ->
                    add(offprintListItemToJson(item, ordered = false, uploadedBlobs))
                }
            })
        }
        is SharedMarkdownBlock.OrderedList -> buildJsonObject {
            put("\$type", "app.offprint.block.bulletList")
            put("children", buildJsonArray {
                block.items.forEach { item ->
                    add(offprintListItemToJson(item, ordered = true, uploadedBlobs))
                }
            })
        }
        is SharedMarkdownBlock.TaskList -> buildJsonObject {
            put("\$type", "app.offprint.block.bulletList")
            put("children", buildJsonArray {
                block.items.forEach { item ->
                    add(offprintListItemToJson(item, ordered = false, uploadedBlobs, task = true))
                }
            })
        }
    }

    private fun offprintListItemToJson(
        item: SharedMarkdownListItem,
        ordered: Boolean,
        uploadedBlobs: Map<String, JsonObject>,
        task: Boolean = false
    ): JsonObject = buildJsonObject {
        put("\$type", "app.offprint.block.text")
        val (plaintext, facets) = facetsFromMarkdown(item.text, "Offprint")
        put("plaintext", plaintext)
        if (facets.isNotEmpty()) put("facets", buildJsonArray { facets.forEach { add(it) } })
        if (task && item.checked != null) put("checked", item.checked)
    }

    // MARK: - Facet Conversion

    private fun facetsFromMarkdown(text: String, format: String): Pair<String, List<JsonObject>> {
        val boldRegex = Regex("\\*\\*(.+?)\\*\\*")
        val italicRegex = Regex("\\*(.+?)\\*")
        val codeRegex = Regex("`(.+?)`")
        val linkRegex = Regex("\\[(.+?)\\]\\((.+?)\\)")
        val strikeRegex = Regex("~~(.+?)~~")

        val facetPrefix = when (format) {
            "pckt" -> FacetSchema.pckt.facet
            "Offprint" -> FacetSchema.offprint.facet
            else -> FacetSchema.leaflet.facet
        }

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
        for (match in strikeRegex.findAll(text)) {
            val innerStart = match.range.first + 2
            claim(Span(match.range.first, innerStart, innerStart + match.groupValues[1].length, match.range.last + 1, "strikethrough"))
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

        val removed = BooleanArray(text.length)
        for (span in spans) {
            for (i in span.start until span.innerStart) removed[i] = true
            for (i in span.innerEnd until span.end) removed[i] = true
        }

        val removedBefore = IntArray(text.length + 1)
        for (i in text.indices) {
            removedBefore[i + 1] = removedBefore[i] + (if (removed[i]) 1 else 0)
        }

        val plaintext = buildString {
            for (i in text.indices) if (!removed[i]) append(text[i])
        }

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
                put("\$type", facetPrefix)
                put("index", buildJsonObject {
                    put("byteStart", bytePrefix[pStart])
                    put("byteEnd", bytePrefix[pEnd])
                })
                put("features", buildJsonArray {
                    add(buildJsonObject {
                        when (span.type) {
                            "bold" -> put("\$type", "$facetPrefix#bold")
                            "italic" -> put("\$type", "$facetPrefix#italic")
                            "code" -> put("\$type", "$facetPrefix#code")
                            "strikethrough" -> put("\$type", "$facetPrefix#strikethrough")
                            else -> {
                                put("\$type", "$facetPrefix#link")
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

package uk.ewancroft.inkwell.ui.writer

import kotlinx.serialization.json.*
import uk.ewancroft.inkwell.data.model.content.*
import uk.ewancroft.inkwell.shared.facets.FacetConverter
import uk.ewancroft.inkwell.shared.facets.FacetSchema
import uk.ewancroft.inkwell.shared.facets.RichTextFacet
import uk.ewancroft.inkwell.shared.facets.RichTextFeature
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
        val schema = when (format) {
            "pckt" -> FacetSchema.pckt
            "Offprint" -> FacetSchema.offprint
            else -> FacetSchema.leaflet
        }
        val (plaintext, sharedFacets) = FacetConverter.markdownToFacets(
            text,
            boldType = schema.bold,
            italicType = schema.italic,
            codeType = schema.code,
            strikeType = schema.strike,
            linkType = schema.link
        )
        val facetPrefix = schema.facet
        val jsonFacets = sharedFacets.map { facet ->
            buildJsonObject {
                put("\$type", facetPrefix)
                put("index", buildJsonObject {
                    put("byteStart", facet.byteStart)
                    put("byteEnd", facet.byteEnd)
                })
                put("features", buildJsonArray {
                    add(buildJsonObject {
                        when (facet.features.firstOrNull()?.type) {
                            schema.bold -> put("\$type", "${schema.facet}#bold")
                            schema.italic -> put("\$type", "${schema.facet}#italic")
                            schema.code -> put("\$type", "${schema.facet}#code")
                            schema.strike -> put("\$type", "${schema.facet}#strikethrough")
                            else -> {
                                put("\$type", "${schema.facet}#link")
                                put("uri", facet.features.firstOrNull()?.uri)
                            }
                        }
                    })
                })
            }
        }
        return Pair(plaintext, jsonFacets)
    }
}

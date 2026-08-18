package uk.ewancroft.inkwell.shared.content

import uk.ewancroft.inkwell.shared.facets.FacetConverter
import uk.ewancroft.inkwell.shared.facets.FacetSchema
import uk.ewancroft.inkwell.shared.markdown.MarkdownBlock
import uk.ewancroft.inkwell.shared.markdown.MarkdownListItem
import uk.ewancroft.inkwell.shared.markdown.MarkdownParser
import uk.ewancroft.inkwell.shared.markdown.MarkdownSerializer

/**
 * Converts between pckt content (`blog.pckt.content`) and markdown blocks.
 *
 * Pckt stores an `items` array of blocks. Blocks map closely to markdown;
 * inline formatting uses pckt's richtext facets. Blockquotes wrap inner
 * text in a `content` array containing a text block. Lists use `content`
 * arrays with nested sub-lists.
 *
 * Mirrors iOS `PcktProvider` and Android `MarkdownConverter.pcktBlockToJson`
 * / `PcktOffprintConverter`.
 */
object PcktContentConverter {

    private val schema = FacetSchema.pckt
    private val lossLabels = BlockLossLabels.pckt

    // ── Read: pckt JSON → Markdown ─────────────────────────────────────

    /**
     * Converts a pckt content map to a [SharedConvertResult].
     *
     * The content map is expected to have the shape:
     * ```
     * { "$type": "blog.pckt.content", "items": [ ... ] }
     * ```
     */
    fun toMarkdown(content: Map<String, Any?>): SharedConvertResult {
        val items = content["items"] as? List<*> ?: return SharedConvertResult(emptyList())
        val lost = mutableSetOf<String>()
        val blocks = mutableListOf<MarkdownBlock>()

        for (item in items) {
            val map = item as? Map<*, *> ?: continue
            @Suppress("UNCHECKED_CAST")
            val blockMap = map as? Map<String, Any?> ?: continue
            val mdBlock = blockToMarkdown(blockMap, lost)
            if (mdBlock != null) blocks.add(mdBlock)
        }

        return SharedConvertResult(blocks, lost)
    }

    private fun blockToMarkdown(block: Map<String, Any?>, lost: MutableSet<String>): MarkdownBlock? {
        val type = block["\$type"] as? String ?: return null

        return when (type) {
            LeafletTypes.BLOCKS_TEXT -> {
                val text = facetsToMarkdown(block)
                text.ifEmpty { null }?.let { MarkdownBlock.Paragraph(it) }
            }

            PcktTypes.BLOCK_HEADING -> {
                val level = ((block["level"] as? Number)?.toInt() ?: 1).coerceIn(1, 6)
                val text = facetsToMarkdown(block)
                MarkdownBlock.Heading(level, text)
            }

            LeafletTypes.BLOCKS_BLOCKQUOTE -> {
                val inner = block["content"] as? List<*> ?: emptyList<Any>()
                val text = inner.mapNotNull { el ->
                    val obj = el as? Map<*, *> ?: return@mapNotNull null
                    @Suppress("UNCHECKED_CAST")
                    facetsToMarkdown(obj as Map<String, Any?>)
                }.joinToString("\n")
                if (text.isNotEmpty()) MarkdownBlock.Blockquote(text) else null
            }

            PcktTypes.BLOCK_CODE_BLOCK -> {
                val language = block["language"] as? String
                val content = block["plaintext"] as? String ?: ""
                MarkdownBlock.Code(language, content)
            }

            LeafletTypes.BLOCKS_HORIZONTAL_RULE -> MarkdownBlock.HorizontalRule

            PcktTypes.BLOCK_HARD_BREAK -> null

            LeafletTypes.BLOCKS_IMAGE -> {
                val attrs = block["attrs"] as? Map<*, *>
                val blob = attrs?.get("blob") as? Map<*, *>
                val ref = blob?.get("\$link") as? String
                    ?: attrs?.get("src") as? String
                    ?: ""
                if (ref.isEmpty()) null
                else MarkdownBlock.Image(attrs?.get("alt") as? String ?: "", ref)
            }

            PcktTypes.BLOCK_BULLET_LIST -> {
                val listContent = block["content"] as? List<*> ?: block["listContent"] as? List<*> ?: return null
                val items = listContent.mapNotNull { el ->
                    val obj = el as? Map<*, *> ?: return@mapNotNull null
                    @Suppress("UNCHECKED_CAST")
                    listItemToMarkdown(obj as Map<String, Any?>, false)
                }
                if (items.isNotEmpty()) MarkdownBlock.UnorderedList(items) else null
            }

            LeafletTypes.BLOCKS_ORDERED_LIST -> {
                val listContent = block["content"] as? List<*> ?: block["listContent"] as? List<*> ?: return null
                val items = listContent.mapNotNull { el ->
                    val obj = el as? Map<*, *> ?: return@mapNotNull null
                    @Suppress("UNCHECKED_CAST")
                    listItemToMarkdown(obj as Map<String, Any?>, true)
                }
                if (items.isNotEmpty()) MarkdownBlock.OrderedList(1, items) else null
            }

            PcktTypes.BLOCK_TASK_LIST -> {
                val listContent = block["content"] as? List<*> ?: block["listContent"] as? List<*> ?: return null
                val items = listContent.mapNotNull { el ->
                    val obj = el as? Map<*, *> ?: return@mapNotNull null
                    @Suppress("UNCHECKED_CAST")
                    listItemToMarkdown(obj as Map<String, Any?>, false, isTask = true)
                }
                if (items.isNotEmpty()) MarkdownBlock.TaskList(items) else null
            }

            else -> {
                val label = lossLabels[type]
                if (label != null) lost.add(label) else lost.add("an unsupported block")
                null
            }
        }
    }

    private fun listItemToMarkdown(item: Map<String, Any?>, ordered: Boolean, isTask: Boolean = false): MarkdownListItem {
        var text = ""
        var children: List<MarkdownListItem>? = null

        val contentEl = item["content"]
        when (contentEl) {
            is List<*> -> {
                for (el in contentEl) {
                    val obj = el as? Map<*, *> ?: continue
                    @Suppress("UNCHECKED_CAST")
                    val blockMap = obj as? Map<String, Any?> ?: continue
                    val blockType = blockMap["\$type"] as? String ?: continue
                    when (blockType) {
                        LeafletTypes.BLOCKS_TEXT -> {
                            text += facetsToMarkdown(blockMap)
                        }
                        LeafletTypes.BLOCKS_IMAGE -> {
                            val attrs = blockMap["attrs"] as? Map<*, *>
                            val blob = attrs?.get("blob") as? Map<*, *>
                            val ref = blob?.get("\$link") as? String
                                ?: attrs?.get("src") as? String
                                ?: ""
                            if (ref.isNotEmpty()) text += "![$ref]($ref)"
                        }
                        PcktTypes.BLOCK_BULLET_LIST, LeafletTypes.BLOCKS_ORDERED_LIST -> {
                            val subItems = blockMap["content"] as? List<*> ?: blockMap["listContent"] as? List<*>
                            if (subItems != null) {
                                val isSubOrdered = blockType == LeafletTypes.BLOCKS_ORDERED_LIST
                                children = subItems.mapNotNull { subEl ->
                                    val subObj = subEl as? Map<*, *> ?: return@mapNotNull null
                                    @Suppress("UNCHECKED_CAST")
                                    listItemToMarkdown(subObj as Map<String, Any?>, isSubOrdered)
                                }
                            }
                        }
                    }
                }
            }
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                text = facetsToMarkdown(contentEl as Map<String, Any?>)
            }
        }

        val checked = when {
            isTask -> item["checked"] as? Boolean
            else -> null
        }

        val childrenFromField = item["children"] as? List<*>
        if (children == null && childrenFromField != null && childrenFromField.isNotEmpty()) {
            children = childrenFromField.mapNotNull { el ->
                val obj = el as? Map<*, *> ?: return@mapNotNull null
                @Suppress("UNCHECKED_CAST")
                listItemToMarkdown(obj as Map<String, Any?>, false)
            }
        }

        return MarkdownListItem(text, checked, children?.ifEmpty { null })
    }

    private fun facetsToMarkdown(block: Map<String, Any?>): String {
        val plaintext = block["plaintext"] as? String ?: ""
        val facets = block["facets"] as? List<*>
        if (facets.isNullOrEmpty()) return plaintext

        val sharedFacets = parseFacets(facets)
        return FacetConverter.facetsToMarkdown(
            plaintext, sharedFacets,
            schema.bold, schema.italic, schema.code, schema.strike, schema.link,
            schema.lossy
        )
    }

    private fun parseFacets(facets: List<*>): List<uk.ewancroft.inkwell.shared.facets.RichTextFacet> {
        return facets.mapNotNull { el ->
            val map = el as? Map<*, *> ?: return@mapNotNull null
            val index = map["index"] as? Map<*, *> ?: return@mapNotNull null
            val byteStart = (index["byteStart"] as? Number)?.toInt() ?: return@mapNotNull null
            val byteEnd = (index["byteEnd"] as? Number)?.toInt() ?: return@mapNotNull null
            val featureList = map["features"] as? List<*> ?: return@mapNotNull null
            val features = featureList.mapNotNull { f ->
                val fMap = f as? Map<*, *> ?: return@mapNotNull null
                val type = fMap["\$type"] as? String ?: return@mapNotNull null
                val uri = fMap["uri"] as? String
                uk.ewancroft.inkwell.shared.facets.RichTextFeature(type, uri)
            }
            if (features.isEmpty()) null
            else uk.ewancroft.inkwell.shared.facets.RichTextFacet(byteStart, byteEnd, features)
        }
    }

    // ── Write: Markdown → pckt JSON ────────────────────────────────────

    /**
     * Converts markdown text to a pckt content map.
     *
     * @param markdown The markdown source text.
     * @param uploadedBlobs Map of CID → blob JSON for image round-tripping.
     */
    fun fromMarkdown(markdown: String, uploadedBlobs: Map<String, Map<String, Any?>> = emptyMap()): SharedWriteResult {
        val blocks = MarkdownParser.parse(markdown)
        val items = blocks.mapNotNull { blockToJson(it, uploadedBlobs) }

        val content = mapOf(
            "\$type" to PcktTypes.CONTENT,
            "items" to items
        )
        return SharedWriteResult(content)
    }

    private fun blockToJson(block: MarkdownBlock, uploadedBlobs: Map<String, Map<String, Any?>>): Map<String, Any?>? = when (block) {
        is MarkdownBlock.Heading -> {
            val (plaintext, facets) = markdownToFacets(block.text)
            buildMap {
                put("\$type", PcktTypes.BLOCK_HEADING)
                put("plaintext", plaintext)
                put("level", block.level)
                if (facets.isNotEmpty()) put("facets", facets)
            }
        }

        is MarkdownBlock.Paragraph -> {
            val (plaintext, facets) = markdownToFacets(block.text)
            buildMap {
                put("\$type", LeafletTypes.BLOCKS_TEXT)
                put("plaintext", plaintext)
                if (facets.isNotEmpty()) put("facets", facets)
            }
        }

        is MarkdownBlock.Code -> buildMap {
            put("\$type", PcktTypes.BLOCK_CODE_BLOCK)
            put("plaintext", block.content)
            if (block.language != null) put("language", block.language)
        }

        is MarkdownBlock.Math -> buildMap {
            put("\$type", PcktTypes.BLOCK_CODE_BLOCK)
            put("plaintext", block.tex)
            put("language", "math")
        }

        is MarkdownBlock.Blockquote -> {
            val (plaintext, facets) = markdownToFacets(block.text)
            val innerBlock = buildMap<String, Any?> {
                put("\$type", LeafletTypes.BLOCKS_TEXT)
                put("plaintext", plaintext)
                if (facets.isNotEmpty()) put("facets", facets)
            }
            buildMap {
                put("\$type", LeafletTypes.BLOCKS_BLOCKQUOTE)
                put("content", listOf(innerBlock))
            }
        }

        is MarkdownBlock.Image -> {
            val blob = uploadedBlobs[block.url] ?: mapOf("\$link" to block.url)
            buildMap {
                put("\$type", LeafletTypes.BLOCKS_IMAGE)
                put("alt", block.alt)
                put("attrs", mapOf(
                    "src" to block.url,
                    "blob" to blob
                ))
            }
        }

        MarkdownBlock.HorizontalRule -> mapOf("\$type" to LeafletTypes.BLOCKS_HORIZONTAL_RULE)

        is MarkdownBlock.UnorderedList -> mapOf(
            "\$type" to PcktTypes.BLOCK_BULLET_LIST,
            "listContent" to block.items.map { listItemToJson(it, task = false) }
        )

        is MarkdownBlock.OrderedList -> mapOf(
            "\$type" to LeafletTypes.BLOCKS_ORDERED_LIST,
            "listContent" to block.items.map { listItemToJson(it, task = false) },
            "start" to block.start
        )

        is MarkdownBlock.TaskList -> mapOf(
            "\$type" to PcktTypes.BLOCK_TASK_LIST,
            "listContent" to block.items.map { listItemToJson(it, task = true) }
        )
    }

    private fun listItemToJson(item: MarkdownListItem, task: Boolean): Map<String, Any?> {
        val (plaintext, facets) = markdownToFacets(item.text)
        val contentBlocks = mutableListOf<Map<String, Any?>>()
        contentBlocks.add(buildMap {
            put("\$type", LeafletTypes.BLOCKS_TEXT)
            put("plaintext", plaintext)
            if (facets.isNotEmpty()) put("facets", facets)
        })

        val mdChildren = item.children
        if (mdChildren != null && mdChildren.isNotEmpty()) {
            contentBlocks.add(buildMap {
                put("\$type", PcktTypes.BLOCK_BULLET_LIST)
                put("listContent", mdChildren.map { listItemToJson(it, task = false) })
            })
        }

        val itemType = if (task) "${PcktTypes.BLOCK_PREFIX}taskItem" else PcktTypes.BLOCK_LIST_ITEM
        val result = mutableMapOf<String, Any?>(
            "\$type" to itemType,
            "content" to contentBlocks
        )
        if (task) result["checked"] = item.checked ?: false
        return result
    }

    private fun markdownToFacets(text: String): Pair<String, List<Map<String, Any?>>> {
        val (plaintext, sharedFacets) = FacetConverter.markdownToFacets(
            text, schema.bold, schema.italic, schema.code, schema.strike, schema.link
        )
        val facetPrefix = schema.facet
        val jsonFacets = sharedFacets.map { facet ->
            val featureType = facet.features.firstOrNull()?.type ?: ""
            val featureMap = mutableMapOf<String, Any?>(
                "\$type" to when (featureType) {
                    schema.bold -> "$facetPrefix#bold"
                    schema.italic -> "$facetPrefix#italic"
                    schema.code -> "$facetPrefix#code"
                    schema.strike -> "$facetPrefix#strikethrough"
                    else -> "$facetPrefix#link"
                }
            )
            if (featureType == schema.link) {
                featureMap["uri"] = facet.features.firstOrNull()?.uri
            }
            mapOf(
                "\$type" to facetPrefix,
                "index" to mapOf(
                    "byteStart" to facet.byteStart,
                    "byteEnd" to facet.byteEnd
                ),
                "features" to listOf(featureMap)
            )
        }
        return Pair(plaintext, jsonFacets)
    }
}

package uk.ewancroft.inkwell.shared.content

import uk.ewancroft.inkwell.shared.facets.FacetConverter
import uk.ewancroft.inkwell.shared.facets.FacetSchema
import uk.ewancroft.inkwell.shared.markdown.MarkdownBlock
import uk.ewancroft.inkwell.shared.markdown.MarkdownListItem
import uk.ewancroft.inkwell.shared.markdown.MarkdownParser
import uk.ewancroft.inkwell.shared.markdown.MarkdownSerializer

/**
 * Converts between Offprint content (`app.offprint.content`) and markdown blocks.
 *
 * Offprint stores an `items` array of blocks. Blocks map closely to markdown;
 * inline formatting uses Offprint's richtext facets. Headings are capped at
 * level 3. Math blocks are supported natively. Blockquotes wrap inner text
 * in a `content` array. Lists use `children` arrays.
 *
 * Mirrors iOS `OffprintProvider` and Android `MarkdownConverter.offprintBlockToJson`
 * / `PcktOffprintConverter`.
 */
object OffprintContentConverter {

    private val schema = FacetSchema.offprint
    private val lossLabels = BlockLossLabels.offprint

    // ── Read: Offprint JSON → Markdown ─────────────────────────────────

    /**
     * Converts an Offprint content map to a [SharedConvertResult].
     *
     * The content map is expected to have the shape:
     * ```
     * { "$type": "app.offprint.content", "items": [ ... ] }
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

            OffprintTypes.BLOCK_HEADING -> {
                val level = ((block["level"] as? Number)?.toInt() ?: 1).coerceIn(1, 3)
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

            OffprintTypes.BLOCK_CODE_BLOCK -> {
                val language = block["language"] as? String
                val content = block["code"] as? String ?: block["plaintext"] as? String ?: ""
                MarkdownBlock.Code(language, content)
            }

            OffprintTypes.BLOCK_MATH_BLOCK -> {
                val tex = block["plaintext"] as? String ?: ""
                MarkdownBlock.Math(tex)
            }

            LeafletTypes.BLOCKS_HORIZONTAL_RULE -> MarkdownBlock.HorizontalRule

            LeafletTypes.BLOCKS_IMAGE -> {
                val image = block["image"] as? Map<*, *>
                val ref = image?.get("\$link") as? String ?: ""
                if (ref.isEmpty()) null
                else MarkdownBlock.Image(block["alt"] as? String ?: "", ref)
            }

            OffprintTypes.BLOCK_BULLET_LIST -> {
                val children = block["children"] as? List<*> ?: emptyList<Any>()
                val items = children.mapNotNull { el ->
                    val obj = el as? Map<*, *> ?: return@mapNotNull null
                    @Suppress("UNCHECKED_CAST")
                    listItemToMarkdown(obj as Map<String, Any?>)
                }
                if (items.isNotEmpty()) MarkdownBlock.UnorderedList(items) else null
            }

            LeafletTypes.BLOCKS_ORDERED_LIST -> {
                val start = (block["start"] as? Number)?.toInt() ?: 1
                val children = block["children"] as? List<*> ?: emptyList<Any>()
                val items = children.mapNotNull { el ->
                    val obj = el as? Map<*, *> ?: return@mapNotNull null
                    @Suppress("UNCHECKED_CAST")
                    listItemToMarkdown(obj as Map<String, Any?>)
                }
                if (items.isNotEmpty()) MarkdownBlock.OrderedList(start, items) else null
            }

            OffprintTypes.BLOCK_TASK_LIST -> {
                val children = block["children"] as? List<*> ?: emptyList<Any>()
                val items = children.mapNotNull { el ->
                    val obj = el as? Map<*, *> ?: return@mapNotNull null
                    @Suppress("UNCHECKED_CAST")
                    listItemToMarkdown(obj as Map<String, Any?>)
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

    private fun listItemToMarkdown(item: Map<String, Any?>): MarkdownListItem {
        var text = ""
        val content = item["content"] as? Map<String, Any?>
        if (content != null && content["\$type"] == LeafletTypes.BLOCKS_TEXT) {
            text = facetsToMarkdown(content)
        }

        var children: List<MarkdownListItem>? = null
        val childrenList = item["children"] as? List<*>
        if (childrenList != null && childrenList.isNotEmpty()) {
            children = childrenList.mapNotNull { el ->
                val obj = el as? Map<*, *> ?: return@mapNotNull null
                @Suppress("UNCHECKED_CAST")
                listItemToMarkdown(obj as Map<String, Any?>)
            }
        }

        val checked = item["checked"] as? Boolean

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

    // ── Write: Markdown → Offprint JSON ────────────────────────────────

    /**
     * Converts markdown text to an Offprint content map.
     *
     * @param markdown The markdown source text.
     * @param uploadedBlobs Map of CID → blob JSON for image round-tripping.
     */
    fun fromMarkdown(markdown: String, uploadedBlobs: Map<String, Map<String, Any?>> = emptyMap()): SharedWriteResult {
        val blocks = MarkdownParser.parse(markdown)
        val items = blocks.mapNotNull { blockToJson(it) }

        val content = mapOf(
            "\$type" to OffprintTypes.CONTENT,
            "items" to items
        )
        return SharedWriteResult(content)
    }

    private fun blockToJson(block: MarkdownBlock): Map<String, Any?>? = when (block) {
        is MarkdownBlock.Heading -> {
            val (plaintext, facets) = markdownToFacets(block.text)
            buildMap {
                put("\$type", OffprintTypes.BLOCK_HEADING)
                put("plaintext", plaintext)
                put("level", minOf(block.level, 3))
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
            put("\$type", OffprintTypes.BLOCK_CODE_BLOCK)
            put("code", block.content)
            if (block.language != null) put("language", block.language)
        }

        is MarkdownBlock.Math -> buildMap {
            put("\$type", OffprintTypes.BLOCK_MATH_BLOCK)
            put("plaintext", block.tex)
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
            val blob = mapOf("\$link" to block.url)
            buildMap {
                put("\$type", LeafletTypes.BLOCKS_IMAGE)
                put("alt", block.alt)
                put("image", blob)
            }
        }

        MarkdownBlock.HorizontalRule -> mapOf("\$type" to LeafletTypes.BLOCKS_HORIZONTAL_RULE)

        is MarkdownBlock.UnorderedList -> mapOf(
            "\$type" to OffprintTypes.BLOCK_BULLET_LIST,
            "children" to block.items.map { listItemToJson(it, ordered = false) }
        )

        is MarkdownBlock.OrderedList -> mapOf(
            "\$type" to LeafletTypes.BLOCKS_ORDERED_LIST,
            "children" to block.items.map { listItemToJson(it, ordered = true) },
            "start" to block.start
        )

        is MarkdownBlock.TaskList -> mapOf(
            "\$type" to OffprintTypes.BLOCK_TASK_LIST,
            "children" to block.items.map { listItemToJson(it, ordered = false) }
        )
    }

    private fun listItemToJson(item: MarkdownListItem, ordered: Boolean): Map<String, Any?> {
        val (plaintext, facets) = markdownToFacets(item.text)
        val textBlock = buildMap<String, Any?> {
            put("\$type", LeafletTypes.BLOCKS_TEXT)
            put("plaintext", plaintext)
            if (facets.isNotEmpty()) put("facets", facets)
        }

        val result = mutableMapOf<String, Any?>(
            "content" to textBlock
        )
        if (item.checked != null) result["checked"] = item.checked
        val mdChildren = item.children
        if (mdChildren != null && mdChildren.isNotEmpty()) {
            result["children"] = mdChildren.map { listItemToJson(it, ordered = false) }
        }
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

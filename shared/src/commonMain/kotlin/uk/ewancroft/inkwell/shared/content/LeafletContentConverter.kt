package uk.ewancroft.inkwell.shared.content

import uk.ewancroft.inkwell.shared.facets.FacetConverter
import uk.ewancroft.inkwell.shared.facets.FacetSchema
import uk.ewancroft.inkwell.shared.markdown.MarkdownBlock
import uk.ewancroft.inkwell.shared.markdown.MarkdownListItem
import uk.ewancroft.inkwell.shared.markdown.MarkdownParser
import uk.ewancroft.inkwell.shared.markdown.MarkdownSerializer

/**
 * Converts between Leaflet content (`pub.leaflet.content`) and markdown blocks.
 *
 * Leaflet documents are a list of pages; we read and write a single
 * `linearDocument` page whose blocks map closely to markdown. Inline
 * formatting uses Leaflet's richtext facets.
 *
 * Mirrors iOS `LeafletProvider` and Android `MarkdownConverter.leafletBlockToJson`.
 */
object LeafletContentConverter {

    private val schema = FacetSchema.leaflet
    private val lossLabels = BlockLossLabels.leaflet

    // ── Read: Leaflet JSON → Markdown ──────────────────────────────────

    /**
     * Converts a Leaflet content map to a [SharedConvertResult].
     *
     * The content map is expected to have the shape:
     * ```
     * { "$type": "pub.leaflet.content", "pages": [ { "blocks": [ ... ] } ] }
     * ```
     *
     * Each block is a map with a `$type` key and format-specific fields.
     */
    fun toMarkdown(content: Map<String, Any?>): SharedConvertResult {
        val pages = content["pages"] as? List<*> ?: return SharedConvertResult(emptyList())
        val page = pages.filterIsInstance<Map<String, Any?>>()
            .firstOrNull { it["\$type"] == LeafletTypes.PAGES_LINEAR_DOCUMENT }
            ?: pages.filterIsInstance<Map<String, Any?>>().firstOrNull()
            ?: return SharedConvertResult(emptyList())

        val blockContainers = page["blocks"] as? List<*> ?: return SharedConvertResult(emptyList())
        val lost = mutableSetOf<String>()
        val blocks = mutableListOf<MarkdownBlock>()

        for (container in blockContainers) {
            val map = container as? Map<*, *> ?: continue
            @Suppress("UNCHECKED_CAST")
            val containerMap = map as? Map<String, Any?> ?: continue
            val block = containerMap["block"] as? Map<String, Any?> ?: continue
            val alignment = containerMap["alignment"] as? String
            val mdBlock = blockToMarkdown(block, alignment, lost)
            if (mdBlock != null) blocks.add(mdBlock)
        }

        return SharedConvertResult(blocks, lost)
    }

    private fun blockToMarkdown(block: Map<String, Any?>, alignment: String?, lost: MutableSet<String>): MarkdownBlock? {
        if (alignment != null && !alignment.endsWith("textAlignLeft")) {
            lost.add("text alignment")
        }

        val type = block["\$type"] as? String ?: return null

        return when (type) {
            LeafletTypes.BLOCKS_TEXT -> {
                val text = facetsToMarkdown(block)
                text.ifEmpty { null }?.let { MarkdownBlock.Paragraph(it) }
            }

            LeafletTypes.BLOCKS_HEADER -> {
                val level = ((block["level"] as? Number)?.toInt() ?: 1).coerceIn(1, 6)
                val text = facetsToMarkdown(block)
                MarkdownBlock.Heading(level, text)
            }

            LeafletTypes.BLOCKS_BLOCKQUOTE -> {
                val text = facetsToMarkdown(block)
                MarkdownBlock.Blockquote(text)
            }

            LeafletTypes.BLOCKS_CODE -> {
                val language = block["language"] as? String
                val content = block["plaintext"] as? String ?: ""
                MarkdownBlock.Code(language, content)
            }

            LeafletTypes.BLOCKS_MATH -> {
                val tex = block["tex"] as? String ?: ""
                MarkdownBlock.Math(tex)
            }

            LeafletTypes.BLOCKS_HORIZONTAL_RULE -> MarkdownBlock.HorizontalRule

            LeafletTypes.BLOCKS_IMAGE -> {
                val image = block["image"] as? Map<*, *>
                val ref = image?.get("\$link") as? String ?: ""
                if (ref.isEmpty()) null
                else MarkdownBlock.Image(block["alt"] as? String ?: "", ref)
            }

            LeafletTypes.BLOCKS_UNORDERED_LIST -> {
                val children = block["children"] as? List<*> ?: emptyList<Any>()
                val items = children.mapNotNull { (it as? Map<*, *>)?.let { m -> @Suppress("UNCHECKED_CAST") listItemToMarkdown(m as Map<String, Any?>) } }
                MarkdownBlock.UnorderedList(items)
            }

            LeafletTypes.BLOCKS_ORDERED_LIST -> {
                val start = (block["startIndex"] as? Number)?.toInt() ?: 1
                val children = block["children"] as? List<*> ?: emptyList<Any>()
                val items = children.mapNotNull { (it as? Map<*, *>)?.let { m -> @Suppress("UNCHECKED_CAST") listItemToMarkdown(m as Map<String, Any?>) } }
                MarkdownBlock.OrderedList(start, items)
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
        if (content != null) {
            val contentType = content["\$type"] as? String
            when (contentType) {
                LeafletTypes.BLOCKS_TEXT -> text = facetsToMarkdown(content)
                LeafletTypes.BLOCKS_IMAGE -> {
                    val cid = (content["image"] as? Map<*, *>)?.get("\$link") as? String ?: ""
                    if (cid.isNotEmpty()) text = "![${content["alt"] as? String ?: ""}]($cid)"
                }
                else -> text = content["plaintext"] as? String ?: ""
            }
        }

        val children = item["children"] as? List<*>
        val mdChildren = children?.mapNotNull { (it as? Map<*, *>)?.let { m -> @Suppress("UNCHECKED_CAST") listItemToMarkdown(m as Map<String, Any?>) } }

        val checked = item["checked"] as? Boolean

        return MarkdownListItem(text, checked, mdChildren?.ifEmpty { null })
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

    // ── Write: Markdown → Leaflet JSON ─────────────────────────────────

    /**
     * Converts markdown text to a Leaflet content map.
     *
     * @param markdown The markdown source text.
     * @param uploadedBlobs Map of CID → blob JSON for image round-tripping.
     */
    fun fromMarkdown(markdown: String, uploadedBlobs: Map<String, Map<String, Any?>> = emptyMap()): SharedWriteResult {
        val blocks = MarkdownParser.parse(markdown)
        val leafletBlocks = blocks.mapNotNull { blockToJson(it, uploadedBlobs) }

        val content = mapOf(
            "\$type" to LeafletTypes.CONTENT,
            "pages" to listOf(
                mapOf(
                    "\$type" to LeafletTypes.PAGES_LINEAR_DOCUMENT,
                    "id" to "page-1",
                    "blocks" to leafletBlocks.map { mapOf("block" to it) }
                )
            )
        )
        return SharedWriteResult(content)
    }

    private fun blockToJson(block: MarkdownBlock, uploadedBlobs: Map<String, Map<String, Any?>>): Map<String, Any?>? = when (block) {
        is MarkdownBlock.Heading -> {
            val (plaintext, facets) = markdownToFacets(block.text)
            buildMap {
                put("\$type", LeafletTypes.BLOCKS_HEADER)
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
            put("\$type", LeafletTypes.BLOCKS_CODE)
            put("plaintext", block.content)
            if (block.language != null) put("language", block.language)
        }

        is MarkdownBlock.Math -> buildMap {
            put("\$type", LeafletTypes.BLOCKS_CODE)
            put("plaintext", block.tex)
            put("language", "math")
        }

        is MarkdownBlock.Blockquote -> {
            val (plaintext, facets) = markdownToFacets(block.text)
            buildMap {
                put("\$type", LeafletTypes.BLOCKS_BLOCKQUOTE)
                put("plaintext", plaintext)
                if (facets.isNotEmpty()) put("facets", facets)
            }
        }

        is MarkdownBlock.Image -> {
            val blob = uploadedBlobs[block.url] ?: mapOf("\$link" to block.url)
            buildMap {
                put("\$type", LeafletTypes.BLOCKS_IMAGE)
                put("alt", block.alt)
                put("image", blob)
            }
        }

        MarkdownBlock.HorizontalRule -> mapOf("\$type" to LeafletTypes.BLOCKS_HORIZONTAL_RULE)

        is MarkdownBlock.UnorderedList -> mapOf(
            "\$type" to LeafletTypes.BLOCKS_UNORDERED_LIST,
            "children" to block.items.map { listItemToJson(it, ordered = false, uploadedBlobs) }
        )

        is MarkdownBlock.OrderedList -> mapOf(
            "\$type" to LeafletTypes.BLOCKS_ORDERED_LIST,
            "children" to block.items.map { listItemToJson(it, ordered = true, uploadedBlobs) },
            "startIndex" to block.start
        )

        is MarkdownBlock.TaskList -> mapOf(
            "\$type" to LeafletTypes.BLOCKS_UNORDERED_LIST,
            "children" to block.items.map { listItemToJson(it, ordered = false, uploadedBlobs, task = true) }
        )
    }

    private fun listItemToJson(item: MarkdownListItem, ordered: Boolean, uploadedBlobs: Map<String, Map<String, Any?>>, task: Boolean = false): Map<String, Any?> {
        val itemType = if (ordered) LeafletTypes.LIST_ITEM_ORDERED else LeafletTypes.LIST_ITEM_UNORDERED
        val (plaintext, facets) = markdownToFacets(item.text)
        val contentMap = mutableMapOf<String, Any?>(
            "\$type" to LeafletTypes.BLOCKS_TEXT,
            "plaintext" to plaintext
        )
        if (facets.isNotEmpty()) contentMap["facets"] = facets

        val result = mutableMapOf<String, Any?>(
            "\$type" to itemType,
            "content" to contentMap
        )
        if (task && item.checked != null) result["checked"] = item.checked
        val mdChildren = item.children
        if (mdChildren != null && mdChildren.isNotEmpty()) {
            result["children"] = mdChildren.map { listItemToJson(it, ordered = false, uploadedBlobs) }
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

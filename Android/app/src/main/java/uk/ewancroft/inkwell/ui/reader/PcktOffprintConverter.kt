package uk.ewancroft.inkwell.ui.reader

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import uk.ewancroft.inkwell.shared.facets.FacetSchema
import uk.ewancroft.inkwell.shared.facets.FacetDefinition
import uk.ewancroft.inkwell.shared.facets.FacetConverter
import uk.ewancroft.inkwell.shared.facets.RichTextFacet
import uk.ewancroft.inkwell.shared.facets.RichTextFeature

data class ConvertResult(
    val markdown: String?,
    val lost: List<String> = emptyList(),
)

/**
 * Converts pckt (blog.pckt.content) and Offprint (app.offprint.content)
 * block-array records into markdown strings. Works from raw JSON to avoid
 * kotlinx.serialization type-mismatch issues with the polymorphic `content`
 * field (blocks vs list items).
 *
 * Mirrors iOS PcktProvider.toMarkdown / OffprintProvider.toMarkdown.
 */
object PcktOffprintConverter {

    fun isSupported(formatType: String?): Boolean {
        return formatType == "blog.pckt.content" || formatType == "app.offprint.content"
    }

    fun toMarkdown(contentObj: JsonObject, formatType: String, authorDid: String = ""): ConvertResult {
        val items = contentObj["items"] as? JsonArray ?: return ConvertResult(null)
        val prefix = if (formatType == "blog.pckt.content") "blog.pckt.block." else "app.offprint.block."
        val isOffprint = formatType == "app.offprint.content"
        val schema = if (isOffprint) FacetSchema.offprint else FacetSchema.pckt

        val blocks = mutableListOf<String>()
        val lost = mutableSetOf<String>()
        for (item in items) {
            val obj = item as? JsonObject ?: continue
            val md = blockToMarkdown(obj, prefix, isOffprint, authorDid, schema, lost)
            if (md != null) blocks.add(md)
        }
        return ConvertResult(blocks.joinToString("\n\n"), lost.toList())
    }

    private fun blockToMarkdown(
        block: JsonObject,
        prefix: String,
        isOffprint: Boolean,
        authorDid: String,
        schema: FacetDefinition,
        lost: MutableSet<String>,
    ): String? {
        val type = block["\$type"]?.jsonPrimitive?.contentOrNull ?: return null

        return when (type) {
            "${prefix}text" -> {
                val text = facetsToMarkdown(
                    block["plaintext"]?.jsonPrimitive?.contentOrNull ?: "",
                    block["facets"] as? JsonArray,
                    schema,
                    lost,
                )
                text.ifEmpty { null }
            }

            "${prefix}heading" -> {
                val maxLevel = if (isOffprint) 3 else 6
                val level = (block["level"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 1).coerceIn(1, maxLevel)
                val text = facetsToMarkdown(
                    block["plaintext"]?.jsonPrimitive?.contentOrNull ?: "",
                    block["facets"] as? JsonArray,
                    schema,
                    lost,
                )
                val hashes = "#".repeat(level)
                "$hashes $text"
            }

            "${prefix}blockquote" -> {
                val inner = block["content"] as? JsonArray ?: return null
                val parts = inner.mapNotNull { el ->
                    val obj = el as? JsonObject ?: return@mapNotNull null
                    blockContentToInlineMarkdown(obj, prefix, isOffprint, authorDid, schema, lost)
                }
                val text = parts.joinToString("\n")
                if (text.isNotEmpty()) "> ${text.replace("\n", "\n> ")}" else null
            }

            "${prefix}codeBlock" -> {
                val lang = block["language"]?.jsonPrimitive?.contentOrNull ?: ""
                val content = block["plaintext"]?.jsonPrimitive?.contentOrNull ?: ""
                "```$lang\n$content\n```"
            }

            "${prefix}mathBlock" -> {
                val content = block["plaintext"]?.jsonPrimitive?.contentOrNull ?: ""
                "$$\n$content\n$$"
            }

            "${prefix}horizontalRule" -> "---"

            "${prefix}hardBreak" -> null

            "${prefix}image" -> {
                val url = resolveImageUrl(block, isOffprint, authorDid)
                val alt = block["alt"]?.jsonPrimitive?.contentOrNull ?: ""
                if (url.isNotEmpty()) "![$alt]($url)" else null
            }

            "${prefix}bulletList" -> {
                val items = block["content"] as? JsonArray ?: return null
                val rendered = items.mapNotNull { el ->
                    val obj = el as? JsonObject ?: return@mapNotNull null
                    listItemToMarkdown(obj, prefix, isOffprint, authorDid, schema, lost, ordered = false)
                }
                if (rendered.isNotEmpty()) rendered.joinToString("\n") else null
            }

            "${prefix}orderedList" -> {
                val start = block["start"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 1
                val items = block["content"] as? JsonArray ?: return null
                val rendered = items.mapIndexedNotNull { index, el ->
                    val obj = el as? JsonObject ?: return@mapIndexedNotNull null
                    listItemToMarkdown(obj, prefix, isOffprint, authorDid, schema, lost, ordered = true, number = start + index)
                }
                if (rendered.isNotEmpty()) rendered.joinToString("\n") else null
            }

            "${prefix}taskList" -> {
                val items = block["content"] as? JsonArray ?: return null
                val rendered = items.mapNotNull { el ->
                    val obj = el as? JsonObject ?: return@mapNotNull null
                    listItemToMarkdown(obj, prefix, isOffprint, authorDid, schema, lost, ordered = false, isTask = true)
                }
                if (rendered.isNotEmpty()) rendered.joinToString("\n") else null
            }

            else -> {
                val label = schema.lossy[type]
                if (label != null) {
                    lost.add(label)
                } else {
                    lost.add("unsupported content")
                }
                null
            }
        }
    }

    /**
     * Converts a block inside a blockquote/list to inline markdown.
     * Handles text, images, and nested formatting blocks.
     */
    private fun blockContentToInlineMarkdown(
        obj: JsonObject,
        prefix: String,
        isOffprint: Boolean,
        authorDid: String,
        schema: FacetDefinition,
        lost: MutableSet<String>,
    ): String? {
        val type = obj["\$type"]?.jsonPrimitive?.contentOrNull ?: return null
        return when (type) {
            "${prefix}text" -> {
                facetsToMarkdown(
                    obj["plaintext"]?.jsonPrimitive?.contentOrNull ?: "",
                    obj["facets"] as? JsonArray,
                    schema,
                    lost,
                )
            }
            "${prefix}image" -> {
                val url = resolveImageUrl(obj, isOffprint, authorDid)
                val alt = obj["alt"]?.jsonPrimitive?.contentOrNull ?: ""
                if (url.isNotEmpty()) "![$alt]($url)" else null
            }
            else -> {
                val label = schema.lossy[type]
                if (label != null) lost.add(label) else lost.add("unsupported content")
                null
            }
        }
    }

    /**
     * Resolves an image URL from either `attrs.src` or `attrs.blob.$link`.
     * For Offprint, also checks `image.$link` directly.
     * Converts bare CID refs to CDN URLs using the author DID.
     */
    private fun resolveImageUrl(block: JsonObject, isOffprint: Boolean, authorDid: String): String {
        var ref = ""

        if (isOffprint) {
            val image = block["image"] as? JsonObject
            ref = image?.get("\$link")?.jsonPrimitive?.contentOrNull ?: ""
        }

        if (ref.isEmpty()) {
            val attrs = block["attrs"] as? JsonObject
            ref = attrs?.get("src")?.jsonPrimitive?.contentOrNull ?: ""
        }

        if (ref.isEmpty()) {
            val attrs = block["attrs"] as? JsonObject
            val blob = attrs?.get("blob") as? JsonObject
            ref = blob?.get("\$link")?.jsonPrimitive?.contentOrNull ?: ""
        }

        if (ref.isEmpty()) return ""

        if (ref.startsWith("http://") || ref.startsWith("https://")) return ref

        if (authorDid.isNotEmpty()) {
            return "https://cdn.bsky.app/img/feed_thumbnail/plain/$authorDid/$ref"
        }

        return ref
    }

    // ── List Item Conversion ──────────────────────────────────────────────

    private fun listItemToMarkdown(
        item: JsonObject,
        prefix: String,
        isOffprint: Boolean,
        authorDid: String,
        schema: FacetDefinition,
        lost: MutableSet<String>,
        ordered: Boolean,
        number: Int? = null,
        isTask: Boolean = false,
    ): String? {
        val checked = item["checked"]?.jsonPrimitive?.contentOrNull?.lowercase()

        val contentEl = item["content"]
        var text = ""
        val nestedFromContent = mutableListOf<String>()

        when (contentEl) {
            is JsonArray -> {
                for (el in contentEl) {
                    val obj = el as? JsonObject ?: continue
                    val blockType = obj["\$type"]?.jsonPrimitive?.contentOrNull ?: continue
                    when (blockType) {
                        "${prefix}text" -> {
                            text += facetsToMarkdown(
                                obj["plaintext"]?.jsonPrimitive?.contentOrNull ?: "",
                                obj["facets"] as? JsonArray,
                                schema,
                                lost,
                            )
                        }
                        "${prefix}image" -> {
                            val url = resolveImageUrl(obj, isOffprint, authorDid)
                            val alt = obj["alt"]?.jsonPrimitive?.contentOrNull ?: ""
                            if (url.isNotEmpty()) text += "![$alt]($url)"
                        }
                        "${prefix}bulletList", "${prefix}orderedList", "${prefix}taskList" -> {
                            val subItems = obj["content"] as? JsonArray ?: continue
                            val isSubOrdered = blockType == "${prefix}orderedList"
                            val isSubTask = blockType == "${prefix}taskList"
                            val subStart = if (isSubOrdered) obj["start"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 1 else 1
                            for ((idx, subItem) in subItems.withIndex()) {
                                val subObj = subItem as? JsonObject ?: continue
                                val rendered = listItemToMarkdown(
                                    subObj, prefix, isOffprint, authorDid, schema, lost,
                                    ordered = isSubOrdered,
                                    number = if (isSubOrdered) subStart + idx else null,
                                    isTask = isSubTask,
                                )
                                if (rendered != null) nestedFromContent.add(rendered)
                            }
                        }
                        else -> {
                            val label = schema.lossy[blockType]
                            if (label != null) lost.add(label) else lost.add("unsupported content")
                        }
                    }
                }
            }
            is JsonObject -> {
                text = facetsToMarkdown(
                    contentEl["plaintext"]?.jsonPrimitive?.contentOrNull ?: "",
                    contentEl["facets"] as? JsonArray,
                    schema,
                    lost,
                )
            }
            else -> {}
        }

        val prefixStr = when {
            isTask -> {
                val check = if (checked == "true" || checked == "x") "[x]" else "[ ]"
                "$check "
            }
            ordered -> "${number ?: 1}. "
            else -> "- "
        }

        val children = item["children"] as? JsonArray
        val nestedFromChildren = if (children != null && children.isNotEmpty()) {
            children.mapNotNull { el ->
                val obj = el as? JsonObject ?: return@mapNotNull null
                listItemToMarkdown(obj, prefix, isOffprint, authorDid, schema, lost, ordered = false)
            }
        } else emptyList()

        val allNested = nestedFromContent + nestedFromChildren
        val nestedMarkdown = if (allNested.isNotEmpty()) "\n${allNested.joinToString("\n").prependIndent("  ")}" else ""

        return "$prefixStr$text$nestedMarkdown"
    }

    // ── Facet to Markdown ─────────────────────────────────────────────────

    private fun facetsToMarkdown(
        plaintext: String,
        facets: JsonArray?,
        schema: FacetDefinition,
        lost: MutableSet<String>,
    ): String {
        if (facets == null || facets.isEmpty()) return plaintext

        val sharedFacets = facets.mapNotNull { facet ->
            val obj = facet as? JsonObject ?: return@mapNotNull null
            val index = obj["index"] as? JsonObject ?: return@mapNotNull null
            val byteStart = index["byteStart"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: return@mapNotNull null
            val byteEnd = index["byteEnd"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: return@mapNotNull null
            val features = (obj["features"] as? JsonArray)?.mapNotNull { feature ->
                val featureObj = feature as? JsonObject ?: return@mapNotNull null
                val featureType = featureObj["\$type"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                RichTextFeature(type = featureType, uri = featureObj["uri"]?.jsonPrimitive?.contentOrNull)
            } ?: return@mapNotNull null

            RichTextFacet(byteStart = byteStart, byteEnd = byteEnd, features = features)
        }

        return FacetConverter.facetsToMarkdown(
            plaintext = plaintext,
            facets = sharedFacets,
            boldType = schema.bold,
            italicType = schema.italic,
            codeType = schema.code,
            strikeType = schema.strike,
            linkType = schema.link,
            lossy = schema.lossy,
            lost = lost,
        )
    }
}

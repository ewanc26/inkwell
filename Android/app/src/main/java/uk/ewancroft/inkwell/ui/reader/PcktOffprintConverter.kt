package uk.ewancroft.inkwell.ui.reader

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Converts pckt (blog.pckt.content) and Offprint (app.offprint.content)
 * block-array records into markdown strings. Works from raw JSON to avoid
 * kotlinx.serialization type-mismatch issues with the polymorphic `content`
 * field (blocks vs list items).
 *
 * Mirrors iOS PcktProvider.toMarkdown / OffprintProvider.toMarkdown.
 */
object PcktOffprintConverter {

    /**
     * Returns true if the content object is a pckt or Offprint record.
     */
    fun isSupported(formatType: String?): Boolean {
        return formatType == "blog.pckt.content" || formatType == "app.offprint.content"
    }

    /**
     * Converts a pckt or Offprint content JSON object to a markdown string.
     * Returns null if conversion fails.
     */
    fun toMarkdown(contentObj: JsonObject, formatType: String, authorDid: String = ""): String? {
        val items = contentObj["items"] as? JsonArray ?: return null
        val prefix = if (formatType == "blog.pckt.content") "blog.pckt.block." else "app.offprint.block."
        val isOffprint = formatType == "app.offprint.content"

        val blocks = mutableListOf<String>()
        for (item in items) {
            val obj = item as? JsonObject ?: continue
            val md = blockToMarkdown(obj, prefix, isOffprint, authorDid)
            if (md != null) blocks.add(md)
        }
        return blocks.joinToString("\n\n")
    }

    private fun blockToMarkdown(block: JsonObject, prefix: String, isOffprint: Boolean, authorDid: String): String? {
        val type = block["\$type"]?.jsonPrimitive?.contentOrNull ?: return null

        return when (type) {
            "${prefix}text" -> {
                val text = facetsToMarkdown(
                    block["plaintext"]?.jsonPrimitive?.contentOrNull ?: "",
                    block["facets"] as? JsonArray,
                    prefix,
                )
                text.ifEmpty { null }
            }

            "${prefix}heading" -> {
                val level = (block["level"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 1).coerceIn(1, 6)
                val text = facetsToMarkdown(
                    block["plaintext"]?.jsonPrimitive?.contentOrNull ?: "",
                    block["facets"] as? JsonArray,
                    prefix,
                )
                val hashes = "#".repeat(level)
                "$hashes $text"
            }

            "${prefix}blockquote" -> {
                val inner = block["content"] as? JsonArray ?: return null
                val text = inner.mapNotNull { el ->
                    val obj = el as? JsonObject ?: return@mapNotNull null
                    facetsToMarkdown(
                        obj["plaintext"]?.jsonPrimitive?.contentOrNull ?: "",
                        obj["facets"] as? JsonArray,
                        prefix,
                    )
                }.joinToString("\n")
                if (text.isNotEmpty()) "> ${text.replace("\n", "\n> ")}" else null
            }

            "${prefix}codeBlock" -> {
                val lang = block["language"]?.jsonPrimitive?.contentOrNull ?: ""
                val content = block["plaintext"]?.jsonPrimitive?.contentOrNull ?: ""
                "```$lang\n$content\n```"
            }

            "${prefix}mathBlock" -> {
                val content = block["plaintext"]?.jsonPrimitive?.contentOrNull ?: ""
                "```\nmath\n$content\n```"
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
                    listItemToMarkdown(obj, prefix, isOffprint, ordered = false)
                }
                if (rendered.isNotEmpty()) rendered.joinToString("\n") else null
            }

            "${prefix}orderedList" -> {
                val start = block["start"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 1
                val items = block["content"] as? JsonArray ?: return null
                val rendered = items.mapIndexedNotNull { index, el ->
                    val obj = el as? JsonObject ?: return@mapIndexedNotNull null
                    listItemToMarkdown(obj, prefix, isOffprint, ordered = true, number = start + index)
                }
                if (rendered.isNotEmpty()) rendered.joinToString("\n") else null
            }

            "${prefix}taskList" -> {
                val items = block["content"] as? JsonArray ?: return null
                val rendered = items.mapNotNull { el ->
                    val obj = el as? JsonObject ?: return@mapNotNull null
                    listItemToMarkdown(obj, prefix, isOffprint, ordered = false, isTask = true)
                }
                if (rendered.isNotEmpty()) rendered.joinToString("\n") else null
            }

            else -> null
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

        // Already a full URL
        if (ref.startsWith("http://") || ref.startsWith("https://")) return ref

        // Bare CID — build CDN URL (matches Leaflet ImageBlock pattern)
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
                // pckt: content is an array of blocks
                for (el in contentEl) {
                    val obj = el as? JsonObject ?: continue
                    val blockType = obj["\$type"]?.jsonPrimitive?.contentOrNull ?: continue
                    when (blockType) {
                        "${prefix}text" -> {
                            text += facetsToMarkdown(
                                obj["plaintext"]?.jsonPrimitive?.contentOrNull ?: "",
                                obj["facets"] as? JsonArray,
                                prefix,
                            )
                        }
                        "${prefix}bulletList", "${prefix}orderedList", "${prefix}taskList" -> {
                            // Nested sub-list inside a pckt list item
                            val subItems = obj["content"] as? JsonArray ?: continue
                            val isSubOrdered = blockType == "${prefix}orderedList"
                            val isSubTask = blockType == "${prefix}taskList"
                            val subStart = if (isSubOrdered) obj["start"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 1 else 1
                            for ((idx, subItem) in subItems.withIndex()) {
                                val subObj = subItem as? JsonObject ?: continue
                                val rendered = listItemToMarkdown(
                                    subObj, prefix, isOffprint,
                                    ordered = isSubOrdered,
                                    number = if (isSubOrdered) subStart + idx else null,
                                    isTask = isSubTask,
                                )
                                if (rendered != null) nestedFromContent.add(rendered)
                            }
                        }
                    }
                }
            }
            is JsonObject -> {
                // Offprint: content is a single block
                text = facetsToMarkdown(
                    contentEl["plaintext"]?.jsonPrimitive?.contentOrNull ?: "",
                    contentEl["facets"] as? JsonArray,
                    prefix,
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

        // Handle nested sub-lists (Offprint uses `children`, pckt uses content array above)
        val children = item["children"] as? JsonArray
        val nestedFromChildren = if (children != null && children.isNotEmpty()) {
            children.mapNotNull { el ->
                val obj = el as? JsonObject ?: return@mapNotNull null
                listItemToMarkdown(obj, prefix, isOffprint, ordered = false)
            }
        } else emptyList()

        val allNested = nestedFromContent + nestedFromChildren
        val nestedMarkdown = if (allNested.isNotEmpty()) "\n${allNested.joinToString("\n").prependIndent("  ")}" else ""

        return "$prefixStr$text$nestedMarkdown"
    }

    // ── Facet to Markdown ─────────────────────────────────────────────────

    /**
     * Converts facets (byte-range inline formatting) to markdown inline syntax.
     * Mirrors iOS FacetConverter.facetsToMarkdown.
     */
    private fun facetsToMarkdown(plaintext: String, facets: JsonArray?, prefix: String): String {
        if (facets == null || facets.isEmpty()) return plaintext

        val utf8Bytes = plaintext.toByteArray(Charsets.UTF_8)
        val totalBytes = utf8Bytes.size

        // Collect all byte boundaries where active marks may change
        val boundaries = mutableSetOf(0, totalBytes)
        for (facet in facets) {
            val obj = facet as? JsonObject ?: continue
            val index = obj["index"] as? JsonObject ?: continue
            index["byteStart"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()?.let { boundaries.add(it) }
            index["byteEnd"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()?.let { boundaries.add(it) }
        }
        val sortedBounds = boundaries.sorted()

        data class Segment(val text: String, val bold: Boolean, val italic: Boolean, val code: Boolean, val strike: Boolean, val link: String?)

        val segments = mutableListOf<Segment>()
        for (idx in 0 until sortedBounds.size - 1) {
            val start = sortedBounds[idx]
            val end = sortedBounds[idx + 1]
            if (start >= end || start >= totalBytes) continue
            val clampedEnd = minOf(end, totalBytes)

            val text = String(utf8Bytes, start, clampedEnd - start, Charsets.UTF_8)
            if (text.isEmpty()) continue

            var bold = false; var italic = false; var code = false; var strike = false; var link: String? = null
            for (facet in facets) {
                val obj = facet as? JsonObject ?: continue
                val index = obj["index"] as? JsonObject ?: continue
                val byteStart = index["byteStart"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: continue
                val byteEnd = index["byteEnd"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: continue
                if (start >= byteStart && start < byteEnd) {
                    val features = obj["features"] as? JsonArray ?: continue
                    for (feature in features) {
                        val featureObj = feature as? JsonObject ?: continue
                        val featureType = featureObj["\$type"]?.jsonPrimitive?.contentOrNull ?: continue
                        when (featureType) {
                            "${prefix}bold" -> bold = true
                            "${prefix}italic" -> italic = true
                            "${prefix}code" -> code = true
                            "${prefix}strikethrough" -> strike = true
                            "${prefix}link" -> link = featureObj["uri"]?.jsonPrimitive?.contentOrNull
                        }
                    }
                }
            }

            val seg = Segment(text, bold, italic, code, strike, link)
            val last = segments.lastOrNull()
            if (last != null && last.bold == seg.bold && last.italic == seg.italic &&
                last.code == seg.code && last.strike == seg.strike && last.link == seg.link
            ) {
                segments[segments.size - 1] = last.copy(text = last.text + seg.text)
            } else {
                segments.add(seg)
            }
        }

        // Build markdown from segments
        return segments.joinToString("") { seg ->
            var wrapped = seg.text
            if (seg.code) {
                wrapped = "`$wrapped`"
            } else {
                if (seg.strike) wrapped = "~~$wrapped~~"
                if (seg.italic) wrapped = "*$wrapped*"
                if (seg.bold) wrapped = "**$wrapped**"
            }
            if (seg.link != null) {
                wrapped = "[$wrapped](${seg.link})"
            }
            wrapped
        }
    }
}

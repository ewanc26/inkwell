package uk.ewancroft.inkwell.shared.content

/**
 * Blob-backed ("spilled") content representations for the formats that define one.
 *
 * AT Protocol keeps individual records small — Inkwell preflights every document
 * record against [uk.ewancroft.inkwell.shared.validation.RecordSizePolicy]. When an
 * authored document would not fit inline, the two formats that define an upstream
 * escape hatch move their payload into a PDS blob and reference it from the record:
 *
 * - Markpub: `content.text.textBlob` replaces `content.text.markdown`;
 * - Leaflet: `content.blobPages` replaces the inline `content.pages` array.
 *
 * pckt and Offprint define no such field. Inventing a private one would produce
 * records no other client could read, so those formats surface a Writer error
 * instead.
 *
 * This object owns only the shapes and the policy. Uploading the blob and writing
 * the record stay on the platform side, which owns the XRPC client.
 */
object BlobBackedContent {

    /** MIME type of the blob holding a spilled Markpub document's markdown source. */
    const val MARKPUB_BLOB_MIME: String = "text/markdown"

    /** MIME type of the blob holding a spilled Leaflet document's `pages` array. */
    const val LEAFLET_BLOB_MIME: String = "application/json"

    /**
     * UTF-8 byte budget for the record's portable `textContent` once content has
     * spilled to a blob. `textContent` is a search/indexing aid, so a truncated
     * copy stays useful while guaranteeing it cannot push the record back over
     * the record-size limit on its own.
     */
    const val MAX_SPILLED_TEXT_CONTENT_BYTES: Int = 64 * 1024

    /** Formats with an upstream-defined blob-backed representation. */
    fun supportsBlobBacking(format: String): Boolean =
        format == "Markpub" || format == "Leaflet"

    /** MIME type for [format]'s backing blob, or null when the format defines none. */
    fun blobMimeType(format: String): String? = when (format) {
        "Markpub" -> MARKPUB_BLOB_MIME
        "Leaflet" -> LEAFLET_BLOB_MIME
        else -> null
    }

    /** Writer-facing explanation for a format that cannot represent the document within limits. */
    fun unsupportedFormatMessage(format: String, encodedBytes: Int, limitBytes: Int): String =
        "This document is too large to publish as $format ($encodedBytes bytes; the limit is " +
            "$limitBytes bytes). $format has no blob-backed representation for long documents, " +
            "so shorten the document or switch it to Markpub or Leaflet."

    /** Markpub content whose markdown lives in `text.textBlob` instead of `text.markdown`. */
    fun markpubBlobContent(textBlob: Map<String, Any?>): Map<String, Any?> = mapOf(
        "\$type" to MarkpubTypes.CONTENT,
        "text" to mapOf(
            "\$type" to MarkpubTypes.TEXT,
            "textBlob" to textBlob,
        ),
    )

    /** Leaflet content whose pages live in `blobPages` instead of the inline `pages` array. */
    fun leafletBlobContent(blobPages: Map<String, Any?>): Map<String, Any?> = mapOf(
        "\$type" to LeafletTypes.CONTENT,
        "blobPages" to blobPages,
    )

    /** Leaflet content rebuilt inline from pages that were resolved out of `blobPages`. */
    fun leafletInlineContent(pages: List<Any?>): Map<String, Any?> = mapOf(
        "\$type" to LeafletTypes.CONTENT,
        "pages" to pages,
    )

    /** Markpub content rebuilt inline from markdown that was resolved out of `text.textBlob`. */
    fun markpubInlineContent(markdown: String): Map<String, Any?> = mapOf(
        "\$type" to MarkpubTypes.CONTENT,
        "text" to mapOf(
            "\$type" to MarkpubTypes.TEXT,
            "markdown" to markdown,
        ),
    )

    /** The `pages` array that goes into a Leaflet document's backing blob. */
    fun leafletPages(content: Map<String, Any?>): List<Any?> =
        content["pages"] as? List<Any?> ?: emptyList()

    /** True when [content] already references its payload through a blob. */
    fun isBlobBacked(content: Map<String, Any?>): Boolean = when (content["\$type"]) {
        MarkpubTypes.CONTENT -> (content["text"] as? Map<*, *>)?.get("textBlob") != null
        LeafletTypes.CONTENT -> content["blobPages"] != null
        else -> false
    }

    /**
     * Truncates `textContent` to a UTF-8 byte budget without splitting a character
     * or a surrogate pair. Returns [text] unchanged when it already fits.
     */
    fun truncateTextContent(text: String, maxBytes: Int = MAX_SPILLED_TEXT_CONTENT_BYTES): String {
        if (maxBytes <= 0) return ""
        if (text.encodeToByteArray().size <= maxBytes) return text

        var low = 0
        var high = text.length
        while (low < high) {
            val mid = (low + high + 1) / 2
            if (text.substring(0, mid).encodeToByteArray().size <= maxBytes) low = mid else high = mid - 1
        }
        var end = low
        if (end > 0 && text[end - 1].isHighSurrogate()) end--
        return text.substring(0, end)
    }
}

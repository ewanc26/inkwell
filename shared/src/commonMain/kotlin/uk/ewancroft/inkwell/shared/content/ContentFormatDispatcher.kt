package uk.ewancroft.inkwell.shared.content

import uk.ewancroft.inkwell.shared.markdown.MarkdownParser

/**
 * Dispatches content conversion to the correct format-specific converter.
 *
 * This is the main entry point for shared content conversion. Both platforms
 * call these functions instead of maintaining their own conversion logic.
 *
 * Mirrors iOS `ProviderRegistry` and Android `MarkdownConverter.convert`.
 */
object ContentFormatDispatcher {

    /**
     * Converts a content map to markdown, detecting the format from `$type`.
     *
     * @param content The content map with a `$type` field.
     * @param authorDid The author's DID, used for CDN image URL resolution.
     * @return A [SharedConvertResult] with the markdown blocks and any lost content.
     */
    fun toMarkdown(content: Map<String, Any?>, authorDid: String = ""): SharedConvertResult {
        val type = content["\$type"] as? String ?: return SharedConvertResult(emptyList())

        return when (type) {
            ContentFormatDetector.LEAFLET -> LeafletContentConverter.toMarkdown(content)
            ContentFormatDetector.MARKPUB -> MarkpubContentConverter.toMarkdown(content)
            ContentFormatDetector.PCKT -> PcktContentConverter.toMarkdown(content)
            ContentFormatDetector.OFFPRINT -> OffprintContentConverter.toMarkdown(content)
            else -> SharedConvertResult(emptyList(), setOf("unknown content format"))
        }
    }

    /**
     * Converts a content map to a raw markdown string.
     *
     * @param content The content map with a `$type` field.
     * @param authorDid The author's DID, used for CDN image URL resolution.
     * @return The markdown string, or empty if conversion fails.
     */
    fun toMarkdownString(content: Map<String, Any?>, authorDid: String = ""): String {
        val result = toMarkdown(content, authorDid)
        return uk.ewancroft.inkwell.shared.markdown.MarkdownSerializer.serialize(result.blocks)
    }

    /**
     * Converts markdown text to a format-specific content map.
     *
     * @param markdown The markdown source text.
     * @param format The format identifier ("Leaflet", "Markpub", "pckt", "Offprint").
     * @param uploadedBlobs Map of CID → blob JSON for image round-tripping.
     * @return A [SharedWriteResult] with the content map and any lost content.
     */
    fun fromMarkdown(
        markdown: String,
        format: String,
        uploadedBlobs: Map<String, Map<String, Any?>> = emptyMap()
    ): SharedWriteResult {
        return when (format) {
            "Leaflet" -> LeafletContentConverter.fromMarkdown(markdown, uploadedBlobs)
            "Markpub" -> MarkpubContentConverter.fromMarkdown(markdown)
            "pckt" -> PcktContentConverter.fromMarkdown(markdown, uploadedBlobs)
            "Offprint" -> OffprintContentConverter.fromMarkdown(markdown, uploadedBlobs)
            else -> LeafletContentConverter.fromMarkdown(markdown, uploadedBlobs)
        }
    }

    /**
     * Returns true if the given content type is a pckt or Offprint format.
     */
    fun isPcktOrOffprint(type: String?): Boolean =
        ContentFormatDetector.isPcktOrOffprint(type)

    /**
     * Returns the content type string for a format name.
     */
    fun contentTypeForFormat(format: String): String? = when (format) {
        "Leaflet" -> ContentFormatDetector.LEAFLET
        "Markpub" -> ContentFormatDetector.MARKPUB
        "pckt" -> ContentFormatDetector.PCKT
        "Offprint" -> ContentFormatDetector.OFFPRINT
        else -> null
    }

    /**
     * Returns the format name for a content type string.
     */
    fun formatForContentType(type: String?): String? = when (type) {
        ContentFormatDetector.LEAFLET -> "Leaflet"
        ContentFormatDetector.MARKPUB -> "Markpub"
        ContentFormatDetector.PCKT -> "pckt"
        ContentFormatDetector.OFFPRINT -> "Offprint"
        else -> null
    }
}

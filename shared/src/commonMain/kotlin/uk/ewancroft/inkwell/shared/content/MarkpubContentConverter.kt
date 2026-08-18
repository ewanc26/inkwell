package uk.ewancroft.inkwell.shared.content

import uk.ewancroft.inkwell.shared.markdown.MarkdownBlock
import uk.ewancroft.inkwell.shared.markdown.MarkdownParser
import uk.ewancroft.inkwell.shared.markdown.MarkdownSerializer

/**
 * Converts between Markpub content (`at.markpub.markdown`) and markdown blocks.
 *
 * Markpub stores GFM markdown directly, so conversion is near-identity:
 * read the inline `text.markdown` and write it straight back. Nothing is
 * ever lost.
 *
 * Mirrors iOS `MarkpubProvider` and Android `MarkdownConverter.buildMarkpubContent`.
 */
object MarkpubContentConverter {

    // ── Read: Markpub JSON → Markdown ──────────────────────────────────

    /**
     * Converts a Markpub content map to a [SharedConvertResult].
     *
     * The content map is expected to have the shape:
     * ```
     * { "$type": "at.markpub.markdown", "text": { "$type": "at.markpub.text", "markdown": "..." } }
     * ```
     */
    fun toMarkdown(content: Map<String, Any?>): SharedConvertResult {
        val text = content["text"] as? Map<*, *> ?: return SharedConvertResult(emptyList())
        val markdown = text["markdown"] as? String ?: return SharedConvertResult(emptyList())
        val blocks = MarkdownParser.parse(markdown)
        return SharedConvertResult(blocks)
    }

    /**
     * Extracts the raw markdown string from a Markpub content map.
     * Use this when you need the markdown text directly without block parsing.
     */
    fun toRawMarkdown(content: Map<String, Any?>): String {
        val text = content["text"] as? Map<*, *> ?: return ""
        return text["markdown"] as? String ?: ""
    }

    // ── Write: Markdown → Markpub JSON ─────────────────────────────────

    /**
     * Converts markdown text to a Markpub content map.
     *
     * @param markdown The markdown source text.
     */
    fun fromMarkdown(markdown: String): SharedWriteResult {
        val content = mapOf(
            "\$type" to MarkpubTypes.CONTENT,
            "text" to mapOf(
                "\$type" to MarkpubTypes.TEXT,
                "markdown" to markdown
            )
        )
        return SharedWriteResult(content)
    }
}

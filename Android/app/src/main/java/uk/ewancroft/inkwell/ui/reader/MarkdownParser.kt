package uk.ewancroft.inkwell.ui.reader

import uk.ewancroft.inkwell.shared.markdown.MarkdownParser as SharedMarkdownParser
import uk.ewancroft.inkwell.shared.markdown.MarkdownBlock as SharedMarkdownBlock
import uk.ewancroft.inkwell.shared.markdown.MarkdownListItem as SharedMarkdownListItem

/**
 * Delegates to the shared KMP [SharedMarkdownParser]. Kept in this package
 * so existing callers (MarkdownRendererView, etc.) resolve the types
 * without import changes.
 */
object MarkdownParser {

    fun parse(markdown: String): List<MarkdownBlock> =
        SharedMarkdownParser.parse(markdown).map { it.toLocal() }
}

/**
 * Local MarkdownBlock sealed class — delegates to the shared [SharedMarkdownBlock]
 * for parsing, then maps back for the existing when-expressions in this package.
 */
sealed class MarkdownBlock {

    data class Heading(val level: Int, val text: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
    data class Code(val language: String?, val content: String) : MarkdownBlock()
    data class Math(val tex: String) : MarkdownBlock()
    data class Blockquote(val text: String) : MarkdownBlock()
    data class Image(val alt: String, val url: String) : MarkdownBlock()
    data object HorizontalRule : MarkdownBlock()
    data class UnorderedList(val items: List<MarkdownListItem>) : MarkdownBlock()
    data class OrderedList(val start: Int, val items: List<MarkdownListItem>) : MarkdownBlock()
    data class TaskList(val items: List<MarkdownListItem>) : MarkdownBlock()
}

data class MarkdownListItem(
    val text: String,
    val checked: Boolean? = null,
    val children: List<MarkdownListItem>? = null,
)

// ── Shared → Local mapping ──────────────────────────────────────────────

private fun SharedMarkdownBlock.toLocal(): MarkdownBlock = when (this) {
    is SharedMarkdownBlock.Heading -> MarkdownBlock.Heading(level, text)
    is SharedMarkdownBlock.Paragraph -> MarkdownBlock.Paragraph(text)
    is SharedMarkdownBlock.Code -> MarkdownBlock.Code(language, content)
    is SharedMarkdownBlock.Math -> MarkdownBlock.Math(tex)
    is SharedMarkdownBlock.Blockquote -> MarkdownBlock.Blockquote(text)
    is SharedMarkdownBlock.Image -> MarkdownBlock.Image(alt, url)
    SharedMarkdownBlock.HorizontalRule -> MarkdownBlock.HorizontalRule
    is SharedMarkdownBlock.UnorderedList -> MarkdownBlock.UnorderedList(items.map { it.toLocal() })
    is SharedMarkdownBlock.OrderedList -> MarkdownBlock.OrderedList(start, items.map { it.toLocal() })
    is SharedMarkdownBlock.TaskList -> MarkdownBlock.TaskList(items.map { it.toLocal() })
}

private fun SharedMarkdownListItem.toLocal(): MarkdownListItem =
    MarkdownListItem(text, checked, children?.map { it.toLocal() })

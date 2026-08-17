package uk.ewancroft.inkwell.shared.markdown

/**
 * A markdown block produced by [MarkdownParser.parse].
 *
 * Mirrors iOS `MarkdownBlock` enum in ContentProvider.swift and Android
 * `MarkdownBlock` sealed class in MarkdownParser.kt.
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

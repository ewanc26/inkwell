package uk.ewancroft.inkwell.shared.markdown

/**
 * Converts [MarkdownBlock] arrays back to markdown strings.
 *
 * Mirrors iOS `MarkdownSerializer` in ContentProvider.swift. Android's
 * `MarkdownConverter` inlines this logic per format; the shared serializer
 * provides a single canonical round-trip.
 */
object MarkdownSerializer {

    fun serialize(blocks: List<MarkdownBlock>): String =
        blocks.joinToString("\n\n") { blockToString(it) }

    private fun blockToString(block: MarkdownBlock): String = when (block) {
        is MarkdownBlock.Heading -> "#".repeat(block.level) + " " + block.text

        is MarkdownBlock.Paragraph -> block.text

        is MarkdownBlock.Code -> {
            val lang = block.language ?: ""
            "```$lang\n${block.content}\n```"
        }

        is MarkdownBlock.Math -> "```math\n${block.tex}\n```"

        is MarkdownBlock.Blockquote -> block.text.lines().joinToString("\n") { "> $it" }

        is MarkdownBlock.Image -> "![${block.alt}](${block.url})"

        is MarkdownBlock.HorizontalRule -> "---"

        is MarkdownBlock.UnorderedList -> block.items.joinToString("\n") { listItemToString(it, "- ") }

        is MarkdownBlock.OrderedList -> block.items.mapIndexed { idx, item ->
            listItemToString(item, "${block.start + idx}. ")
        }.joinToString("\n")

        is MarkdownBlock.TaskList -> block.items.map { item ->
            val checkbox = when {
                item.checked == true -> "[x] "
                item.checked == false -> "[ ] "
                else -> ""
            }
            "- $checkbox${item.text}"
        }.joinToString("\n")
    }

    private fun listItemToString(item: MarkdownListItem, prefix: String): String {
        val childrenLines = item.children?.map { child ->
            listItemToString(child, "  - ")
        }?.joinToString("\n")
        return if (childrenLines != null) {
            prefix + item.text + "\n" + childrenLines
        } else {
            prefix + item.text
        }
    }
}

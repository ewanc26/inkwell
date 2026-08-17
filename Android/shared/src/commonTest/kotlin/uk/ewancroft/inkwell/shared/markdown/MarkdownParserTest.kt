package uk.ewancroft.inkwell.shared.markdown

import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownParserTest {

    // ── Headings ────────────────────────────────────────────────────────

    @Test
    fun parseH1Heading() {
        val blocks = MarkdownParser.parse("# Hello World")
        assertEquals(1, blocks.size)
        assertEquals(MarkdownBlock.Heading(1, "Hello World"), blocks[0])
    }

    @Test
    fun parseH2ThroughH6() {
        val markdown = "## H2\n### H3\n#### H4\n##### H5\n###### H6"
        val blocks = MarkdownParser.parse(markdown)
        assertEquals(5, blocks.size)
        assertEquals(2, (blocks[0] as MarkdownBlock.Heading).level)
        assertEquals(6, (blocks[4] as MarkdownBlock.Heading).level)
    }

    // ── Paragraphs ─────────────────────────────────────────────────────

    @Test
    fun parseSingleParagraph() {
        val blocks = MarkdownParser.parse("Hello world")
        assertEquals(1, blocks.size)
        assertEquals(MarkdownBlock.Paragraph("Hello world"), blocks[0])
    }

    @Test
    fun parseMultipleParagraphs() {
        val blocks = MarkdownParser.parse("First para\n\nSecond para")
        assertEquals(2, blocks.size)
        assertEquals("First para", (blocks[0] as MarkdownBlock.Paragraph).text)
        assertEquals("Second para", (blocks[1] as MarkdownBlock.Paragraph).text)
    }

    // ── Code blocks ────────────────────────────────────────────────────

    @Test
    fun parseFencedCodeBlock() {
        val blocks = MarkdownParser.parse("```kotlin\nval x = 1\n```")
        assertEquals(1, blocks.size)
        val code = blocks[0] as MarkdownBlock.Code
        assertEquals("kotlin", code.language)
        assertEquals("val x = 1", code.content)
    }

    @Test
    fun parseMathBlock() {
        val blocks = MarkdownParser.parse("```math\nE = mc^2\n```")
        assertEquals(1, blocks.size)
        assertEquals(MarkdownBlock.Math("E = mc^2"), blocks[0])
    }

    // ── Blockquotes ────────────────────────────────────────────────────

    @Test
    fun parseBlockquote() {
        val blocks = MarkdownParser.parse("> Hello\n> World")
        assertEquals(1, blocks.size)
        assertEquals(MarkdownBlock.Blockquote("Hello\nWorld"), blocks[0])
    }

    // ── Images ─────────────────────────────────────────────────────────

    @Test
    fun parseImage() {
        val blocks = MarkdownParser.parse("![alt text](https://example.com/img.png)")
        assertEquals(1, blocks.size)
        val img = blocks[0] as MarkdownBlock.Image
        assertEquals("alt text", img.alt)
        assertEquals("https://example.com/img.png", img.url)
    }

    // ── Horizontal rule ────────────────────────────────────────────────

    @Test
    fun parseHorizontalRule() {
        assertEquals(listOf(MarkdownBlock.HorizontalRule), MarkdownParser.parse("---"))
        assertEquals(listOf(MarkdownBlock.HorizontalRule), MarkdownParser.parse("***"))
        assertEquals(listOf(MarkdownBlock.HorizontalRule), MarkdownParser.parse("___"))
    }

    // ── Unordered lists ────────────────────────────────────────────────

    @Test
    fun parseUnorderedList() {
        val blocks = MarkdownParser.parse("- Apple\n- Banana\n- Cherry")
        assertEquals(1, blocks.size)
        val list = blocks[0] as MarkdownBlock.UnorderedList
        assertEquals(3, list.items.size)
        assertEquals("Apple", list.items[0].text)
        assertEquals("Banana", list.items[1].text)
    }

    @Test
    fun parseNestedUnorderedList() {
        val blocks = MarkdownParser.parse("- Parent\n  - Child\n  - Child2\n- Parent2")
        assertEquals(1, blocks.size)
        val list = blocks[0] as MarkdownBlock.UnorderedList
        assertEquals(2, list.items.size)
        assertEquals(2, list.items[0].children!!.size)
        assertEquals("Child", list.items[0].children!![0].text)
    }

    // ── Ordered lists ──────────────────────────────────────────────────

    @Test
    fun parseOrderedList() {
        val blocks = MarkdownParser.parse("1. First\n2. Second\n3. Third")
        assertEquals(1, blocks.size)
        val list = blocks[0] as MarkdownBlock.OrderedList
        assertEquals(1, list.start)
        assertEquals(3, list.items.size)
        assertEquals("First", list.items[0].text)
    }

    // ── Task lists ─────────────────────────────────────────────────────

    @Test
    fun parseTaskList() {
        val blocks = MarkdownParser.parse("- [x] Done\n- [ ] Todo")
        assertEquals(1, blocks.size)
        val list = blocks[0] as MarkdownBlock.TaskList
        assertEquals(true, list.items[0].checked)
        assertEquals(false, list.items[1].checked)
        assertEquals("Done", list.items[0].text)
    }

    @Test
    fun parseMixedListWithTaskAndPlain() {
        val blocks = MarkdownParser.parse("- [x] Done\n- Plain item")
        assertEquals(1, blocks.size)
        assertEquals(MarkdownBlock.UnorderedList::class, blocks[0]::class)
        val list = blocks[0] as MarkdownBlock.UnorderedList
        assertEquals(true, list.items[0].checked)
        assertEquals(null, list.items[1].checked)
    }

    // ── Round-trip: parse → serialize → parse ──────────────────────────

    @Test
    fun roundTripHeading() {
        val original = "# Hello"
        assertEquals(original, MarkdownSerializer.serialize(MarkdownParser.parse(original)))
    }

    @Test
    fun roundTripParagraph() {
        val original = "Hello world"
        assertEquals(original, MarkdownSerializer.serialize(MarkdownParser.parse(original)))
    }

    @Test
    fun roundTripUnorderedList() {
        val original = "- Apple\n- Banana"
        assertEquals(original, MarkdownSerializer.serialize(MarkdownParser.parse(original)))
    }

    @Test
    fun roundTripOrderedList() {
        val original = "1. First\n2. Second"
        assertEquals(original, MarkdownSerializer.serialize(MarkdownParser.parse(original)))
    }

    @Test
    fun roundTripTaskList() {
        val original = "- [x] Done\n- [ ] Todo"
        assertEquals(original, MarkdownSerializer.serialize(MarkdownParser.parse(original)))
    }

    @Test
    fun roundTripCodeBlock() {
        val original = "```kotlin\nval x = 1\n```"
        assertEquals(original, MarkdownSerializer.serialize(MarkdownParser.parse(original)))
    }

    @Test
    fun roundTripMixedBlocks() {
        val original = "# Title\n\nParagraph text\n\n- Item 1\n- Item 2\n\n> Quote"
        val reparsed = MarkdownParser.parse(original)
        val serialized = MarkdownSerializer.serialize(reparsed)
        val reparsed2 = MarkdownParser.parse(serialized)
        assertEquals(reparsed, reparsed2)
    }

    @Test
    fun roundTripNestedList() {
        val original = "- Parent\n  - Child\n  - Child2"
        val reparsed = MarkdownParser.parse(original)
        val serialized = MarkdownSerializer.serialize(reparsed)
        val reparsed2 = MarkdownParser.parse(serialized)
        assertEquals(reparsed, reparsed2)
    }
}

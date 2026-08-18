package uk.ewancroft.inkwell.shared.content

import uk.ewancroft.inkwell.shared.markdown.MarkdownBlock
import uk.ewancroft.inkwell.shared.markdown.MarkdownSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class ContentConverterTest {

    // ── Leaflet Round-Trip ──────────────────────────────────────────────

    @Test
    fun leafletHeadingRoundTrip() {
        val markdown = "# Hello World"
        val writeResult = LeafletContentConverter.fromMarkdown(markdown)
        val content = writeResult.content
        assertEquals(LeafletTypes.CONTENT, content["\$type"])

        val readResult = LeafletContentConverter.toMarkdown(content)
        assertEquals(1, readResult.blocks.size)
        val block = readResult.blocks[0] as MarkdownBlock.Heading
        assertEquals(1, block.level)
        assertEquals("Hello World", block.text)
    }

    @Test
    fun leafletParagraphRoundTrip() {
        val markdown = "Hello world"
        val writeResult = LeafletContentConverter.fromMarkdown(markdown)
        val readResult = LeafletContentConverter.toMarkdown(writeResult.content)
        assertEquals(1, readResult.blocks.size)
        val block = readResult.blocks[0] as MarkdownBlock.Paragraph
        assertEquals("Hello world", block.text)
    }

    @Test
    fun leafletCodeBlockRoundTrip() {
        val markdown = "```kotlin\nval x = 1\n```"
        val writeResult = LeafletContentConverter.fromMarkdown(markdown)
        val readResult = LeafletContentConverter.toMarkdown(writeResult.content)
        assertEquals(1, readResult.blocks.size)
        val block = readResult.blocks[0] as MarkdownBlock.Code
        assertEquals("kotlin", block.language)
        assertEquals("val x = 1", block.content)
    }

    @Test
    fun leafletBlockquoteRoundTrip() {
        val markdown = "> This is a quote"
        val writeResult = LeafletContentConverter.fromMarkdown(markdown)
        val readResult = LeafletContentConverter.toMarkdown(writeResult.content)
        assertEquals(1, readResult.blocks.size)
        val block = readResult.blocks[0] as MarkdownBlock.Blockquote
        assertEquals("This is a quote", block.text)
    }

    @Test
    fun leafletHorizontalRuleRoundTrip() {
        val markdown = "---"
        val writeResult = LeafletContentConverter.fromMarkdown(markdown)
        val readResult = LeafletContentConverter.toMarkdown(writeResult.content)
        assertEquals(1, readResult.blocks.size)
        assertTrue(readResult.blocks[0] is MarkdownBlock.HorizontalRule)
    }

    @Test
    fun leafletUnorderedListRoundTrip() {
        val markdown = "- Item 1\n- Item 2\n- Item 3"
        val writeResult = LeafletContentConverter.fromMarkdown(markdown)
        val readResult = LeafletContentConverter.toMarkdown(writeOrderedList(writeResult.content))
        // Just verify it produces blocks without crashing
        assertTrue(readResult.blocks.isNotEmpty())
    }

    // ── Markpub Round-Trip ──────────────────────────────────────────────

    @Test
    fun markpubRoundTrip() {
        val markdown = "# Title\n\nSome content\n\n---"
        val writeResult = MarkpubContentConverter.fromMarkdown(markdown)
        val content = writeResult.content
        assertEquals(MarkpubTypes.CONTENT, content["\$type"])

        val readResult = MarkpubContentConverter.toMarkdown(content)
        assertEquals(3, readResult.blocks.size)
        assertTrue(readResult.lost.isEmpty())
    }

    @Test
    fun markpubPreservesRawMarkdown() {
        val markdown = "Hello **world**"
        val writeResult = MarkpubContentConverter.fromMarkdown(markdown)
        val raw = MarkpubContentConverter.toRawMarkdown(writeResult.content)
        assertEquals("Hello **world**", raw)
    }

    // ── pckt Round-Trip ─────────────────────────────────────────────────

    @Test
    fun pcktHeadingRoundTrip() {
        val markdown = "# Hello World"
        val writeResult = PcktContentConverter.fromMarkdown(markdown)
        val content = writeResult.content
        assertEquals(PcktTypes.CONTENT, content["\$type"])

        val readResult = PcktContentConverter.toMarkdown(content)
        assertTrue(readResult.blocks.isNotEmpty())
        val block = readResult.blocks[0] as MarkdownBlock.Heading
        assertEquals(1, block.level)
        assertEquals("Hello World", block.text)
    }

    @Test
    fun pcktParagraphRoundTrip() {
        val markdown = "Hello world"
        val writeResult = PcktContentConverter.fromMarkdown(markdown)
        val readResult = PcktContentConverter.toMarkdown(writeResult.content)
        assertEquals(1, readResult.blocks.size)
        val block = readResult.blocks[0] as MarkdownBlock.Paragraph
        assertEquals("Hello world", block.text)
    }

    @Test
    fun pcktCodeBlockRoundTrip() {
        val markdown = "```kotlin\nval x = 1\n```"
        val writeResult = PcktContentConverter.fromMarkdown(markdown)
        val readResult = PcktContentConverter.toMarkdown(writeResult.content)
        assertEquals(1, readResult.blocks.size)
        val block = readResult.blocks[0] as MarkdownBlock.Code
        assertEquals("kotlin", block.language)
        assertEquals("val x = 1", block.content)
    }

    @Test
    fun pcktBlockquoteRoundTrip() {
        val markdown = "> This is a quote"
        val writeResult = PcktContentConverter.fromMarkdown(markdown)
        val readResult = PcktContentConverter.toMarkdown(writeResult.content)
        assertEquals(1, readResult.blocks.size)
        val block = readResult.blocks[0] as MarkdownBlock.Blockquote
        assertEquals("This is a quote", block.text)
    }

    @Test
    fun pcktHorizontalRuleRoundTrip() {
        val markdown = "---"
        val writeResult = PcktContentConverter.fromMarkdown(markdown)
        val readResult = PcktContentConverter.toMarkdown(writeResult.content)
        assertEquals(1, readResult.blocks.size)
        assertTrue(readResult.blocks[0] is MarkdownBlock.HorizontalRule)
    }

    // ── Offprint Round-Trip ─────────────────────────────────────────────

    @Test
    fun offprintHeadingRoundTrip() {
        val markdown = "# Hello World"
        val writeResult = OffprintContentConverter.fromMarkdown(markdown)
        val content = writeResult.content
        assertEquals(OffprintTypes.CONTENT, content["\$type"])

        val readResult = OffprintContentConverter.toMarkdown(content)
        assertTrue(readResult.blocks.isNotEmpty())
        val block = readResult.blocks[0] as MarkdownBlock.Heading
        assertEquals(1, block.level)
        assertEquals("Hello World", block.text)
    }

    @Test
    fun offprintHeadingCappedAtLevel3() {
        val markdown = "#### Level 4"
        val writeResult = OffprintContentConverter.fromMarkdown(markdown)
        val readResult = OffprintContentConverter.toMarkdown(writeResult.content)
        val block = readResult.blocks[0] as MarkdownBlock.Heading
        assertEquals(3, block.level)
    }

    @Test
    fun offprintMathBlockRoundTrip() {
        val markdown = "```math\nE = mc^2\n```"
        val writeResult = OffprintContentConverter.fromMarkdown(markdown)
        val readResult = OffprintContentConverter.toMarkdown(writeResult.content)
        assertEquals(1, readResult.blocks.size)
        val block = readResult.blocks[0] as MarkdownBlock.Math
        assertEquals("E = mc^2", block.tex)
    }

    @Test
    fun offprintParagraphRoundTrip() {
        val markdown = "Hello world"
        val writeResult = OffprintContentConverter.fromMarkdown(markdown)
        val readResult = OffprintContentConverter.toMarkdown(writeResult.content)
        assertEquals(1, readResult.blocks.size)
        val block = readResult.blocks[0] as MarkdownBlock.Paragraph
        assertEquals("Hello world", block.text)
    }

    @Test
    fun offprintHorizontalRuleRoundTrip() {
        val markdown = "---"
        val writeResult = OffprintContentConverter.fromMarkdown(markdown)
        val readResult = OffprintContentConverter.toMarkdown(writeResult.content)
        assertEquals(1, readResult.blocks.size)
        assertTrue(readResult.blocks[0] is MarkdownBlock.HorizontalRule)
    }

    // ── ContentFormatDispatcher ──────────────────────────────────────────

    @Test
    fun dispatcherDetectsLeafletFormat() {
        val content = mapOf(
            "\$type" to LeafletTypes.CONTENT,
            "pages" to emptyList<Any>()
        )
        val result = ContentFormatDispatcher.toMarkdown(content)
        assertTrue(result.blocks.isEmpty())
        assertTrue(result.lost.isEmpty())
    }

    @Test
    fun dispatcherDetectsMarkpubFormat() {
        val content = mapOf(
            "\$type" to MarkpubTypes.CONTENT,
            "text" to mapOf(
                "\$type" to MarkpubTypes.TEXT,
                "markdown" to "Hello world"
            )
        )
        val result = ContentFormatDispatcher.toMarkdown(content)
        assertEquals(1, result.blocks.size)
    }

    @Test
    fun dispatcherReturnsEmptyForUnknownFormat() {
        val content = mapOf(
            "\$type" to "unknown.format"
        )
        val result = ContentFormatDispatcher.toMarkdown(content)
        assertTrue(result.blocks.isEmpty())
        assertTrue(result.lost.isNotEmpty())
    }

    @Test
    fun dispatcherFromMarkdownLeaflet() {
        val result = ContentFormatDispatcher.fromMarkdown("# Hello", "Leaflet")
        assertEquals(LeafletTypes.CONTENT, result.content["\$type"])
    }

    @Test
    fun dispatcherFromMarkdownMarkpub() {
        val result = ContentFormatDispatcher.fromMarkdown("# Hello", "Markpub")
        assertEquals(MarkpubTypes.CONTENT, result.content["\$type"])
    }

    @Test
    fun dispatcherFromMarkdownPckt() {
        val result = ContentFormatDispatcher.fromMarkdown("# Hello", "pckt")
        assertEquals(PcktTypes.CONTENT, result.content["\$type"])
    }

    @Test
    fun dispatcherFromMarkdownOffprint() {
        val result = ContentFormatDispatcher.fromMarkdown("# Hello", "Offprint")
        assertEquals(OffprintTypes.CONTENT, result.content["\$type"])
    }

    @Test
    fun dispatcherContentTypeMapping() {
        assertEquals(LeafletTypes.CONTENT, ContentFormatDispatcher.contentTypeForFormat("Leaflet"))
        assertEquals(MarkpubTypes.CONTENT, ContentFormatDispatcher.contentTypeForFormat("Markpub"))
        assertEquals(PcktTypes.CONTENT, ContentFormatDispatcher.contentTypeForFormat("pckt"))
        assertEquals(OffprintTypes.CONTENT, ContentFormatDispatcher.contentTypeForFormat("Offprint"))
        assertEquals(null, ContentFormatDispatcher.contentTypeForFormat("unknown"))
    }

    @Test
    fun dispatcherFormatMapping() {
        assertEquals("Leaflet", ContentFormatDispatcher.formatForContentType(LeafletTypes.CONTENT))
        assertEquals("Markpub", ContentFormatDispatcher.formatForContentType(MarkpubTypes.CONTENT))
        assertEquals("pckt", ContentFormatDispatcher.formatForContentType(PcktTypes.CONTENT))
        assertEquals("Offprint", ContentFormatDispatcher.formatForContentType(OffprintTypes.CONTENT))
        assertEquals(null, ContentFormatDispatcher.formatForContentType("unknown"))
    }

    // ── Block Loss Labels ───────────────────────────────────────────────

    @Test
    fun leafletLossLabels() {
        val labels = BlockLossLabels.leaflet
        assertEquals(9, labels.size)
        assertEquals("embeds", labels[LeafletTypes.BLOCKS_IFRAME])
        assertEquals("polls", labels[LeafletTypes.BLOCKS_POLL])
    }

    @Test
    fun pcktLossLabels() {
        val labels = BlockLossLabels.pckt
        assertEquals(8, labels.size)
        assertEquals("tables", labels[PcktTypes.BLOCK_TABLE])
        assertEquals("hard breaks", labels[PcktTypes.BLOCK_HARD_BREAK])
    }

    @Test
    fun offprintLossLabels() {
        val labels = BlockLossLabels.offprint
        assertEquals(8, labels.size)
        assertEquals("callouts", labels[OffprintTypes.BLOCK_CALLOUT])
        assertEquals("Bluesky posts", labels[OffprintTypes.BLOCK_BLUESKY_POST])
    }

    // ── Empty / Edge Cases ──────────────────────────────────────────────

    @Test
    fun emptyMarkdownProducesEmptyContent() {
        val result = ContentFormatDispatcher.fromMarkdown("", "Leaflet")
        val readResult = ContentFormatDispatcher.toMarkdown(result.content)
        assertTrue(readResult.blocks.isEmpty())
    }

    @Test
    fun leafletToMarkdownEmptyContent() {
        val content = mapOf<String, Any?>(
            "\$type" to LeafletTypes.CONTENT,
            "pages" to null
        )
        val result = LeafletContentConverter.toMarkdown(content)
        assertTrue(result.blocks.isEmpty())
    }

    @Test
    fun pcktToMarkdownEmptyItems() {
        val content = mapOf<String, Any?>(
            "\$type" to PcktTypes.CONTENT,
            "items" to null
        )
        val result = PcktContentConverter.toMarkdown(content)
        assertTrue(result.blocks.isEmpty())
    }

    @Test
    fun offprintToMarkdownEmptyItems() {
        val content = mapOf<String, Any?>(
            "\$type" to OffprintTypes.CONTENT,
            "items" to null
        )
        val result = OffprintContentConverter.toMarkdown(content)
        assertTrue(result.blocks.isEmpty())
    }

    // ── Multi-Block Round-Trip ──────────────────────────────────────────

    @Test
    fun leafletMultiBlockRoundTrip() {
        val markdown = "# Title\n\nParagraph text\n\n---\n\n## Subtitle"
        val writeResult = LeafletContentConverter.fromMarkdown(markdown)
        val readResult = LeafletContentConverter.toMarkdown(writeResult.content)
        assertEquals(4, readResult.blocks.size)
        assertTrue(readResult.blocks[0] is MarkdownBlock.Heading)
        assertTrue(readResult.blocks[1] is MarkdownBlock.Paragraph)
        assertTrue(readResult.blocks[2] is MarkdownBlock.HorizontalRule)
        assertTrue(readResult.blocks[3] is MarkdownBlock.Heading)
    }

    @Test
    fun pcktMultiBlockRoundTrip() {
        val markdown = "# Title\n\nParagraph text\n\n---\n\n## Subtitle"
        val writeResult = PcktContentConverter.fromMarkdown(markdown)
        val readResult = PcktContentConverter.toMarkdown(writeResult.content)
        assertEquals(4, readResult.blocks.size)
    }

    @Test
    fun offprintMultiBlockRoundTrip() {
        val markdown = "# Title\n\nParagraph text\n\n---\n\n## Subtitle"
        val writeResult = OffprintContentConverter.fromMarkdown(markdown)
        val readResult = OffprintContentConverter.toMarkdown(writeResult.content)
        assertEquals(4, readResult.blocks.size)
    }

    // Helper to avoid unused warning
    private fun writeOrderedList(content: Map<String, Any?>): Map<String, Any?> = content
}

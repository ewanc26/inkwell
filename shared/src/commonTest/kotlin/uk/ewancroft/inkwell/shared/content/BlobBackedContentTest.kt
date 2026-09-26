package uk.ewancroft.inkwell.shared.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BlobBackedContentTest {

    private val blob = mapOf(
        "\$type" to "blob",
        "ref" to mapOf("\$link" to "bafyreiexamplecid"),
        "mimeType" to "text/markdown",
        "size" to 1234,
    )

    @Test
    fun `only markpub and leaflet define a blob-backed representation`() {
        assertTrue(BlobBackedContent.supportsBlobBacking("Markpub"))
        assertTrue(BlobBackedContent.supportsBlobBacking("Leaflet"))
        assertFalse(BlobBackedContent.supportsBlobBacking("pckt"))
        assertFalse(BlobBackedContent.supportsBlobBacking("Offprint"))
        assertNull(BlobBackedContent.blobMimeType("pckt"))
        assertNull(BlobBackedContent.blobMimeType("Offprint"))
    }

    @Test
    fun `markpub blob content puts the blob on the text node`() {
        val content = BlobBackedContent.markpubBlobContent(blob)

        assertEquals(MarkpubTypes.CONTENT, content["\$type"])
        val text = content["text"] as Map<*, *>
        assertEquals(MarkpubTypes.TEXT, text["\$type"])
        assertEquals(blob, text["textBlob"])
        assertNull(text["markdown"])
        assertTrue(BlobBackedContent.isBlobBacked(content))
    }

    @Test
    fun `leaflet blob content replaces the inline pages array`() {
        val content = BlobBackedContent.leafletBlobContent(blob)

        assertEquals(LeafletTypes.CONTENT, content["\$type"])
        assertEquals(blob, content["blobPages"])
        assertNull(content["pages"])
        assertTrue(BlobBackedContent.isBlobBacked(content))
    }

    @Test
    fun `inline content is not reported as blob-backed`() {
        assertFalse(BlobBackedContent.isBlobBacked(MarkpubContentConverter.fromMarkdown("# hi").content))
        assertFalse(BlobBackedContent.isBlobBacked(LeafletContentConverter.fromMarkdown("# hi").content))
        assertFalse(BlobBackedContent.isBlobBacked(PcktContentConverter.fromMarkdown("# hi").content))
    }

    @Test
    fun `leaflet pages are extracted for the backing blob`() {
        val inline = LeafletContentConverter.fromMarkdown("# Heading\n\nBody text.").content
        val pages = BlobBackedContent.leafletPages(inline)

        assertEquals(1, pages.size)
        val page = pages.first() as Map<*, *>
        assertEquals(LeafletTypes.PAGES_LINEAR_DOCUMENT, page["\$type"])
    }

    @Test
    fun `leaflet pages round-trip through the blob-backed representation`() {
        val original = LeafletContentConverter.fromMarkdown("# Heading\n\nBody text.").content
        val pages = BlobBackedContent.leafletPages(original)

        val spilled = BlobBackedContent.leafletBlobContent(blob)
        assertTrue(BlobBackedContent.isBlobBacked(spilled))

        val restored = BlobBackedContent.leafletInlineContent(pages)
        assertFalse(BlobBackedContent.isBlobBacked(restored))
        assertEquals(
            ContentFormatDispatcher.toMarkdownString(original),
            ContentFormatDispatcher.toMarkdownString(restored),
        )
    }

    @Test
    fun `markpub markdown round-trips through the blob-backed representation`() {
        val markdown = "# Heading\n\nBody text."
        val spilled = BlobBackedContent.markpubBlobContent(blob)
        assertEquals("", MarkpubContentConverter.toRawMarkdown(spilled))

        val restored = BlobBackedContent.markpubInlineContent(markdown)
        assertEquals(markdown, MarkpubContentConverter.toRawMarkdown(restored))
        assertFalse(BlobBackedContent.isBlobBacked(restored))
    }

    @Test
    fun `text content shorter than the budget is left alone`() {
        val text = "a".repeat(100)
        assertEquals(text, BlobBackedContent.truncateTextContent(text, maxBytes = 1_000))
    }

    @Test
    fun `text content longer than the budget is truncated to the byte budget`() {
        val text = "a".repeat(5_000)
        val truncated = BlobBackedContent.truncateTextContent(text, maxBytes = 1_000)

        assertEquals(1_000, truncated.encodeToByteArray().size)
    }

    @Test
    fun `truncation never splits a multi-byte character`() {
        val text = "é".repeat(100)
        val truncated = BlobBackedContent.truncateTextContent(text, maxBytes = 9)

        assertEquals("é".repeat(4), truncated)
        assertEquals(8, truncated.encodeToByteArray().size)
    }

    @Test
    fun `truncation never splits a surrogate pair`() {
        val text = "😀".repeat(10)
        val truncated = BlobBackedContent.truncateTextContent(text, maxBytes = 10)

        assertEquals("😀😀", truncated)
        assertEquals(8, truncated.encodeToByteArray().size)
    }
}

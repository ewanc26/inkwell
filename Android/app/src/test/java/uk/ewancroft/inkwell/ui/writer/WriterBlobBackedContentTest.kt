package uk.ewancroft.inkwell.ui.writer

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import uk.ewancroft.inkwell.shared.content.BlobBackedContent
import uk.ewancroft.inkwell.shared.content.ContentFormatDetector
import uk.ewancroft.inkwell.shared.content.ContentFormatDispatcher
import uk.ewancroft.inkwell.shared.content.JsonMapBridge
import uk.ewancroft.inkwell.shared.validation.RecordSizePolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

/**
 * Covers the record-size spill threshold: content just under the limit must stay
 * inline, content just over it must move into the format's defined blob-backed
 * representation (or fail with a clear error where no such representation exists).
 */
class WriterBlobBackedContentTest {

    private class RecordingUploader : BlobUploader {
        var calls = 0
        var lastBytes: ByteArray? = null
        var lastMimeType: String? = null

        override suspend fun upload(bytes: ByteArray, mimeType: String): JsonObject {
            calls++
            lastBytes = bytes
            lastMimeType = mimeType
            return buildJsonObject {
                put(
                    "blob",
                    buildJsonObject {
                        put("\$type", "blob")
                        put("ref", buildJsonObject { put("\$link", "bafyreitestblobcid") })
                        put("mimeType", mimeType)
                        put("size", bytes.size)
                    },
                )
            }
        }
    }

    /** Envelope overhead of the fields the Writer always writes alongside `content`. */
    private fun buildRecord(content: JsonObject, textContent: String): JsonObject = buildJsonObject {
        put("\$type", "site.standard.document")
        put("site", "at://did:plc:example/site.standard.publication/self")
        put("title", "Test document")
        put("publishedAt", "2026-09-25T00:00:00Z")
        put("content", content)
        if (textContent.isNotBlank()) put("textContent", textContent)
    }

    /**
     * Markdown whose inline record lands a little under the limit. `textContent`
     * duplicates the body, so the body itself is roughly half the budget.
     */
    private fun markdownUnderLimit(): String =
        "word ".repeat((RecordSizePolicy.MAX_DOCUMENT_RECORD_BYTES / 2 - 8_000) / 5)

    private fun markdownOverLimit(): String =
        "word ".repeat(RecordSizePolicy.MAX_DOCUMENT_RECORD_BYTES / 5)

    private suspend fun build(markdown: String, format: String, uploader: BlobUploader): JsonObject =
        buildFittingDocumentRecord(
            markdown = markdown,
            format = format,
            uploadedBlobs = emptyMap(),
            uploader = uploader,
            buildRecord = ::buildRecord,
        )

    // ── Below the threshold ────────────────────────────────────────────────

    @Test
    fun `markpub content below the threshold stays inline`() = runBlocking {
        val uploader = RecordingUploader()
        val markdown = markdownUnderLimit()

        val record = build(markdown, "Markpub", uploader)

        assertEquals(0, uploader.calls)
        val content = record["content"]!!.jsonObject
        assertEquals(ContentFormatDetector.MARKPUB, content["\$type"]!!.jsonPrimitive.content)
        assertEquals(markdown, content["text"]!!.jsonObject["markdown"]!!.jsonPrimitive.content)
        assertNull(content["text"]!!.jsonObject["textBlob"])
        assertTrue(encodedRecordSize(record) <= RecordSizePolicy.MAX_DOCUMENT_RECORD_BYTES)
    }

    @Test
    fun `leaflet content below the threshold stays inline`() = runBlocking {
        val uploader = RecordingUploader()

        val record = build(markdownUnderLimit(), "Leaflet", uploader)

        assertEquals(0, uploader.calls)
        val content = record["content"]!!.jsonObject
        assertEquals(ContentFormatDetector.LEAFLET, content["\$type"]!!.jsonPrimitive.content)
        assertNotNull(content["pages"])
        assertNull(content["blobPages"])
    }

    // ── Above the threshold ────────────────────────────────────────────────

    @Test
    fun `markpub content above the threshold spills into textBlob`() = runBlocking {
        val uploader = RecordingUploader()
        val markdown = markdownOverLimit()

        val record = build(markdown, "Markpub", uploader)

        assertEquals(1, uploader.calls)
        assertEquals(BlobBackedContent.MARKPUB_BLOB_MIME, uploader.lastMimeType)
        assertEquals(markdown, uploader.lastBytes!!.decodeToString())

        val text = record["content"]!!.jsonObject["text"]!!.jsonObject
        assertNull(text["markdown"])
        assertEquals(
            "bafyreitestblobcid",
            text["textBlob"]!!.jsonObject["ref"]!!.jsonObject["\$link"]!!.jsonPrimitive.content,
        )
        assertTrue(encodedRecordSize(record) <= RecordSizePolicy.MAX_DOCUMENT_RECORD_BYTES)
    }

    @Test
    fun `leaflet content above the threshold spills into blobPages`() = runBlocking {
        val uploader = RecordingUploader()

        val record = build(markdownOverLimit(), "Leaflet", uploader)

        assertEquals(1, uploader.calls)
        assertEquals(BlobBackedContent.LEAFLET_BLOB_MIME, uploader.lastMimeType)

        val content = record["content"]!!.jsonObject
        assertNull(content["pages"])
        assertEquals(
            "bafyreitestblobcid",
            content["blobPages"]!!.jsonObject["ref"]!!.jsonObject["\$link"]!!.jsonPrimitive.content,
        )
        assertTrue(encodedRecordSize(record) <= RecordSizePolicy.MAX_DOCUMENT_RECORD_BYTES)
    }

    @Test
    fun `spilled records keep a truncated textContent`() = runBlocking {
        val record = build(markdownOverLimit(), "Markpub", RecordingUploader())

        val textContent = record["textContent"]!!.jsonPrimitive.content
        assertTrue(textContent.isNotBlank())
        assertTrue(textContent.encodeToByteArray().size <= BlobBackedContent.MAX_SPILLED_TEXT_CONTENT_BYTES)
    }

    // ── Formats with no blob-backed representation ─────────────────────────

    @Test
    fun `pckt content above the threshold reports that the format cannot fit`() {
        val error = assertFailsWith<DocumentTooLargeException> {
            runBlocking { build(markdownOverLimit(), "pckt", RecordingUploader()) }
        }
        assertTrue(error.message!!.contains("pckt"))
        assertTrue(error.message!!.contains("no blob-backed representation"))
    }

    @Test
    fun `offprint content above the threshold reports that the format cannot fit`() {
        val error = assertFailsWith<DocumentTooLargeException> {
            runBlocking { build(markdownOverLimit(), "Offprint", RecordingUploader()) }
        }
        assertTrue(error.message!!.contains("Offprint"))
    }

    @Test
    fun `no blob is uploaded for a format that cannot spill`() {
        val uploader = RecordingUploader()
        runCatching { runBlocking { build(markdownOverLimit(), "pckt", uploader) } }
        assertEquals(0, uploader.calls)
    }

    // ── Round trip ─────────────────────────────────────────────────────────

    @Test
    fun `leaflet pages round-trip through the uploaded blob payload`() = runBlocking {
        val uploader = RecordingUploader()
        val markdown = markdownOverLimit()

        build(markdown, "Leaflet", uploader)

        val pages = Json.parseToJsonElement(uploader.lastBytes!!.decodeToString()) as JsonArray
        val restored = JsonMapBridge.mapToJson(
            BlobBackedContent.leafletInlineContent(JsonMapBridge.arrayToList(pages)),
        )
        val inline = MarkdownConverter.convert(markdown, "Leaflet")

        assertEquals(
            ContentFormatDispatcher.toMarkdownString(JsonMapBridge.jsonToMap(inline)),
            ContentFormatDispatcher.toMarkdownString(JsonMapBridge.jsonToMap(restored)),
        )
    }

    @Test
    fun `markpub markdown round-trips through the uploaded blob payload`() = runBlocking {
        val uploader = RecordingUploader()
        val markdown = markdownOverLimit()

        build(markdown, "Markpub", uploader)

        val restored = JsonMapBridge.mapToJson(
            BlobBackedContent.markpubInlineContent(uploader.lastBytes!!.decodeToString()),
        )
        assertEquals(markdown, restored["text"]!!.jsonObject["markdown"]!!.jsonPrimitive.content)
    }
}

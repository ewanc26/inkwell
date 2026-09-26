package uk.ewancroft.inkwell.ui.writer

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import uk.ewancroft.inkwell.data.repository.PdsRepository
import uk.ewancroft.inkwell.data.repository.downloadBlobRef
import uk.ewancroft.inkwell.shared.content.BlobBackedContent
import uk.ewancroft.inkwell.shared.content.ContentFormatDetector
import uk.ewancroft.inkwell.shared.content.JsonMapBridge
import uk.ewancroft.inkwell.shared.validation.JsonSafety
import uk.ewancroft.inkwell.shared.validation.RecordSizePolicy

/**
 * Blob-backed document content for the Writer.
 *
 * Long documents cannot fit inside a single AT Protocol record, so the formats
 * that define a blob-backed representation spill their payload into a PDS blob
 * and reference it from the record. The shapes and the policy live in shared KMP
 * ([BlobBackedContent]); this file owns the Android side of the lifecycle —
 * uploading the backing blob before the record is written, and resolving one back
 * into editable content when an existing document is loaded.
 */

/** Uploads [bytes] and returns the raw `com.atproto.repo.uploadBlob` response. */
internal fun interface BlobUploader {
    suspend fun upload(bytes: ByteArray, mimeType: String): JsonObject
}

private val blobContentJson = Json { ignoreUnknownKeys = true; isLenient = true }

/**
 * Builds a document record that fits inside the record-size limit.
 *
 * The inline representation is tried first — most documents never need a blob.
 * If it does not fit and the format defines a blob-backed representation, the
 * payload is uploaded as a blob *before* the record is written and the returned
 * reference replaces the inline content. Formats without such a representation
 * raise [DocumentTooLargeException] rather than letting the PDS reject the write.
 *
 * @param buildRecord assembles the final record from the content object and the
 *   portable `textContent`, so create and edit paths share one size-aware flow.
 */
internal suspend fun buildFittingDocumentRecord(
    markdown: String,
    format: String,
    uploadedBlobs: Map<String, JsonObject>,
    uploader: BlobUploader,
    buildRecord: (content: JsonObject, textContent: String) -> JsonObject,
): JsonObject {
    val plaintext = markdownToPlaintext(markdown)
    val inlineContent = MarkdownConverter.convert(markdown, format, uploadedBlobs)
    val inlineRecord = buildRecord(inlineContent, plaintext)

    val inlineBytes = encodedRecordSize(inlineRecord)
    if (!RecordSizePolicy.exceedsLimit(inlineBytes)) return inlineRecord

    if (!BlobBackedContent.supportsBlobBacking(format)) {
        throw DocumentTooLargeException(
            BlobBackedContent.unsupportedFormatMessage(
                format = format,
                encodedBytes = inlineBytes,
                limitBytes = RecordSizePolicy.MAX_DOCUMENT_RECORD_BYTES,
            ),
        )
    }

    val spilledContent = spillContentToBlob(
        inlineContent = inlineContent,
        markdown = markdown,
        format = format,
        uploader = uploader,
    )
    val spilledRecord = buildRecord(
        spilledContent,
        BlobBackedContent.truncateTextContent(plaintext),
    )
    ensureDocumentRecordFits(spilledRecord)
    return spilledRecord
}

/** Uploads the format's payload as a blob and returns the blob-backed content object. */
private suspend fun spillContentToBlob(
    inlineContent: JsonObject,
    markdown: String,
    format: String,
    uploader: BlobUploader,
): JsonObject {
    val mimeType = BlobBackedContent.blobMimeType(format)
        ?: throw DocumentTooLargeException("$format has no blob-backed representation.")

    val payload = when (format) {
        "Markpub" -> markdown.encodeToByteArray()
        else -> {
            val pages = inlineContent["pages"] as? JsonArray ?: JsonArray(emptyList())
            Json.encodeToString(JsonArray.serializer(), pages).encodeToByteArray()
        }
    }

    val blob = uploader.upload(payload, mimeType)["blob"]?.jsonObject
        ?: throw IllegalStateException("Blob upload response did not include a blob reference")
    val blobMap = JsonMapBridge.jsonToMap(blob)

    val content = when (format) {
        "Markpub" -> BlobBackedContent.markpubBlobContent(blobMap)
        else -> BlobBackedContent.leafletBlobContent(blobMap)
    }
    return JsonMapBridge.mapToJson(content)
}

/**
 * Resolves a blob-backed document's content back into its inline form so the
 * Writer can edit it. Inline content is returned untouched.
 *
 * Throws rather than degrading to empty content: silently loading nothing would
 * let a re-save overwrite a published document with a blank body.
 */
internal suspend fun PdsRepository.resolveBlobBackedContent(
    content: JsonObject?,
    authorDid: String,
): JsonObject? {
    if (content == null) return null
    if (!BlobBackedContent.isBlobBacked(JsonMapBridge.jsonToMap(content))) return content

    return when (content["\$type"]?.jsonPrimitive?.contentOrNull) {
        ContentFormatDetector.MARKPUB -> {
            val blob = content["text"]?.jsonObject?.get("textBlob")?.jsonObject ?: return content
            val bytes = downloadBlobRef(blob, authorDid, BlobBackedContent.MARKPUB_BLOB_MIME)
            JsonMapBridge.mapToJson(BlobBackedContent.markpubInlineContent(bytes.decodeToString()))
        }

        ContentFormatDetector.LEAFLET -> {
            val blob = content["blobPages"]?.jsonObject ?: return content
            val bytes = downloadBlobRef(blob, authorDid, BlobBackedContent.LEAFLET_BLOB_MIME)
            val pages = blobContentJson.parseToJsonElement(bytes.decodeToString()) as? JsonArray
                ?: throw IllegalStateException("Blob-backed Leaflet pages were not a JSON array")
            if (!JsonSafety.isSafe(pages)) {
                throw IllegalStateException("Blob-backed Leaflet pages were nested too deeply to edit")
            }
            JsonMapBridge.mapToJson(BlobBackedContent.leafletInlineContent(JsonMapBridge.arrayToList(pages)))
        }

        else -> content
    }
}


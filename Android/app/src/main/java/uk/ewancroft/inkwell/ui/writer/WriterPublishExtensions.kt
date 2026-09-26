package uk.ewancroft.inkwell.ui.writer

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import uk.ewancroft.inkwell.shared.graph.CollectionNsids
import uk.ewancroft.inkwell.shared.validation.RecordSizePolicy
import uk.ewancroft.inkwell.shared.validation.StandardSiteValidation

fun WriterViewModel.publish() {
    val state = uiStateInternal.value
    val pub = state.selectedPublication ?: return

    if (state.title.isBlank()) {
        uiStateInternal.value = state.copy(publishError = "Title is required")
        return
    }

    val validationError = StandardSiteValidation.validateDocument(
        StandardSiteValidation.DocumentInput(
            site = pub.uri,
            title = state.title,
            description = state.description.takeIf(String::isNotBlank),
            tags = null,
            path = state.path.trim().ifBlank { null },
            publishedAt = "pending",
        ),
    ).firstOrNull()
    if (validationError != null) {
        uiStateInternal.value = state.copy(publishError = "${validationError.field}: ${validationError.message}")
        return
    }

    if (state.verifiedPublicationUri == null) {
        uiStateInternal.value = state.copy(publishError = "Publication must be verified before publishing")
        return
    }

    viewModelScope.launch {
        uiStateInternal.value = uiStateInternal.value.copy(isPublishing = true, publishError = null, publishSuccess = null)
        try {
            val now = java.time.Instant.now().toString()

            val normalizedPath = state.path.trim().let { p ->
                when {
                    p.isEmpty() -> ""
                    p.startsWith("/") -> p
                    else -> "/$p"
                }
            }

            val uploader = BlobUploader { bytes, mimeType -> pdsRepository.uploadBlob(bytes, mimeType) }

            if (state.editingDocumentUri != null) {
                val recordCID = state.editingDocumentRecordCID
                if (recordCID == null) {
                    uiStateInternal.value = uiStateInternal.value.copy(
                        isPublishing = false,
                        publishError = "Missing record CID for existing document",
                    )
                    return@launch
                }

                val record = buildFittingDocumentRecord(
                    markdown = state.markdown,
                    format = state.selectedFormat,
                    uploadedBlobs = state.uploadedBlobs,
                    uploader = uploader,
                ) { content, textContent ->
                    mergeExistingDocumentRecord(state.editingDocumentRecord) {
                        put("\$type", CollectionNsids.DOCUMENT)
                        put("site", pub.uri)
                        put("title", state.title.trim())
                        applyEditTimestamps(this, state.editingDocumentRecord, now)
                        if (state.description.isNotBlank()) {
                            put("description", state.description.trim())
                        }
                        if (normalizedPath.isNotBlank()) {
                            put("path", normalizedPath)
                        }
                        put("content", content)
                        if (textContent.isNotBlank()) {
                            put("textContent", textContent)
                        }
                    }
                }

                pdsRepository.updateRecord(
                    uri = state.editingDocumentUri,
                    record = record,
                    recordCID = recordCID,
                )

                uiStateInternal.value = uiStateInternal.value.copy(
                    isPublishing = false,
                    publishSuccess = "Updated successfully.",
                    publishedUri = state.editingDocumentUri,
                    editingDocumentUri = null,
                    editingDocumentRecordCID = null,
                )
            } else {
                val record = buildFittingDocumentRecord(
                    markdown = state.markdown,
                    format = state.selectedFormat,
                    uploadedBlobs = state.uploadedBlobs,
                    uploader = uploader,
                ) { content, textContent ->
                    buildJsonObject {
                        put("\$type", CollectionNsids.DOCUMENT)
                        put("site", pub.uri)
                        put("title", state.title.trim())
                        put("publishedAt", now)
                        if (state.description.isNotBlank()) {
                            put("description", state.description.trim())
                        }
                        if (normalizedPath.isNotBlank()) {
                            put("path", normalizedPath)
                        }
                        put("content", content)
                        if (textContent.isNotBlank()) {
                            put("textContent", textContent)
                        }
                    }
                }

                val result = pdsRepository.createRecord(
                    collection = CollectionNsids.DOCUMENT,
                    record = record,
                )

                val publishedUri = result["uri"]?.jsonPrimitive?.content
                uiStateInternal.value = uiStateInternal.value.copy(
                    isPublishing = false,
                    publishSuccess = "Published successfully.",
                    publishedUri = publishedUri,
                    title = "",
                    description = "",
                    path = "",
                    markdown = "",
                    uploadedBlobs = emptyMap(),
                )
            }
        } catch (e: DocumentTooLargeException) {
            uiStateInternal.value = uiStateInternal.value.copy(
                isPublishing = false,
                publishError = e.message,
            )
        } catch (e: Exception) {
            uiStateInternal.value = uiStateInternal.value.copy(
                isPublishing = false,
                publishError = if (state.editingDocumentUri != null) {
                    editDocumentErrorMessage(e)
                } else {
                    "Failed to publish: ${e.message}"
                },
            )
        }
    }
}

/**
 * A document that cannot be represented inside the AT Protocol record-size limit.
 *
 * Extends [IllegalStateException] so `check`-style preflight failures and
 * format-cannot-fit failures surface through the same Writer error path.
 */
internal class DocumentTooLargeException(message: String) : IllegalStateException(message)

/** Encoded size, in bytes, of the record as it will be submitted over XRPC. */
internal fun encodedRecordSize(record: JsonObject): Int =
    Json.encodeToString(JsonObject.serializer(), record).encodeToByteArray().size

internal fun ensureDocumentRecordFits(record: JsonObject) {
    val encodedBytes = encodedRecordSize(record)
    if (RecordSizePolicy.exceedsLimit(encodedBytes)) {
        throw DocumentTooLargeException(
            "Document is too large to publish ($encodedBytes bytes; limit is " +
                "${RecordSizePolicy.MAX_DOCUMENT_RECORD_BYTES} bytes). Use a shorter document or a blob-backed format.",
        )
    }
}

internal fun mergeExistingDocumentRecord(
    existing: JsonObject?,
    overrides: JsonObjectBuilder.() -> Unit,
): JsonObject = buildJsonObject {
    existing?.forEach { (key, value) -> put(key, value) }
    overrides()
}

internal fun applyEditTimestamps(
    builder: JsonObjectBuilder,
    existing: JsonObject?,
    now: String,
) {
    if (existing?.containsKey("publishedAt") != true) {
        builder.put("publishedAt", now)
    }
    builder.put("updatedAt", now)
}

internal fun markdownToPlaintext(markdown: String): String {
    var text = markdown
    text = text.replace(Regex("^#{1,6}\\s+", RegexOption.MULTILINE), "")
    text = text.replace(Regex("^[-*]\\s+", RegexOption.MULTILINE), "")
    text = text.replace(Regex("^>\\s*", RegexOption.MULTILINE), "")
    text = text.replace(Regex("```[\\s\\S]*?```"), "")
    text = text.replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
    text = text.replace(Regex("\\*(.+?)\\*"), "$1")
    text = text.replace(Regex("~~(.+?)~~"), "$1")
    text = text.replace(Regex("`(.+?)`"), "$1")
    text = text.replace(Regex("!\\[(.+?)\\]\\((.+?)\\)"), "$1")
    text = text.replace(Regex("\\[(.+?)\\]\\((.+?)\\)"), "$1")
    text = text.replace(Regex("^---$|^\\*\\*\\*$", RegexOption.MULTILINE), "")
    return text.trim()
}

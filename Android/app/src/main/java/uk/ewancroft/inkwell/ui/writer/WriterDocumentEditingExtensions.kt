package uk.ewancroft.inkwell.ui.writer

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import uk.ewancroft.inkwell.shared.content.ContentFormatDetector
import uk.ewancroft.inkwell.shared.content.ContentFormatDispatcher
import uk.ewancroft.inkwell.shared.content.JsonMapBridge
import uk.ewancroft.inkwell.shared.markdown.MarkdownSerializer
import uk.ewancroft.inkwell.util.OfflineContentCacheManager

internal fun deleteDocumentErrorMessage(error: Throwable): String {
    return if (error.message.orEmpty().contains("swap", ignoreCase = true)
        || error.message.orEmpty().contains("invalidswap", ignoreCase = true)
    ) {
        "This document changed elsewhere. Reload it before deleting."
    } else {
        "Failed to delete document: ${error.message}"
    }
}

internal fun editDocumentErrorMessage(error: Throwable): String {
    return if (error.message.orEmpty().contains("swap", ignoreCase = true)
        || error.message.orEmpty().contains("invalidswap", ignoreCase = true)
    ) {
        "This document changed elsewhere. Reload it before saving."
    } else {
        "Failed to update document: ${error.message}"
    }
}

fun WriterViewModel.loadDocumentForEditing(uri: String) {
    viewModelScope.launch {
        uiStateInternal.value = uiStateInternal.value.copy(isEditing = true, publishError = null)
        try {
            val record = pdsRepository.getRecord(uri)
            val value = record["value"]?.jsonObject ?: throw IllegalStateException("Missing document value")
            val cid = record["cid"]?.jsonPrimitive?.content ?: throw IllegalStateException("Missing revision")

            val title = value["title"]?.jsonPrimitive?.content ?: ""
            val description = value["description"]?.jsonPrimitive?.contentOrNull ?: ""
            val path = value["path"]?.jsonPrimitive?.contentOrNull ?: ""

            // Blob-backed documents keep their payload in a PDS blob rather than
            // inline, so resolve it before converting to markdown — otherwise the
            // editor would open empty and a re-save would blank the document.
            val authorDid = uk.ewancroft.inkwell.shared.AtUri.parse(uri)?.did
                ?: throw IllegalArgumentException("Invalid document URI")
            val content = pdsRepository.resolveBlobBackedContent(value["content"]?.jsonObject, authorDid)
            val contentType = content?.get("\$type")?.jsonPrimitive?.contentOrNull
            val format = when (contentType) {
                ContentFormatDetector.MARKPUB -> "Markpub"
                ContentFormatDetector.PCKT -> "pckt"
                ContentFormatDetector.OFFPRINT -> "Offprint"
                else -> "Leaflet"
            }

            // Convert content to markdown via shared KMP for loss reporting
            val markdownResult = if (content != null) {
                val contentMap = JsonMapBridge.jsonToMap(content)
                ContentFormatDispatcher.toMarkdown(contentMap)
            } else null

            val markdownText = markdownResult?.let {
                MarkdownSerializer.serialize(it.blocks)
            } ?: value["textContent"]?.jsonPrimitive?.contentOrNull ?: ""

            val lostFeatures = markdownResult?.lost?.toList() ?: emptyList()

            val existingBlobs = harvestBlobRefs(markdownText)

            uiStateInternal.value = uiStateInternal.value.copy(
                editingDocumentUri = uri,
                editingDocumentTitle = title,
                editingDocumentDescription = description,
                editingDocumentPath = path,
                editingDocumentMarkdown = markdownText,
                editingDocumentRecordCID = cid,
                editingDocumentRecord = value,
                title = title,
                description = description,
                path = path,
                markdown = markdownText,
                selectedFormat = format,
                uploadedBlobs = existingBlobs,
                lostFeatures = lostFeatures,
                verifiedPublicationUri = null,
                verificationMessage = null,
                isEditing = false,
            )
            setMetadata(WriterMetadataDraft.fromRecord(value))
        } catch (e: Exception) {
            uiStateInternal.value = uiStateInternal.value.copy(
                isEditing = false,
                publishError = "Failed to load document: ${e.message}",
            )
        }
    }
}

private fun harvestBlobRefs(markdown: String?): Map<String, JsonObject> {
    if (markdown == null) return emptyMap()
    val regex = Regex("^!\\[([^\\]]*)\\]\\(([^)]+)\\)$", RegexOption.MULTILINE)
    return regex.findAll(markdown).associate {
        val url = it.groupValues[2]
        url to buildJsonObject { put("\$link", url) }
    }
}

fun WriterViewModel.cancelEditing() {
    uiStateInternal.value = uiStateInternal.value.copy(
        editingDocumentUri = null,
        editingDocumentRecordCID = null,
        editingDocumentRecord = null,
        editingDocumentTitle = null,
        editingDocumentDescription = null,
        editingDocumentPath = null,
        editingDocumentMarkdown = null,
        uploadedBlobs = emptyMap(),
        lostFeatures = emptyList(),
    )
}

fun WriterViewModel.deleteDocument() {
    val state = uiStateInternal.value
    val uri = state.editingDocumentUri
    val recordCID = state.editingDocumentRecordCID
    if (uri == null || recordCID == null) {
        uiStateInternal.value = state.copy(publishError = "Missing document record CID")
        return
    }

    viewModelScope.launch {
        uiStateInternal.value = uiStateInternal.value.copy(isPublishing = true, publishError = null)
        try {
            val parsed = uk.ewancroft.inkwell.shared.AtUri.parse(uri)
                ?: throw IllegalArgumentException("Invalid document URI")
            pdsRepository.deleteRecord(
                collection = parsed.collection,
                rkey = parsed.recordKey,
                swapRecord = recordCID,
            )
            OfflineContentCacheManager.remove(context, uri)
            cancelEditing()
            uiStateInternal.value = uiStateInternal.value.copy(
                isPublishing = false,
                publishSuccess = "Document deleted.",
            )
        } catch (e: Exception) {
            uiStateInternal.value = uiStateInternal.value.copy(
                isPublishing = false,
                publishError = deleteDocumentErrorMessage(e),
            )
        }
    }
}

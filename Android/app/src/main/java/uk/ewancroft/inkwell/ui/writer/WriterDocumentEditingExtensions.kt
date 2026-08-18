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

            val content = value["content"]?.jsonObject
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
                editingDocumentRevision = cid,
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
        editingDocumentRevision = null,
        editingDocumentTitle = null,
        editingDocumentDescription = null,
        editingDocumentPath = null,
        editingDocumentMarkdown = null,
        uploadedBlobs = emptyMap(),
        lostFeatures = emptyList(),
    )
}

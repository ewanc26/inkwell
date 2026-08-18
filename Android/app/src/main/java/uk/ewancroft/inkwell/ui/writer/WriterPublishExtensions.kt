package uk.ewancroft.inkwell.ui.writer

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import uk.ewancroft.inkwell.shared.graph.CollectionNsids

fun WriterViewModel.publish() {
    val state = uiStateInternal.value
    val pub = state.selectedPublication ?: return

    if (state.title.isBlank()) {
        uiStateInternal.value = state.copy(publishError = "Title is required")
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

            val content = MarkdownConverter.convert(state.markdown, state.selectedFormat, state.uploadedBlobs)
            val plaintext = markdownToPlaintext(state.markdown)

            if (state.editingDocumentUri != null) {
                val revision = state.editingDocumentRevision
                if (revision == null) {
                    uiStateInternal.value = uiStateInternal.value.copy(
                        isPublishing = false,
                        publishError = "Missing revision for existing document",
                    )
                    return@launch
                }

                val record = buildJsonObject {
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
                    if (plaintext.isNotBlank()) {
                        put("textContent", plaintext)
                    }
                }

                val result = pdsRepository.updateRecord(
                    uri = state.editingDocumentUri,
                    record = record,
                    revision = revision,
                )

                uiStateInternal.value = uiStateInternal.value.copy(
                    isPublishing = false,
                    publishSuccess = "Updated successfully.",
                    publishedUri = state.editingDocumentUri,
                    editingDocumentUri = null,
                    editingDocumentRevision = null,
                )
            } else {
                val record = buildJsonObject {
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
                    if (plaintext.isNotBlank()) {
                        put("textContent", plaintext)
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
        } catch (e: Exception) {
            uiStateInternal.value = uiStateInternal.value.copy(
                isPublishing = false,
                publishError = "Failed to publish: ${e.message}"
            )
        }
    }
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

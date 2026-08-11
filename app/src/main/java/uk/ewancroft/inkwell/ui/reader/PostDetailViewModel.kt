package uk.ewancroft.inkwell.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import uk.ewancroft.inkwell.data.model.common.AtUri
import uk.ewancroft.inkwell.data.model.content.LeafletContent
import uk.ewancroft.inkwell.data.model.content.LeafletPage
import uk.ewancroft.inkwell.data.repository.PdsRepository
import javax.inject.Inject

/**
 * A `site.standard.document` record's `content` union, normalised into
 * something the UI can render without knowing every possible format.
 *
 * Known formats get first-class rendering (Leaflet). Formats this client
 * doesn't model yet (Markpub, pckt, Offprint) degrade to a best-effort
 * plaintext extraction rather than being dropped — see AGENTS.md's note
 * that `ContentUnion` is intentionally incomplete and must not silently
 * lose unmodelled variants.
 */
sealed class DocumentContent {
    data class Leaflet(val pages: List<LeafletPage>, val authorDid: String) : DocumentContent()
    data class PlainText(val text: String) : DocumentContent()
    data object Empty : DocumentContent()
    data class Unsupported(val formatType: String?) : DocumentContent()
}

data class PostDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val uri: String = "",
    val title: String? = null,
    val description: String? = null,
    val publishedAt: String? = null,
    val coverUrl: String? = null,
    val content: DocumentContent = DocumentContent.Empty,
)

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val pdsRepository: PdsRepository,
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _uiState = MutableStateFlow(PostDetailUiState())
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    /** Tracks which URI is currently loaded/loading so repeated calls for the
     * same URI (e.g. recomposition) don't re-trigger a fetch. */
    private var loadedUri: String? = null

    fun loadPost(uri: String, forceRefresh: Boolean = false) {
        if (!forceRefresh && loadedUri == uri && (_uiState.value.error == null)) return
        loadedUri = uri

        viewModelScope.launch {
            _uiState.value = PostDetailUiState(isLoading = true, uri = uri)
            try {
                val parsed = AtUri.parse(uri)
                    ?: throw IllegalArgumentException("Malformed post link.")

                // PdsRepository.getRecord already resolves the author's PDS
                // from the DID and performs the fetch on Dispatchers.IO.
                val record = pdsRepository.getRecord(uri)
                val value = record["value"]?.jsonObject
                    ?: throw IllegalStateException("This post's record could not be read.")

                val title = value["title"]?.jsonPrimitive?.contentOrNull
                val description = value["description"]?.jsonPrimitive?.contentOrNull
                val publishedAt = value["publishedAt"]?.jsonPrimitive?.contentOrNull
                val coverUrl = value["coverImage"]?.jsonObject?.get("link")?.jsonPrimitive?.contentOrNull
                    ?: value["coverImage"]?.jsonObject?.get("\$link")?.jsonPrimitive?.contentOrNull

                val textContent = value["textContent"]?.jsonPrimitive?.contentOrNull
                val contentObj = value["content"]?.jsonObject
                val docContent = parseContent(contentObj, textContent, parsed.did)

                _uiState.value = PostDetailUiState(
                    isLoading = false,
                    error = null,
                    uri = uri,
                    title = title,
                    description = description,
                    publishedAt = publishedAt,
                    coverUrl = coverUrl,
                    content = docContent,
                )
            } catch (e: Exception) {
                _uiState.value = PostDetailUiState(
                    isLoading = false,
                    error = e.message ?: "Failed to load this post.",
                    uri = uri,
                )
            }
        }
    }

    /**
     * Resolves the record's `content` union to something renderable.
     * Unrecognised or partially-malformed content never throws here — the
     * worst case is falling back to plaintext extraction or an explicit
     * "unsupported" state, never a crash on untrusted PDS data.
     */
    private fun parseContent(
        contentObj: JsonObject?,
        textContent: String?,
        authorDid: String,
    ): DocumentContent {
        if (contentObj != null) {
            val formatType = contentObj["\$type"]?.jsonPrimitive?.contentOrNull

            if (formatType == "pub.leaflet.content") {
                val leaflet = runCatching { json.decodeFromJsonElement<LeafletContent>(contentObj) }.getOrNull()
                val pages = leaflet?.pages
                if (!pages.isNullOrEmpty()) {
                    return DocumentContent.Leaflet(pages, authorDid)
                }
            }

            if (formatType == "at.markpub.markdown") {
                val markdown = contentObj["text"]?.jsonObject?.get("markdown")?.jsonPrimitive?.contentOrNull
                if (!markdown.isNullOrBlank()) return DocumentContent.PlainText(markdown)
            }

            // blog.pckt.content, app.offprint.content, and any other
            // block-array format: no bespoke model yet, so fall back to a
            // generic walk that pulls every "plaintext" leaf out of the
            // block tree in document order. Lossy (loses block structure,
            // facets, embeds) but shows real content instead of nothing.
            val extracted = StringBuilder()
            collectPlaintext(contentObj, extracted)
            if (extracted.isNotBlank()) return DocumentContent.PlainText(extracted.toString())

            if (!textContent.isNullOrBlank()) return DocumentContent.PlainText(textContent)

            return DocumentContent.Unsupported(formatType)
        }

        if (!textContent.isNullOrBlank()) return DocumentContent.PlainText(textContent)

        return DocumentContent.Empty
    }

    private fun collectPlaintext(element: JsonElement, out: StringBuilder) {
        when (element) {
            is JsonObject -> {
                val text = element["plaintext"]?.jsonPrimitive?.contentOrNull
                if (!text.isNullOrBlank()) {
                    if (out.isNotEmpty()) out.append("\n\n")
                    out.append(text)
                }
                for ((key, child) in element) {
                    if (key == "plaintext") continue
                    collectPlaintext(child, out)
                }
            }
            is JsonArray -> element.forEach { collectPlaintext(it, out) }
            else -> {}
        }
    }
}

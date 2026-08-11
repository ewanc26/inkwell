package uk.ewancroft.inkwell.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
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
import uk.ewancroft.inkwell.data.model.atproto.DocumentRecord
import uk.ewancroft.inkwell.data.model.atproto.PublicationRecord
import uk.ewancroft.inkwell.data.model.common.AtUri
import uk.ewancroft.inkwell.data.model.content.LeafletContent
import uk.ewancroft.inkwell.data.model.content.LeafletPage
import uk.ewancroft.inkwell.data.remote.ConstellationClient
import uk.ewancroft.inkwell.data.remote.StandardSiteVerifier
import uk.ewancroft.inkwell.data.remote.VerificationFailure
import uk.ewancroft.inkwell.data.remote.VerificationResult
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

/**
 * Combined UI state for [PostDetailScreen]: the document itself (content,
 * metadata, load state), its standard.site verification badge, and the
 * signed-in user's recommend/unrecommend state — all keyed off the same
 * [uri] since they describe one post.
 */
data class PostDetailUiState(
    // Document content + metadata.
    val uri: String = "",
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val title: String? = null,
    val description: String? = null,
    val publishedAt: String? = null,
    val path: String? = null,
    val coverUrl: String? = null,
    val content: DocumentContent = DocumentContent.Empty,

    // standard.site verification badge. Null while pending/unknown — the
    // document is shown regardless, since verification is an unobtrusive
    // annotation, not a gate on reading.
    val verification: VerificationResult? = null,

    // Recommend/unrecommend toggle + count.
    val isRecommended: Boolean = false,
    val recommendRkey: String? = null,
    val recommendCount: Int = 0,
    val isLoadingRecommendState: Boolean = false,
    val hasLoadedRecommendState: Boolean = false,
    val isTogglingRecommend: Boolean = false,
    val recommendError: String? = null,
)

/**
 * Backs [PostDetailScreen]: fetches a single `site.standard.document` record
 * and renders its content, kicks off standard.site verification against the
 * domain it claims (never blocking the screen on the result), and backs the
 * recommend/unrecommend toggle with its live count.
 *
 * The signed-in user's own recommend state is checked against their local
 * repo (``PdsRepository.fetchRecommends``); the global count comes from the
 * Constellation backlink index (``ConstellationClient.getRecommendCount``)
 * since recommends from other users live in their own repos and are only
 * discoverable through the network-wide index. Mirrors Inkwell iOS
 * LoginStateManager.createRecommend/fetchRecommends/fetchRecommendCount/
 * deleteRecommend and ReadView's verification badge.
 */
@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val pdsRepository: PdsRepository,
    private val constellationClient: ConstellationClient,
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _uiState = MutableStateFlow(PostDetailUiState())
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    /** Tracks which URI is currently loaded/loading so repeated calls for the
     * same URI (e.g. recomposition) don't re-trigger a fetch. */
    private var loadedUri: String? = null

    /** Loads a post's document content, verification, and recommend state. Safe to call
     * repeatedly (e.g. on recomposition) — only re-fetches when [uri] changes, the
     * previous load errored, or [forceRefresh] is set. */
    fun loadPost(uri: String, forceRefresh: Boolean = false) {
        val current = _uiState.value
        if (!forceRefresh && loadedUri == uri && current.loadError == null) return
        loadedUri = uri

        _uiState.value = if (current.uri == uri) {
            // Retrying the same post: keep whatever recommend state already loaded
            // rather than throwing it away along with the failed document fetch.
            current.copy(isLoading = true, loadError = null, verification = null)
        } else {
            PostDetailUiState(uri = uri, isLoading = true)
        }

        loadDocument(uri)
        loadRecommendState(uri)
    }

    private fun loadDocument(uri: String) {
        viewModelScope.launch {
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
                val path = value["path"]?.jsonPrimitive?.contentOrNull
                val site = value["site"]?.jsonPrimitive?.contentOrNull
                val coverUrl = value["coverImage"]?.jsonObject?.get("link")?.jsonPrimitive?.contentOrNull
                    ?: value["coverImage"]?.jsonObject?.get("\$link")?.jsonPrimitive?.contentOrNull

                val textContent = value["textContent"]?.jsonPrimitive?.contentOrNull
                val contentObj = value["content"]?.jsonObject
                val docContent = parseContent(contentObj, textContent, parsed.did)

                // Guard against a stale response landing after the user navigated to a
                // different post.
                if (_uiState.value.uri != uri) return@launch
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadError = null,
                    title = title,
                    description = description,
                    publishedAt = publishedAt,
                    path = path,
                    coverUrl = coverUrl,
                    content = docContent,
                )

                if (site != null) {
                    verify(documentURI = uri, site = site, title = title, path = path, publishedAt = publishedAt)
                }
            } catch (e: Exception) {
                if (_uiState.value.uri != uri) return@launch
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadError = e.message ?: "Failed to load this post.",
                )
            }
        }
    }

    /**
     * Runs standard.site verification for the document against the domain it claims, and
     * records the outcome once it resolves. Deliberately only invoked from here — the
     * post detail screen — and never from the feed, so scrolling a list of cards doesn't
     * trigger a `.well-known` fetch (and, when `site` is an AT-URI, an extra `getRecord`
     * to resolve the publication) per card.
     */
    private suspend fun verify(
        documentURI: String,
        site: String,
        title: String?,
        path: String?,
        publishedAt: String?,
    ) {
        val result = try {
            val document = DocumentRecord(
                site = site,
                title = title ?: "",
                publishedAt = publishedAt ?: "",
                path = path,
            )
            val publication = resolvePublication(site)
            StandardSiteVerifier.verifyDocument(
                documentURI = documentURI,
                document = document,
                publication = publication,
            )
        } catch (e: Exception) {
            VerificationResult.Failed(VerificationFailure.Unexpected(e.message))
        }
        if (_uiState.value.uri != documentURI) return
        _uiState.value = _uiState.value.copy(verification = result)
    }

    /** Resolves the publication a document belongs to, when `site` is an AT-URI rather
     * than a direct URL. Best-effort: any failure here surfaces as a verification failure
     * downstream (an unresolvable publication means the document's canonical URL — and
     * thus its verification — can't be established), not a crash. */
    private suspend fun resolvePublication(site: String): PublicationRecord? {
        if (!site.startsWith("at://")) return null
        val publicationJson = pdsRepository.getRecord(site)
        val value = publicationJson["value"]?.jsonObject ?: return null
        val url = value["url"]?.jsonPrimitive?.contentOrNull ?: return null
        val name = value["name"]?.jsonPrimitive?.contentOrNull ?: ""
        return PublicationRecord(url = url, name = name)
    }

    /** Loads recommend state + count for [documentUri]. Safe to call repeatedly (e.g. on
     * recomposition) — only re-fetches if it hasn't already loaded/started loading for
     * this document. */
    private fun loadRecommendState(documentUri: String) {
        val current = _uiState.value
        if (current.uri == documentUri && (current.isLoadingRecommendState || current.hasLoadedRecommendState)) {
            return // already loaded (or loading) for this document
        }
        _uiState.value = _uiState.value.copy(isLoadingRecommendState = true)

        viewModelScope.launch {
            val countDeferred = async {
                runCatching { constellationClient.getRecommendCount(documentUri) }.getOrDefault(0)
            }

            var isRecommended = false
            var recommendRkey: String? = null
            val session = runCatching { pdsRepository.getSession() }.getOrNull()
            if (session != null) {
                runCatching { pdsRepository.fetchRecommends(session.did, session.pdsUrl) }
                    .onSuccess { entries ->
                        entries.firstOrNull { it.documentUri == documentUri }?.let {
                            isRecommended = true
                            recommendRkey = it.rkey
                        }
                    }
            }

            val count = countDeferred.await()
            // Guard against a stale response landing after the user navigated to a different post.
            if (_uiState.value.uri != documentUri) return@launch
            _uiState.value = _uiState.value.copy(
                recommendCount = count,
                isRecommended = isRecommended,
                recommendRkey = recommendRkey,
                isLoadingRecommendState = false,
                hasLoadedRecommendState = true,
            )
        }
    }

    /** Toggles the signed-in user's recommendation of the currently loaded document. */
    fun toggleRecommend() {
        val state = _uiState.value
        if (state.isTogglingRecommend || state.uri.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTogglingRecommend = true, recommendError = null)
            try {
                if (state.isRecommended) {
                    val rkey = state.recommendRkey
                        ?: throw IllegalStateException("Missing record key for existing recommend")
                    pdsRepository.deleteRecommend(rkey)
                    _uiState.value = _uiState.value.copy(
                        isRecommended = false,
                        recommendRkey = null,
                        recommendCount = (_uiState.value.recommendCount - 1).coerceAtLeast(0),
                        isTogglingRecommend = false,
                    )
                } else {
                    val result = pdsRepository.createRecommend(state.uri)
                    val newUri = result["uri"]?.jsonPrimitive?.content
                    val rkey = newUri?.let { AtUri.parse(it)?.recordKey }
                    _uiState.value = _uiState.value.copy(
                        isRecommended = true,
                        recommendRkey = rkey,
                        recommendCount = _uiState.value.recommendCount + 1,
                        isTogglingRecommend = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isTogglingRecommend = false,
                    recommendError = e.message ?: "Failed to update recommendation",
                )
            }
        }
    }

    fun dismissRecommendError() {
        _uiState.value = _uiState.value.copy(recommendError = null)
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

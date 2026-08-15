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
import uk.ewancroft.inkwell.data.model.graph.LeafletComment
import uk.ewancroft.inkwell.data.model.bluesky.ConstellationBacklink
import uk.ewancroft.inkwell.data.remote.ConstellationClient
import uk.ewancroft.inkwell.data.remote.StandardSiteVerifier
import uk.ewancroft.inkwell.data.remote.VerificationFailure
import uk.ewancroft.inkwell.data.remote.VerificationResult
import uk.ewancroft.inkwell.data.repository.PdsRepository
import javax.inject.Inject

sealed class DocumentContent {
    data class Leaflet(val pages: List<LeafletPage>, val authorDid: String) : DocumentContent()
    data class PlainText(val text: String) : DocumentContent()
    data object Empty : DocumentContent()
    data class Unsupported(val formatType: String?) : DocumentContent()
}

data class PostDetailUiState(
    val uri: String = "",
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val title: String? = null,
    val description: String? = null,
    val publishedAt: String? = null,
    val path: String? = null,
    val coverUrl: String? = null,
    val content: DocumentContent = DocumentContent.Empty,

    val verification: VerificationResult? = null,

    val isRecommended: Boolean = false,
    val recommendRkey: String? = null,
    val recommendCount: Int = 0,
    val isLoadingRecommendState: Boolean = false,
    val hasLoadedRecommendState: Boolean = false,
    val isTogglingRecommend: Boolean = false,
    val recommendError: String? = null,

    val comments: List<CommentEntry> = emptyList(),
    val newCommentText: String = "",
    val isSubmittingComment: Boolean = false,
    val isLoadingComments: Boolean = false,
    val replyToComment: CommentEntry? = null,
    val commentError: String? = null,

    val previousUri: String? = null,
    val previousTitle: String? = null,
    val nextUri: String? = null,
    val nextTitle: String? = null,
)

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val pdsRepository: PdsRepository,
    private val constellationClient: ConstellationClient,
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _uiState = MutableStateFlow(PostDetailUiState())
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    private var loadedUri: String? = null

    fun loadPost(uri: String, forceRefresh: Boolean = false) {
        val current = _uiState.value
        if (!forceRefresh && loadedUri == uri && current.loadError == null) return
        loadedUri = uri

        _uiState.value = if (current.uri == uri) {
            current.copy(isLoading = true, loadError = null, verification = null)
        } else {
            PostDetailUiState(uri = uri, isLoading = true)
        }

        loadDocument(uri)
        loadRecommendState(uri)
        loadComments(uri)
    }

    fun setPreviousNext(previousUri: String?, previousTitle: String?, nextUri: String?, nextTitle: String?) {
        _uiState.value = _uiState.value.copy(
            previousUri = previousUri,
            previousTitle = previousTitle,
            nextUri = nextUri,
            nextTitle = nextTitle,
        )
    }

    private fun loadDocument(uri: String) {
        viewModelScope.launch {
            try {
                val parsed = AtUri.parse(uri)
                    ?: throw IllegalArgumentException("Malformed post link.")

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

    private suspend fun resolvePublication(site: String): PublicationRecord? {
        if (!site.startsWith("at://")) return null
        val publicationJson = pdsRepository.getRecord(site)
        val value = publicationJson["value"]?.jsonObject ?: return null
        val url = value["url"]?.jsonPrimitive?.contentOrNull ?: return null
        val name = value["name"]?.jsonPrimitive?.contentOrNull ?: ""
        return PublicationRecord(url = url, name = name)
    }

    private fun loadRecommendState(documentUri: String) {
        val current = _uiState.value
        if (current.uri == documentUri && (current.isLoadingRecommendState || current.hasLoadedRecommendState)) {
            return
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

    // MARK: - Comments

    fun loadComments(documentUri: String) {
        val current = _uiState.value
        if (current.uri == documentUri && (current.isLoadingComments || current.comments.isNotEmpty())) return
        _uiState.value = _uiState.value.copy(isLoadingComments = true, comments = emptyList())

        viewModelScope.launch {
            try {
                val repoComments = mutableListOf<PdsRepository.CommentEntry>()
                val session = pdsRepository.getSession()

                if (session != null) {
                    val local = runCatching { pdsRepository.fetchComments(session.did, session.pdsUrl) }.getOrNull()
                    if (local != null) repoComments.addAll(local)
                }

                val backlinks = runCatching { constellationClient.getCommentBacklinks(documentUri) }.getOrNull()
                if (backlinks != null) {
                    val seen = repoComments.map { it.uri }.toSet()
                    val hydrated = backlinks.filter { !seen.contains(it.recordUri) }
                    for (backlink in hydrated) {
                        try {
                            val record = pdsRepository.getRecord(backlink.recordUri)
                            val value = record["value"]?.jsonObject ?: continue
                            val comment = json.decodeFromJsonElement(LeafletComment.serializer(), value)
                            repoComments.add(
                                PdsRepository.CommentEntry(
                                    uri = backlink.recordUri,
                                    rkey = backlink.rkey,
                                    comment = comment
                                )
                            )
                        } catch (_: Exception) {}
                    }
                }

                val sorted = repoComments.sortedByDescending { it.comment.plaintext }
                if (_uiState.value.uri != documentUri) return@launch
                _uiState.value = _uiState.value.copy(
                    comments = sorted.map { c ->
                        CommentEntry(
                            uri = c.uri,
                            recordKey = c.rkey,
                            record = c.comment
                        )
                    },
                    isLoadingComments = false,
                )
            } catch (e: Exception) {
                if (_uiState.value.uri != documentUri) return@launch
                _uiState.value = _uiState.value.copy(isLoadingComments = false)
            }
        }
    }

    fun onNewCommentTextChanged(text: String) {
        _uiState.value = _uiState.value.copy(newCommentText = text, commentError = null)
    }

    fun submitComment() {
        val state = _uiState.value
        val text = state.newCommentText.trim()
        if (text.isEmpty() || state.uri.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmittingComment = true, commentError = null)
            try {
                pdsRepository.createComment(
                    subject = state.uri,
                    plaintext = text,
                    replyTo = state.replyToComment?.uri,
                    onPage = null
                )
                _uiState.value = _uiState.value.copy(
                    isSubmittingComment = false,
                    newCommentText = "",
                    replyToComment = null,
                )
                loadComments(state.uri)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmittingComment = false,
                    commentError = e.message ?: "Failed to post comment",
                )
            }
        }
    }

    fun setReplyTo(comment: CommentEntry?) {
        _uiState.value = _uiState.value.copy(replyToComment = comment)
    }

    fun dismissCommentError() {
        _uiState.value = _uiState.value.copy(commentError = null)
    }
}

data class CommentEntry(
    val uri: String,
    val recordKey: String,
    val record: uk.ewancroft.inkwell.data.model.graph.LeafletComment,
)

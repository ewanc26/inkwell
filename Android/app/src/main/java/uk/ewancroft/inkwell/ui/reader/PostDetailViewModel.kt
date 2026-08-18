package uk.ewancroft.inkwell.ui.reader

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import uk.ewancroft.inkwell.data.model.atproto.BasicTheme
import uk.ewancroft.inkwell.data.model.atproto.DocumentRecord
import uk.ewancroft.inkwell.data.model.atproto.PublicationRecord
import uk.ewancroft.inkwell.data.model.atproto.PublicationTheme
import uk.ewancroft.inkwell.shared.AtUri
import uk.ewancroft.inkwell.shared.graph.CollectionNsids
import uk.ewancroft.inkwell.data.model.content.LeafletPollDefinition
import uk.ewancroft.inkwell.data.model.graph.LeafletComment
import uk.ewancroft.inkwell.data.remote.ConstellationClient
import uk.ewancroft.inkwell.data.remote.StandardSiteVerifier
import uk.ewancroft.inkwell.shared.verification.VerificationFailure
import uk.ewancroft.inkwell.shared.verification.VerificationResult
import uk.ewancroft.inkwell.data.repository.PdsRepository
import uk.ewancroft.inkwell.data.repository.CommentEntry as PdsCommentEntry
import uk.ewancroft.inkwell.data.repository.createComment
import uk.ewancroft.inkwell.data.repository.createRecommend
import uk.ewancroft.inkwell.data.repository.createSubscription
import uk.ewancroft.inkwell.data.repository.deleteRecommend
import uk.ewancroft.inkwell.data.repository.deleteSubscription
import uk.ewancroft.inkwell.data.repository.fetchComments
import uk.ewancroft.inkwell.data.repository.fetchRecommends
import uk.ewancroft.inkwell.data.repository.fetchSubscriptions
import javax.inject.Inject

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    internal val pdsRepository: PdsRepository,
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
                val parseResult = parseContent(contentObj, textContent, parsed.did, uri)

                // Extract document-level theme override
                val docTheme = runCatching {
                    value["theme"]?.jsonObject?.let { json.decodeFromJsonElement<PublicationTheme>(it) }
                }.getOrNull()

                if (_uiState.value.uri != uri) return@launch
                val pubUri = if (site?.startsWith("at://") == true &&
                    AtUri.parse(site)?.collection == CollectionNsids.PUBLICATION) {
                    site
                } else {
                    null
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadError = null,
                    title = title,
                    description = description,
                    publishedAt = publishedAt,
                    path = path,
                    coverUrl = coverUrl,
                    content = parseResult.content,
                    lostContent = parseResult.lost,
                    publicationUri = pubUri,
                    documentTheme = docTheme,
                )

                if (pubUri != null) {
                    verify(documentURI = uri, site = site!!, title = title, path = path, publishedAt = publishedAt)
                    loadSubscriptionState(pubUri)
                    loadPublicationTheme(pubUri)
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
        _uiState.value = _uiState.value.copy(publicationUrl = url)
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

    private fun loadSubscriptionState(publicationUri: String) {
        val current = _uiState.value
        if (current.uri.isBlank()) return
        if (current.hasLoadedSubscriptionState) return
        _uiState.value = _uiState.value.copy(isLoadingSubscriptionState = true)

        viewModelScope.launch {
            var isSubscribed = false
            var subscriptionRkey: String? = null
            val session = runCatching { pdsRepository.getSession() }.getOrNull()
            if (session != null) {
                runCatching { pdsRepository.fetchSubscriptions(session.did, session.pdsUrl) }
                    .onSuccess { entries ->
                        entries.firstOrNull { it.publicationUri == publicationUri }?.let {
                            isSubscribed = true
                            subscriptionRkey = it.rkey
                        }
                    }
            }

            if (_uiState.value.uri != current.uri) return@launch
            _uiState.value = _uiState.value.copy(
                isSubscribed = isSubscribed,
                subscriptionRkey = subscriptionRkey,
                isLoadingSubscriptionState = false,
                hasLoadedSubscriptionState = true,
            )
        }
    }

    fun toggleSubscription() {
        val state = _uiState.value
        if (state.isTogglingSubscription || state.uri.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTogglingSubscription = true, subscriptionError = null)
            try {
                if (state.isSubscribed) {
                    val rkey = state.subscriptionRkey
                        ?: throw IllegalStateException("Missing record key for existing subscription")
                    pdsRepository.deleteSubscription(rkey)
                    _uiState.value = _uiState.value.copy(
                        isSubscribed = false,
                        subscriptionRkey = null,
                        isTogglingSubscription = false,
                    )
                } else {
                    val publicationUri = state.publicationUri
                        ?: throw IllegalStateException("Missing publication URI")
                    val result = pdsRepository.createSubscription(publicationUri)
                    val newUri = result["uri"]?.jsonPrimitive?.content
                    val rkey = newUri?.let { AtUri.parse(it)?.recordKey }
                    _uiState.value = _uiState.value.copy(
                        isSubscribed = true,
                        subscriptionRkey = rkey,
                        isTogglingSubscription = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isTogglingSubscription = false,
                    subscriptionError = e.message ?: "Failed to update subscription",
                )
            }
        }
    }

    fun dismissSubscriptionError() {
        _uiState.value = _uiState.value.copy(subscriptionError = null)
    }

    private fun loadPublicationTheme(publicationUri: String) {
        viewModelScope.launch {
            try {
                val record = pdsRepository.getRecord(publicationUri)
                val value = record["value"]?.jsonObject ?: return@launch
                val pubTheme = runCatching {
                    value["theme"]?.jsonObject?.let { json.decodeFromJsonElement<PublicationTheme>(it) }
                }.getOrNull()
                val basic = runCatching {
                    value["basicTheme"]?.jsonObject?.let { json.decodeFromJsonElement<BasicTheme>(it) }
                }.getOrNull()
                if (_uiState.value.publicationUri != publicationUri) return@launch
                _uiState.value = _uiState.value.copy(
                    publicationTheme = pubTheme,
                    basicTheme = basic,
                )
            } catch (e: Exception) {
                Log.w("PostDetailVM", "Failed to load theme", e)
            }
        }
    }

    // MARK: - Comments

    fun loadComments(documentUri: String, forceRefresh: Boolean = false) {
        val current = _uiState.value
        if (!forceRefresh && current.uri == documentUri && (current.isLoadingComments || current.comments.isNotEmpty())) return
        _uiState.value = _uiState.value.copy(isLoadingComments = true, comments = emptyList())

        viewModelScope.launch {
            try {
                val repoComments = mutableListOf<PdsCommentEntry>()
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
                                PdsCommentEntry(
                                    uri = backlink.recordUri,
                                    rkey = backlink.rkey,
                                    comment = comment
                                )
                            )
                        } catch (e: Exception) {
                            Log.w("PostDetailVM", "Failed to parse comment", e)
                        }
                    }
                }

                // Newest-first, matching iOS LoginStateManager.fetchComments
                // (sorted by createdAt descending).
                val sorted = repoComments.sortedByDescending { it.comment.createdAt ?: "" }
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
                loadComments(state.uri, forceRefresh = true)
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

    data class PollData(
        val definition: LeafletPollDefinition,
        val voteCounts: Map<String, Int>,
        val myVote: List<String>?,
        val totalVotes: Int,
    )

    internal val pollDataInternal = MutableStateFlow<Map<String, PollData>>(emptyMap())
    val pollData: StateFlow<Map<String, PollData>> = pollDataInternal.asStateFlow()
}

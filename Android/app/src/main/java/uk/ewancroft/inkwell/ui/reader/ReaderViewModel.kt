package uk.ewancroft.inkwell.ui.reader

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import uk.ewancroft.inkwell.data.model.bluesky.BlueskyProfile
import uk.ewancroft.inkwell.shared.AtUri
import uk.ewancroft.inkwell.shared.graph.CollectionNsids
import uk.ewancroft.inkwell.shared.xrpc.XrpcEndpoints
import uk.ewancroft.inkwell.data.model.atproto.DocumentRecord
import uk.ewancroft.inkwell.data.remote.StandardSiteVerifier
import uk.ewancroft.inkwell.shared.verification.VerificationResult
import uk.ewancroft.inkwell.data.repository.PdsRepository
import uk.ewancroft.inkwell.ScreenshotConfig
import uk.ewancroft.inkwell.util.formatPublishedDate
import javax.inject.Inject

data class PostItem(
    val uri: String,
    val title: String,
    val description: String?,
    val publicationName: String?,
    val publishedAt: String,
    val coverUrl: String?,
    val site: String,
    val authorDisplayName: String? = null,
    val authorAvatar: String? = null,
    val isVerified: Boolean? = null,
) {
    val date: String get() = publishedAt.formatPublishedDate()
}

data class ReaderUiState(
    val followingPosts: List<PostItem> = emptyList(),
    val yoursPosts: List<PostItem> = emptyList(),
    val isLoadingFollowing: Boolean = false,
    val isLoadingYours: Boolean = false,
    val isLoadingMoreFollowing: Boolean = false,
    val hasMoreFollowing: Boolean = false,
    val error: String? = null,
    val selectedTab: Int = 0,
    val isVerifyingPosts: Boolean = false,
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val pdsRepository: PdsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    /** Per-publication-DID cursors for the next page of documents. */
    private val followingCursors = mutableMapOf<String, String>()

    init {
        if (ScreenshotConfig.enabled) {
            loadMockData()
        } else {
            loadData()
        }
    }

    private fun loadMockData() {
        _uiState.value = _uiState.value.copy(
            followingPosts = listOf(
                PostItem(
                    uri = "at://did:plc:ewan/site.standard.document/1",
                    title = "Publishing on the Open Web with Standard.site",
                    description = "The Standard.site publishing spec brings structured, portable records to AT Protocol.",
                    publicationName = "Ewan's Corner",
                    publishedAt = "2026-06-20T12:00:00Z",
                    coverUrl = null,
                    site = "https://ewancroft.uk",
                    authorDisplayName = "Ewan Croft",
                    authorAvatar = null,
                )
            ),
            yoursPosts = emptyList(),
            isLoadingFollowing = false,
            isLoadingYours = false,
            isLoadingMoreFollowing = false,
            hasMoreFollowing = false,
            error = null,
            selectedTab = 0,
        )
    }

    fun selectTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }

    private suspend fun verifyPosts(posts: List<PostItem>): List<PostItem> {
        val session = pdsRepository.getSession() ?: return posts
        val docsToVerify = posts.filter { it.site.startsWith("https://") && it.isVerified == null }
        if (docsToVerify.isEmpty()) return posts

        return posts.map { post ->
            if (post.site.startsWith("https://") && post.isVerified == null) {
                val result = StandardSiteVerifier.verifyDocument(
                    documentURI = post.uri,
                    document = uk.ewancroft.inkwell.data.model.atproto.DocumentRecord(
                        site = post.site,
                        title = post.title,
                        publishedAt = post.publishedAt,
                    ),
                )
                post.copy(isVerified = result is VerificationResult.Verified)
            } else {
                post
            }
        }
    }

    fun loadData() {
        if (ScreenshotConfig.enabled) {
            loadMockData()
            return
        }
        viewModelScope.launch {
            val session = pdsRepository.getSession()
            if (session != null) {
                loadFollowingFeed(session)
                loadYoursFeed(session)
            }
        }
    }

    fun loadNextFollowingPage() {
        val state = _uiState.value
        if (state.isLoadingFollowing || state.isLoadingMoreFollowing || followingCursors.isEmpty()) return
        viewModelScope.launch {
            val session = pdsRepository.getSession() ?: return@launch
            _uiState.value = _uiState.value.copy(isLoadingMoreFollowing = true, error = null)
            try {
                val cursors = followingCursors.toMap()
                val posts = mutableListOf<PostItem>()
                for ((did, cursor) in cursors) {
                    try {
                        val profile = runCatching { pdsRepository.getProfile(did) }.getOrNull()
                        val docsResponse = pdsRepository.listRecords(
                            did = did,
                            collection = CollectionNsids.DOCUMENT,
                            limit = 25,
                            cursor = cursor,
                        )
                        val docsJson = docsResponse["records"]?.jsonArray.orEmpty()
                        val nextCursor = docsResponse["cursor"]?.jsonPrimitive?.contentOrNull
                        if (nextCursor != null && nextCursor != cursor) {
                            followingCursors[did] = nextCursor
                        } else {
                            followingCursors.remove(did)
                        }
                        for (docJson in docsJson) {
                            try {
                                val docValue = docJson.jsonObject["value"]?.jsonObject ?: continue
                                val docUri = docJson.jsonObject["uri"]?.jsonPrimitive?.content ?: continue
                                posts.add(PostItem(
                                    uri = docUri,
                                    title = docValue["title"]?.jsonPrimitive?.content ?: "Untitled",
                                    description = docValue["description"]?.jsonPrimitive?.contentOrNull,
                                    publicationName = profile?.displayName ?: profile?.handle,
                                    publishedAt = docValue["publishedAt"]?.jsonPrimitive?.content ?: "",
                                    coverUrl = docValue["coverImage"]?.jsonObject?.get("link")?.jsonPrimitive?.content
                                        ?: docValue["coverImage"]?.jsonObject?.get("\$link")?.jsonPrimitive?.content,
                                    site = docValue["site"]?.jsonPrimitive?.content ?: "",
                                    authorDisplayName = profile?.displayName,
                                    authorAvatar = profile?.avatar,
                                ))
                            } catch (e: Exception) {
                                Log.w("ReaderViewModel", "Failed to parse document in following feed", e)
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("ReaderViewModel", "Failed to fetch documents for DID $did", e)
                    }
                }
                val merged = (state.followingPosts + posts)
                    .distinctBy { it.uri }
                    .sortedByDescending { it.publishedAt }
                _uiState.value = _uiState.value.copy(
                    followingPosts = merged,
                    isLoadingMoreFollowing = false,
                    hasMoreFollowing = followingCursors.isNotEmpty(),
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingMoreFollowing = false,
                    error = e.message,
                )
            }
        }
    }

    private suspend fun loadFollowingFeed(session: uk.ewancroft.inkwell.data.repository.UserSessionInfo) {
        _uiState.value = _uiState.value.copy(isLoadingFollowing = true, error = null)
        try {
            val subscriptionsResponse = pdsRepository.listRecords(
                did = session.did,
                collection = CollectionNsids.GRAPH_SUBSCRIPTION,
                pdsUrl = session.pdsUrl
            )

            val subscriptionsJson = subscriptionsResponse["records"]?.jsonArray.orEmpty()

            followingCursors.clear()
            val didToProfile = mutableMapOf<String, BlueskyProfile>()
            val posts = mutableListOf<PostItem>()

            for (subJson in subscriptionsJson) {
                try {
                    val valueObj = subJson.jsonObject["value"]?.jsonObject ?: continue
                    val publication = valueObj["publication"]?.jsonPrimitive?.content ?: continue
                    val parsed = AtUri.parse(publication) ?: continue

                    if (parsed.did !in didToProfile) {
                        runCatching { pdsRepository.getProfile(parsed.did) }
                            .onSuccess { didToProfile[parsed.did] = it }
                    }

                    val docsResponse = pdsRepository.listRecords(
                        did = parsed.did,
                         collection = CollectionNsids.DOCUMENT,
                        limit = 25
                    )
                    val docsJson = docsResponse["records"]?.jsonArray.orEmpty()
                    val cursor = docsResponse["cursor"]?.jsonPrimitive?.contentOrNull
                    if (cursor != null) followingCursors[parsed.did] = cursor
                    val profile = didToProfile[parsed.did]
                    for (docJson in docsJson) {
                        try {
                            val docValue = docJson.jsonObject["value"]?.jsonObject ?: continue
                            val docUri = docJson.jsonObject["uri"]?.jsonPrimitive?.content ?: continue
                            posts.add(PostItem(
                                uri = docUri,
                                title = docValue["title"]?.jsonPrimitive?.content ?: "Untitled",
                                description = docValue["description"]?.jsonPrimitive?.contentOrNull,
                                publicationName = profile?.displayName ?: profile?.handle,
                                publishedAt = docValue["publishedAt"]?.jsonPrimitive?.content ?: "",
                                coverUrl = docValue["coverImage"]?.jsonObject?.get("link")?.jsonPrimitive?.content
                                    ?: docValue["coverImage"]?.jsonObject?.get("\$link")?.jsonPrimitive?.content,
                                site = docValue["site"]?.jsonPrimitive?.content ?: "",
                                authorDisplayName = profile?.displayName,
                                authorAvatar = profile?.avatar,
                            ))
                        } catch (e: Exception) {
                            Log.w("ReaderViewModel", "Failed to parse following feed document", e)
                        }
                    }
                } catch (e: Exception) {
                    Log.w("ReaderViewModel", "Failed to fetch publication documents in following feed", e)
                }
            }

            _uiState.value = _uiState.value.copy(
                followingPosts = posts.distinctBy { it.uri }.sortedByDescending { it.publishedAt },
                isLoadingFollowing = false,
                hasMoreFollowing = followingCursors.isNotEmpty(),
            )
            val verifiedFollowing = verifyPosts(_uiState.value.followingPosts)
            _uiState.value = _uiState.value.copy(followingPosts = verifiedFollowing)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoadingFollowing = false,
                error = e.message
            )
        }
    }

    private suspend fun loadYoursFeed(session: uk.ewancroft.inkwell.data.repository.UserSessionInfo) {
        _uiState.value = _uiState.value.copy(isLoadingYours = true)
        try {
            val profile = runCatching { pdsRepository.getProfile(session.did) }.getOrNull()

            val response = pdsRepository.listRecords(
                did = session.did,
                collection = CollectionNsids.DOCUMENT,
                pdsUrl = session.pdsUrl
            )
            val docsJson = response["records"]?.jsonArray.orEmpty()
            val posts = docsJson.mapNotNull { docJson ->
                try {
                    val valueObj = docJson.jsonObject["value"]?.jsonObject ?: return@mapNotNull null
                    val docUri = docJson.jsonObject["uri"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    PostItem(
                        uri = docUri,
                        title = valueObj["title"]?.jsonPrimitive?.content ?: "Untitled",
                        description = valueObj["description"]?.jsonPrimitive?.contentOrNull,
                        publicationName = profile?.displayName ?: profile?.handle,
                        publishedAt = valueObj["publishedAt"]?.jsonPrimitive?.content ?: "",
                        coverUrl = valueObj["coverImage"]?.jsonObject?.get("link")?.jsonPrimitive?.content
                            ?: valueObj["coverImage"]?.jsonObject?.get("\$link")?.jsonPrimitive?.content,
                        site = valueObj["site"]?.jsonPrimitive?.content ?: "",
                        authorDisplayName = profile?.displayName,
                        authorAvatar = profile?.avatar,
                    )
                } catch (e: Exception) {
                    Log.w("ReaderViewModel", "Failed to parse document in yours feed", e)
                    null
                }
            }

            _uiState.value = _uiState.value.copy(
                yoursPosts = posts.sortedByDescending { it.publishedAt },
                isLoadingYours = false
            )
            val verifiedYours = verifyPosts(_uiState.value.yoursPosts)
            _uiState.value = _uiState.value.copy(yoursPosts = verifiedYours)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoadingYours = false,
                error = e.message
            )
        }
    }
}

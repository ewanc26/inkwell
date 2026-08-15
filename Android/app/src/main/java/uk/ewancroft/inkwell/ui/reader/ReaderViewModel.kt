package uk.ewancroft.inkwell.ui.reader

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
import uk.ewancroft.inkwell.data.model.common.AtUri
import uk.ewancroft.inkwell.data.repository.PdsRepository
import javax.inject.Inject

data class PostItem(
    val uri: String,
    val title: String,
    val description: String?,
    val publicationName: String?,
    val date: String,
    val coverUrl: String?,
    val site: String,
    val authorDisplayName: String? = null,
    val authorAvatar: String? = null,
)

data class ReaderUiState(
    val followingPosts: List<PostItem> = emptyList(),
    val yoursPosts: List<PostItem> = emptyList(),
    val isLoadingFollowing: Boolean = false,
    val isLoadingYours: Boolean = false,
    val error: String? = null,
    val selectedTab: Int = 0
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val pdsRepository: PdsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun selectTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }

    fun loadData() {
        viewModelScope.launch {
            val session = pdsRepository.getSession()
            if (session != null) {
                loadFollowingFeed(session)
                loadYoursFeed(session)
            }
        }
    }

    private suspend fun loadFollowingFeed(session: uk.ewancroft.inkwell.data.repository.UserSessionInfo) {
        _uiState.value = _uiState.value.copy(isLoadingFollowing = true, error = null)
        try {
            val subscriptionsResponse = pdsRepository.listRecords(
                did = session.did,
                collection = "site.standard.graph.subscription",
                pdsUrl = session.pdsUrl
            )

            val subscriptionsJson = subscriptionsResponse["records"]?.jsonArray.orEmpty()

            val didToProfile = mutableMapOf<String, uk.ewancroft.inkwell.data.model.bluesky.BlueskyProfile>()
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
                        collection = "site.standard.document"
                    )
                    val docsJson = docsResponse["records"]?.jsonArray.orEmpty()
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
                                date = docValue["publishedAt"]?.jsonPrimitive?.content?.take(10) ?: "",
                                coverUrl = docValue["coverImage"]?.jsonObject?.get("link")?.jsonPrimitive?.content
                                    ?: docValue["coverImage"]?.jsonObject?.get("\$link")?.jsonPrimitive?.content,
                                site = docValue["site"]?.jsonPrimitive?.content ?: "",
                                authorDisplayName = profile?.displayName,
                                authorAvatar = profile?.avatar,
                            ))
                        } catch (_: Exception) {}
                    }
                } catch (_: Exception) {}
            }

            _uiState.value = _uiState.value.copy(
                followingPosts = posts.distinctBy { it.uri },
                isLoadingFollowing = false
            )
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
                collection = "site.standard.document",
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
                        date = valueObj["publishedAt"]?.jsonPrimitive?.content?.take(10) ?: "",
                        coverUrl = valueObj["coverImage"]?.jsonObject?.get("link")?.jsonPrimitive?.content
                            ?: valueObj["coverImage"]?.jsonObject?.get("\$link")?.jsonPrimitive?.content,
                        site = valueObj["site"]?.jsonPrimitive?.content ?: "",
                        authorDisplayName = profile?.displayName,
                        authorAvatar = profile?.avatar,
                    )
                } catch (_: Exception) { null }
            }

            _uiState.value = _uiState.value.copy(
                yoursPosts = posts,
                isLoadingYours = false
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoadingYours = false,
                error = e.message
            )
        }
    }
}

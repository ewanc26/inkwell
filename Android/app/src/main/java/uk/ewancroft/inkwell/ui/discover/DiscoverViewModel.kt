package uk.ewancroft.inkwell.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import uk.ewancroft.inkwell.data.model.common.SearchActorResponse
import uk.ewancroft.inkwell.data.model.common.SearchActorResult
import uk.ewancroft.inkwell.data.model.common.SearchResponse
import uk.ewancroft.inkwell.data.model.common.SearchResult
import uk.ewancroft.inkwell.data.model.common.PublicationResult
import uk.ewancroft.inkwell.shared.content.SearchBackendUrl
import uk.ewancroft.inkwell.data.repository.PdsRepository
import uk.ewancroft.inkwell.data.remote.readBoundedUtf8
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** Which facet of the Standard.site network a Discover search targets. */
enum class DiscoverSearchScope {
    DOCUMENTS,
    PUBLICATIONS,
}

data class DiscoverUiState(
    val query: String = "",
    val scope: DiscoverSearchScope = DiscoverSearchScope.DOCUMENTS,
    val results: List<SearchResult> = emptyList(),
    val actors: List<SearchActorResult> = emptyList(),
    // Distinct publications aggregated from [results] for the Publications scope.
    val publications: List<PublicationResult> = emptyList(),
    val isSearching: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val pdsRepository: PdsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun onQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
    }

    fun onScopeChanged(scope: DiscoverSearchScope) {
        if (scope == _uiState.value.scope) return
        _uiState.value = _uiState.value.copy(
            scope = scope,
            results = emptyList(),
            actors = emptyList(),
            publications = emptyList(),
        )
    }

    fun search() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) return
        val scope = _uiState.value.scope

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true, error = null)
            try {
                // Publication records are returned by the normal corpus;
                // the API supports keyword/semantic/hybrid modes, not a
                // client-invented `publications` mode.
                val mode = SearchBackendUrl.KEYWORD_MODE
                val searchUrl = "${SearchBackendUrl.BASE}/search?q=${
                    java.net.URLEncoder.encode(query, "UTF-8")
                }&mode=$mode&limit=40&format=v2"

                val searchBody = withContext(Dispatchers.IO) {
                    val searchRequest = Request.Builder().url(searchUrl).get().build()
                    client.newCall(searchRequest).execute().use {
                        if (!it.isSuccessful) throw IllegalStateException("Search returned HTTP ${it.code}")
                            it.body?.readBoundedUtf8() ?: throw IllegalStateException("Search returned an empty response")
                    }
                }

                val searchResponse = withContext(Dispatchers.IO) {
                    json.decodeFromString<SearchResponse>(searchBody)
                }

                // Actor (Bluesky) search only applies to the document scope.
                val actorResponse = if (scope == DiscoverSearchScope.DOCUMENTS) {
                    val actorsUrl = "${SearchBackendUrl.PUBLIC_APPVIEW}/xrpc/app.bsky.actor.searchActorsTypeahead?q=${
                        java.net.URLEncoder.encode(query, "UTF-8")
                    }&limit=10"
                    val actorsBody = withContext(Dispatchers.IO) {
                        val actorsRequest = Request.Builder().url(actorsUrl).get().build()
                        client.newCall(actorsRequest).execute().use {
                            if (!it.isSuccessful) throw IllegalStateException("Actor search returned HTTP ${it.code}")
                            it.body?.readBoundedUtf8() ?: throw IllegalStateException("Actor search returned an empty response")
                        }
                    }
                    withContext(Dispatchers.IO) {
                        json.decodeFromString<SearchActorResponse>(actorsBody)
                    }
                } else {
                    SearchActorResponse()
                }

                val publications = if (scope == DiscoverSearchScope.PUBLICATIONS) {
                    coroutineScope {
                        searchResponse.results.filter { it.isPublication }.map { result ->
                            async(Dispatchers.IO) {
                                runCatching {
                                    val value = pdsRepository.getRecord(result.uri)["value"]?.jsonObject
                                        ?: return@runCatching null
                                    val url = value["url"]?.jsonPrimitive?.contentOrNull
                                        ?: return@runCatching null
                                    PublicationResult(
                                        uri = result.uri,
                                        name = value["name"]?.jsonPrimitive?.contentOrNull ?: result.title,
                                        domain = url,
                                        url = url,
                                        did = result.did,
                                        coverImage = value["icon"]?.jsonPrimitive?.contentOrNull
                                            ?: result.coverImage,
                                    )
                                }.getOrNull()
                            }
                        }.awaitAll().filterNotNull()
                    }
                } else {
                    emptyList()
                }

                _uiState.value = _uiState.value.copy(
                    results = searchResponse.results,
                    actors = actorResponse.actors,
                    publications = publications,
                    isSearching = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    error = "Search failed: ${e.message}"
                )
            }
        }
    }

}

package uk.ewancroft.inkwell.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import uk.ewancroft.inkwell.data.model.common.SearchActorResponse
import uk.ewancroft.inkwell.data.model.common.SearchActorResult
import uk.ewancroft.inkwell.data.model.common.SearchResponse
import uk.ewancroft.inkwell.data.model.common.SearchResult
import uk.ewancroft.inkwell.data.model.common.PublicationResult
import uk.ewancroft.inkwell.shared.content.SearchBackendUrl
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
class DiscoverViewModel @Inject constructor() : ViewModel() {

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
                val mode = if (scope == DiscoverSearchScope.PUBLICATIONS) {
                    SearchBackendUrl.PUBLICATIONS_MODE
                } else {
                    SearchBackendUrl.KEYWORD_MODE
                }
                val searchUrl = "${SearchBackendUrl.BASE}/search?q=${
                    java.net.URLEncoder.encode(query, "UTF-8")
                }&mode=$mode&limit=40&format=v2"

                val searchBody = withContext(Dispatchers.IO) {
                    val searchRequest = Request.Builder().url(searchUrl).get().build()
                    client.newCall(searchRequest).execute().use { it.body!!.string() }
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
                        client.newCall(actorsRequest).execute().use { it.body!!.string() }
                    }
                    withContext(Dispatchers.IO) {
                        json.decodeFromString<SearchActorResponse>(actorsBody)
                    }
                } else {
                    SearchActorResponse()
                }

                // The backend indexes documents, not publications, so the
                // Publications scope reconstructs distinct publications by
                // grouping results that share an author and origin domain.
                val publications = if (scope == DiscoverSearchScope.PUBLICATIONS) {
                    aggregatePublications(searchResponse.results)
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

    /**
     * Collapses document search results into distinct publications. Results
     * from the same author DID and `basePath` (origin domain) are one
     * publication; the first cover image wins as the publication thumbnail.
     */
    private fun aggregatePublications(results: List<SearchResult>): List<PublicationResult> {
        val grouped = results.groupBy { it.did to (it.basePath ?: "") }
        return grouped.mapNotNull { (key, items) ->
            val domain = key.second
            if (domain.isBlank()) return@mapNotNull null
            val first = items.first()
            PublicationResult(
                name = domain,
                domain = domain,
                url = "https://$domain",
                did = key.first,
                coverImage = items.firstNotNullOfOrNull { it.coverImage },
            )
        }.sortedBy { it.name }
    }
}

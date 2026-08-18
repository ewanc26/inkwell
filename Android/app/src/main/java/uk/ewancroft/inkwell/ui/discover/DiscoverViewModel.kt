package uk.ewancroft.inkwell.ui.discover

import android.util.Log
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
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import uk.ewancroft.inkwell.data.model.common.SearchResponse
import uk.ewancroft.inkwell.data.model.common.SearchResult
import uk.ewancroft.inkwell.data.repository.PdsRepository
import uk.ewancroft.inkwell.data.repository.createSubscription
import uk.ewancroft.inkwell.data.repository.deleteSubscription
import uk.ewancroft.inkwell.data.repository.fetchSubscriptions
import uk.ewancroft.inkwell.shared.AtUri
import uk.ewancroft.inkwell.shared.content.SearchBackendUrl
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class DiscoverUiState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val error: String? = null,
    // publicationUri -> subscription record key, for publications the
    // signed-in user already subscribes to.
    val subscriptions: Map<String, String> = emptyMap(),
    // Publication URIs with an in-flight subscribe/unsubscribe call.
    val pendingSubscriptions: Set<String> = emptySet(),
    val subscriptionError: String? = null,
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

    init {
        loadSubscriptions()
    }


    fun onQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
    }

    fun search() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true, error = null)
            try {
                val url = "${SearchBackendUrl.BASE}/search?q=${
                    java.net.URLEncoder.encode(query, "UTF-8")
                }&mode=keyword&limit=40&format=v2"
                val body = withContext(Dispatchers.IO) {
                    val request = Request.Builder().url(url).get().build()
                    client.newCall(request).execute().use { it.body!!.string() }
                }
                val searchResponse = withContext(Dispatchers.IO) {
                    json.decodeFromString<SearchResponse>(body)
                }
                _uiState.value = _uiState.value.copy(
                    results = searchResponse.results,
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

    /** Loads the signed-in user's existing subscriptions so publication rows can show "Subscribed". */
    fun loadSubscriptions() {
        viewModelScope.launch {
            val session = pdsRepository.getSession() ?: return@launch
            try {
                val entries = pdsRepository.fetchSubscriptions(session.did, session.pdsUrl)
                _uiState.value = _uiState.value.copy(
                    subscriptions = entries.associate { it.publicationUri to it.rkey }
                )
            } catch (e: Exception) {
                // Non-fatal: rows just won't reflect subscribed state until retried.
                Log.d("DiscoverVM", "Failed to load subscriptions", e)
            }
        }
    }

    /** Subscribes to, or unsubscribes from, the publication behind [result]. */
    fun toggleSubscription(result: SearchResult) {
        val publicationUri = result.uri
        val state = _uiState.value
        if (publicationUri in state.pendingSubscriptions) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                pendingSubscriptions = _uiState.value.pendingSubscriptions + publicationUri,
                subscriptionError = null,
            )
            try {
                val existingRkey = state.subscriptions[publicationUri]
                if (existingRkey != null) {
                    pdsRepository.deleteSubscription(existingRkey)
                    _uiState.value = _uiState.value.copy(
                        subscriptions = _uiState.value.subscriptions - publicationUri
                    )
                } else {
                    val created = pdsRepository.createSubscription(publicationUri)
                    val newUri = created["uri"]?.jsonPrimitive?.content
                    val rkey = newUri?.let { AtUri.parse(it)?.recordKey }
                    if (rkey != null) {
                        _uiState.value = _uiState.value.copy(
                            subscriptions = _uiState.value.subscriptions + (publicationUri to rkey)
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    subscriptionError = e.message ?: "Failed to update subscription"
                )
            } finally {
                _uiState.value = _uiState.value.copy(
                    pendingSubscriptions = _uiState.value.pendingSubscriptions - publicationUri
                )
            }
        }
    }

    fun dismissSubscriptionError() {
        _uiState.value = _uiState.value.copy(subscriptionError = null)
    }
}

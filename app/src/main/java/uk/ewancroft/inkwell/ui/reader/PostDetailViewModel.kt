package uk.ewancroft.inkwell.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive
import uk.ewancroft.inkwell.data.model.common.AtUri
import uk.ewancroft.inkwell.data.remote.ConstellationClient
import uk.ewancroft.inkwell.data.repository.PdsRepository
import javax.inject.Inject

data class PostDetailUiState(
    val uri: String = "",
    val isRecommended: Boolean = false,
    val recommendRkey: String? = null,
    val recommendCount: Int = 0,
    val isLoadingRecommendState: Boolean = false,
    val hasLoadedRecommendState: Boolean = false,
    val isTogglingRecommend: Boolean = false,
    val error: String? = null,
)

/**
 * Backs [PostDetailScreen]'s recommend/unrecommend toggle and count.
 *
 * The signed-in user's own recommend state is checked against their local
 * repo (``PdsRepository.fetchRecommends``); the global count comes from the
 * Constellation backlink index (``ConstellationClient.getRecommendCount``)
 * since recommends from other users live in their own repos and are only
 * discoverable through the network-wide index. Mirrors Inkwell iOS
 * LoginStateManager.createRecommend/fetchRecommends/fetchRecommendCount/
 * deleteRecommend.
 */
@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val pdsRepository: PdsRepository,
    private val constellationClient: ConstellationClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PostDetailUiState())
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    /** Loads recommend state + count for [documentUri]. Safe to call repeatedly (e.g. on recomposition). */
    fun load(documentUri: String) {
        val current = _uiState.value
        if (current.uri == documentUri && (current.isLoadingRecommendState || current.hasLoadedRecommendState)) {
            return // already loaded (or loading) for this document
        }
        _uiState.value = PostDetailUiState(uri = documentUri, isLoadingRecommendState = true)

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
            _uiState.value = _uiState.value.copy(isTogglingRecommend = true, error = null)
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
                    error = e.message ?: "Failed to update recommendation",
                )
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

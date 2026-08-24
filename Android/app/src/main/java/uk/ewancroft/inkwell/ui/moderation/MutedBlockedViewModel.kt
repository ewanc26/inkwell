package uk.ewancroft.inkwell.ui.moderation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uk.ewancroft.inkwell.data.repository.BlockedActorEntry
import uk.ewancroft.inkwell.data.repository.ModeratedActor
import uk.ewancroft.inkwell.data.repository.PdsRepository
import uk.ewancroft.inkwell.data.repository.fetchBlockedActors
import uk.ewancroft.inkwell.data.repository.fetchMutedActors
import uk.ewancroft.inkwell.data.repository.unmuteActor
import uk.ewancroft.inkwell.data.repository.deleteBlock
import javax.inject.Inject

data class MutedBlockedUiState(
    val isLoading: Boolean = true,
    val mutes: List<ModeratedActor> = emptyList(),
    val blocks: List<BlockedActorEntry> = emptyList(),
    val error: String? = null,
    /** DIDs/rkeys currently mid-removal, to disable their row and show a spinner. */
    val removingKeys: Set<String> = emptySet(),
)

/** Backs the "Muted & Blocked Accounts" screen (Settings → Account). */
@HiltViewModel
class MutedBlockedViewModel @Inject constructor(
    private val pdsRepository: PdsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MutedBlockedUiState())
    val uiState: StateFlow<MutedBlockedUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val session = pdsRepository.getSession() ?: throw Exception("Not signed in")
                val mutes = pdsRepository.fetchMutedActors()
                val blocks = pdsRepository.fetchBlockedActors(session.did)
                _uiState.value = _uiState.value.copy(isLoading = false, mutes = mutes, blocks = blocks)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Failed to load.")
            }
        }
    }

    fun unmute(did: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(removingKeys = _uiState.value.removingKeys + did)
            try {
                pdsRepository.unmuteActor(did)
                _uiState.value = _uiState.value.copy(mutes = _uiState.value.mutes.filterNot { it.did == did })
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Failed to unmute.")
            } finally {
                _uiState.value = _uiState.value.copy(removingKeys = _uiState.value.removingKeys - did)
            }
        }
    }

    fun unblock(entry: BlockedActorEntry) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(removingKeys = _uiState.value.removingKeys + entry.rkey)
            try {
                pdsRepository.deleteBlock(entry.rkey)
                _uiState.value = _uiState.value.copy(blocks = _uiState.value.blocks.filterNot { it.rkey == entry.rkey })
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Failed to unblock.")
            } finally {
                _uiState.value = _uiState.value.copy(removingKeys = _uiState.value.removingKeys - entry.rkey)
            }
        }
    }
}

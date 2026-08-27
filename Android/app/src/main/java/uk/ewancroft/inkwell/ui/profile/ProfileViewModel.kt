package uk.ewancroft.inkwell.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uk.ewancroft.inkwell.data.model.bluesky.BlueskyProfile
import uk.ewancroft.inkwell.data.repository.PdsRepository
import uk.ewancroft.inkwell.data.repository.getProfile
import uk.ewancroft.inkwell.data.repository.submitReport
import uk.ewancroft.inkwell.shared.moderation.ReportReasonType
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = true,
    val profile: BlueskyProfile? = null,
    val error: String? = null,
    val reportError: String? = null,
    val reportConfirmation: String? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val pdsRepository: PdsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var profileLoadJob: Job? = null

    fun loadProfile(did: String) {
        profileLoadJob?.cancel()
        if (!did.startsWith("did:")) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                profile = null,
                error = "This account identifier is invalid.",
            )
            return
        }

        profileLoadJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                profile = null,
                error = null,
            )
            try {
                val profile = pdsRepository.getProfile(did)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    profile = profile,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error.message ?: "Couldn't load this profile",
                )
            }
        }
    }

    fun submitReport(
        did: String,
        reasonType: ReportReasonType,
        reason: String?,
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                reportError = null,
                reportConfirmation = null,
            )
            try {
                pdsRepository.submitReport(
                    subject = did,
                    recordCid = null,
                    reasonType = reasonType,
                    reason = reason,
                )
                _uiState.value = _uiState.value.copy(reportConfirmation = "Report submitted.")
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(
                    reportError = error.message ?: "Failed to submit report",
                )
            }
        }
    }

    fun dismissReportError() {
        _uiState.value = _uiState.value.copy(reportError = null)
    }

    fun dismissReportConfirmation() {
        _uiState.value = _uiState.value.copy(reportConfirmation = null)
    }
}

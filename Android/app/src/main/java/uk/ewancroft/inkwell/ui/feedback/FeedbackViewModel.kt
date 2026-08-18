package uk.ewancroft.inkwell.ui.feedback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uk.ewancroft.inkwell.data.repository.PdsRepository
import uk.ewancroft.inkwell.shared.feedback.UserInputLexicon
import javax.inject.Inject

data class FeedbackUiState(
    val title: String = "",
    val body: String = "",
    val selectedTag: String? = null,
    val isSubmitting: Boolean = false,
    val submitted: Boolean = false,
    val error: String? = null,
) {
    val canSubmit: Boolean get() = title.isNotBlank() && !isSubmitting
}

/** Backs the "Send Feedback" dialog, which posts to Inkwell's userinput.app board. */
@HiltViewModel
class FeedbackViewModel @Inject constructor(
    private val pdsRepository: PdsRepository,
) : ViewModel() {

    val tags: List<String> = UserInputLexicon.TAGS

    private val _uiState = MutableStateFlow(FeedbackUiState())
    val uiState: StateFlow<FeedbackUiState> = _uiState.asStateFlow()

    fun updateTitle(value: String) {
        _uiState.value = _uiState.value.copy(title = value, error = null)
    }

    fun updateBody(value: String) {
        _uiState.value = _uiState.value.copy(body = value, error = null)
    }

    fun selectTag(tag: String?) {
        _uiState.value = _uiState.value.copy(selectedTag = tag)
    }

    fun submit() {
        val state = _uiState.value
        if (!state.canSubmit) return
        _uiState.value = state.copy(isSubmitting = true, error = null)
        viewModelScope.launch {
            try {
                pdsRepository.submitFeedback(
                    title = state.title.trim(),
                    body = state.body.trim().ifBlank { null },
                    tag = state.selectedTag,
                )
                _uiState.value = _uiState.value.copy(isSubmitting = false, submitted = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    error = e.message ?: "Failed to send feedback.",
                )
            }
        }
    }

    fun reset() {
        _uiState.value = FeedbackUiState()
    }
}

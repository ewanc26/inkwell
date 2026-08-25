package uk.ewancroft.inkwell.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uk.ewancroft.inkwell.data.repository.PdsRepository
import javax.inject.Inject

/**
 * Backs the "Inkwell user" toggle in Settings. The toggle writes (or deletes)
 * a `uk.ewancroft.inkwell.user` record in the signed-in account's repository,
 * which the Inkwell website enumerates via Constellation to show a
 * "users already using Inkwell" carousel.
 */
@HiltViewModel
class UserLexiconViewModel @Inject constructor(
    private val pdsRepository: PdsRepository,
) : ViewModel() {

    private val _isInkwellUser = MutableStateFlow(false)
    val isInkwellUser: StateFlow<Boolean> = _isInkwellUser.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    /** Reads the current Inkwell-user record state, if signed in. */
    fun load() {
        viewModelScope.launch {
            if (pdsRepository.getSession() == null) return@launch
            _isInkwellUser.value = pdsRepository.getUserLexiconRkey() != null
        }
    }

    /** Creates or deletes the Inkwell-user record to match [enabled]. */
    fun setInkwellUser(enabled: Boolean) {
        if (_isBusy.value) return
        _isBusy.value = true
        viewModelScope.launch {
            try {
                if (enabled) {
                    pdsRepository.createUserLexicon(user = true)
                } else {
                    pdsRepository.deleteUserLexicon()
                }
                _isInkwellUser.value = enabled
            } catch (_: Exception) {
                // Leave state reflecting the last confirmed value on failure.
            } finally {
                _isBusy.value = false
            }
        }
    }
}

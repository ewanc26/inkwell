package uk.ewancroft.inkwell.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.kikin81.atproto.oauth.AtOAuth
import io.github.kikin81.atproto.oauth.OAuthSessionStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AuthUiState {
    data object Loading : AuthUiState
    data object LoggedOut : AuthUiState
    data class LoggedIn(val handle: String, val did: String?) : AuthUiState
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val oauth: AtOAuth,
    private val sessionStore: OAuthSessionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Loading)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _authUrl = MutableSharedFlow<String>()
    val authUrl: SharedFlow<String> = _authUrl.asSharedFlow()

    init {
        checkExistingSession()
    }

    private fun checkExistingSession() {
        viewModelScope.launch {
            val session = sessionStore.load()
            _uiState.value = if (session != null) {
                AuthUiState.LoggedIn(
                    handle = session.handle ?: session.did.orEmpty(),
                    did = session.did,
                )
            } else {
                AuthUiState.LoggedOut
            }
        }
    }

    fun beginLogin(handle: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.LoggedOut
            runCatching { oauth.beginLogin(handle) }
                .onSuccess { url -> _authUrl.emit(url) }
                .onFailure { t ->
                    Log.e(TAG, "beginLogin failed", t)
                    _uiState.value = AuthUiState.LoggedOut
                }
        }
    }

    fun completeLogin(redirectUri: String) {
        viewModelScope.launch {
            runCatching { oauth.completeLogin(redirectUri) }
                .onSuccess {
                    val session = sessionStore.load()
                    if (session != null) {
                        _uiState.value = AuthUiState.LoggedIn(
                            handle = session.handle ?: session.did.orEmpty(),
                            did = session.did,
                        )
                    } else {
                        _uiState.value = AuthUiState.LoggedOut
                    }
                }
                .onFailure { t ->
                    Log.e(TAG, "completeLogin failed", t)
                    _uiState.value = AuthUiState.LoggedOut
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            oauth.logout()
            _uiState.value = AuthUiState.LoggedOut
        }
    }

    companion object {
        private const val TAG = "AuthViewModel"
    }
}

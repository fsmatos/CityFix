package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.isec.amov.cityfix.data.AuthError
import pt.isec.amov.cityfix.data.AuthRepository

/**
 * Password reset ViewModel, created per route. [initialEmail] comes from ForgotPasswordRoute
 * (what was typed on Login); later edits are kept in [savedState].
 */
class ForgotPasswordViewModel(
    private val auth: AuthRepository,
    private val savedState: SavedStateHandle,
    initialEmail: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ForgotPasswordUiState(email = savedState[KEY_EMAIL] ?: initialEmail),
    )
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun onEmailChange(text: String) {
        savedState[KEY_EMAIL] = text
        _uiState.update { it.copy(email = text, emailError = null, error = null, sent = false) }
    }

    fun send() {
        val state = _uiState.value
        if (state.isBusy) return
        val email = state.email.trim()
        val emailError = emailErrorFor(email)
        if (emailError != null) {
            _uiState.update { it.copy(emailError = emailError) }
            return
        }

        _uiState.update { it.copy(isBusy = true, error = null, sent = false) }
        viewModelScope.launch {
            val error = auth.sendPasswordReset(email)
            _uiState.update {
                // Only connection problems are shown. Any other answer looks like success, so
                // nobody can tell from this screen whether an e-mail has an account.
                if (error == AuthError.NETWORK || error == AuthError.TOO_MANY_REQUESTS) {
                    it.copy(isBusy = false, error = error.messageRes())
                } else {
                    it.copy(isBusy = false, sent = true)
                }
            }
        }
    }

    private companion object {
        const val KEY_EMAIL = "email"
    }
}

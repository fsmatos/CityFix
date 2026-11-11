package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.data.AuthRepository
import pt.isec.amov.cityfix.data.SignInResult

/**
 * Login screen ViewModel, created per route (inside composable<LoginRoute>). The e-mail is also
 * kept in [savedState], so it survives process death; the password is not saved anywhere.
 * A successful sign-in only changes the session: CityFixApp's redirect then navigates home.
 */
class LoginViewModel(
    private val auth: AuthRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState(email = savedState[KEY_EMAIL] ?: ""))
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(text: String) {
        savedState[KEY_EMAIL] = text
        _uiState.update { it.copy(email = text, emailError = null, error = null) }
    }

    fun onPasswordChange(text: String) {
        _uiState.update { it.copy(password = text, passwordError = null, error = null) }
    }

    fun onToggleShowPassword() {
        _uiState.update { it.copy(showPassword = !it.showPassword) }
    }

    fun signIn() {
        val state = _uiState.value
        if (state.isBusy) return
        val email = state.email.trim()
        val emailError = emailErrorFor(email)
        val passwordError = if (state.password.isBlank()) R.string.login_error_password_blank else null
        if (emailError != null || passwordError != null) {
            _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }

        _uiState.update { it.copy(isBusy = true, error = null) }
        viewModelScope.launch {
            when (val result = auth.signIn(email, state.password)) {
                // Stays busy: the session changes and the redirect leaves this screen.
                SignInResult.Success -> Unit
                // D-009 slice 6 replaces this with the code step.
                is SignInResult.SecondFactorRequired -> _uiState.update {
                    it.copy(isBusy = false, error = R.string.login_error_second_factor_unsupported)
                }
                is SignInResult.Failure -> _uiState.update {
                    it.copy(isBusy = false, error = result.error.messageRes())
                }
            }
        }
    }

    private companion object {
        const val KEY_EMAIL = "email"
    }
}

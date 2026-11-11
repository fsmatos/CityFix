package pt.isec.amov.cityfix.viewmodel

import android.app.Activity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.data.AuthError
import pt.isec.amov.cityfix.data.AuthRepository
import pt.isec.amov.cityfix.data.CodeSendResult
import pt.isec.amov.cityfix.data.SecondFactorChallenge
import pt.isec.amov.cityfix.data.SignInResult

/**
 * Login screen ViewModel, created per route (inside composable<LoginRoute>). The e-mail is also
 * kept in [savedState], so it survives process death; the password is not saved anywhere.
 * A successful sign-in only changes the session: CityFixApp's redirect then navigates home.
 *
 * An enrolled account needs the SMS code: the [challenge] (opaque, from the repository) and the
 * [verificationId] stay in private fields, never in the UI state or saved state. After process
 * death they are gone, so the screen starts again at the credentials step with the e-mail kept.
 */
class LoginViewModel(
    private val auth: AuthRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState(email = savedState[KEY_EMAIL] ?: ""))
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var challenge: SecondFactorChallenge? = null
    private var verificationId: String? = null

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
                is SignInResult.SecondFactorRequired -> {
                    challenge = result.challenge
                    verificationId = null
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            step = LoginStep.SECOND_FACTOR,
                            maskedPhone = result.challenge.maskedPhone,
                            code = "",
                            codeError = null,
                            notice = null,
                            codeRequested = false,
                        )
                    }
                }
                is SignInResult.Failure -> _uiState.update {
                    it.copy(isBusy = false, error = result.error.messageRes())
                }
            }
        }
    }

    /**
     * Sends (or resends) the SMS code. The screen calls it once when the code step opens and on
     * Resend; [activity] goes straight to the repository (reCAPTCHA fallback) and is not kept.
     */
    fun sendCode(activity: Activity) {
        val current = challenge
        if (current == null) {
            backToCredentials()
            return
        }
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true, codeRequested = true, error = null, notice = null) }
        viewModelScope.launch {
            val result = auth.sendSecondFactorCode(activity, current)
            // The user went back to the credentials step meanwhile: drop the answer.
            if (challenge !== current) return@launch
            when (result) {
                is CodeSendResult.Sent -> {
                    verificationId = result.verificationId
                    _uiState.update { it.copy(isBusy = false, notice = R.string.mfa_code_sent) }
                }
                is CodeSendResult.Failure -> _uiState.update {
                    it.copy(isBusy = false, error = result.error.messageRes())
                }
            }
        }
    }

    fun onCodeChange(text: String) {
        val digits = text.filter { it.isDigit() }.take(SMS_CODE_DIGITS)
        _uiState.update { it.copy(code = digits, codeError = null, error = null) }
    }

    fun confirmCode() {
        val state = _uiState.value
        val current = challenge
        val id = verificationId
        if (state.isBusy || current == null) return
        if (id == null) {
            // No SMS went out yet (the send failed or timed out): ask for a new one.
            _uiState.update { it.copy(error = R.string.auth_error_code_not_sent) }
            return
        }
        if (state.code.length != SMS_CODE_DIGITS) {
            _uiState.update { it.copy(codeError = R.string.mfa_error_code_length) }
            return
        }
        _uiState.update { it.copy(isBusy = true, error = null, notice = null) }
        viewModelScope.launch {
            val error = auth.resolveSecondFactor(current, id, state.code)
            if (challenge !== current) return@launch
            when (error) {
                // Stays busy: the session changes and the redirect leaves this screen.
                null -> Unit
                AuthError.WRONG_CODE -> _uiState.update {
                    it.copy(isBusy = false, codeError = R.string.auth_error_wrong_code)
                }
                else -> _uiState.update { it.copy(isBusy = false, error = error.messageRes()) }
            }
        }
    }

    /** Back from the code step: the challenge is dropped, the e-mail and password stay. */
    fun backToCredentials() {
        challenge = null
        verificationId = null
        _uiState.update {
            it.copy(
                step = LoginStep.CREDENTIALS,
                isBusy = false,
                maskedPhone = "",
                code = "",
                codeError = null,
                error = null,
                notice = null,
                codeRequested = false,
            )
        }
    }

    private companion object {
        const val KEY_EMAIL = "email"
    }
}

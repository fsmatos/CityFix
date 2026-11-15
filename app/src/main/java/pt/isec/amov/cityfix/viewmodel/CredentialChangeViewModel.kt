package pt.isec.amov.cityfix.viewmodel

import android.app.Activity
import androidx.annotation.StringRes
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
import pt.isec.amov.cityfix.data.UsersRepository
import pt.isec.amov.cityfix.utils.PasswordProblem
import pt.isec.amov.cityfix.utils.passwordProblem

/**
 * Change password / Change e-mail ViewModel (D-011), created per route. Steps FORM -> CODE ->
 * DONE, modelled on LoginViewModel: Submit re-authenticates with the current password, which
 * for an enrolled account (every account, decision 0015) asks for the SMS code; once the code
 * is accepted the change runs at once.
 *
 * The [challenge] and [verificationId] stay in private fields, never in the UI or saved state.
 * The passwords are never saved; after process death the form step returns empty.
 */
class CredentialChangeViewModel(
    private val change: CredentialChange,
    private val isOperator: Boolean,
    private val auth: AuthRepository,
    private val users: UsersRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CredentialChangeUiState(change = change))
    val uiState: StateFlow<CredentialChangeUiState> = _uiState.asStateFlow()

    private var challenge: SecondFactorChallenge? = null
    private var verificationId: String? = null

    fun onCurrentPasswordChange(text: String) {
        _uiState.update { it.copy(currentPassword = text, currentPasswordError = null, error = null) }
    }

    fun onNewPasswordChange(text: String) {
        _uiState.update {
            val changed = it.copy(newPassword = text, error = null)
            changed.copy(
                newPasswordError = ifTyped(text, newPasswordError(changed)),
                confirmError = ifTyped(changed.confirmPassword, confirmError(changed)),
            )
        }
    }

    fun onConfirmPasswordChange(text: String) {
        _uiState.update {
            val changed = it.copy(confirmPassword = text, error = null)
            changed.copy(confirmError = ifTyped(text, confirmError(changed)))
        }
    }

    fun onToggleShowPassword() {
        _uiState.update { it.copy(showPassword = !it.showPassword) }
    }

    /** Checks the form, then re-authenticates with the current password. */
    fun submit() {
        val state = _uiState.value
        if (state.isBusy || state.step != CredentialStep.FORM) return
        val currentError = if (state.currentPassword.isBlank()) R.string.credential_error_current_blank else null
        val newError = newPasswordError(state)
        val confirmError = confirmError(state)
        if (currentError != null || newError != null || confirmError != null) {
            _uiState.update {
                it.copy(currentPasswordError = currentError, newPasswordError = newError, confirmError = confirmError)
            }
            return
        }

        _uiState.update { it.copy(isBusy = true, error = null) }
        viewModelScope.launch {
            when (val result = auth.reauthenticate(state.currentPassword)) {
                is SignInResult.SecondFactorRequired -> {
                    challenge = result.challenge
                    verificationId = null
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            step = CredentialStep.CODE,
                            maskedPhone = result.challenge.maskedPhone,
                            code = "",
                            codeError = null,
                            notice = null,
                            codeRequested = false,
                        )
                    }
                }
                // An account without a factor (the session normally prevents it): no code needed.
                SignInResult.Success -> performChange()
                is SignInResult.Failure -> _uiState.update {
                    if (result.error == AuthError.INVALID_CREDENTIALS) {
                        it.copy(isBusy = false, currentPasswordError = R.string.credential_error_current_wrong)
                    } else {
                        it.copy(isBusy = false, error = result.error.messageRes())
                    }
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
            backToForm()
            return
        }
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true, codeRequested = true, error = null, notice = null) }
        viewModelScope.launch {
            val result = auth.sendSecondFactorCode(activity, current)
            // The user went back to the form meanwhile: drop the answer.
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

    /** Completes the re-authentication with the typed code, then runs the change. */
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
                null -> performChange()
                AuthError.WRONG_CODE -> _uiState.update {
                    it.copy(isBusy = false, codeError = R.string.auth_error_wrong_code)
                }
                else -> _uiState.update { it.copy(isBusy = false, error = error.messageRes()) }
            }
        }
    }

    /** Back from the code step: the challenge is dropped, the typed values stay. */
    fun backToForm() {
        challenge = null
        verificationId = null
        _uiState.update {
            it.copy(
                step = CredentialStep.FORM,
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

    /** Runs right after a successful re-authentication; [isBusy] is still true. */
    private suspend fun performChange() {
        challenge = null
        verificationId = null
        val error = auth.changePassword(_uiState.value.newPassword)
        _uiState.update {
            when (error) {
                null -> it.copy(isBusy = false, step = CredentialStep.DONE, error = null, notice = null)
                AuthError.WEAK_PASSWORD -> it.backOnForm().copy(newPasswordError = R.string.auth_error_weak_password)
                else -> it.backOnForm().copy(error = error.messageRes())
            }
        }
    }

    private fun CredentialChangeUiState.backOnForm(): CredentialChangeUiState = copy(
        step = CredentialStep.FORM,
        isBusy = false,
        maskedPhone = "",
        code = "",
        codeError = null,
        notice = null,
        codeRequested = false,
    )

    /** A field's error while typing: an empty field shows none until Submit (as on Register). */
    @StringRes
    private fun ifTyped(text: String, @StringRes error: Int?): Int? = if (text.isEmpty()) null else error

    @StringRes
    private fun newPasswordError(state: CredentialChangeUiState): Int? = when (passwordProblem(state.newPassword)) {
        PasswordProblem.TOO_SHORT -> R.string.register_error_password_short
        PasswordProblem.NO_LETTER -> R.string.register_error_password_letter
        PasswordProblem.NO_DIGIT -> R.string.register_error_password_digit
        null -> null
    }

    @StringRes
    private fun confirmError(state: CredentialChangeUiState): Int? =
        if (state.confirmPassword == state.newPassword) null else R.string.register_error_confirm
}

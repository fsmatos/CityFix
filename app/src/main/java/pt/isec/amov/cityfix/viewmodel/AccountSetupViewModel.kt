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
import pt.isec.amov.cityfix.data.AuthAccount
import pt.isec.amov.cityfix.data.AuthError
import pt.isec.amov.cityfix.data.AuthRepository
import pt.isec.amov.cityfix.data.CodeSendResult
import pt.isec.amov.cityfix.utils.isPortugueseMobile

/**
 * Account setup ViewModel, created per route (inside composable<AccountSetupRoute>).
 *
 * The step follows the same account the session uses: not verified -> VERIFY_EMAIL; verified
 * and no code requested -> PHONE; a code requested -> CODE. When the phone is linked (F-018:
 * verified once, at sign-up, not asked again at login) the session becomes SignedIn and CityFixApp's redirect leaves this
 * screen. The phone digits and the verification id are kept in [savedState], so the step and
 * the typed number survive process death. The Activity passed to [sendCode] goes straight to
 * the repository and is never stored.
 */
class AccountSetupViewModel(
    private val auth: AuthRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    /** Set once the SMS is sent; the code step needs it to confirm. */
    private var verificationId: String? = savedState[KEY_VERIFICATION_ID]

    private val _uiState = MutableStateFlow(
        AccountSetupUiState(phoneDigits = savedState[KEY_PHONE] ?: ""),
    )
    val uiState: StateFlow<AccountSetupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            auth.account.collect { account ->
                if (account == null) return@collect
                _uiState.update { it.copy(email = account.email, step = stepFor(account)) }
                // The verification e-mail goes out once, automatically, on the first visit.
                if (!account.isEmailVerified && savedState.get<Boolean>(KEY_EMAIL_SENT) != true) {
                    sendVerificationEmail()
                }
            }
        }
    }

    fun sendVerificationEmail() {
        if (_uiState.value.isBusy) return
        savedState[KEY_EMAIL_SENT] = true
        _uiState.update { it.copy(isBusy = true, error = null, notice = null) }
        viewModelScope.launch {
            val error = auth.sendEmailVerification()
            _uiState.update {
                if (error == null) {
                    it.copy(isBusy = false, notice = R.string.setup_verification_sent)
                } else {
                    it.copy(isBusy = false, error = error.messageRes())
                }
            }
        }
    }

    /** "I have verified": reloads the account. A verified account moves to the phone step by itself. */
    fun checkVerified() {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true, error = null, notice = null) }
        viewModelScope.launch {
            val error = auth.reloadAccount()
            val verified = auth.account.value?.isEmailVerified == true
            _uiState.update {
                when {
                    error != null -> it.copy(isBusy = false, error = error.messageRes())
                    !verified -> it.copy(isBusy = false, error = R.string.setup_not_verified)
                    else -> it.copy(isBusy = false)
                }
            }
        }
    }

    /** Keeps digits only, at most [PHONE_DIGITS]. */
    fun onPhoneChange(text: String) {
        val digits = text.filter { it.isDigit() }.take(PHONE_DIGITS)
        savedState[KEY_PHONE] = digits
        _uiState.update { it.copy(phoneDigits = digits, phoneError = null, error = null) }
    }

    /** Keeps digits only, at most [SMS_CODE_DIGITS]. */
    fun onCodeChange(text: String) {
        val digits = text.filter { it.isDigit() }.take(SMS_CODE_DIGITS)
        _uiState.update { it.copy(code = digits, codeError = null, error = null) }
    }

    /** Sends (or resends) the verification SMS to +351 and the typed digits. */
    fun sendCode(activity: Activity) {
        val state = _uiState.value
        if (state.isBusy) return
        if (!isPortugueseMobile(state.phoneDigits)) {
            _uiState.update { it.copy(phoneError = R.string.setup_error_phone) }
            return
        }
        _uiState.update { it.copy(isBusy = true, error = null, notice = null, codeError = null) }
        viewModelScope.launch {
            when (val result = auth.sendPhoneCode(activity, "+351${state.phoneDigits}")) {
                is CodeSendResult.Sent -> {
                    setVerificationId(result.verificationId)
                    _uiState.update {
                        it.copy(isBusy = false, code = "", step = SetupStep.CODE, notice = R.string.mfa_code_sent)
                    }
                }
                is CodeSendResult.Failure -> _uiState.update {
                    if (result.error == AuthError.INVALID_PHONE_NUMBER) {
                        // Shown on the field, like the local format check.
                        it.copy(isBusy = false, phoneError = result.error.messageRes())
                    } else {
                        it.copy(isBusy = false, error = result.error.messageRes())
                    }
                }
            }
        }
    }

    fun confirmCode() {
        val state = _uiState.value
        val id = verificationId
        if (state.isBusy || id == null) return
        if (state.code.length != SMS_CODE_DIGITS) {
            _uiState.update { it.copy(codeError = R.string.mfa_error_code_length) }
            return
        }
        _uiState.update { it.copy(isBusy = true, error = null, notice = null) }
        viewModelScope.launch {
            when (val error = auth.linkPhone(id, state.code)) {
                // Stays busy: the session changes and the redirect leaves this screen.
                null -> Unit
                AuthError.WRONG_CODE -> _uiState.update {
                    it.copy(isBusy = false, codeError = R.string.auth_error_wrong_code)
                }
                // Another account has this phone: back to the phone step to type another number.
                AuthError.PHONE_IN_USE -> {
                    setVerificationId(null)
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            step = SetupStep.PHONE,
                            code = "",
                            phoneError = R.string.setup_error_phone_in_use,
                        )
                    }
                }
                // Linking needs a recent login. The message asks the user to log in again,
                // and Log out is on this screen and in the top bar.
                else -> _uiState.update { it.copy(isBusy = false, error = error.messageRes()) }
            }
        }
    }

    /** Back from the code step to the phone step, to type another number. */
    fun changeNumber() {
        if (_uiState.value.isBusy) return
        setVerificationId(null)
        _uiState.update { it.copy(step = SetupStep.PHONE, code = "", codeError = null, error = null, notice = null) }
    }

    private fun setVerificationId(id: String?) {
        verificationId = id
        savedState[KEY_VERIFICATION_ID] = id
    }

    private fun stepFor(account: AuthAccount): SetupStep = when {
        !account.isEmailVerified -> SetupStep.VERIFY_EMAIL
        verificationId == null -> SetupStep.PHONE
        else -> SetupStep.CODE
    }

    private companion object {
        const val KEY_PHONE = "phone"
        const val KEY_VERIFICATION_ID = "verificationId"
        const val KEY_EMAIL_SENT = "emailSent"
    }
}

package pt.isec.amov.cityfix.viewmodel

import androidx.annotation.StringRes

/** The login steps: e-mail and password, then the SMS code for an enrolled account. */
enum class LoginStep { CREDENTIALS, SECOND_FACTOR }

/** Login form. Errors are string resource ids, so the ViewModel holds no text. */
data class LoginUiState(
    val step: LoginStep = LoginStep.CREDENTIALS,
    val email: String = "",
    val password: String = "",
    val showPassword: Boolean = false,
    @StringRes val emailError: Int? = null,
    @StringRes val passwordError: Int? = null,
    /** The last call failed (wrong credentials, no network...). */
    @StringRes val error: Int? = null,
    /** The enrolled phone as Firebase masks it; set on the code step. */
    val maskedPhone: String = "",
    val code: String = "",
    @StringRes val codeError: Int? = null,
    /** A neutral result, such as "code sent". */
    @StringRes val notice: Int? = null,
    /** True once the first SMS of this code step was requested, so the screen asks only once. */
    val codeRequested: Boolean = false,
    /** True while Firebase answers; blocks a second submit. */
    val isBusy: Boolean = false,
)

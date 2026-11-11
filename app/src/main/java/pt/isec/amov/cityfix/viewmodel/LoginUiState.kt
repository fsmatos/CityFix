package pt.isec.amov.cityfix.viewmodel

import androidx.annotation.StringRes

/** Login form. Errors are string resource ids, so the ViewModel holds no text. */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val showPassword: Boolean = false,
    @StringRes val emailError: Int? = null,
    @StringRes val passwordError: Int? = null,
    /** The sign-in failed (wrong credentials, no network...). */
    @StringRes val error: Int? = null,
    /** True while Firebase answers; blocks a second submit. */
    val isBusy: Boolean = false,
)

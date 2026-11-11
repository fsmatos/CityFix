package pt.isec.amov.cityfix.viewmodel

import androidx.annotation.StringRes

/** Password reset form. */
data class ForgotPasswordUiState(
    val email: String = "",
    @StringRes val emailError: Int? = null,
    @StringRes val error: Int? = null,
    val isBusy: Boolean = false,
    /** The request went out. The same message is shown whether or not the account exists. */
    val sent: Boolean = false,
)

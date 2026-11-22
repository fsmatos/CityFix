package pt.isec.amov.cityfix.viewmodel

import androidx.annotation.StringRes

data class ForgotPasswordUiState(
    val email: String = "",
    @StringRes val emailError: Int? = null,
    @StringRes val error: Int? = null,
    val isBusy: Boolean = false,
    val sent: Boolean = false,
)

package pt.isec.amov.cityfix.viewmodel

import androidx.annotation.StringRes

enum class LoginStep { CREDENTIALS, SECOND_FACTOR }

data class LoginUiState(
    val step: LoginStep = LoginStep.CREDENTIALS,
    val email: String = "",
    val password: String = "",
    val showPassword: Boolean = false,
    @StringRes val emailError: Int? = null,
    @StringRes val passwordError: Int? = null,
    @StringRes val error: Int? = null,
    val maskedPhone: String = "",
    val code: String = "",
    @StringRes val codeError: Int? = null,
    @StringRes val notice: Int? = null,
    val codeRequested: Boolean = false,
    val isBusy: Boolean = false,
)

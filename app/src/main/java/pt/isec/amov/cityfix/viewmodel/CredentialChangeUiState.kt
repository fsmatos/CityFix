package pt.isec.amov.cityfix.viewmodel

import androidx.annotation.StringRes

enum class CredentialChange { EMAIL, PASSWORD }

enum class CredentialStep { FORM, CODE, DONE }

data class CredentialChangeUiState(
    val change: CredentialChange,
    val step: CredentialStep = CredentialStep.FORM,
    val currentPassword: String = "",
    val currentEmail: String = "",
    val newEmail: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val showPassword: Boolean = false,
    val isOperator: Boolean = false,
    val operatorDomain: String? = null,
    val isLoadingDomain: Boolean = false,
    val maskedPhone: String = "",
    val code: String = "",
    val codeRequested: Boolean = false,
    val isBusy: Boolean = false,
    @StringRes val currentPasswordError: Int? = null,
    @StringRes val newEmailError: Int? = null,
    @StringRes val newPasswordError: Int? = null,
    @StringRes val confirmError: Int? = null,
    @StringRes val codeError: Int? = null,
    @StringRes val error: Int? = null,
    @StringRes val notice: Int? = null,
)

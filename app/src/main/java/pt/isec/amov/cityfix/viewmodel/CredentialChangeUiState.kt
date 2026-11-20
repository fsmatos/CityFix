package pt.isec.amov.cityfix.viewmodel

import androidx.annotation.StringRes

/** Which credential the screen changes; the factory picks it per route. */
enum class CredentialChange { EMAIL, PASSWORD }

/** The form, the SMS code of the re-authentication, then the result. */
enum class CredentialStep { FORM, CODE, DONE }

/**
 * Change password / Change e-mail (D-011). Errors are string resource ids, so the ViewModel
 * holds no text. The passwords are never saved (process death clears them).
 */
data class CredentialChangeUiState(
    val change: CredentialChange,
    val step: CredentialStep = CredentialStep.FORM,
    val currentPassword: String = "",
    /** EMAIL: the account's e-mail now, shown read-only. */
    val currentEmail: String = "",
    /** EMAIL: the address the link goes to. */
    val newEmail: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val showPassword: Boolean = false,
    /** EMAIL: an operator's new e-mail must stay in the council's domain (brief I.3.A). */
    val isOperator: Boolean = false,
    /** The council's e-mail domain (lowercase, no '@'); null when missing or unreadable. */
    val operatorDomain: String? = null,
    /** True while the domain is read (operators only). */
    val isLoadingDomain: Boolean = false,
    /** Where the SMS code went (already masked by Firebase). */
    val maskedPhone: String = "",
    val code: String = "",
    /** True once the code step has sent its first SMS; stops a resend on rotation. */
    val codeRequested: Boolean = false,
    /** True while Firebase answers. */
    val isBusy: Boolean = false,
    @StringRes val currentPasswordError: Int? = null,
    /** `register_error_email_domain` is formatted with [operatorDomain] by the screen. */
    @StringRes val newEmailError: Int? = null,
    @StringRes val newPasswordError: Int? = null,
    @StringRes val confirmError: Int? = null,
    @StringRes val codeError: Int? = null,
    /** The last call failed (no network...). */
    @StringRes val error: Int? = null,
    /** A neutral line on the code step ("Code sent."). */
    @StringRes val notice: Int? = null,
)

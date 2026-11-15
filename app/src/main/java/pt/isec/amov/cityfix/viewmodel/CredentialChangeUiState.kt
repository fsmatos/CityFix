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
    val newPassword: String = "",
    val confirmPassword: String = "",
    val showPassword: Boolean = false,
    /** Where the SMS code went (already masked by Firebase). */
    val maskedPhone: String = "",
    val code: String = "",
    /** True once the code step has sent its first SMS; stops a resend on rotation. */
    val codeRequested: Boolean = false,
    /** True while Firebase answers. */
    val isBusy: Boolean = false,
    @StringRes val currentPasswordError: Int? = null,
    @StringRes val newPasswordError: Int? = null,
    @StringRes val confirmError: Int? = null,
    @StringRes val codeError: Int? = null,
    /** The last call failed (no network...). */
    @StringRes val error: Int? = null,
    /** A neutral line on the code step ("Code sent."). */
    @StringRes val notice: Int? = null,
)

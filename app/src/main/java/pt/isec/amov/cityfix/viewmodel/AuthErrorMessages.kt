package pt.isec.amov.cityfix.viewmodel

import androidx.annotation.StringRes
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.data.AuthError
import pt.isec.amov.cityfix.utils.isValidEmail

/** The message for each [AuthError], shared by the login, password reset, account setup and registration screens. */
@StringRes
fun AuthError.messageRes(): Int = when (this) {
    AuthError.INVALID_CREDENTIALS -> R.string.auth_error_invalid_credentials
    AuthError.USER_DISABLED -> R.string.auth_error_user_disabled
    AuthError.NETWORK -> R.string.auth_error_network
    AuthError.TOO_MANY_REQUESTS -> R.string.auth_error_too_many_requests
    AuthError.WRONG_CODE -> R.string.auth_error_wrong_code
    AuthError.CODE_EXPIRED -> R.string.auth_error_code_expired
    AuthError.CODE_NOT_SENT -> R.string.auth_error_code_not_sent
    AuthError.INVALID_PHONE_NUMBER -> R.string.setup_error_phone
    AuthError.PHONE_IN_USE -> R.string.setup_error_phone_in_use
    AuthError.RECENT_LOGIN_REQUIRED -> R.string.auth_error_recent_login
    AuthError.EMAIL_IN_USE -> R.string.auth_error_email_in_use
    AuthError.WEAK_PASSWORD -> R.string.auth_error_weak_password
    AuthError.UNKNOWN -> R.string.auth_error_unknown
}

/** The error for a typed e-mail, or null when it looks valid. Shared by Login and Forgot password. */
@StringRes
fun emailErrorFor(email: String): Int? = when {
    email.isBlank() -> R.string.login_error_email_blank
    !isValidEmail(email) -> R.string.login_error_email_invalid
    else -> null
}

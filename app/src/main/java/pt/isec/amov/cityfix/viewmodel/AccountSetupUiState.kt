package pt.isec.amov.cityfix.viewmodel

import androidx.annotation.StringRes

/** The steps of the account setup, in order. */
enum class SetupStep { VERIFY_EMAIL, PHONE, CODE }

/** Digits of a Portuguese mobile number, typed after the fixed +351 prefix. */
const val PHONE_DIGITS = 9

/** Digits of the SMS code Firebase sends. */
const val SMS_CODE_DIGITS = 6

/**
 * Account setup on the first login (D-009 Q1, Q8): verify the e-mail, then enrol a phone as the
 * second factor. Errors and notices are string resource ids.
 */
data class AccountSetupUiState(
    val step: SetupStep = SetupStep.VERIFY_EMAIL,
    val email: String = "",
    /** Only the 9 digits; the screen shows the +351 prefix. */
    val phoneDigits: String = "",
    val code: String = "",
    @StringRes val phoneError: Int? = null,
    @StringRes val codeError: Int? = null,
    /** A failed call (no network, could not send...). */
    @StringRes val error: Int? = null,
    /** A neutral result, such as "verification e-mail sent". */
    @StringRes val notice: Int? = null,
    val isBusy: Boolean = false,
)

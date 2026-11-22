package pt.isec.amov.cityfix.viewmodel

import androidx.annotation.StringRes

enum class SetupStep { VERIFY_EMAIL, PHONE, CODE }

const val PHONE_DIGITS = 9

const val SMS_CODE_DIGITS = 6

data class AccountSetupUiState(
    val step: SetupStep = SetupStep.VERIFY_EMAIL,
    val email: String = "",
    val phoneDigits: String = "",
    val code: String = "",
    @StringRes val phoneError: Int? = null,
    @StringRes val codeError: Int? = null,
    @StringRes val error: Int? = null,
    @StringRes val notice: Int? = null,
    val isBusy: Boolean = false,
)

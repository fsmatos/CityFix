package pt.isec.amov.cityfix.viewmodel

import androidx.annotation.StringRes
import pt.isec.amov.cityfix.data.RegistrationRole
import pt.isec.amov.cityfix.model.Coordinates
import pt.isec.amov.cityfix.utils.isInEmailDomain
import pt.isec.amov.cityfix.utils.isPortuguesePhone
import pt.isec.amov.cityfix.utils.isValidEmail
import pt.isec.amov.cityfix.utils.isValidName
import pt.isec.amov.cityfix.utils.isValidNif
import pt.isec.amov.cityfix.utils.passwordProblem

data class RegisterUiState(
    val role: RegistrationRole = RegistrationRole.CITIZEN,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val nif: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val showPassword: Boolean = false,
    val location: Coordinates? = null,
    val isLocating: Boolean = false,
    val locationPermissionDenied: Boolean = false,
    val impactDetection: Boolean = false,
    val photoPath: String? = null,
    val isCopyingPhoto: Boolean = false,
    @StringRes val photoNotice: Int? = null,
    val operatorDomain: String? = null,
    val isLoadingDomain: Boolean = true,
    @StringRes val nameError: Int? = null,
    @StringRes val emailError: Int? = null,
    @StringRes val phoneError: Int? = null,
    @StringRes val nifError: Int? = null,
    @StringRes val passwordError: Int? = null,
    @StringRes val confirmError: Int? = null,
    @StringRes val locationError: Int? = null,
    @StringRes val error: Int? = null,
    val showErrorSummary: Boolean = false,
    val isRegistering: Boolean = false,
    val registered: Boolean = false,
    val failed: Boolean = false,
) {
    val isValid: Boolean
        get() = isValidName(name) &&
            isValidEmail(email) &&
            (role == RegistrationRole.CITIZEN || (operatorDomain != null && isInEmailDomain(email, operatorDomain))) &&
            isPortuguesePhone(phone) &&
            isValidNif(nif) &&
            passwordProblem(password) == null &&
            confirmPassword == password &&
            (role == RegistrationRole.OPERATOR || location != null)
}

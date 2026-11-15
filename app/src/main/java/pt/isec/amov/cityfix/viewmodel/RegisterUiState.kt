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

/**
 * Register form (brief I.2.A, I.3.A). Errors are string resource ids, so the ViewModel holds no
 * text; `register_error_email_domain` is formatted with [operatorDomain] by the screen.
 */
data class RegisterUiState(
    val role: RegistrationRole = RegistrationRole.CITIZEN,
    val name: String = "",
    val email: String = "",
    /** Digits only, at most 9. */
    val phone: String = "",
    /** Digits only, at most 9. */
    val nif: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val showPassword: Boolean = false,
    /** Citizen only: the residence (brief I.2.A georeferencing). */
    val location: Coordinates? = null,
    /** True while "Use my location" waits for a fix. */
    val isLocating: Boolean = false,
    /** True after the location permission was refused; the screen shows Try again and Open settings. */
    val locationPermissionDenied: Boolean = false,
    /** Citizen only; off by default. */
    val impactDetection: Boolean = false,
    /** The optional photo: a local copy in filesDir/images, uploaded after the registration. */
    val photoPath: String? = null,
    /** True while a gallery pick is copied; the photo buttons and Submit wait for it. */
    val isCopyingPhoto: Boolean = false,
    /** A photo problem (copy failed, camera unavailable). */
    @StringRes val photoNotice: Int? = null,
    /** The council's e-mail domain (lowercase, no '@'); null when missing or unreadable. */
    val operatorDomain: String? = null,
    /** True until the domain read in the ViewModel's init has answered. */
    val isLoadingDomain: Boolean = true,
    @StringRes val nameError: Int? = null,
    @StringRes val emailError: Int? = null,
    @StringRes val phoneError: Int? = null,
    @StringRes val nifError: Int? = null,
    @StringRes val passwordError: Int? = null,
    @StringRes val confirmError: Int? = null,
    @StringRes val locationError: Int? = null,
    /** The last call failed (no network...). */
    @StringRes val error: Int? = null,
    /** True after a Submit with an invalid form, until the form becomes valid. */
    val showErrorSummary: Boolean = false,
    /** True while Firebase answers; holds CityFixApp's redirect and blocks a second submit. */
    val isRegistering: Boolean = false,
    /** True once the account exists; keeps Submit disabled until the redirect leaves the screen. */
    val registered: Boolean = false,
    /**
     * True after a failed Submit. The new Auth user (if one was created) is deleted, but the
     * session may still show it for a few frames; the screen keeps the redirect held until the
     * session is anonymous again, so the form and its error stay. Not saved: after process death
     * the redirect handles the account (D-010 Q11).
     */
    val failed: Boolean = false,
) {
    /** Every field rule for the role. Submit gates on this, so an invalid form never reaches Firebase. */
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

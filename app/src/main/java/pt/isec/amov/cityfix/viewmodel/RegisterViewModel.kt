package pt.isec.amov.cityfix.viewmodel

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.data.AttachmentStorage
import pt.isec.amov.cityfix.data.AuthError
import pt.isec.amov.cityfix.data.LocationRepository
import pt.isec.amov.cityfix.data.RegisterResult
import pt.isec.amov.cityfix.data.Registration
import pt.isec.amov.cityfix.data.RegistrationRole
import pt.isec.amov.cityfix.data.UsersRepository
import pt.isec.amov.cityfix.model.Coordinates
import pt.isec.amov.cityfix.utils.PasswordProblem
import pt.isec.amov.cityfix.utils.isInEmailDomain
import pt.isec.amov.cityfix.utils.isPortuguesePhone
import pt.isec.amov.cityfix.utils.isValidName
import pt.isec.amov.cityfix.utils.isValidNif
import pt.isec.amov.cityfix.utils.passwordProblem
import java.io.File

/**
 * Register screen ViewModel, created per route (inside composable<RegisterRoute>).
 *
 * Every field except the two passwords is also kept in [savedState], so the form survives process
 * death; rotation keeps the whole ViewModel. A field's error is recomputed as it is typed (only
 * once it is not empty, D-010 Q10); Submit shows every error. A successful registration signs the
 * new user in; CityFixApp holds its redirect while [RegisterUiState.isRegistering] is true, then
 * moves to Account setup. A citizen's location comes from [locationRepository] (F-012), asked
 * for at the point of use; its two coordinates are saved as Doubles. The optional photo is a local
 * copy ([attachmentStorage] copies a gallery pick off the main thread); it is deleted when the
 * form is left without registering, and otherwise handed to the upload worker.
 */
class RegisterViewModel(
    private val users: UsersRepository,
    private val locationRepository: LocationRepository,
    private val attachmentStorage: AttachmentStorage,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        RegisterUiState(
            role = RegistrationRole.entries.firstOrNull { it.name == savedState.get<String>(KEY_ROLE) }
                ?: RegistrationRole.CITIZEN,
            name = savedState[KEY_NAME] ?: "",
            email = savedState[KEY_EMAIL] ?: "",
            phone = savedState[KEY_PHONE] ?: "",
            nif = savedState[KEY_NIF] ?: "",
            impactDetection = savedState[KEY_IMPACT] ?: false,
            location = savedLocation(),
            photoPath = savedState.get<String>(KEY_PHOTO)?.takeIf { File(it).exists() },
        ),
    )
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    init {
        // Read once: the operator e-mail must be in this domain (brief I.3.A).
        viewModelScope.launch {
            val domain = users.operatorEmailDomain()
            update {
                val loaded = copy(operatorDomain = domain, isLoadingDomain = false)
                loaded.copy(emailError = ifTyped(email, emailError(loaded)))
            }
        }
    }

    fun onRoleSelected(role: RegistrationRole) {
        savedState[KEY_ROLE] = role.name
        // The e-mail rule depends on the role; the location is a citizen field.
        update {
            val changed = copy(role = role, locationError = null, error = null)
            changed.copy(emailError = ifTyped(email, emailError(changed)))
        }
    }

    fun onNameChange(text: String) {
        savedState[KEY_NAME] = text
        update {
            val changed = copy(name = text, error = null)
            changed.copy(nameError = ifTyped(text, nameError(changed)))
        }
    }

    fun onEmailChange(text: String) {
        savedState[KEY_EMAIL] = text
        update {
            val changed = copy(email = text, error = null)
            changed.copy(emailError = ifTyped(text, emailError(changed)))
        }
    }

    /** Keeps digits only, at most [FIELD_DIGITS]. */
    fun onPhoneChange(text: String) {
        val digits = text.filter { it.isDigit() }.take(FIELD_DIGITS)
        savedState[KEY_PHONE] = digits
        update {
            val changed = copy(phone = digits, error = null)
            changed.copy(phoneError = ifTyped(digits, phoneError(changed)))
        }
    }

    /** Keeps digits only, at most [FIELD_DIGITS]. */
    fun onNifChange(text: String) {
        val digits = text.filter { it.isDigit() }.take(FIELD_DIGITS)
        savedState[KEY_NIF] = digits
        update {
            val changed = copy(nif = digits, error = null)
            changed.copy(nifError = ifTyped(digits, nifError(changed)))
        }
    }

    /** Not saved: passwords never go to the saved state. */
    fun onPasswordChange(text: String) {
        update {
            val changed = copy(password = text, error = null)
            changed.copy(
                passwordError = ifTyped(text, passwordError(changed)),
                confirmError = ifTyped(confirmPassword, confirmError(changed)),
            )
        }
    }

    fun onConfirmPasswordChange(text: String) {
        update {
            val changed = copy(confirmPassword = text, error = null)
            changed.copy(confirmError = ifTyped(text, confirmError(changed)))
        }
    }

    fun onToggleShowPassword() {
        _uiState.update { it.copy(showPassword = !it.showPassword) }
    }

    fun onImpactDetectionChange(enabled: Boolean) {
        savedState[KEY_IMPACT] = enabled
        _uiState.update { it.copy(impactDetection = enabled) }
    }

    /** The answer to the permission dialog of "Use my location". */
    fun onLocationPermissionResult(granted: Boolean) {
        if (granted) {
            _uiState.update { it.copy(locationPermissionDenied = false, locationError = null) }
            refreshLocation()
        } else {
            _uiState.update { it.copy(locationPermissionDenied = true, locationError = null) }
        }
    }

    /** Reads one fix (any accuracy: the residence is not used for grouping). No fix keeps the previous one. */
    fun refreshLocation() {
        if (_uiState.value.isLocating) return
        _uiState.update { it.copy(isLocating = true, locationPermissionDenied = false) }
        viewModelScope.launch {
            val fix = locationRepository.currentLocation()
            if (fix != null) {
                savedState[KEY_LATITUDE] = fix.latitude
                savedState[KEY_LONGITUDE] = fix.longitude
            }
            update {
                copy(
                    location = fix ?: location,
                    locationError = if (fix == null) R.string.register_location_unavailable else null,
                    isLocating = false,
                    error = null,
                )
            }
        }
    }

    /** Gallery pick: copied into filesDir/images in viewModelScope, then it replaces the previous photo. */
    fun onPhotoPicked(uri: Uri) {
        if (_uiState.value.isCopyingPhoto) return
        _uiState.update { it.copy(isCopyingPhoto = true, photoNotice = null) }
        viewModelScope.launch {
            val path = attachmentStorage.copyToInternalStorage(listOf(uri)).firstOrNull()
            val state = _uiState.value
            when {
                path == null -> _uiState.update {
                    it.copy(isCopyingPhoto = false, photoNotice = R.string.register_photo_copy_failed)
                }
                // Submitted meanwhile: the copy is not part of this registration.
                state.isRegistering || state.registered -> {
                    File(path).delete()
                    _uiState.update { it.copy(isCopyingPhoto = false) }
                }
                else -> {
                    setPhoto(path)
                    _uiState.update { it.copy(isCopyingPhoto = false) }
                }
            }
        }
    }

    /** [path] is the camera's file in filesDir/images, already written. */
    fun onPhotoCaptured(path: String) {
        setPhoto(path)
    }

    fun onRemovePhoto() {
        setPhoto(null)
    }

    fun onCameraUnavailable() {
        _uiState.update { it.copy(photoNotice = R.string.report_camera_failed) }
    }

    /** Replaces the photo, deleting the previous local copy. */
    private fun setPhoto(path: String?) {
        val previous = _uiState.value.photoPath
        if (previous != null && previous != path) File(previous).delete()
        savedState[KEY_PHOTO] = path
        _uiState.update { it.copy(photoPath = path, photoNotice = null) }
    }

    /** Leaving the form without an account: the photo copy is not needed any more. */
    override fun onCleared() {
        val state = _uiState.value
        if (!state.registered) state.photoPath?.let { File(it).delete() }
    }

    fun submit() {
        val state = _uiState.value
        if (state.isRegistering || state.registered || state.isCopyingPhoto) return
        if (!state.isValid) {
            _uiState.update { it.withAllErrors().copy(showErrorSummary = true) }
            return
        }

        // Set before launch: the screen reports it to CityFixApp, which holds the redirect while
        // the sign-up (which signs the user in) and the NIF transaction run.
        _uiState.update { it.copy(isRegistering = true, error = null, showErrorSummary = false) }
        val citizen = state.role == RegistrationRole.CITIZEN
        val registration = Registration(
            role = state.role,
            name = state.name,
            email = state.email,
            phone = state.phone,
            nif = state.nif,
            georeferencing = if (citizen) state.location else null,
            impactDetection = citizen && state.impactDetection,
            photoPath = state.photoPath,
        )
        viewModelScope.launch {
            val result = users.register(registration, state.password)
            _uiState.update {
                when (result) {
                    // One update: isRegistering = false releases the hold, registered keeps
                    // Submit disabled until the redirect leaves this screen.
                    RegisterResult.Success -> it.copy(isRegistering = false, registered = true)
                    RegisterResult.NifTaken ->
                        it.copy(isRegistering = false, nifError = R.string.register_error_nif_taken)
                    RegisterResult.DomainNotAllowed ->
                        it.copy(isRegistering = false, emailError = R.string.register_error_email_domain)
                    is RegisterResult.Failure ->
                        if (result.error == AuthError.EMAIL_IN_USE) {
                            it.copy(isRegistering = false, emailError = R.string.auth_error_email_in_use)
                        } else {
                            it.copy(isRegistering = false, error = result.error.messageRes())
                        }
                }
            }
        }
    }

    /** Applies [change], then hides the summary once the form is valid. */
    private fun update(change: RegisterUiState.() -> RegisterUiState) {
        _uiState.update { current ->
            val next = current.change()
            next.copy(showErrorSummary = next.showErrorSummary && !next.isValid)
        }
    }

    /** The location saved before a process death, when both coordinates are there. */
    private fun savedLocation(): Coordinates? {
        val latitude = savedState.get<Double>(KEY_LATITUDE) ?: return null
        val longitude = savedState.get<Double>(KEY_LONGITUDE) ?: return null
        return Coordinates(latitude, longitude)
    }

    /** A field's error while typing: an empty field shows none until Submit (Q10). */
    @StringRes
    private fun ifTyped(text: String, @StringRes error: Int?): Int? = if (text.isEmpty()) null else error

    private fun RegisterUiState.withAllErrors(): RegisterUiState = copy(
        nameError = nameError(this),
        emailError = emailError(this),
        phoneError = phoneError(this),
        nifError = nifError(this),
        passwordError = passwordError(this),
        confirmError = confirmError(this),
        locationError = if (role == RegistrationRole.CITIZEN && location == null) {
            R.string.register_error_location
        } else {
            null
        },
    )

    @StringRes
    private fun nameError(state: RegisterUiState): Int? =
        if (isValidName(state.name)) null else R.string.register_error_name

    @StringRes
    private fun emailError(state: RegisterUiState): Int? {
        emailErrorFor(state.email.trim())?.let { return it }
        if (state.role == RegistrationRole.CITIZEN || state.isLoadingDomain) return null
        val domain = state.operatorDomain ?: return R.string.register_error_operator_unavailable
        return if (isInEmailDomain(state.email, domain)) null else R.string.register_error_email_domain
    }

    @StringRes
    private fun phoneError(state: RegisterUiState): Int? =
        if (isPortuguesePhone(state.phone)) null else R.string.register_error_phone

    @StringRes
    private fun nifError(state: RegisterUiState): Int? =
        if (isValidNif(state.nif)) null else R.string.register_error_nif

    @StringRes
    private fun passwordError(state: RegisterUiState): Int? = when (passwordProblem(state.password)) {
        PasswordProblem.TOO_SHORT -> R.string.register_error_password_short
        PasswordProblem.NO_LETTER -> R.string.register_error_password_letter
        PasswordProblem.NO_DIGIT -> R.string.register_error_password_digit
        null -> null
    }

    @StringRes
    private fun confirmError(state: RegisterUiState): Int? =
        if (state.confirmPassword == state.password) null else R.string.register_error_confirm

    private companion object {
        const val FIELD_DIGITS = 9
        const val KEY_ROLE = "role"
        const val KEY_NAME = "name"
        const val KEY_EMAIL = "email"
        const val KEY_PHONE = "phone"
        const val KEY_NIF = "nif"
        const val KEY_IMPACT = "impactDetection"
        const val KEY_LATITUDE = "latitude"
        const val KEY_LONGITUDE = "longitude"
        const val KEY_PHOTO = "photoPath"
    }
}

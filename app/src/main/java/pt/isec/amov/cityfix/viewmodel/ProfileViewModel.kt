package pt.isec.amov.cityfix.viewmodel

import android.net.Uri
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
import pt.isec.amov.cityfix.data.AuthRepository
import pt.isec.amov.cityfix.data.ProfileChanges
import pt.isec.amov.cityfix.data.UserProfile
import pt.isec.amov.cityfix.data.UsersRepository
import pt.isec.amov.cityfix.model.Citizen
import pt.isec.amov.cityfix.utils.isPortuguesePhone
import java.io.File

/**
 * Profile screen ViewModel (D-011), created per route for the signed-in [uid].
 *
 * It listens to `users/[uid]` itself (the cache serves it and the session's listener). Every
 * snapshot refreshes the read-only fields and the stored photo; the editable values (phone,
 * impact detection) are taken from the first snapshot only, unless [savedState] restored them,
 * so a later snapshot never overwrites what the user is typing. Save writes only what changed,
 * without waiting (works offline). A photo picked here is a local copy, deleted when the screen
 * is left without saving, and otherwise handed to the upload worker.
 */
class ProfileViewModel(
    private val uid: String,
    private val users: UsersRepository,
    private val auth: AuthRepository,
    private val attachmentStorage: AttachmentStorage,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ProfileUiState(
            email = auth.account.value?.email.orEmpty(),
            newPhotoPath = savedState.get<String>(KEY_PHOTO)?.takeIf { File(it).exists() },
        ),
    )
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    /** False until the first profile snapshot has set the editable values. */
    private var initialised = false

    /** True once [reloadAccount] ran for this visit. */
    private var reloaded = false

    /** The stored photo when Save queued a new one; a different value means the upload finished. */
    private var photoAtSave: String? = null

    init {
        viewModelScope.launch {
            users.observeUser(uid).collect { profile ->
                when (profile) {
                    is UserProfile.Found -> onProfile(profile)
                    UserProfile.Missing, UserProfile.Unavailable ->
                        _uiState.update { it.copy(unavailable = true) }
                }
            }
        }
        viewModelScope.launch {
            auth.account.collect { account ->
                _uiState.update { it.copy(email = account?.email.orEmpty()) }
            }
        }
    }

    private fun onProfile(found: UserProfile.Found) {
        val user = found.user
        _uiState.update { state ->
            var next = state.copy(profile = user, unavailable = false)
            if (!initialised) {
                val phone = savedState.get<String>(KEY_PHONE) ?: user.phone
                next = next.copy(
                    phone = phone,
                    impactDetection = savedState.get<Boolean>(KEY_IMPACT)
                        ?: ((user as? Citizen)?.impactDetection ?: false),
                    phoneError = if (phone.isEmpty() || isPortuguesePhone(phone)) null else R.string.register_error_phone,
                )
            }
            if (state.photoUploadPending && user.photo != photoAtSave) {
                next = next.copy(photoUploadPending = false)
            }
            next
        }
        initialised = true
    }

    /**
     * Best effort, once per visit: reloads the account, so an e-mail changed through the link
     * (maybe on another device) shows up and the session syncs it. If the link revoked this
     * device's token, the reload signs the user out (the decision 0015 involuntary path).
     */
    fun reloadAccount() {
        // The ViewModel outlives a rotation, the screen's LaunchedEffect does not.
        if (reloaded) return
        reloaded = true
        viewModelScope.launch { auth.reloadAccount() }
    }

    /** Keeps digits only, at most [FIELD_DIGITS]; the D-010 phone rule while typing. */
    fun onPhoneChange(text: String) {
        val digits = text.filter { it.isDigit() }.take(FIELD_DIGITS)
        savedState[KEY_PHONE] = digits
        _uiState.update {
            it.copy(
                phone = digits,
                phoneError = if (digits.isEmpty() || isPortuguesePhone(digits)) null else R.string.register_error_phone,
                notice = null,
            )
        }
    }

    fun onImpactDetectionChange(enabled: Boolean) {
        savedState[KEY_IMPACT] = enabled
        _uiState.update { it.copy(impactDetection = enabled, notice = null) }
    }

    /** Gallery pick: copied into filesDir/images in viewModelScope, then it replaces the previous pick. */
    fun onPhotoPicked(uri: Uri) {
        if (_uiState.value.isCopyingPhoto) return
        _uiState.update { it.copy(isCopyingPhoto = true, photoNotice = null, notice = null) }
        viewModelScope.launch {
            val path = attachmentStorage.copyToInternalStorage(listOf(uri)).firstOrNull()
            if (path == null) {
                _uiState.update { it.copy(isCopyingPhoto = false, photoNotice = R.string.register_photo_copy_failed) }
            } else {
                setPhoto(path)
                _uiState.update { it.copy(isCopyingPhoto = false) }
            }
        }
    }

    /** [path] is the camera's file in filesDir/images, already written. */
    fun onPhotoCaptured(path: String) {
        setPhoto(path)
    }

    /** Discards the photo picked in this visit (the stored one stays; D-011 Q7). */
    fun onRemoveNewPhoto() {
        setPhoto(null)
    }

    fun onCameraUnavailable() {
        _uiState.update { it.copy(photoNotice = R.string.report_camera_failed) }
    }

    /** Replaces the picked photo, deleting the previous local copy. */
    private fun setPhoto(path: String?) {
        val previous = _uiState.value.newPhotoPath
        if (previous != null && previous != path) File(previous).delete()
        savedState[KEY_PHOTO] = path
        _uiState.update { it.copy(newPhotoPath = path, photoNotice = null, notice = null) }
    }

    fun save() {
        val state = _uiState.value
        val profile = state.profile ?: return
        if (state.isCopyingPhoto || !state.hasChanges) return
        if (!state.isValid) {
            _uiState.update { it.copy(phoneError = R.string.register_error_phone) }
            return
        }
        val changes = ProfileChanges(
            phone = state.phone.takeIf { it != profile.phone },
            impactDetection = if (profile is Citizen && state.impactDetection != profile.impactDetection) {
                state.impactDetection
            } else {
                null
            },
            newPhotoPath = state.newPhotoPath,
        )
        // Not awaited: the write is queued in the local cache and the listener shows it at once.
        users.updateProfile(profile, changes)
        if (state.newPhotoPath != null) photoAtSave = profile.photo
        // The stored values are the baseline now; the photo file belongs to the worker.
        savedState.remove<String>(KEY_PHOTO)
        savedState.remove<String>(KEY_PHONE)
        savedState.remove<Boolean>(KEY_IMPACT)
        _uiState.update {
            it.copy(
                notice = R.string.profile_saved,
                photoUploadPending = it.photoUploadPending || state.newPhotoPath != null,
                newPhotoPath = null,
            )
        }
    }

    /** Leaving the screen without saving: the picked photo copy is not needed any more. */
    override fun onCleared() {
        _uiState.value.newPhotoPath?.let { File(it).delete() }
    }

    private companion object {
        const val FIELD_DIGITS = 9
        const val KEY_PHONE = "phone"
        const val KEY_IMPACT = "impactDetection"
        const val KEY_PHOTO = "photoPath"
    }
}

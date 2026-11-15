package pt.isec.amov.cityfix.viewmodel

import androidx.annotation.StringRes
import pt.isec.amov.cityfix.model.Citizen
import pt.isec.amov.cityfix.model.User
import pt.isec.amov.cityfix.utils.isPortuguesePhone

/**
 * Profile screen (brief I.2.C, I.3.C). Name, NIF, role and (citizen) georeferencing come from
 * [profile] and are read-only; phone, photo and (citizen) impact detection are editable.
 */
data class ProfileUiState(
    /** The stored profile (read-only fields, remote photo); null until the first snapshot. */
    val profile: User? = null,
    /** The stored photo's version (UserProfile.Found.photoUpdatedAt); part of the image cache key. */
    val photoUpdatedAt: Long? = null,
    /** The Auth account's e-mail (the current one). */
    val email: String = "",
    /** Digits only, at most 9. */
    val phone: String = "",
    val impactDetection: Boolean = false,
    /** A photo picked in this visit, not saved yet. */
    val newPhotoPath: String? = null,
    /** True while a gallery pick is copied; the photo buttons and Save wait for it. */
    val isCopyingPhoto: Boolean = false,
    /** A photo problem (copy failed, camera unavailable). */
    @StringRes val photoNotice: Int? = null,
    @StringRes val phoneError: Int? = null,
    /** "Profile saved." after Save; cleared by the next edit. */
    @StringRes val notice: Int? = null,
    /** True after Save with a photo, until photoUpdatedAt changes (the worker stored it). */
    val photoUploadPending: Boolean = false,
    /**
     * The local file of the photo saved in this visit, shown until the upload finishes. Display
     * only: the upload worker owns the file and deletes it.
     */
    val savedPhotoPath: String? = null,
    /** The profile could not be read (Unavailable/Missing); the screen shows a message. */
    val unavailable: Boolean = false,
) {
    val isCitizen: Boolean get() = profile is Citizen

    val isValid: Boolean get() = isPortuguesePhone(phone)

    /** Something differs from the stored profile. Save is enabled only then. */
    val hasChanges: Boolean
        get() = profile != null &&
            (phone != profile.phone || newPhotoPath != null ||
                (profile is Citizen && impactDetection != profile.impactDetection))
}

package pt.isec.amov.cityfix.viewmodel

import androidx.annotation.StringRes
import pt.isec.amov.cityfix.model.Citizen
import pt.isec.amov.cityfix.model.User
import pt.isec.amov.cityfix.utils.isPortuguesePhone

data class ProfileUiState(
    val profile: User? = null,
    val photoUpdatedAt: Long? = null,
    val email: String = "",
    val phone: String = "",
    val impactDetection: Boolean = false,
    val newPhotoPath: String? = null,
    val isCopyingPhoto: Boolean = false,
    @StringRes val photoNotice: Int? = null,
    @StringRes val phoneError: Int? = null,
    @StringRes val notice: Int? = null,
    val photoUploadPending: Boolean = false,
    val savedPhotoPath: String? = null,
    val unavailable: Boolean = false,
) {
    val isCitizen: Boolean get() = profile is Citizen

    val isValid: Boolean get() = isPortuguesePhone(phone)

    val hasChanges: Boolean
        get() = profile != null &&
            (phone != profile.phone || newPhotoPath != null ||
                (profile is Citizen && impactDetection != profile.impactDetection))
}

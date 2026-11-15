package pt.isec.amov.cityfix.data

import kotlinx.coroutines.flow.Flow
import pt.isec.amov.cityfix.model.Coordinates
import pt.isec.amov.cityfix.model.User

/** The result of reading `users/{uid}`. */
sealed interface UserProfile {
    /**
     * A Citizen or an Operator; the concrete type is the role (decision 0010).
     * [photoUpdatedAt] is when ProfilePhotoWorker last stored the photo (epoch ms; null before the
     * first upload). The overwritten photo may keep the same download URL, so this is the
     * photo's version: the image cache key and the end of a Profile upload (D-011 R4).
     */
    data class Found(val user: User, val photoUpdatedAt: Long? = null) : UserProfile

    /** No document, or a `role` that is not CITIZEN or OPERATOR. The role is never guessed. */
    data object Missing : UserProfile

    /** The read failed, or the device is offline with nothing cached. */
    data object Unavailable : UserProfile
}

/** The role chosen on the Register screen. The model's User subtypes need an id we do not have yet. */
enum class RegistrationRole { CITIZEN, OPERATOR }

/** What the Register form sends (brief I.2.A, I.3.A). The password travels apart and is never stored. */
data class Registration(
    val role: RegistrationRole,
    val name: String,
    val email: String,
    /** 9 digits, without +351. */
    val phone: String,
    /** 9 digits. */
    val nif: String,
    /** Citizen only, and required for a citizen. */
    val georeferencing: Coordinates?,
    /** Citizen only. */
    val impactDetection: Boolean,
    /** A local image file in filesDir/images, optional; uploaded by ProfilePhotoWorker after the registration. */
    val photoPath: String?,
)

sealed interface RegisterResult {
    data object Success : RegisterResult

    /** `nifs/{ROLE}_{nif}` already exists: the NIF is registered for this role. */
    data object NifTaken : RegisterResult

    /** An operator e-mail outside the council's domain. */
    data object DomainNotAllowed : RegisterResult

    data class Failure(val error: AuthError) : RegisterResult
}

/** What the Profile screen may change (brief I.2.C, I.3.C). null = unchanged. */
data class ProfileChanges(
    /** 9 digits, without +351. */
    val phone: String? = null,
    /** Citizens only; ignored for an Operator. */
    val impactDetection: Boolean? = null,
    /** A new local photo in filesDir/images; uploaded by ProfilePhotoWorker. */
    val newPhotoPath: String? = null,
)

/**
 * User profiles: read (D-009), registration (D-010) and profile editing (D-011).
 * [FirestoreUsersRepository] is the implementation.
 */
interface UsersRepository {
    /** Emits the profile now (from the cache when offline) and again whenever the document changes. */
    fun observeUser(uid: String): Flow<UserProfile>

    /**
     * Creates the Firebase Auth user (which signs it in), then writes `nifs/{ROLE}_{nif}` and
     * `users/{uid}` in one transaction. Online only. When the profile cannot be written the Auth
     * user is deleted again, so a failed registration leaves no account behind.
     */
    suspend fun register(registration: Registration, password: String): RegisterResult

    /** `municipality/main.emailDomain`, lowercase without '@'; null when missing or unreadable. */
    suspend fun operatorEmailDomain(): String?

    /**
     * Writes only phone and (citizen) impactDetection to users/{user.id}, then queues the photo.
     * Never name, nif, georeferencing, role, registrationStatus or email. Not awaited: the write
     * is queued in the local cache (works offline) and the listener shows it at once.
     * Writes nothing when [changes] is empty (the Profile screen calls it only with changes).
     */
    fun updateProfile(user: User, changes: ProfileChanges)

    /** Copies the Auth e-mail into users/{uid}.email (after a verified change). Not awaited. */
    fun syncEmail(uid: String, email: String)
}

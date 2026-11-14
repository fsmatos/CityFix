package pt.isec.amov.cityfix.data

import kotlinx.coroutines.flow.Flow
import pt.isec.amov.cityfix.model.Coordinates
import pt.isec.amov.cityfix.model.User

/** The result of reading `users/{uid}`. */
sealed interface UserProfile {
    /** A Citizen or an Operator; the concrete type is the role (decision 0010). */
    data class Found(val user: User) : UserProfile

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

/**
 * User profiles: read (D-009) and registration (D-010); profile editing is D-011.
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
}

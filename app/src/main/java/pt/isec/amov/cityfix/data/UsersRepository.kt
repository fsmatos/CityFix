package pt.isec.amov.cityfix.data

import kotlinx.coroutines.flow.Flow
import pt.isec.amov.cityfix.model.Citizen
import pt.isec.amov.cityfix.model.Coordinates
import pt.isec.amov.cityfix.model.User

sealed interface UserProfile {
    data class Found(val user: User, val photoUpdatedAt: Long? = null) : UserProfile

    data object Missing : UserProfile

    data object Unavailable : UserProfile
}

enum class RegistrationRole { CITIZEN, OPERATOR }

data class Registration(
    val role: RegistrationRole,
    val name: String,
    val email: String,
    val phone: String,
    val nif: String,
    val georeferencing: Coordinates?,
    val impactDetection: Boolean,
    val photoPath: String?,
)

sealed interface RegisterResult {
    data object Success : RegisterResult

    data object NifTaken : RegisterResult

    data object DomainNotAllowed : RegisterResult

    data class Failure(val error: AuthError) : RegisterResult
}

data class ProfileChanges(
    val phone: String? = null,
    val impactDetection: Boolean? = null,
    val newPhotoPath: String? = null,
)

interface UsersRepository {
    fun observeUser(uid: String): Flow<UserProfile>

    suspend fun register(registration: Registration, password: String): RegisterResult

    suspend fun operatorEmailDomain(): String?

    fun updateProfile(user: User, changes: ProfileChanges)

    fun syncEmail(uid: String, email: String)

    fun pendingCitizens(): Flow<List<Citizen>>

    fun activateCitizen(citizenId: String, operatorId: String)
}

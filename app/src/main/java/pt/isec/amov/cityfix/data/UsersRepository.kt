package pt.isec.amov.cityfix.data

import kotlinx.coroutines.flow.Flow
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

/**
 * User profiles. Only the read part is in D-009; registration is D-010, profile editing D-011.
 * [FirestoreUsersRepository] is the implementation.
 */
interface UsersRepository {
    /** Emits the profile now (from the cache when offline) and again whenever the document changes. */
    fun observeUser(uid: String): Flow<UserProfile>
}

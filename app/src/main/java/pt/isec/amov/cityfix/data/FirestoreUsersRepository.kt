package pt.isec.amov.cityfix.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import pt.isec.amov.cityfix.model.Citizen
import pt.isec.amov.cityfix.model.Coordinates
import pt.isec.amov.cityfix.model.Operator
import pt.isec.amov.cityfix.model.RegistrationStatus

/**
 * [UsersRepository] over the `users/{uid}` documents (fields in decision 0010). [firestore] is the
 * instance from CityFixApplication, which has the persistent cache on, so a profile read once
 * online is still served offline.
 */
class FirestoreUsersRepository(private val firestore: FirebaseFirestore) : UsersRepository {

    override fun observeUser(uid: String): Flow<UserProfile> =
        firestore.collection(USERS).document(uid)
            .snapshots()
            .map { it.toProfile() }
            .catch { emit(UserProfile.Unavailable) }

    /**
     * Mapped by hand, not with toObject(): the `role` string picks the type, and `role` is not a
     * field of the model (decision 0010).
     */
    private fun DocumentSnapshot.toProfile(): UserProfile {
        if (!exists()) {
            // Offline, a document that was never read looks like a missing one: say "unavailable",
            // so the user is not told the account is not set up.
            return if (metadata.isFromCache) UserProfile.Unavailable else UserProfile.Missing
        }
        val name = getString("name").orEmpty()
        val nif = getString("nif").orEmpty()
        val email = getString("email").orEmpty()
        val phone = getString("phone").orEmpty()
        val photo = getString("photoPath")
        return when (getString("role")) {
            ROLE_CITIZEN -> {
                val location = getGeoPoint("georeferencing")
                UserProfile.Found(
                    Citizen(
                        id = id,
                        name = name,
                        nif = nif,
                        email = email,
                        phone = phone,
                        photo = photo,
                        georeferencing = Coordinates(
                            latitude = location?.latitude ?: 0.0,
                            longitude = location?.longitude ?: 0.0,
                        ),
                        registrationStatus = registrationStatusOf(getString("registrationStatus")),
                        impactDetection = getBoolean("impactDetection") ?: false,
                    ),
                )
            }
            ROLE_OPERATOR -> UserProfile.Found(
                Operator(id = id, name = name, nif = nif, email = email, phone = phone, photo = photo),
            )
            else -> UserProfile.Missing
        }
    }

    /** An unknown or absent value counts as PENDING: never give report access by mistake. */
    private fun registrationStatusOf(value: String?): RegistrationStatus =
        RegistrationStatus.entries.firstOrNull { it.name == value } ?: RegistrationStatus.PENDING

    private companion object {
        const val USERS = "users"
        const val ROLE_CITIZEN = "CITIZEN"
        const val ROLE_OPERATOR = "OPERATOR"
    }
}

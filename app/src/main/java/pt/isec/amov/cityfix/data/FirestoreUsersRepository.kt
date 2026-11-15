package pt.isec.amov.cityfix.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import pt.isec.amov.cityfix.model.Citizen
import pt.isec.amov.cityfix.model.Coordinates
import pt.isec.amov.cityfix.model.Operator
import pt.isec.amov.cityfix.model.RegistrationStatus
import pt.isec.amov.cityfix.utils.isInEmailDomain
import kotlin.coroutines.cancellation.CancellationException

/**
 * [UsersRepository] over the `users/{uid}` documents (fields in decision 0010). [firestore] is the
 * instance from CityFixApplication, which has the persistent cache on, so a profile read once
 * online is still served offline. [auth] creates (and, on failure, deletes) the account at
 * registration; [photoUploader] queues the optional photo once the profile exists.
 *
 * Every `catch (e: Exception)` is preceded by a rethrow of CancellationException, as in
 * FirebaseAuthRepository.
 */
class FirestoreUsersRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val photoUploader: ProfilePhotoUploader,
) : UsersRepository {

    override fun observeUser(uid: String): Flow<UserProfile> =
        firestore.collection(USERS).document(uid)
            .snapshots()
            .map { it.toProfile() }
            .catch { emit(UserProfile.Unavailable) }

    override suspend fun operatorEmailDomain(): String? =
        try {
            firestore.collection(MUNICIPALITY).document(MUNICIPALITY_MAIN).get().await()
                .getString("emailDomain")
                ?.trim()
                ?.lowercase()
                ?.removePrefix("@")
                ?.takeIf { it.isNotEmpty() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }

    override suspend fun register(registration: Registration, password: String): RegisterResult {
        val email = registration.email.trim().lowercase()
        val name = registration.name.trim()

        // The form already checked these; checked again so a wrong call never creates an account.
        when (registration.role) {
            RegistrationRole.OPERATOR -> {
                val domain = operatorEmailDomain() ?: return RegisterResult.Failure(AuthError.UNKNOWN)
                if (!isInEmailDomain(email, domain)) return RegisterResult.DomainNotAllowed
            }
            RegistrationRole.CITIZEN ->
                if (registration.georeferencing == null) return RegisterResult.Failure(AuthError.UNKNOWN)
        }

        // Signs the new user in at once; CityFixApp holds its redirect until this function returns.
        val user = try {
            auth.createUserWithEmailAndPassword(email, password).await().user
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return RegisterResult.Failure(e.toAuthError())
        } ?: return RegisterResult.Failure(AuthError.UNKNOWN)

        // One transaction reserves the NIF for the role and writes the profile (decision 0010).
        // Transactions need the network, as sign-up does.
        try {
            val nifRef = firestore.collection(NIFS).document("${registration.role.name}_${registration.nif}")
            val userRef = firestore.collection(USERS).document(user.uid)
            val document = userDocument(registration, name, user.email ?: email)
            firestore.runTransaction { tx ->
                if (tx.get(nifRef).exists()) throw NifTakenException()
                tx.set(nifRef, mapOf("uid" to user.uid))
                tx.set(userRef, document)
            }.await()
        } catch (e: CancellationException) {
            withContext(NonCancellable) { deleteNewUser(user) }
            throw e
        } catch (e: Exception) {
            withContext(NonCancellable) { deleteNewUser(user) }
            return when {
                e is NifTakenException || e.cause is NifTakenException -> RegisterResult.NifTaken
                e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.UNAVAILABLE ->
                    RegisterResult.Failure(AuthError.NETWORK)
                else -> RegisterResult.Failure(AuthError.UNKNOWN)
            }
        }
        // Only after the commit: the worker updates users/{uid}, which now exists.
        registration.photoPath?.let { photoUploader.enqueue(user.uid, it) }
        return RegisterResult.Success
    }

    /**
     * The `users/{uid}` fields of decision 0010 and the draft create rule. No `photoPath` (set by
     * the upload later) and no `validatedBy` / `validatedAt` (set by the operator who validates).
     * An operator has no `registrationStatus`: the account is active at once (brief I.3.A).
     */
    private fun userDocument(registration: Registration, name: String, email: String): Map<String, Any> {
        val fields = mutableMapOf<String, Any>(
            "name" to name,
            "nif" to registration.nif,
            "email" to email,
            "phone" to registration.phone,
            "createdAt" to System.currentTimeMillis(),
            "fcmTokens" to emptyList<String>(),
        )
        when (registration.role) {
            RegistrationRole.CITIZEN -> {
                // register() returns before this point when a citizen has no location.
                val location = requireNotNull(registration.georeferencing) { "citizen without location" }
                fields["role"] = ROLE_CITIZEN
                fields["georeferencing"] = GeoPoint(location.latitude, location.longitude)
                fields["registrationStatus"] = RegistrationStatus.PENDING.name
                fields["impactDetection"] = registration.impactDetection
            }
            RegistrationRole.OPERATOR -> fields["role"] = ROLE_OPERATOR
        }
        return fields
    }

    /**
     * Best effort: the new account goes away when its profile could not be written. If the delete
     * fails too (offline), the user is signed out and the Auth user stays orphaned (D-010 Q11).
     * Called inside NonCancellable, so it finishes even when the screen is gone.
     */
    private suspend fun deleteNewUser(user: FirebaseUser) {
        try {
            user.delete().await()
        } catch (e: Exception) {
            auth.signOut()
        }
    }

    /** Thrown inside the transaction when the NIF is taken; aborts it without retries. */
    private class NifTakenException : Exception("NIF already registered for this role")

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
        const val NIFS = "nifs"
        const val MUNICIPALITY = "municipality"
        const val MUNICIPALITY_MAIN = "main"
        const val ROLE_CITIZEN = "CITIZEN"
        const val ROLE_OPERATOR = "OPERATOR"
    }
}

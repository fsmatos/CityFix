package pt.isec.amov.cityfix.data

import android.util.Log
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
import pt.isec.amov.cityfix.model.User
import pt.isec.amov.cityfix.utils.isInEmailDomain
import pt.isec.amov.cityfix.utils.nationalPhoneDigits
import kotlin.coroutines.cancellation.CancellationException

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

    override fun updateProfile(user: User, changes: ProfileChanges) {
        val fields = buildMap<String, Any> {
            changes.phone?.let { put("phone", it) }
            if (user is Citizen) changes.impactDetection?.let { put("impactDetection", it) }
        }
        if (fields.isNotEmpty()) {
            // Not awaited (as F-011): the write is queued in the local cache, also offline, and
            // the listener shows it at once. A refusal by the server is only logged.
            firestore.collection(USERS).document(user.id).update(fields)
                .addOnFailureListener { Log.w(TAG, "profile update refused", it) }
        }
        changes.newPhotoPath?.let { photoUploader.enqueue(user.id, it) }
    }

    override fun syncEmail(uid: String, email: String) {
        // Not awaited, as updateProfile.
        firestore.collection(USERS).document(uid).update("email", email)
            .addOnFailureListener { Log.w(TAG, "e-mail sync refused", it) }
    }

    override fun pendingCitizens(): Flow<List<Citizen>> =
        // One equality filter is enough: operators have no registrationStatus, and toProfile()
        // drops anything that is not a citizen. Sorted here by createdAt (oldest first) rather
        // than with orderBy, which together with the filter would need a composite index.
        firestore.collection(USERS)
            .whereEqualTo("registrationStatus", RegistrationStatus.PENDING.name)
            .snapshots()
            .map { snapshot ->
                snapshot.documents
                    // Read leniently: getLong would throw on a hand-made console document with a
                    // Timestamp or a string here, and the .catch below would end the listener.
                    .sortedBy { (it.get("createdAt") as? Number)?.toLong() ?: 0L }
                    .mapNotNull { (it.toProfile() as? UserProfile.Found)?.user as? Citizen }
            }
            .catch {
                Log.w(TAG, "pending citizens failed", it)
                emit(emptyList())
            }

    override fun activateCitizen(citizenId: String, operatorId: String) {
        // Exactly the three keys of the draft validation rule. Not awaited, as updateProfile.
        val fields = mapOf(
            "registrationStatus" to RegistrationStatus.ACTIVE.name,
            "validatedBy" to operatorId,
            "validatedAt" to System.currentTimeMillis(),
        )
        firestore.collection(USERS).document(citizenId).update(fields)
            .addOnFailureListener { Log.w(TAG, "activation refused", it) }
    }

    private fun userDocument(registration: Registration, name: String, email: String): Map<String, Any> {
        val fields = mutableMapOf<String, Any>(
            "name" to name,
            "nif" to registration.nif,
            "email" to email,
            "phone" to registration.phone,
            "createdAt" to System.currentTimeMillis(),
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

    private suspend fun deleteNewUser(user: FirebaseUser) {
        try {
            user.delete().await()
        } catch (e: Exception) {
            auth.signOut()
        }
    }

    private class NifTakenException : Exception("NIF already registered for this role")

    private fun DocumentSnapshot.toProfile(): UserProfile {
        if (!exists()) {
            // Offline, a document that was never read looks like a missing one: say "unavailable",
            // so the user is not told the account is not set up.
            return if (metadata.isFromCache) UserProfile.Unavailable else UserProfile.Missing
        }
        val name = getString("name").orEmpty()
        val nif = getString("nif").orEmpty()
        val email = getString("email").orEmpty()
        // Accounts made in the console may store "+351…"; the model keeps 9 digits.
        val phone = nationalPhoneDigits(getString("phone").orEmpty())
        val photo = getString("photoPath")
        val photoUpdatedAt = getLong("photoUpdatedAt")
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
                    photoUpdatedAt,
                )
            }
            ROLE_OPERATOR -> UserProfile.Found(
                Operator(id = id, name = name, nif = nif, email = email, phone = phone, photo = photo),
                photoUpdatedAt,
            )
            else -> UserProfile.Missing
        }
    }

    private fun registrationStatusOf(value: String?): RegistrationStatus =
        RegistrationStatus.entries.firstOrNull { it.name == value } ?: RegistrationStatus.PENDING

    private companion object {
        const val TAG = "UsersRepository"
        const val USERS = "users"
        const val NIFS = "nifs"
        const val MUNICIPALITY = "municipality"
        const val MUNICIPALITY_MAIN = "main"
        const val ROLE_CITIZEN = "CITIZEN"
        const val ROLE_OPERATOR = "OPERATOR"
    }
}

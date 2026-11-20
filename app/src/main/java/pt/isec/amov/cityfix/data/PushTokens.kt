package pt.isec.amov.cityfix.data

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/**
 * Keeps this installation's FCM registration token in `users/{uid}.fcmTokens` (F-016, Gate 1
 * Q1). As P11: the token identifies the installation and must be stored again when it changes
 * (CityFixMessagingService.onNewToken). No server sends to it yet; the console test uses it.
 * Create one per app (CityFixApplication).
 */
class PushTokens(
    private val messaging: FirebaseMessaging,
    private val firestore: FirebaseFirestore,
) {
    /** Reads the current token (P11 `Firebase.messaging.token`) and saves it for [uid]. */
    suspend fun register(uid: String) {
        try {
            save(uid, messaging.token.await())
        } catch (e: Exception) {
            Log.w(TAG, "token not read", e)
        }
    }

    /**
     * Adds [token] to the user's list (arrayUnion: one user may have several devices). Not
     * awaited, as the other writes: offline it is queued; failures are only logged.
     */
    fun save(uid: String, token: String) {
        firestore.collection("users").document(uid)
            .update("fcmTokens", FieldValue.arrayUnion(token))
            .addOnFailureListener { Log.w(TAG, "token not saved", it) }
    }

    private companion object {
        const val TAG = "PushTokens"
    }
}

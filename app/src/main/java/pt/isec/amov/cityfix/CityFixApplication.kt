package pt.isec.amov.cityfix

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.messaging
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage
import pt.isec.amov.cityfix.data.AttachmentStorage
import pt.isec.amov.cityfix.data.AuthRepository
import pt.isec.amov.cityfix.data.FirebaseAuthRepository
import pt.isec.amov.cityfix.data.FirestoreUsersRepository
import pt.isec.amov.cityfix.data.LocalTicketsRepository
import pt.isec.amov.cityfix.data.TicketsRepository
import pt.isec.amov.cityfix.data.UsersRepository

/**
 * One container for the whole app, like ContactsApp in Contacts_#3 (decision 0009): it holds the
 * repositories and the Firebase instances. Registered in the manifest (android:name); MainActivity
 * reads it with `application as CityFixApplication` and hands it to the ViewModel factories.
 *
 * Every property is created on first use (`by lazy`). The Firestore settings must be applied
 * before any other Firestore call, so they are applied here, once, when [firestore] is first read.
 */
class CityFixApplication : Application() {
    val ticketsRepository: TicketsRepository by lazy { LocalTicketsRepository() }

    /** Copies picked media off the main thread (report form). */
    val attachmentStorage: AttachmentStorage by lazy { AttachmentStorage(this) }

    val auth: FirebaseAuth by lazy { Firebase.auth }

    /** Firestore with the persistent offline cache (default size, 100 MB). */
    val firestore: FirebaseFirestore by lazy {
        Firebase.firestore.apply {
            firestoreSettings = firestoreSettings {
                setLocalCacheSettings(persistentCacheSettings { })
            }
        }
    }

    val storage: FirebaseStorage by lazy { Firebase.storage }

    val messaging: FirebaseMessaging by lazy { Firebase.messaging }

    /** Sign-in, MFA and password reset (D-009), built from [auth]. */
    val authRepository: AuthRepository by lazy { FirebaseAuthRepository(auth) }

    /** `users/{uid}` profiles (D-009 reads them), built from [firestore] so the cache settings apply. */
    val usersRepository: UsersRepository by lazy { FirestoreUsersRepository(firestore) }
}

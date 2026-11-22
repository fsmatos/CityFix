package pt.isec.amov.cityfix

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage
import kotlinx.coroutines.flow.MutableSharedFlow
import pt.isec.amov.cityfix.data.AttachmentStorage
import pt.isec.amov.cityfix.data.AttachmentUploader
import pt.isec.amov.cityfix.data.AuthRepository
import pt.isec.amov.cityfix.data.CategoriesRepository
import pt.isec.amov.cityfix.data.FirebaseAuthRepository
import pt.isec.amov.cityfix.data.FirestoreCategoriesRepository
import pt.isec.amov.cityfix.data.FirestoreTicketsRepository
import pt.isec.amov.cityfix.data.FirestoreUsersRepository
import pt.isec.amov.cityfix.data.LocationRepository
import pt.isec.amov.cityfix.data.ProfilePhotoUploader
import pt.isec.amov.cityfix.data.TicketNotifier
import pt.isec.amov.cityfix.data.TicketsRepository
import pt.isec.amov.cityfix.data.UsersRepository
import pt.isec.amov.cityfix.utils.createNotificationChannels

/**
 * One container for the whole app, like ContactsApp in Contacts_#3 (decision 0009): it holds the
 * repositories and the Firebase instances. Registered in the manifest (android:name); MainActivity
 * reads it with `application as CityFixApplication` and hands it to the ViewModel factories.
 *
 * Every property is created on first use (`by lazy`). The Firestore settings must be applied
 * before any other Firestore call, so they are applied here, once, when [firestore] is first read.
 */
class CityFixApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // F-016: the class creates the channel before any notification is shown.
        createNotificationChannels(this)
    }

    val ticketsRepository: TicketsRepository by lazy {
        FirestoreTicketsRepository(firestore, attachmentUploader)
    }

    /** Copies picked media off the main thread (report form). */
    val attachmentStorage: AttachmentStorage by lazy { AttachmentStorage(this) }

    /** Compresses report media and enqueues their uploads (F-013; D-013 reuses it). */
    val attachmentUploader: AttachmentUploader by lazy { AttachmentUploader(this) }

    /** Enqueues the upload of the photo chosen at registration (D-010). */
    val profilePhotoUploader: ProfilePhotoUploader by lazy { ProfilePhotoUploader(this) }

    /** One-off GPS fixes for the 10 km gate and the report form (F-012). */
    val locationRepository: LocationRepository by lazy { LocationRepository(this) }

    /**
     * True while MainActivity is started (set in its onStart/onStop). TrackingService reads it to
     * choose between the in-app snackbar and a notification for an impact (F-022).
     */
    @Volatile
    var appInForeground = false

    /** Impacts detected by TrackingService while the app is in front; ImpactViewModel shows them. */
    val impacts = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

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

    /** Ticket changes -> local notifications, for the in-app watch and the worker (F-016). */
    val ticketNotifier: TicketNotifier by lazy { TicketNotifier(this) }

    /** Sign-in, MFA and password reset (D-009), built from [auth]. */
    val authRepository: AuthRepository by lazy { FirebaseAuthRepository(auth) }

    /**
     * `users/{uid}` profiles (D-009 reads them, D-010 registers), built from [firestore] so the
     * cache settings apply, [auth] for the sign-up and [profilePhotoUploader] for the photo.
     */
    val usersRepository: UsersRepository by lazy {
        FirestoreUsersRepository(firestore, auth, profilePhotoUploader)
    }

    /** Category management for operators (D-012), built from [firestore] so the cache settings apply. */
    val categoriesRepository: CategoriesRepository by lazy { FirestoreCategoriesRepository(firestore) }
}

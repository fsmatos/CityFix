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

class CityFixApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // F-016: the class creates the channel before any notification is shown.
        createNotificationChannels(this)
    }

    val ticketsRepository: TicketsRepository by lazy {
        FirestoreTicketsRepository(firestore, attachmentUploader)
    }

    val attachmentStorage: AttachmentStorage by lazy { AttachmentStorage(this) }

    val attachmentUploader: AttachmentUploader by lazy { AttachmentUploader(this) }

    val profilePhotoUploader: ProfilePhotoUploader by lazy { ProfilePhotoUploader(this) }

    val locationRepository: LocationRepository by lazy { LocationRepository(this) }

    @Volatile
    var appInForeground = false

    val impacts = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val auth: FirebaseAuth by lazy { Firebase.auth }

    val firestore: FirebaseFirestore by lazy {
        Firebase.firestore.apply {
            firestoreSettings = firestoreSettings {
                setLocalCacheSettings(persistentCacheSettings { })
            }
        }
    }

    val storage: FirebaseStorage by lazy { Firebase.storage }

    val ticketNotifier: TicketNotifier by lazy { TicketNotifier(this) }

    val authRepository: AuthRepository by lazy { FirebaseAuthRepository(auth) }

    val usersRepository: UsersRepository by lazy {
        FirestoreUsersRepository(firestore, auth, profilePhotoUploader)
    }

    val categoriesRepository: CategoriesRepository by lazy { FirestoreCategoriesRepository(firestore) }
}

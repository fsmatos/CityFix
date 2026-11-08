package pt.isec.amov.cityfix

import android.app.Application
import pt.isec.amov.cityfix.data.AttachmentStorage
import pt.isec.amov.cityfix.data.LocalTicketsRepository
import pt.isec.amov.cityfix.data.TicketsRepository

/**
 * One repository for the whole app, like ContactsApp in Contacts_#3. Registered in the manifest
 * (android:name); MainActivity reads it with `application as CityFixApplication` and hands it to
 * the ViewModel factories. Tickets live as long as the process.
 */
class CityFixApplication : Application() {
    val ticketsRepository: TicketsRepository by lazy { LocalTicketsRepository() }

    /** Copies picked media off the main thread (report form). */
    val attachmentStorage: AttachmentStorage by lazy { AttachmentStorage(this) }
}

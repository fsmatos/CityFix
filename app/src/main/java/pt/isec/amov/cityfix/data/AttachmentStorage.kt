package pt.isec.amov.cityfix.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import pt.isec.amov.cityfix.utils.FileUtils
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

/**
 * Copies picked photos and videos into filesDir/images off the main thread (a video can take
 * seconds). Goes beyond Contacts_#3, which copies in the launcher callback. Create one per app
 * (CityFixApplication); it keeps the application context only.
 */
class AttachmentStorage(context: Context) {

    private val appContext = context.applicationContext

    /**
     * Copies [uris] on Dispatchers.IO and returns the paths that copied (a failed one is left out).
     * If the caller is cancelled (the user left the form), deletes what it copied and rethrows.
     */
    suspend fun copyToInternalStorage(uris: List<Uri>): List<String> {
        val copied = mutableListOf<String>()
        try {
            withContext(Dispatchers.IO) {
                for (uri in uris) {
                    ensureActive()
                    FileUtils.copyUriToInternalStorage(appContext, uri)?.let { copied.add(it) }
                }
            }
        } catch (e: CancellationException) {
            copied.forEach { File(it).delete() }
            throw e
        }
        return copied
    }
}

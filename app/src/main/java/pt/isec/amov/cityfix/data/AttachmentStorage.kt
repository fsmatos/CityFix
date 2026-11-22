package pt.isec.amov.cityfix.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import pt.isec.amov.cityfix.utils.FileUtils
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

class AttachmentStorage(context: Context) {

    private val appContext = context.applicationContext

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

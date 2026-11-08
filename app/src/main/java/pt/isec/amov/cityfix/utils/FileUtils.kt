package pt.isec.amov.cityfix.utils

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/**
 * Local copies of the report attachments (P09), all in filesDir/images. Depends on Android, so
 * model/ must not import it. The file name carries the type: VID_ is a video, anything else an
 * image, so a saved path is enough to tell them apart.
 */
object FileUtils {

    private const val MEDIA_FOLDER = "images"
    private const val IMAGE_PREFIX = "IMG_"
    private const val VIDEO_PREFIX = "VID_"
    private const val CAPTURE_PREFIX = "CAPTURE_"
    private const val DEFAULT_IMAGE_EXTENSION = "jpg"
    private const val DEFAULT_VIDEO_EXTENSION = "mp4"

    /**
     * Blocking: call off the main thread (AttachmentStorage does). Null on any failure, including
     * a null stream; deletes a partial file.
     */
    fun copyUriToInternalStorage(context: Context, sourceUri: Uri): String? {
        val mimeType = context.contentResolver.getType(sourceUri)
        val target = File(mediaFolder(context), fileNameFor(mimeType))
        return try {
            val input = context.contentResolver.openInputStream(sourceUri) ?: return null
            input.use { source ->
                target.outputStream().use { destination -> source.copyTo(destination) }
            }
            target.absolutePath
        } catch (_: Exception) {
            target.delete()
            null
        }
    }

    /** Empty slot for the camera: the file, and the FileProvider Uri to hand to TakePicture. */
    fun createTempImageFile(context: Context): Pair<File, Uri> {
        val file = File(mediaFolder(context), "$CAPTURE_PREFIX${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Pair(file, uri)
    }

    fun isVideoPath(path: String): Boolean = File(path).name.startsWith(VIDEO_PREFIX)

    private fun mediaFolder(context: Context): File =
        File(context.filesDir, MEDIA_FOLDER).apply { mkdirs() }

    private fun fileNameFor(mimeType: String?): String {
        val isVideo = mimeType?.startsWith("video/") == true
        val prefix = if (isVideo) VIDEO_PREFIX else IMAGE_PREFIX
        val fallback = if (isVideo) DEFAULT_VIDEO_EXTENSION else DEFAULT_IMAGE_EXTENSION
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: fallback
        return "$prefix${UUID.randomUUID()}.$extension"
    }
}

package pt.isec.amov.cityfix.utils

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
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

    /** Every tag of the EXIF GPS IFD (EXIF 2.32), not only the latitude/longitude pair. */
    private val GPS_TAGS = listOf(
        ExifInterface.TAG_GPS_VERSION_ID,
        ExifInterface.TAG_GPS_LATITUDE_REF,
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LONGITUDE_REF,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_ALTITUDE_REF,
        ExifInterface.TAG_GPS_ALTITUDE,
        ExifInterface.TAG_GPS_TIMESTAMP,
        ExifInterface.TAG_GPS_SATELLITES,
        ExifInterface.TAG_GPS_STATUS,
        ExifInterface.TAG_GPS_MEASURE_MODE,
        ExifInterface.TAG_GPS_DOP,
        ExifInterface.TAG_GPS_SPEED_REF,
        ExifInterface.TAG_GPS_SPEED,
        ExifInterface.TAG_GPS_TRACK_REF,
        ExifInterface.TAG_GPS_TRACK,
        ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
        ExifInterface.TAG_GPS_IMG_DIRECTION,
        ExifInterface.TAG_GPS_MAP_DATUM,
        ExifInterface.TAG_GPS_DEST_LATITUDE_REF,
        ExifInterface.TAG_GPS_DEST_LATITUDE,
        ExifInterface.TAG_GPS_DEST_LONGITUDE_REF,
        ExifInterface.TAG_GPS_DEST_LONGITUDE,
        ExifInterface.TAG_GPS_DEST_BEARING_REF,
        ExifInterface.TAG_GPS_DEST_BEARING,
        ExifInterface.TAG_GPS_DEST_DISTANCE_REF,
        ExifInterface.TAG_GPS_DEST_DISTANCE,
        ExifInterface.TAG_GPS_PROCESSING_METHOD,
        ExifInterface.TAG_GPS_AREA_INFORMATION,
        ExifInterface.TAG_GPS_DATESTAMP,
        ExifInterface.TAG_GPS_DIFFERENTIAL,
        ExifInterface.TAG_GPS_H_POSITIONING_ERROR,
    )

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

    /** The file called [name] in filesDir/images (compressed media and thumbnails, F-013). */
    fun mediaFile(context: Context, name: String): File = File(mediaFolder(context), name)

    /**
     * Blocking. Clears the GPS IFD of the image at [path] in place, so the file does not carry
     * the place it was taken; make, model and capture time stay. False when the format cannot
     * be written (e.g. HEIC) or IO fails.
     */
    fun removeImageLocation(path: String): Boolean = try {
        val exif = ExifInterface(path)
        GPS_TAGS.forEach { exif.setAttribute(it, null) }
        exif.saveAttributes()
        true
    } catch (_: Exception) {
        false
    }

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

package pt.isec.amov.cityfix.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import androidx.exifinterface.media.ExifInterface
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Compressed copies and thumbnails of the report media (brief IV and V), made before upload.
 * Blocking: call on Dispatchers.IO (AttachmentUploader does).
 *
 * As in class: BitmapFactory.decodeFile and Bitmap.createScaledBitmap (P10 Challenge I) and
 * bitmap.compress(JPEG, quality, stream) (T8 AMOVStorageDemo).
 * Beyond class: inSampleSize (decodes a large photo at a smaller size so it fits in memory),
 * the EXIF rotation (camera photos are stored sideways with a rotation tag) and
 * MediaMetadataRetriever (a frame of a video). Re-encoding writes a new JPEG with no EXIF, so the
 * compressed photo carries no location.
 */
object ImageCompression {

    const val MAX_SIDE = 1600
    const val QUALITY = 80
    const val THUMB_SIDE = 256
    const val THUMB_QUALITY = 70

    /**
     * Photo at [source] -> JPEG [target] (long side <= 1600 px, upright, no EXIF) and JPEG
     * [thumbnail] (long side <= 256 px). False if it cannot be decoded or written.
     */
    fun compressImage(source: String, target: File, thumbnail: File): Boolean = try {
        // Read only the size first, to pick how much to shrink while decoding.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source, bounds)
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2

        val decoded = BitmapFactory.decodeFile(source, BitmapFactory.Options().apply { inSampleSize = sample })
        if (decoded == null) {
            false
        } else {
            val upright = rotate(decoded, ExifInterface(source).rotationDegrees)
            writeJpeg(scaleDown(upright, MAX_SIDE), target, QUALITY)
            writeJpeg(scaleDown(upright, THUMB_SIDE), thumbnail, THUMB_QUALITY)
            true
        }
    } catch (_: Exception) {
        false
    }

    /** A frame of the video at [source] -> JPEG [thumbnail] (long side <= 256 px). False on failure. */
    fun videoThumbnail(source: String, thumbnail: File): Boolean {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(source)
            val frame = retriever.frameAtTime
            if (frame == null) {
                false
            } else {
                writeJpeg(scaleDown(frame, THUMB_SIDE), thumbnail, THUMB_QUALITY)
                true
            }
        } catch (_: Exception) {
            false
        } finally {
            retriever.release()
        }
    }

    /** The bitmap turned by [degrees] (0 = unchanged). */
    private fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /** The bitmap itself when its long side fits in [maxSide], otherwise a smaller copy (same aspect ratio). */
    private fun scaleDown(bitmap: Bitmap, maxSide: Int): Bitmap {
        val longSide = max(bitmap.width, bitmap.height)
        if (longSide <= maxSide) return bitmap
        val ratio = maxSide.toFloat() / longSide
        val width = (bitmap.width * ratio).roundToInt()
        val height = (bitmap.height * ratio).roundToInt()
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    /** As in the T8 demo: bitmap.compress into the file's stream. */
    private fun writeJpeg(bitmap: Bitmap, file: File, quality: Int) {
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it) }
    }
}

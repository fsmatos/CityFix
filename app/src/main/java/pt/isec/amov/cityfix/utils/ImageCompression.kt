package pt.isec.amov.cityfix.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import androidx.exifinterface.media.ExifInterface
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

object ImageCompression {

    const val MAX_SIDE = 1600
    const val QUALITY = 80
    const val THUMB_SIDE = 256
    const val THUMB_QUALITY = 70

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

    private fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun scaleDown(bitmap: Bitmap, maxSide: Int): Bitmap {
        val longSide = max(bitmap.width, bitmap.height)
        if (longSide <= maxSide) return bitmap
        val ratio = maxSide.toFloat() / longSide
        val width = (bitmap.width * ratio).roundToInt()
        val height = (bitmap.height * ratio).roundToInt()
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun writeJpeg(bitmap: Bitmap, file: File, quality: Int) {
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it) }
    }
}

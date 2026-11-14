package pt.isec.amov.cityfix.data

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.storage.storageMetadata
import kotlinx.coroutines.tasks.await
import pt.isec.amov.cityfix.CityFixApplication
import pt.isec.amov.cityfix.utils.FileUtils
import pt.isec.amov.cityfix.utils.ImageCompression
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

/**
 * Uploads the profile photo chosen at registration to `users/{uid}/photo.jpg` and stores its https
 * download URL in `users/{uid}.photoPath` (decision 0010, D-010 Q4). ProfilePhotoUploader enqueues
 * it with a network constraint, after the registration transaction committed.
 *
 * Each attempt compresses the picked file with ImageCompression (long side 1600 px, JPEG, no EXIF;
 * the thumbnail it also writes is not needed and is deleted), uploads, then updates the document.
 * An exception means Result.retry() (exponential backoff); after [MAX_ATTEMPTS] the photo is given
 * up: the account is fine without it. The local files are deleted in every final case.
 */
class ProfilePhotoWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val uid = inputData.getString(KEY_UID) ?: return Result.failure()
        val source = inputData.getString(KEY_PATH) ?: return Result.failure()
        if (!File(source).exists()) return Result.failure()

        // The app container, as UploadWorker reads it.
        val app = applicationContext as CityFixApplication
        val target = FileUtils.mediaFile(applicationContext, "profile_$uid.jpg")
        val thumbnail = FileUtils.mediaFile(applicationContext, "profile_${uid}_thumb.jpg")

        val compressed = ImageCompression.compressImage(source, target, thumbnail)
        thumbnail.delete()
        if (!compressed) {
            // Not decodable (for example HEIC on an old device): retrying would not help.
            File(source).delete()
            target.delete()
            return Result.failure()
        }

        return try {
            val ref = app.storage.reference.child("users/$uid/photo.jpg")
            ref.putFile(Uri.fromFile(target), storageMetadata { contentType = "image/jpeg" }).await()
            val url = ref.downloadUrl.await()
            app.firestore.collection("users").document(uid).update("photoPath", url.toString()).await()
            File(source).delete()
            target.delete()
            Result.success()
        } catch (e: CancellationException) {
            throw e // WorkManager stopped the worker (e.g. no network): it runs again later.
        } catch (e: Exception) {
            Log.w(TAG, "profile photo upload failed (attempt $runAttemptCount)", e)
            if (runAttemptCount + 1 >= MAX_ATTEMPTS) {
                File(source).delete()
                target.delete()
                Result.failure()
            } else {
                Result.retry()
            }
        }
    }

    companion object {
        private const val TAG = "ProfilePhotoWorker"
        private const val MAX_ATTEMPTS = 5
        const val KEY_UID = "uid"
        const val KEY_PATH = "path"
    }
}

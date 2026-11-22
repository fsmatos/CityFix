package pt.isec.amov.cityfix.data

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.storageMetadata
import kotlinx.coroutines.tasks.await
import pt.isec.amov.cityfix.CityFixApplication
import pt.isec.amov.cityfix.utils.FileUtils
import pt.isec.amov.cityfix.utils.ImageCompression
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

class ProfilePhotoWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val uid = inputData.getString(KEY_UID) ?: return Result.failure()
        val source = inputData.getString(KEY_PATH) ?: return Result.failure()
        if (!File(source).exists()) {
            // Deleted before the upload ran (for example a process death right after Submit).
            Log.w(TAG, "profile photo file is gone, nothing to upload")
            return Result.failure()
        }

        // The app container, as UploadWorker reads it.
        val app = applicationContext as CityFixApplication
        if (app.auth.currentUser?.uid != uid) {
            Log.w(TAG, "the photo's user is not signed in any more, giving up")
            File(source).delete()
            return Result.failure()
        }
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
            app.firestore.collection("users").document(uid)
                .update(mapOf("photoPath" to url.toString(), "photoUpdatedAt" to System.currentTimeMillis()))
                .await()
            File(source).delete()
            target.delete()
            Result.success()
        } catch (e: CancellationException) {
            throw e // WorkManager stopped the worker (e.g. no network): it runs again later.
        } catch (e: Exception) {
            Log.w(TAG, "profile photo upload failed (attempt $runAttemptCount)", e)
            if (isRefused(e) || runAttemptCount + 1 >= MAX_ATTEMPTS) {
                File(source).delete()
                target.delete()
                Result.failure()
            } else {
                Result.retry()
            }
        }
    }

    private fun isRefused(e: Exception): Boolean = when (e) {
        is StorageException ->
            e.errorCode == StorageException.ERROR_NOT_AUTHORIZED ||
                e.errorCode == StorageException.ERROR_NOT_AUTHENTICATED
        is FirebaseFirestoreException ->
            e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                e.code == FirebaseFirestoreException.Code.UNAUTHENTICATED
        else -> false
    }

    companion object {
        private const val TAG = "ProfilePhotoWorker"
        private const val MAX_ATTEMPTS = 5
        const val KEY_UID = "uid"
        const val KEY_PATH = "path"
    }
}

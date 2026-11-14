package pt.isec.amov.cityfix.data

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storageMetadata
import kotlinx.coroutines.tasks.await
import pt.isec.amov.cityfix.CityFixApplication
import pt.isec.amov.cityfix.model.UploadState
import java.io.File

/**
 * Uploads one prepared attachment (file + thumbnail) to Firebase Storage and marks it UPLOADED
 * in its Firestore document (brief IV). AttachmentUploader.enqueue creates it with a network
 * constraint, so WorkManager runs it when there is a connection, even after the app was closed.
 *
 * As on the slides 08 example: a CoroutineWorker, the file path from inputData, Result.retry()
 * on an exception (WorkManager waits longer each time, exponential backoff).
 * Beyond class: Storage putFile + downloadUrl (the class names Storage only), and
 * FieldValue.arrayUnion for the public thumbnail. The Firestore entry is changed with a plain
 * get() then update(), like the T8 demo's get/set. After [MAX_ATTEMPTS] failed attempts the
 * entry is marked FAILED, so the detail screen can say so instead of "pending" forever.
 *
 * Storage paths (decision 0010): tickets/{ticketId}/{id}.jpg or .mp4, and {id}_thumb.jpg.
 */
class UploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val documentPath = inputData.getString(KEY_DOCUMENT) ?: return Result.failure()
        val ticketId = inputData.getString(KEY_TICKET_ID) ?: return Result.failure()
        val attachmentId = inputData.getString(KEY_ATTACHMENT_ID) ?: return Result.failure()
        val file = inputData.getString(KEY_FILE) ?: return Result.failure()
        val thumbnail = inputData.getString(KEY_THUMBNAIL)
        val isVideo = inputData.getBoolean(KEY_IS_VIDEO, false)
        val addPublicThumbnail = inputData.getBoolean(KEY_PUBLIC_THUMBNAIL, false)

        // The app container, as ProximityService reads it in P10.
        val app = applicationContext as CityFixApplication
        val docRef = app.firestore.document(documentPath)
        return try {
            val extension = if (isVideo) "mp4" else "jpg"
            val type = if (isVideo) "video/mp4" else "image/jpeg"
            val fileUrl = upload(app.storage, "tickets/$ticketId/$attachmentId.$extension", file, type)
            val thumbUrl = thumbnail?.let {
                upload(app.storage, "tickets/$ticketId/${attachmentId}_thumb.jpg", it, "image/jpeg")
            }

            // Known limit (Gate 1 Q1): two workers of one ticket finishing together can
            // overwrite each other's entry, since this is a plain get() then update().
            val list = docRef.get().await().attachmentsWith(attachmentId) {
                it.copy(file = fileUrl, thumbnail = thumbUrl, uploadState = UploadState.UPLOADED)
            } ?: throw IllegalStateException("attachment $attachmentId not found")

            val batch = app.firestore.batch()
            batch.update(docRef, "attachments", list)
            if (addPublicThumbnail && thumbUrl != null) {
                batch.update(
                    app.firestore.collection(PUBLIC_TICKETS).document(ticketId),
                    "thumbnails",
                    FieldValue.arrayUnion(thumbUrl),
                )
            }
            batch.commit().await()

            // Uploaded: the local copies are no longer needed (Gate 1 Q3).
            File(file).delete()
            thumbnail?.let { File(it).delete() }
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "upload failed (attempt $runAttemptCount)", e)
            if (runAttemptCount + 1 >= MAX_ATTEMPTS) {
                markFailed(docRef, attachmentId)
                Result.failure()
            } else {
                Result.retry()
            }
        }
    }

    /** Uploads the local file to [path] and returns its https download URL. */
    private suspend fun upload(storage: FirebaseStorage, path: String, local: String, type: String): String {
        val ref = storage.reference.child(path)
        ref.putFile(Uri.fromFile(File(local)), storageMetadata { contentType = type }).await()
        return ref.downloadUrl.await().toString()
    }

    /** Sets the entry to FAILED (paths unchanged). If Firestore cannot be reached, only logs. */
    private suspend fun markFailed(docRef: DocumentReference, attachmentId: String) {
        try {
            val list = docRef.get().await().attachmentsWith(attachmentId) {
                it.copy(uploadState = UploadState.FAILED)
            } ?: return
            docRef.update("attachments", list).await()
        } catch (e: Exception) {
            Log.w(TAG, "could not mark $attachmentId as failed", e)
        }
    }

    companion object {
        private const val TAG = "UploadWorker"
        const val KEY_DOCUMENT = "document"
        const val KEY_TICKET_ID = "ticketId"
        const val KEY_ATTACHMENT_ID = "attachmentId"
        const val KEY_IS_VIDEO = "isVideo"
        const val KEY_FILE = "file"
        const val KEY_THUMBNAIL = "thumbnail"
        const val KEY_PUBLIC_THUMBNAIL = "publicThumbnail"
        const val MAX_ATTEMPTS = 5
    }
}

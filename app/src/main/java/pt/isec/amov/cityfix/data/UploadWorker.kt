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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import pt.isec.amov.cityfix.CityFixApplication
import pt.isec.amov.cityfix.model.Attachment
import pt.isec.amov.cityfix.model.AttachmentType
import pt.isec.amov.cityfix.model.UploadState
import java.io.File

/**
 * Uploads the prepared media of one Firestore document (a ticket, or a joiner's report) to
 * Firebase Storage and marks each entry UPLOADED (brief IV). AttachmentUploader.enqueue creates
 * it with a network constraint, so WorkManager runs it when there is a connection, even after
 * the app was closed.
 *
 * It reads the document and, one entry at a time, uploads every PENDING entry whose local file
 * is on this device (file + thumbnail), then writes that entry back. One worker per document, so
 * the get() then update() calls for that document run one after the other and cannot overwrite
 * each other (Gate 1 Q1: no transaction).
 *
 * As on the slides 08 example: a CoroutineWorker, the document path from inputData,
 * Result.retry() on an exception (WorkManager waits longer each time, exponential backoff); the
 * next run resumes with the entries still PENDING. Beyond class: Storage putFile + downloadUrl
 * (the class names Storage only), and FieldValue.arrayUnion for the public thumbnail. The
 * Firestore entry is changed with a plain get() then update(), like the T8 demo's get/set.
 * After [MAX_ATTEMPTS] failed attempts the entries still PENDING are marked FAILED, so the
 * detail screen can say so instead of "pending" forever.
 *
 * Storage paths (decision 0010): tickets/{ticketId}/{id}.jpg or .mp4, and {id}_thumb.jpg.
 */
class UploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val documentPath = inputData.getString(KEY_DOCUMENT) ?: return Result.failure()
        val ticketId = inputData.getString(KEY_TICKET_ID) ?: return Result.failure()
        val addPublicThumbnail = inputData.getBoolean(KEY_PUBLIC_THUMBNAIL, false)

        // The app container, as ProximityService reads it in P10.
        val app = applicationContext as CityFixApplication
        val docRef = app.firestore.document(documentPath)
        return try {
            val snapshot = docRef.get().await()
            // The batch may not be on the server yet: retry later.
            if (!snapshot.exists()) throw IllegalStateException("$documentPath not found")

            for (attachment in snapshot.attachments().filter { isLocalPending(it) }) {
                uploadOne(app, docRef, ticketId, attachment, addPublicThumbnail)
            }
            Result.success()
        } catch (e: CancellationException) {
            throw e // WorkManager stopped the worker (e.g. no network): it runs again later.
        } catch (e: Exception) {
            Log.w(TAG, "upload failed (attempt $runAttemptCount)", e)
            if (runAttemptCount + 1 >= MAX_ATTEMPTS) {
                markFailed(docRef)
                Result.failure()
            } else {
                Result.retry()
            }
        }
    }

    /** Uploads [attachment] and its thumbnail, writes the URLs to its entry, deletes the local copies. */
    private suspend fun uploadOne(
        app: CityFixApplication,
        docRef: DocumentReference,
        ticketId: String,
        attachment: Attachment,
        addPublicThumbnail: Boolean,
    ) {
        val isVideo = attachment.type == AttachmentType.VIDEO
        val extension = if (isVideo) "mp4" else "jpg"
        val type = if (isVideo) "video/mp4" else "image/jpeg"
        val fileUrl = upload(app.storage, "tickets/$ticketId/${attachment.id}.$extension", attachment.file, type)
        val thumbUrl = attachment.thumbnail?.let {
            upload(app.storage, "tickets/$ticketId/${attachment.id}_thumb.jpg", it, "image/jpeg")
        }

        val list = docRef.get().await().attachmentsWith {
            if (it.id == attachment.id) {
                it.copy(file = fileUrl, thumbnail = thumbUrl, uploadState = UploadState.UPLOADED)
            } else {
                it
            }
        }
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
        File(attachment.file).delete()
        attachment.thumbnail?.let { File(it).delete() }
    }

    /** Uploads the local file to [path] and returns its https download URL. */
    private suspend fun upload(storage: FirebaseStorage, path: String, local: String, type: String): String {
        val ref = storage.reference.child(path)
        ref.putFile(Uri.fromFile(File(local)), storageMetadata { contentType = type }).await()
        return ref.downloadUrl.await().toString()
    }

    /** Sets this device's entries still PENDING to FAILED (paths unchanged). If Firestore cannot be reached, only logs. */
    private suspend fun markFailed(docRef: DocumentReference) {
        try {
            val list = docRef.get().await().attachmentsWith {
                if (isLocalPending(it)) it.copy(uploadState = UploadState.FAILED) else it
            }
            docRef.update("attachments", list).await()
        } catch (e: Exception) {
            Log.w(TAG, "could not mark $docRef as failed", e)
        }
    }

    /**
     * PENDING with its file on this device. Another device's PENDING entries (e.g. the creator's
     * when the operator adds resolution media to the same ticket) are left to that device.
     */
    private fun isLocalPending(attachment: Attachment): Boolean =
        attachment.uploadState == UploadState.PENDING && File(attachment.file).exists()

    companion object {
        private const val TAG = "UploadWorker"
        const val KEY_DOCUMENT = "document"
        const val KEY_TICKET_ID = "ticketId"
        const val KEY_PUBLIC_THUMBNAIL = "publicThumbnail"
        const val MAX_ATTEMPTS = 5
    }
}

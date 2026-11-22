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

    private suspend fun upload(storage: FirebaseStorage, path: String, local: String, type: String): String {
        val ref = storage.reference.child(path)
        ref.putFile(Uri.fromFile(File(local)), storageMetadata { contentType = type }).await()
        return ref.downloadUrl.await().toString()
    }

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

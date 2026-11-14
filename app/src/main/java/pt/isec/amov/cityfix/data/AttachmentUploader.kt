package pt.isec.amov.cityfix.data

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pt.isec.amov.cityfix.model.Attachment
import pt.isec.amov.cityfix.model.AttachmentType
import pt.isec.amov.cityfix.model.UploadState
import pt.isec.amov.cityfix.utils.FileUtils
import pt.isec.amov.cityfix.utils.ImageCompression
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Prepares report media for upload (brief IV): compressed copy and thumbnail in filesDir/images,
 * named by a new UUID; then enqueues one UploadWorker per attachment. One entry point for the citizen's report (F-013) and the operator's
 * resolution proof (D-013). Create one per app (CityFixApplication); it keeps the application
 * context only.
 */
class AttachmentUploader(context: Context) {

    private val appContext = context.applicationContext

    /**
     * Runs on Dispatchers.IO. Photo: compressed JPEG + thumbnail (ImageCompression), the original
     * deleted. Video: moved as recorded to the new name, plus a thumbnail of one frame (a missing
     * thumbnail alone does not fail the video). Returns the attachment with the new id, paths,
     * PENDING and [uploadedById]; FAILED (original path and id kept) when the photo cannot be
     * compressed or the video cannot be moved.
     */
    suspend fun prepare(attachment: Attachment, uploadedById: String): Attachment =
        withContext(Dispatchers.IO) {
            val id = UUID.randomUUID().toString()
            val thumb = FileUtils.mediaFile(appContext, "${id}_thumb.jpg")
            val target: File
            val ok: Boolean
            if (attachment.type == AttachmentType.IMAGE) {
                target = FileUtils.mediaFile(appContext, "$id.jpg")
                ok = ImageCompression.compressImage(attachment.file, target, thumb)
                if (ok) File(attachment.file).delete()
            } else {
                target = FileUtils.mediaFile(appContext, "$id.mp4")
                ok = File(attachment.file).renameTo(target)
                if (ok) ImageCompression.videoThumbnail(target.path, thumb)
            }

            if (ok) {
                attachment.copy(
                    id = id,
                    file = target.path,
                    thumbnail = if (thumb.exists()) thumb.path else null,
                    uploadState = UploadState.PENDING,
                    uploadedById = uploadedById,
                )
            } else {
                target.delete()
                thumb.delete()
                attachment.copy(uploadState = UploadState.FAILED, uploadedById = uploadedById)
            }
        }

    /**
     * One OneTimeWorkRequest for [attachment] (slides 08: inputData, Constraints, enqueue): it runs
     * only with a network connection and, after a failed attempt, waits 30 s, then longer each
     * time (exponential backoff). Call after the batch that stores [attachment] in
     * [documentPath] (the ticket, or the joiner's report). [addPublicThumbnail] = the thumbnail
     * also goes to publicTickets/{ticketId} (creator's REPORT media of a PUBLIC ticket).
     */
    fun enqueue(documentPath: String, ticketId: String, attachment: Attachment, addPublicThumbnail: Boolean) {
        val request = OneTimeWorkRequestBuilder<UploadWorker>()
            .setInputData(
                workDataOf(
                    UploadWorker.KEY_DOCUMENT to documentPath,
                    UploadWorker.KEY_TICKET_ID to ticketId,
                    UploadWorker.KEY_ATTACHMENT_ID to attachment.id,
                    UploadWorker.KEY_IS_VIDEO to (attachment.type == AttachmentType.VIDEO),
                    UploadWorker.KEY_FILE to attachment.file,
                    UploadWorker.KEY_THUMBNAIL to attachment.thumbnail,
                    UploadWorker.KEY_PUBLIC_THUMBNAIL to addPublicThumbnail,
                ),
            )
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(appContext).enqueue(request)
    }
}

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

class AttachmentUploader(context: Context) {

    private val appContext = context.applicationContext

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

    fun enqueue(documentPath: String, ticketId: String, addPublicThumbnail: Boolean) {
        val request = OneTimeWorkRequestBuilder<UploadWorker>()
            .setInputData(
                workDataOf(
                    UploadWorker.KEY_DOCUMENT to documentPath,
                    UploadWorker.KEY_TICKET_ID to ticketId,
                    UploadWorker.KEY_PUBLIC_THUMBNAIL to addPublicThumbnail,
                ),
            )
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(appContext).enqueue(request)
    }
}

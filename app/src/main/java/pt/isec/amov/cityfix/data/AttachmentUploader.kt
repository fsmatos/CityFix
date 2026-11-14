package pt.isec.amov.cityfix.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pt.isec.amov.cityfix.model.Attachment
import pt.isec.amov.cityfix.model.AttachmentType
import pt.isec.amov.cityfix.model.UploadState
import pt.isec.amov.cityfix.utils.FileUtils
import pt.isec.amov.cityfix.utils.ImageCompression
import java.io.File
import java.util.UUID

/**
 * Prepares report media for upload (brief IV): compressed copy and thumbnail in filesDir/images,
 * named by a new UUID. One entry point for the citizen's report (F-013) and the operator's
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
}

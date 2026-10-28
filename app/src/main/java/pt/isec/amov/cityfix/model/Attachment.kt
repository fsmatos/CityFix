package pt.isec.amov.cityfix.model

/** Image or video attached to a ticket ("Anexo" in the diagram). */
data class Attachment(
    /** Firestore document id. Empty until persisted; sample data must set distinct ids. */
    val id: String = "",
    val type: AttachmentType = AttachmentType.IMAGE,
    /** Local content/file URI before upload, Storage path or URL after it. */
    val file: String = "",
    val thumbnail: String? = null,
    val uploadState: UploadState = UploadState.PENDING,
    val uploadedById: String = "",
)

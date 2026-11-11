package pt.isec.amov.cityfix.model

/** Image or video attached to a ticket ("Anexo" in the diagram). */
data class Attachment(
    /**
     * Unique per ticket; the local file name before upload (F-007), later the Storage/Firestore id.
     * Sample data must set distinct ids.
     */
    val id: String = "",
    val type: AttachmentType = AttachmentType.IMAGE,
    /** Absolute local path before upload, Storage path or URL after it. */
    val file: String = "",
    val thumbnail: String? = null,
    val uploadState: UploadState = UploadState.PENDING,
    val uploadedById: String = "",
    /** REPORT for media added when reporting; RESOLUTION for the operator's proof (set by transition). */
    val phase: AttachmentPhase = AttachmentPhase.REPORT,
)

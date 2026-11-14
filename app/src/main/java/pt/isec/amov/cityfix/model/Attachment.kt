package pt.isec.amov.cityfix.model

/** Image or video attached to a ticket ("Anexo" in the diagram). */
data class Attachment(
    /**
     * A random UUID set by AttachmentUploader.prepare (F-013), also the local and Storage file
     * name. Sample data must set distinct ids.
     */
    val id: String = "",
    val type: AttachmentType = AttachmentType.IMAGE,
    /** Absolute local path until uploaded, then the https download URL. Same for [thumbnail]. */
    val file: String = "",
    val thumbnail: String? = null,
    val uploadState: UploadState = UploadState.PENDING,
    val uploadedById: String = "",
    /** REPORT for media added when reporting; RESOLUTION for the operator's proof (set by transition). */
    val phase: AttachmentPhase = AttachmentPhase.REPORT,
)

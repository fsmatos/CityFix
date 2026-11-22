package pt.isec.amov.cityfix.model

data class Attachment(
    val id: String = "",
    val type: AttachmentType = AttachmentType.IMAGE,
    val file: String = "",
    val thumbnail: String? = null,
    val uploadState: UploadState = UploadState.PENDING,
    val uploadedById: String = "",
    val phase: AttachmentPhase = AttachmentPhase.REPORT,
)

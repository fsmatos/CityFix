package pt.isec.amov.cityfix.model

/** WorkManager upload progress of an attachment (PENDENTE, A_ENVIAR, ENVIADO, FALHOU in the diagram). */
enum class UploadState { PENDING, UPLOADING, UPLOADED, FAILED }

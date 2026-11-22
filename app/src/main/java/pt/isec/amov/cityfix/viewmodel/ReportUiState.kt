package pt.isec.amov.cityfix.viewmodel

import androidx.annotation.StringRes
import pt.isec.amov.cityfix.model.Attachment
import pt.isec.amov.cityfix.model.Category
import pt.isec.amov.cityfix.model.Coordinates
import pt.isec.amov.cityfix.model.Scope
import pt.isec.amov.cityfix.model.Situation

const val MAX_REPORT_ATTACHMENTS = 3

data class ReportUiState(
    val categories: List<Category> = emptyList(),
    val categoryId: String = "",
    val categoryName: String = "",
    val situation: Situation? = null,
    val description: String = "",
    val scope: Scope? = null,
    val attachments: List<Attachment> = emptyList(),
    val location: Coordinates? = null,
    val isLocating: Boolean = false,
    val locationPermissionDenied: Boolean = false,
    @StringRes val locationError: Int? = null,
    @StringRes val categoryError: Int? = null,
    @StringRes val situationError: Int? = null,
    @StringRes val descriptionError: Int? = null,
    @StringRes val scopeError: Int? = null,
    @StringRes val attachmentsError: Int? = null,
    @StringRes val attachmentsNotice: Int? = null,
    val showErrorSummary: Boolean = false,
    @StringRes val submitError: Int? = null,
    val submitted: Boolean = false,
    val isSaving: Boolean = false,
    val isCopyingAttachments: Boolean = false,
) {
    val remainingAttachments: Int
        get() = (MAX_REPORT_ATTACHMENTS - attachments.size).coerceAtLeast(0)

    val canAddAttachment: Boolean
        get() = remainingAttachments > 0 && !submitted && !isCopyingAttachments

    val isValid: Boolean
        get() = categoryId.isNotBlank() && categories.any { it.id == categoryId } &&
            situation != null && scope != null && description.isNotBlank() &&
            attachments.isNotEmpty() && location != null &&
            categoryError == null && situationError == null &&
            descriptionError == null && scopeError == null && attachmentsError == null
}

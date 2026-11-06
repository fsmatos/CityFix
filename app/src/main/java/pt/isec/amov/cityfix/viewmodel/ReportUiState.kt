package pt.isec.amov.cityfix.viewmodel

import androidx.annotation.StringRes
import pt.isec.amov.cityfix.model.Attachment
import pt.isec.amov.cityfix.model.Category
import pt.isec.amov.cityfix.model.Scope
import pt.isec.amov.cityfix.model.Situation

/** Photos and videos allowed on one report, counted across every pick and the camera. */
const val MAX_REPORT_ATTACHMENTS = 3

/**
 * Report form (brief I.2.E). Nothing is preselected: null / blank means "not chosen".
 * Errors are string resource ids, so the ViewModel holds no text and they are localised.
 * attachmentsError is the required rule and blocks; attachmentsNotice only informs.
 */
data class ReportUiState(
    /** Active categories only (I.3.E). */
    val categories: List<Category> = emptyList(),
    val categoryId: String = "",
    val categoryName: String = "",
    val situation: Situation? = null,
    val description: String = "",
    val scope: Scope? = null,
    /** Local copies in filesDir/images; `file` is an absolute path. */
    val attachments: List<Attachment> = emptyList(),
    @StringRes val categoryError: Int? = null,
    @StringRes val situationError: Int? = null,
    @StringRes val descriptionError: Int? = null,
    @StringRes val scopeError: Int? = null,
    @StringRes val attachmentsError: Int? = null,
    @StringRes val attachmentsNotice: Int? = null,
    /** Shown after a Submit with missing fields; TalkBack announces it. */
    val showErrorSummary: Boolean = false,
    /** Set when the caller could not create the ticket. */
    @StringRes val submitError: Int? = null,
    /** True after a valid submit; blocks a second one. */
    val submitted: Boolean = false,
) {
    val remainingAttachments: Int
        get() = MAX_REPORT_ATTACHMENTS - attachments.size

    val canAddAttachment: Boolean
        get() = remainingAttachments > 0 && !submitted

    val isValid: Boolean
        get() = categoryId.isNotBlank() && categories.any { it.id == categoryId } &&
            situation != null && scope != null && description.isNotBlank() &&
            categoryError == null && situationError == null &&
            descriptionError == null && scopeError == null
}

/** Validated values, ready for TicketsViewModel.reportTicket(...). Description is trimmed. */
data class ReportSubmission(
    val categoryId: String,
    val categoryName: String,
    val situation: Situation,
    val description: String,
    val scope: Scope,
    val attachments: List<Attachment>,
)

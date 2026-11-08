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
    /** Set when the repository could not create the ticket. */
    @StringRes val submitError: Int? = null,
    /** True after a valid submit; blocks a second one. Cleared only when the save fails. */
    val submitted: Boolean = false,
    /** True while the repository saves the ticket. */
    val isSaving: Boolean = false,
    /** True while picked media are copied into filesDir/images; blocks adding more and Submit. */
    val isCopyingAttachments: Boolean = false,
) {
    val remainingAttachments: Int
        get() = (MAX_REPORT_ATTACHMENTS - attachments.size).coerceAtLeast(0)

    val canAddAttachment: Boolean
        get() = remainingAttachments > 0 && !submitted && !isCopyingAttachments

    val isValid: Boolean
        get() = categoryId.isNotBlank() && categories.any { it.id == categoryId } &&
            situation != null && scope != null && description.isNotBlank() &&
            attachments.isNotEmpty() &&
            categoryError == null && situationError == null &&
            descriptionError == null && scopeError == null && attachmentsError == null
}

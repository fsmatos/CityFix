package pt.isec.amov.cityfix.viewmodel

import androidx.annotation.StringRes
import pt.isec.amov.cityfix.model.Category
import pt.isec.amov.cityfix.model.Scope
import pt.isec.amov.cityfix.model.Situation

/**
 * Report form (brief I.2.E). Nothing is preselected: null / blank means "not chosen".
 * Errors are string resource ids, so the ViewModel holds no text and they are localised.
 * Attachments come in F-007 (P09): add `attachments` and `@StringRes attachmentsError` after
 * `scope`, and `attachments.isNotEmpty() && attachmentsError == null` to isValid
 * ("deve ser carregada uma imagem/vídeo"); ReportViewModel.submit() must also set
 * `attachmentsError` when the list is empty. submit() gates on isValid, so that rule is enough
 * to block the submit. Until then a report has no image/video (I.2.E gap).
 */
data class ReportUiState(
    /** Active categories only (I.3.E). */
    val categories: List<Category> = emptyList(),
    val categoryId: String = "",
    val categoryName: String = "",
    val situation: Situation? = null,
    val description: String = "",
    val scope: Scope? = null,
    @StringRes val categoryError: Int? = null,
    @StringRes val situationError: Int? = null,
    @StringRes val descriptionError: Int? = null,
    @StringRes val scopeError: Int? = null,
    /** Shown after a Submit with missing fields; TalkBack announces it. */
    val showErrorSummary: Boolean = false,
    /** Set when the caller could not create the ticket. */
    @StringRes val submitError: Int? = null,
    /** True after a valid submit; blocks a second one. */
    val submitted: Boolean = false,
) {
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
)

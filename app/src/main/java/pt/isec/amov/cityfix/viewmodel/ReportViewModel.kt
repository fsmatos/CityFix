package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.data.TicketsRepository
import pt.isec.amov.cityfix.model.Attachment
import pt.isec.amov.cityfix.model.AttachmentType
import pt.isec.amov.cityfix.model.Category
import pt.isec.amov.cityfix.model.Scope
import pt.isec.amov.cityfix.model.Situation
import pt.isec.amov.cityfix.utils.FileUtils
import java.io.File

/**
 * Form-only ViewModel for the report screen. Create it per route (viewModel() inside
 * composable<ReportTicketRoute>, with ReportViewModelFactory): it survives rotation and is cleared
 * when the user leaves. Saves the ticket through [repository] (like EditContactViewModel.saveContact
 * in Contacts_#3); categories come from it too, active ones only.
 */
class ReportViewModel(private val repository: TicketsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ReportUiState(categories = repository.activeCategories()),
    )

    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    fun onCategorySelected(category: Category) {
        _uiState.update {
            it.copy(
                categoryId = category.id,
                categoryName = category.name,
                categoryError = null,
                submitError = null,
            ).withSummaryRefreshed()
        }
    }

    fun onSituationSelected(situation: Situation) {
        _uiState.update {
            it.copy(situation = situation, situationError = null, submitError = null)
                .withSummaryRefreshed()
        }
    }

    fun onDescriptionChange(text: String) {
        _uiState.update {
            it.copy(
                description = text,
                descriptionError = if (text.isBlank()) R.string.report_error_description else null,
                submitError = null,
            ).withSummaryRefreshed()
        }
    }

    fun onScopeSelected(scope: Scope) {
        _uiState.update {
            it.copy(scope = scope, scopeError = null, submitError = null).withSummaryRefreshed()
        }
    }

    /** [paths] are local copies already in filesDir/images. Empty when every copy failed. */
    fun onPhotosAttached(paths: List<String>) {
        if (paths.isEmpty()) return
        _uiState.update {
            it.copy(
                attachments = it.attachments + paths.map(::attachmentFor),
                attachmentsError = null,
                attachmentsNotice = null,
                submitError = null,
            ).withSummaryRefreshed()
        }
    }

    fun onAttachmentCopyFailed() {
        _uiState.update { it.copy(attachmentsNotice = R.string.report_attachment_copy_failed) }
    }

    fun onAttachmentLimitReached() {
        _uiState.update { it.copy(attachmentsNotice = R.string.report_attachments_limit) }
    }

    fun onCameraUnavailable() {
        _uiState.update { it.copy(attachmentsNotice = R.string.report_camera_failed) }
    }

    /** Deletes the local copy. Removing the last one sets the error, as deleting the description does. */
    fun onRemoveAttachment(attachment: Attachment) {
        File(attachment.file).delete()
        _uiState.update {
            val remaining = it.attachments - attachment
            it.copy(
                attachments = remaining,
                attachmentsError = if (remaining.isEmpty()) R.string.report_error_attachments else null,
                attachmentsNotice = null,
                submitError = null,
            ).withSummaryRefreshed()
        }
    }

    /**
     * Sets the error of every missing field, then gates on [ReportUiState.isValid] (P08 slide 15),
     * so isValid is the one rule that decides. When valid, locks the form (submitted) before the
     * save starts, so a second tap does nothing, and saves in viewModelScope. A saved ticket calls
     * [onSuccess] (no id: a PRIVATE ticket must not be opened, D-006); a refused one unlocks the
     * form and sets submitError = R.string.report_error_not_created.
     */
    fun submit(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.submitted || state.isSaving) return

        // Locals so the null checks below smart-cast (no !!).
        val situation = state.situation
        val scope = state.scope
        val categoryMissing = state.categoryId.isBlank() ||
            state.categories.none { it.id == state.categoryId }

        val checked = state.copy(
            categoryError = if (categoryMissing) R.string.report_error_category else null,
            situationError = if (situation == null) R.string.report_error_situation else null,
            descriptionError =
                if (state.description.isBlank()) R.string.report_error_description else null,
            scopeError = if (scope == null) R.string.report_error_scope else null,
            attachmentsError =
                if (state.attachments.isEmpty()) R.string.report_error_attachments else null,
        )
        if (!checked.isValid || situation == null || scope == null) {
            _uiState.value = checked.copy(showErrorSummary = true)
            return
        }

        _uiState.value = checked.copy(
            submitted = true,
            isSaving = true,
            showErrorSummary = false,
            submitError = null,
        )
        viewModelScope.launch {
            val id = repository.reportTicket(
                description = checked.description.trim(),
                categoryId = checked.categoryId,
                categoryName = checked.categoryName,
                situation = situation,
                scope = scope,
                attachments = checked.attachments,
            )
            if (id != null) {
                _uiState.update { it.copy(isSaving = false) }
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        submitted = false,
                        isSaving = false,
                        submitError = R.string.report_error_not_created,
                    )
                }
            }
        }
    }

    /** Leaving without submitting discards the draft: its files have no ticket to belong to. */
    override fun onCleared() {
        val state = _uiState.value
        if (!state.submitted) {
            state.attachments.forEach { File(it.file).delete() }
        }
    }

    private fun attachmentFor(path: String): Attachment = Attachment(
        id = File(path).name,
        type = if (FileUtils.isVideoPath(path)) AttachmentType.VIDEO else AttachmentType.IMAGE,
        file = path,
    )

    /** The summary stays only while something is still missing. */
    private fun ReportUiState.withSummaryRefreshed(): ReportUiState =
        copy(showErrorSummary = showErrorSummary && !isValid)
}

package pt.isec.amov.cityfix.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.data.AttachmentStorage
import pt.isec.amov.cityfix.data.LocationRepository
import pt.isec.amov.cityfix.data.ReportOutcome
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
 * in Contacts_#3) as reported by [creatorId]; categories come from it too, active ones only, and
 * follow changes while the form is open. Picked media are copied off the main thread by
 * [attachmentStorage]. The ticket's location is the reporter's GPS fix from [locationRepository],
 * read when the form opens and on "Refresh location", so Submit does not wait for GPS.
 * [fromImpact] (F-015): opened from an impact suggestion, so Damage and the Pavement category are
 * chosen; the user can still change them.
 */
class ReportViewModel(
    private val repository: TicketsRepository,
    private val attachmentStorage: AttachmentStorage,
    private val locationRepository: LocationRepository,
    private val creatorId: String,
    fromImpact: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())

    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    init {
        if (fromImpact) {
            _uiState.update { it.copy(situation = Situation.DAMAGE) }
        }
        viewModelScope.launch {
            repository.activeCategories().collect { list ->
                _uiState.update { it.copy(categories = list) }
                // Only while no category is chosen, so a later user choice is kept.
                val pavement = list.find { it.id == PAVEMENT_ID }
                if (fromImpact && _uiState.value.categoryId.isBlank() && pavement != null) {
                    onCategorySelected(pavement)
                }
            }
        }
    }

    /** Gallery copies still running; isCopyingAttachments is true while this is above 0. */
    private var copiesInProgress = 0

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

    /** Called when the form opens (permission already decided at app start) and after Try again. */
    fun onLocationPermissionResult(granted: Boolean) {
        if (granted) {
            _uiState.update { it.copy(locationPermissionDenied = false, locationError = null) }
            refreshLocation()
        } else {
            _uiState.update {
                it.copy(locationPermissionDenied = true, locationError = null)
            }
        }
    }

    /** Reads one fix; without one the previous fix (if any) is kept and an error is shown. */
    fun refreshLocation() {
        if (_uiState.value.isLocating) return
        _uiState.update { it.copy(isLocating = true) }
        viewModelScope.launch {
            val fix = locationRepository.currentLocation()
            _uiState.update {
                it.copy(
                    location = fix ?: it.location,
                    locationError = if (fix == null) R.string.report_location_unavailable else null,
                    isLocating = false,
                    submitError = null,
                ).withSummaryRefreshed()
            }
        }
    }

    /**
     * Gallery pick: copies what fits under the limit in viewModelScope (off the main thread), then
     * attaches what copied. The add buttons and Submit stay disabled until every copy finishes.
     * The limit is checked again when the copy returns, because a camera photo or another pick
     * may have taken slots meanwhile: extra copies are deleted and the limit notice shown. If the
     * form was submitted meanwhile, the copies are deleted. If the user leaves mid-copy,
     * AttachmentStorage deletes them.
     */
    fun onMediaPicked(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val accepted = uris.take(_uiState.value.remainingAttachments)
        copiesInProgress++
        _uiState.update { it.copy(isCopyingAttachments = true) }
        viewModelScope.launch {
            try {
                val savedPaths = attachmentStorage.copyToInternalStorage(accepted)
                if (_uiState.value.submitted) {
                    savedPaths.forEach { File(it).delete() }
                    return@launch
                }
                val fits = savedPaths.take(_uiState.value.remainingAttachments)
                savedPaths.drop(fits.size).forEach { File(it).delete() }
                onPhotosAttached(fits)
                if (savedPaths.size < accepted.size) {
                    onAttachmentCopyFailed()
                } else if (fits.size < savedPaths.size || accepted.size < uris.size) {
                    onAttachmentLimitReached()
                }
            } finally {
                copiesInProgress--
                _uiState.update { it.copy(isCopyingAttachments = copiesInProgress > 0) }
            }
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
     * save starts, so a second tap does nothing, and saves in viewModelScope. A saved report calls
     * [onSuccess] with its [ReportOutcome] (no id: a PRIVATE ticket must not be opened, D-006); a
     * refused one unlocks the form and sets submitError = R.string.report_error_not_created.
     * ALREADY_FOLLOWING wrote nothing, so the draft's local files are deleted.
     */
    fun submit(onSuccess: (ReportOutcome) -> Unit) {
        val state = _uiState.value
        if (state.submitted || state.isSaving || state.isCopyingAttachments) return

        // Locals so the null checks below smart-cast (no !!).
        val situation = state.situation
        val scope = state.scope
        val location = state.location
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
            locationError =
                if (location == null) state.locationError ?: R.string.report_error_location else null,
        )
        if (!checked.isValid || situation == null || scope == null || location == null) {
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
            val outcome = repository.reportTicket(
                description = checked.description.trim(),
                categoryId = checked.categoryId,
                categoryName = checked.categoryName,
                situation = situation,
                scope = scope,
                attachments = checked.attachments,
                location = location,
                creatorId = creatorId,
            )
            if (outcome != null) {
                _uiState.update { it.copy(isSaving = false) }
                if (outcome == ReportOutcome.ALREADY_FOLLOWING) {
                    checked.attachments.forEach { File(it.file).delete() }
                }
                onSuccess(outcome)
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

    companion object {
        /** Category id of Pavement in DebugSeed / SampleCategories (F-015 Q8). */
        const val PAVEMENT_ID = "pavement"
    }
}

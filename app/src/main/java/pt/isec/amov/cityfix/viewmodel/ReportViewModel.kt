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

    private fun ReportUiState.withSummaryRefreshed(): ReportUiState =
        copy(showErrorSummary = showErrorSummary && !isValid)

    companion object {
        const val PAVEMENT_ID = "pavement"
    }
}

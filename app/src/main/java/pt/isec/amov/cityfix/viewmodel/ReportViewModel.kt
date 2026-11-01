package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.data.SampleCategories
import pt.isec.amov.cityfix.model.Category
import pt.isec.amov.cityfix.model.Scope
import pt.isec.amov.cityfix.model.Situation

/**
 * Form-only ViewModel for the report screen. Create it per route (viewModel() inside
 * composable<ReportTicketRoute>): it survives rotation and is cleared when the user leaves.
 * Does not create tickets: the caller does it with TicketsViewModel.reportTicket (D-006).
 */
class ReportViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        ReportUiState(categories = SampleCategories.all.filter { it.active }),
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

    /**
     * Sets the error of every missing field. Returns the data when valid and not yet submitted
     * (then locks), else null.
     */
    fun submit(): ReportSubmission? {
        val state = _uiState.value
        if (state.submitted) return null

        val situation = state.situation
        val scope = state.scope
        val categoryMissing = state.categoryId.isBlank() ||
            state.categories.none { it.id == state.categoryId }
        val descriptionMissing = state.description.isBlank()

        if (categoryMissing || situation == null || descriptionMissing || scope == null) {
            _uiState.value = state.copy(
                categoryError = if (categoryMissing) R.string.report_error_category else null,
                situationError = if (situation == null) R.string.report_error_situation else null,
                descriptionError =
                    if (descriptionMissing) R.string.report_error_description else null,
                scopeError = if (scope == null) R.string.report_error_scope else null,
                showErrorSummary = true,
            )
            return null
        }

        _uiState.value = state.copy(submitted = true, showErrorSummary = false, submitError = null)
        return ReportSubmission(
            categoryId = state.categoryId,
            categoryName = state.categoryName,
            situation = situation,
            description = state.description.trim(),
            scope = scope,
        )
    }

    /** Result of reportTicket: false unlocks and sets submitError = R.string.report_error_not_created. */
    fun onSubmitResult(created: Boolean) {
        if (created) return
        _uiState.update {
            it.copy(submitted = false, submitError = R.string.report_error_not_created)
        }
    }

    /** The summary stays only while something is still missing. */
    private fun ReportUiState.withSummaryRefreshed(): ReportUiState =
        copy(showErrorSummary = showErrorSummary && !isValid)
}

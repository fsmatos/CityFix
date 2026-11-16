package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.isec.amov.cityfix.data.CategoriesRepository
import pt.isec.amov.cityfix.model.TicketStatus

/**
 * The operations panel's filter and sort (brief I.3.E). Kept here, not in the composable, so the
 * choice survives rotation. Created inside the OperatorPanelRoute guard, so the categories
 * listener ends with the panel.
 */
class OperatorPanelViewModel(categories: CategoriesRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(OperatorPanelUiState())
    val uiState: StateFlow<OperatorPanelUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            categories.allCategories().collect { list ->
                _uiState.update { it.copy(categories = list) }
            }
        }
    }

    /** null = every status. */
    fun onStatusFilter(status: TicketStatus?) {
        _uiState.update { it.copy(filter = it.filter.copy(status = status)) }
    }

    /** null = every category. */
    fun onCategoryFilter(categoryId: String?) {
        _uiState.update { it.copy(filter = it.filter.copy(categoryId = categoryId)) }
    }

    /** Newest first <-> oldest first. */
    fun onToggleSort() {
        _uiState.update { it.copy(filter = it.filter.copy(newestFirst = !it.filter.newestFirst)) }
    }
}

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

    fun onStatusFilter(status: TicketStatus?) {
        _uiState.update { it.copy(filter = it.filter.copy(status = status)) }
    }

    fun onCategoryFilter(categoryId: String?) {
        _uiState.update { it.copy(filter = it.filter.copy(categoryId = categoryId)) }
    }

    fun onToggleSort() {
        _uiState.update { it.copy(filter = it.filter.copy(newestFirst = !it.filter.newestFirst)) }
    }
}

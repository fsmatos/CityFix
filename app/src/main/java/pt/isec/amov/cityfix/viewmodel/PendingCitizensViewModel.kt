package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.isec.amov.cityfix.data.UsersRepository

class PendingCitizensViewModel(users: UsersRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(PendingCitizensUiState())
    val uiState: StateFlow<PendingCitizensUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            users.pendingCitizens().collect { list ->
                _uiState.update { it.copy(citizens = list, isLoading = false) }
            }
        }
    }
}

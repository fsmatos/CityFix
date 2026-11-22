package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.isec.amov.cityfix.data.UserProfile
import pt.isec.amov.cityfix.data.UsersRepository
import pt.isec.amov.cityfix.model.Citizen

class CitizenApprovalViewModel(
    private val citizenId: String,
    private val operatorId: String,
    private val users: UsersRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CitizenApprovalUiState())
    val uiState: StateFlow<CitizenApprovalUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            users.observeUser(citizenId).collect { profile ->
                _uiState.update { state ->
                    val user = (profile as? UserProfile.Found)?.user
                    when {
                        user is Citizen ->
                            state.copy(citizen = user, isLoading = false, notFound = false)
                        // Offline with nothing cached: keep the last citizen, else say not found.
                        profile == UserProfile.Unavailable ->
                            state.copy(isLoading = false, notFound = state.citizen == null)
                        // Missing, or an operator's document.
                        else -> state.copy(citizen = null, isLoading = false, notFound = true)
                    }
                }
            }
        }
    }

    fun onActivateClick() {
        val state = _uiState.value
        if (state.canActivate && !state.activated) _uiState.update { it.copy(confirming = true) }
    }

    fun onDismissConfirm() {
        _uiState.update { it.copy(confirming = false) }
    }

    fun activate(onActivated: () -> Unit) {
        val state = _uiState.value
        if (!state.confirming || !state.canActivate) return
        _uiState.update { it.copy(confirming = false, activated = true) }
        users.activateCitizen(citizenId, operatorId)
        onActivated()
    }
}

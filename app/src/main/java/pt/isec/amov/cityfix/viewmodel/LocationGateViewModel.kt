package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pt.isec.amov.cityfix.data.LocationRepository
import pt.isec.amov.cityfix.model.isInsideAnonymousArea

enum class GateStatus { CHECKING, DENIED, NO_FIX, OUTSIDE, INSIDE }

data class LocationGateUiState(val status: GateStatus = GateStatus.CHECKING)

class LocationGateViewModel(private val locationRepository: LocationRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationGateUiState())

    val uiState: StateFlow<LocationGateUiState> = _uiState.asStateFlow()

    fun onPermissionResult(granted: Boolean) {
        if (!granted) {
            _uiState.value = LocationGateUiState(GateStatus.DENIED)
        } else if (_uiState.value.status != GateStatus.INSIDE) {
            check()
        }
    }

    private fun check() {
        _uiState.value = LocationGateUiState(GateStatus.CHECKING)
        viewModelScope.launch {
            val here = locationRepository.currentLocation()
            val status = when {
                here == null -> GateStatus.NO_FIX
                isInsideAnonymousArea(here) -> GateStatus.INSIDE
                else -> GateStatus.OUTSIDE
            }
            _uiState.value = LocationGateUiState(status)
        }
    }
}

class LocationGateViewModelFactory(
    private val locationRepository: LocationRepository,
) : ViewModelProvider.Factory {
    // Only ever asked for LocationGateViewModel (viewModel(factory = ...) in MainActivity).
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        LocationGateViewModel(locationRepository) as T
}

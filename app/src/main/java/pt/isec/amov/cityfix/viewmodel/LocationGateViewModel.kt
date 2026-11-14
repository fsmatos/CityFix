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

/** Where the anonymous start-up check stands (brief I.1: only within 10 km of Pr. Republica). */
enum class GateStatus { CHECKING, DENIED, NO_FIX, OUTSIDE, INSIDE }

data class LocationGateUiState(val status: GateStatus = GateStatus.CHECKING)

/**
 * The 10 km gate for anonymous users. MainActivity requests the location permission at start (as
 * in class) and passes the answer to [onPermissionResult]; a granted permission reads one fix and
 * applies [isInsideAnonymousArea]. The fix itself is not kept: the gate only needs the answer.
 */
class LocationGateViewModel(private val locationRepository: LocationRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationGateUiState())

    val uiState: StateFlow<LocationGateUiState> = _uiState.asStateFlow()

    /**
     * A rotation asks for the permission again and Android answers at once; when the user is
     * already inside, the check is not repeated, so the app does not flash the gate.
     */
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

/** Builds [LocationGateViewModel] with the app's LocationRepository, like the other factories. */
class LocationGateViewModelFactory(
    private val locationRepository: LocationRepository,
) : ViewModelProvider.Factory {
    // Only ever asked for LocationGateViewModel (viewModel(factory = ...) in MainActivity).
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        LocationGateViewModel(locationRepository) as T
}

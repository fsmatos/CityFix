package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Impact suggestion state for the UI (brief I.2.F). Since F-022 the sensor lives in
 * TrackingService, so it also works with the app closed; while the app is in front the service
 * emits on [impacts] (CityFixApplication.impacts) and this turns it into [impactDetected], which
 * goes back to false when the snackbar is shown ([onSuggestionShown]).
 */
class ImpactViewModel(impacts: SharedFlow<Unit>) : ViewModel() {

    private val _impactDetected = MutableStateFlow(false)

    val impactDetected: StateFlow<Boolean> = _impactDetected.asStateFlow()

    init {
        viewModelScope.launch { impacts.collect { _impactDetected.value = true } }
    }

    fun onSuggestionShown() {
        _impactDetected.value = false
    }
}

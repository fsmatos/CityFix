package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

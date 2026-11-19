package pt.isec.amov.cityfix.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pt.isec.amov.cityfix.utils.ImpactDetector

/**
 * Owns the [ImpactDetector] (brief I.2.F), so composables never touch the sensor. MainActivity
 * calls [start]/[stop] from ImpactDetectionEffect; [impactDetected] becomes true on an impact and
 * goes back to false when the suggestion is shown ([onSuggestionShown]).
 */
class ImpactViewModel(context: Context) : ViewModel() {

    // applicationContext: the ViewModel outlives the Activity, so it must not keep it.
    private val detector = ImpactDetector(context.applicationContext)

    private val _impactDetected = MutableStateFlow(false)

    val impactDetected: StateFlow<Boolean> = _impactDetected.asStateFlow()

    fun start() {
        detector.start { _impactDetected.value = true }
    }

    fun stop() {
        detector.stop()
    }

    fun onSuggestionShown() {
        _impactDetected.value = false
    }

    override fun onCleared() {
        detector.stop()
    }
}

package pt.isec.amov.cityfix.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pt.isec.amov.cityfix.data.ImpactPrefs
import pt.isec.amov.cityfix.data.LocationRepository
import pt.isec.amov.cityfix.utils.ImpactDetector

/**
 * Owns the [ImpactDetector] (brief I.2.F), so composables never touch the sensor. MainActivity
 * calls [start]/[stop] from ImpactDetectionEffect; [impactDetected] becomes true on an impact and
 * goes back to false when the suggestion is shown ([onSuggestionShown]). While started it also
 * collects GPS fixes, so the detector only counts a jolt while the phone moves (F-022).
 */
class ImpactViewModel(
    context: Context,
    private val locationRepository: LocationRepository,
    impactPrefs: ImpactPrefs,
) : ViewModel() {

    /** Speed (m/s) and time of the last GPS fix; a fix older than [FIX_MAX_AGE_MS] is ignored. */
    private var lastSpeed = 0f
    private var lastFixAt = 0L
    private var fixesJob: Job? = null

    // applicationContext: the ViewModel outlives the Activity, so it must not keep it.
    private val detector = ImpactDetector(
        context.applicationContext,
        threshold = impactPrefs::threshold,
        speed = { if (System.currentTimeMillis() - lastFixAt < FIX_MAX_AGE_MS) lastSpeed else null },
    )

    private val _impactDetected = MutableStateFlow(false)

    val impactDetected: StateFlow<Boolean> = _impactDetected.asStateFlow()

    fun start() {
        fixesJob?.cancel()
        fixesJob = viewModelScope.launch {
            locationRepository.locationFixes().collect {
                lastSpeed = if (it.hasSpeed()) it.speed else 0f
                lastFixAt = System.currentTimeMillis()
            }
        }
        detector.start { _impactDetected.value = true }
    }

    fun stop() {
        detector.stop()
        fixesJob?.cancel()
        fixesJob = null
    }

    fun onSuggestionShown() {
        _impactDetected.value = false
    }

    override fun onCleared() {
        detector.stop()
    }

    private companion object {
        /** A GPS fix older than this (ms) says nothing about the current speed (F-022 Q2). */
        const val FIX_MAX_AGE_MS = 5_000L
    }
}

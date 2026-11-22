package pt.isec.amov.cityfix.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import pt.isec.amov.cityfix.data.LocationRepository

/** Builds [ImpactViewModel] with a Context for the SensorManager and the GPS fixes (F-022). */
class ImpactViewModelFactory(
    private val context: Context,
    private val locationRepository: LocationRepository,
) : ViewModelProvider.Factory {
    // Only ever asked for ImpactViewModel (viewModel(factory = ...) in MainActivity), so the cast holds.
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ImpactViewModel(context, locationRepository) as T
}

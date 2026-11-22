package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.SharedFlow

/** Builds [ImpactViewModel] with the impacts the TrackingService emits (F-022). */
class ImpactViewModelFactory(
    private val impacts: SharedFlow<Unit>,
) : ViewModelProvider.Factory {
    // Only ever asked for ImpactViewModel (viewModel(factory = ...) in MainActivity), so the cast holds.
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ImpactViewModel(impacts) as T
}

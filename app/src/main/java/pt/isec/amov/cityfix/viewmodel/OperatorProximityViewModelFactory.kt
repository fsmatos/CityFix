package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import pt.isec.amov.cityfix.data.LocationRepository
import pt.isec.amov.cityfix.data.TicketsRepository

/** Builds [OperatorProximityViewModel] with the app's LocationRepository and TicketsRepository. */
class OperatorProximityViewModelFactory(
    private val locationRepository: LocationRepository,
    private val ticketsRepository: TicketsRepository,
) : ViewModelProvider.Factory {
    // Only ever asked for OperatorProximityViewModel (viewModel(factory = ...) in MainActivity).
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        OperatorProximityViewModel(locationRepository, ticketsRepository) as T
}

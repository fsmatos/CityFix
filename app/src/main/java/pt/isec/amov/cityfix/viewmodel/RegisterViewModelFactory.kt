package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import pt.isec.amov.cityfix.data.LocationRepository
import pt.isec.amov.cityfix.data.UsersRepository

/**
 * Builds [RegisterViewModel]. Uses the CreationExtras overload because the ViewModel needs a
 * SavedStateHandle, which the back stack entry supplies.
 */
class RegisterViewModelFactory(
    private val users: UsersRepository,
    private val locationRepository: LocationRepository,
) : ViewModelProvider.Factory {
    // Only ever asked for RegisterViewModel (viewModel(factory = ...) in MainActivity), so the cast holds.
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
        RegisterViewModel(users, locationRepository, extras.createSavedStateHandle()) as T
}

package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import pt.isec.amov.cityfix.data.AuthRepository

/**
 * Builds [AccountSetupViewModel]. Uses the CreationExtras overload because the ViewModel needs a
 * SavedStateHandle, which the back stack entry supplies.
 */
class AccountSetupViewModelFactory(
    private val auth: AuthRepository,
) : ViewModelProvider.Factory {
    // Only ever asked for AccountSetupViewModel (viewModel(factory = ...) in MainActivity), so the cast holds.
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
        AccountSetupViewModel(auth, extras.createSavedStateHandle()) as T
}

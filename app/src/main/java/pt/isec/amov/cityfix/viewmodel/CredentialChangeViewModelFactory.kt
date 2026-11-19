package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import pt.isec.amov.cityfix.data.AuthRepository
import pt.isec.amov.cityfix.data.UsersRepository

/**
 * Builds [CredentialChangeViewModel] for one [change] (one per route). Uses the CreationExtras
 * overload because the ViewModel needs a SavedStateHandle, which the back stack entry supplies.
 */
class CredentialChangeViewModelFactory(
    private val change: CredentialChange,
    private val isOperator: Boolean,
    private val auth: AuthRepository,
    private val users: UsersRepository,
) : ViewModelProvider.Factory {
    // Only ever asked for CredentialChangeViewModel (viewModel(factory = ...) in MainActivity), so the cast holds.
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
        CredentialChangeViewModel(change, isOperator, auth, users, extras.createSavedStateHandle()) as T
}

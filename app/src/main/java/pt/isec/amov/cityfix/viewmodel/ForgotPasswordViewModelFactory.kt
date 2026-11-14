package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import pt.isec.amov.cityfix.data.AuthRepository

/** Builds [ForgotPasswordViewModel] with the e-mail from the route and a SavedStateHandle. */
class ForgotPasswordViewModelFactory(
    private val auth: AuthRepository,
    private val initialEmail: String,
) : ViewModelProvider.Factory {
    // Only ever asked for ForgotPasswordViewModel (viewModel(factory = ...) in MainActivity), so the cast holds.
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
        ForgotPasswordViewModel(auth, extras.createSavedStateHandle(), initialEmail) as T
}

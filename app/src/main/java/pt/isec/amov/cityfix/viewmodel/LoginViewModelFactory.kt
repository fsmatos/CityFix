package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import pt.isec.amov.cityfix.data.AuthRepository

class LoginViewModelFactory(
    private val auth: AuthRepository,
) : ViewModelProvider.Factory {
    // Only ever asked for LoginViewModel (viewModel(factory = ...) in MainActivity), so the cast holds.
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
        LoginViewModel(auth, extras.createSavedStateHandle()) as T
}

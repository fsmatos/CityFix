package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import pt.isec.amov.cityfix.data.AuthRepository
import pt.isec.amov.cityfix.data.UsersRepository

class SessionViewModelFactory(
    private val auth: AuthRepository,
    private val users: UsersRepository,
) : ViewModelProvider.Factory {
    // Only ever asked for SessionViewModel (viewModel(factory = ...) in MainActivity), so the cast holds.
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        SessionViewModel(auth, users) as T
}

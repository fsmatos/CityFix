package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import pt.isec.amov.cityfix.data.UsersRepository

class PendingCitizensViewModelFactory(
    private val users: UsersRepository,
) : ViewModelProvider.Factory {
    // Only ever asked for PendingCitizensViewModel (viewModel(factory = ...) in MainActivity).
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        PendingCitizensViewModel(users) as T
}

package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import pt.isec.amov.cityfix.data.TicketsRepository

/** Builds [TicketsViewModel] with the app's repository, like ContactsViewModelFactory in Contacts_#3. */
class TicketsViewModelFactory(
    private val repository: TicketsRepository,
) : ViewModelProvider.Factory {
    // Only ever asked for TicketsViewModel (viewModel(factory = ...) in MainActivity), so the cast holds.
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        TicketsViewModel(repository) as T
}

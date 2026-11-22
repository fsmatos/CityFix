package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import pt.isec.amov.cityfix.data.TicketsRepository
import pt.isec.amov.cityfix.model.User

class TicketDetailViewModelFactory(
    private val repository: TicketsRepository,
    private val ticketId: String,
    private val user: User,
) : ViewModelProvider.Factory {
    // Only ever asked for TicketDetailViewModel (viewModel(factory = ...) in MainActivity), so the cast holds.
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        TicketDetailViewModel(repository, ticketId, user) as T
}

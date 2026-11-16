package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import pt.isec.amov.cityfix.data.TicketsRepository
import pt.isec.amov.cityfix.data.UsersRepository
import pt.isec.amov.cityfix.model.Operator

/** Builds [OperatorTicketViewModel] with the app's repositories, the route's ticket id and the operator. */
class OperatorTicketViewModelFactory(
    private val tickets: TicketsRepository,
    private val users: UsersRepository,
    private val ticketId: String,
    private val operator: Operator,
) : ViewModelProvider.Factory {
    // Only ever asked for OperatorTicketViewModel (viewModel(factory = ...) in MainActivity).
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        OperatorTicketViewModel(tickets, users, ticketId, operator) as T
}

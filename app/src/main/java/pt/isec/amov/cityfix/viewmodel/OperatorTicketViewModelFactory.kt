package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import pt.isec.amov.cityfix.data.AttachmentStorage
import pt.isec.amov.cityfix.data.TicketsRepository
import pt.isec.amov.cityfix.data.UsersRepository
import pt.isec.amov.cityfix.model.Operator

class OperatorTicketViewModelFactory(
    private val tickets: TicketsRepository,
    private val users: UsersRepository,
    private val attachmentStorage: AttachmentStorage,
    private val ticketId: String,
    private val operator: Operator,
) : ViewModelProvider.Factory {
    // Only ever asked for OperatorTicketViewModel (viewModel(factory = ...) in MainActivity).
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        OperatorTicketViewModel(tickets, users, attachmentStorage, ticketId, operator) as T
}

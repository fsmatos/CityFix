package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.data.TicketsRepository
import pt.isec.amov.cityfix.model.Ticket
import pt.isec.amov.cityfix.model.User

/**
 * The participant detail of ticket [ticketId] (F-014): its message thread, the message being typed
 * and the reopen dialog. Created per destination, like EditContactViewModel in Contacts_#3, and
 * only for a REGISTERED or OPERATOR audience: anonymous and pending users never create it, so they
 * never read the messages (brief I.1.C).
 *
 * [user] is fixed: a role change clears the back stack (D-009), so this ViewModel never outlives
 * its user. The ticket is not stored here; the screen passes the current one from
 * TicketsViewModel's list to [confirmReopen] and [closeIfExpired].
 */
class TicketDetailViewModel(
    private val repository: TicketsRepository,
    private val ticketId: String,
    private val user: User,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TicketDetailUiState())
    val uiState: StateFlow<TicketDetailUiState> = _uiState.asStateFlow()

    init {
        // Live thread; the listener is removed when the destination leaves the back stack.
        viewModelScope.launch {
            repository.messagesFor(ticketId).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    fun onDraftChange(text: String) {
        _uiState.update { it.copy(draft = text) }
    }

    /** Sends the draft; it is cleared once the message is queued (offline too). */
    fun sendMessage() {
        if (repository.sendMessage(ticketId, _uiState.value.draft, user)) {
            _uiState.update { it.copy(draft = "") }
        }
    }

    fun onReopenClick() {
        _uiState.update { it.copy(isReopenDialogOpen = true, justification = "", reopenError = null) }
    }

    fun onJustificationChange(text: String) {
        _uiState.update { it.copy(justification = text, reopenError = null) }
    }

    fun onReopenDismiss() {
        _uiState.update { it.copy(isReopenDialogOpen = false) }
    }

    /**
     * Reopens [ticket] with the typed justification (brief II.4). A blank justification, or a move
     * the lifecycle rejects (for example the 5-day window ended meanwhile), keeps the dialog open
     * with an error.
     */
    fun confirmReopen(ticket: Ticket) {
        val justification = _uiState.value.justification
        if (justification.isBlank()) {
            _uiState.update { it.copy(reopenError = R.string.reopen_error_blank) }
            return
        }
        if (!repository.reopenTicket(ticket, user, justification)) {
            _uiState.update { it.copy(reopenError = R.string.reopen_error_not_allowed) }
            return
        }
        _uiState.update { it.copy(isReopenDialogOpen = false, justification = "") }
    }

    /** Writes [ticket] as CLOSED if its reopen window has ended (F-014 Q6); nothing otherwise. */
    fun closeIfExpired(ticket: Ticket) {
        repository.closeExpiredTicket(ticket)
    }
}

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
import pt.isec.amov.cityfix.data.UserProfile
import pt.isec.amov.cityfix.data.UsersRepository
import pt.isec.amov.cityfix.model.Operator
import pt.isec.amov.cityfix.model.SYSTEM_RESPONSIBLE_ID
import pt.isec.amov.cityfix.model.Ticket

/**
 * The operator's detail of ticket [ticketId] (brief I.3.E, D-013): the message thread, the
 * contacts of the people on the ticket and the lifecycle actions (request information, start work). Created inside the operator branch of the ticket detail
 * destination, so it exists only for an operator and its listeners end with the entry.
 *
 * [operator] is fixed: a role change clears the back stack (D-009). The ticket is not stored
 * here; the screen passes the current one from TicketsViewModel's list.
 */
class OperatorTicketViewModel(
    private val tickets: TicketsRepository,
    private val users: UsersRepository,
    private val ticketId: String,
    private val operator: Operator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OperatorTicketUiState())
    val uiState: StateFlow<OperatorTicketUiState> = _uiState.asStateFlow()

    /** Ids already being listened to, so each profile has one listener. */
    private val watched = mutableSetOf<String>()

    init {
        // Live thread, as TicketDetailViewModel (D-013 Q7).
        viewModelScope.launch {
            tickets.messagesFor(ticketId).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    fun onDraftChange(text: String) {
        _uiState.update { it.copy(draft = text) }
    }

    /** Sends the draft as the municipality (byOperator); cleared once queued (offline too). */
    fun sendMessage() {
        if (tickets.sendMessage(ticketId, _uiState.value.draft, operator)) {
            _uiState.update { it.copy(draft = "") }
        }
    }

    /**
     * Starts one live users/{uid} read per new id in [ids] (creator, followers, history
     * responsibles). Blank ids and the automatic close's "system" are skipped. Called again
     * when the ticket changes, so a new follower appears without reopening.
     */
    fun watchPeople(ids: Set<String>) {
        for (id in ids) {
            if (id.isBlank() || id == SYSTEM_RESPONSIBLE_ID || id in watched) continue
            watched += id
            viewModelScope.launch {
                users.observeUser(id).collect { profile ->
                    _uiState.update { it.copy(people = it.people + (id to profile.toContact())) }
                }
            }
        }
    }

    fun onRequestInfoClick() {
        openDialog(OperatorDialog.REQUEST_INFO)
    }

    fun onStartWorkClick() {
        openDialog(OperatorDialog.START_WORK)
    }

    fun onDialogTextChange(text: String) {
        _uiState.update { it.copy(dialogText = text, dialogError = null) }
    }

    fun onDialogDismiss() {
        _uiState.update { it.copy(openDialog = null) }
    }

    /**
     * Runs the open dialog's action on [ticket], the current one from the list.
     * Request information (brief II.2): the text is required; it becomes the history
     * justification and a message by the operator in one batch (askForClarification, reused).
     * Start work (brief II.3): the note is optional. A move the lifecycle rejects (the status
     * changed meanwhile) keeps the dialog open with an error.
     */
    fun confirmDialog(ticket: Ticket) {
        val state = _uiState.value
        val dialog = state.openDialog ?: return
        val done = when (dialog) {
            OperatorDialog.REQUEST_INFO -> {
                if (state.dialogText.isBlank()) {
                    _uiState.update { it.copy(dialogError = R.string.operator_request_info_error_blank) }
                    return
                }
                tickets.askForClarification(ticket, operator, state.dialogText)
            }
            OperatorDialog.START_WORK -> tickets.startWork(ticket, operator, state.dialogText.ifBlank { null })
        }
        if (!done) {
            _uiState.update { it.copy(dialogError = R.string.operator_action_error_not_allowed) }
            return
        }
        _uiState.update { it.copy(openDialog = null, dialogText = "") }
    }

    /** Writes [ticket] as CLOSED if its reopen window has ended (as F-014); nothing otherwise. */
    fun closeIfExpired(ticket: Ticket) {
        tickets.closeExpiredTicket(ticket)
    }

    /** Opens [dialog] with an empty text and no error. */
    private fun openDialog(dialog: OperatorDialog) {
        _uiState.update { it.copy(openDialog = dialog, dialogText = "", dialogError = null) }
    }

    private fun UserProfile.toContact(): PersonContact? = when (this) {
        is UserProfile.Found -> PersonContact(user.name, user.email, user.phone, user is Operator)
        UserProfile.Missing, UserProfile.Unavailable -> null
    }
}

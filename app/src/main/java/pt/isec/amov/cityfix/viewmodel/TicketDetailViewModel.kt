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
 * and the reopen / clarification dialog. Created per destination, like EditContactViewModel in
 * Contacts_#3, and only for a REGISTERED or OPERATOR audience: anonymous and pending users never
 * create it, so they never read the messages (brief I.1.C).
 *
 * [user] is fixed: a role change clears the back stack (D-009), so this ViewModel never outlives
 * its user. The ticket is not stored here; the screen passes the current one from
 * TicketsViewModel's list to [confirmDialog] and [closeIfExpired].
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
        openDialog(DetailDialog.REOPEN)
    }

    fun onAskClarificationClick() {
        openDialog(DetailDialog.ASK_CLARIFICATION)
    }

    fun onDialogTextChange(text: String) {
        _uiState.update { it.copy(dialogText = text, dialogError = null) }
    }

    fun onDialogDismiss() {
        _uiState.update { it.copy(openDialog = null) }
    }

    /**
     * Runs the open dialog's action on [ticket] with the typed text: reopen (brief II.4) or ask
     * for clarification (brief II.2). A blank text, or a move the lifecycle rejects (for example
     * the 5-day window ended meanwhile), keeps the dialog open with an error.
     */
    fun confirmDialog(ticket: Ticket) {
        val state = _uiState.value
        val dialog = state.openDialog ?: return
        if (state.dialogText.isBlank()) {
            val error = when (dialog) {
                DetailDialog.REOPEN -> R.string.reopen_error_blank
                DetailDialog.ASK_CLARIFICATION -> R.string.clarification_error_blank
            }
            _uiState.update { it.copy(dialogError = error) }
            return
        }
        val done = when (dialog) {
            DetailDialog.REOPEN -> repository.reopenTicket(ticket, user, state.dialogText)
            DetailDialog.ASK_CLARIFICATION -> repository.askForClarification(ticket, user, state.dialogText)
        }
        if (!done) {
            val error = when (dialog) {
                DetailDialog.REOPEN -> R.string.reopen_error_not_allowed
                DetailDialog.ASK_CLARIFICATION -> R.string.clarification_error_not_allowed
            }
            _uiState.update { it.copy(dialogError = error) }
            return
        }
        _uiState.update { it.copy(openDialog = null, dialogText = "") }
    }

    /** Writes [ticket] as CLOSED if its reopen window has ended (F-014 Q6); nothing otherwise. */
    fun closeIfExpired(ticket: Ticket) {
        repository.closeExpiredTicket(ticket)
    }

    /** Opens [dialog] with an empty text and no error. */
    private fun openDialog(dialog: DetailDialog) {
        _uiState.update { it.copy(openDialog = dialog, dialogText = "", dialogError = null) }
    }
}

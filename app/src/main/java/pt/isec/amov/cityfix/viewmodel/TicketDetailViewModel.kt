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

    fun closeIfExpired(ticket: Ticket) {
        repository.closeExpiredTicket(ticket)
    }

    private fun openDialog(dialog: DetailDialog) {
        _uiState.update { it.copy(openDialog = dialog, dialogText = "", dialogError = null) }
    }
}

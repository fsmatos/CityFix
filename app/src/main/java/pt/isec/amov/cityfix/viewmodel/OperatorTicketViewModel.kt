package pt.isec.amov.cityfix.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.data.AttachmentStorage
import pt.isec.amov.cityfix.data.TicketsRepository
import pt.isec.amov.cityfix.data.UserProfile
import pt.isec.amov.cityfix.data.UsersRepository
import pt.isec.amov.cityfix.model.Attachment
import pt.isec.amov.cityfix.model.AttachmentType
import pt.isec.amov.cityfix.model.Operator
import pt.isec.amov.cityfix.model.SYSTEM_RESPONSIBLE_ID
import pt.isec.amov.cityfix.model.Ticket
import pt.isec.amov.cityfix.model.TicketStatus
import pt.isec.amov.cityfix.model.allowedTransitions
import pt.isec.amov.cityfix.utils.FileUtils
import java.io.File

class OperatorTicketViewModel(
    private val tickets: TicketsRepository,
    private val users: UsersRepository,
    private val attachmentStorage: AttachmentStorage,
    private val ticketId: String,
    private val operator: Operator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OperatorTicketUiState())
    val uiState: StateFlow<OperatorTicketUiState> = _uiState.asStateFlow()

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

    fun sendMessage() {
        if (tickets.sendMessage(ticketId, _uiState.value.draft, operator)) {
            _uiState.update { it.copy(draft = "") }
        }
    }

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

    fun onResolveClick() {
        _uiState.update { it.copy(resolving = true, resolveError = null) }
    }

    fun onCancelResolve() {
        val draft = _uiState.value.proof
        _uiState.update { it.copy(resolving = false, proof = emptyList(), resolveNote = "", resolveError = null) }
        deleteFiles(draft)
    }

    fun onResolveNoteChange(text: String) {
        _uiState.update { it.copy(resolveNote = text) }
    }

    fun onMediaPicked(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val accepted = uris.take(MAX_PROOF_ATTACHMENTS - _uiState.value.proof.size)
        if (accepted.isEmpty()) return
        _uiState.update { it.copy(isCopying = true, resolveError = null) }
        viewModelScope.launch {
            try {
                val paths = attachmentStorage.copyToInternalStorage(accepted)
                val fits = paths.take(MAX_PROOF_ATTACHMENTS - _uiState.value.proof.size)
                paths.drop(fits.size).forEach { File(it).delete() }
                val notice = when {
                    paths.size < accepted.size -> R.string.report_attachment_copy_failed
                    fits.size < paths.size || accepted.size < uris.size -> R.string.report_attachments_limit
                    else -> null
                }
                _uiState.update { it.copy(proof = it.proof + fits.map(::proofFor), resolveError = notice) }
            } finally {
                _uiState.update { it.copy(isCopying = false) }
            }
        }
    }

    fun onPhotoTaken(path: String) {
        if (_uiState.value.proof.size >= MAX_PROOF_ATTACHMENTS) {
            File(path).delete()
            _uiState.update { it.copy(resolveError = R.string.report_attachments_limit) }
            return
        }
        _uiState.update { it.copy(proof = it.proof + proofFor(path), resolveError = null) }
    }

    fun onCameraUnavailable() {
        _uiState.update { it.copy(resolveError = R.string.resolve_camera_unavailable) }
    }

    fun onRemoveProof(attachment: Attachment) {
        _uiState.update { it.copy(proof = it.proof - attachment) }
        deleteFiles(listOf(attachment))
    }

    fun resolve(ticket: Ticket) {
        val state = _uiState.value
        if (!state.canConfirmResolve) return
        // Review N6: a move that is no longer allowed gets its own message, not the combined one.
        if (TicketStatus.RESOLVED !in ticket.allowedTransitions(operator, System.currentTimeMillis())) {
            _uiState.update { it.copy(resolveError = R.string.operator_action_error_not_allowed) }
            return
        }
        _uiState.update { it.copy(isResolving = true, resolveError = null) }
        viewModelScope.launch {
            val ok = tickets.resolveTicket(ticket, operator, state.proof, state.resolveNote.ifBlank { null })
            if (ok) {
                _uiState.update {
                    it.copy(resolving = false, proof = emptyList(), resolveNote = "", isResolving = false)
                }
            } else {
                _uiState.update { it.copy(isResolving = false, resolveError = R.string.resolve_error_failed) }
            }
        }
    }

    override fun onCleared() {
        // Kept on the main thread on purpose (review N2): viewModelScope is already cancelled here,
        // so a coroutine launched on it would never run, and the app has no application scope.
        // It is at most MAX_PROOF_ATTACHMENTS local deletes (usually none), as in ReportViewModel.
        _uiState.value.proof.forEach { File(it.file).delete() }
    }

    fun closeIfExpired(ticket: Ticket) {
        tickets.closeExpiredTicket(ticket)
    }

    private fun proofFor(path: String): Attachment = Attachment(
        id = File(path).name,
        type = if (FileUtils.isVideoPath(path)) AttachmentType.VIDEO else AttachmentType.IMAGE,
        file = path,
    )

    private fun deleteFiles(attachments: List<Attachment>) {
        if (attachments.isEmpty()) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { attachments.forEach { File(it.file).delete() } }
        }
    }

    private fun openDialog(dialog: OperatorDialog) {
        _uiState.update { it.copy(openDialog = dialog, dialogText = "", dialogError = null) }
    }

    private fun UserProfile.toContact(): PersonContact? = when (this) {
        is UserProfile.Found -> PersonContact(user.name, user.email, user.phone, user is Operator)
        UserProfile.Missing, UserProfile.Unavailable -> null
    }
}

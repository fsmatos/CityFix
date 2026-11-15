package pt.isec.amov.cityfix.viewmodel

import androidx.annotation.StringRes
import pt.isec.amov.cityfix.model.Message

/**
 * The participant detail's own state (F-014): the thread, the message being typed and the reopen
 * dialog. The ticket itself is not here: it comes from TicketsViewModel's list, the single source
 * of truth for tickets.
 */
data class TicketDetailUiState(
    /** Oldest first. */
    val messages: List<Message> = emptyList(),
    val draft: String = "",
    val isReopenDialogOpen: Boolean = false,
    val justification: String = "",
    @StringRes val reopenError: Int? = null,
)

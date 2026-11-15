package pt.isec.amov.cityfix.viewmodel

import androidx.annotation.StringRes
import pt.isec.amov.cityfix.model.Message

/** The two actions of the participant detail that ask for a text first (F-014). */
enum class DetailDialog {
    /** Brief II.4: reopen a RESOLVED ticket with a justification. */
    REOPEN,

    /** Brief II.2: ask the municipality for clarification; the ticket waits for information. */
    ASK_CLARIFICATION,
}

/**
 * The participant detail's own state (F-014): the thread, the message being typed and the open
 * dialog. The ticket itself is not here: it comes from TicketsViewModel's list, the single source
 * of truth for tickets.
 */
data class TicketDetailUiState(
    /** Oldest first. */
    val messages: List<Message> = emptyList(),
    val draft: String = "",
    /** The dialog on screen, or null. */
    val openDialog: DetailDialog? = null,
    /** The reopen justification or the clarification question typed in the dialog. */
    val dialogText: String = "",
    @StringRes val dialogError: Int? = null,
)

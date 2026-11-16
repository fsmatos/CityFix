package pt.isec.amov.cityfix.viewmodel

import androidx.annotation.StringRes
import pt.isec.amov.cityfix.model.Message

/**
 * What the operator sees of one person (brief I.3.E: the author's contacts). Built only inside
 * the operator's ticket detail; never shown to citizens or anonymous users.
 */
data class PersonContact(
    val name: String,
    val email: String,
    /** 9 digits, without +351. */
    val phone: String,
    val isOperator: Boolean,
)

/** The operator actions that open a dialog first (D-013). */
enum class OperatorDialog {
    /** Brief II.2, I.3.E: ask the citizen for information; the text is also a message. */
    REQUEST_INFO,

    /** Brief II.3: move to In progress, with an optional note (no undo, so it asks first, Q6). */
    START_WORK,
}

/**
 * The operator ticket detail's own state (D-013). The ticket itself is not here: it comes from
 * TicketsViewModel's list, the single source of truth for tickets (F-014 pattern).
 */
data class OperatorTicketUiState(
    /** Oldest first. */
    val messages: List<Message> = emptyList(),
    val draft: String = "",
    /** uid -> contact; absent = not loaded yet; null = not available (offline with no cache, or missing). */
    val people: Map<String, PersonContact?> = emptyMap(),
    /** The dialog on screen, or null. */
    val openDialog: OperatorDialog? = null,
    /** The request text or the start-work note typed in the dialog. */
    val dialogText: String = "",
    @StringRes val dialogError: Int? = null,
)

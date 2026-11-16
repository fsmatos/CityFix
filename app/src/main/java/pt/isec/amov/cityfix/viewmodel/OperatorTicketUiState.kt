package pt.isec.amov.cityfix.viewmodel

import androidx.annotation.StringRes
import pt.isec.amov.cityfix.model.Attachment
import pt.isec.amov.cityfix.model.Message
import pt.isec.amov.cityfix.model.TicketStatus

/** Proof media per resolution (D-013 Q11): 1 to 3 photos or videos. */
const val MAX_PROOF_ATTACHMENTS = 3

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

/**
 * The operator actions that open a dialog first (D-013). [target] is the status the action moves
 * the ticket to: the dialog is shown only while that move is still allowed (review N1).
 */
enum class OperatorDialog(val target: TicketStatus) {
    /** Brief II.2, I.3.E: ask the citizen for information; the text is also a message. */
    REQUEST_INFO(TicketStatus.INFO_REQUESTED),

    /** Brief II.3: move to In progress, with an optional note (no undo, so it asks first, Q6). */
    START_WORK(TicketStatus.IN_PROGRESS),
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
    /** The Resolve section is open. */
    val resolving: Boolean = false,
    /** Local copies of the proof picked or taken, not prepared yet. */
    val proof: List<Attachment> = emptyList(),
    val resolveNote: String = "",
    val isCopying: Boolean = false,
    /** True from Confirm until the batch is queued, so a second tap does nothing. */
    val isResolving: Boolean = false,
    @StringRes val resolveError: Int? = null,
) {
    val canAddProof: Boolean get() = proof.size < MAX_PROOF_ATTACHMENTS && !isCopying && !isResolving

    /** Brief I.3.E: no resolution without proof media. */
    val canConfirmResolve: Boolean get() = proof.isNotEmpty() && !isCopying && !isResolving
}

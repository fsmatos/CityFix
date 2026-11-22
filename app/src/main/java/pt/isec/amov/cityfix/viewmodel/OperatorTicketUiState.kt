package pt.isec.amov.cityfix.viewmodel

import androidx.annotation.StringRes
import pt.isec.amov.cityfix.model.Attachment
import pt.isec.amov.cityfix.model.Message
import pt.isec.amov.cityfix.model.TicketStatus

const val MAX_PROOF_ATTACHMENTS = 3

data class PersonContact(
    val name: String,
    val email: String,
    val phone: String,
    val isOperator: Boolean,
)

enum class OperatorDialog(val target: TicketStatus) {
    REQUEST_INFO(TicketStatus.INFO_REQUESTED),

    START_WORK(TicketStatus.IN_PROGRESS),
}

data class OperatorTicketUiState(
    val messages: List<Message> = emptyList(),
    val draft: String = "",
    val people: Map<String, PersonContact?> = emptyMap(),
    val openDialog: OperatorDialog? = null,
    val dialogText: String = "",
    @StringRes val dialogError: Int? = null,
    val resolving: Boolean = false,
    val proof: List<Attachment> = emptyList(),
    val resolveNote: String = "",
    val isCopying: Boolean = false,
    val isResolving: Boolean = false,
    @StringRes val resolveError: Int? = null,
) {
    val canAddProof: Boolean get() = proof.size < MAX_PROOF_ATTACHMENTS && !isCopying && !isResolving

    val canConfirmResolve: Boolean get() = proof.isNotEmpty() && !isCopying && !isResolving
}

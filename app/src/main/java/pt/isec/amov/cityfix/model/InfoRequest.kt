package pt.isec.amov.cityfix.model

/**
 * The information request a ticket is waiting on (brief II.2, D-018). Derived from the last
 * StatusHistory entry and the messages: nothing extra is stored, so firestore.rules is unchanged.
 */
data class InfoRequest(
    /** The question: the INFO_REQUESTED entry's justification. */
    val question: String,
    /** When it was asked (the entry's dateTime, epoch ms). */
    val askedAt: Long,
    /** True when an operator asked; false when a participant citizen asked for clarification. */
    val byMunicipality: Boolean,
    /** Status before the request (OPEN or IN_PROGRESS): "Resume work" vs "Start work". */
    val statusBefore: TicketStatus,
)

/**
 * The open request while the ticket is INFO_REQUESTED; null otherwise or for a projected ticket
 * (empty history). Who asked: the entry's responsibleId is not a participant = an operator.
 * statusBefore is the previous entry's status, OPEN when there is none.
 */
fun Ticket.pendingInfoRequest(): InfoRequest? {
    if (currentStatus != TicketStatus.INFO_REQUESTED) return null
    val entry = statusHistory.lastOrNull() ?: return null
    if (entry.status != TicketStatus.INFO_REQUESTED) return null
    val before = statusHistory.getOrNull(statusHistory.size - 2)?.status ?: TicketStatus.OPEN
    return InfoRequest(
        question = entry.justification.orEmpty(),
        askedAt = entry.dateTime,
        byMunicipality = !isParticipant(entry.responsibleId),
        statusBefore = before,
    )
}

/**
 * True when the other side has written since the request: a message sent after [InfoRequest.askedAt]
 * by a citizen when the municipality asked, or by an operator when a citizen asked. The question's
 * own message is written in the same batch with sentAt == askedAt, so it never counts. sentAt
 * comes from the sender's clock (D-018 failure modes).
 */
fun InfoRequest.isAnswered(messages: List<Message>): Boolean =
    messages.any { it.sentAt > askedAt && it.byOperator != byMunicipality }

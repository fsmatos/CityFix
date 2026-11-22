package pt.isec.amov.cityfix.model

data class InfoRequest(
    val question: String,
    val askedAt: Long,
    val byMunicipality: Boolean,
    val statusBefore: TicketStatus,
)

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

fun InfoRequest.isAnswered(messages: List<Message>): Boolean =
    messages.any { it.sentAt > askedAt && it.byOperator != byMunicipality }

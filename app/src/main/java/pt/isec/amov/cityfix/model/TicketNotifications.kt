package pt.isec.amov.cityfix.model

/*
 * Which ticket changes notify whom (brief III, II.2-II.4; F-016). Pure rule: the times are
 * parameters. There is no server (decision 0012), so the app compares the tickets it already
 * reads with the time of its last check ([since]) and notifies what happened in between.
 */

enum class TicketEventKind { STATUS, MESSAGE, OPERATOR_NEARBY, CLOSING_SOON, CLOSED }

data class TicketEvent(
    val ticketId: String,
    val categoryName: String,
    val kind: TicketEventKind,
    val status: TicketStatus? = null,
    val justification: String? = null,
)

fun Ticket.eventsFor(userId: String, isOperator: Boolean, since: Long, now: Long): List<TicketEvent> {
    if (statusHistory.isEmpty()) return emptyList()
    if (!isOperator && !isParticipant(userId)) return emptyList()

    val events = mutableListOf<TicketEvent>()

    for (entry in statusHistory.drop(1)) {
        val isNew = entry.dateTime > since && entry.dateTime <= now
        val isOther = entry.responsibleId != userId
        val countsForOperator = !isOperator || isParticipant(entry.responsibleId)
        if (isNew && isOther && countsForOperator && entry.status != TicketStatus.CLOSED) {
            events += TicketEvent(id, categoryName, TicketEventKind.STATUS, entry.status, entry.justification)
        }
    }

    val messageAt = lastMessageAt
    val author = lastMessageAuthorId
    if (messageAt != null && author != null && messageAt > since && author != userId &&
        (!isOperator || isParticipant(author)) &&
        statusHistory.none { it.dateTime == messageAt && it.justification != null }
    ) {
        events += TicketEvent(id, categoryName, TicketEventKind.MESSAGE)
    }

    if (!isOperator) {
        val nearbyAt = lastOperatorNearbyAt
        if (nearbyAt != null && nearbyAt > since && now - nearbyAt < PROXIMITY_ALERT_INTERVAL_MS) {
            events += TicketEvent(id, categoryName, TicketEventKind.OPERATOR_NEARBY)
        }
        val warningAt = closingWarningAt()
        if (warningAt != null && warningAt > since && warningAt <= now && !isLocked(now)) {
            events += TicketEvent(id, categoryName, TicketEventKind.CLOSING_SOON)
        }
        val resolved = resolvedAt()
        if (resolved != null) {
            val closedAt = resolved + REOPEN_WINDOW_MS
            if (closedAt > since && closedAt <= now) {
                events += TicketEvent(id, categoryName, TicketEventKind.CLOSED)
            }
        }
    }
    return events
}

package pt.isec.amov.cityfix.model

/*
 * Which ticket changes notify whom (brief III, II.2-II.4; F-016). Pure rule: the times are
 * parameters. There is no server (decision 0012), so the app compares the tickets it already
 * reads with the time of its last check ([since]) and notifies what happened in between.
 */

/** What happened to a ticket. */
enum class TicketEventKind { STATUS, MESSAGE, OPERATOR_NEARBY, CLOSING_SOON, CLOSED }

/** One thing to notify. [status] and [justification] only for STATUS. */
data class TicketEvent(
    val ticketId: String,
    val categoryName: String,
    val kind: TicketEventKind,
    val status: TicketStatus? = null,
    val justification: String? = null,
)

/**
 * The events of this ticket after [since] and up to [now] for user [userId]; never their own
 * actions. Empty for a projected ticket (decision 0006) and for a citizen who is not a
 * participant.
 *
 * - STATUS: each history entry after the first, dated in (since, now], made by someone else,
 *   except CLOSED (the CLOSED event below covers it).
 * - MESSAGE: a newest message after [since] by someone else; dropped when a STATUS event of this
 *   ticket has a justification, because that text is the message (Gate 1 Q10).
 * - Operator (Gate 1 Q2): STATUS and MESSAGE only when a citizen participant did it, so one
 *   operator's action does not notify the other operators.
 * - Citizen only: OPERATOR_NEARBY when an operator came within 20 m after [since] and less than
 *   an hour ago (decision 0022); CLOSING_SOON when the 24 h warning time is in (since, now] and
 *   the ticket is not closed yet; CLOSED when the end of the reopen window is in (since, now].
 */
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
        events.none { it.justification != null }
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

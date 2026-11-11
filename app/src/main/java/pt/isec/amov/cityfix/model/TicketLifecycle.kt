package pt.isec.amov.cityfix.model

/*
 * Ticket lifecycle rules (brief II, I.3.E; decisions 0004 and 0006).
 *
 * Pure functions: time is always a parameter (epoch ms, UTC), never read here. Status and
 * history change together (decision 0004). Never call these on a projected ticket
 * (toAnonymousView, decision 0006): it has an empty history, so they return null, false or
 * the stored status.
 */

/** Brief II: a resolved ticket can be reopened for 5 days, then it is closed. Offset from the RESOLVED time. */
const val REOPEN_WINDOW_MS: Long = 5L * 24 * 60 * 60 * 1000

/** Brief III: warn 24 h before closing, so 4 days after the RESOLVED time. */
const val CLOSING_WARNING_MS: Long = REOPEN_WINDOW_MS - 24L * 60 * 60 * 1000

/**
 * Time of the last RESOLVED entry while the ticket is RESOLVED or CLOSED (decision 0004);
 * null otherwise (a reopened ticket is not resolved) or with no such entry.
 */
fun Ticket.resolvedAt(): Long? {
    if (currentStatus != TicketStatus.RESOLVED && currentStatus != TicketStatus.CLOSED) return null
    return statusHistory.lastOrNull { it.status == TicketStatus.RESOLVED }?.dateTime
}

/**
 * Brief II: true while RESOLVED and less than [REOPEN_WINDOW_MS] has passed since [resolvedAt].
 * A negative gap (device clock behind) counts as inside the window.
 */
fun Ticket.canReopen(now: Long): Boolean {
    if (currentStatus != TicketStatus.RESOLVED) return false
    val resolved = resolvedAt() ?: return false
    return now - resolved < REOPEN_WINDOW_MS
}

/**
 * Brief II: CLOSED for a RESOLVED ticket whose reopen window has ended (>= [REOPEN_WINDOW_MS]);
 * [Ticket.currentStatus] otherwise. Uses the same difference as [canReopen], so a ticket is never
 * both reopenable and closed.
 */
fun Ticket.effectiveStatus(now: Long): TicketStatus {
    if (currentStatus != TicketStatus.RESOLVED) return currentStatus
    val resolved = resolvedAt() ?: return currentStatus
    return if (now - resolved >= REOPEN_WINDOW_MS) TicketStatus.CLOSED else currentStatus
}

/** Brief II: a closed ticket is locked: no messages, no reopen, no operator action. */
fun Ticket.isLocked(now: Long): Boolean = effectiveStatus(now) == TicketStatus.CLOSED

/** Brief III: time of the 24 h closing warning ([resolvedAt] + [CLOSING_WARNING_MS]) while RESOLVED; null otherwise. */
fun Ticket.closingWarningAt(): Long? {
    if (currentStatus != TicketStatus.RESOLVED) return null
    val resolved = resolvedAt() ?: return null
    return resolved + CLOSING_WARNING_MS
}

/** Decision 0004: has a history whose last entry matches [Ticket.currentStatus]; false for a projected ticket. */
private fun Ticket.isFullTicket(): Boolean =
    statusHistory.isNotEmpty() && statusHistory.last().status == currentStatus

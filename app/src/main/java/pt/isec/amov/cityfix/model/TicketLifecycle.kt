package pt.isec.amov.cityfix.model

/*
 * Ticket lifecycle rules (brief II, I.3.E; decisions 0004 and 0006).
 *
 * Pure functions: time is always a parameter (epoch ms, UTC), never read here. Status and
 * history change together (decision 0004). Never call these on a projected ticket
 * (toAnonymousView, decision 0006): it has an empty history, so they return null, false or
 * the stored status.
 */

/** responsibleId of the automatic CLOSED entry written by [closeIfExpired]: no person made that transition. */
const val SYSTEM_RESPONSIBLE_ID = "system"

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
 * Time only: it does not check who reopens. To show a Reopen button, use
 * `TicketStatus.OPEN in allowedTransitions(user, now)` (review S1).
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

/**
 * Brief III: time of the 24 h closing warning ([resolvedAt] + [CLOSING_WARNING_MS]) while RESOLVED; null otherwise.
 * It can be in the past for an expired ticket not yet closed: check `!isLocked(now)` before warning (review S2).
 */
fun Ticket.closingWarningAt(): Long? {
    if (currentStatus != TicketStatus.RESOLVED) return null
    val resolved = resolvedAt() ?: return null
    return resolved + CLOSING_WARNING_MS
}

/**
 * Brief II, I.3.E: statuses [by] may move this ticket to at [now]. Works on [effectiveStatus], so
 * an expired RESOLVED ticket offers nothing.
 *
 * Operator: the moves in [operatorTargets]. Citizen: OPEN (reopen) only, when ACTIVE, creator or
 * follower, and [canReopen]. Nobody moves a ticket to CLOSED by hand (see [closeIfExpired]).
 * Empty for a projected ticket, a locked one, or a blank user id.
 */
fun Ticket.allowedTransitions(by: User, now: Long): Set<TicketStatus> {
    if (!isFullTicket() || by.id.isBlank()) return emptySet()
    val status = effectiveStatus(now)
    return when (by) {
        is Operator -> operatorTargets(status)
        is Citizen -> {
            val isActiveCitizen = by.registrationStatus == RegistrationStatus.ACTIVE
            if (isActiveCitizen && isParticipant(by.id) && canReopen(now)) {
                setOf(TicketStatus.OPEN)
            } else {
                emptySet()
            }
        }
    }
}

/**
 * The ticket after one transition (brief II, I.3.E; decision 0004), or null when the move is rejected.
 *
 * Rejected when [to] is not in [allowedTransitions]; when the trimmed [justification] is blank for
 * INFO_REQUESTED or a reopen (RESOLVED -> OPEN); when [to] is RESOLVED and [proof] is empty; when
 * [proof] is given for any other status; or when a proof id is blank, repeated, or already on the ticket.
 *
 * Accepted: appends one StatusHistory (to, now, trimmed justification or null, by.id) and sets
 * currentStatus in the same copy. For RESOLVED, the proof is appended with phase RESOLUTION and
 * uploadedById = by.id.
 */
fun Ticket.transition(
    to: TicketStatus,
    by: User,
    now: Long,
    justification: String? = null,
    proof: List<Attachment> = emptyList(),
): Ticket? {
    if (to !in allowedTransitions(by, now)) return null

    val trimmed = justification?.trim()
    val reason = if (trimmed.isNullOrEmpty()) null else trimmed
    val isReopen = to == TicketStatus.OPEN && currentStatus == TicketStatus.RESOLVED
    if ((to == TicketStatus.INFO_REQUESTED || isReopen) && reason == null) return null

    if (to == TicketStatus.RESOLVED && proof.isEmpty()) return null
    if (to != TicketStatus.RESOLVED && proof.isNotEmpty()) return null
    val proofIds = proof.map { it.id }
    if (proofIds.any { it.isBlank() }) return null
    if (proofIds.distinct().size != proofIds.size) return null
    if (attachments.any { it.id in proofIds }) return null

    val newProof = proof.map { it.copy(phase = AttachmentPhase.RESOLUTION, uploadedById = by.id) }
    return copy(
        currentStatus = to,
        statusHistory = statusHistory + StatusHistory(to, now, reason, by.id),
        attachments = attachments + newProof,
    )
}

/**
 * Brief II: a RESOLVED ticket whose reopen window has ended, as a CLOSED copy with one CLOSED entry
 * at [resolvedAt] + [REOPEN_WINDOW_MS] (when it closed, not when someone noticed) by
 * [SYSTEM_RESPONSIBLE_ID], no justification. Null otherwise. Callers may write the result; reading
 * [effectiveStatus] works without it.
 */
fun Ticket.closeIfExpired(now: Long): Ticket? {
    if (!isFullTicket() || currentStatus != TicketStatus.RESOLVED) return null
    val resolved = resolvedAt() ?: return null
    if (now - resolved < REOPEN_WINDOW_MS) return null
    val closedAt = resolved + REOPEN_WINDOW_MS
    return copy(
        currentStatus = TicketStatus.CLOSED,
        statusHistory = statusHistory + StatusHistory(TicketStatus.CLOSED, closedAt, null, SYSTEM_RESPONSIBLE_ID),
    )
}

/** Decision 0004: has a history whose last entry matches [Ticket.currentStatus]; false for a projected ticket. */
private fun Ticket.isFullTicket(): Boolean =
    statusHistory.isNotEmpty() && statusHistory.last().status == currentStatus

/** Option A: the creator or one of the followers (decision 0002). */
private fun Ticket.isParticipant(userId: String): Boolean =
    userId == creatorId || userId in followerIds

/**
 * Operator moves from an effective status (brief II). No else branch: a new status must not
 * compile until someone decides its moves. Operators do not reopen or close.
 */
private fun operatorTargets(status: TicketStatus): Set<TicketStatus> = when (status) {
    TicketStatus.OPEN -> setOf(TicketStatus.INFO_REQUESTED, TicketStatus.IN_PROGRESS)
    TicketStatus.INFO_REQUESTED -> setOf(TicketStatus.IN_PROGRESS)
    TicketStatus.IN_PROGRESS -> setOf(TicketStatus.INFO_REQUESTED, TicketStatus.RESOLVED)
    TicketStatus.RESOLVED -> emptySet()
    TicketStatus.CLOSED -> emptySet()
}

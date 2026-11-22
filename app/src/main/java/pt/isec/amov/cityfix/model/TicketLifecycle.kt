package pt.isec.amov.cityfix.model

/*
 * Ticket lifecycle rules (brief II, I.3.E; decisions 0004 and 0006).
 *
 * Pure functions: time is always a parameter (epoch ms, UTC), never read here. Status and
 * history change together (decision 0004). Never call these on a projected ticket
 * (toAnonymousView, decision 0006): it has an empty history, so they return null, false or
 * the stored status.
 */

const val SYSTEM_RESPONSIBLE_ID = "system"

const val REOPEN_WINDOW_MS: Long = 5L * 24 * 60 * 60 * 1000

const val CLOSING_WARNING_MS: Long = REOPEN_WINDOW_MS - 24L * 60 * 60 * 1000

fun Ticket.resolvedAt(): Long? {
    if (currentStatus != TicketStatus.RESOLVED && currentStatus != TicketStatus.CLOSED) return null
    return statusHistory.lastOrNull { it.status == TicketStatus.RESOLVED }?.dateTime
}

fun Ticket.canReopen(now: Long): Boolean {
    if (currentStatus != TicketStatus.RESOLVED) return false
    val resolved = resolvedAt() ?: return false
    return now - resolved < REOPEN_WINDOW_MS
}

fun Ticket.effectiveStatus(now: Long): TicketStatus {
    if (currentStatus != TicketStatus.RESOLVED) return currentStatus
    val resolved = resolvedAt() ?: return currentStatus
    return if (now - resolved >= REOPEN_WINDOW_MS) TicketStatus.CLOSED else currentStatus
}

fun Ticket.isLocked(now: Long): Boolean = effectiveStatus(now) == TicketStatus.CLOSED

fun Ticket.needsOperatorGeofence(now: Long): Boolean =
    effectiveStatus(now) == TicketStatus.IN_PROGRESS && location != Coordinates()

fun Ticket.closingWarningAt(): Long? {
    if (currentStatus != TicketStatus.RESOLVED) return null
    val resolved = resolvedAt() ?: return null
    return resolved + CLOSING_WARNING_MS
}

fun Ticket.allowedTransitions(by: User, now: Long): Set<TicketStatus> {
    if (!isFullTicket() || by.id.isBlank()) return emptySet()
    val status = effectiveStatus(now)
    return when (by) {
        is Operator -> operatorTargets(status)
        is Citizen -> {
            val isActiveCitizen = by.registrationStatus == RegistrationStatus.ACTIVE
            when {
                !isActiveCitizen || !isParticipant(by.id) -> emptySet()
                canReopen(now) -> setOf(TicketStatus.OPEN)
                status == TicketStatus.OPEN || status == TicketStatus.IN_PROGRESS ->
                    setOf(TicketStatus.INFO_REQUESTED)
                else -> emptySet()
            }
        }
    }
}

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

private fun Ticket.isFullTicket(): Boolean =
    statusHistory.isNotEmpty() && statusHistory.last().status == currentStatus

fun Ticket.isParticipant(userId: String): Boolean =
    userId == creatorId || userId in followerIds

private fun operatorTargets(status: TicketStatus): Set<TicketStatus> = when (status) {
    TicketStatus.OPEN -> setOf(TicketStatus.INFO_REQUESTED, TicketStatus.IN_PROGRESS)
    TicketStatus.INFO_REQUESTED -> setOf(TicketStatus.IN_PROGRESS)
    TicketStatus.IN_PROGRESS -> setOf(TicketStatus.INFO_REQUESTED, TicketStatus.RESOLVED)
    TicketStatus.RESOLVED -> emptySet()
    TicketStatus.CLOSED -> emptySet()
}

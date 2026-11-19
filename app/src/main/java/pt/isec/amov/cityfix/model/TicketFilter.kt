package pt.isec.amov.cityfix.model

/**
 * The operator panel's filter and order (brief I.3.E: the operator sees every report).
 * null means "all" for [status] and [categoryId].
 */
data class TicketFilter(
    val status: TicketStatus? = null,
    val categoryId: String? = null,
    val newestFirst: Boolean = true,
)

/**
 * The tickets that match [filter] at [now], ordered by createdAt (newest or oldest first), with
 * the id as tie-break so the order is stable.
 *
 * The status is compared with effectiveStatus(now), not the stored one (decision 0011): a RESOLVED
 * ticket whose 5-day window has ended is listed under CLOSED even before its CLOSED entry is written.
 * The category is compared by id, so a renamed category still finds its old tickets.
 *
 * Meant for full tickets (operator view). On a projected ticket (no history) effectiveStatus is
 * just the stored status: harmless, but meaningless.
 */
fun List<Ticket>.filteredFor(filter: TicketFilter, now: Long): List<Ticket> {
    val matching = filter { ticket ->
        (filter.status == null || ticket.effectiveStatus(now) == filter.status) &&
            (filter.categoryId == null || ticket.categoryId == filter.categoryId)
    }
    val order = if (filter.newestFirst) {
        compareByDescending<Ticket> { it.createdAt }.thenBy { it.id }
    } else {
        compareBy<Ticket> { it.createdAt }.thenBy { it.id }
    }
    return matching.sortedWith(order)
}

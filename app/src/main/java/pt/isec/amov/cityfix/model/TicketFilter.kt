package pt.isec.amov.cityfix.model

data class TicketFilter(
    val status: TicketStatus? = null,
    val categoryId: String? = null,
    val newestFirst: Boolean = true,
)

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

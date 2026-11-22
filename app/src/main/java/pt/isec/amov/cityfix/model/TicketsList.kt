package pt.isec.amov.cityfix.model

import pt.isec.amov.cityfix.utils.haversineMeters

private fun Coordinates.isUnset() = latitude == 0.0 && longitude == 0.0

val Ticket.isActive: Boolean
    get() = currentStatus == TicketStatus.OPEN ||
        currentStatus == TicketStatus.INFO_REQUESTED ||
        currentStatus == TicketStatus.IN_PROGRESS

class TicketsList(initial: List<Ticket> = emptyList()) {
    private val tickets = mutableListOf<Ticket>()

    init {
        initial.forEach { add(it) }
    }

    val rawList: List<Ticket>
        get() = tickets.toList()

    fun add(ticket: Ticket): Boolean {
        if (ticket.id.isBlank() || findById(ticket.id) != null) return false
        tickets.add(ticket)
        return true
    }

    fun findById(id: String): Ticket? = tickets.firstOrNull { it.id == id }

    fun getAllSorted(): List<Ticket> = tickets.sortedWith(newestFirst)

    fun publicActive(): List<Ticket> =
        tickets.filter { it.scope == Scope.PUBLIC && it.isActive }.sortedWith(newestFirst)

    fun findNearby(location: Coordinates, categoryId: String, situation: Situation): Ticket? {
        if (location.isUnset() || categoryId.isBlank()) return null

        val candidates = tickets.filter { ticket ->
            ticket.isActive &&
                ticket.categoryId == categoryId &&
                ticket.situation == situation &&
                !ticket.location.isUnset()
        }

        val withDistance = candidates.map { ticket ->
            val meters = haversineMeters(
                location.latitude,
                location.longitude,
                ticket.location.latitude,
                ticket.location.longitude,
            )
            ticket to meters
        }

        return withDistance
            .filter { (_, meters) -> meters <= GROUPING_RADIUS_METERS }
            .minByOrNull { (_, meters) -> meters }
            ?.first
    }

    fun stats(): DashboardStats {
        val total = tickets.size
        val active = tickets.count { it.isActive }

        val durations = tickets.mapNotNull { ticket ->
            if (ticket.isActive || ticket.createdAt <= 0) return@mapNotNull null
            val end = ticket.statusHistory
                .lastOrNull { it.status == TicketStatus.RESOLVED }
                ?.dateTime
                ?: return@mapNotNull null
            val duration = end - ticket.createdAt
            if (duration >= 0) duration else null
        }

        return DashboardStats(
            totalTickets = total,
            activeTickets = active,
            resolvedTickets = durations.size,
            averageRepairTimeMillis = if (durations.isEmpty()) null else durations.sum() / durations.size,
        )
    }

    companion object {
        const val GROUPING_RADIUS_METERS = 10.0

        private val newestFirst = compareByDescending<Ticket> { it.createdAt }.thenBy { it.id }
    }
}

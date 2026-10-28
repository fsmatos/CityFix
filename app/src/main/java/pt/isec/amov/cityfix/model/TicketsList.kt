package pt.isec.amov.cityfix.model

import pt.isec.amov.cityfix.utils.haversineMeters

/** True for the default (0, 0), which is not a real location (see [Coordinates]). */
private fun Coordinates.isUnset() = latitude == 0.0 && longitude == 0.0

/**
 * A ticket is active while the problem is not yet repaired: OPEN, INFO_REQUESTED or IN_PROGRESS.
 * RESOLVED and CLOSED are not active (brief I.1.A "% ocorrencias ativas"; brief I.2 "Obs", 20 m
 * geofence on an "ocorrencia ativa"). Shared by publicActive(), findNearby() and stats().
 * Written as an allow-list, so a status added later stays inactive until someone decides otherwise.
 */
val Ticket.isActive: Boolean
    get() = currentStatus == TicketStatus.OPEN ||
        currentStatus == TicketStatus.INFO_REQUESTED ||
        currentStatus == TicketStatus.IN_PROGRESS

/** In-memory tickets for phase 1 (like ContactsList in P07). Returns copies, never the internal list. */
class TicketsList(initial: List<Ticket> = emptyList()) {
    private val tickets = mutableListOf<Ticket>()

    /** Newest first, then by id so the order is stable. */
    private val newestFirst = compareByDescending<Ticket> { it.createdAt }.thenBy { it.id }

    init {
        initial.forEach { add(it) }
    }

    /** All tickets in insertion order (a copy). */
    val rawList: List<Ticket>
        get() = tickets.toList()

    /** Adds [ticket]. Returns false and changes nothing when its id is blank or already present. */
    fun add(ticket: Ticket): Boolean {
        if (ticket.id.isBlank() || findById(ticket.id) != null) return false
        tickets.add(ticket)
        return true
    }

    fun findById(id: String): Ticket? = tickets.firstOrNull { it.id == id }

    /** All tickets, newest first. */
    fun getAllSorted(): List<Ticket> = tickets.sortedWith(newestFirst)

    /** Public active tickets for anonymous users (brief I.1.B), newest first. */
    fun publicActive(): List<Ticket> =
        tickets.filter { it.scope == Scope.PUBLIC && it.isActive }.sortedWith(newestFirst)

    /**
     * Report grouping (brief I.1 "Obs", decision 0002): the nearest active ticket with the same
     * [categoryId] and [situation] within [GROUPING_RADIUS_METERS] of [location], or null.
     * Returns null when [location] is unset (0, 0) or [categoryId] is blank; tickets at (0, 0)
     * never match. Scope is not filtered.
     * May return a ticket the reporter created or already follows; the caller checks
     * `creatorId`/`followerIds`.
     */
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

    companion object {
        /** Brief I.1 "Obs": same category + situation within 10 m joins the existing ticket. */
        const val GROUPING_RADIUS_METERS = 10.0
    }
}

package pt.isec.amov.cityfix.model

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

    companion object {
        /** Brief I.1 "Obs": same category + situation within 10 m joins the existing ticket. */
        const val GROUPING_RADIUS_METERS = 10.0
    }
}

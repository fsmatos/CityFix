package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pt.isec.amov.cityfix.data.SampleTickets
import pt.isec.amov.cityfix.model.DashboardStats
import pt.isec.amov.cityfix.model.Scope
import pt.isec.amov.cityfix.model.Situation
import pt.isec.amov.cityfix.model.StatusHistory
import pt.isec.amov.cityfix.model.Ticket
import pt.isec.amov.cityfix.model.TicketStatus
import pt.isec.amov.cityfix.model.TicketsList
import pt.isec.amov.cityfix.model.toAnonymousView
import java.util.UUID

/**
 * Phase-1 tickets for the UI. Owns an in-memory TicketsList seeded with SampleTickets; composables
 * only see the read-only flows below. Survives rotation as a ViewModel. Create it once above the
 * NavHost (D-006) so every screen shares the same instance.
 * Phase 1 has no login, so everything exposed here is the anonymous view.
 */
class TicketsViewModel : ViewModel() {

    // Declared first: the flows below read it in their initialisers.
    private val ticketsList = TicketsList(SampleTickets.all)

    private val _tickets = MutableStateFlow(visibleTickets())

    /** Public active tickets, newest first, projected with toAnonymousView(). */
    val tickets: StateFlow<List<Ticket>> = _tickets.asStateFlow()

    private val _stats = MutableStateFlow(ticketsList.stats())

    /** Dashboard figures over every ticket, private ones included (decision 0005). */
    val stats: StateFlow<DashboardStats> = _stats.asStateFlow()

    /** The visible (projected) ticket with this id, or null: unknown, private, resolved or closed. */
    fun getTicketById(id: String): Ticket? = _tickets.value.firstOrNull { it.id == id }

    /**
     * Opens a new ticket from the provisional report form (F-006). Returns its id, or null when
     * description, categoryId or categoryName is blank (all three are trimmed). No location yet,
     * so no 10 m grouping (00-plan Q8).
     */
    fun reportTicket(
        description: String,
        categoryId: String,
        categoryName: String,
        situation: Situation,
        scope: Scope,
    ): String? {
        val text = description.trim()
        val cleanCategoryId = categoryId.trim()
        val cleanCategoryName = categoryName.trim()
        if (text.isBlank() || cleanCategoryId.isBlank() || cleanCategoryName.isBlank()) return null

        val now = System.currentTimeMillis()
        val ticket = Ticket(
            id = UUID.randomUUID().toString(),
            description = text,
            situation = situation,
            scope = scope,
            currentStatus = TicketStatus.OPEN,
            createdAt = now,
            categoryId = cleanCategoryId,
            categoryName = cleanCategoryName,
            creatorId = LOCAL_USER_ID,
            statusHistory = listOf(StatusHistory(TicketStatus.OPEN, now, null, LOCAL_USER_ID)),
        )

        if (!ticketsList.add(ticket)) return null
        refresh()
        return ticket.id
    }

    private fun visibleTickets(): List<Ticket> =
        ticketsList.publicActive().map { it.toAnonymousView() }

    private fun refresh() {
        _tickets.value = visibleTickets()
        _stats.value = ticketsList.stats()
    }

    companion object {
        /** Phase 1 has no login; stands in for the reporter's user id. */
        const val LOCAL_USER_ID = "local-citizen"
    }
}

package pt.isec.amov.cityfix.data

import pt.isec.amov.cityfix.model.Attachment
import pt.isec.amov.cityfix.model.Category
import pt.isec.amov.cityfix.model.DashboardStats
import pt.isec.amov.cityfix.model.Scope
import pt.isec.amov.cityfix.model.Situation
import pt.isec.amov.cityfix.model.Ticket

/**
 * Where the tickets and categories come from. Phase 1 has one implementation,
 * [LocalTicketsRepository] (in memory); the Firebase phase adds a second one, so the
 * ViewModels do not change. Phase 1 has no login, so every ticket returned is the
 * anonymous view.
 */
interface TicketsRepository {

    /** Public active tickets, newest first, projected with toAnonymousView(). */
    suspend fun loadPublicTickets(): List<Ticket>

    /** Dashboard figures over every ticket, private ones included (decision 0005). */
    suspend fun loadStats(): DashboardStats

    /** The visible (projected) ticket with this id, or null: unknown, private, resolved or closed. */
    fun getTicketById(id: String): Ticket?

    /**
     * Opens a new ticket from the report form. Returns its id, or null when description,
     * categoryId or categoryName is blank (all three are trimmed), attachments is empty or the
     * ticket could not be added.
     */
    suspend fun reportTicket(
        description: String,
        categoryId: String,
        categoryName: String,
        situation: Situation,
        scope: Scope,
        attachments: List<Attachment>,
    ): String?

    /** Active categories only (I.3.E). Synchronous: in memory, needed for the first frame. */
    fun activeCategories(): List<Category>
}

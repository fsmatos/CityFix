package pt.isec.amov.cityfix.data

import kotlinx.coroutines.flow.Flow
import pt.isec.amov.cityfix.model.Attachment
import pt.isec.amov.cityfix.model.Category
import pt.isec.amov.cityfix.model.Coordinates
import pt.isec.amov.cityfix.model.DashboardStats
import pt.isec.amov.cityfix.model.Scope
import pt.isec.amov.cityfix.model.Situation
import pt.isec.amov.cityfix.model.Ticket

/**
 * Where the tickets and categories come from (decisions 0009, 0010). The ViewModels depend on this
 * interface; the app uses [FirestoreTicketsRepository]. Reads are flows: each new value replaces
 * the previous one, so the screens follow changes without a refresh.
 */
interface TicketsRepository {

    /** Active publicTickets (anonymous projection), newest first. Never reads tickets/. */
    fun publicTickets(): Flow<List<Ticket>>

    /** Full tickets whose participantIds contain [userId] (creator or follower), newest first. */
    fun participantTickets(userId: String): Flow<List<Ticket>>

    /** Every full ticket, newest first (operators). */
    fun allTickets(): Flow<List<Ticket>>

    /** Dashboard figures from stats/summary (decision 0005 meaning; private tickets counted). */
    fun stats(): Flow<DashboardStats>

    /** Active categories (I.3.E), sorted by name with Collator. */
    fun activeCategories(): Flow<List<Category>>

    /**
     * Opens a new ticket from the report form at [location] (the reporter's GPS fix). Returns its
     * id, or null when description, categoryId or categoryName is blank (all three are trimmed),
     * attachments is empty or location is the unset (0, 0). [creatorId] is the reporter. Returns
     * once the write is queued.
     */
    suspend fun reportTicket(
        description: String,
        categoryId: String,
        categoryName: String,
        situation: Situation,
        scope: Scope,
        attachments: List<Attachment>,
        location: Coordinates,
        creatorId: String,
    ): String?
}

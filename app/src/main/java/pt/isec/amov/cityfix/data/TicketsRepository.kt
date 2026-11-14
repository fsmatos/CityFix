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
 * What a valid report did (brief I.2 Obs., decision 0002): opened a new ticket, joined an active
 * ticket with the same category and situation within 10 m, or found that the reporter already
 * created or follows that ticket (nothing written).
 */
enum class ReportOutcome { CREATED, JOINED, ALREADY_FOLLOWING }

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
     * Saves a report from the form at [location] (the reporter's GPS fix): a new ticket, or a join
     * of a matching ticket within 10 m (see [ReportOutcome]). Returns null when description,
     * categoryId or categoryName is blank (all three are trimmed), attachments is empty or
     * location is the unset (0, 0). [creatorId] is the reporter. Returns once the write is queued.
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
    ): ReportOutcome?
}

package pt.isec.amov.cityfix.model

/**
 * An issue report ("Ocorrencia" in the brief), Option A: one ticket, several following citizens.
 * Defaults allow `Ticket()` for previews. A real ticket has at least one attachment and one
 * history entry (brief); creation logic enforces that.
 */
data class Ticket(
    /** Firestore document id. Empty until persisted; sample data must set distinct ids. */
    val id: String = "",
    val description: String = "",
    val situation: Situation = Situation.DAMAGE,
    val scope: Scope = Scope.PRIVATE,
    /** Exact report location. Anonymous views must not show it: coarse map zoom only (brief I.1.C). */
    val location: Coordinates = Coordinates(),
    /**
     * Denormalised copy of the last [statusHistory] entry. Kept so lists, filters and Firestore
     * queries read one field. Update both together.
     * Change it only through transition or closeIfExpired (TicketLifecycle.kt, decision 0004).
     * A projected ticket (toAnonymousView) has a status with an empty history.
     */
    val currentStatus: TicketStatus = TicketStatus.OPEN,
    /** Epoch milliseconds, UTC. */
    val createdAt: Long = 0L,
    val categoryId: String = "",
    /** Snapshot of the category name, so lists show it offline without a lookup. */
    val categoryName: String = "",
    /** User id of the citizen who opened the ticket. Not shown to anonymous users (brief I.1.C). */
    val creatorId: String = "",
    /**
     * Option A: citizens who reported the same category + situation within 10 m. They follow
     * this ticket instead of opening a new one. Does not include [creatorId]; no duplicates.
     * Not shown to anonymous users (brief I.1.C).
     */
    val followerIds: List<String> = emptyList(),
    val attachments: List<Attachment> = emptyList(),
    /** Thread between citizens and the municipality; registered participants and operators only (brief I.1.C). */
    val messages: List<Message> = emptyList(),
    val statusHistory: List<StatusHistory> = emptyList(),
    /** Time (epoch ms) an operator last came within 20 m while IN_PROGRESS (D-014). Time only, never a location. Null = never. */
    val lastOperatorNearbyAt: Long? = null,
)

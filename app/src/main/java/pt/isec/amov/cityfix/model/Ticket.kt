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
    val location: Coordinates = Coordinates(),
    /**
     * Denormalised copy of the last [statusHistory] entry. Kept so lists, filters and Firestore
     * queries read one field. Update both together.
     */
    val currentStatus: TicketStatus = TicketStatus.OPEN,
    /** Epoch milliseconds, UTC. */
    val createdAt: Long = 0L,
    val categoryId: String = "",
    /** Snapshot of the category name, so lists show it offline without a lookup. */
    val categoryName: String = "",
    val creatorId: String = "",
    /**
     * Option A: citizens who reported the same category + situation within 10 m. They follow
     * this ticket instead of opening a new one. Does not include [creatorId].
     */
    val followerIds: List<String> = emptyList(),
    val attachments: List<Attachment> = emptyList(),
    val messages: List<Message> = emptyList(),
    val statusHistory: List<StatusHistory> = emptyList(),
)

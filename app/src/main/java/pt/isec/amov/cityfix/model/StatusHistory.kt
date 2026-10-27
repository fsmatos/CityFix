package pt.isec.amov.cityfix.model

/**
 * One status transition of a ticket ("HistoricoEstado" in the diagram).
 * No id: ordered by position or [dateTime]. Add one if it becomes a Firestore subcollection.
 */
data class StatusHistory(
    val status: TicketStatus = TicketStatus.OPEN,
    /** Epoch milliseconds, UTC. */
    val dateTime: Long = 0L,
    /** Reason for reopening or for an info request; null otherwise. */
    val justification: String? = null,
    /** User id of who made the transition (creator for the first OPEN entry). */
    val responsibleId: String = "",
)

package pt.isec.amov.cityfix.model

/**
 * One message in a ticket thread ("Mensagem" in the diagram), stored in the
 * `tickets/{id}/messages` subcollection (F-014, amends decision 0010).
 */
data class Message(
    /** Firestore document id. Empty until persisted; sample data must set distinct ids. */
    val id: String = "",
    val text: String = "",
    /** Epoch milliseconds, UTC. */
    val sentAt: Long = 0L,
    /** User id of the sender. Never shown on screen: the thread shows "You", "Municipality" or "Citizen". */
    val authorId: String = "",
    /** True when an operator sent it, so the thread can label it "Municipality" without reading the author's profile (F-014 Q4). */
    val byOperator: Boolean = false,
)

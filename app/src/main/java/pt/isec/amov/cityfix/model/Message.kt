package pt.isec.amov.cityfix.model

/** One message in a ticket thread ("Mensagem" in the diagram). */
data class Message(
    /** Firestore document id. Empty until persisted; sample data must set distinct ids. */
    val id: String = "",
    val text: String = "",
    /** Epoch milliseconds, UTC. */
    val sentAt: Long = 0L,
    val authorId: String = "",
)

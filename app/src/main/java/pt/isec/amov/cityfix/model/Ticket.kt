package pt.isec.amov.cityfix.model

data class Ticket(
    val id: String = "",
    val description: String = "",
    val situation: Situation = Situation.DAMAGE,
    val scope: Scope = Scope.PRIVATE,
    val location: Coordinates = Coordinates(),
    val currentStatus: TicketStatus = TicketStatus.OPEN,
    val createdAt: Long = 0L,
    val categoryId: String = "",
    val categoryName: String = "",
    val creatorId: String = "",
    val followerIds: List<String> = emptyList(),
    val attachments: List<Attachment> = emptyList(),
    val messages: List<Message> = emptyList(),
    val statusHistory: List<StatusHistory> = emptyList(),
    val lastOperatorNearbyAt: Long? = null,
    val lastMessageAt: Long? = null,
    val lastMessageAuthorId: String? = null,
)

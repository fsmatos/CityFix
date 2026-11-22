package pt.isec.amov.cityfix.model

data class StatusHistory(
    val status: TicketStatus = TicketStatus.OPEN,
    val dateTime: Long = 0L,
    val justification: String? = null,
    val responsibleId: String = "",
)

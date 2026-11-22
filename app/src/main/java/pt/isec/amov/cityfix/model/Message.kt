package pt.isec.amov.cityfix.model

data class Message(
    val id: String = "",
    val text: String = "",
    val sentAt: Long = 0L,
    val authorId: String = "",
    val byOperator: Boolean = false,
)

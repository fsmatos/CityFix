package pt.isec.amov.cityfix.viewmodel

import androidx.annotation.StringRes
import pt.isec.amov.cityfix.model.Message

enum class DetailDialog {
    REOPEN,

    ASK_CLARIFICATION,
}

data class TicketDetailUiState(
    val messages: List<Message> = emptyList(),
    val draft: String = "",
    val openDialog: DetailDialog? = null,
    val dialogText: String = "",
    @StringRes val dialogError: Int? = null,
)

package pt.isec.amov.cityfix.viewmodel

import pt.isec.amov.cityfix.model.Category
import pt.isec.amov.cityfix.model.TicketFilter

data class OperatorPanelUiState(
    val categories: List<Category> = emptyList(),
    val filter: TicketFilter = TicketFilter(),
)

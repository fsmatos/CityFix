package pt.isec.amov.cityfix.viewmodel

import pt.isec.amov.cityfix.model.Category
import pt.isec.amov.cityfix.model.TicketFilter

/**
 * The operations panel's own state (brief I.3.E). The tickets are not here: they come from the
 * shared TicketsViewModel list (already every ticket for an operator), so the panel opens no
 * second listener (D-013 Q5).
 */
data class OperatorPanelUiState(
    /** Every category, inactive too (old tickets), sorted by name (CategoriesRepository). */
    val categories: List<Category> = emptyList(),
    val filter: TicketFilter = TicketFilter(),
)

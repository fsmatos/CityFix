package pt.isec.amov.cityfix.viewmodel

import pt.isec.amov.cityfix.model.Citizen

/** The Registration approvals list (D-012). */
data class PendingCitizensUiState(
    /** Citizens waiting for validation, oldest registration first. */
    val citizens: List<Citizen> = emptyList(),
    /** True until the first snapshot arrives. */
    val isLoading: Boolean = true,
)

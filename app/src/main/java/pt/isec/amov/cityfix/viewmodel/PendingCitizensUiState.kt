package pt.isec.amov.cityfix.viewmodel

import pt.isec.amov.cityfix.model.Citizen

data class PendingCitizensUiState(
    val citizens: List<Citizen> = emptyList(),
    val isLoading: Boolean = true,
)

package pt.isec.amov.cityfix.viewmodel

import pt.isec.amov.cityfix.model.Citizen
import pt.isec.amov.cityfix.model.RegistrationStatus

data class CitizenApprovalUiState(
    val citizen: Citizen? = null,
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val confirming: Boolean = false,
    val activated: Boolean = false,
) {
    val canActivate: Boolean get() = citizen?.registrationStatus == RegistrationStatus.PENDING

    val showAlreadyActive: Boolean get() = !canActivate && !activated
}

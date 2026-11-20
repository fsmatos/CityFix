package pt.isec.amov.cityfix.viewmodel

import pt.isec.amov.cityfix.model.Citizen
import pt.isec.amov.cityfix.model.RegistrationStatus

/** One pending citizen's registration, seen by an operator (D-012). */
data class CitizenApprovalUiState(
    val citizen: Citizen? = null,
    /** True until the first snapshot arrives. */
    val isLoading: Boolean = true,
    /** No users/{id} document, or not a citizen. */
    val notFound: Boolean = false,
    /** The Activate confirmation dialog is open (Q5). */
    val confirming: Boolean = false,
    /** This screen wrote the activation and is closing: keeps "already active" from flashing. */
    val activated: Boolean = false,
) {
    /** Only a PENDING citizen can be activated; an ACTIVE one was validated already (maybe by another operator). */
    val canActivate: Boolean get() = citizen?.registrationStatus == RegistrationStatus.PENDING

    /** "Already active" only when someone else activated it, not while this screen closes. */
    val showAlreadyActive: Boolean get() = !canActivate && !activated
}

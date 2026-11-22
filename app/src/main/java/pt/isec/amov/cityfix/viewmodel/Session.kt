package pt.isec.amov.cityfix.viewmodel

import pt.isec.amov.cityfix.model.Citizen
import pt.isec.amov.cityfix.model.Operator
import pt.isec.amov.cityfix.model.RegistrationStatus
import pt.isec.amov.cityfix.model.User

sealed interface Session {
    data object Loading : Session

    data object Anonymous : Session

    data class NeedsEmailVerification(val email: String) : Session

    data class NeedsPhoneVerification(val email: String) : Session

    data object ProfileMissing : Session

    data object ProfileUnavailable : Session

    data class SignedIn(val user: User) : Session
}

enum class AccessRole { LOADING, ANONYMOUS, SETUP, ACCOUNT_PROBLEM, PENDING_CITIZEN, CITIZEN, OPERATOR }

val Session.accessRole: AccessRole
    get() = when (this) {
        Session.Loading -> AccessRole.LOADING
        Session.Anonymous -> AccessRole.ANONYMOUS
        is Session.NeedsEmailVerification -> AccessRole.SETUP
        is Session.NeedsPhoneVerification -> AccessRole.SETUP
        Session.ProfileMissing -> AccessRole.ACCOUNT_PROBLEM
        Session.ProfileUnavailable -> AccessRole.ACCOUNT_PROBLEM
        is Session.SignedIn -> when (val user = user) {
            is Operator -> AccessRole.OPERATOR
            is Citizen ->
                if (user.registrationStatus == RegistrationStatus.ACTIVE) {
                    AccessRole.CITIZEN
                } else {
                    AccessRole.PENDING_CITIZEN
                }
        }
    }

val Session.canReport: Boolean
    get() = accessRole == AccessRole.CITIZEN

val Session.ticketsUser: User?
    get() = when (accessRole) {
        AccessRole.CITIZEN, AccessRole.OPERATOR -> (this as? Session.SignedIn)?.user
        else -> null
    }

val Session.isSignedIn: Boolean
    get() = this != Session.Loading && this != Session.Anonymous

package pt.isec.amov.cityfix.viewmodel

import pt.isec.amov.cityfix.model.Citizen
import pt.isec.amov.cityfix.model.Operator
import pt.isec.amov.cityfix.model.RegistrationStatus
import pt.isec.amov.cityfix.model.User

/**
 * Who is using the app, built by [SessionViewModel] from the Firebase account and the
 * `users/{uid}` profile. Checked in this order: no account, e-mail not verified, no second
 * factor, profile missing or unavailable, signed in.
 */
sealed interface Session {
    /** The first account and profile reads are not back yet. */
    data object Loading : Session

    data object Anonymous : Session

    data class NeedsEmailVerification(val email: String) : Session

    data class NeedsSecondFactor(val email: String) : Session

    /** An Auth account without a usable `users/{uid}` document (registration not finished). */
    data object ProfileMissing : Session

    /** The profile could not be read (offline and never cached). */
    data object ProfileUnavailable : Session

    /** A Citizen or an Operator. */
    data class SignedIn(val user: User) : Session
}

/** What the navigation needs from a [Session]: one value per home screen. */
enum class AccessRole { LOADING, ANONYMOUS, SETUP, ACCOUNT_PROBLEM, PENDING_CITIZEN, CITIZEN, OPERATOR }

val Session.accessRole: AccessRole
    get() = when (this) {
        Session.Loading -> AccessRole.LOADING
        Session.Anonymous -> AccessRole.ANONYMOUS
        is Session.NeedsEmailVerification -> AccessRole.SETUP
        is Session.NeedsSecondFactor -> AccessRole.SETUP
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

/** Only an active citizen reports issues (brief I.2.E); pending citizens wait for validation. */
val Session.canReport: Boolean
    get() = accessRole == AccessRole.CITIZEN

/** True for every state with a Firebase account, so Log out is offered. */
val Session.isSignedIn: Boolean
    get() = this != Session.Loading && this != Session.Anonymous

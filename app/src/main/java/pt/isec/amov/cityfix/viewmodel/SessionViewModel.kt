package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import pt.isec.amov.cityfix.data.AuthRepository
import pt.isec.amov.cityfix.data.UserProfile
import pt.isec.amov.cityfix.data.UsersRepository

/**
 * The app's [Session]. Created in CityFixApp above the NavHost (like TicketsViewModel), so every
 * destination shares it and it survives rotation. It combines the Firebase account from [auth]
 * with the `users/{uid}` profile from [users].
 */
class SessionViewModel(
    private val auth: AuthRepository,
    private val users: UsersRepository,
) : ViewModel() {

    /** Bumped by [retry]: a new value restarts the profile listener below. */
    private val retries = MutableStateFlow(0)

    // flatMapLatest: when the account changes (login, logout) or Retry is pressed, the listener
    // on the old profile is cancelled and a new one starts, so logging out stops the users/{uid}
    // listener.
    @OptIn(ExperimentalCoroutinesApi::class)
    val session: StateFlow<Session> = combine(auth.account, retries) { account, _ -> account }
        .flatMapLatest { account ->
            when {
                account == null -> flowOf(Session.Anonymous)
                // MFA is mandatory for both roles (Gate Q1), and Firebase needs a verified
                // e-mail before a factor can be enrolled: both steps come before the profile.
                !account.isEmailVerified -> flowOf(Session.NeedsEmailVerification(account.email))
                !account.hasSecondFactor -> flowOf(Session.NeedsSecondFactor(account.email))
                // E-mail sync (D-011): after a verified change through the link, the Auth e-mail
                // differs from users/{uid}.email, so it is copied once; the next snapshot then
                // matches and nothing more is written. Runs whether or not Profile is opened.
                else -> users.observeUser(account.uid)
                    .onEach { profile ->
                        if (profile is UserProfile.Found &&
                            account.email.isNotBlank() &&
                            !profile.user.email.equals(account.email, ignoreCase = true)
                        ) {
                            users.syncEmail(account.uid, account.email)
                        }
                    }
                    .map { it.toSession() }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Session.Loading)

    /** Reads the profile again (the "Retry" of the account problem screen). */
    fun retry() {
        retries.update { it + 1 }
    }

    fun signOut() {
        auth.signOut()
    }

    private fun UserProfile.toSession(): Session = when (this) {
        is UserProfile.Found -> Session.SignedIn(user)
        UserProfile.Missing -> Session.ProfileMissing
        UserProfile.Unavailable -> Session.ProfileUnavailable
    }
}

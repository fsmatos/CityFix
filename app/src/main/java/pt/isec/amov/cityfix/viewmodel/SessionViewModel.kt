package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

    // flatMapLatest: when the account changes (login, logout), the listener on the old user's
    // profile is cancelled and a new one starts, so logging out stops the users/{uid} listener.
    @OptIn(ExperimentalCoroutinesApi::class)
    val session: StateFlow<Session> = auth.account
        .flatMapLatest { account ->
            if (account == null) {
                flowOf(Session.Anonymous)
            } else {
                users.observeUser(account.uid).map { it.toSession() }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Session.Loading)

    fun signOut() {
        auth.signOut()
    }

    private fun UserProfile.toSession(): Session = when (this) {
        is UserProfile.Found -> Session.SignedIn(user)
        UserProfile.Missing -> Session.ProfileMissing
        UserProfile.Unavailable -> Session.ProfileUnavailable
    }
}

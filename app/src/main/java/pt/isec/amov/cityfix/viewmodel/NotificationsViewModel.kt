package pt.isec.amov.cityfix.viewmodel

import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import pt.isec.amov.cityfix.CityFixApplication
import pt.isec.amov.cityfix.data.TicketCheckWorker
import pt.isec.amov.cityfix.model.Operator
import pt.isec.amov.cityfix.model.User

/**
 * Notifications for the signed-in user (brief III, F-016). It has no UI state: CityFixApp tells
 * it who the user is, and it keeps the FCM token in Firestore and checks the tickets for changes
 * while the app is open.
 */
class NotificationsViewModel(private val app: CityFixApplication) : ViewModel() {

    /**
     * Called when the session's tickets user changes: an active citizen or an operator, or null
     * for everyone else (anonymous, pending, logged out). Saves the FCM token and starts the
     * periodic background check for that user, or stops it when there is none (Gate 1 Q2, Q7).
     */
    fun onUserChanged(user: User?) {
        if (user != null) {
            viewModelScope.launch { app.pushTokens.register(user.id) }
            TicketCheckWorker.schedule(app, user.id, user is Operator)
        } else {
            TicketCheckWorker.cancel(app)
        }
    }

    /**
     * Log out (review S1): removes this app's notifications from the tray, so the next person
     * on the device does not read the previous user's ticket texts.
     */
    fun onLoggedOut() {
        NotificationManagerCompat.from(app).cancelAll()
    }

    /**
     * Runs until cancelled (CityFixApp calls it inside repeatOnLifecycle, like D-014's proximity
     * watch): every list the server sends for this user is checked for changes.
     *
     * It uses its own listener (ticketsFromServer), not the shared list: the shared list starts
     * from the offline cache, and a check on cached data would move the last-check time past
     * changes made while the app was closed or offline, so they would never be notified.
     */
    suspend fun watch(user: User) {
        val isOperator = user is Operator
        app.ticketsRepository.ticketsFromServer(user.id, isOperator).collect { list ->
            app.ticketNotifier.check(user.id, isOperator, list, System.currentTimeMillis())
        }
    }
}

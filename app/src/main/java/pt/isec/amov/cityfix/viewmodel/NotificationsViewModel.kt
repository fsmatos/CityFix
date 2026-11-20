package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import pt.isec.amov.cityfix.CityFixApplication
import pt.isec.amov.cityfix.model.User

/**
 * Notifications for the signed-in user (brief III, F-016). It has no UI state: CityFixApp tells
 * it who the user is, and it keeps the FCM token in Firestore.
 */
class NotificationsViewModel(private val app: CityFixApplication) : ViewModel() {

    /**
     * Called when the session's tickets user changes: an active citizen or an operator, or null
     * for everyone else (anonymous, pending, logged out).
     */
    fun onUserChanged(user: User?) {
        if (user != null) {
            viewModelScope.launch { app.pushTokens.register(user.id) }
        }
    }
}

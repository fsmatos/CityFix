package pt.isec.amov.cityfix.data

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import pt.isec.amov.cityfix.CityFixApplication
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.utils.CHANNEL_TICKETS
import pt.isec.amov.cityfix.utils.showNotification

/**
 * Receives FCM messages (P11 MyFirebaseMessagingService, F-016). Registered in the manifest.
 * With the app in the foreground every message arrives here; in the background Android shows a
 * notification payload by itself and its data (e.g. `ticketId`) becomes the launch intent's
 * extras, which MainActivity reads the same way.
 */
class CityFixMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Message received from: ${remoteMessage.from}")

        // As P11 FCM.2: the notification payload first, the data fields as a fallback.
        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: getString(R.string.app_name)
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: ""
        val ticketId = remoteMessage.data["ticketId"]

        showNotification(this, CHANNEL_TICKETS, (ticketId ?: TAG).hashCode(), title, body, ticketId)
    }

    /** P11: the token changed, so store the new one for the signed-in user (if any). */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val app = application as CityFixApplication
        val user = app.auth.currentUser ?: return
        app.pushTokens.save(user.uid, token)
    }

    private companion object {
        const val TAG = "CityFixFCM"
    }
}

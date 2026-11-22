package pt.isec.amov.cityfix.data

import android.content.Context
import androidx.core.content.edit

class NotificationPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("notifications", Context.MODE_PRIVATE)

    fun lastCheckedAt(uid: String): Long? =
        if (prefs.contains(uid)) prefs.getLong(uid, 0L) else null

    fun save(uid: String, time: Long) {
        prefs.edit { putLong(uid, time) }
    }
}

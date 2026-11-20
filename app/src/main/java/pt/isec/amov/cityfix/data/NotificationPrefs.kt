package pt.isec.amov.cityfix.data

import android.content.Context
import androidx.core.content.edit

/**
 * The time of each user's last notification check (F-016, Gate 1 Q3), in SharedPreferences as
 * on slides 09. One value per user id, so another account on the same device starts fresh.
 */
class NotificationPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("notifications", Context.MODE_PRIVATE)

    /** Epoch ms of the last check of [uid], or null before the first one. */
    fun lastCheckedAt(uid: String): Long? =
        if (prefs.contains(uid)) prefs.getLong(uid, 0L) else null

    fun save(uid: String, time: Long) {
        prefs.edit { putLong(uid, time) }
    }
}

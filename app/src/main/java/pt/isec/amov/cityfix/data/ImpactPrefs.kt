package pt.isec.amov.cityfix.data

import android.content.Context
import androidx.core.content.edit

/**
 * The impact threshold chosen by the citizen (F-022, Q5), in SharedPreferences as on slides 09:
 * a device setting, like NotificationPrefs, not part of the Firestore profile. The detector reads
 * [threshold] on each sensor event, so a change applies at once.
 */
class ImpactPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("impact", Context.MODE_PRIVATE)

    /** Vertical acceleration (m/s², gravity removed) that counts as an impact. */
    fun threshold(): Float = prefs.getFloat(KEY_THRESHOLD, DEFAULT)

    fun save(value: Float) {
        prefs.edit { putFloat(KEY_THRESHOLD, value.coerceIn(MIN, MAX)) }
    }

    companion object {
        private const val KEY_THRESHOLD = "threshold"
        /** F-022 Q1: default 6 m/s², range 3..15 in steps of 0.5. Lower = more sensitive. */
        const val DEFAULT = 6f
        const val MIN = 3f
        const val MAX = 15f
        const val STEP = 0.5f
    }
}

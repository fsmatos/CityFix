package pt.isec.amov.cityfix.data

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pt.isec.amov.cityfix.CityFixApplication
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.Coordinates
import pt.isec.amov.cityfix.utils.CHANNEL_TRACKING
import pt.isec.amov.cityfix.utils.ImpactDetector
import pt.isec.amov.cityfix.utils.showImpactNotification

/**
 * Foreground service of type location (F-022), shaped like the TrackingService of class slides 08
 * and the ProximityService of sheet P10: a permanent notification, startForeground within 5 s and
 * a serviceScope cancelled in onDestroy. It keeps working with the app in the background or
 * closed, and runs two things on one stream of GPS fixes:
 * - the operator's 20 m geofence ([OperatorGeofence]) when started with an operator id;
 * - impact detection ([ImpactDetector]) for a citizen who turned it on; the fixes give the speed.
 * On an impact with the app in front it emits CityFixApplication.impacts (snackbar); otherwise it
 * shows a notification that opens the prefilled report. MainActivity starts and stops it from the
 * session ([start], [stop]); each start replaces what was running.
 */
class TrackingService : Service() {

    // Main: the sensor and location callbacks arrive on the main thread; Firestore calls suspend.
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var watchJob: Job? = null
    private var detector: ImpactDetector? = null

    /** Speed (m/s) and time of the last GPS fix, for the impact movement check. */
    private var lastSpeed = 0f
    private var lastFixAt = 0L

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, CHANNEL_TRACKING)
            .setContentTitle(getString(R.string.tracking_notification_title))
            .setContentText(getString(R.string.tracking_notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
        // As slides 08: the type is given explicitly from Android 10 (required on 14+).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        val operatorId = intent?.getStringExtra(EXTRA_OPERATOR_ID)
        val detectImpacts = intent?.getBooleanExtra(EXTRA_DETECT_IMPACTS, false) ?: false
        startWork(operatorId, detectImpacts)
        // Recreated with the same intent if the system kills it (slides 08).
        return START_REDELIVER_INTENT
    }

    private fun startWork(operatorId: String?, detectImpacts: Boolean) {
        stopWork()
        val app = application as CityFixApplication
        Log.d(TAG, "start (operator: ${operatorId != null}, impacts: $detectImpacts)")
        watchJob = serviceScope.launch {
            // One location request shared by the speed reader and the geofence.
            val fixes = app.locationRepository.locationFixes().shareIn(this, SharingStarted.Eagerly)
            launch {
                fixes.collect {
                    lastSpeed = if (it.hasSpeed()) it.speed else 0f
                    lastFixAt = System.currentTimeMillis()
                }
            }
            if (operatorId != null) {
                launch {
                    // An operator reads every ticket (as TicketsViewModel.setUser does).
                    val tickets = app.ticketsRepository.allTickets().stateIn(this)
                    OperatorGeofence(app.ticketsRepository)
                        .watch(fixes.map { Coordinates(it.latitude, it.longitude) }, tickets)
                }
            }
        }
        if (detectImpacts) {
            detector = ImpactDetector(
                this,
                threshold = app.impactPrefs::threshold,
                speed = { if (System.currentTimeMillis() - lastFixAt < FIX_MAX_AGE_MS) lastSpeed else null },
            ).also { it.start { onImpact(app) } }
        }
    }

    private fun onImpact(app: CityFixApplication) {
        if (app.appInForeground) {
            app.impacts.tryEmit(Unit)
        } else {
            showImpactNotification(this)
        }
    }

    private fun stopWork() {
        detector?.stop()
        detector = null
        watchJob?.cancel()
        watchJob = null
    }

    override fun onDestroy() {
        stopWork()
        serviceScope.cancel()
        Log.d(TAG, "stop")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "TrackingService"
        private const val NOTIFICATION_ID = 1001
        private const val EXTRA_OPERATOR_ID = "operatorId"
        private const val EXTRA_DETECT_IMPACTS = "detectImpacts"

        /** A GPS fix older than this (ms) says nothing about the current speed (F-022 Q2). */
        private const val FIX_MAX_AGE_MS = 5_000L

        /**
         * Starts the service, or updates what it runs, as P10 does with startForegroundService.
         * Must be called while the app is visible (Android 12+ forbids it from the background).
         */
        fun start(context: Context, operatorId: String?, detectImpacts: Boolean) {
            val intent = Intent(context, TrackingService::class.java)
                .putExtra(EXTRA_OPERATOR_ID, operatorId)
                .putExtra(EXTRA_DETECT_IMPACTS, detectImpacts)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, TrackingService::class.java))
        }
    }
}

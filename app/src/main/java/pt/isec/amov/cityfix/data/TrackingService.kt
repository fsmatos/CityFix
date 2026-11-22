package pt.isec.amov.cityfix.data

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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

class TrackingService : Service() {

    // Main: the sensor and location callbacks arrive on the main thread; Firestore calls suspend.
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var watchJob: Job? = null
    private var detector: ImpactDetector? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Review S1: when the system restarts it (START_REDELIVER_INTENT) the user may have removed
        // the location permission meanwhile; a location service cannot start then (Android 14).
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if (fine != PackageManager.PERMISSION_GRANTED) {
            stopSelf()
            return START_NOT_STICKY
        }
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
            // Started eagerly for both roles (this is a location service); only the geofence reads it.
            val fixes = app.locationRepository.locationFixes().shareIn(this, SharingStarted.Eagerly)
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
            detector = ImpactDetector(this).also { it.start { onImpact(app) } }
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

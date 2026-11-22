package pt.isec.amov.cityfix.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import pt.isec.amov.cityfix.model.Coordinates

/**
 * Reads the device location with the FusedLocationProviderClient (P10). Create one per app
 * (CityFixApplication); it keeps the application context only.
 */
class LocationRepository(context: Context) {

    private val appContext = context.applicationContext
    private val client = LocationServices.getFusedLocationProviderClient(appContext)

    /**
     * One current fix, or null when there is no location permission or no fix (location off).
     * Beyond P10, which receives continuous updates in a service: getCurrentLocation asks for a
     * single fix and await() waits for it without a callback. [locationUpdates] gives continuous
     * fixes while collected (D-014).
     */
    suspend fun currentLocation(): Coordinates? {
        // Written out instead of hasLocationPermission(): lint's MissingPermission check only
        // sees checkSelfPermission calls in this function.
        val fine = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        return client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
            ?.let { Coordinates(it.latitude, it.longitude) }
    }

    /**
     * Continuous precise fixes every [intervalMs] while collected (D-014, brief II.3), as
     * coordinates. See [locationFixes].
     */
    fun locationUpdates(intervalMs: Long = 5_000): Flow<Coordinates> =
        locationFixes(intervalMs).map { Coordinates(it.latitude, it.longitude) }

    /**
     * Continuous precise fixes every [intervalMs] while collected, as full [Location]s so the
     * caller can also read the speed (F-022 impact check); empty without FINE. Fixes less
     * accurate than [MAX_FIX_ACCURACY_METERS] are dropped, since a 20 m check needs a precise
     * position. Collecting stops the updates when cancelled (awaitClose). As P10; F-022 collects
     * it inside TrackingService, so it also runs with the app in the background. If the updates
     * cannot start (for example no Play services), the flow ends after a log line.
     */
    fun locationFixes(intervalMs: Long = 5_000): Flow<Location> {
        // Written out, as in currentLocation, for lint's MissingPermission check.
        val fine =
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION)
        if (fine != PackageManager.PERMISSION_GRANTED) return emptyFlow()
        // Gate 1 Q11: every 5 s, but accept fixes other apps request as often as every 2 s.
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(MIN_UPDATE_INTERVAL_MS)
            .build()
        return callbackFlow {
            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation
                        ?.takeIf { !it.hasAccuracy() || it.accuracy <= MAX_FIX_ACCURACY_METERS }
                        ?.let { trySend(it) }
                }
            }
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
                .addOnFailureListener {
                    // No coordinates in the log. close() without the exception, so the
                    // collector ends quietly instead of crashing.
                    Log.w(TAG, "location updates not started", it)
                    close()
                }
            awaitClose { client.removeLocationUpdates(callback) }
        }
    }

    private companion object {
        const val TAG = "LocationRepository"

        /** Fixes with a larger accuracy radius (metres) are too coarse for the 20 m geofence. */
        const val MAX_FIX_ACCURACY_METERS = 25f

        /** Shortest gap (ms) between fixes, when other apps ask for faster updates (Q11). */
        const val MIN_UPDATE_INTERVAL_MS = 2_000L
    }
}

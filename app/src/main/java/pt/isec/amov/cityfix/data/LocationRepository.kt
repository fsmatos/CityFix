package pt.isec.amov.cityfix.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ApiException
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

class LocationRepository(context: Context) {

    private val appContext = context.applicationContext
    private val client = LocationServices.getFusedLocationProviderClient(appContext)

    suspend fun currentLocation(): Coordinates? {
        // Written out instead of hasLocationPermission(): lint's MissingPermission check only
        // sees checkSelfPermission calls in this function.
        val fine = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        return try {
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                ?.let { Coordinates(it.latitude, it.longitude) }
        } catch (e: ApiException) {
            // D-022: Play services can fail or disconnect; the callers treat it as "no fix".
            Log.w(TAG, "Current location failed", e)
            null
        }
    }

    fun locationUpdates(intervalMs: Long = 5_000): Flow<Coordinates> =
        locationFixes(intervalMs).map { Coordinates(it.latitude, it.longitude) }

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

        const val MAX_FIX_ACCURACY_METERS = 25f

        const val MIN_UPDATE_INTERVAL_MS = 2_000L
    }
}

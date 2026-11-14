package pt.isec.amov.cityfix.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
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
     * single fix and await() waits for it without a callback. D-014 adds updates here.
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
}

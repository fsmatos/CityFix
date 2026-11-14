package pt.isec.amov.cityfix.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/** The two location permissions requested together, as in P10 (RequestMultiplePermissions). */
val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

/** True when fine or coarse location is granted (checkSelfPermission, class 02). */
fun hasLocationPermission(context: Context): Boolean =
    LOCATION_PERMISSIONS.any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

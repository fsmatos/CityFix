package pt.isec.amov.cityfix.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
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

/**
 * True when precise (fine) location is granted. The report form needs it: with "Approximate"
 * (Android 12+) the fix is about 2 km off, which would break the 10 m grouping.
 */
fun hasPreciseLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

/**
 * Opens this app's page in the system settings. After "don't ask again" Android no longer shows
 * the permission dialog, so this is the only way left to allow location.
 */
fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null),
    )
    context.startActivity(intent)
}

package pt.isec.amov.cityfix.utils

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Mean Earth radius in metres used by [haversineMeters]. */
const val EARTH_RADIUS_METERS = 6_371_000.0

/**
 * Great-circle distance in metres between two WGS84 points given in degrees (Haversine formula).
 * Used by the 10 m report grouping, and later by the 20 m geofence and the 10 km anonymous radius.
 */
fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val sinHalfLat = sin(dLat / 2)
    val sinHalfLon = sin(dLon / 2)
    val a = sinHalfLat * sinHalfLat +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sinHalfLon * sinHalfLon
    return 2 * EARTH_RADIUS_METERS * atan2(sqrt(a), sqrt(1 - a))
}

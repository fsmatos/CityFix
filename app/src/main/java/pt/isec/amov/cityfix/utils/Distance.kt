package pt.isec.amov.cityfix.utils

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

const val EARTH_RADIUS_METERS = 6_371_000.0

fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val sinHalfLat = sin(dLat / 2)
    val sinHalfLon = sin(dLon / 2)
    // Rounding can push a slightly outside 0..1 for near-antipodal points; sqrt(1 - a) would be NaN.
    val a = (
        sinHalfLat * sinHalfLat +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sinHalfLon * sinHalfLon
        ).coerceIn(0.0, 1.0)
    return 2 * EARTH_RADIUS_METERS * atan2(sqrt(a), sqrt(1 - a))
}

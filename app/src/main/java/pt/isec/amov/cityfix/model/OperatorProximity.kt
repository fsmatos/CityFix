package pt.isec.amov.cityfix.model

import pt.isec.amov.cityfix.utils.haversineMeters

/*
 * Operator 20 m geofence (brief I.3 Obs, II.3; D-014). Pure rules: the time is a parameter, the
 * caller reads the clock once per location fix.
 */

const val OPERATOR_GEOFENCE_RADIUS_METERS = 20.0

const val PROXIMITY_ALERT_INTERVAL_MS: Long = 60L * 60 * 1000

fun Ticket.isWithinOperatorGeofence(at: Coordinates): Boolean =
    haversineMeters(location.latitude, location.longitude, at.latitude, at.longitude) <=
        OPERATOR_GEOFENCE_RADIUS_METERS

fun Ticket.canRecordOperatorNearby(now: Long): Boolean {
    if (statusHistory.isEmpty()) return false // projected ticket (decision 0006): never written
    if (!needsOperatorGeofence(now)) return false
    val last = lastOperatorNearbyAt ?: return true
    return now - last >= PROXIMITY_ALERT_INTERVAL_MS
}

fun List<Ticket>.insideOperatorGeofence(at: Coordinates, now: Long): Set<String> =
    filter {
        it.statusHistory.isNotEmpty() && it.needsOperatorGeofence(now) &&
            it.isWithinOperatorGeofence(at)
    }.map { it.id }.toSet()

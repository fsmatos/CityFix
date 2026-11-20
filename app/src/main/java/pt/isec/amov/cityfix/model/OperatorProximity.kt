package pt.isec.amov.cityfix.model

import pt.isec.amov.cityfix.utils.haversineMeters

/*
 * Operator 20 m geofence (brief I.3 Obs, II.3; D-014). Pure rules: the time is a parameter, the
 * caller reads the clock once per location fix.
 */

/** Brief II.3: an operator within this distance of an IN_PROGRESS ticket triggers the alert. */
const val OPERATOR_GEOFENCE_RADIUS_METERS = 20.0

/** At most one proximity record per ticket per hour, so participants are not flooded (D-014 Q6). */
const val PROXIMITY_ALERT_INTERVAL_MS: Long = 60L * 60 * 1000

/**
 * True when [at] is within [OPERATOR_GEOFENCE_RADIUS_METERS] of this ticket's location
 * (Haversine).
 */
fun Ticket.isWithinOperatorGeofence(at: Coordinates): Boolean =
    haversineMeters(location.latitude, location.longitude, at.latitude, at.longitude) <=
        OPERATOR_GEOFENCE_RADIUS_METERS

/**
 * True when a new proximity record may be written at [now]: the ticket is a full ticket (a
 * projected one has an empty statusHistory, decision 0006), needs the geofence
 * ([needsOperatorGeofence]) and the last record is at least [PROXIMITY_ALERT_INTERVAL_MS] old.
 * A last record in the future (clock skew) gives false.
 */
fun Ticket.canRecordOperatorNearby(now: Long): Boolean {
    if (statusHistory.isEmpty()) return false // projected ticket (decision 0006): never written
    if (!needsOperatorGeofence(now)) return false
    val last = lastOperatorNearbyAt ?: return true
    return now - last >= PROXIMITY_ALERT_INTERVAL_MS
}

/**
 * Ids of the full tickets (non-empty statusHistory, so never a projected one) that need the
 * geofence at [now] and have [at] inside their 20 m circle.
 */
fun List<Ticket>.insideOperatorGeofence(at: Coordinates, now: Long): Set<String> =
    filter {
        it.statusHistory.isNotEmpty() && it.needsOperatorGeofence(now) &&
            it.isWithinOperatorGeofence(at)
    }.map { it.id }.toSet()

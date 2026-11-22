package pt.isec.amov.cityfix.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import pt.isec.amov.cityfix.model.Coordinates
import pt.isec.amov.cityfix.model.PROXIMITY_ALERT_INTERVAL_MS
import pt.isec.amov.cityfix.model.Ticket
import pt.isec.amov.cityfix.model.insideOperatorGeofence

/**
 * The operator's 20 m geofence (brief I.3 Obs, II.3; D-014): on each location fix it finds the
 * IN_PROGRESS tickets within 20 m and records the operator's arrival on the ones just entered
 * (TicketsRepository.recordOperatorNearby). F-022 moved this loop from OperatorProximityViewModel
 * into a plain class, so TrackingService can run it with the app in the background. One instance
 * per watch: the sets below start empty each time the service (re)starts the watch.
 */
class OperatorGeofence(private val ticketsRepository: TicketsRepository) {

    /** Ids of the tickets inside the 20 m circle on the last fix. */
    private val inside = mutableSetOf<String>()

    /**
     * Ticket id -> time this device last recorded it. Lost when the service stops, where the
     * stored lastOperatorNearbyAt still applies the 1 h throttle.
     */
    private val recentlyRecorded = mutableMapOf<String, Long>()

    /**
     * Runs until cancelled. Reads [tickets].value on each fix. Only an entry counts: a ticket
     * already inside on the previous fix is skipped, so staying near it writes nothing.
     */
    suspend fun watch(fixes: Flow<Coordinates>, tickets: StateFlow<List<Ticket>>) {
        fixes.collect { fix ->
            val now = System.currentTimeMillis()
            val current = tickets.value.insideOperatorGeofence(fix, now)
            val entered = current - inside
            inside.clear()
            inside += current
            for (id in entered) {
                val ticket = tickets.value.firstOrNull { it.id == id } ?: continue
                val last = recentlyRecorded[id]
                if (last != null && now - last < PROXIMITY_ALERT_INTERVAL_MS) continue
                if (ticketsRepository.recordOperatorNearby(ticket, now)) recentlyRecorded[id] = now
            }
        }
    }
}

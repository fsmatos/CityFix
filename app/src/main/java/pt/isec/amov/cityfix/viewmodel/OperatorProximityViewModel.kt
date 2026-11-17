package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import pt.isec.amov.cityfix.data.LocationRepository
import pt.isec.amov.cityfix.data.TicketsRepository
import pt.isec.amov.cityfix.model.PROXIMITY_ALERT_INTERVAL_MS
import pt.isec.amov.cityfix.model.Ticket
import pt.isec.amov.cityfix.model.insideOperatorGeofence

/**
 * The operator's 20 m geofence (brief I.3 Obs, II.3; D-014), checked in the foreground: on each
 * location fix it finds the IN_PROGRESS tickets within 20 m and records the operator's arrival on
 * the ones just entered (TicketsRepository.recordOperatorNearby). It has no UI state.
 */
class OperatorProximityViewModel(
    private val locationRepository: LocationRepository,
    private val ticketsRepository: TicketsRepository,
) : ViewModel() {

    /**
     * Ticket id -> time this device last recorded it. Survives rotation; lost on process death,
     * where the stored lastOperatorNearbyAt still applies the 1 h throttle.
     */
    private val recentlyRecorded = mutableMapOf<String, Long>()

    /**
     * Runs until cancelled (CityFixApp calls it inside repeatOnLifecycle). Reads [tickets].value on
     * each fix. Only an entry counts: a ticket already inside on the previous fix is skipped, so
     * staying near it writes nothing. The set of tickets inside starts empty on every call.
     */
    suspend fun watch(tickets: StateFlow<List<Ticket>>) {
        val inside = mutableSetOf<String>()
        locationRepository.locationUpdates().collect { fix ->
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

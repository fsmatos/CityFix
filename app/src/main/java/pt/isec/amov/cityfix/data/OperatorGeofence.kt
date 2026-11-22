package pt.isec.amov.cityfix.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import pt.isec.amov.cityfix.model.Coordinates
import pt.isec.amov.cityfix.model.PROXIMITY_ALERT_INTERVAL_MS
import pt.isec.amov.cityfix.model.Ticket
import pt.isec.amov.cityfix.model.insideOperatorGeofence

class OperatorGeofence(private val ticketsRepository: TicketsRepository) {

    private val inside = mutableSetOf<String>()

    private val recentlyRecorded = mutableMapOf<String, Long>()

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

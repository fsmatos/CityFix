package pt.isec.amov.cityfix.data

import android.content.Context
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.Ticket
import pt.isec.amov.cityfix.model.TicketEvent
import pt.isec.amov.cityfix.model.TicketEventKind
import pt.isec.amov.cityfix.model.TicketStatus
import pt.isec.amov.cityfix.model.eventsFor
import pt.isec.amov.cityfix.utils.CHANNEL_MESSAGES
import pt.isec.amov.cityfix.utils.CHANNEL_PROXIMITY
import pt.isec.amov.cityfix.utils.CHANNEL_TICKETS
import pt.isec.amov.cityfix.utils.showNotification

/**
 * Turns ticket changes into notifications (brief III, F-016). Used by the in-app watch
 * (NotificationsViewModel) and by the periodic TicketCheckWorker. Create one per app
 * (CityFixApplication).
 *
 * Each check notifies the events between the user's last check and now (model eventsFor) and
 * then saves now. The title is the category; the text never holds a location or operator data
 * (decision 0022).
 */
class TicketNotifier(private val context: Context) {
    private val prefs = NotificationPrefs(context)

    /**
     * Notifies [uid] of what changed in [tickets] since their last check. The first check of a
     * user notifies nothing: it only saves [now] (Gate 1 Q3), so old history is not replayed.
     */
    fun check(uid: String, isOperator: Boolean, tickets: List<Ticket>, now: Long) {
        val since = prefs.lastCheckedAt(uid)
        if (since == null) {
            prefs.save(uid, now)
            return
        }
        for (ticket in tickets) {
            for (event in ticket.eventsFor(uid, isOperator, since, now)) {
                show(event)
            }
        }
        prefs.save(uid, now)
    }

    /** One notification; the id is per ticket and kind, so the app and the worker replace each other's. */
    private fun show(event: TicketEvent) {
        val channel = when (event.kind) {
            TicketEventKind.MESSAGE -> CHANNEL_MESSAGES
            TicketEventKind.OPERATOR_NEARBY -> CHANNEL_PROXIMITY
            else -> CHANNEL_TICKETS
        }
        val text = when (event.kind) {
            TicketEventKind.STATUS -> statusText(event)
            TicketEventKind.MESSAGE -> context.getString(R.string.notif_new_message)
            TicketEventKind.OPERATOR_NEARBY -> context.getString(R.string.notif_operator_nearby)
            TicketEventKind.CLOSING_SOON -> context.getString(R.string.notif_closing_soon)
            TicketEventKind.CLOSED -> context.getString(R.string.notif_closed)
        }
        val id = (event.ticketId + event.kind.name).hashCode()
        showNotification(context, channel, id, event.categoryName, text, event.ticketId)
    }

    /** "New status: In progress", or "Info requested: <justification>" when there is one (Q9, Q10). */
    private fun statusText(event: TicketEvent): String {
        val status = context.getString(statusLabel(event.status ?: TicketStatus.OPEN))
        val justification = event.justification
        return if (justification == null) {
            context.getString(R.string.notif_status, status)
        } else {
            context.getString(R.string.notif_status_with_reason, status, justification)
        }
    }

    private fun statusLabel(status: TicketStatus): Int = when (status) {
        TicketStatus.OPEN -> R.string.status_open
        TicketStatus.INFO_REQUESTED -> R.string.status_info_requested
        TicketStatus.IN_PROGRESS -> R.string.status_in_progress
        TicketStatus.RESOLVED -> R.string.status_resolved
        TicketStatus.CLOSED -> R.string.status_closed
    }
}

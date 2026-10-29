package pt.isec.amov.cityfix.ui.navigation

import kotlinx.serialization.Serializable

// Type-safe Navigation Compose routes for the phase-1 flow: dashboard -> list -> detail -> report.
// The NavHost that uses them is built in MainActivity (D-006).

/** Start destination: dashboard figures. */
@Serializable
data object DashboardRoute

/** List of public active tickets. */
@Serializable
data object TicketListRoute

/**
 * Detail of one ticket. Carries only the id; the screen takes the ticket from the collected
 * `tickets` list. Use ids from that list only, never the id returned by `reportTicket`
 * (a PRIVATE report is not in the anonymous list).
 */
@Serializable
data class TicketDetailRoute(val ticketId: String)

/** Provisional report form. */
@Serializable
data object ReportTicketRoute

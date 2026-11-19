package pt.isec.amov.cityfix.ui.navigation

import kotlinx.serialization.Serializable

// Type-safe Navigation Compose routes of the app. The NavHost that uses them is built in
// MainActivity (D-006); the role routes' homes, menu and guards are in RoleAccess.kt (D-009).

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

/** Report form; [fromImpact] prefills Pavement + Damage after an impact suggestion (F-015). */
@Serializable
data class ReportTicketRoute(val fromImpact: Boolean = false)

/** E-mail and password; the SMS code is a step of this route (D-009, slice 6). */
@Serializable
data object LoginRoute

/** Self-registration for citizens and operators, opened from Login (D-010). */
@Serializable
data object RegisterRoute

/** Password reset; [email] prefills the field with what was typed on Login. */
@Serializable
data class ForgotPasswordRoute(val email: String = "")

/** Home of a citizen whose registration an operator has not validated yet (D-009 Q2). */
@Serializable
data object PendingValidationRoute

/** Operator home. Placeholder until D-013's operations panel. */
@Serializable
data object OperatorPanelRoute

/** Tickets the signed-in active citizen created or follows (F-014). */
@Serializable
data object MyTicketsRoute

/** Profile of the signed-in user. Placeholder until D-011. */
@Serializable
data object ProfileRoute

/** Operator: citizen registrations to validate. Placeholder until D-012. */
@Serializable
data object ApprovalsRoute

/** Operator: ticket categories. Placeholder until D-012. */
@Serializable
data object CategoriesRoute

/** An account without a usable profile (missing, or not loadable offline). */
@Serializable
data object AccountProblemRoute

/** First login of an account without MFA: verify the e-mail, then enrol a phone (D-009 Q1, Q8). */
@Serializable
data object AccountSetupRoute

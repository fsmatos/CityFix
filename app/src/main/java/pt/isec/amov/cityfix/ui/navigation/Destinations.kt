package pt.isec.amov.cityfix.ui.navigation

import kotlinx.serialization.Serializable

// Type-safe Navigation Compose routes of the app. The NavHost that uses them is built in
// MainActivity (D-006); the role routes' homes, menu and guards are in RoleAccess.kt (D-009).

@Serializable
data object DashboardRoute

@Serializable
data object TicketListRoute

@Serializable
data object ActiveTicketsRoute

@Serializable
data class TicketDetailRoute(val ticketId: String)

@Serializable
data class ReportTicketRoute(val fromImpact: Boolean = false)

@Serializable
data object LoginRoute

@Serializable
data object RegisterRoute

@Serializable
data class ForgotPasswordRoute(val email: String = "")

@Serializable
data object PendingValidationRoute

@Serializable
data object OperatorPanelRoute

@Serializable
data object ProfileRoute

@Serializable
data object EditProfileRoute

@Serializable
data object ChangePasswordRoute

@Serializable
data object ChangeEmailRoute

@Serializable
data object ApprovalsRoute

@Serializable
data class PendingCitizenRoute(val citizenId: String)

@Serializable
data object CategoriesRoute

@Serializable
data object AccountProblemRoute

@Serializable
data object AccountSetupRoute

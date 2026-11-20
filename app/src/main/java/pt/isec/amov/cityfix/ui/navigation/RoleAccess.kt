package pt.isec.amov.cityfix.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import pt.isec.amov.cityfix.viewmodel.AccessRole

/**
 * The home route of each role, where CityFixApp's redirect goes when the role changes.
 * LOADING never navigates (the redirect skips it).
 */
fun homeFor(role: AccessRole): Any = when (role) {
    AccessRole.LOADING,
    AccessRole.ANONYMOUS,
    AccessRole.CITIZEN -> DashboardRoute
    AccessRole.SETUP -> AccountSetupRoute
    AccessRole.ACCOUNT_PROBLEM -> AccountProblemRoute
    AccessRole.PENDING_CITIZEN -> PendingValidationRoute
    AccessRole.OPERATOR -> OperatorPanelRoute
}

/**
 * The bottom bar tabs of each role (D-016), in order; the first is the role's home. Roles without
 * a usable account (setup, account problem, loading) get none, so no bar is drawn.
 */
fun bottomTabsFor(role: AccessRole): List<BottomTab> = when (role) {
    AccessRole.CITIZEN,
    AccessRole.PENDING_CITIZEN -> listOf(BottomTab.DASHBOARD, BottomTab.ACTIVE, BottomTab.PROFILE)
    AccessRole.ANONYMOUS -> listOf(BottomTab.DASHBOARD, BottomTab.ACTIVE)
    AccessRole.OPERATOR -> listOf(
        BottomTab.OPERATOR_PANEL,
        BottomTab.APPROVALS,
        BottomTab.CATEGORIES,
        BottomTab.PROFILE,
    )
    AccessRole.LOADING,
    AccessRole.SETUP,
    AccessRole.ACCOUNT_PROBLEM -> emptyList()
}

/**
 * Guard for a destination: draws [content] only when [role] is in [allowed]. Otherwise it draws
 * nothing and calls [onDenied] once, which navigates home. Covers back-stack leftovers after a
 * logout and any future deep link; the entry points (buttons, tabs) are hidden as well.
 */
@Composable
fun RequireAccess(
    role: AccessRole,
    allowed: Set<AccessRole>,
    onDenied: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (role in allowed) {
        content()
    } else {
        LaunchedEffect(role) { onDenied() }
    }
}

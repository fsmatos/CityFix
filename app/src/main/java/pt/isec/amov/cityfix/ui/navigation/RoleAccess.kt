package pt.isec.amov.cityfix.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import pt.isec.amov.cityfix.viewmodel.AccessRole

fun homeFor(role: AccessRole): Any = when (role) {
    AccessRole.LOADING,
    AccessRole.ANONYMOUS,
    AccessRole.CITIZEN -> DashboardRoute
    AccessRole.SETUP -> AccountSetupRoute
    AccessRole.ACCOUNT_PROBLEM -> AccountProblemRoute
    AccessRole.PENDING_CITIZEN -> PendingValidationRoute
    AccessRole.OPERATOR -> OperatorPanelRoute
}

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

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
    AccessRole.SETUP,
    AccessRole.ACCOUNT_PROBLEM,
    AccessRole.PENDING_CITIZEN,
    AccessRole.CITIZEN,
    AccessRole.OPERATOR -> DashboardRoute
}

/**
 * Guard for a destination: draws [content] only when [role] is in [allowed]. Otherwise it draws
 * nothing and calls [onDenied] once, which navigates home. Covers back-stack leftovers after a
 * logout and any future deep link; the entry points (buttons, menu) are hidden as well.
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

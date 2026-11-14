package pt.isec.amov.cityfix.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import pt.isec.amov.cityfix.R
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

/** One entry of the top bar's overflow menu: a label and the route it opens. */
data class TopBarMenuItem(@StringRes val label: Int, val route: Any)

/** The role-only screens each role reaches from the overflow menu (brief I.2, I.3). */
fun menuItemsFor(role: AccessRole): List<TopBarMenuItem> = when (role) {
    AccessRole.CITIZEN -> listOf(
        TopBarMenuItem(R.string.menu_my_tickets, MyTicketsRoute),
        TopBarMenuItem(R.string.menu_profile, ProfileRoute),
    )
    AccessRole.PENDING_CITIZEN -> listOf(
        TopBarMenuItem(R.string.menu_profile, ProfileRoute),
    )
    AccessRole.OPERATOR -> listOf(
        TopBarMenuItem(R.string.menu_approvals, ApprovalsRoute),
        TopBarMenuItem(R.string.menu_categories, CategoriesRoute),
        TopBarMenuItem(R.string.menu_profile, ProfileRoute),
    )
    AccessRole.LOADING,
    AccessRole.ANONYMOUS,
    AccessRole.SETUP,
    AccessRole.ACCOUNT_PROBLEM -> emptyList()
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

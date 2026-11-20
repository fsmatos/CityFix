package pt.isec.amov.cityfix.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.ui.theme.CityFixTheme

/**
 * One tab of the bottom bar (D-016): its label, icon, the route it opens and the screens that
 * belong to it (the tab is shown selected on those). Which tabs a role gets is in RoleAccess.kt.
 */
enum class BottomTab(
    @StringRes val label: Int,
    val icon: ImageVector,
    val route: Any,
    val screens: Set<TopBarScreen>,
) {
    DASHBOARD(
        R.string.nav_dashboard,
        Icons.Filled.Home,
        DashboardRoute,
        setOf(TopBarScreen.DASHBOARD, TopBarScreen.PENDING_VALIDATION),
    ),
    ACTIVE(
        R.string.nav_active,
        Icons.Filled.Place,
        ActiveTicketsRoute,
        setOf(TopBarScreen.ACTIVE_TICKETS, TopBarScreen.LIST),
    ),
    PROFILE(R.string.nav_profile, Icons.Filled.Person, ProfileRoute, setOf(TopBarScreen.PROFILE)),
    OPERATOR_PANEL(
        R.string.nav_operator_panel,
        Icons.Filled.Home,
        OperatorPanelRoute,
        setOf(TopBarScreen.OPERATOR_PANEL),
    ),
    APPROVALS(
        R.string.nav_approvals,
        Icons.Filled.CheckCircle,
        ApprovalsRoute,
        setOf(TopBarScreen.APPROVALS, TopBarScreen.PENDING_CITIZEN),
    ),
    CATEGORIES(
        R.string.nav_categories,
        Icons.AutoMirrored.Filled.List,
        CategoriesRoute,
        setOf(TopBarScreen.CATEGORIES),
    ),
}

/** The screens that are a tab's own page: the top bar shows no back arrow on them. */
val tabRootScreens: Set<TopBarScreen> = setOf(
    TopBarScreen.DASHBOARD,
    TopBarScreen.ACTIVE_TICKETS,
    TopBarScreen.PROFILE,
    TopBarScreen.OPERATOR_PANEL,
    TopBarScreen.APPROVALS,
    TopBarScreen.CATEGORIES,
    TopBarScreen.PENDING_VALIDATION,
)

/**
 * App bottom navigation bar (Material 3 NavigationBar). Stateless: the caller gives the role's
 * [tabs] and the [selected] one, and receives the clicks. The icon is decorative: the label is
 * the tab's accessible name.
 */
@Composable
fun MainBottomBar(
    tabs: List<BottomTab>,
    selected: BottomTab?,
    onTabClick: (BottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = tab == selected,
                onClick = { onTabClick(tab) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = {
                    Text(
                        text = stringResource(tab.label),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun CitizenBottomBarPreview() {
    CityFixTheme {
        MainBottomBar(
            tabs = listOf(BottomTab.DASHBOARD, BottomTab.ACTIVE, BottomTab.PROFILE),
            selected = BottomTab.DASHBOARD,
            onTabClick = {},
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun OperatorBottomBarPreview() {
    CityFixTheme {
        MainBottomBar(
            tabs = listOf(BottomTab.OPERATOR_PANEL, BottomTab.APPROVALS, BottomTab.CATEGORIES, BottomTab.PROFILE),
            selected = BottomTab.APPROVALS,
            onTabClick = {},
        )
    }
}

package pt.isec.amov.cityfix.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.ui.theme.CityFixTheme

/**
 * One tab of the bottom bar (D-016): its accessible name, its icon (outlined; filled when
 * selected), the route it opens and the screens that belong to it (the tab is shown selected on
 * those). Which tabs a role gets is in RoleAccess.kt.
 */
enum class BottomTab(
    @StringRes val label: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val route: Any,
    val screens: Set<TopBarScreen>,
) {
    DASHBOARD(
        R.string.nav_dashboard,
        Icons.Outlined.Home,
        Icons.Filled.Home,
        DashboardRoute,
        setOf(TopBarScreen.DASHBOARD, TopBarScreen.PENDING_VALIDATION),
    ),
    ACTIVE(
        R.string.nav_active,
        Icons.Outlined.Warning,
        Icons.Filled.Warning,
        ActiveTicketsRoute,
        setOf(TopBarScreen.ACTIVE_TICKETS, TopBarScreen.LIST),
    ),
    PROFILE(
        R.string.nav_profile,
        Icons.Outlined.Person,
        Icons.Filled.Person,
        ProfileRoute,
        setOf(TopBarScreen.PROFILE),
    ),
    OPERATOR_PANEL(
        R.string.nav_operator_panel,
        Icons.Outlined.Home,
        Icons.Filled.Home,
        OperatorPanelRoute,
        setOf(TopBarScreen.OPERATOR_PANEL),
    ),
    APPROVALS(
        R.string.nav_approvals,
        Icons.Outlined.CheckCircle,
        Icons.Filled.CheckCircle,
        ApprovalsRoute,
        setOf(TopBarScreen.APPROVALS, TopBarScreen.PENDING_CITIZEN),
    ),
    CATEGORIES(
        R.string.nav_categories,
        Icons.AutoMirrored.Outlined.List,
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

/** Height of the island: a 56dp tab plus 8dp above and below. */
val IslandHeight = 72.dp

/** Gap between the island and the system navigation bar (or the bottom of the screen). */
val IslandBottomMargin = 12.dp

/** What the island takes above the system navigation bar: margin plus island. */
val IslandClearance = IslandHeight + IslandBottomMargin

/**
 * Bottom padding a scrolling screen adds after its last item, so it can scroll above the island
 * (island, system navigation bar and a gap; also the report button on the Dashboard). 0.dp when
 * no island is shown. Provided by CityFixApp around the NavHost.
 */
val LocalBottomBarPadding = compositionLocalOf { 0.dp }

/**
 * App bottom bar as a floating "island" (D-016): a pill with the card background, a thin outline
 * and a soft shadow, centred and only as wide as its icons, [IslandBottomMargin] above the system
 * navigation bar (it adds that inset itself). It is drawn over the content, which scrolls under
 * it (see [LocalBottomBarPadding]). Icons only: each tab's label is its contentDescription, and
 * the row is a selectable group of tabs for TalkBack. Stateless: the caller gives the role's
 * [tabs] and the [selected] one, and receives the clicks.
 */
@Composable
fun MainBottomBar(
    tabs: List<BottomTab>,
    selected: BottomTab?,
    onTabClick: (BottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, bottom = IslandBottomMargin),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 6.dp,
        ) {
            Row(
                modifier = Modifier
                    .height(IslandHeight)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEach { tab ->
                    IslandTab(
                        tab = tab,
                        isSelected = tab == selected,
                        onClick = { onTabClick(tab) },
                    )
                }
            }
        }
    }
}

/**
 * One 56dp tab (above the 48dp minimum): the icon, with a discreet circle behind it when
 * selected. The circle is the content colour at low alpha, so it is slightly darker on a light
 * theme and lighter on a dark one.
 */
@Composable
private fun IslandTab(tab: BottomTab, isSelected: Boolean, onClick: () -> Unit) {
    val circle by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f) else Color.Transparent,
        label = "tabCircle",
    )
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "tabIcon",
    )
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(circle)
            .selectable(selected = isSelected, onClick = onClick, role = Role.Tab),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isSelected) tab.selectedIcon else tab.icon,
            contentDescription = stringResource(tab.label),
            tint = iconColor,
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun CitizenBottomBarPreview() {
    CityFixTheme {
        MainBottomBar(
            tabs = listOf(BottomTab.DASHBOARD, BottomTab.ACTIVE, BottomTab.PROFILE),
            selected = BottomTab.ACTIVE,
            onTabClick = {},
        )
    }
}

@Preview(showBackground = true)
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

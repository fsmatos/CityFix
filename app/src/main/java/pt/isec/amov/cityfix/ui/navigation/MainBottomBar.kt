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
        // Not LIST: it also opens from the Dashboard, so it keeps the tab it was opened from (D-022).
        setOf(TopBarScreen.ACTIVE_TICKETS),
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

val tabRootScreens: Set<TopBarScreen> = setOf(
    TopBarScreen.DASHBOARD,
    TopBarScreen.ACTIVE_TICKETS,
    TopBarScreen.PROFILE,
    TopBarScreen.OPERATOR_PANEL,
    TopBarScreen.APPROVALS,
    TopBarScreen.CATEGORIES,
    TopBarScreen.PENDING_VALIDATION,
)

val IslandHeight = 72.dp

val IslandBottomMargin = 12.dp

val IslandClearance = IslandHeight + IslandBottomMargin

val LocalBottomBarPadding = compositionLocalOf { 0.dp }

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

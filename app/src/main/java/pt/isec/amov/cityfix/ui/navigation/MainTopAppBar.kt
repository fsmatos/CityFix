package pt.isec.amov.cityfix.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.ui.theme.CityFixTheme

/** The screen the top bar is showing. One value per route in Destinations.kt. */
enum class TopBarScreen {
    DASHBOARD, LIST, DETAIL, REPORT, LOGIN, FORGOT_PASSWORD,
    PENDING_VALIDATION, OPERATOR_PANEL, MY_TICKETS, PROFILE, APPROVALS, CATEGORIES, ACCOUNT_PROBLEM,
    ACCOUNT_SETUP, REGISTER, CHANGE_PASSWORD, CHANGE_EMAIL, PENDING_CITIZEN,
}

/** The account button at the end of the bar: Log in for anonymous users, Log out once signed in. */
enum class AccountAction { NONE, LOG_IN, LOG_OUT }

/**
 * App top bar. Stateless: the caller says which screen is showing and receives the clicks.
 * The toggle appears on the list only, and only when [showExpandToggle]; anonymous users never
 * get it (brief I.1.B, D-007). Reporting is a floating button in the caller's Scaffold, not a
 * bar action (D-015, Q5). [accountAction] picks
 * the Log in / Log out button, which calls [onAccountClick]. [menuItems] are the role's screens,
 * in an overflow menu shown only when the list is not empty; when it is shown, Log out is its
 * last entry instead of a separate icon, so the bar does not overflow in portrait.
 * Routes are plain values here: the bar knows nothing about navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopAppBar(
    screen: TopBarScreen,
    canNavigateBack: Boolean,
    isExpanded: Boolean,
    showExpandToggle: Boolean,
    accountAction: AccountAction,
    menuItems: List<TopBarMenuItem>,
    onBackClick: () -> Unit,
    onToggleExpandedClick: () -> Unit,
    onAccountClick: () -> Unit,
    onMenuItemClick: (TopBarMenuItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val titleRes = when (screen) {
        TopBarScreen.DASHBOARD -> R.string.top_bar_title_dashboard
        TopBarScreen.LIST -> R.string.top_bar_title_list
        TopBarScreen.DETAIL -> R.string.top_bar_title_detail
        TopBarScreen.REPORT -> R.string.top_bar_title_report
        TopBarScreen.LOGIN -> R.string.top_bar_title_login
        TopBarScreen.FORGOT_PASSWORD -> R.string.top_bar_title_forgot_password
        TopBarScreen.PENDING_VALIDATION -> R.string.top_bar_title_pending
        TopBarScreen.OPERATOR_PANEL -> R.string.top_bar_title_operator_panel
        TopBarScreen.MY_TICKETS -> R.string.top_bar_title_my_tickets
        TopBarScreen.PROFILE -> R.string.top_bar_title_profile
        TopBarScreen.APPROVALS -> R.string.top_bar_title_approvals
        TopBarScreen.CATEGORIES -> R.string.top_bar_title_categories
        TopBarScreen.ACCOUNT_PROBLEM -> R.string.top_bar_title_account_problem
        TopBarScreen.ACCOUNT_SETUP -> R.string.top_bar_title_account_setup
        TopBarScreen.REGISTER -> R.string.top_bar_title_register
        TopBarScreen.CHANGE_PASSWORD -> R.string.top_bar_title_change_password
        TopBarScreen.CHANGE_EMAIL -> R.string.top_bar_title_change_email
        TopBarScreen.PENDING_CITIZEN -> R.string.top_bar_title_pending_citizen
    }
    val showMenu = menuItems.isNotEmpty()
    var menuExpanded by rememberSaveable { mutableStateOf(false) }

    TopAppBar(
        title = {
            Text(
                text = stringResource(titleRes),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        modifier = modifier,
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_navigate_back),
                    )
                }
            }
        },
        actions = {
            if (screen == TopBarScreen.LIST && showExpandToggle) {
                IconButton(onClick = onToggleExpandedClick) {
                    Icon(
                        imageVector = if (isExpanded) {
                            Icons.Filled.KeyboardArrowUp
                        } else {
                            Icons.Filled.KeyboardArrowDown
                        },
                        contentDescription = stringResource(
                            if (isExpanded) R.string.cd_collapse_tickets else R.string.cd_expand_tickets
                        ),
                    )
                }
            }
            if (showMenu) {
                // The Box anchors the DropdownMenu to the button.
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.cd_more_options),
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        menuItems.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(stringResource(item.label)) },
                                onClick = {
                                    menuExpanded = false
                                    onMenuItemClick(item)
                                },
                            )
                        }
                        if (accountAction == AccountAction.LOG_OUT) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_log_out)) },
                                onClick = {
                                    menuExpanded = false
                                    onAccountClick()
                                },
                            )
                        }
                    }
                }
            }
            when (accountAction) {
                AccountAction.NONE -> Unit
                AccountAction.LOG_IN -> IconButton(onClick = onAccountClick) {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = stringResource(R.string.cd_log_in),
                    )
                }
                AccountAction.LOG_OUT -> if (!showMenu) {
                    IconButton(onClick = onAccountClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = stringResource(R.string.cd_log_out),
                        )
                    }
                }
            }
        },
    )
}

private const val PORTRAIT = "spec:width=411dp,height=891dp"
private const val LANDSCAPE = "spec:width=411dp,height=891dp,orientation=landscape"

@Preview(name = "Portrait", device = PORTRAIT, showBackground = true)
@Preview(name = "Landscape", device = LANDSCAPE, showBackground = true)
private annotation class OrientationPreviews

@Composable
private fun BarPreview(
    screen: TopBarScreen,
    canNavigateBack: Boolean,
    isExpanded: Boolean = false,
    showExpandToggle: Boolean = true,
    accountAction: AccountAction = AccountAction.LOG_OUT,
    menuItems: List<TopBarMenuItem> = emptyList(),
) {
    CityFixTheme {
        MainTopAppBar(
            screen = screen,
            canNavigateBack = canNavigateBack,
            isExpanded = isExpanded,
            showExpandToggle = showExpandToggle,
            accountAction = accountAction,
            menuItems = menuItems,
            onBackClick = {},
            onToggleExpandedClick = {},
            onAccountClick = {},
            onMenuItemClick = {},
        )
    }
}

@OrientationPreviews
@Composable
private fun DashboardBarPreview() {
    BarPreview(TopBarScreen.DASHBOARD, canNavigateBack = false)
}

@OrientationPreviews
@Composable
private fun ListCollapsedBarPreview() {
    BarPreview(TopBarScreen.LIST, canNavigateBack = true)
}

@OrientationPreviews
@Composable
private fun ListExpandedBarPreview() {
    BarPreview(TopBarScreen.LIST, canNavigateBack = true, isExpanded = true)
}

@OrientationPreviews
@Composable
private fun ListAnonymousBarPreview() {
    BarPreview(TopBarScreen.LIST, canNavigateBack = true, showExpandToggle = false)
}

@OrientationPreviews
@Composable
private fun DashboardAnonymousBarPreview() {
    BarPreview(
        TopBarScreen.DASHBOARD,
        canNavigateBack = false,
        accountAction = AccountAction.LOG_IN,
    )
}

@OrientationPreviews
@Composable
private fun OperatorBarPreview() {
    BarPreview(
        TopBarScreen.OPERATOR_PANEL,
        canNavigateBack = false,
        menuItems = listOf(
            TopBarMenuItem(R.string.menu_approvals, ApprovalsRoute),
            TopBarMenuItem(R.string.menu_categories, CategoriesRoute),
            TopBarMenuItem(R.string.menu_profile, ProfileRoute),
        ),
    )
}

@OrientationPreviews
@Composable
private fun LoginBarPreview() {
    BarPreview(
        TopBarScreen.LOGIN,
        canNavigateBack = true,
        accountAction = AccountAction.NONE,
    )
}

@OrientationPreviews
@Composable
private fun DetailBarPreview() {
    BarPreview(TopBarScreen.DETAIL, canNavigateBack = true)
}

@OrientationPreviews
@Composable
private fun ReportBarPreview() {
    BarPreview(TopBarScreen.REPORT, canNavigateBack = true)
}

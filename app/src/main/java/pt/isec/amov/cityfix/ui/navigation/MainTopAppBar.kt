package pt.isec.amov.cityfix.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.ui.theme.CityFixTheme

/** The screen the top bar is showing. One value per route in Destinations.kt. */
enum class TopBarScreen {
    DASHBOARD, ACTIVE_TICKETS, LIST, DETAIL, REPORT, LOGIN, FORGOT_PASSWORD,
    PENDING_VALIDATION, OPERATOR_PANEL, PROFILE, EDIT_PROFILE, APPROVALS, CATEGORIES, ACCOUNT_PROBLEM,
    ACCOUNT_SETUP, REGISTER, CHANGE_PASSWORD, CHANGE_EMAIL, PENDING_CITIZEN,
}

/** The account button at the end of the bar: Log in for anonymous users, Log out once signed in. */
enum class AccountAction { NONE, LOG_IN, LOG_OUT }

/**
 * App top bar. Stateless: the caller says which screen is showing and receives the clicks.
 * The toggle appears on the list only, and only when [showExpandToggle]; anonymous users never
 * get it (brief I.1.B, D-007). Reporting is a floating button in the caller's Scaffold, not a
 * bar action (D-015, Q5). [accountAction] picks
 * the Log in / Log out button, which calls [onAccountClick]. The role's screens are tabs of the
 * bottom bar (D-016, MainBottomBar.kt), so there is no overflow menu; Log out is the Material
 * "Logout" icon (res/drawable/ic_logout.xml, not in material-icons-core).
 * The bar knows nothing about navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopAppBar(
    screen: TopBarScreen,
    canNavigateBack: Boolean,
    isExpanded: Boolean,
    showExpandToggle: Boolean,
    accountAction: AccountAction,
    onBackClick: () -> Unit,
    onToggleExpandedClick: () -> Unit,
    onAccountClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val titleRes = when (screen) {
        TopBarScreen.DASHBOARD -> R.string.top_bar_title_dashboard
        TopBarScreen.ACTIVE_TICKETS -> R.string.top_bar_title_active
        TopBarScreen.LIST -> R.string.top_bar_title_list
        TopBarScreen.DETAIL -> R.string.top_bar_title_detail
        TopBarScreen.REPORT -> R.string.top_bar_title_report
        TopBarScreen.LOGIN -> R.string.top_bar_title_login
        TopBarScreen.FORGOT_PASSWORD -> R.string.top_bar_title_forgot_password
        TopBarScreen.PENDING_VALIDATION -> R.string.top_bar_title_pending
        TopBarScreen.OPERATOR_PANEL -> R.string.top_bar_title_operator_panel
        TopBarScreen.PROFILE -> R.string.top_bar_title_profile
        TopBarScreen.EDIT_PROFILE -> R.string.top_bar_title_edit_profile
        TopBarScreen.APPROVALS -> R.string.top_bar_title_approvals
        TopBarScreen.CATEGORIES -> R.string.top_bar_title_categories
        TopBarScreen.ACCOUNT_PROBLEM -> R.string.top_bar_title_account_problem
        TopBarScreen.ACCOUNT_SETUP -> R.string.top_bar_title_account_setup
        TopBarScreen.REGISTER -> R.string.top_bar_title_register
        TopBarScreen.CHANGE_PASSWORD -> R.string.top_bar_title_change_password
        TopBarScreen.CHANGE_EMAIL -> R.string.top_bar_title_change_email
        TopBarScreen.PENDING_CITIZEN -> R.string.top_bar_title_pending_citizen
    }
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
            when (accountAction) {
                AccountAction.NONE -> Unit
                AccountAction.LOG_IN -> IconButton(onClick = onAccountClick) {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = stringResource(R.string.cd_log_in),
                    )
                }
                AccountAction.LOG_OUT -> IconButton(onClick = onAccountClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_logout),
                        contentDescription = stringResource(R.string.cd_log_out),
                    )
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
) {
    CityFixTheme {
        MainTopAppBar(
            screen = screen,
            canNavigateBack = canNavigateBack,
            isExpanded = isExpanded,
            showExpandToggle = showExpandToggle,
            accountAction = accountAction,
            onBackClick = {},
            onToggleExpandedClick = {},
            onAccountClick = {},
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

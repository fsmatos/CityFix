package pt.isec.amov.cityfix

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.coroutines.launch
import pt.isec.amov.cityfix.ui.navigation.DashboardRoute
import pt.isec.amov.cityfix.ui.navigation.MainTopAppBar
import pt.isec.amov.cityfix.ui.navigation.ReportTicketRoute
import pt.isec.amov.cityfix.ui.navigation.TicketDetailRoute
import pt.isec.amov.cityfix.ui.navigation.TicketListRoute
import pt.isec.amov.cityfix.ui.navigation.TopBarScreen
import pt.isec.amov.cityfix.ui.screens.DashboardScreen
import pt.isec.amov.cityfix.ui.screens.DetailAudience
import pt.isec.amov.cityfix.ui.screens.ListScreen
import pt.isec.amov.cityfix.ui.screens.ReportScreen
import pt.isec.amov.cityfix.ui.screens.ShowScreen
import pt.isec.amov.cityfix.ui.theme.CityFixTheme
import pt.isec.amov.cityfix.viewmodel.ReportViewModel
import pt.isec.amov.cityfix.viewmodel.TicketsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is light-only, so keep dark system bar icons even when the
        // device is in dark mode (the default style follows the device).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        setContent {
            CityFixTheme {
                CityFixApp()
            }
        }
    }
}

/**
 * The app's navigation and shared state. The screens are stateless; this is their stateful half.
 * TicketsViewModel is created here, above the NavHost, so every destination shares one instance.
 */
@Composable
private fun CityFixApp() {
    val ticketsViewModel: TicketsViewModel = viewModel()
    val tickets by ticketsViewModel.tickets.collectAsStateWithLifecycle()
    val stats by ticketsViewModel.stats.collectAsStateWithLifecycle()

    var isExpanded by rememberSaveable { mutableStateOf(false) }

    // Above the NavHost: the report destination is popped while the snackbar is showing.
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val reportSubmittedMessage = stringResource(R.string.report_submitted)

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val screen = backStackEntry?.destination.toTopBarScreen()
    // previousBackStackEntry is not Compose state: read it here, in the same scope as
    // backStackEntry, so a back-stack change recomposes this line too (D-005 review N4).
    val canNavigateBack = navController.previousBackStackEntry != null

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            MainTopAppBar(
                screen = screen,
                canNavigateBack = canNavigateBack,
                isExpanded = isExpanded,
                onBackClick = { navController.navigateUp() },
                onToggleExpandedClick = { isExpanded = !isExpanded },
                onAddClick = {
                    navController.navigate(ReportTicketRoute) { launchSingleTop = true }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = DashboardRoute,
            // consumeWindowInsets: ReportContent's imePadding() must not add these insets again.
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            composable<DashboardRoute> {
                DashboardScreen(
                    stats = stats,
                    onSeeTicketsClick = {
                        navController.navigate(TicketListRoute) { launchSingleTop = true }
                    },
                )
            }
            composable<TicketListRoute> {
                ListScreen(
                    tickets = tickets,
                    isExpanded = isExpanded,
                    onTicketClick = { id ->
                        navController.navigate(TicketDetailRoute(id)) { launchSingleTop = true }
                    },
                )
            }
            composable<TicketDetailRoute> { entry ->
                val id = entry.toRoute<TicketDetailRoute>().ticketId
                ShowScreen(
                    ticket = tickets.firstOrNull { it.id == id },
                    // Phase 1 has no login: every user is anonymous (decisions 0006, 0007).
                    audience = DetailAudience.PUBLIC,
                )
            }
            composable<ReportTicketRoute> {
                // Inside the destination: scoped to this entry, so a finished form is cleared.
                val reportViewModel: ReportViewModel = viewModel()
                ReportScreen(
                    viewModel = reportViewModel,
                    onSubmit = { submission ->
                        val id = ticketsViewModel.reportTicket(
                            description = submission.description,
                            categoryId = submission.categoryId,
                            categoryName = submission.categoryName,
                            situation = submission.situation,
                            scope = submission.scope,
                        )
                        if (id != null) {
                            // Never navigate to id: a PRIVATE ticket is not visible (decision 0006).
                            snackbarScope.launch {
                                snackbarHostState.showSnackbar(reportSubmittedMessage)
                            }
                            navController.navigateUp()
                        }
                        id != null
                    },
                )
            }
        }
    }
}

/** Maps the current destination to the bar's enum. Add new routes here: `else` will not warn. */
private fun NavDestination?.toTopBarScreen(): TopBarScreen = when {
    this == null -> TopBarScreen.DASHBOARD
    hasRoute<TicketListRoute>() -> TopBarScreen.LIST
    hasRoute<TicketDetailRoute>() -> TopBarScreen.DETAIL
    hasRoute<ReportTicketRoute>() -> TopBarScreen.REPORT
    else -> TopBarScreen.DASHBOARD
}

package pt.isec.amov.cityfix

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import pt.isec.amov.cityfix.ui.navigation.RequireAccess
import pt.isec.amov.cityfix.ui.navigation.TicketDetailRoute
import pt.isec.amov.cityfix.ui.navigation.TicketListRoute
import pt.isec.amov.cityfix.ui.navigation.TopBarScreen
import pt.isec.amov.cityfix.ui.screens.DashboardScreen
import pt.isec.amov.cityfix.ui.screens.DetailAudience
import pt.isec.amov.cityfix.ui.screens.ListScreen
import pt.isec.amov.cityfix.ui.screens.ReportScreen
import pt.isec.amov.cityfix.ui.screens.ShowScreen
import pt.isec.amov.cityfix.ui.theme.CityFixTheme
import pt.isec.amov.cityfix.viewmodel.AccessRole
import pt.isec.amov.cityfix.viewmodel.ReportViewModel
import pt.isec.amov.cityfix.viewmodel.ReportViewModelFactory
import pt.isec.amov.cityfix.viewmodel.Session
import pt.isec.amov.cityfix.viewmodel.SessionViewModel
import pt.isec.amov.cityfix.viewmodel.SessionViewModelFactory
import pt.isec.amov.cityfix.viewmodel.accessRole
import pt.isec.amov.cityfix.viewmodel.canReport
import pt.isec.amov.cityfix.viewmodel.TicketsViewModel
import pt.isec.amov.cityfix.viewmodel.TicketsViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is light-only, so keep dark system bar icons even when the
        // device is in dark mode (the default style follows the device).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        // Like ContactsApp in Contacts_#3: the one repository lives in the Application.
        val app = application as CityFixApplication
        setContent {
            CityFixTheme {
                CityFixApp(app)
            }
        }
    }
}

/**
 * The app's navigation and shared state. The screens are stateless; this is their stateful half.
 * TicketsViewModel and SessionViewModel are created here, above the NavHost, so every destination
 * shares one instance of each. [app] holds the repositories the ViewModel factories receive.
 */
@Composable
private fun CityFixApp(app: CityFixApplication) {
    val ticketsViewModel: TicketsViewModel =
        viewModel(factory = TicketsViewModelFactory(app.ticketsRepository))
    val tickets by ticketsViewModel.tickets.collectAsStateWithLifecycle()
    val stats by ticketsViewModel.stats.collectAsStateWithLifecycle()

    val sessionViewModel: SessionViewModel =
        viewModel(factory = SessionViewModelFactory(app.authRepository, app.usersRepository))
    val session by sessionViewModel.session.collectAsStateWithLifecycle()
    val role = session.accessRole

    // Nobody gets the expand toggle (D-009 Gate Q7). `tickets` is always the projected public
    // list, so an expanded card would add only the date; brief I.1.B lists category, situation and
    // thumbnail, and I.2.D gives citizens the anonymous features. The operator's full view
    // (coordinates, author, history) is D-013's operations panel (brief I.3.E), not this list.
    // The flag and the `canExpandCards && isExpanded` below stay for that later decision.
    val canExpandCards = false
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    // Above the NavHost: the report destination is popped while the snackbar is showing.
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val reportSubmittedMessage = stringResource(R.string.report_submitted)
    val loadingDescription = stringResource(R.string.cd_loading_session)

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val screen = backStackEntry?.destination.toTopBarScreen()
    // previousBackStackEntry is not Compose state: read it here, in the same scope as
    // backStackEntry, so a back-stack change recomposes this line too (D-005 review N4).
    val canNavigateBack = navController.previousBackStackEntry != null

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            MainTopAppBar(
                screen = screen,
                canNavigateBack = canNavigateBack,
                isExpanded = isExpanded,
                showExpandToggle = canExpandCards,
                showAddAction = session.canReport && screen != TopBarScreen.REPORT,
                onBackClick = { navController.navigateUp() },
                onToggleExpandedClick = { isExpanded = !isExpanded },
                onAddClick = {
                    navController.navigate(ReportTicketRoute) { launchSingleTop = true }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (session == Session.Loading) {
            // Until the account and profile are known, show nothing role-specific: an operator
            // must not see the citizen dashboard flash before the redirect.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.semantics { contentDescription = loadingDescription },
                )
            }
            return@Scaffold
        }
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
                    isExpanded = canExpandCards && isExpanded,
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
                // Active citizens only (brief I.2.E); anyone else is sent back to the dashboard.
                RequireAccess(
                    role = role,
                    allowed = setOf(AccessRole.CITIZEN),
                    onDenied = {
                        navController.navigate(DashboardRoute) {
                            popUpTo<DashboardRoute> { inclusive = true }
                        }
                    },
                ) {
                    // Inside the destination: scoped to this entry, so a finished form is cleared.
                    val reportViewModel: ReportViewModel =
                        viewModel(
                            factory = ReportViewModelFactory(app.ticketsRepository, app.attachmentStorage),
                        )
                    ReportScreen(
                        viewModel = reportViewModel,
                        // Like saveContact { refreshContacts(); popBackStack() } in Contacts_#3.
                        // No id here: never navigate to the new ticket, a PRIVATE one is not
                        // visible (decision 0006).
                        onSubmitted = {
                            ticketsViewModel.refresh()
                            snackbarScope.launch {
                                snackbarHostState.showSnackbar(reportSubmittedMessage)
                            }
                            navController.navigateUp()
                        },
                    )
                }
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

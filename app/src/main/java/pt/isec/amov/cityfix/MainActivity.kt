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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import pt.isec.amov.cityfix.ui.navigation.DashboardRoute
import pt.isec.amov.cityfix.ui.navigation.MainTopAppBar
import pt.isec.amov.cityfix.ui.navigation.TicketListRoute
import pt.isec.amov.cityfix.ui.navigation.TopBarScreen
import pt.isec.amov.cityfix.ui.screens.DashboardScreen
import pt.isec.amov.cityfix.ui.screens.ListScreen
import pt.isec.amov.cityfix.ui.theme.CityFixTheme
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
                onAddClick = {}, // slice 5
            )
        },
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
                    onTicketClick = {}, // slice 4
                )
            }
        }
    }
}

/** Maps the current destination to the bar's enum. Add new routes here: `else` will not warn. */
private fun NavDestination?.toTopBarScreen(): TopBarScreen = when {
    this == null -> TopBarScreen.DASHBOARD
    hasRoute<TicketListRoute>() -> TopBarScreen.LIST
    else -> TopBarScreen.DASHBOARD
}

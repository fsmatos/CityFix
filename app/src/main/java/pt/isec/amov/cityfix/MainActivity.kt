package pt.isec.amov.cityfix

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import pt.isec.amov.cityfix.data.ReportOutcome
import pt.isec.amov.cityfix.data.TrackingService
import pt.isec.amov.cityfix.model.Citizen
import pt.isec.amov.cityfix.model.Operator
import pt.isec.amov.cityfix.model.isActive
import pt.isec.amov.cityfix.model.isParticipant
import pt.isec.amov.cityfix.ui.navigation.AccountAction
import pt.isec.amov.cityfix.ui.navigation.AccountProblemRoute
import pt.isec.amov.cityfix.ui.navigation.AccountSetupRoute
import pt.isec.amov.cityfix.ui.navigation.ActiveTicketsRoute
import pt.isec.amov.cityfix.ui.navigation.ApprovalsRoute
import pt.isec.amov.cityfix.ui.navigation.BottomTab
import pt.isec.amov.cityfix.ui.navigation.CategoriesRoute
import pt.isec.amov.cityfix.ui.navigation.ChangeEmailRoute
import pt.isec.amov.cityfix.ui.navigation.ChangePasswordRoute
import pt.isec.amov.cityfix.ui.navigation.DashboardRoute
import pt.isec.amov.cityfix.ui.navigation.EditProfileRoute
import pt.isec.amov.cityfix.ui.navigation.IslandClearance
import pt.isec.amov.cityfix.ui.navigation.LocalBottomBarPadding
import pt.isec.amov.cityfix.ui.navigation.ForgotPasswordRoute
import pt.isec.amov.cityfix.ui.navigation.LoginRoute
import pt.isec.amov.cityfix.ui.navigation.MainBottomBar
import pt.isec.amov.cityfix.ui.navigation.MainTopAppBar
import pt.isec.amov.cityfix.ui.navigation.OperatorPanelRoute
import pt.isec.amov.cityfix.ui.navigation.PendingCitizenRoute
import pt.isec.amov.cityfix.ui.navigation.PendingValidationRoute
import pt.isec.amov.cityfix.ui.navigation.ProfileRoute
import pt.isec.amov.cityfix.ui.navigation.RegisterRoute
import pt.isec.amov.cityfix.ui.navigation.ReportTicketRoute
import pt.isec.amov.cityfix.ui.navigation.RequireAccess
import pt.isec.amov.cityfix.ui.navigation.TicketDetailRoute
import pt.isec.amov.cityfix.ui.navigation.TicketListRoute
import pt.isec.amov.cityfix.ui.navigation.TopBarScreen
import pt.isec.amov.cityfix.ui.navigation.bottomTabsFor
import pt.isec.amov.cityfix.ui.navigation.homeFor
import pt.isec.amov.cityfix.ui.navigation.tabRootScreens
import pt.isec.amov.cityfix.ui.screens.AccountProblemScreen
import pt.isec.amov.cityfix.ui.screens.ActiveTicketsScreen
import pt.isec.amov.cityfix.ui.screens.AccountSetupScreen
import pt.isec.amov.cityfix.ui.screens.CategoriesScreen
import pt.isec.amov.cityfix.ui.screens.ChangeCredentialScreen
import pt.isec.amov.cityfix.ui.screens.CitizenApprovalScreen
import pt.isec.amov.cityfix.ui.screens.DashboardScreen
import pt.isec.amov.cityfix.ui.screens.DetailAudience
import pt.isec.amov.cityfix.ui.screens.ForgotPasswordScreen
import pt.isec.amov.cityfix.ui.screens.ListScreen
import pt.isec.amov.cityfix.ui.screens.LocationGateScreen
import pt.isec.amov.cityfix.ui.screens.LoginScreen
import pt.isec.amov.cityfix.ui.screens.OperatorPanelScreen
import pt.isec.amov.cityfix.ui.screens.OperatorTicketScreen
import pt.isec.amov.cityfix.ui.screens.PendingCitizensScreen
import pt.isec.amov.cityfix.ui.screens.PendingValidationScreen
import pt.isec.amov.cityfix.ui.screens.ProfileOverviewScreen
import pt.isec.amov.cityfix.ui.screens.ProfileScreen
import pt.isec.amov.cityfix.ui.screens.RegisterScreen
import pt.isec.amov.cityfix.ui.screens.ReportScreen
import pt.isec.amov.cityfix.ui.screens.ShowScreen
import pt.isec.amov.cityfix.ui.screens.TicketDetailScreen
import pt.isec.amov.cityfix.ui.screens.detailAudience
import pt.isec.amov.cityfix.ui.theme.CityFixTheme
import pt.isec.amov.cityfix.ui.theme.Dimens
import pt.isec.amov.cityfix.utils.EXTRA_FROM_IMPACT
import pt.isec.amov.cityfix.utils.EXTRA_TICKET_ID
import pt.isec.amov.cityfix.utils.LOCATION_PERMISSIONS
import pt.isec.amov.cityfix.utils.hasPreciseLocationPermission
import pt.isec.amov.cityfix.viewmodel.AccessRole
import pt.isec.amov.cityfix.viewmodel.AccountSetupViewModel
import pt.isec.amov.cityfix.viewmodel.AccountSetupViewModelFactory
import pt.isec.amov.cityfix.viewmodel.CategoriesViewModel
import pt.isec.amov.cityfix.viewmodel.CategoriesViewModelFactory
import pt.isec.amov.cityfix.viewmodel.CitizenApprovalViewModel
import pt.isec.amov.cityfix.viewmodel.CitizenApprovalViewModelFactory
import pt.isec.amov.cityfix.viewmodel.CredentialChange
import pt.isec.amov.cityfix.viewmodel.CredentialChangeViewModel
import pt.isec.amov.cityfix.viewmodel.CredentialChangeViewModelFactory
import pt.isec.amov.cityfix.viewmodel.ForgotPasswordViewModel
import pt.isec.amov.cityfix.viewmodel.ForgotPasswordViewModelFactory
import pt.isec.amov.cityfix.viewmodel.GateStatus
import pt.isec.amov.cityfix.viewmodel.ImpactViewModel
import pt.isec.amov.cityfix.viewmodel.ImpactViewModelFactory
import pt.isec.amov.cityfix.viewmodel.LocationGateViewModel
import pt.isec.amov.cityfix.viewmodel.LocationGateViewModelFactory
import pt.isec.amov.cityfix.viewmodel.LoginViewModel
import pt.isec.amov.cityfix.viewmodel.LoginViewModelFactory
import pt.isec.amov.cityfix.viewmodel.NotificationsViewModel
import pt.isec.amov.cityfix.viewmodel.NotificationsViewModelFactory
import pt.isec.amov.cityfix.viewmodel.OperatorPanelViewModel
import pt.isec.amov.cityfix.viewmodel.OperatorPanelViewModelFactory
import pt.isec.amov.cityfix.viewmodel.OperatorTicketViewModel
import pt.isec.amov.cityfix.viewmodel.OperatorTicketViewModelFactory
import pt.isec.amov.cityfix.viewmodel.PendingCitizensViewModel
import pt.isec.amov.cityfix.viewmodel.PendingCitizensViewModelFactory
import pt.isec.amov.cityfix.viewmodel.ProfileViewModel
import pt.isec.amov.cityfix.viewmodel.ProfileViewModelFactory
import pt.isec.amov.cityfix.viewmodel.RegisterViewModel
import pt.isec.amov.cityfix.viewmodel.RegisterViewModelFactory
import pt.isec.amov.cityfix.viewmodel.ReportViewModel
import pt.isec.amov.cityfix.viewmodel.ReportViewModelFactory
import pt.isec.amov.cityfix.viewmodel.Session
import pt.isec.amov.cityfix.viewmodel.SessionViewModel
import pt.isec.amov.cityfix.viewmodel.SessionViewModelFactory
import pt.isec.amov.cityfix.viewmodel.TicketDetailViewModel
import pt.isec.amov.cityfix.viewmodel.TicketDetailViewModelFactory
import pt.isec.amov.cityfix.viewmodel.accessRole
import pt.isec.amov.cityfix.viewmodel.canReport
import pt.isec.amov.cityfix.viewmodel.isSignedIn
import pt.isec.amov.cityfix.viewmodel.ticketsUser
import pt.isec.amov.cityfix.viewmodel.TicketsViewModel
import pt.isec.amov.cityfix.viewmodel.TicketsViewModelFactory

/** Logcat tag for CityFixApp (D-014). Never log coordinates with it. */
private const val TAG = "CityFixApp"

class MainActivity : ComponentActivity() {
    /** F-016: ticket a tapped notification asked to open; CityFixApp opens it and clears it. */
    private var openTicketId by mutableStateOf<String?>(null)

    /** F-022: the impact notification was tapped; CityFixApp opens the prefilled report. */
    private var openFromImpact by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // D-016: light and dark themes, so the default style is used: the system bar icons follow
        // the device's dark mode, like CityFixTheme.
        enableEdgeToEdge()
        // Like ContactsApp in Contacts_#3: the one repository lives in the Application.
        val app = application as CityFixApplication
        // Only on a fresh start: after a rotation the same intent must not open the ticket again.
        if (savedInstanceState == null) {
            openTicketId = intent.getStringExtra(EXTRA_TICKET_ID)
            openFromImpact = intent.getBooleanExtra(EXTRA_FROM_IMPACT, false)
        }
        setContent {
            CityFixTheme {
                CityFixApp(
                    app,
                    openTicketId,
                    onTicketOpened = { openTicketId = null },
                    openFromImpact = openFromImpact,
                    onImpactOpened = { openFromImpact = false },
                )
            }
        }
    }

    /** singleTop (manifest): a notification tapped while the app is open arrives here. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openTicketId = intent.getStringExtra(EXTRA_TICKET_ID)
        openFromImpact = intent.getBooleanExtra(EXTRA_FROM_IMPACT, false)
    }

    // F-022: TrackingService shows an impact as a snackbar while the app is in front, otherwise
    // as a notification.
    override fun onStart() {
        super.onStart()
        (application as CityFixApplication).appInForeground = true
    }

    override fun onStop() {
        (application as CityFixApplication).appInForeground = false
        super.onStop()
    }
}

/**
 * The app's navigation and shared state. The screens are stateless; this is their stateful half.
 * TicketsViewModel and SessionViewModel are created here, above the NavHost, so every destination
 * shares one instance of each. [app] holds the repositories the ViewModel factories receive.
 */
@Composable
private fun CityFixApp(
    app: CityFixApplication,
    openTicketId: String?,
    onTicketOpened: () -> Unit,
    openFromImpact: Boolean,
    onImpactOpened: () -> Unit,
) {
    val ticketsViewModel: TicketsViewModel =
        viewModel(factory = TicketsViewModelFactory(app.ticketsRepository, null))
    val tickets by ticketsViewModel.tickets.collectAsStateWithLifecycle()
    val stats by ticketsViewModel.stats.collectAsStateWithLifecycle()

    val sessionViewModel: SessionViewModel =
        viewModel(factory = SessionViewModelFactory(app.authRepository, app.usersRepository))
    val session by sessionViewModel.session.collectAsStateWithLifecycle()
    val role = session.accessRole

    // The tickets follow the session: an active citizen's or an operator's own view, the public
    // tickets for everyone else. Also re-run after rotation and process death; setUser keeps the
    // running listener when the role and id did not change, so the list is not blanked.
    val ticketsUser = session.ticketsUser
    LaunchedEffect(ticketsUser) {
        ticketsViewModel.setUser(ticketsUser)
    }

    // Nobody gets the expand toggle (D-009 Gate Q7). Since F-011 an active citizen's list holds
    // their own tickets plus the public ones and an operator's holds all, but the list cards
    // render only the public fields for every role, so an expanded card would add only the date;
    // brief I.1.B lists category, situation and thumbnail, and I.2.D gives citizens the anonymous
    // features. A citizen's own view is the participant detail and My tickets (F-014); the
    // operator's is D-013's operations panel (coordinates, author, history; brief I.3.E).
    // The flag and the `canExpandCards && isExpanded` below stay for that later decision.
    val canExpandCards = false
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    // Above the NavHost: the report destination is popped while the snackbar is showing.
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val reportSubmittedMessage = stringResource(R.string.report_submitted)
    val reportJoinedMessage = stringResource(R.string.report_joined)
    val reportAlreadyFollowingMessage = stringResource(R.string.report_already_following)
    val citizenActivatedMessage = stringResource(R.string.citizen_approval_activated)
    val loadingDescription = stringResource(R.string.cd_loading_session)

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val screen = backStackEntry?.destination.toTopBarScreen()
    // previousBackStackEntry is not Compose state: read it here, in the same scope as
    // backStackEntry, so a back-stack change recomposes this line too (D-005 review N4).
    val canNavigateBack = navController.previousBackStackEntry != null

    // Impact detection (F-015, brief I.2.F): only for an active citizen who turned it on. Since
    // F-022 the sensor runs in TrackingService (started below); this only shows its suggestions.
    val impactViewModel: ImpactViewModel = viewModel(factory = ImpactViewModelFactory(app.impacts))
    val impactDetected by impactViewModel.impactDetected.collectAsStateWithLifecycle()
    val detectImpacts = role == AccessRole.CITIZEN &&
        (session.ticketsUser as? Citizen)?.impactDetection == true
    val impactMessage = stringResource(R.string.impact_suggestion)
    val impactAction = stringResource(R.string.impact_report_action)
    LaunchedEffect(impactDetected) {
        if (impactDetected) {
            impactViewModel.onSuggestionShown()
            // Not while the user is already reporting (Q9); the 30 s cooldown still starts, so the
            // same bump is not suggested later. Never opens a report on its own.
            if (screen != TopBarScreen.REPORT) {
                snackbarScope.launch {
                    val result = snackbarHostState.showSnackbar(
                        impactMessage, impactAction, withDismissAction = true, duration = SnackbarDuration.Long,
                    )
                    // If the user opened the form with "+" meanwhile, keep that one (Q9).
                    val onReport = navController.currentDestination?.hasRoute<ReportTicketRoute>() == true
                    if (result == SnackbarResult.ActionPerformed && !onReport) {
                        navController.navigate(ReportTicketRoute(fromImpact = true)) { launchSingleTop = true }
                    }
                }
            }
        }
    }

    // The role's home with the back stack cleared: used by the redirect and every destination guard.
    val goHome = {
        navController.navigate(homeFor(role)) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }
    // The last role the redirect (after the NavHost below) has handled. Saveable: null means a
    // fresh start; a restored value means process recreation, whose back stack is kept.
    var handledRole by rememberSaveable { mutableStateOf<AccessRole?>(null) }
    // True while the Register screen creates an account (D-010). Sign-up signs the new user in at
    // once, so without this hold the redirect would clear the Register entry, and cancel its
    // ViewModel, before the NIF transaction ends. Saveable, so a rotation keeps the hold.
    var holdRedirect by rememberSaveable { mutableStateOf(false) }

    // Every Log out path (top bar, account setup, account problem). The list is cleared first,
    // synchronously, so the next frame never shows the previous user's tickets to an anonymous
    // user; the LaunchedEffect(ticketsUser) that follows with null is then a no-op.
    // F-016 (review S1): the previous user's notifications are removed from the tray too.
    val notificationsViewModel: NotificationsViewModel =
        viewModel(factory = NotificationsViewModelFactory(app))
    val logOut = {
        ticketsViewModel.setUser(null)
        notificationsViewModel.onLoggedOut()
        sessionViewModel.signOut()
    }

    val onAuthScreen = screen == TopBarScreen.LOGIN || screen == TopBarScreen.FORGOT_PASSWORD ||
        screen == TopBarScreen.ACCOUNT_SETUP || screen == TopBarScreen.REGISTER
    val accountAction = when {
        // The new account is briefly signed in while it registers: no Log out there.
        screen == TopBarScreen.REGISTER -> AccountAction.NONE
        session.isSignedIn -> AccountAction.LOG_OUT
        role == AccessRole.ANONYMOUS && !onAuthScreen -> AccountAction.LOG_IN
        else -> AccountAction.NONE
    }

    // D-014: whether precise location is granted, for the operator's 20 m watch below. Read again
    // on every resume: the user may change it in Settings, and the start-up permission dialog
    // pauses and resumes the activity, so its answer is picked up here too.
    val context = LocalContext.current
    var hasPrecise by remember { mutableStateOf(hasPreciseLocationPermission(context)) }
    LifecycleResumeEffect(Unit) {
        hasPrecise = hasPreciseLocationPermission(context)
        onPauseOrDispose { }
    }

    // Location is requested when the app starts, as in Contacts_#3 (P10). The answer goes to the
    // 10 km gate; the report form checks the permission again when it opens.
    val gateViewModel: LocationGateViewModel =
        viewModel(factory = LocationGateViewModelFactory(app.locationRepository))
    val gateState by gateViewModel.uiState.collectAsStateWithLifecycle()
    // F-016: the notification request below waits for this answer.
    var locationAnswered by remember { mutableStateOf(false) }
    // F-016: saveable, so a rotation does not ask (or show the denied snackbar) again.
    var askedNotifications by rememberSaveable { mutableStateOf(false) }
    val locationPermissionRequest = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        gateViewModel.onPermissionResult(permissions.values.any { it })
        locationAnswered = true
    }
    LaunchedEffect(Unit) {
        locationPermissionRequest.launch(LOCATION_PERMISSIONS)
    }

    // F-016 (Gate 1 Q6): ask for notifications when a citizen or operator session starts, as P11
    // asks with RequestPermission (Android 13+). Only after the location answer, because Android
    // shows one permission request at a time. Asked once per session; denied -> a snackbar.
    val notificationsDeniedMessage = stringResource(R.string.notifications_denied)
    val notificationPermissionRequest = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            snackbarScope.launch { snackbarHostState.showSnackbar(notificationsDeniedMessage) }
        }
    }
    LaunchedEffect(role, locationAnswered) {
        if (role != AccessRole.CITIZEN && role != AccessRole.OPERATOR) {
            askedNotifications = false
        } else if (!askedNotifications && locationAnswered &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            askedNotifications = true
            notificationPermissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Brief II.3 (D-014) and I.2.F, F-022: the operator's 20 m geofence and the citizen's impact
    // detection run in TrackingService, a location foreground service that keeps working with the
    // app in the background or closed. Started here, while the app is visible (Android 12+), and
    // restarted when the operator or the impact choice changes; Log out (no operator, no
    // detection) or no precise location stops it. Not while Loading, so an app start does not
    // stop a running service only to start it again.
    val operator = session.ticketsUser as? Operator
    val lifecycleOwner = LocalLifecycleOwner.current
    val sessionLoading = session == Session.Loading
    LaunchedEffect(operator?.id, detectImpacts, hasPrecise, sessionLoading) {
        if (sessionLoading) return@LaunchedEffect
        if (hasPrecise && (operator != null || detectImpacts)) {
            TrackingService.start(context, operator?.id, detectImpacts)
        } else {
            if (operator != null || detectImpacts) {
                // Q10 (D-014): no UI; the user keeps working without the geofence or impacts.
                Log.i(TAG, "No precise location: tracking service not started")
            }
            TrackingService.stop(context)
        }
    }

    // F-016: notifications follow the session's tickets user (active citizen or operator). Not
    // while Loading (review S3): the user is not known yet, and a null there would cancel and
    // re-enqueue the worker on every app start.
    val isLoading = session == Session.Loading
    LaunchedEffect(session.ticketsUser?.id, isLoading) {
        if (!isLoading) notificationsViewModel.onUserChanged(session.ticketsUser)
    }
    // While the app is in the foreground, check the user's tickets for changes (like the
    // proximity watch below); a new user or Log out cancels it. Anonymous users get nothing.
    val notifiedUser = session.ticketsUser
    LaunchedEffect(notifiedUser?.id) {
        if (notifiedUser != null) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                notificationsViewModel.watch(notifiedUser)
            }
        }
    }

    // Brief I.1: only the anonymous user is limited to 10 km around Pr. Republica; registered
    // citizens and operators use the app wherever they are. Never while the session is loading,
    // and not on the login screens, which the gate's "Log in" opens.
    val showGate = role == AccessRole.ANONYMOUS && !onAuthScreen &&
        gateState.status != GateStatus.INSIDE

    // D-016: the report button is on the Dashboard tab only.
    val showReportFab = session.canReport && !showGate && screen == TopBarScreen.DASHBOARD

    // D-016: bottom bar with the role's tabs (RoleAccess.kt). Hidden on the login flow, on Edit
    // profile and its credential screens (Gate 1), and while the gate covers the app.
    // D-020: the selected tab is the tab last shown or tapped (lastTab); a screen outside every
    // tab (detail, report) keeps it. Setting it on a tap (openTab) makes a restored detail
    // highlight the tab it was restored into. Not derived from NavController.currentBackStack,
    // which is a restricted API for apps (lint RestrictedApi).
    val tabs = bottomTabsFor(role)
    val currentTab = tabs.firstOrNull { screen in it.screens }
    var lastTab by rememberSaveable { mutableStateOf<BottomTab?>(null) }
    SideEffect { if (currentTab != null) lastTab = currentTab }
    val selectedTab = currentTab ?: lastTab?.takeIf { it in tabs }
    val showBottomBar = tabs.isNotEmpty() && !showGate && !onAuthScreen && screen !in setOf(
        TopBarScreen.ACCOUNT_PROBLEM,
        TopBarScreen.EDIT_PROFILE,
        TopBarScreen.CHANGE_PASSWORD,
        TopBarScreen.CHANGE_EMAIL,
    )
    val islandShown = showBottomBar && session != Session.Loading
    // What the island covers at the bottom of the screen (system bar + margin + island), plus a
    // 16dp gap, plus the report button on the Dashboard: the end padding of scrolling screens.
    val navigationBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomBarPadding = if (islandShown) {
        navigationBarBottom + IslandClearance + Dimens.lg +
            if (showReportFab) Dimens.reportButtonHeight + Dimens.lg else 0.dp
    } else {
        0.dp
    }
    // D-020: one event per tap on the selected tab while on its own screen. Only the screen on
    // display collects it (ScrollToTopOnReselect), so it is the one that scrolls. A flow, not a
    // counter: a LaunchedEffect keyed on a counter would also scroll when the screen comes back.
    val tabReselects = remember { MutableSharedFlow<Unit>(extraBufferCapacity = 1) }
    // Usual bottom-navigation pattern: one entry per tab above the home, each tab's own back
    // stack saved and restored when the user comes back to it. D-020, tapping the selected tab:
    // - on its root: only scrolls that screen to the top (tabReselects), no navigation;
    // - deeper (a detail): pops back to its root.
    // The first tab's root is the role's home (a pending citizen's is Registration pending, not
    // the Dashboard route). popBackStack returns false when the root is not on the stack, and
    // then the usual navigate runs.
    val openTab = { tab: BottomTab ->
        val root = if (tab == tabs.first()) homeFor(role) else tab.route
        val onRoot = navController.currentDestination?.hasRoute(root::class) == true
        when {
            tab == selectedTab && onRoot -> tabReselects.tryEmit(Unit)
            tab == selectedTab && navController.popBackStack(root, inclusive = false) -> Unit
            else -> navController.navigate(tab.route) {
                popUpTo(homeFor(role)) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
        lastTab = tab
    }

    // The gate is drawn over the app instead of replacing it, so the NavHost (and its graph)
    // exists when the gate's "Log in" navigates.
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            // While the gate covers the app, TalkBack must not reach the screen behind it.
            modifier = if (showGate) {
                Modifier.fillMaxSize().clearAndSetSemantics { }
            } else {
                Modifier.fillMaxSize()
            },
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                MainTopAppBar(
                    screen = screen,
                    // While the gate covers the app, no bar control stays reachable behind it
                    // (keyboard or switch focus); the gate has its own Log in.
                    // Nor while Register holds the redirect: leaving it mid-registration is not allowed.
                    // A tab's own screen has no back arrow (D-016); system Back still goes home.
                    canNavigateBack = canNavigateBack && !showGate && !holdRedirect &&
                        screen !in tabRootScreens,
                    isExpanded = isExpanded,
                    showExpandToggle = canExpandCards,
                    accountAction = if (showGate) AccountAction.NONE else accountAction,
                    onBackClick = { navController.navigateUp() },
                    onToggleExpandedClick = { isExpanded = !isExpanded },
                    onAccountClick = {
                        if (accountAction == AccountAction.LOG_OUT) {
                            // The session becomes Anonymous and the redirect goes to the dashboard.
                            logOut()
                        } else {
                            navController.navigate(LoginRoute) { launchSingleTop = true }
                        }
                    },
                )
            },
            // Report an issue (D-015, Q5): an active citizen on the dashboard (D-016). The icon has no description. The label is also set as the button's
            // contentDescription: on the device the FAB's Text did not reach the accessibility
            // tree (uiautomator and TalkBack saw an unnamed button), so the name is explicit.
            floatingActionButton = {
                if (showReportFab) {
                    val reportLabel = stringResource(R.string.top_bar_title_report)
                    // D-016: indigo with white text in both themes (primaryContainer), 16dp
                    // corners, 52dp high; the label style is the theme's labelLarge (weight 500).
                    ExtendedFloatingActionButton(
                        onClick = { navController.navigate(ReportTicketRoute()) { launchSingleTop = true } },
                        // Above the floating island (the Scaffold already adds the system bar).
                        modifier = Modifier
                            .padding(bottom = if (islandShown) IslandClearance else 0.dp)
                            .height(Dimens.reportButtonHeight)
                            .semantics { contentDescription = reportLabel },
                        shape = MaterialTheme.shapes.medium,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        text = { Text(reportLabel) },
                    )
                }
            },
            snackbarHost = {
                SnackbarHost(
                    snackbarHostState,
                    modifier = Modifier.padding(bottom = if (islandShown) IslandClearance else 0.dp),
                )
            },
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
            // With the island the content goes down to the bottom of the screen and scrolls under
            // it; the screens add LocalBottomBarPadding at the end of their scroll instead.
            val layoutDirection = LocalLayoutDirection.current
            val contentPadding = if (islandShown) {
                PaddingValues(
                    start = innerPadding.calculateStartPadding(layoutDirection),
                    top = innerPadding.calculateTopPadding(),
                    end = innerPadding.calculateEndPadding(layoutDirection),
                    bottom = 0.dp,
                )
            } else {
                innerPadding
            }
            CompositionLocalProvider(LocalBottomBarPadding provides bottomBarPadding) {
            NavHost(
                navController = navController,
                startDestination = DashboardRoute,
                // consumeWindowInsets: ReportContent's imePadding() must not add these insets again.
                modifier = Modifier
                    .padding(contentPadding)
                    .consumeWindowInsets(contentPadding),
            ) {
                composable<DashboardRoute> {
                    val scrollState = rememberScrollState()
                    ScrollToTopOnReselect(tabReselects) { scrollState.animateScrollTo(0) }
                    DashboardScreen(
                        stats = stats,
                        onSeeTicketsClick = {
                            navController.navigate(TicketListRoute) { launchSingleTop = true }
                        },
                        scrollState = scrollState,
                    )
                }
                composable<ActiveTicketsRoute> {
                    // The first active tickets of the same list; "See more" opens the full list.
                    val listState = rememberLazyListState()
                    ScrollToTopOnReselect(tabReselects) { listState.animateScrollToItem(0) }
                    ActiveTicketsScreen(
                        tickets = tickets.filter { it.isActive },
                        onTicketClick = { id ->
                            navController.navigate(TicketDetailRoute(id)) { launchSingleTop = true }
                        },
                        onSeeMoreClick = {
                            navController.navigate(TicketListRoute) { launchSingleTop = true }
                        },
                        listState = listState,
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
                    val ticket = tickets.firstOrNull { it.id == id }
                    val user = session.ticketsUser
                    // Decision 0007: the view depends on who is looking. A citizen who created or
                    // follows the ticket gets the full detail with the thread (F-014); anonymous,
                    // pending and other citizens get the public fields and never read messages.
                    val audience = detailAudience(user, ticket)
                    if (user == null || audience == DetailAudience.PUBLIC) {
                        ShowScreen(ticket = ticket, audience = audience, now = System.currentTimeMillis())
                    } else if (user is Operator) {
                        // D-013 Q1: the operator's own view of the same route (contacts, actions).
                        // Inside the branch: scoped to this entry, so the messages and profile
                        // listeners end with it, and never created for a citizen.
                        // Review B1: keyed on the user, so an entry restored from a saved tab
                        // stack after a logout never hands this user the previous one's ViewModel.
                        val operatorViewModel: OperatorTicketViewModel = viewModel(
                            key = "operator-ticket-${user.id}",
                            factory = OperatorTicketViewModelFactory(
                                app.ticketsRepository,
                                app.usersRepository,
                                app.attachmentStorage,
                                id,
                                user,
                            ),
                        )
                        OperatorTicketScreen(ticket = ticket, operator = user, viewModel = operatorViewModel)
                    } else {
                        // Inside the destination: scoped to this entry, so leaving the ticket
                        // removes its messages listener.
                        // Review B1: keyed on the user, as the operator's view above.
                        val detailViewModel: TicketDetailViewModel = viewModel(
                            key = "ticket-detail-${user.id}",
                            factory = TicketDetailViewModelFactory(app.ticketsRepository, id, user),
                        )
                        TicketDetailScreen(ticket = ticket, user = user, viewModel = detailViewModel)
                    }
                }
                composable<LoginRoute> {
                    val loginViewModel: LoginViewModel =
                        viewModel(factory = LoginViewModelFactory(app.authRepository))
                    LoginScreen(
                        viewModel = loginViewModel,
                        onForgotPassword = { email ->
                            navController.navigate(ForgotPasswordRoute(email)) { launchSingleTop = true }
                        },
                        onCreateAccount = {
                            navController.navigate(RegisterRoute) { launchSingleTop = true }
                        },
                    )
                }
                composable<RegisterRoute> {
                    // No RequireAccess: reached only from Login, which only anonymous users see, and
                    // the user it creates is signed in while this screen is still showing.
                    val registerViewModel: RegisterViewModel =
                        viewModel(factory = RegisterViewModelFactory(app.usersRepository, app.locationRepository, app.attachmentStorage))
                    RegisterScreen(
                        viewModel = registerViewModel,
                        accountSignedIn = role != AccessRole.ANONYMOUS,
                        onBusyChange = { holdRedirect = it },
                    )
                }
                composable<ForgotPasswordRoute> { entry ->
                    val email = entry.toRoute<ForgotPasswordRoute>().email
                    val forgotViewModel: ForgotPasswordViewModel =
                        viewModel(factory = ForgotPasswordViewModelFactory(app.authRepository, email))
                    ForgotPasswordScreen(viewModel = forgotViewModel)
                }
                composable<ReportTicketRoute> { entry ->
                    val fromImpact = entry.toRoute<ReportTicketRoute>().fromImpact
                    // Active citizens only (brief I.2.E); anyone else is sent to their home.
                    RequireAccess(role = role, allowed = setOf(AccessRole.CITIZEN), onDenied = goHome) {
                        // The guard lets only an active citizen in, so the session holds their profile.
                        val citizenId = session.ticketsUser?.id ?: return@RequireAccess
                        // Inside the destination: scoped to this entry, so a finished form is cleared.
                        // Review B1: keyed on the citizen, so a restored form is never another user's draft.
                        val reportViewModel: ReportViewModel =
                            viewModel(
                                key = "report-$citizenId",
                                factory = ReportViewModelFactory(
                                    app.ticketsRepository,
                                    app.attachmentStorage,
                                    app.locationRepository,
                                    citizenId,
                                    fromImpact,
                                ),
                            )
                        ReportScreen(
                            viewModel = reportViewModel,
                            // Like saveContact { popBackStack() } in Contacts_#3, without the refresh:
                            // the tickets listener shows the new ticket from the local cache.
                            // No id here: never navigate to the new ticket, a PRIVATE one is not
                            // visible (decision 0006).
                            // One message per outcome of the 10 m grouping (F-012).
                            onSubmitted = { outcome ->
                                val message = when (outcome) {
                                    ReportOutcome.CREATED -> reportSubmittedMessage
                                    ReportOutcome.JOINED -> reportJoinedMessage
                                    ReportOutcome.ALREADY_FOLLOWING -> reportAlreadyFollowingMessage
                                }
                                snackbarScope.launch {
                                    snackbarHostState.showSnackbar(message)
                                }
                                navController.navigateUp()
                            },
                        )
                    }
                }
                composable<AccountSetupRoute> {
                    RequireAccess(role = role, allowed = setOf(AccessRole.SETUP), onDenied = goHome) {
                        val setupViewModel: AccountSetupViewModel =
                            viewModel(factory = AccountSetupViewModelFactory(app.authRepository))
                        AccountSetupScreen(viewModel = setupViewModel, onLogOut = logOut)
                    }
                }
                composable<PendingValidationRoute> {
                    RequireAccess(role = role, allowed = setOf(AccessRole.PENDING_CITIZEN), onDenied = goHome) {
                        PendingValidationScreen(
                            onBrowseClick = {
                                navController.navigate(DashboardRoute) { launchSingleTop = true }
                            },
                        )
                    }
                }
                composable<AccountProblemRoute> {
                    RequireAccess(role = role, allowed = setOf(AccessRole.ACCOUNT_PROBLEM), onDenied = goHome) {
                        AccountProblemScreen(
                            canRetry = session == Session.ProfileUnavailable,
                            onRetry = sessionViewModel::retry,
                            onLogOut = logOut,
                        )
                    }
                }
                // Profile and credential changes: any signed-in user with a profile.
                composable<ProfileRoute> {
                    RequireAccess(
                        role = role,
                        allowed = setOf(AccessRole.CITIZEN, AccessRole.PENDING_CITIZEN, AccessRole.OPERATOR),
                        onDenied = goHome,
                    ) {
                        val user = (session as? Session.SignedIn)?.user ?: return@RequireAccess
                        // The overview only reads the ViewModel's state (D-016). Review B1: keyed on
                        // the user, so a Profile restored from a saved tab stack after a logout
                        // builds a new ViewModel instead of showing the previous user's data.
                        val profileViewModel: ProfileViewModel = viewModel(
                            key = "profile-${user.id}",
                            factory = ProfileViewModelFactory(
                                user.id,
                                app.usersRepository,
                                app.authRepository,
                                app.attachmentStorage,
                                app.impactPrefs,
                            ),
                        )
                        // As on the former My tickets (F-014): an active citizen's list already
                        // holds their own tickets; keep the ones they created or follow. A pending
                        // citizen has none yet; an operator gets no tickets section.
                        val myTickets = when (role) {
                            AccessRole.CITIZEN -> tickets.filter { it.isParticipant(user.id) }
                            AccessRole.PENDING_CITIZEN -> emptyList()
                            else -> null
                        }
                        val listState = rememberLazyListState()
                        ScrollToTopOnReselect(tabReselects) { listState.animateScrollToItem(0) }
                        ProfileOverviewScreen(
                            viewModel = profileViewModel,
                            myTickets = myTickets,
                            now = System.currentTimeMillis(),
                            onEditDetails = {
                                navController.navigate(EditProfileRoute) { launchSingleTop = true }
                            },
                            onTicketClick = { id ->
                                navController.navigate(TicketDetailRoute(id)) { launchSingleTop = true }
                            },
                            listState = listState,
                        )
                    }
                }
                composable<EditProfileRoute> {
                    RequireAccess(
                        role = role,
                        allowed = setOf(AccessRole.CITIZEN, AccessRole.PENDING_CITIZEN, AccessRole.OPERATOR),
                        onDenied = goHome,
                    ) {
                        // The guard lets only a signed-in user in, so the session holds their profile.
                        val user = (session as? Session.SignedIn)?.user ?: return@RequireAccess
                        val profileViewModel: ProfileViewModel = viewModel(
                            key = "profile-${user.id}",
                            factory = ProfileViewModelFactory(
                                user.id,
                                app.usersRepository,
                                app.authRepository,
                                app.attachmentStorage,
                                app.impactPrefs,
                            ),
                        )
                        ProfileScreen(
                            viewModel = profileViewModel,
                            onChangeEmail = {
                                navController.navigate(ChangeEmailRoute) { launchSingleTop = true }
                            },
                            onChangePassword = {
                                navController.navigate(ChangePasswordRoute) { launchSingleTop = true }
                            },
                            onLogOut = logOut,
                        )
                    }
                }
                composable<ChangePasswordRoute> {
                    RequireAccess(
                        role = role,
                        allowed = setOf(AccessRole.CITIZEN, AccessRole.PENDING_CITIZEN, AccessRole.OPERATOR),
                        onDenied = goHome,
                    ) {
                        val user = (session as? Session.SignedIn)?.user ?: return@RequireAccess
                        val credentialViewModel: CredentialChangeViewModel = viewModel(
                            key = "credential-${user.id}",
                            factory = CredentialChangeViewModelFactory(
                                CredentialChange.PASSWORD,
                                user is Operator,
                                app.authRepository,
                                app.usersRepository,
                            ),
                        )
                        ChangeCredentialScreen(
                            viewModel = credentialViewModel,
                            onDone = { navController.navigateUp() },
                        )
                    }
                }
                composable<ChangeEmailRoute> {
                    RequireAccess(
                        role = role,
                        allowed = setOf(AccessRole.CITIZEN, AccessRole.PENDING_CITIZEN, AccessRole.OPERATOR),
                        onDenied = goHome,
                    ) {
                        val user = (session as? Session.SignedIn)?.user ?: return@RequireAccess
                        val credentialViewModel: CredentialChangeViewModel = viewModel(
                            key = "credential-${user.id}",
                            factory = CredentialChangeViewModelFactory(
                                CredentialChange.EMAIL,
                                user is Operator,
                                app.authRepository,
                                app.usersRepository,
                            ),
                        )
                        ChangeCredentialScreen(
                            viewModel = credentialViewModel,
                            onDone = { navController.navigateUp() },
                        )
                    }
                }
                composable<OperatorPanelRoute> {
                    RequireAccess(role = role, allowed = setOf(AccessRole.OPERATOR), onDenied = goHome) {
                        // Inside the guard: scoped to this entry, so the categories listener ends with it.
                        val panelViewModel: OperatorPanelViewModel =
                            viewModel(factory = OperatorPanelViewModelFactory(app.categoriesRepository))
                        val listState = rememberLazyListState()
                        ScrollToTopOnReselect(tabReselects) { listState.animateScrollToItem(0) }
                        // The shared list is allTickets() for an operator (TicketsViewModel.setUser).
                        OperatorPanelScreen(
                            tickets = tickets,
                            viewModel = panelViewModel,
                            onTicketClick = { id ->
                                navController.navigate(TicketDetailRoute(id)) { launchSingleTop = true }
                            },
                            listState = listState,
                        )
                    }
                }
                composable<ApprovalsRoute> {
                    RequireAccess(role = role, allowed = setOf(AccessRole.OPERATOR), onDenied = goHome) {
                        // Inside the guard: scoped to this entry, so the users listener ends with it.
                        val approvalsViewModel: PendingCitizensViewModel =
                            viewModel(factory = PendingCitizensViewModelFactory(app.usersRepository))
                        val listState = rememberLazyListState()
                        ScrollToTopOnReselect(tabReselects) { listState.animateScrollToItem(0) }
                        PendingCitizensScreen(
                            viewModel = approvalsViewModel,
                            onCitizenClick = { id ->
                                navController.navigate(PendingCitizenRoute(id)) { launchSingleTop = true }
                            },
                            listState = listState,
                        )
                    }
                }
                composable<PendingCitizenRoute> { entry ->
                    RequireAccess(role = role, allowed = setOf(AccessRole.OPERATOR), onDenied = goHome) {
                        // The guard lets only an operator in, so the session holds their profile.
                        val operatorId = session.ticketsUser?.id ?: return@RequireAccess
                        val citizenId = entry.toRoute<PendingCitizenRoute>().citizenId
                        // Inside the guard: scoped to this entry, so the citizen listener ends with it.
                        // Review B1: keyed on the operator, so the approval is never the previous one's.
                        val approvalViewModel: CitizenApprovalViewModel = viewModel(
                            key = "approval-$operatorId",
                            factory = CitizenApprovalViewModelFactory(citizenId, operatorId, app.usersRepository),
                        )
                        CitizenApprovalScreen(
                            viewModel = approvalViewModel,
                            // As the report's onSubmitted: the write is queued, so confirm and go back.
                            onActivated = {
                                snackbarScope.launch {
                                    snackbarHostState.showSnackbar(citizenActivatedMessage)
                                }
                                navController.navigateUp()
                            },
                        )
                    }
                }
                composable<CategoriesRoute> {
                    RequireAccess(role = role, allowed = setOf(AccessRole.OPERATOR), onDenied = goHome) {
                        // Inside the guard: the only category writes come from this ViewModel.
                        val categoriesViewModel: CategoriesViewModel =
                            viewModel(factory = CategoriesViewModelFactory(app.categoriesRepository))
                        val listState = rememberLazyListState()
                        ScrollToTopOnReselect(tabReselects) { listState.animateScrollToItem(0) }
                        CategoriesScreen(viewModel = categoriesViewModel, listState = listState)
                    }
                }
            }
            }

            // Redirect to the role's home when the role changes (login, logout, activation) and on a
            // fresh start with a stored session, so an operator or a setup user does not stay on the
            // dashboard. Placed after the NavHost, so its graph is set before goHome() runs; never
            // reached while the session is Loading. Rotation does not change the role, and process
            // recreation restores handledRole, so neither resets the back stack.
            // While Register holds the redirect, nothing moves: on success the hold is released and
            // the new role (SETUP) opens Account setup; on failure the account is deleted and the
            // hold lasts until the role is ANONYMOUS again (= handledRole), so the form stays with
            // its error.
            LaunchedEffect(role, holdRedirect) {
                if (holdRedirect) return@LaunchedEffect
                if (role != handledRole) {
                    // null = fresh start: the NavHost is on the dashboard, so only other homes navigate.
                    if (handledRole != null || homeFor(role) != DashboardRoute) goHome()
                    handledRole = role
                }
            }

            // F-016: a tapped notification opens its ticket. After the redirect above, so the
            // role's home is set first and the ticket goes on top of it; never while Loading.
            LaunchedEffect(openTicketId, role) {
                if (openTicketId != null) {
                    navController.navigate(TicketDetailRoute(openTicketId)) { launchSingleTop = true }
                    onTicketOpened()
                }
            }
            // F-022: the impact notification opens the prefilled report, as the snackbar's action.
            LaunchedEffect(openFromImpact, role) {
                if (openFromImpact && role == AccessRole.CITIZEN) {
                    navController.navigate(ReportTicketRoute(fromImpact = true)) { launchSingleTop = true }
                    onImpactOpened()
                }
            }
        }

        // D-016: the bottom bar is a floating island drawn over the content (not the Scaffold's
        // bottomBar slot), so lists pass under it; it handles the system bar insets itself.
        if (islandShown) {
            MainBottomBar(
                tabs = tabs,
                selected = selectedTab,
                onTabClick = openTab,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        if (showGate) {
            // Surface: an opaque background that also takes the touches meant for the app behind.
            Surface(modifier = Modifier.fillMaxSize()) {
                LocationGateScreen(
                    viewModel = gateViewModel,
                    onTryAgainClick = { locationPermissionRequest.launch(LOCATION_PERMISSIONS) },
                    onLoginClick = {
                        navController.navigate(LoginRoute) { launchSingleTop = true }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                )
            }
        }
    }
}

/**
 * D-020: runs [scrollToTop] each time the selected tab is tapped again on its own screen. Placed
 * in a tab's root destination; it collects only while that entry is resumed, so the screen that
 * fades out during a tab change does not scroll too (review N1).
 */
@Composable
private fun ScrollToTopOnReselect(reselects: Flow<Unit>, scrollToTop: suspend () -> Unit) {
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(reselects, owner) {
        owner.repeatOnLifecycle(Lifecycle.State.RESUMED) { reselects.collect { scrollToTop() } }
    }
}

/** Maps the current destination to the bar's enum. Add new routes here: `else` will not warn. */
private fun NavDestination?.toTopBarScreen(): TopBarScreen = when {
    this == null -> TopBarScreen.DASHBOARD
    hasRoute<ActiveTicketsRoute>() -> TopBarScreen.ACTIVE_TICKETS
    hasRoute<TicketListRoute>() -> TopBarScreen.LIST
    hasRoute<TicketDetailRoute>() -> TopBarScreen.DETAIL
    hasRoute<ReportTicketRoute>() -> TopBarScreen.REPORT
    hasRoute<LoginRoute>() -> TopBarScreen.LOGIN
    hasRoute<ForgotPasswordRoute>() -> TopBarScreen.FORGOT_PASSWORD
    hasRoute<PendingValidationRoute>() -> TopBarScreen.PENDING_VALIDATION
    hasRoute<OperatorPanelRoute>() -> TopBarScreen.OPERATOR_PANEL
    hasRoute<ProfileRoute>() -> TopBarScreen.PROFILE
    hasRoute<EditProfileRoute>() -> TopBarScreen.EDIT_PROFILE
    hasRoute<ApprovalsRoute>() -> TopBarScreen.APPROVALS
    hasRoute<CategoriesRoute>() -> TopBarScreen.CATEGORIES
    hasRoute<AccountProblemRoute>() -> TopBarScreen.ACCOUNT_PROBLEM
    hasRoute<AccountSetupRoute>() -> TopBarScreen.ACCOUNT_SETUP
    hasRoute<RegisterRoute>() -> TopBarScreen.REGISTER
    hasRoute<ChangePasswordRoute>() -> TopBarScreen.CHANGE_PASSWORD
    hasRoute<ChangeEmailRoute>() -> TopBarScreen.CHANGE_EMAIL
    hasRoute<PendingCitizenRoute>() -> TopBarScreen.PENDING_CITIZEN
    else -> TopBarScreen.DASHBOARD
}

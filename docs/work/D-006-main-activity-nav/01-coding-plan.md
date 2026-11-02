# D-006: Coding plan

## Files
| Path | Change | Why |
| --- | --- | --- |
| `gradle/libs.versions.toml` | edit | `navigationCompose` version and three libraries |
| `app/build.gradle.kts` | edit | Three `implementation` lines |
| `app/src/main/java/pt/isec/amov/cityfix/MainActivity.kt` | edit | Replace `Greeting` with `CityFixApp` |
| `app/src/main/java/pt/isec/amov/cityfix/ui/screens/ReportScreen.kt` | edit | `collectAsStateWithLifecycle` (F-006 contract) |
| `app/src/main/res/values/strings.xml` | edit | Remove `welcome_message`, add `report_submitted` |
| `app/src/main/res/values-pt/strings.xml` | edit | The same |
| `app/src/main/java/pt/isec/amov/cityfix/ui/navigation/MainTopAppBar.kt` | edit **only if** slice 6's check needs it | Cutout inset for the bar's controls |

## Interfaces

### Version catalog
```toml
# [versions]
navigationCompose = "2.9.8"

# [libraries]
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycleRuntimeKtx" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleRuntimeKtx" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
```

### `app/build.gradle.kts`
```kotlin
implementation(libs.androidx.lifecycle.runtime.compose)
implementation(libs.androidx.lifecycle.viewmodel.compose)
implementation(libs.androidx.navigation.compose)
```

### Strings
```xml
<!-- values/strings.xml, in the "Report form" group -->
<string name="report_submitted">Report submitted.</string>
<!-- values-pt/strings.xml -->
<string name="report_submitted">Ocorrência enviada.</string>
```

### `MainActivity.kt`
`onCreate` keeps its `enableEdgeToEdge(...)` call and comment. The `setContent` body becomes:

```kotlin
setContent {
    CityFixTheme {
        CityFixApp()
    }
}
```

End state of `CityFixApp` (slice 5 and 6 complete):

```kotlin
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
        contentWindowInsets = WindowInsets.safeDrawing,
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
```

Import notes:
- `hasRoute`: `androidx.navigation.NavDestination.Companion.hasRoute`.
- `viewModel`: `androidx.lifecycle.viewmodel.compose.viewModel`.
- `collectAsStateWithLifecycle`: `androidx.lifecycle.compose.collectAsStateWithLifecycle`.
- `composable`, `NavHost`, `rememberNavController`, `currentBackStackEntryAsState`: `androidx.navigation.compose`.
- `toRoute`: `androidx.navigation.toRoute`.
- `android.graphics.Color` stays for `SystemBarStyle`.

## Steps
For every slice: `./gradlew assembleDebug` and `./gradlew lint` pass (0 errors), and the baseline is 13 warnings. Do not stage `.idea/`.

### Slice 1: dependencies
- Catalog and `app/build.gradle.kts` as above. Nothing uses them yet.
- Commit: `feat(D-006): add navigation and lifecycle compose dependencies`.
- Verify with `./gradlew :app:dependencies --configuration debugRuntimeClasspath`. Record in `state.md`:

| Library | Must be |
| --- | --- |
| `org.jetbrains.kotlinx:kotlinx-serialization-core` | 1.11.0, **stop if higher** |
| `androidx.compose.runtime:runtime` | 1.11.0, **stop if higher** |
| `androidx.compose.ui:ui` | expected 1.11.0 (was 1.10.4), **stop if higher** |
| `androidx.compose.foundation:foundation`, `androidx.compose.material3:material3` | record; expected 1.10.4, 1.4.0 |
| `androidx.navigation:navigation-compose` | 2.9.8 |
| `androidx.lifecycle:lifecycle-viewmodel-compose`, `lifecycle-runtime-compose` | 2.11.0 |
| `kotlin-stdlib` | record; was 2.3.20 |

- Lint: one new `NewerVersionAvailable` for navigation is expected and accepted. Record the count.

### Slice 2: lifecycle-aware collection in the report form
- In `ReportScreen`: `val state by viewModel.uiState.collectAsStateWithLifecycle()`. Delete the "once D-006 adds" comment and the `collectAsState` import, and add the lifecycle import. Nothing else in the file changes.
- Commit: `feat(D-006): collect report state with lifecycle`.
- Verify: build and lint.

### Slice 3: scaffold, top bar, dashboard and list
- Add `CityFixApp` and `toTopBarScreen()` with `DashboardRoute` and `TicketListRoute` only.
- `onAddClick = {}` and `onTicketClick = {}`, each with a `// slice N` comment: these routes are not in the graph yet, so a real `navigate` would crash.
- Leave `contentWindowInsets` at its default for now; keep `padding(innerPadding).consumeWindowInsets(innerPadding)` on the `NavHost`.
- Delete `Greeting` and `GreetingPreview` and their now unused imports. Delete `welcome_message` from both `strings.xml`.
- Commit: `feat(D-006): add app scaffold with dashboard and list routes`.
- Verify on the emulator: **first run of the Compose `ui` mix**, watch Logcat. Dashboard shows the sample figures. "See active issues" opens the list, the arrow appears, the toggle changes the cards, rotation keeps the list and the toggle, back works. The dashboard has no arrow. Grep: no `Greeting` or `welcome_message` left in `app/src`.
- A `NoSuchMethodError` or similar: stop and ask Diogo.

### Slice 4: detail
- Add `composable<TicketDetailRoute>` and wire `onTicketClick` with `launchSingleTop`.
- Commit: `feat(D-006): add ticket detail route`.
- Verify on the emulator: a card opens its ticket (this also proves the route serializer), the anonymous detail shows only header, media and map, rotation keeps it, back returns to the list with the toggle unchanged.

### Slice 5: report and snackbar
- Add `composable<ReportTicketRoute>`, wire `onAddClick`, add the snackbar state, `snackbarHost` and `report_submitted` in EN and PT in this same commit.
- Commit: `feat(D-006): add report route with confirmation snackbar`.
- Verify on the emulator, EN and PT: from dashboard, list and detail, add opens an empty form and Back returns. A half-filled form rotates with values and errors kept. Leave and reopen: empty. PUBLIC and PRIVATE reports as in the acceptance checks. Reopen after a submit: empty, Submit enabled. Double tap on add adds one screen. The keyboard does not cover the description in either orientation.
- Read the code for the `null` branch: returns `false` with no navigation and no snackbar.

### Slice 6: cutout
- Add `contentWindowInsets = WindowInsets.safeDrawing` to the `Scaffold`.
- Run the inset checks: Developer options > Display cutout > Tall cutout, landscape in both rotations, all four screens. Also repeat the keyboard check: no double gap.
- If a top-bar control sits under the cutout, add to `TopAppBar(...)` in `MainTopAppBar.kt`:
  ```kotlin
  windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
  ```
  It still imports nothing from `androidx.navigation`, `viewmodel` or `model`. Record in `state.md` which way the check went.
- Commit: `fix(D-006): keep content clear of the display cutout`.

### After the slices
Run all the acceptance checks in `00-plan.md` once more, and record the result in `state.md`.

## Conventions
- No user-visible literals; the only new string has EN and PT in one commit.
- `modifier` is not passed to screens. The padding sits on the `NavHost` (see 00-plan, Insets); record it as an intended difference from the overview's snippets.
- `navigate(...) { launchSingleTop = true }` on every forward navigation.
- Named arguments on `reportTicket`.
- Keep the code plain, with a comment only where the reason is not obvious (the four in the sketch). Do not extract destination helpers, a `NavGraphBuilder` extension or an `AppState` class.
- No `!!`, no `GlobalScope`.
- Commits: Conventional Commits with the task ID, no attribution.

## Do not touch
- `model/` including `TicketPrivacy.kt`, `viewmodel/`, `data/`, `utils/`, `ui/theme/`, `ui/components/`.
- `DashboardScreen.kt`, `ListScreen.kt`, `ShowScreen.kt`, `Destinations.kt`.
- `ReportScreen.kt` beyond slice 2; `MainTopAppBar.kt` beyond the one line in slice 6.
- `AndroidManifest.xml`; no `configChanges`.
- `composeBom`, `kotlin`, `agp`, `lifecycleRuntimeKtx`, `kotlinxSerialization` versions.
- `.idea/`, `google-services.json`, Firestore or Storage rules.

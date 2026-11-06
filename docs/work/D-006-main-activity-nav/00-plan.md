# D-006: MainActivity with Scaffold and NavHost
Type: feature · Task file: docs/tasks/phase-1/D-006-main-activity-nav.md
Labels: none

## Problem
All the phase-1 pieces exist, but nothing connects them. `MainActivity.kt` still draws the template `Greeting`, so none of the screens, the top bar or the ViewModels have run on a device.

Three things are missing:
- **Dependencies:** `navigation-compose`, `lifecycle-viewmodel-compose` and `lifecycle-runtime-compose` are not in the catalog.
- **Wiring:** nothing owns the `NavHost`, `isExpanded`, the shared `TicketsViewModel` or the report confirmation.
- **Insets:** `enableEdgeToEdge` is on, so a cutout in landscape can cover content unless the `Scaffold` and bar handle it.

The real signatures differ from `docs/tasks/phase-1/README.md`:
- `ShowScreen` needs a required `audience`.
- `ReportScreen` takes a `ReportViewModel` and `onSubmit: (ReportSubmission) -> Boolean`.
- `DashboardScreen` takes `onSeeTicketsClick`.

## Outcome
- The app opens on the dashboard and navigates dashboard -> list -> detail -> report.
- `MainActivity.kt` holds the activity plus one private composable, `CityFixApp`, which is the only code that knows about navigation.
- A submitted report shows a localised snackbar and returns to the previous screen. It never opens the new ticket.
- The template `Greeting`, its preview and `welcome_message` (EN and PT) are gone.

## Acceptance checks
All are checked by hand on an emulator, in English and Portuguese where text is involved.

Task checks:
- [ ] The app navigates dashboard -> list -> detail -> report ("See active issues", tap a card, top-bar add).
- [ ] Rotating on each of the four screens keeps the screen, and on the list keeps the expanded state.
- [ ] Back works from every screen, with both the system back and the arrow. Back on the dashboard leaves the app.

Contract checks:
- [ ] The back arrow is absent on the dashboard and present on the other three screens, including right after going back (D-005 N4).
- [ ] The toggle changes the list cards, and the value survives list -> detail -> back.
- [ ] A PUBLIC report: snackbar, back on the previous screen, the ticket appears at the top of the list, and the dashboard total goes up by 1.
- [ ] A PRIVATE report: snackbar, the ticket is not in the list, no "Ticket not found", and the dashboard total goes up by 1.
- [ ] The snackbar text is EN "Report submitted." and PT "Ocorrência enviada.".
- [ ] A half-filled form keeps its values and errors on rotation. Leaving and reopening starts empty, and so does reopening after a successful submit.
- [ ] A double tap on add, on "See active issues" or on a card adds only one screen (one back press undoes it).
- [ ] The anonymous detail shows no description, history, date or coordinates (`DetailAudience.PUBLIC`).
- [ ] The keyboard does not cover the description in either orientation, and there is no double bottom gap (F-006 contract).

Inset check (D-005 N5, F-003 N3), on an emulator with Developer options > Display cutout > Tall cutout, in landscape in both rotations:
- [ ] No top-bar control sits under the cutout on any of the four screens.
- [ ] No card, field or Submit button sits under the cutout, and the last item clears the navigation bar.

Build checks:
- [ ] `./gradlew assembleDebug` and `./gradlew lint` pass after each slice, 0 errors. The only new warning allowed is `NewerVersionAvailable` for navigation.
- [ ] `debugRuntimeClasspath` shows the versions listed in Approach, "Dependencies".

## Approach

### Dependencies
The D-004 note in `overview.md` and `docs/report/notes.md` is about **`kotlinx-serialization-core`** staying at 1.11.0. The instruction for this task says "Compose runtime". Both happen to be 1.11.0 today, so this plan checks both.

Verified on 2026-11-02 (from `debug-artifact-libraries.xml`, the Gradle cache, and the Google Maven POM and `.module` files):

| Fact | Evidence |
| --- | --- |
| BOM 2026.02.01 sets Compose `runtime` and `ui` to 1.10.4 | BOM POM |
| Today `runtime` already resolves to **1.11.0**, above the BOM, and `ui` and `foundation` to 1.10.4 | lint model |
| The cause is `lifecycle-runtime-compose` 2.11.0, already on the runtime classpath only: `activity-compose` brings it, and it requires Compose `runtime` 1.11.0 | lint model, `.module` in cache |
| `kotlinx-serialization-core` is 1.11.0, `kotlin-stdlib` 2.3.20, `savedstate-compose` 1.4.0 | lint model |
| `navigation-compose` **2.9.8** requires Compose 1.7.x, serialization 1.7.3, lifecycle 2.9.0 | POM and `.module` |
| `navigation-compose` **2.10.2** (latest stable) requires Compose `runtime` **1.11.2** | POM |
| `lifecycle-viewmodel-compose` 2.11.0 requires `runtime` 1.11.0, and at runtime **`ui` 1.11.0** | `.module` |

Consequences:
- **Use `navigation-compose` 2.9.8**, the version D-004 suggested. 2.10.2 is rejected because it would push the Compose runtime past 1.11.0. 2.10.0 and 2.10.1 were not checked and are not needed.
- **`viewModel()` needs `lifecycle-viewmodel-compose`.** `activity-compose` does not bring it, and it is not in the Gradle cache. `navigation-compose` would bring it transitively, but we import it directly, so declare it explicitly.
- **`collectAsStateWithLifecycle` needs `lifecycle-runtime-compose` declared.** It is on the runtime classpath but not the compile classpath (runtime scope in `activity-compose`).
- Both lifecycle entries reuse `version.ref = "lifecycleRuntimeKtx"` (2.11.0), because lifecycle artifacts move together.
- **One visible side effect:** `androidx.compose.ui` is expected to move from 1.10.4 to 1.11.0, while `foundation` stays at 1.10.4 and `material3` at 1.4.0. This does not cross the 1.11.0 limit, but the mix is new, so slice 1 records the result and slice 3 runs the app (see Failure modes).

Expected `debugRuntimeClasspath`: `kotlinx-serialization-core` 1.11.0, Compose `runtime` 1.11.0, Compose `ui` 1.11.0, `navigation-compose` 2.9.8, lifecycle 2.11.0.

### Structure
`MainActivity` keeps its `enableEdgeToEdge` setup unchanged and calls `CityFixApp()` inside `CityFixTheme`. `CityFixApp` is the stateful layer for the stateless screens:

| State | Owner | Why |
| --- | --- | --- |
| `TicketsViewModel` | `viewModel()` above the `NavHost` (activity scope) | One instance, so a report reaches the list; survives rotation |
| `tickets`, `stats` | `collectAsStateWithLifecycle()` above the `NavHost` | Passed down to the destinations |
| `NavController` | `rememberNavController()` | Restores the back stack after rotation |
| `isExpanded` | `rememberSaveable` above the `NavHost` | Needed by the bar and the list; survives rotation and leaving the list |
| `SnackbarHostState`, coroutine scope | `remember` / `rememberCoroutineScope` above the `NavHost` | Must outlive the report destination, which is popped straight away |
| `ReportViewModel` | `viewModel()` **inside** `composable<ReportTicketRoute>` | Scoped to that back-stack entry (decision 0008) |

### Top bar
- `backStackEntry` comes from `currentBackStackEntryAsState()`.
- A small private `NavDestination?.toTopBarScreen()` uses `hasRoute<T>()`.
- `canNavigateBack = navController.previousBackStackEntry != null` is read in the body of `CityFixApp`, the same scope that reads `backStackEntry`, and passed to the bar as a plain value. Reading it inside the `topBar` lambda or alone is what makes the arrow go stale (N4).

### Detail
- The id comes from `backStackEntry.toRoute<TicketDetailRoute>().ticketId`, and the ticket from `tickets.firstOrNull { it.id == id }`. This is not `getTicketById`, which is a one-off snapshot.
- **Audience: `DetailAudience.PUBLIC`**, always, because phase 1 has no login and every user is anonymous (decisions 0006 and 0007). `tickets` is already projected by `toAnonymousView()`, so the author fields are blank and the location is coarse. `PUBLIC` also keeps `description` and `statusHistory` off the detail. The `Citizen`/`Operator` mapping is the Auth task's.

### Report: how the two ViewModels fit
They never reference each other. The flow is:
1. `ReportScreen`'s Submit calls `ReportViewModel.submit()`, which validates and returns a `ReportSubmission` or `null`.
2. `ReportScreen` calls our `onSubmit(submission)`.
3. `onSubmit` calls `TicketsViewModel.reportTicket(...)`. D-006 is its only caller. Use **named arguments**: `ReportSubmission`'s order differs from `reportTicket`'s and three of the values are `String`, so positional arguments could compile with fields swapped.
4. On a non-null id: launch the snackbar, `navController.navigateUp()`, return `true`. On `null`: return `false` and the form stays open with its own error.
5. `ReportScreen` passes the `Boolean` to `onSubmitResult`.

- **Scope of `ReportViewModel`:** the back-stack entry, not the activity. Rotation keeps the form, because the entry's `ViewModelStore` survives configuration changes. `navigateUp()` pops the entry, which clears the ViewModel, so the next visit gets a new one with empty fields and `submitted = false`. With activity scope, the old values and the `submitted` lock would come back and Submit would stay disabled.
- **The returned id is used only as a success flag**, never navigated to (decision 0006).
- `ReportScreen` moves from `collectAsState()` to `collectAsStateWithLifecycle()`, the one-line change F-006 left for this task.

### Navigation details
- Dashboard -> list, list -> detail and add -> report all use `launchSingleTop = true`, so a double tap cannot stack a screen twice.
- `onBackClick = { navController.navigateUp() }`.
- The add button stays on the detail screen (D-005 Q2), as that is how detail -> report works.

### Insets
- `Scaffold(contentWindowInsets = WindowInsets.safeDrawing)`, so the inner padding covers system bars, cutout and keyboard. It is one word, and it is the option F-003 N3 named.
- **One padding, in one place:** `NavHost(modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding))`, instead of passing a modifier to all four screens as the overview's contract snippets do. The effect is the same with less repetition, and `consumeWindowInsets` covers the report route. Its `imePadding()` then adds nothing extra, because the keyboard inset is already consumed.
- The screens' own 16dp padding stays as it is, inside the inner padding.
- `MainTopAppBar`'s `TopAppBar` uses its default `windowInsets`. I could not confirm whether that default includes the display cutout: one search result says it does, the source was not read. So this is decided on the emulator (slice 6). If a control sits under the cutout, add one line in `MainTopAppBar.kt`:
  `windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)`.
  Otherwise leave the bar untouched.

### Alternatives considered
- **`CityFixApp` in its own file in `ui/navigation/`.** Tidier, but the task names `MainActivity.kt`, and the D-005 notes say `MainActivity` stays the only place that knows about navigation (Q1).
- **Confirmation as a one-shot event in a ViewModel** (`SharedFlow`/`Channel`). It would survive rotation, but it adds an event type and a collector to save a message that lasts 4 seconds.
- **`getTicketById` for the detail.** It would not update when the list changes (D-003 review N4).
- **`ReportViewModel` above the `NavHost`.** Rejected by decision 0008; see above.
- **Default `Scaffold` insets plus `displayCutout` padding per screen.** Four edits instead of one.

## Architecture impact
- First real run of every screen.
- `MainActivity.kt` becomes the app's composition root.
- New dependencies: three, through the catalog.
- One edit in Francisco's file `ReportScreen.kt`, which F-006's contract asks for. A possible one-line edit in `MainTopAppBar.kt`.
- `model/`, `viewmodel/`, `data/` and the screens' signatures do not change.
- No manifest change: rotation recreates the activity, which is what the state above relies on, so do not add `configChanges`.

## Failure modes
- **Compose `ui` 1.11.0 next to `foundation` 1.10.4 crashes or misdraws.** This has not been run. The mismatch would show as `NoSuchMethodError` when the app runs. Stop at slice 3, record it in `state.md`, and ask Diogo. A BOM bump or lifecycle downgrade is a dependency decision for him, not something to do silently.
- **`navigation-compose` 2.9.8 was built against lifecycle 2.9 but runs on 2.11 and `savedstate` 1.4.** The same risk and the same response apply. Watch `viewModel()` inside a destination and the rotation checks.
- **A dependency resolves above the limit.** Slice 1's check catches it. Do not continue, and do not add `strictly` constraints without asking.
- **Missing route serializer at runtime.** The plugin is already applied (D-004), so the first navigation would show this. Check `TicketDetailRoute` by opening a ticket.
- **Title flashes "Dashboard" for one frame after rotation.** `currentBackStackEntryAsState()` starts as `null`, which maps to `DASHBOARD`. It is cosmetic. Check it on the emulator and record it in `state.md`, and fix it only if it is actually visible.
- **Rotating while the snackbar shows loses it.** The state is in `remember` and the scope is cancelled. Accepted: the ticket exists and the list shows it (PUBLIC).
- **`onSubmit` returns `false`.** It cannot be triggered from the form, because `submit()` already validates. It is checked by reading the code, not on the emulator.
- **`toTopBarScreen()` has an `else`.** A future route would silently get the dashboard title. Accepted, because `hasRoute` calls are not exhaustive. Add a comment.
- **Reported ticket lost on process death, while the route still holds its id.** The detail shows "Ticket not found". Accepted (decision 0006, phase 1).
- **The snackbar sits under the keyboard or the navigation bar.** Check after a submit with the keyboard open.

## Risks and open questions
Recommendations are in **bold**.

- **Q1. Where does `CityFixApp` live?** **A private composable in `MainActivity.kt`**, as the task file says. Move it only if the file grows past what one person can read in one go.
- **Q2. Is the expand toggle wired unconditionally?** This is the question to look at most closely. In phase 1 everyone is anonymous, so the toggle shows the free-text description in the list, which brief I.1.B leaves out and which may contain personal data. It also contradicts the detail, which hides that text (decision 0007). The D-006 task and its acceptance check require the expanded state, and F-003 Gate 1 Q2 accepted it until roles exist. **Wire it as specified in D-006, and open a separate task for the projection change (S4: `toAnonymousView()` blanks `description` and clears `statusHistory`) plus the toggle's anonymous behaviour, to be finished before delivery.** Note the two interact: once the description is blanked, the toggle shows only the date. Alternative: hide the toggle now, but that drops a task acceptance check.
- **Q3. Should the projection change (S4) be done in D-006?** It is a `model/` rule, not wiring. **No, as in Q2.** Until then those fields stay hidden from the detail only in the UI.
- **Q4. One snackbar text for both scopes?** A PRIVATE report leaves no trace in the app, so the user may look for it. **One text, "Report submitted." / "Ocorrência enviada."**: phase 1 has no "my tickets" screen to point to. `Enviar` matches the PT Submit button.
- **Q5. Accept Compose `ui` 1.11.0 above the BOM's 1.10.4?** It is expected to follow from the required lifecycle library. **Accept it if slice 3 runs cleanly**, and record it in the overview. The alternatives, a BOM bump (which may pass 1.11.0) or moving all of lifecycle down, change more than this task needs.
- **Q6. Which is the limit: serialization or Compose runtime?** The written note says serialization; the instruction says Compose runtime. **Treat both as limits at 1.11.0.** Plan 2.9.8 satisfies both.
- **Q7. Keep `getTicketById`?** D-006 does not call it. **Keep it**: it is D-003's documented API, and removing it is out of scope.
- **Q8. The brief.** I did not re-read the PDF for this task. The rules come from the decisions and the overview, which quote it.
- Risk: lint may report more than one new `NewerVersionAvailable`. The two lifecycle entries share the existing version, so one new warning for navigation is expected. Anything else needs a look.
- Risk: the emulator checks cover a lot and need Diogo's hands. The plan cannot complete them.

## Out of scope
- F-007 report attachments, including the `reportTicket` attachments parameter and the `onSubmit` call change. That is coordinated with Francisco when F-007 is done.
- The projection change (S4) and hiding the toggle or add button by role (Auth task).
- A real map, real media, or a sample ticket with attachments (F-005 handover).
- Registered and operator detail views; mapping roles to `DetailAudience`.
- A "my tickets" screen; a "no longer public" message; animations or custom transitions.
- `formatDate` locale (F-005 N7) and the F-006 review nits, including rewriting the `submit()?.let` wiring (N5).
- `SavedStateHandle`, process-death survival.
- Moving lifecycle, Kotlin, AGP or the BOM. Automated tests.

## Slices
Each leaves the build green and becomes one commit.

1. `feat(D-006): add navigation and lifecycle compose dependencies`: catalog and `app/build.gradle.kts`; record the resolved versions.
2. `feat(D-006): collect report state with lifecycle`: `ReportScreen.kt`, one line plus the import and comment.
3. `feat(D-006): add app scaffold with dashboard and list routes`: `CityFixApp`, top bar, `isExpanded`; remove `Greeting`, its preview and `welcome_message`. The add button is a temporary no-op.
4. `feat(D-006): add ticket detail route`: `composable<TicketDetailRoute>` with `DetailAudience.PUBLIC`.
5. `feat(D-006): add report route with confirmation snackbar`: report destination, add button, snackbar, `report_submitted` EN and PT.
6. `fix(D-006): keep content clear of the display cutout`: `safeDrawing`, and the bar line if the emulator shows it is needed.

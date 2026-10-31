# D-005: MainTopAppBar
Type: feature · Task file: docs/tasks/phase-1/D-005-main-top-app-bar.md
Labels: none

## Problem
The app needs one top bar whose content changes with the current screen. D-006 will place it in the `Scaffold`. Three things stand in the way:
- `navigation-compose` is not a dependency yet (D-006 adds it), so the bar cannot read the back stack itself.
- No icon library is available. `material3` 1.4.0 has no `material-icons` dependency (checked in the cached `.module` file), so `Icons.*` does not compile today.
- `strings.xml` holds only `app_name`, and `values-pt/` does not exist. F-001 has not reached this branch.

The task names three controls but does not say on which routes they appear. This plan settles that (see Approach, table).

## Outcome
- `ui/navigation/MainTopAppBar.kt` holds a stateless `MainTopAppBar` composable and a small enum, `TopBarScreen`, with one value per route.
- `MainTopAppBar` shows a title, a back arrow, an expand/collapse toggle and an add button, each only where the table below says.
- All text and content descriptions come from `strings.xml`, in English and Portuguese.
- The bar has no navigation library, no ViewModel and no state of its own. Nothing calls it yet; D-006 does.

## Acceptance checks
- [ ] Back arrow only when there is a back stack: it appears only when `canNavigateBack = true`, on any screen. D-006 passes `false` on the start destination.
- [ ] Every icon has a `contentDescription` from `strings.xml`, with no hard-coded text. The toggle's description changes with its state.
- [ ] `@Preview` in portrait and landscape, for each of the four screens.
- [ ] The toggle appears only on `LIST`, and the add button on every screen except `REPORT` (Q1, Q2). Checked in the previews.
- [ ] Toggle icon: arrow down when collapsed, arrow up when expanded.
- [ ] `MainTopAppBar.kt` imports nothing from `androidx.navigation`, `viewmodel` or `model`.
- [ ] All new strings exist in `values/strings.xml` and `values-pt/strings.xml`.
- [ ] `./gradlew assembleDebug` and `./gradlew lint` pass after each slice, with no new lint errors.

## Approach
**The bar is stateless.** Everything arrives as parameters, and clicks leave as callbacks:

```kotlin
MainTopAppBar(
    screen: TopBarScreen,
    canNavigateBack: Boolean,
    isExpanded: Boolean,
    onBackClick: () -> Unit,
    onToggleExpandedClick: () -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
)
```

**How it knows the route.** `TopBarScreen { DASHBOARD, LIST, DETAIL, REPORT }` has one value per route in `Destinations.kt`. D-006 converts the current destination to this enum with `hasRoute<T>()`. The rule "which control on which screen" stays in this one file, inside a `when`, so the compiler forces a decision if a screen is added later.

**How it knows the back stack.** `canNavigateBack` is a plain `Boolean`. D-006 passes `navController.previousBackStackEntry != null`. It is separate from `screen` because the acceptance check depends on the back stack, not on the route.

**Controls per route:**

| Route | Title | Back arrow | Toggle | Add |
| --- | --- | --- | --- | --- |
| `DashboardRoute` | Dashboard | if back stack (never in practice: start destination) | no | yes |
| `TicketListRoute` | Tickets | if back stack | **yes** | yes |
| `TicketDetailRoute` | Ticket details | if back stack | no | yes |
| `ReportTicketRoute` | Report an issue | if back stack | no | no |

- **Expand/collapse toggle:** it switches the ticket list between compact and expanded cards. Only `ListScreen(tickets, isExpanded, ...)` and `TicketCard(ticket, isExpanded, ...)` take `isExpanded`, so it belongs on the list only. `ShowScreen` and `DashboardScreen` have no such parameter. D-006 owns the value with `rememberSaveable`.
- **Add button:** it opens the report form (`ReportTicketRoute`). D-006 passes the callback. It is hidden on the report screen, where it would open the form on top of itself. It is on the detail screen because `ShowScreen` has no report callback and the required flow is dashboard -> list -> detail -> report (Q2).

**Icons:** add `androidx.compose.material:material-icons-core` with no version; the Compose BOM sets 1.7.8. That is the frozen last release of the library, not a mismatch with the other Compose libraries. Icons used:
- `Icons.AutoMirrored.Filled.ArrowBack` (flips for right-to-left languages);
- `Icons.Filled.KeyboardArrowDown` and `KeyboardArrowUp`;
- `Icons.Filled.Add`.

`ExpandMore`/`ExpandLess` would look the same, but they are in the extended library (Q3).

Alternatives considered:
- **Plain flags only** (`showToggle`, `showAdd`, `title`), as suggested in D-004 Q1. It is one concept fewer, but it moves the route rule into `MainActivity`. D-005 would then not deliver "shown depending on the route", and that rule could not be previewed here. An enum keeps it just as simple and keeps the rule in this task (Q4).
- **Reading `NavBackStackEntry` in the bar.** This needs `navigation-compose` now, ties previews to a `NavController` and goes against D-004 Q1.
- **`sealed interface` over the routes** (D-004 Q4). It would need edits to `Destinations.kt`, and the bar would still have to pass the current route object in from D-006. Not needed: the answer to D-004 Q4 is no.
- **`material-icons-extended`.** Very large for four icons (Q3).
- **Hand-copied vector drawables.** No dependency, but four more XML files and `painterResource` calls, and the course style uses `Icons` (Q3).

## Architecture impact
- First composable in `ui/navigation/`. It depends only on Compose, Material3 and resources.
- One new dependency: `material-icons-core`, through the catalog, managed by the BOM.
- Creates `res/values-pt/strings.xml`, unless F-001 has landed first (Q5).
- **Contract for D-006:**
  - Convert the destination to `TopBarScreen` with `hasRoute<T>()`.
  - Pass `canNavigateBack = navController.previousBackStackEntry != null`.
  - Keep `isExpanded` in `rememberSaveable` above the `NavHost`, and pass it to both the bar and `ListScreen`.
  - `onBackClick = navController::navigateUp`; `onAddClick` navigates to `ReportTicketRoute`.
  - Use `launchSingleTop = true` for the add navigation, as a double tap could otherwise open the form twice.

## Failure modes
- **An icon is not in the core set.** I could not confirm the core list: the library is not in the local Gradle cache, and the online lookups failed. The list above is from memory. A missing icon is a compile error in slice 3, not a runtime problem. Mitigation: stop, record it in `state.md`, and use a vector drawable for that icon (Q3).
- **Lint `MissingTranslation` error.** Once `values-pt/` exists, every default string must be translated, and `app_name` would be missing. Mitigation: `values-pt/strings.xml` also contains `app_name`.
- **Merge conflict with F-001.** Both tasks touch `strings.xml`, and both may add `values-pt/strings.xml`. Mitigation: a small additive conflict, resolved by keeping both sides. Names are prefixed `top_bar_` and `cd_` (Q5).
- **`TopAppBar` is experimental.** It may need `@OptIn(ExperimentalMaterial3Api::class)`. Mitigation: add the opt-in on the composable; the build confirms it.
- **Long title in landscape or Portuguese.** Mitigation: `maxLines = 1` with an ellipsis.
- **Stale `isExpanded`.** The bar only shows what it receives, so a toggle with no effect is a D-006 wiring bug. D-006's acceptance check covers it.

## Risks and open questions
Recommendations are in **bold**.

- **Q1. Is the toggle global or per card, and where does it appear?** The task does not say. `isExpanded` is one value passed to `ListScreen`, so it is global for the whole list. **List screen only, global.**
- **Q2. Where does the add button appear?** No file says. The flow "detail -> report" needs a way out of detail, and `ShowScreen(ticket?, ...)` has no report callback. Alternative: show add only on dashboard and list, and add a report button to `ShowScreen` (F-005, Francisco). **Everywhere except the report screen**, so no change is needed in Francisco's screens. Later phases will probably have to hide it by role (see the risk below).
- **Q3. Icon source?** (a) `material-icons-core`, (b) `material-icons-extended`, (c) vector drawables. Google's direction is Material Symbols vectors, and the icons library is no longer updated, but it still works and is in the BOM. **(a)**, with (c) as fallback for any icon that is missing.
- **Q4. Enum or plain flags?** See Alternatives. The brief to the planner expected stateless parameters, and this is still stateless. **Enum `TopBarScreen`.** If you prefer flags, the rule moves to D-006 and the bar loses `screen` but gains `title`, `showToggle` and `showAdd`.
- **Q5. Who owns the strings?** F-001 owns the theme and strings, but it is not on this branch. `origin/dev/Francisco` (693b05d) differs from `origin/main` (9b15733), and I could not read that commit (no git access, and the planner stays inside the work folder). **D-005 adds its strings and creates `values-pt/strings.xml` now.** Diogo should check `git diff origin/main origin/dev/Francisco -- app/src/main/res` before slice 2. If `values-pt` or similar strings already exist there, merge first or reuse those names.
- **Q6. Titles.** The task does not mention them, but a `TopAppBar` needs one. Alternative: always show `app_name`, which needs fewer strings but does not tell the user where they are. **One title per screen.**
- **Q7. The brief was not read.** The PDF could not be opened here (`pdftoppm` is not installed). The route rules come from the task files and the overview only. Diogo should confirm nothing in the brief contradicts them.
- **Q8. Commit type for the dependency slice.** D-004 used `build(...)`; this task was given the scope `feat(D-005)`. **`feat(D-005)` on all slices**, as instructed. Either is fine.
- Risk: the add button is shown to everyone in phase 1, where the single local user may report. If the brief does not let anonymous users report, the Auth phase must add a role check to the bar.
- Risk: the bar cannot be run until D-006. Until then, verification is build, lint and previews.

## Out of scope
- Automated tests.
- `MainActivity`, `Scaffold`, `NavHost`, and adding `navigation-compose` (D-006).
- The `rememberSaveable` expanded state (D-006).
- Theme work and the wider string set (F-001).
- Role-based controls, search, menus, scroll behaviour, large or collapsing top bars.
- Bumping the Compose BOM, Kotlin or AGP.

## Slices
1. `feat(D-005): add material icons core dependency`: catalog entry and `app/build.gradle.kts`.
2. `feat(D-005): add top bar strings`: `values/strings.xml`, new `values-pt/strings.xml`.
3. `feat(D-005): add MainTopAppBar`: `ui/navigation/MainTopAppBar.kt` with its previews.

Each slice is verified with `./gradlew assembleDebug` and `./gradlew lint`, and becomes one commit.

# D-006: Review

Reviewer: reviewer agent · Date: 2026-11-02
Diff: `git diff 3eb1195..HEAD`, the Base in `state.md`. The code commits are d359cb2, 8147a46, 9ea90fb, aefec5e and 1181701. The requested range `d359cb2..47ba0a6` leaves out d359cb2 itself (the dependencies), so the review starts from Base to include it. 980424d, 2ac12d7 and 47ba0a6 are docs only.
Checked against: 00-plan, 01-coding-plan, 03-docs-plan, task file `docs/tasks/phase-1/D-006-main-activity-nav.md`, overview.md, decisions 0006, 0007 and 0008, kotlin-conventions, `.claude/rules/kotlin.md`, CLAUDE.md and the brief (I.1 and section V, text extracted with `pdftotext`). Gate 1 approved Q1-Q7 as the planner recommended.
Verification: `./gradlew assembleDebug lint --rerun-tasks` finishes with 0 errors and 14 warnings. The one new warning is `GradleDependency` for `navigation-compose`, and none is in `MainActivity.kt`, `ReportScreen.kt` or a `strings.xml` file. `debugRuntimeClasspath` resolves `kotlinx-serialization-core` 1.11.0, `runtime` 1.11.0, `ui` 1.11.0, `foundation` 1.10.4, `material3` 1.4.0, `navigation-compose` 2.9.8, both lifecycle artifacts 2.11.0 and `kotlin-stdlib` 2.3.20, matching `state.md`. No test is asked for, by project decision.
Not verified: nothing was run on a device. Navigation, rotation, the snackbar and insets are checked here by reading the code. Slice 6 and every emulator check are known open items, not findings, including slice 3's stop condition (the first run of Compose `ui` 1.11.0 with `foundation` 1.10.4). `LocalViewModelStoreOwner` still exists in `lifecycle-viewmodel-compose` 2.11.0 at the package `navigation-compose` 2.9.8 references, but its method signatures were not compared, so `viewModel()` inside a destination stays part of that first run.

## Verdict
**Approve for the emulator checks.** Nothing blocks, and the code matches the coding plan's `CityFixApp` apart from the `safeDrawing` line that belongs to slice 6. Every point on the contract list holds. One action is needed outside the code: S1, the task file for the deferred privacy work. The nits are small; N1 is the only one that changes code.

## What was checked and holds
- **Back arrow (D-005 N4).** `canNavigateBack` is read in the body of `CityFixApp`, right after `backStackEntry` is read through its `State` delegate, so any change of the current entry recomposes that line. It reaches `topBar` as a plain value. No route in this graph changes the previous entry without changing the current one, so the arrow cannot go stale.
- **ViewModel scopes (0006, 0008).** `TicketsViewModel` comes from `viewModel()` in `CityFixApp`, above the `NavHost`, so its owner is the activity. `ReportViewModel` comes from `viewModel()` inside `composable<ReportTicketRoute>`, so its owner is the back-stack entry. The two never reference each other: the form hands over a `ReportSubmission` and gets a `Boolean` back through `onSubmit`.
- **Returned id.** `id` is only compared with `null`. It reaches neither `navigate` nor `TicketDetailRoute`. The `null` path skips the snackbar and `navigateUp()` and returns `false`, so `onSubmitResult(false)` shows the form error. `reportTicket` has exactly one caller.
- **Named arguments.** All five are named and each maps to the field of the same name: `description`, `categoryId`, `categoryName`, `situation`, `scope`. `ReportSubmission` declares them in a different order, so naming them is what prevents a silent swap.
- **Double tap, forward.** The dashboard button, the card tap and the add button all use `launchSingleTop = true`. Two different cards tapped quickly replace the top detail entry rather than stacking. Submit has its own lock (`submitted`). Backward taps are in N1.
- **Insets as far as slice 5.** The `NavHost` has `padding(innerPadding)` and then `consumeWindowInsets(innerPadding)`, in that order, so `imePadding()` in `ReportContent` does not add the navigation bar again. All four screens take `modifier` as optional, so one padding on the `NavHost` covers them, as 00-plan intends. The same modifier is correct after slice 6: with `safeDrawing`, the keyboard inset is inside `innerPadding` and the consume makes `imePadding()` add nothing.
- **`DetailAudience.PUBLIC`.** Set at the only call site, with the ticket taken from the collected, projected `tickets` rather than `getTicketById`. `ShowScreen`'s PUBLIC branches render neither `DescriptionSection` nor `StatusHistorySection`, and "Reported on" is inside the first, so the detail shows no description, history or date. This matches brief I.1.C and decision 0007, with the UI-only limit in "Dependencies".
- **Strings.** `report_submitted` exists in EN and PT in the same commit, with the agreed text. "Ocorrência enviada." uses the brief's term and matches the PT button "Enviar ocorrência". The message is resolved in the composable body and passed to the coroutine, so `stringResource` is not called from `launch`. `welcome_message` and `Greeting` are gone from `app/src`. No new literal is visible to the user.
- **State ownership.** `isExpanded` is `rememberSaveable` above the `NavHost`. `SnackbarHostState` and the coroutine scope are also above it, so they outlive the popped report entry. No `configChanges` in the manifest. The report form starts empty on reopening because `navigateUp()` pops the entry and its ViewModel with it.
- **Plan limits.** `model/`, `viewmodel/`, `data/`, `utils/`, the other screens and `MainTopAppBar.kt` are untouched. `ReportScreen.kt` has only the one-line change plus its import and comment. No `!!` and no `GlobalScope`. The catalog and `app/build.gradle.kts` match the plan line for line.
- **MVVM.** The flows are collected with `collectAsStateWithLifecycle` in one place, and the screens stay stateless, except the F-006 report pair. `MainActivity.kt` is the only file that knows about navigation (Q1).
- **Permissions.** None requested and no manifest change.
- **Accessibility, orientation.** D-006 adds no control of its own. The `Snackbar` is an M3 component and the screens keep their own landscape layouts.
- **Commits.** Conventional, with the task ID and no attribution. `.idea/` is not staged.
- **Can it be explained at the defence?** Yes. It is one 95-line composable, plain, with no helpers. Three things to prepare:
  - why the same `viewModel()` call gives two lifetimes (`LocalViewModelStoreOwner`);
  - why the order `padding` then `consumeWindowInsets` matters;
  - why `previousBackStackEntry` needs the neighbouring state read.

## Dependencies (deferred at Gate 1, not findings)
Diogo moved these to a separate task at Gate 1. They do not block D-006, but they must land before delivery:
- **Projection (F-005 S4).** `toAnonymousView()` still passes `description` and `statusHistory`, so `DetailAudience.PUBLIC` hides them only in the UI, against the rule in `.claude/rules/kotlin.md`.
- **Expand toggle.** D-006 wires it unconditionally, so for the first time a device shows the free-text description in the anonymous list, the text the detail withholds and brief I.1.B leaves out. Blanking `description` alone does not close this: `createdAt` stays in the projection, so the expanded card would still show "Reported on", which PUBLIC detail hides. The follow-up has to decide the toggle as well, not just the projection.
- **Role checks (Auth task).** Anonymous users can open the report form, and the add button shows for everyone, as already recorded in overview.md and decision 0008.

## Blocking
None.

## Should-fix
- **S1. The deferred privacy work has no task file.** `state.md` says it goes to "a separate task, to finish before delivery", and 03-docs-plan says to point to it, but `docs/tasks/` has no such file and nothing in it mentions the projection. The project has no tracker, so `state.md` is the only record of work that closes a non-negotiable CLAUDE.md rule. Fix: Diogo adds a task file in `docs/tasks/phase-1/` with the next D- ID, covering `toAnonymousView()` (blank `description`, clear `statusHistory`, keep `currentStatus`), the amendment to decision 0006, and hiding or forcing off the toggle for anonymous users, with the `createdAt` point above in its checks. The Docs phase then links it from overview.md's S4 item. This does not hold back the emulator checks.

## Nit
- **N1. A double tap on the back arrow goes back two screens.** `launchSingleTop` guards forward navigation only. After the first `navigateUp()` the arrow is still there on the previous screen, so from detail or from a report opened on the detail a double tap lands one screen further than intended. Nothing is lost that a single tap would not lose, and on the list the second tap does nothing. The 00-plan checks do not cover it. Fix, if Diogo wants it: in `onBackClick`, act only when the entry has finished its transition, and add `androidx.lifecycle.Lifecycle` to the imports:

  ```kotlin
  onBackClick = {
      if (backStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
          navController.navigateUp()
      }
  },
  ```

  `dropUnlessResumed` does not work here: above the `NavHost` it would read the activity's lifecycle, not the entry's. This delays a deliberate second back tap until the crossfade ends, so leaving the code as planned is also acceptable; record the choice in `state.md`.
- **N2. The rotation flash is wider than the plan says.** 00-plan's failure mode mentions only the title. After recreation, `backStackEntry` starts `null` and the graph is not set yet, so the first frame can also have no back arrow and show the dashboard's controls (the add button on `REPORT`, no toggle on `LIST`). It corrects itself on the next recomposition. Fix: when running the rotation check, look at all three, and record them in `state.md`. Change the code only if it is really visible.
- **N3. The lint id in the docs needs correcting.** `state.md` is right that the new warning is `GradleDependency`, and the lint report confirms it for navigation and the BOM. 03-docs-plan still says "accepted `NewerVersionAvailable` warning for navigation", and overview.md's "Navigation (D-004)" says lint reports newer versions as `NewerVersionAvailable` (not `GradleDependency`), which is true only of the serialization and Kotlin plugin entries. Fix: in the Docs phase, name `GradleDependency` for navigation and limit the D-004 sentence to those entries.

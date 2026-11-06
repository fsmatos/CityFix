# D-006: Docs plan

## Docs to update

### `docs/architecture/overview.md`
- Add a subsection "App shell (D-006)":
  - `CityFixApp` as the stateful layer, and the table of who owns which state and at what scope;
  - the report flow across the two ViewModels;
  - the versions and why (below);
  - the inset setup: `safeDrawing`, one padding plus `consumeWindowInsets` on the `NavHost`.
- Turn the "Contract for D-006" blocks into past tense or link them to that subsection, in "ViewModel (D-003)", "Navigation (D-004)", "Top bar (D-005)" and each screen under "Screens".
- Record the intended difference: screens receive no `modifier` from the caller, because the padding is on the `NavHost`.
- Correct statements this task makes stale:
  - "UI foundation": `MainActivity` is no longer the template;
  - "Report form (F-006)": the line "`collectAsState()` instead of `collectAsStateWithLifecycle()` until D-006";
  - "ReportScreen": "`MainActivity` today uses only `Modifier.padding(innerPadding)`";
  - "Top bar (D-005)": "Nothing calls it before D-006";
  - `ui/navigation/` description, if the bar changed in slice 6.
- Dependency facts:
  - `navigation-compose` 2.9.8, and why not 2.10.x (2.10.2 requires Compose runtime 1.11.2);
  - the resolved Compose versions, including `runtime` and `ui` above the BOM because of lifecycle 2.11.0;
  - both limits: `kotlinx-serialization-core` and Compose runtime at 1.11.0;
  - the accepted `NewerVersionAvailable` warning for navigation.
- Keep the projection gap (S4) and the toggle handover, and point to the follow-up task if Q2 creates one.

### `docs/tasks/phase-1/README.md`
- Update the old signatures line, or note that the code is the source of truth (`ShowScreen` audience, `ReportScreen` ViewModel and `onSubmit`).

### `docs/tasks/phase-1/D-006-main-activity-nav.md`
- Tick the acceptance checks once Diogo has confirmed them on the emulator, and set the status.

### `docs/report/notes.md`
- Add the D-006 section below.

## Decisions to record
- **No new decision record is needed by default.** The state ownership and ViewModel scopes follow decisions 0006, 0007 and 0008.
- **Consider a short decision 0009 only if Q5 changes the plan**, for example if the Compose mix forces a BOM or lifecycle change.
- If Q2 leads to the projection change, that task amends decision 0006 (the handover in 0007), not D-006.

## Report notes
To add to `docs/report/notes.md` as "D-006: MainActivity with Scaffold and NavHost".

### Technical solutions
- One composition root: `CityFixApp` is the stateful layer over stateless screens, with state hoisted to the lowest level that serves all its users.
- `rememberSaveable` for `isExpanded` (UI state shared by the bar and the list), ViewModels for data, `rememberNavController` for the back stack. Explain why each survives rotation.
- Type-safe routes with `toRoute<T>()` and `hasRoute<T>()`.
- Back-arrow trap: `previousBackStackEntry` is not Compose state, so it is read in the same scope as `currentBackStackEntryAsState()`.
- Two ViewModels at two scopes: activity (`TicketsViewModel`) and back-stack entry (`ReportViewModel`). For the defence: `viewModel()` uses `LocalViewModelStoreOwner`, which is the activity above the `NavHost` and the `NavBackStackEntry` inside `composable`. That is why the same call gives different lifetimes.
- A one-way hand-off between ViewModels through a callback returning `Boolean`, so neither holds the other.
- Snackbar state above the `NavHost`, because the destination that triggers it is popped at once.
- Named arguments on `reportTicket`, because three of the parameters are `String` and the order differs from `ReportSubmission`.
- Insets: edge-to-edge, `WindowInsets.safeDrawing`, and `consumeWindowInsets` to prevent a double inset with `imePadding()`.
- Audience `PUBLIC` in phase 1, and the three-layer privacy: list filter, projection, UI branch.

### Options chosen and rejected
- `navigation-compose` 2.9.8 over the latest 2.10.2: the transitive Compose runtime requirement (1.11.2) would pass our limit. A good example of how Gradle's "highest requested version wins" affects a choice.
- Explicit lifecycle dependencies rather than relying on transitive ones.
- Rejected: navigating to the new ticket; `getTicketById` for the detail; `ReportViewModel` at activity scope; a one-shot event in a ViewModel for the confirmation; `configChanges` to avoid recreation.
- `launchSingleTop` on all forward navigation, for double taps.

### Improvement opportunities
- Keep the snackbar across rotation with a one-shot event.
- A different confirmation for PRIVATE reports once a "my tickets" screen exists.
- Align the Compose BOM with the resolved versions when Kotlin moves to 2.3.
- Move to Navigation 2.10 at that point too.
- Nested graphs per role and deep links from FCM (already in the D-004 notes).
- Screen transitions.
- `LazyColumn` content drawn behind the navigation bar, using `innerPadding` as `contentPadding`.

### Verification
Record what actually ran: build and lint per slice, the `debugRuntimeClasspath` versions, and the emulator checks with their date, including the cutout result and whether the bar needed the inset line. State anything left unchecked. D-006 is the first time the previously unrun checks from F-003 to F-006 can be done on a device, so note which of those were closed.

# D-020: Bottom bar keeps the wrong tab and cannot leave a detail
Branch: dev/Diogo · Base: 48b8397
Remote: not pushed

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-21 | Found in the manual checklist while testing I.3.E |
| Plan | done | 2026-11-21 | 00/01/03 written; one slice, MainActivity.kt only; awaiting Gate 1 |
| Gate 1: plan approved | done | 2026-11-21 | Diogo approved; Q1 reselect on root scrolls to top; Q2 keep list -> Active |
| Code | done | 2026-11-21 | 2 slices; slice 1 used the R1 fallback (currentBackStack is RestrictedApi); device checks pending |
| Review | | | |
| Docs | | | |
| Pushed | | | |
| Merged | | | |

## Log
- 2026-11-21: started after D-019 code; origin/main already merged for D-019 (e6b34f4).
- 2026-11-21: plan (bug mode). Read MainActivity.kt (`openTab`, `selectedTab`, detail entry points), MainBottomBar.kt, RoleAccess.kt. Root cause confirmed: (1) `lastTab` is the last tab screen shown, so a restored detail highlights Profile; (2) re-selecting the current tab runs the same popUpTo(home){saveState} + restoreState, which pops and immediately restores the detail. Plan: one slice in MainActivity.kt: selected tab from `navController.currentBackStack` (nearest tab screen from the top, NavGraph skipped), `lastTab`/`SideEffect` removed, re-select = `popBackStack(tab.route, inclusive = false)` with fallback to the usual navigate. Risk R1: `currentBackStack` visibility not compiled yet (fallback recorded). Open: Q1 scroll-to-top (no), Q2 LIST highlights Active when opened from Dashboard (keep). ShowScreen/OperatorTicketScreen not touched (D-019 review).
- 2026-11-21: Gate 1 approved by Diogo. Q1 changed from the plan: tapping the selected tab while already on its root scrolls that screen back to the top (added to the coding plan as slice 2). Q2 kept: the list opened from the Dashboard still highlights Active.
- 2026-11-21: code slice 1 (31d5123, MainActivity.kt). Deviation (planned fallback R1): the back-stack derivation compiled but `./gradlew lint` failed with `RestrictedApi` on `NavController.currentBackStack` ("can only be called from within the same library group"). Applied 00-plan R1: `lastTab` + `SideEffect` kept, `openTab` also sets `lastTab = tab`, so a restored detail highlights the tab that was tapped. The reselect branch is as planned: `tab == selectedTab` -> `popBackStack(tab.route, inclusive = false)`, falling back to the usual navigate. Consequence: R2 (a citizen's Dashboard tap restoring the Active stack, if it happens) would now highlight Dashboard over Active's screen, since the highlight is the tapped tab, not the real stack; check on the device. Build OK; lint 0 errors, 17 warnings (same as before, none in changed files).
- 2026-11-21: code slice 2 (added to 01-coding-plan.md first). `MainActivity.kt`: `tabReselects` (MutableSharedFlow<Unit>, extraBufferCapacity 1) emitted in `openTab` when the selected tab is tapped on its own screen (nothing popped, `tab == currentTab`); private `ScrollToTopOnReselect` collects it in each tab root destination, whose scroll state is hoisted there. A flow instead of the suggested counter key: `LaunchedEffect(counter)` would also fire when the screen recomposes after coming back. Screens get a scroll-state parameter with the old default (Content stays previewable): DashboardScreen (ScrollState), ActiveTicketsScreen, ProfileOverviewScreen/Content, OperatorPanelScreen/Content, PendingCitizensScreen/Content, CategoriesScreen/Content (LazyListState). Categories added too (operator tab root with a list); PendingValidation (no list) and ListScreen (not a tab root) left out. ShowScreen.kt and OperatorTicketScreen.kt not touched. Build OK; lint 0 errors, 17 warnings (unchanged). Device acceptance checks not run yet.

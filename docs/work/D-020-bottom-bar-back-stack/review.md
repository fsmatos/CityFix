# D-020 review

Range: `1df270c..HEAD` (31d5123, e83bf81). Reviewed against `00-plan.md`, `01-coding-plan.md`
(slice 1 with the R1 fallback, slice 2 added at Gate 1), the task spec
`docs/tasks/phase-3/D-020-bottom-bar-detail-back-stack.md`, CLAUDE.md and kotlin-conventions.
Navigation behaviour below was traced through the Navigation 2.9.8 `navigate` / `popBackStack` /
`restoreState` code paths by reading. Nothing was run on a device.

Verdict: the reported bug is fixed and the slice-2 flow design works. One blocking issue that
was already there before (role change restores the previous user's saved tab stacks) sits right
in the area this task covers and should be fixed before delivery. Two should-fix items, a few nits.

## How the cases trace

- **(a) Original repro, operator.** Panel -> detail: the `SideEffect` sets `lastTab = PANEL`.
  Profile: `popUpTo(Panel){saveState}` pops the detail. Panel had no mapping yet, so Panel and
  the detail both map to the detail's saved state. Panel: the save/restore brings the detail back,
  and `openTab` now sets `lastTab = PANEL`, so **Panel is highlighted**. Panel again:
  `selectedTab == PANEL`, so `popBackStack(OperatorPanelRoute, false)` pops the detail (no save)
  and returns true; the `navigate` is skipped and **the panel shows**. Fixed. Approvals ->
  pending citizen and Active -> list -> detail follow the same path.
- **(b) Can `lastTab` disagree with the screen?**
  - *Notification tap.* `LaunchedEffect(openTicketId, role)` pushes the detail on top of whatever
    is showing, and `lastTab` already holds that screen's tab, so the tab under the detail is
    highlighted. OK.
  - *Rotation / process death.* `lastTab` is `rememberSaveable` and the NavController saves its
    own back stack, so both come back together. OK.
  - *Role change.* `goHome` lands on the new home, the `SideEffect` resets `lastTab`, and
    `takeIf { it in tabs }` covers the frame in between. The highlight is fine, but see B1: the
    tab stacks that get restored are wrong.
  - *R2 (the home key restoring another tab's stack).* I traced it: when you leave the home root
    by a tab tap, the home always maps to `null` (`!inclusive` branch, `savedState` empty). The
    home loses that mapping only when it is restored, and then you are inside the home tab. So
    with today's entry points, a stack from another tab never sits above an unmapped home, and R2
    does not happen. If it ever did (for example a future non-tab path from the home into another
    tab), `lastTab = tab` would highlight the home over that stack, and one more tap on the home
    would pop back to it. So the fallback recovers.
  - **Public-API alternative, not recommended.** You could derive the tab with
    `runCatching { navController.getBackStackEntry(tab.route) }` per non-home tab (the stack is
    always `[home, ≤1 tab root, …]`). It is public and works on today's stacks. But it fixes no
    disagreement that actually happens. In the hypothetical R2 case it would make things worse:
    the highlight would be the other tab, so tapping the home would run the normal
    `navigate(home)`, which saves the same stack under the home again and restores it. That is a
    loop. Keep the R1 fallback.
- **(c) SharedFlow.** `replay = 0` with `extraBufferCapacity = 1`. With no collector, `tryEmit`
  returns true and drops the value, so nothing is replayed when you come back to a tab. With a
  collector busy in `animateScrollTo`, one event is buffered and extra taps are dropped. Only
  composed destinations collect. That is right, apart from the crossfade window (N1).
- **(d) Stateless/stateful and previews.** The scroll state is created in the NavHost destination
  (so it is saved per entry) and passed down with the old `remember…State()` default. The `Content`
  composables keep their previews unchanged. That matches the conventions.

## Blocking

### B1. A new user gets the previous user's saved tab stacks, including entry-scoped ViewModels (personal data). Not introduced by D-020: it exists since D-016's save/restore
`goHome` (`MainActivity.kt` ~299) runs `popUpTo(graph.id) { inclusive = true }` with no
`saveState`. That clears only the visible stack. Stacks saved earlier by `openTab`
(`backStackMap` / `backStackStates`, with their `ViewModelStore`s) survive the logout. Example:
citizen A opens Profile, taps Dashboard (Profile is saved together with `ProfileViewModel(A.id)`),
then logs out. Citizen B logs in and taps Profile. `restoreState` brings back A's entry, and
`viewModel(factory = ProfileViewModelFactory(user.id, …))` returns the existing `ProfileViewModel`
because the factory is ignored when a ViewModel already exists. **B sees A's name, NIF, e-mail and
phone.** The same applies to:
- `ReportViewModel(citizenId)`: A's draft, photos and location, submitted under A's id.
- `TicketDetailViewModel(…, user)` and `OperatorTicketViewModel(…, user)`: actions attributed to
  the previous operator.
- `CitizenApprovalViewModel`.

The spec of this task asks that the tab stacks behave correctly "for operator, citizen, pending
and anonymous roles", so this belongs here.

Fix (smallest): key each user-scoped, entry-scoped ViewModel on the user, so a restored entry
from another user builds a new one:
```kotlin
viewModel(key = "profile-${user.id}", factory = ProfileViewModelFactory(user.id, …))      // ~756, and ~796
viewModel(key = "report-$citizenId", factory = ReportViewModelFactory(…, citizenId, …))   // ~692
viewModel(key = "detail-$id-${user.id}", factory = TicketDetailViewModelFactory(…))      // ~650
viewModel(key = "opticket-$id-${user.id}", factory = OperatorTicketViewModelFactory(…))  // ~636
```
(`CitizenApprovalViewModel` and `CredentialChangeViewModel` are the same if they capture the
operator or user.) A more thorough alternative is to drop the saved stacks in `goHome`: call
`navController.clearBackStack(route)` for every `BottomTab` route except `homeFor(role)`. For the
home, do not call `clearBackStack`: it restores the stack and then pops the home itself. Instead,
call `navigate(home) { restoreState = true; launchSingleTop = true }` followed by
`popBackStack(home, inclusive = false)`. Either way, test on a device: A Profile -> Dashboard ->
log out -> B logs in -> Profile shows B.

## Should-fix

### S1. Pending citizen: tapping Dashboard on Pending validation opens the stats Dashboard
For `PENDING_CITIZEN`, `selectedTab == DASHBOARD` (because `PENDING_VALIDATION` is in
`DASHBOARD.screens`), but `tab.route` is `DashboardRoute`, which is not on the stack. So
`popBackStack` returns false (and logs a warning), the emit is dropped, and the usual `navigate`
then **pushes `DashboardRoute`** on top of `PendingValidationRoute`. This was there before, but the
task's acceptance says "tapping the selected tab on its own screen does nothing visible", and D-016
says "home stays Registration pending". Fix: use the role's home as the root of the first tab,
and treat "already on the root" on its own:
```kotlin
val openTab = { tab: BottomTab ->
    val root = if (tab == tabs.first()) homeFor(role) else tab.route
    val onRoot = navController.currentDestination?.hasRoute(root::class) == true
    when {
        tab == selectedTab && onRoot -> tabReselects.tryEmit(Unit)            // S2: no navigate
        tab == selectedTab && navController.popBackStack(root, inclusive = false) -> Unit
        else -> navController.navigate(tab.route) {
            popUpTo(homeFor(role)) { saveState = true }; launchSingleTop = true; restoreState = true
        }
    }
    lastTab = tab
}
```
If Diogo actually wants pending citizens to reach the stats Dashboard from that tab, record that
in `state.md` instead and reword the acceptance check.

### S2. Reselect on a non-home tab root still pops and restores that same entry
On Profile, Approvals, Categories (and Active/Profile for citizens), `tab == currentTab`, so the
event is emitted and then the usual `navigate` still runs. Its `popUpTo(home){saveState}` pops the
tab's own entry, and `restoreState` immediately rebuilds it from the state it saved a moment ago.
Navigation-Compose's `AnimatedContent` probably keeps the content, because the entry id is the
same. If it does not, the collector that is running `animateScrollToItem(0)` gets cancelled, and
the restored `LazyListState` returns the old offset. Then scroll-to-top would work on the home tab
and silently fail on the others. Even if it works, it is a pointless save/restore churn. Fix: the
`onRoot` branch in S1 (emit and do **not** navigate). It also satisfies the acceptance "no
flicker, no duplicate entry". If you keep the current code, add "scroll-to-top on Profile /
Approvals / Categories" to the device checks.

## Nit

### N1. During the tab crossfade the outgoing screen also collects
NavHost's default 700 ms fade keeps both destinations composed. If you tap a tab twice quickly,
the screen that is leaving also scrolls to the top, so its saved position is lost. Fix: collect
only while the entry is resumed:
```kotlin
val owner = LocalLifecycleOwner.current
LaunchedEffect(reselects, owner) {
    owner.repeatOnLifecycle(Lifecycle.State.RESUMED) { reselects.collect { scrollToTop() } }
}
```

### N2. The stateless `…Content(listState = rememberLazyListState())` default
This is fine for previews. The stateful wrappers always pass the state now, so the default on the
stateful `…Screen` overloads is never used. You could drop it there to make it explicit that
MainActivity owns the state. Optional.

### N3. The comment above `lastTab` (~437-441)
It says "keeps the last tab selected", then corrects that in the D-020 sentence. Reword it as a
single rule ("the tab last shown or tapped; a screen outside every tab keeps it") so it is easy to
explain at the defence. Also mention there why `currentBackStack` is not used (`RestrictedApi`),
which it already partly does.

### N4. The `DashboardScreen.kt` import order
`androidx.compose.foundation.ScrollState` is inserted among the `layout.*` imports. Move it next to
`rememberScrollState`.

## Checked, no finding
- i18n: no new strings. Accessibility: the bar's `selected` semantics are now correct on a
  restored detail. No new touch targets.
- Orientation: the scroll states are saveable per entry, and `lastTab` is saveable.
- Permissions: not touched.
- Anonymous data: anonymous users never get Profile, and the detail recomputes `detailAudience`
  per user, so a restored detail is still the public view (B1 is about registered users).
- Files listed under "Do not touch" (`ShowScreen.kt`, `OperatorTicketScreen.kt`,
  `MainBottomBar.kt`, `RoleAccess.kt`): untouched.
- The R1 deviation is logged in `state.md`. The `01-coding-plan.md` slice 2 was added before the
  code was written.
- Device acceptance checks are still pending (`state.md`), so the docs phase should not mark
  I.3.E fixed until they have been run.

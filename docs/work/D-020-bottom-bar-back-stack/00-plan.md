# D-020: Bottom bar keeps the wrong tab and cannot leave a detail
Type: bug · Task file: docs/tasks/phase-3/D-020-bottom-bar-detail-back-stack.md
Labels: none

## Problem
From a tab, open a screen that belongs to no tab (ticket detail, report form, list), switch to
another tab and come back: the screen comes back but the bar highlights the tab visited last, and
tapping the home tab again never closes the detail. Found by Diogo in the manual checklist
(I.3.E, operator); the same code serves every role.

## Outcome
The highlighted tab always matches the screen: it is the tab whose part of the back stack holds
the current screen. Tapping the tab that is already selected goes back to that tab's own screen,
closing what was opened from it. Switching tabs still keeps each tab's stack (save/restore).

## Acceptance checks
- [ ] Operator: Panel -> ticket detail -> Profile -> Panel: the detail shows and **Panel** is
      highlighted. Tap Panel again: the panel shows (detail closed).
- [ ] Operator: Approvals -> pending citizen -> Panel -> Approvals: the citizen shows with
      Approvals highlighted; tap Approvals again: the approvals list.
- [ ] Citizen: Active -> See more (list) -> detail -> Profile -> Active: detail with **Active**
      highlighted; tap Active: the Active screen.
- [ ] Citizen: Dashboard -> "+" report form -> Profile -> Dashboard: whatever screen shows, the
      highlighted tab is the one whose stack it is in (no Profile highlight on the form).
- [ ] Citizen: Profile -> one of my tickets: Profile highlighted; tap Profile: the profile.
- [ ] Pending citizen (Dashboard tab on Pending validation) and anonymous (Dashboard, Active):
      tapping the selected tab on its own screen does nothing visible; the checks above with
      their tabs pass.
- [ ] Tapping a tab on its own root screen behaves as before (no flicker, no duplicate entry).
- [ ] System Back from a detail still returns to the screen it was opened from; on a tab's root
      it behaves as before.
- [ ] Rotate on a restored detail: the same tab stays highlighted; tapping it still closes the
      detail.
- [ ] Notification tap opening a ticket: the tab under it is highlighted.
- [ ] `./gradlew assembleDebug` and `./gradlew lint` pass with no new errors or warnings.

## Reproduction
Operator account, any device/emulator:
1. Panel tab: tap a ticket. Detail opens, Panel highlighted (`lastTab` = Panel).
2. Tap Profile. Profile shows (`lastTab` = Profile).
3. Tap Panel. The **detail** comes back; the bar highlights **Profile**.
4. Tap Panel again. The detail stays.
The same happens for a citizen with Active -> list -> detail, and for Approvals -> pending citizen.

## Root cause
Two separate defects in `CityFixApp` (`MainActivity.kt`, lines 432-464), both confirmed by
reading the code:

1. **Highlight.** `selectedTab = currentTab ?: lastTab`. `currentTab` is null on any screen that is
   in no `BottomTab.screens` (DETAIL, REPORT; `TopBarScreen` mapping in `toTopBarScreen()`).
   `lastTab` is a saveable value updated in a `SideEffect` only when a tab's own screen is shown.
   In step 2 Profile is shown, so `lastTab` = Profile; in step 3 the restore lands directly on the
   detail, no tab screen is shown in between, so `lastTab` stays Profile. `lastTab` records "the
   last tab screen displayed", not "the tab the current screen belongs to".
2. **Cannot leave the detail.** `openTab` always runs the standard
   `navigate(tab.route) { popUpTo(homeFor(role)) { saveState = true }; launchSingleTop = true;
   restoreState = true }`. For the operator the home (`OperatorPanelRoute`) is also a tab, and the
   detail is pushed directly on it. Step 2 pops the detail and saves it as the panel's stack;
   step 3 restores it. In step 4 the user is again on that tab, and the same call pops the detail
   with `saveState = true` and immediately restores it, so the tap is a no-op. Re-selecting the
   current tab is never handled separately. The same applies to any tab whose saved stack has
   entries above its root (Approvals -> pending citizen, Active -> list -> detail).

How the detail is opened (checked): Panel, Active, List, Profile and the notification effect all
`navigate(TicketDetailRoute(id)) { launchSingleTop = true }` on top of whatever is current, so the
detail always sits above the root of the tab it was opened from. The operator Panel is the home
itself (`homeFor(OPERATOR)` = `OperatorPanelRoute` = `BottomTab.OPERATOR_PANEL.route`); the
citizen home `DashboardRoute` is also the Dashboard tab; the pending citizen's home
`PendingValidationRoute` is not a tab route but is in `BottomTab.DASHBOARD.screens`.

## Approach
Chosen (one slice, `MainActivity.kt` only):
1. **Highlight from the back stack.** Collect `navController.currentBackStack` (a public
   `StateFlow<List<NavBackStackEntry>>` in Navigation 2.9.8) and pick the nearest entry, from the
   top, whose screen belongs to one of the role's tabs; skip the `NavGraph` entry at the bottom
   (its `toTopBarScreen()` would fall to `else` = DASHBOARD). Remove `lastTab` and its
   `SideEffect`. On a tab screen this gives the same tab as `currentTab` today; on a detail or
   report it gives the tab underneath. It survives rotation and process death for free because
   the NavController saves the back stack.
2. **Re-select pops to the root.** In `openTab`: if `tab == selectedTab`, call
   `navController.popBackStack(tab.route, inclusive = false)`; if that pops something, stop.
   Otherwise (a different tab, or the selected tab is already on its root, or its route is not on
   the stack, e.g. the pending citizen on Pending validation) run the existing `navigate` call
   unchanged. Switching tabs keeps save/restore.

Alternatives considered:
- *Keep `lastTab` and set it in `openTab`.* Smaller diff, but the highlight would then be the tab
  tapped, not the stack that was restored; Navigation maps saved state by destination and can
  restore a stack under another key (see Risks), so the highlight could still mismatch.
- *Drop `restoreState` (or `saveState`).* Fixes "cannot leave" by never restoring, but loses each
  tab's stack, which D-016 chose on purpose; the task allows keeping restore.
- *Add a per-tab "owner" field to each route / nested graphs per tab.* The textbook fix, but it
  rewrites the NavHost on the day before the deadline. Out of scope.

## Architecture impact
None structural. `CityFixApp` derives the selected tab from the back stack instead of a saved
variable, and handles re-selecting the current tab. `BottomTab`, `bottomTabsFor`, `homeFor`, the
routes, the NavHost and every screen stay as they are.

## Failure modes
- `popBackStack(route)` with a route not on the stack returns false and logs a warning; the code
  then falls back to the normal `navigate`, so nothing breaks.
- A screen that is in a tab's `screens` but was opened from another tab (Dashboard -> "See
  tickets" -> LIST, which is in `ACTIVE.screens`) highlights Active, as today. Tapping Active then
  does not find `ActiveTicketsRoute` on the stack and switches tabs normally. Pre-existing, kept.
- Empty back stack during the first frame: `selectedTab` is null, the bar shows nothing selected
  for one frame (the bar is hidden while Loading anyway).

## Risks and open questions
- **R1 (API visibility).** `NavController.currentBackStack` must be public in 2.9.8 (it is in the
  2.8/2.9 API reference, but this was not compiled yet). If the build or lint flags it as
  restricted (`RestrictedApi`), the fallback is alternative 1: keep `lastTab` and also set
  `lastTab = tab` in `openTab` (`previousBackStackEntry` only sees one level, so it is not
  enough); record the deviation in `state.md`.
- **R2 (restore keyed by the home).** With `popUpTo(home) { saveState = true }`, Navigation also
  maps the saved stack to the `popUpTo` destination itself (the home) when it has no mapping yet.
  For a citizen this may mean tapping Dashboard after leaving Active -> detail restores the Active
  stack. Not confirmed on a device. With this fix the highlight follows the real stack (Active),
  so the screen and highlight still match, which is what the acceptance asks; changing restore
  behaviour is out of scope. Check on the device during the acceptance run.
- **Q1.** Should tapping the selected tab while on its root also scroll the list to the top (a
  common pattern)? Proposed: no, out of scope.
- **Q2.** On Dashboard -> "See tickets" (LIST) the bar highlights Active, though the list was
  opened from the Dashboard. Keep as today (proposed), or move LIST out of `ACTIVE.screens`?
  Changing it is a D-016 design choice, not this bug.

## Out of scope
- Nested navigation graphs per tab, changes to `BottomTab.screens`, the top bar back arrow,
  scroll-to-top on re-select.
- `ShowScreen.kt`, `OperatorTicketScreen.kt` (D-019 under review in parallel).

## Slices
1. `fix(D-020): highlight the tab that holds the screen and reselect to its root` -
   `MainActivity.kt` only: selected tab from `currentBackStack`, `lastTab` removed, re-select
   branch in `openTab`, comments updated. Build + lint, then the acceptance checks on a device.

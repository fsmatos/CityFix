## Files
| Path | Change | Why |
| --- | --- | --- |
| `app/src/main/java/pt/isec/amov/cityfix/MainActivity.kt` | `selectedTab` from the back stack; remove `lastTab` + `SideEffect`; re-select branch in `openTab`; imports; comments | Root causes 1 and 2 |

No other file changes. No strings, no resources, no routes.

## Interfaces
No public signature changes. `MainBottomBar(tabs, selected, onTabClick)` is called exactly as now.

Sketch (inside `CityFixApp`, replacing lines 432-439 and 456-464; final wording may differ):

```kotlin
// D-020: the selected tab is the one whose part of the back stack holds the current screen: the
// nearest entry from the top that is one of the role's tab screens. On a detail or the report
// form that is the tab it was opened from. The NavGraph entry at the bottom is skipped.
val tabs = bottomTabsFor(role)
val backStack by navController.currentBackStack.collectAsStateWithLifecycle()
val selectedTab = backStack.asReversed()
    .filter { it.destination !is NavGraph }
    .firstNotNullOfOrNull { entry ->
        val entryScreen = entry.destination.toTopBarScreen()
        tabs.firstOrNull { entryScreen in it.screens }
    }

// Usual bottom-navigation pattern (save/restore each tab's stack), plus D-020: tapping the
// selected tab goes back to its own screen. popBackStack returns false when there is nothing
// to pop (already on the root, or the route is not on the stack), and then the usual navigate runs.
val openTab = { tab: BottomTab ->
    val backToRoot = tab == selectedTab && navController.popBackStack(tab.route, inclusive = false)
    if (!backToRoot) {
        navController.navigate(tab.route) {
            popUpTo(homeFor(role)) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
}
```

Notes:
- `currentTab`, `lastTab` and the `SideEffect` (lines 436-439) are used nowhere else (checked):
  remove all three.
- `screen` (from `currentBackStackEntryAsState`) stays for the top bar, FAB and bar visibility.
- Imports: add `androidx.navigation.NavGraph`; remove `androidx.compose.runtime.SideEffect` (its
  only use is line 438). `rememberSaveable` stays (`isExpanded`, `handledRole`, `holdRedirect`).
- `popBackStack(route: T, inclusive: Boolean)` with `T = Any` is the type-safe overload of 2.8+;
  `tab.route` is a `@Serializable` object, as already used by `navigate(tab.route)`.

## Steps
1. Slice 1 - `fix(D-020): highlight the tab that holds the screen and reselect to its root`
   - Replace the `currentTab` / `lastTab` / `SideEffect` / `selectedTab` block with the back-stack
     derivation above; update the comment at line 432-434 (it says "keeps the last tab selected").
   - Replace `openTab` with the re-select version; update its comment.
   - Fix imports.
   - Verify: `./gradlew assembleDebug`, `./gradlew lint` (no `RestrictedApi` on
     `currentBackStack`; if there is one, apply the fallback in 00-plan R1 and log the deviation).
     Then the acceptance checks in `00-plan.md` on a device, operator first (the reported case),
     then citizen, pending, anonymous, and one rotation on a restored detail.

## Conventions
- Plain code with short comments tagged `D-020`, as the rest of `CityFixApp`.
- Keep every forward `navigate` with `launchSingleTop = true`.
- Commit message Conventional Commits with the task ID; no attribution trailers.

## Do not touch
- `ui/screens/ShowScreen.kt`, `ui/screens/OperatorTicketScreen.kt` (D-019 review in parallel).
- `ui/navigation/MainBottomBar.kt` (`BottomTab.screens`), `RoleAccess.kt`, `Destinations.kt`,
  the NavHost destinations, `goHome`, the redirect and notification effects.
- `google-services.json`, Firestore/Storage rules.

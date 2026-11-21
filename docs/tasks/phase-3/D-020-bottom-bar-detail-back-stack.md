# D-020: Bottom bar keeps the wrong tab and cannot leave a detail

Type: bug · Found by Diogo during the manual test checklist (2026-11-21), while testing I.3.E.

## Problem
Steps (operator, the same likely happens for citizens):
1. From the home tab (panel), open a ticket detail.
2. Tap the Profile tab.
3. Tap the Home tab: the app comes back to the **detail**, but the bar highlights **Profile**.
4. Tap Home again: the app stays on the detail instead of going to the panel.

Likely cause (`MainActivity.kt`, `openTab` and `selectedTab`, around lines 435-464):
- `openTab` uses the usual `popUpTo(home) { saveState = true }` + `restoreState = true`. Leaving
  the home tab saves its stack (home + detail); coming back restores it, so the detail reopens.
  Tapping Home again saves and restores the same stack, so the detail never closes.
- The detail is in no tab's `screens`, so `selectedTab` falls back to `lastTab`, which is the
  last tab *screen* shown (Profile), not the tab the detail belongs to.

## Acceptance
- On a screen outside every tab (detail, report form), the bar highlights the tab whose stack
  holds it (the tab you opened it from), never the last tab visited.
- Tapping the tab that is already selected returns to that tab's root screen (detail closed).
- Switching tabs and coming back may keep the tab's stack (restore), but the highlighted tab and
  the screen always match.
- System Back still behaves as now; works for operator, citizen, pending and anonymous roles,
  and after rotation.

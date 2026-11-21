# D-020: Bottom bar keeps the wrong tab and cannot leave a detail
Branch: dev/Diogo · Base: 48b8397
Remote: not pushed

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-21 | Found in the manual checklist while testing I.3.E |
| Plan | done | 2026-11-21 | 00/01/03 written; one slice, MainActivity.kt only; awaiting Gate 1 |
| Gate 1: plan approved | done | 2026-11-21 | Diogo approved; Q1 reselect on root scrolls to top; Q2 keep list -> Active |
| Code | | | |
| Review | | | |
| Docs | | | |
| Pushed | | | |
| Merged | | | |

## Log
- 2026-11-21: started after D-019 code; origin/main already merged for D-019 (e6b34f4).
- 2026-11-21: plan (bug mode). Read MainActivity.kt (`openTab`, `selectedTab`, detail entry points), MainBottomBar.kt, RoleAccess.kt. Root cause confirmed: (1) `lastTab` is the last tab screen shown, so a restored detail highlights Profile; (2) re-selecting the current tab runs the same popUpTo(home){saveState} + restoreState, which pops and immediately restores the detail. Plan: one slice in MainActivity.kt: selected tab from `navController.currentBackStack` (nearest tab screen from the top, NavGraph skipped), `lastTab`/`SideEffect` removed, re-select = `popBackStack(tab.route, inclusive = false)` with fallback to the usual navigate. Risk R1: `currentBackStack` visibility not compiled yet (fallback recorded). Open: Q1 scroll-to-top (no), Q2 LIST highlights Active when opened from Dashboard (keep). ShowScreen/OperatorTicketScreen not touched (D-019 review).
- 2026-11-21: Gate 1 approved by Diogo. Q1 changed from the plan: tapping the selected tab while already on its root scrolls that screen back to the top (added to the coding plan as slice 2). Q2 kept: the list opened from the Dashboard still highlights Active.

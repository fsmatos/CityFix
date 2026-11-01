# F-004: DashboardScreen
Branch: dev/Francisco · Base: 693b05d
Remote: not pushed

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-10-30 | |
| Plan | done | 2026-10-30 | 00-plan, 01-coding-plan, 03-docs-plan; 3 slices; Q1-Q7 decided (revised 2026-10-30) |
| Gate 1: plan approved | done | 2026-11-01 | approved as revised (Q4 "N of M issues", Q5 button, no guards) |
| Code | | | |
| Review | | | |
| Docs | | | |
| Pushed | | | |
| Merged | | | |

## Log
Append-only.
- 2026-10-30: fetched origin; origin/main had no commits missing from dev/Francisco. The person chose not to merge (already up to date). Started F-004 from `docs/tasks/phase-1/F-004-dashboard-screen.md`; dependencies D-001 and D-002 are in the branch.
- 2026-10-30: Plan written (00-plan, 01-coding-plan, 03-docs-plan).
  - Read: the task file, the phase-1 index, the overview (Rules D-002, ViewModel D-003, Navigation D-004, UI foundation, Screens), decision 0005, the D-002 plan and review (handover notes for F-004), `DashboardStats.kt`, `TicketsList.kt`, `TicketsViewModel.kt`, `SampleTickets.kt`, `ListScreen.kt`, `TicketCard.kt`, both string files, the version catalog, the D-005/D-006 task files, and F-003's plans, review and state.
  - Zero-ticket handling is already safe in the model: `activeRatio` is 0.0 when `totalTickets == 0`, and `averageRepairTimeMillis` is `null` when nothing is resolved. The screen adds a NaN/clamp guard anyway.
  - Approach: a stateless `DashboardScreen(stats, onSeeTicketsClick, modifier)` that only formats `DashboardStats`:
    - locale-aware `NumberFormat` with the locale from `LocalConfiguration`;
    - the average in days and hours with `<plurals>`, and "No repairs yet" when `null`;
    - "Based on N repairs" under the average;
    - each card merged for TalkBack;
    - a scrollable column in portrait and a row of three cards in landscape.
  - Slices:
    1. stat cards, strings and plurals (EN + PT), and previews;
    2. the landscape row;
    3. the "See active issues" button (Q5).
  - Open questions with defaults:
    - Q1: no stateful wrapper (same as F-003);
    - Q2: truncated days/hours format;
    - Q3: "0%" with zero tickets;
    - Q4: only the "Based on N repairs" supporting line, and the % label says "not yet repaired";
    - Q5: a dashboard button for dashboard -> list, because D-005's top bar has no list action (handover to D-006 needed);
    - Q6: a row in landscape;
    - Q7: private preview stats.
  - Risk: PT plurals may need a `many` item for lint.
  - The brief PDF was not reread; the I.1.A wording comes from decision 0005.
  - Gate 1 pending.
- 2026-10-30: Plan revised with the person's answers (00-plan, 01-coding-plan, 03-docs-plan); Gate 1 still pending.
  - Q4 changed: keep "Based on N repairs" and add "N of M issues" under % active (new plural `dashboard_active_of_total`, quantity `totalTickets`, numbers via the locale-aware integer `NumberFormat` as `%1$s`/`%2$s`); omitted when `totalTickets == 0`.
  - Q5 confirmed: slice 3 button and `onSeeTicketsClick` stay; overview handover to Diogo (D-006) stays.
  - Guards removed: the screen trusts D-002's `DashboardStats` contract. Percent formats `stats.activeRatio` directly (no `totalTickets <= 0`/`isNaN`/`coerceIn`); "Based on" shows when `averageRepairTimeMillis != null`; `repairTimeText` checks only `null` (no `millis < 0`). The `Int` clamp on the days count stays (type conversion).
  - Q1, Q2, Q3, Q6, Q7 defaults confirmed; all questions now listed as decided in 00-plan. Expected preview texts (e.g. "40%", "12 of 30 issues", "2 de 3 ocorrências") and grep checks (`isNaN|coerceIn|>= 0|< 0` empty) added to 01-coding-plan.
- 2026-11-01: Gate 1 approved by the person after reading the revised plans. Asked to merge origin/main into dev/Francisco before implementing.

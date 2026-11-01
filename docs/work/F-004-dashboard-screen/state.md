# F-004: DashboardScreen
Branch: dev/Francisco · Base: 693b05d
Remote: not pushed

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-10-30 | |
| Plan | done | 2026-10-30 | 00-plan, 01-coding-plan, 03-docs-plan; 3 slices; Q1-Q7 decided (revised 2026-10-30) |
| Gate 1: plan approved | done | 2026-11-01 | approved as revised (Q4 "N of M issues", Q5 button, no guards) |
| Code | done | 2026-11-01 | 3 slices: b52af64, 47a915a, b4e6a81; verify green (lint baseline 0 errors, 13 warnings) |
| Review | done | 2026-11-01 | approve with 1 should-fix (S1 PT landscape preview), 5 nits; 0 blocking; build pass, lint 0 errors, 13 warnings (baseline) |
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
- 2026-11-01: merged origin/main into dev/Francisco (0dfdec0) before coding, as the person asked: 18 commits from D-005 (MainTopAppBar). Conflicts in both strings.xml files, overview.md and report notes, all resolved by keeping both sides; app_name translatable="false" kept in values/ and removed from values-pt/ (D-005 merge note). Verify passed (0 errors, 13-warning baseline). Note: D-005 strings already use "ocorrência(s)" in PT, matching the F-004 plan wording.
- 2026-11-01: Slice 1 done (b52af64): `DashboardScreen(stats, modifier)` with the private `StatCard`, `repairTimeText`, locale from `LocalConfiguration`, both supporting lines, merged card semantics and the six previews; Dashboard strings and the four plurals in EN and PT after the `<!-- Top bar -->` section. Verify: assembleDebug pass; lint 0 errors, 13 warnings (baseline, same IDs; no MissingQuantity/UnusedQuantity/ImpliedQuantity), so the PT `many` item is kept in all four plurals. Plan greps: recompute pattern empty, guard pattern empty, `dashboard_active_of_total` in both string files and the screen. Minor deviation: `repairTimeText` converts days/hours to `Int` before the `when` (plan compared the `Long`s first); same behaviour, one fewer conversion per branch.
- 2026-11-01: Slice 2 done (47a915a): landscape `Row(fillMaxWidth().height(IntrinsicSize.Min))` with each card `weight(1f).fillMaxHeight()`, portrait unchanged, orientation from `LocalConfiguration`. Verify: assembleDebug pass; lint 0 errors, 13 warnings (baseline). Plan greps still empty. Choice (allowed by the plan): first tried a private `StatCards(..., cardModifier)` helper, but lint added a `ModifierParameter` warning (14 warnings) and renaming it to `modifier` would apply one modifier to three children; switched to the plan's other option, three `StatCard` calls in each branch, which brought lint back to the baseline.
- 2026-11-01: Slice 3 done (b4e6a81): `onSeeTicketsClick: () -> Unit` before `modifier`, a centred M3 `Button` "See active issues" / "Ver ocorrências ativas" below the stats, `dashboard_see_tickets` in both string files (placed with the Dashboard `<string>`s, before the plurals), previews pass `onSeeTicketsClick = {}`. Verify: assembleDebug pass; lint 0 errors, 13 warnings (baseline). Plan greps empty; key present in both string files.
- 2026-11-01: Code phase done: 3 slices, 3 commits (b52af64, 47a915a, b4e6a81). Not pushed. Manual checks left for the person in Android Studio: preview texts against the 01-coding-plan table (EN and PT), landscape row with equal-height cards and no clipping, TalkBack reading each card as one item, interactive preview tap and 48dp button. Handover to D-006 (Diogo): wire `onSeeTicketsClick` to `navigate(TicketListRoute)` and pass `innerPadding`.
- 2026-11-01: Review done (review.md), range 03f9615..b4e6a81. Verdict: approve with one should-fix.
  - Reviewer ran assembleDebug (pass) and lint: 0 errors, 13 warnings, the same baseline IDs, none from the dashboard keys.
  - Plan greps (recompute and guard patterns) are empty. No Diogo files changed (`model/`, `viewmodel/`, `data/`, `ui/navigation/` untouched).
  - Both deviations accepted: the `Int` conversion before the `when`, and three `StatCard` calls per branch instead of a helper.
  - S1: no PT landscape preview, so slice 2's PT wrap check cannot be done. Fix: add the PT landscape `@Preview` to `DashboardScreenPreview`.
  - Nits:
    - N1: the stats count private tickets but the button leads to the public list. Rename the button or document the difference (person's call, Q5 wording).
    - N2: `remember(locale)` the `NumberFormat`s.
    - N3: optional edge-case preview (less than 1 hour, 1 of 1 issue).
    - N4: the 48dp is the touch target, the button is 40dp visually; fix the wording in the docs.
    - N5: percent HALF_EVEN rounding at the extremes; document it.

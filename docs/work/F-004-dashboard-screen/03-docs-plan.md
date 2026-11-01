## Docs to update
- `docs/architecture/overview.md`, "Screens" subsection: add a `DashboardScreen` entry after `ListScreen`:
  - the signature `DashboardScreen(stats, onSeeTicketsClick, modifier)` (`ui/screens/DashboardScreen.kt`, F-004), stateless, behind `DashboardRoute` (brief I.1.A);
  - the screen formats `DashboardStats` and recomputes nothing. It trusts the D-002 contract (`activeRatio` never NaN, 0.0 with no tickets; `averageRepairTimeMillis` `null` when nothing is resolved) and adds no guard of its own, so any change to `DashboardStats` must keep that contract. The formats are locale-aware with the locale from `LocalConfiguration`, and the average shows days and hours with plurals or "No repairs yet" when `null`;
  - the supporting lines: "Based on N repairs" under the average (only when it is not `null`) and "N of M issues" under % active (from `activeTickets`/`totalTickets`, numbers in the locale-aware integer format, omitted when `totalTickets == 0`);
  - the layout: a column in portrait and a row of three cards in landscape, scrollable, with each card merged for TalkBack;
  - the D-006 contract from `01-coding-plan.md`: collect `vm.stats`, and wire `onSeeTicketsClick` to `navigate(TicketListRoute)` (Q5 handover to Diogo). Repeat the F-003 N3 inset note for landscape;
  - the Gate 1 outcomes for Q1-Q7 and the "no screen guards" decision.
- `docs/architecture/overview.md`, "UI foundation", Strings bullet: add one sentence saying that plurals use `<plurals>` with `pluralStringResource`, that PT needs `one`/`many`/`other` (or whatever lint accepted in slice 1), and that a plural whose numbers are pre-formatted (like `dashboard_active_of_total`) uses `%1$s`/`%2$s` while the quantity argument stays an `Int`.
- `docs/tasks/phase-1/F-004-dashboard-screen.md`: tick the acceptance checks only after the manual Android Studio checks pass (same rule as F-001 to F-003).
- `docs/work/F-004-dashboard-screen/state.md`: phase rows and log, including the Q5 handover line.

## Decisions to record
- No ADR expected. Write one only if Gate 1 picks a non-default with lasting impact: the Q1 alternative (a stateful wrapper, which changes the convention for all screens), or a Q5 outcome that moves dashboard navigation into the top bar (that would also affect D-005).

## Report notes
Add a section "F-004: DashboardScreen" to `docs/report/notes.md`:
- Technical solutions:
  - a stateless screen that only formats `DashboardStats`;
  - locale-aware `NumberFormat` (integer and percent), with the locale taken from the Compose configuration so previews and the device agree;
  - duration split into days and hours, with Android plurals (the first `<plurals>` in the project) and a "no repairs yet" placeholder for `null`;
  - no defensive guards in the UI: the screen relies on the model's documented never-NaN / `null` contract, so each rule has one owner;
  - "N of M issues" as a pluralised supporting line that reuses the total card's integer format;
  - the landscape row with `IntrinsicSize.Min` for equal-height cards;
  - `semantics(mergeDescendants = true)` per card for TalkBack;
  - `verticalScroll` so nothing is clipped at large font scales.
- Options chosen: the Q2-Q7 outcomes (including Q4: both supporting lines, "N of M" omitted with zero tickets), and why the formatting stays in the UI and not in `utils/` (it needs resources, and `utils/` stays pure Kotlin).
- Rejected: decimal days, `FlowRow`, window size classes in phase 1, recomputing stats in the screen, a duplicate NaN/clamp guard in the UI, and "0 of 0 issues".
- Improvement opportunities:
  - a median repair time, or excluding outliers;
  - small charts (active vs resolved);
  - per-category figures;
  - window-size-based layouts for tablets;
  - an aggregate Firestore stats document for anonymous users in the Firebase phase (decision 0005).

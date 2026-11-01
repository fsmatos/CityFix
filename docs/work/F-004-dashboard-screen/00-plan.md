# F-004: DashboardScreen
Type: feature · Task file: docs/tasks/phase-1/F-004-dashboard-screen.md
Labels: none

## Problem
Phase 1 needs the screen behind `DashboardRoute` (D-004), which is the start destination. It is the anonymous citizen's dashboard (brief I.1.A): average repair time, total number of issues ("nº total de ocorrências") and % active issues ("% ocorrências ativas"). The numbers already exist: `TicketsList.stats()` (D-002, decision 0005) computes a `DashboardStats`, and `TicketsViewModel.stats` (D-003) exposes it as a `StateFlow`. What is missing is the screen that formats and shows them, handles "no tickets" and "no repairs yet", and works in both orientations.

What the model gives (`model/DashboardStats.kt`):
- `totalTickets: Int`, `activeTickets: Int`, `resolvedTickets: Int` (tickets that entered the average);
- `averageRepairTimeMillis: Long?`, in milliseconds, `null` when no ticket has been resolved;
- the computed `activeRatio: Double`, 0.0..1.0. It is 0.0 when `totalTickets == 0`, so it is never NaN.

With the sample data: total 3, active 2 (67%), 1 repair, average exactly 3 days (259 200 000 ms).

## Outcome
A stateless composable `DashboardScreen(stats, onSeeTicketsClick, modifier)` in `ui/screens/DashboardScreen.kt`:
- three stat cards in the brief's order: average repair time, total issues, % active issues. Each card has a label, a large value and an optional supporting line (used by the average and the % active cards);
- average repair time formatted as days and hours with plurals ("3 days", "1 day and 4 hours", "5 hours", "Less than 1 hour"). With `null` it shows "No repairs yet". The supporting line "Based on N repairs" appears when the average is not `null`;
- total formatted with a locale-aware integer format, and % active with a locale-aware percent format (EN "67%", PT "67 %"). The percent formats `stats.activeRatio` as given: the model documents that it is never NaN and is 0.0 with no tickets;
- under % active, the supporting line "N of M issues" (`activeTickets` of `totalTickets`, e.g. "2 of 3 issues" / "2 de 3 ocorrências"). It is omitted when `totalTickets == 0`;
- portrait: the cards stacked in a column. Landscape: the three cards side by side in one row. Both scroll vertically, so nothing is clipped at large font sizes;
- a "See active issues" button that calls `onSeeTicketsClick` (Q5), so D-006 can navigate dashboard -> list;
- each card is merged for TalkBack and read as "label, value, supporting line";
- previews with sample data and with zero tickets, in portrait and landscape, plus PT previews. All strings are in EN and PT.

D-006 wires it like this: `DashboardScreen(stats = vm.stats.collectAsStateWithLifecycle().value, onSeeTicketsClick = { navController.navigate(TicketListRoute) }, modifier = Modifier.padding(innerPadding))`.

## Acceptance checks
- [ ] The sample stats preview shows all three values from `DashboardStats`: average repair time, total and % active (task check 1). With `previewStats` it shows "3 days and 4 hours", "Based on 8 repairs", "30", "40%" and "12 of 30 issues".
- [ ] The zero-ticket preview (`DashboardStats()`) shows total "0", % active "0%", and "No repairs yet" for the average, with no "Based on" line and no "N of M issues" line. It shows no "NaN", no "∞" and no crash (task check 2).
- [ ] `@Preview` exists in portrait and landscape for both the sample and the zero-ticket states. In landscape the three cards sit in one row, and nothing is clipped (task check 3).
- [ ] With the PT preview (`locale = "pt"`) every label, the duration and the supporting lines are in Portuguese, and the percent uses the PT format: the sample-figures PT preview shows "3 dias", "Com base em 1 reparação", "67 %" and "2 de 3 ocorrências".
- [ ] The duration formatting gives "3 days" for 259 200 000 ms, "1 day and 4 hours" for 100 800 000 ms and "Less than 1 hour" for 1 800 000 ms. Check this with preview variants or by reading the code path.
- [ ] TalkBack (or the Android Studio accessibility inspector) reads each card as one item, with the label, the value and the supporting line together.
- [ ] Tapping "See active issues" calls `onSeeTicketsClick`. Check it in the interactive preview, or after D-006 by opening the list.
- [ ] `./gradlew assembleDebug` and `./gradlew lint` pass with no new warnings (no `MissingTranslation`, no `MissingQuantity`).
- [ ] `DashboardScreen.kt` does not compute stats itself. It reads only `DashboardStats` fields, never a `Ticket`, and adds no guard on `activeRatio` (no `isNaN`/`coerceIn`).

## Approach
**Stats stay upstream, and the screen trusts their contract.** `TicketsList.stats()` is the only place that decides what is "active" and what counts as a repair (decision 0005). The screen formats the values and recomputes nothing. It relies on the documented `DashboardStats` contract (D-002) instead of re-checking it:
- `activeRatio` is in 0.0..1.0 and never NaN (0.0 when `totalTickets == 0`), so the percent formats `stats.activeRatio` directly, with no `totalTickets <= 0`, `isNaN()` or `coerceIn` guard;
- `averageRepairTimeMillis` is `null` when nothing has been resolved and is never negative (the model skips negative durations), so `null` is the only special case.
One place owns each rule; duplicating the checks in the UI would hide a model bug instead of exposing it, and the contract is what we explain at the defence.

**Formatting, private to `DashboardScreen.kt`.**
- Locale: `val locale = LocalConfiguration.current.locales[0]` (`Configuration.getLocales()` is API 24, our minSdk). The number formats then follow the app's configuration, and the `locale = "pt"` preview really shows "67 %". F-002's `formatDate` uses `Locale.getDefault()`, which ignores the preview locale (F-002 review S3).
- One integer formatter: `val integerFormat = NumberFormat.getIntegerInstance(locale)`.
- Total: `integerFormat.format(stats.totalTickets)`.
- % active: `NumberFormat.getPercentInstance(locale).format(stats.activeRatio)`. It expects 0..1, rounds to whole percent, and is locale-aware.
- Average repair time, in a private `@Composable fun repairTimeText(millis: Long?): String`:
  - `null` -> `stringResource(R.string.dashboard_repair_time_none)` ("No repairs yet");
  - otherwise `days = millis / DAY_MILLIS` and `hours = (millis % DAY_MILLIS) / HOUR_MILLIS`. Both truncate, see Q2. Then:
    - `days == 0L && hours == 0L` -> "Less than 1 hour";
    - `days > 0 && hours == 0L` -> plural days ("3 days");
    - `days == 0L` -> plural hours ("5 hours");
    - both -> `dashboard_duration_days_hours` "%1$s and %2$s" / "%1$s e %2$s" with the two plurals.
  - Plurals through `pluralStringResource(R.plurals.x, count, count)`, which is stable in the Compose BOM 2026.02. The count is `Int`, so the code uses `days.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()` (a type conversion, not a contract guard: a `Long` of days can exceed `Int`).
- Supporting line under the average: `pluralStringResource(R.plurals.dashboard_based_on_repairs, stats.resolvedTickets, stats.resolvedTickets)`, only when `averageRepairTimeMillis != null`. This plural uses `%d`, like the days/hours plurals.
- Supporting line under % active: `pluralStringResource(R.plurals.dashboard_active_of_total, stats.totalTickets, integerFormat.format(stats.activeTickets), integerFormat.format(stats.totalTickets))`, only when `stats.totalTickets > 0`.
  - It pluralises on `totalTickets` ("1 of 1 issue", "2 of 3 issues").
  - Number format: the arguments are `%1$s`/`%2$s` filled with the same locale-aware `integerFormat` used for the total card, so "M" in "N of M issues" always looks exactly like the total value above it (grouping separators included). The other plurals keep `%d`, because their counts are small (repairs, days, hours) and they have no neighbouring formatted value to match.
  - When `totalTickets == 0` the line is omitted: "0 of 0 issues" would only repeat the "0" total and the "0%" next to it.

**Layout.**
- Root: `Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = spacedBy(12.dp))`. `rememberScrollState` is saveable UI element state, not screen data, so it does not break the "stateless" rule. It keeps the scroll position across rotation.
- A private `StatCard(label, value, supporting: String?, modifier)` builds each card: an M3 `Card` (not clickable) holding a `Column(Modifier.padding(16.dp))` with the label in `titleMedium`/`onSurfaceVariant`, the value in `headlineMedium`, and the supporting text in `bodySmall`. `Modifier.semantics(mergeDescendants = true) {}` on the card makes TalkBack read it as one item.
- Card usage: average -> `supporting = basedOn` (or `null`); total -> `supporting = null`; % active -> `supporting = activeOfTotal` (or `null` when there are no tickets).
- Orientation: `LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE` (as in P07).
  - Portrait: the three `StatCard`s full width, one under the other.
  - Landscape: `Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), spacedBy(12.dp))` with each card `Modifier.weight(1f).fillMaxHeight()`, so the cards share the width and have equal heights when a PT label wraps or only some cards have a supporting line.
  - Both are followed by the `Button` (slice 3).
- The `modifier` is the last parameter and is applied once, to the root.

**No stateful wrapper.** This is the same decision as F-003 Gate 1 Q1 and the overview "Screens" section. `DashboardScreen` is the stateless half, and D-006 collects `vm.stats` in `composable<DashboardRoute>`.

Alternatives considered:
- Format the average as a decimal number of days ("3.2 days"). Rejected: plural rules with decimals are awkward in resources, and "3 days and 4 hours" is clearer.
- Put a `formatDuration` helper in `utils/`. Rejected: it needs string resources, and `utils/` must stay pure Kotlin because `model/` imports it (decision 0005). A pure "split into days/hours" helper would be fine there, but it is two lines, so it stays private in the screen.
- Guard the ratio in the screen as well (NaN check and clamp). Rejected at Gate 1: the model already guarantees it, and one owner per rule is easier to explain.
- Use `BoxWithConstraints`/window size classes for landscape. Rejected: orientation via `LocalConfiguration` is what P07 teaches and is enough for a phone. A width-based switch is an improvement for later.
- Use `FlowRow` instead of choosing Column or Row. Rejected: it is less predictable and harder to explain at the defence.
- Show "—" for % active when there are no tickets. Rejected (Q3): "0%" is what the model documents.
- Show "0 of 0 issues" with zero tickets. Rejected (Q4): it repeats the total and the percent.

## Architecture impact
- New file `ui/screens/DashboardScreen.kt`. The task names only this file, and all helpers stay `private` in it.
- New string keys in `values/strings.xml` and `values-pt/strings.xml`. These are the first `<plurals>` in the project (four of them).
- No change to `model/`, `viewmodel/`, `data/`, navigation, theme, Gradle or the manifest. No new dependency: `pluralStringResource`, `LocalConfiguration`, `verticalScroll`, `semantics` and `IntrinsicSize` are all in the existing Compose BOM.
- Gives D-006 the call it needs for `composable<DashboardRoute>`, with a navigation callback (Q5).

## Failure modes
- **NaN or infinite percent.** Prevented by the model: `activeRatio` is 0.0 when `totalTickets == 0` and never NaN (D-002 contract). The screen relies on that contract and adds no guard; a model regression would show up as "NaN" in the zero-ticket preview, which acceptance check 2 covers.
- **`averageRepairTimeMillis == null`** shows "No repairs yet", never "0 days", and hides the "Based on" line.
- **Negative average.** The model skips negative durations, so by contract it cannot happen; the screen does not re-check it.
- **Zero tickets and "N of M".** The line is omitted when `totalTickets == 0`, so there is no "0 of 0 issues".
- **Very large values** are clamped before the `Int` plural count, so there is no overflow crash. `activeTickets` and `totalTickets` are already `Int`.
- **PT plurals.** Android's `pt` locale has the plural categories `one`, `many` and `other`. The PT plurals therefore include a `many` item equal to `other`. Slice 1 checks lint (`MissingQuantity`/`UnusedQuantity`) and keeps or drops `many` to match what lint accepts, for all four plurals.
- **Plural with `%s` arguments.** `dashboard_active_of_total` pluralises on `totalTickets` but receives formatted strings; lint's `ImpliedQuantity` only needs each `one` item to contain the placeholder, which it does.
- **Large font scale or long PT labels in landscape.** Labels and supporting lines wrap inside the weighted cards, and the root scrolls, so nothing is clipped.
- **Wrong preview locale.** Formats built with `Locale.getDefault()` would show EN-style numbers in the PT preview. This is avoided by reading the locale from `LocalConfiguration`.
- **Edge-to-edge.** Without D-006 passing `innerPadding`, the first card could sit under the status bar, and in landscape under a camera cutout (F-003 review N3). That is D-006's wiring.

## Decisions (Gate 1 revision, 2026-10-30)
The person answered every question. Gate 1 itself (approval of the revised plan) is still pending.

- **Q1. Signature and stateful split. Decided (default).** `DashboardScreen(stats: DashboardStats, onSeeTicketsClick: () -> Unit, modifier: Modifier = Modifier)`, stateless, with no stateful wrapper. This is consistent with F-003 Gate 1 Q1 and the overview "Screens" section, and D-006 collects `vm.stats`.
- **Q2. Average repair time format. Decided (default).** Whole days and hours, both truncated: "3 days", "1 day and 4 hours", "5 hours", "Less than 1 hour", with plurals (`dashboard_days`, `dashboard_hours`); `null` shows "No repairs yet" / "Ainda sem reparações".
- **Q3. % active with zero tickets. Decided (default).** Show "0%", as the model defines `activeRatio = 0.0` for no tickets; the total "0" next to it makes the meaning clear.
- **Q4. Supporting lines. Decided (changed from the default).** Both lines:
  - "Based on N repairs" / "Com base em N reparações" under the average, when `averageRepairTimeMillis != null` (uses `resolvedTickets`);
  - "N of M issues" / "N de M ocorrências" under % active, reading `stats.activeTickets` and `stats.totalTickets` directly, pluralised on `totalTickets`, numbers formatted with the locale-aware integer `NumberFormat` (`%1$s`/`%2$s`), omitted when `totalTickets == 0`.
  - The % active label stays "Active issues (not yet repaired)" / "Ocorrências ativas (por reparar)", as the D-002 review suggested, because "active" excludes RESOLVED.
- **Q5. Navigation from the dashboard. Decided (default).** Keep slice 3: a `Button` "See active issues" / "Ver ocorrências ativas" calling `onSeeTicketsClick: () -> Unit`. The overview "Screens" entry tells Diogo (D-006) to wire it to `navigate(TicketListRoute)`, since D-005's top bar has no list action.
- **Q6. Landscape layout. Decided (default).** The three cards in one row with equal widths and equal heights, and the button below; orientation from `LocalConfiguration` (P07).
- **Q7. Preview data. Decided (default).** Private values in `DashboardScreen.kt`:
  - `previewStats = DashboardStats(totalTickets = 30, activeTickets = 12, resolvedTickets = 8, averageRepairTimeMillis = 3 days + 4 hours)`, which shows "3 days and 4 hours", "Based on 8 repairs", "30", "40%" and "12 of 30 issues";
  - `DashboardStats()` for the zero-ticket state: "No repairs yet", "0", "0%", and no supporting lines;
  - `sampleFigureStats` with the sample figures (3, 2, 1, exactly 3 days): "3 days", "Based on 1 repair", "3", "67%", "2 of 3 issues" (PT "3 dias", "Com base em 1 reparação", "3", "67 %", "2 de 3 ocorrências"). It checks the singular "1 repair" and the "days only" branch.
- **Guards. Decided.** No screen-side guard on `activeRatio` or on a negative average; the screen trusts the D-002 `DashboardStats` contract (see Approach).

Other risks:
- The brief PDF was not reread for this plan. The I.1.A wording above ("tempo médio de reparação", "nº total de ocorrências", "% ocorrências ativas") comes from decision 0005 and the D-002 plan, which quoted it. Check I.1.A against the PDF at Gate 1 if in doubt.
- Q5 adds a parameter to the agreed `DashboardScreen(stats, ...)` signature. The phase-1 index leaves `...` open, but Diogo must know about it. The overview "Screens" entry is the handover, as in F-003.
- Without the screen guard, a future change to `DashboardStats` that breaks its contract would show directly in the UI. Any such change is Diogo's (D-002) and must keep the documented contract.
- No preview has `totalTickets == 1`, so the singular "1 of 1 issue" is checked by reading the resource, not visually.
- Manual Android Studio checks (previews, PT, landscape, TalkBack, the interactive tap) can only be done by the person. The implementer verifies build and lint only.

## Out of scope
- Collecting the ViewModel, the `NavHost`, the `Scaffold` and the top bar (D-005, D-006).
- Any change to how the stats are computed (`TicketsList.stats()`, `DashboardStats`; Diogo's D-002).
- Charts, a per-category breakdown, a median repair time, or refresh/pull-to-refresh.
- Role-specific dashboards (registered citizen, operator), and the aggregate Firestore source anonymous users will need in the Firebase phase (decision 0005).
- Window size classes and tablet layouts.

## Slices
1. **Stats in a column.**
   - Add the strings (EN + PT, with the four plurals).
   - Write `DashboardScreen(stats, modifier)` with the private `StatCard`, the formatting (locale from `LocalConfiguration`, integer and percent formats on `stats.activeRatio` directly, `repairTimeText`, the "Based on N repairs" line and the "N of M issues" line) and the merged semantics.
   - Lay the cards out in a scrolling `Column` in both orientations.
   - Add the previews: sample in portrait and landscape, zero tickets in portrait and landscape, PT, and the sample figures.
   - Build and lint green. This alone satisfies the three task checks.
2. **Landscape row.** Switch to a `Row` of equal-weight, equal-height cards when `LocalConfiguration.current.orientation` is landscape. The landscape previews now show one row. Build green.
3. **"See active issues" button (Q5, confirmed).** Add the `onSeeTicketsClick` parameter (before `modifier`), the `Button` below the stats and the `dashboard_see_tickets` string (EN + PT), and update the previews. Build and lint green.

# F-004: DashboardScreen — review
Reviewed: 2026-11-01 · Range: `03f9615..b4e6a81` (b52af64, 47a915a, b4e6a81) plus `state.md` (b7be620)
Against: 00-plan and 01-coding-plan (Gate 1 approved 2026-11-01 as revised), task file `docs/tasks/phase-1/F-004-dashboard-screen.md`, brief I.1.A (as quoted in decision 0005), `docs/architecture/overview.md`, kotlin-conventions.

**Verdict: approve with one should-fix.** Nothing blocks. The code follows the approved plan almost line for line, and both recorded deviations are sound.

## What was checked
- **Build and lint (run by the reviewer):** `./gradlew assembleDebug` passes. `./gradlew lint` gives 0 errors and 13 warnings, the same baseline: AndroidGradlePluginVersion 1, GradleDependency 1, NewerVersionAvailable 3, RedundantLabel 1, UnusedResources 7. None is a `dashboard_*` key, and there is no MissingTranslation, MissingQuantity, UnusedQuantity or ImpliedQuantity.
- **Files touched:** only `ui/screens/DashboardScreen.kt` and the two `strings.xml` files (plus `state.md`). Nothing changed under `model/`, `viewmodel/`, `data/` or `ui/navigation/`. `DashboardStats.kt` (Diogo) is untouched.
- **Plan greps:** the recompute pattern `Ticket\b|\.filter|isActive|sumOf|average\(` matches nothing. The guard pattern `isNaN|coerceIn|>= 0|< 0` matches nothing. `dashboard_active_of_total` is in both string files and in the screen.
- **DashboardStats fields:** each field is used once, for the purpose the plan gives it:
  - `totalTickets`: the total card, the plural quantity and the `> 0` condition;
  - `activeTickets`: the "N of M" line;
  - `resolvedTickets`: "Based on N";
  - `averageRepairTimeMillis`: the duration and the `!= null` condition;
  - `activeRatio`: the percent, formatted directly with no guard, as Gate 1 decided.
- **Duration maths:** `days = millis / DAY_MILLIS` and `hours = (millis % DAY_MILLIS) / HOUR_MILLIS` both truncate (Q2). Tracing the code gives:
  - 259 200 000 ms -> "3 days";
  - 100 800 000 ms -> "1 day and 4 hours";
  - 1 800 000 ms -> "Less than 1 hour".

  The `Int` conversion clamps days, and hours is always below 24.
- **Plurals:** EN has `one`/`other`. PT has `one`/`many`/`other` with `many` = `other`, and lint accepts it. The CLDR `pt` rule for `one` is `i = 0..1`, so a count of 0 would pick "0 reparação". No call site can pass 0:
  - `resolvedTickets` is at least 1 whenever the average is non-null (D-002 contract);
  - `totalTickets` is greater than 0 by the guard;
  - days and hours are greater than 0 in their branches.

  Keep this in mind if a 0 count is ever shown.
- **Locale:** `LocalConfiguration.current.locales[0]` (API 24) feeds both `NumberFormat`s, so the PT preview gets "67 %". `pluralStringResource` and `stringResource` format with the resources' configuration locale, so the `%d` values match it.
- **MVVM / stateless:** no ViewModel, repository or `mutableStateOf`. The only remembered state is `rememberScrollState()`. This matches F-003 Q1 and the overview "Screens" section.
- **Anonymous data:** the screen shows only aggregates. It reads no `Ticket`, so there is no author data and no coordinates.
- **Accessibility:**
  - each `Card` has `semantics(mergeDescendants = true) {}`, so TalkBack reads "label, value, supporting";
  - the M3 `Button` gets a 48dp touch target from `minimumInteractiveComponentSize`;
  - colours come from `MaterialTheme`.
- **Orientation:**
  - the landscape `Row(fillMaxWidth().height(IntrinsicSize.Min))` uses `weight(1f).fillMaxHeight()` cards, which gives them equal widths and equal heights;
  - the root `Column` scrolls in both orientations, so nothing is clipped;
  - `rememberScrollState` keeps the scroll position across rotation.
- **Consistency with ListScreen (F-003):** the parameter order (data, callbacks, `modifier` last), the previews (`private`, `CityFixTheme`), the landscape `spec:` string and the 16dp padding are all the same.
- **Preview coverage against the 01-coding-plan table:** all five rows are covered:
  - `previewStats` EN, portrait and landscape;
  - `DashboardStats()` EN portrait and landscape, and PT;
  - `sampleFigureStats` EN and PT.

## Deviations (from state.md)
- **Slice 1: `repairTimeText` converts to `Int` before the `when`.** Accepted. The behaviour is identical: hours is always below 24 and days is clamped. Each branch has one conversion fewer, and the code is easier to read aloud at the defence.
- **Slice 2: three `StatCard` calls in each branch instead of a `StatCards(cardModifier)` helper.** Accepted. The plan allowed either option. The helper added a `ModifierParameter` lint warning, and renaming its parameter to `modifier` would have applied one modifier to three children, which is wrong. The duplicated calls are explicit and easy to explain.

## Blocking
None.

## Should-fix
**S1. No PT landscape preview, so slice 2's manual check cannot be done.**
- 01-coding-plan slice 2 "Verify" asks the person to check that "the PT landscape label and '12 de 30 ocorrências'-style lines wrap without clipping".
- Problem: the only landscape previews are EN (`DashboardScreenPreview` and `DashboardScreenEmptyPreview`).
- Why it matters: the longest label, "Ocorrências ativas (por reparar)", inside a third of the width, is exactly the clipping and equal-height risk the plan names. No preview renders it.
- Fix: add one annotation to `DashboardScreenPreview`, so the PT label and "12 de 30 ocorrências" render in the row:
  ```kotlin
  @Preview(showBackground = true, locale = "pt", device = "spec:width=411dp,height=891dp,orientation=landscape")
  ```
- Optionally, also add a plain `@Preview(showBackground = true, locale = "pt")` there for PT "3 dias e 4 horas" in portrait.

## Nits
**N1. The "See active issues" button counts a different set of tickets than the dashboard figures.**
- `stats()` counts every ticket, private ones included (decision 0005).
- The list behind the button shows only PUBLIC active tickets (`publicActive()`).
- Once private tickets exist, "12 of 30 issues" can lead to a list of 9. Today this is invisible, because all three samples are PUBLIC.
- The wording was approved at Gate 1 (Q5), so this is the person's call.
- Fix: either rename the button to "See public issues" / "Ver ocorrências públicas" (`dashboard_see_tickets` in both files), or keep the wording and add one sentence to the overview "Screens" entry (docs phase) explaining the difference, ready for the defence.

**N2. A new `NumberFormat` is built on every recomposition.**
- Cheap, but avoidable.
- Fix: `val integerFormat = remember(locale) { NumberFormat.getIntegerInstance(locale) }` and `val percentFormat = remember(locale) { NumberFormat.getPercentInstance(locale) }`. This keeps the formatter keyed to the configuration locale.

**N3. Three code paths have no preview.**
- Paths without a preview:
  - the "hours only" branch;
  - the "Less than 1 hour" branch;
  - the singular "1 of 1 issue".
- The plan accepts checking these by reading the code.
- Fix (optional): add `private val edgeStats = DashboardStats(totalTickets = 1, activeTickets = 1, resolvedTickets = 1, averageRepairTimeMillis = 30 * 60_000L)` and a `DashboardScreenEdgePreview` (EN and PT). It shows "Less than 1 hour", "Based on 1 repair", "100%" and "1 of 1 issue" / "1 de 1 ocorrência".

**N4. The 48dp wording in the plan and in state.md is imprecise.**
- The slice 3 verify step and the state.md log say the button is "at least 48dp tall (M3 default)".
- In fact, an M3 `Button` is 40dp tall visually. Its touch target is 48dp, through `minimumInteractiveComponentSize`.
- Fix: in the docs phase, write "48dp touch target (M3 minimum interactive size)", so the defence answer is accurate.

**N5. Percent rounding at the extremes.**
- `getPercentInstance` rounds to whole percent with HALF_EVEN. So 1 of 300 shows "0%" and 299 of 300 shows "100%".
- The "N of M issues" line underneath makes the real figure clear, so no code change is needed.
- Fix: mention this in the docs phase, next to Q3, as the reason the supporting line exists. If a finer figure is ever wanted, use `percentFormat.maximumFractionDigits = 1`.

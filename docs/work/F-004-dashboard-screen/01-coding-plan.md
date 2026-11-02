## Files
Paths are relative to `app/src/main/`. The Kotlin package root is `java/pt/isec/amov/cityfix/`.

| Path | Change | Why |
| --- | --- | --- |
| `java/pt/isec/amov/cityfix/ui/screens/DashboardScreen.kt` | New: `DashboardScreen`, the private `StatCard`, formatting helpers, preview data and previews | Anonymous dashboard (brief I.1.A), slices 1-3 |
| `res/values/strings.xml` | Add the keys below under a `<!-- Dashboard -->` comment | EN labels (slices 1 and 3) |
| `res/values-pt/strings.xml` | Add the same keys | PT labels (slices 1 and 3) |

String keys (EN / PT), using the Gate 1 decisions (00-plan "Decisions"):
- Slice 1, `<string>`:
  - `dashboard_repair_time_label`: Average repair time / Tempo médio de reparação
  - `dashboard_total_label`: Total issues / Total de ocorrências
  - `dashboard_active_label`: Active issues (not yet repaired) / Ocorrências ativas (por reparar)
  - `dashboard_repair_time_none`: No repairs yet / Ainda sem reparações
  - `dashboard_duration_less_than_hour`: Less than 1 hour / Menos de 1 hora
  - `dashboard_duration_days_hours`: %1$s and %2$s / %1$s e %2$s
- Slice 1, `<plurals>` (EN items `one` and `other`; PT items `one`, `many` and `other`, where `many` equals `other`, see 00-plan Failure modes):
  - `dashboard_days`: %d day / %d days; PT %d dia / %d dias
  - `dashboard_hours`: %d hour / %d hours; PT %d hora / %d horas
  - `dashboard_based_on_repairs`: Based on %d repair / Based on %d repairs; PT Com base em %d reparação / Com base em %d reparações
  - `dashboard_active_of_total` (quantity = `totalTickets`; arguments are strings already formatted with the locale-aware integer `NumberFormat`, hence `%1$s`/`%2$s`, not `%d`): %1$s of %2$s issue / %1$s of %2$s issues; PT %1$s de %2$s ocorrência / %1$s de %2$s ocorrências
- Slice 3, `<string>`:
  - `dashboard_see_tickets`: See active issues / Ver ocorrências ativas

## Interfaces
Package `pt.isec.amov.cityfix.ui.screens`:
```kotlin
/**
 * Anonymous dashboard (brief I.1.A): average repair time, total issues and % active issues.
 * Formats the given DashboardStats only; the figures are computed upstream (TicketsList.stats())
 * and their contract (activeRatio never NaN, null average when nothing is resolved) is trusted.
 */
@Composable
fun DashboardScreen(
    stats: DashboardStats,
    onSeeTicketsClick: () -> Unit,   // added in slice 3 (Q5)
    modifier: Modifier = Modifier,
)

// private in the same file
@Composable
private fun StatCard(label: String, value: String, supporting: String?, modifier: Modifier = Modifier)

@Composable
private fun repairTimeText(millis: Long?): String

private const val HOUR_MILLIS = 3_600_000L
private const val DAY_MILLIS = 86_400_000L
```

Caller (D-006, not part of this task):
```kotlin
composable<DashboardRoute> {
    DashboardScreen(
        stats = stats,                       // vm.stats.collectAsStateWithLifecycle().value, collected above the NavHost
        onSeeTicketsClick = { navController.navigate(TicketListRoute) },
        modifier = Modifier.padding(innerPadding),
    )
}
```

## Steps
1. **Stats in a column.**
   - Strings: add every slice-1 key to both string files in the same commit. Put the `<plurals>` after the `<string>`s under `<!-- Dashboard -->`.
   - Create `ui/screens/DashboardScreen.kt`.
   - `DashboardScreen(stats, modifier)`. No `onSeeTicketsClick` yet: it arrives in slice 3, so this slice stays self-contained.
     - `val configuration = LocalConfiguration.current` and `val locale = configuration.locales[0]`.
     - `val integerFormat = NumberFormat.getIntegerInstance(locale)`.
     - `val total = integerFormat.format(stats.totalTickets)`.
     - `val percent = NumberFormat.getPercentInstance(locale).format(stats.activeRatio)`. No guard: `activeRatio` is never NaN and is 0.0 with no tickets (D-002 contract).
     - `val average = repairTimeText(stats.averageRepairTimeMillis)`.
     - `val basedOn = if (stats.averageRepairTimeMillis != null) pluralStringResource(R.plurals.dashboard_based_on_repairs, stats.resolvedTickets, stats.resolvedTickets) else null`.
     - `val activeOfTotal = if (stats.totalTickets > 0) pluralStringResource(R.plurals.dashboard_active_of_total, stats.totalTickets, integerFormat.format(stats.activeTickets), integerFormat.format(stats.totalTickets)) else null`. Omitted with zero tickets (no "0 of 0 issues").
     - Root: `Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp))` with three `StatCard`s, each `Modifier.fillMaxWidth()`, in the brief's order:
       - average: `StatCard(stringResource(R.string.dashboard_repair_time_label), average, basedOn)`;
       - total: `StatCard(stringResource(R.string.dashboard_total_label), total, null)`;
       - % active: `StatCard(stringResource(R.string.dashboard_active_label), percent, activeOfTotal)`.
   - `StatCard`:
     - `Card(modifier = modifier.semantics(mergeDescendants = true) {})` holding `Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp))`;
     - label: `Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)`;
     - value: `Text(value, style = MaterialTheme.typography.headlineMedium)`;
     - supporting: `supporting?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }`.
   - `repairTimeText(millis)`:
     - if `millis == null`, return `stringResource(R.string.dashboard_repair_time_none)`. No negative check: the model never produces a negative average;
     - `val days = millis / DAY_MILLIS` and `val hours = (millis % DAY_MILLIS) / HOUR_MILLIS`;
     - convert to `Int` with `.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()` (a `Long` of days can exceed `Int`; hours is always under 24);
     - `when`: both 0 -> `dashboard_duration_less_than_hour`; `hours == 0` -> days plural; `days == 0` -> hours plural; else `stringResource(R.string.dashboard_duration_days_hours, daysText, hoursText)`.
     - It is a `@Composable` function that returns a value, so its name starts lowercase (Compose naming rule for value-returning composables).
   - Preview data (private), Q7:
     - `previewStats = DashboardStats(totalTickets = 30, activeTickets = 12, resolvedTickets = 8, averageRepairTimeMillis = 3 * DAY_MILLIS + 4 * HOUR_MILLIS)`;
     - `sampleFigureStats = DashboardStats(totalTickets = 3, activeTickets = 2, resolvedTickets = 1, averageRepairTimeMillis = 3 * DAY_MILLIS)`.
   - Previews, all `private` and wrapped in `CityFixTheme`, with the same annotations as `ListScreen.kt`:
     - `DashboardScreenPreview`: `@Preview(showBackground = true)` and `@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,orientation=landscape")`, with `previewStats`.
     - `DashboardScreenEmptyPreview`: portrait, the landscape `spec:` and `@Preview(showBackground = true, locale = "pt")`, with `DashboardStats()`.
     - `DashboardScreenSampleFiguresPreview`: `@Preview(showBackground = true)` and `@Preview(showBackground = true, locale = "pt")`, with `sampleFigureStats`.
   - Expected preview text:

     | Preview | Average | Supporting | Total | % active | Supporting |
     | --- | --- | --- | --- | --- | --- |
     | `previewStats` (EN) | 3 days and 4 hours | Based on 8 repairs | 30 | 40% | 12 of 30 issues |
     | `DashboardStats()` (EN) | No repairs yet | (none) | 0 | 0% | (none) |
     | `DashboardStats()` (PT) | Ainda sem reparações | (none) | 0 | 0 % | (none) |
     | `sampleFigureStats` (EN) | 3 days | Based on 1 repair | 3 | 67% | 2 of 3 issues |
     | `sampleFigureStats` (PT) | 3 dias | Com base em 1 reparação | 3 | 67 % | 2 de 3 ocorrências |
   - Commit: `feat(F-004): add DashboardScreen with stat cards`.
   - Verify:
     - `./gradlew assembleDebug` passes.
     - `./gradlew lint` passes with the same earlier warnings only, and no `MissingTranslation`, `MissingQuantity` or `ImpliedQuantity`. If lint flags `MissingQuantity` or `UnusedQuantity` for the PT `many` item, keep or remove `many` in all four plurals to satisfy lint, and log which in `state.md`.
     - A grep for the code pattern `Ticket\b|\.filter|isActive|sumOf|average\(` in `DashboardScreen.kt` is empty (the screen recomputes nothing; `totalTickets`/`activeTickets` do not match `Ticket\b`).
     - A grep for `isNaN|coerceIn|>= 0|< 0` in `DashboardScreen.kt` is empty (no contract guards; the allowed `coerceAtMost` does not match).
     - A grep for `dashboard_active_of_total` finds it in both string files and in `DashboardScreen.kt`.
     - By hand in Android Studio: previews render with the texts in the table above; landscape scrolls and nothing is clipped.
2. **Landscape row.**
   - In `DashboardScreen`, read `val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE`.
   - Landscape: put the three `StatCard`s in `Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp))`, each with `Modifier.weight(1f).fillMaxHeight()`.
   - Portrait: keep them stacked with `fillMaxWidth()`.
   - To avoid duplication, write the three cards once as a small private `@Composable` that takes a `cardModifier`, or call `StatCard` three times in each branch. Prefer whichever reads more plainly (the defence rule). The supporting arguments (`basedOn`, `null`, `activeOfTotal`) are the same in both branches.
   - The root `Column` and its scroll stay the same.
   - Commit: `feat(F-004): lay out dashboard stats in a row in landscape`.
   - Verify: build and lint pass. By hand: the landscape previews show one row of three equal-height cards (the total card, which has no supporting line, matches the others' height); the PT landscape label and "12 de 30 ocorrências"-style lines wrap without clipping.
3. **"See active issues" button (Q5, confirmed).**
   - Add `onSeeTicketsClick: () -> Unit` before `modifier`.
   - After the stats, inside the root `Column`, add `Button(onClick = onSeeTicketsClick, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(stringResource(R.string.dashboard_see_tickets)) }`.
   - Add `dashboard_see_tickets` to both string files.
   - Update every preview call with `onSeeTicketsClick = {}`.
   - Commit: `feat(F-004): add see-issues button to DashboardScreen`.
   - Verify: build and lint pass, and the key is in both string files. By hand: in interactive preview the button shows a ripple, and it is at least 48dp tall (M3 default).

Record each slice's outcome and any deviation in `state.md`.

## Conventions
- Stateless: no ViewModel, repository, Firebase or `mutableStateOf` in `DashboardScreen.kt`. The only remembered state is `rememberScrollState()` (saveable UI element state).
- No stats maths in the screen beyond formatting and the days/hours split. `DashboardStats` is the input; never take a `Ticket` or a list. Trust its contract: no NaN/clamp guard on `activeRatio`, no negative check on the average.
- Locale-aware formatting through `java.text.NumberFormat` with the locale from `LocalConfiguration.current.locales[0]`, not `Locale.getDefault()`. The integer formatter is shared by the total card and the "N of M issues" line.
- Text through `stringResource`/`pluralStringResource`. EN and PT go in the same commit, keys are snake_case, and there is no string concatenation for sentences (use `%1$s`/`%2$s` placeholders).
- Colours and styles come from `MaterialTheme`. Use `dp` for layout; text sizes come from typography.
- Accessibility: each card is merged (`semantics(mergeDescendants = true)`). Non-clickable cards need no `contentDescription` because their text is read. The button uses the M3 `Button` (48dp).
- `modifier: Modifier = Modifier` is the last parameter and is applied once, to the root.
- Previews are `private`, in `CityFixTheme`, with fixed values. Landscape uses the same `spec:width=411dp,height=891dp,orientation=landscape` string as F-002/F-003.
- One KDoc on the public function. Constants are `private const val` at file level.
- No new dependencies.

## Do not touch
`model/` (including `DashboardStats.kt` and `TicketsList.kt`, which are Diogo's), `viewmodel/`, `data/`, `ui/navigation/`, `ui/components/`, `ui/theme/`, `ui/screens/ListScreen.kt`, `MainActivity.kt`, `gradle/libs.versions.toml`, `app/build.gradle.kts`, `AndroidManifest.xml`, `google-services.json`, and other tasks' work folders.

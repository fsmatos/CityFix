## Files
Paths relative to `app/src/main/`; Kotlin package root `java/pt/isec/amov/cityfix/`.

| Path | Change | Why |
| --- | --- | --- |
| `java/pt/isec/amov/cityfix/ui/components/StatusBadge.kt` | New: `statusLabelRes`, `StatusBadge`, previews | Shared status label and badge (slice 1) |
| `java/pt/isec/amov/cityfix/ui/components/.gitkeep` | Delete | Folder now has a real file |
| `java/pt/isec/amov/cityfix/ui/theme/Color.kt` | Add `CityFixGreen = Color(0xFF2E7D32)` (only if Q1 default kept) | RESOLVED badge colour |
| `java/pt/isec/amov/cityfix/ui/components/TicketCard.kt` | New: `TicketCard`, `situationLabelRes`, previews | List row / summary card; collapsed content per brief I.1.B (slice 2), expanded details (slice 3) |
| `res/drawable/ic_image_placeholder.xml` | New vector asset (Material "image" outline, 24dp) | Placeholder thumbnail without a new dependency |
| `java/pt/isec/amov/cityfix/utils/DateFormatting.kt` | New: `formatDate(epochMillis)` | Locale-aware date, reusable by F-005 (slice 3) |
| `java/pt/isec/amov/cityfix/utils/.gitkeep` | Delete | Folder now has a real file |
| `res/values/strings.xml` | Add keys below (EN) | Labels and card texts |
| `res/values-pt/strings.xml` | Add the same keys (PT) | Device language PT |

String keys (EN / PT):
- Slice 1: `status_open` Open / Aberto; `status_info_requested` Info requested / Pedido de informações; `status_in_progress` In progress / Em resolução; `status_resolved` Resolved / Resolvido; `status_closed` Closed / Encerrado (PT wording from brief sections II and III).
- Slice 2: `ticket_thumbnail_description` Ticket photo / Fotografia da ocorrência; `ticket_no_category` No category / Sem categoria; `situation_damage` Damage / Dano; `situation_breakdown` Breakdown / Avaria; `situation_destruction` Destruction / Destruição; `situation_theft` Theft / Roubo (PT from brief I.2.E).
- Slice 3: `ticket_reported_on` Reported on %1$s / Reportado em %1$s.

## Interfaces
Package `pt.isec.amov.cityfix.ui.components`:
```kotlin
fun statusLabelRes(status: TicketStatus): Int          // exhaustive when, no else

@Composable
fun StatusBadge(status: TicketStatus, modifier: Modifier = Modifier)

fun situationLabelRes(situation: Situation): Int       // exhaustive when, no else

@Composable
fun TicketCard(
    ticket: Ticket,
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
)
```
Package `pt.isec.amov.cityfix.utils`:
```kotlin
fun formatDate(epochMillis: Long): String   // DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(epochMillis))
```
Callers: F-003 calls `TicketCard(ticket, isExpanded, onClick = { onTicketClick(ticket.id) })` inside `items(tickets, key = { it.id })`; F-005 uses `StatusBadge`, `statusLabelRes`, `situationLabelRes` and `formatDate` (or a date-time sibling it adds).

## Steps
1. **StatusBadge.** Add the five `status_*` strings to both files. Create `StatusBadge.kt`: `statusLabelRes` as a `when`; `StatusBadge` picks `containerColor`/`textColor` with a `when (status)` (OPEN primary/onPrimary, INFO_REQUESTED tertiary/onTertiary, IN_PROGRESS secondary/onSecondary, RESOLVED `CityFixGreen`/`Color.White`, CLOSED surfaceVariant/onSurfaceVariant), then `Surface(modifier, shape = MaterialTheme.shapes.small, color = containerColor, contentColor = textColor) { Text(stringResource(statusLabelRes(status)), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) }`. Add `CityFixGreen` to `Color.kt`. Private preview `StatusBadgePreview`: `CityFixTheme { Column { TicketStatus.entries.forEach { StatusBadge(it) } } }` with spacing, two `@Preview` annotations (portrait `showBackground = true`; landscape `device = "spec:width=411dp,height=891dp,orientation=landscape"`). Delete `ui/components/.gitkeep`.
   Commit: `feat(F-002): add StatusBadge with localised status labels`.
   Verify: `./gradlew assembleDebug`, `./gradlew lint` (no `MissingTranslation`); preview shows five readable badges; grep `\.name` in `ui/components/` is empty.
2. **TicketCard, collapsed (brief I.1.B: category, situation, thumbnail).** Import the vector asset `ic_image_placeholder` (Android Studio: New > Vector Asset > Clip art "image", outlined). Add `ticket_thumbnail_description`, `ticket_no_category` and the four `situation_*` strings. Create `TicketCard.kt` with `situationLabelRes` (exhaustive `when`) and the card: `Card(onClick = onClick, modifier = modifier.fillMaxWidth())` > `Row(Modifier.padding(12.dp), verticalAlignment = CenterVertically, horizontalArrangement = spacedBy(12.dp))` > private `TicketThumbnail()` (`Box(Modifier.size(64.dp).clip(MaterialTheme.shapes.small).background(colorScheme.surfaceVariant), contentAlignment = Center) { Icon(painterResource(R.drawable.ic_image_placeholder), contentDescription = stringResource(R.string.ticket_thumbnail_description), tint = colorScheme.onSurfaceVariant) }`) and a `Column(Modifier.weight(1f), verticalArrangement = spacedBy(4.dp))` with the category `Text` (`ticket.categoryName.ifBlank { stringResource(R.string.ticket_no_category) }` — compute the fallback string outside the lambda since `stringResource` is composable; `titleMedium`, `maxLines = 1`, `TextOverflow.Ellipsis`), the situation `Text(stringResource(situationLabelRes(ticket.situation)), style = bodySmall)` and `StatusBadge(ticket.currentStatus)`. `isExpanded` is in the signature but unused in this slice (lint may warn; it is used in slice 3). Previews: `TicketCardListPreview` (one card per status, distinct ids `"t1"`..`"t5"`, one with a very long category name), portrait and landscape.
   Commit: `feat(F-002): add TicketCard with thumbnail, category, situation and status`.
   Verify: build + lint (no `MissingTranslation`); previews render with category, situation and badge on every card; in interactive preview a tap shows ripple; grep `TicketCard.kt` for `location|creatorId|followerIds|messages` is empty.
3. **TicketCard, expanded.** Add the `ticket_reported_on` string. Create `utils/DateFormatting.kt` with `formatDate` and a one-line KDoc (device locale, works on minSdk 24); delete `utils/.gitkeep`. In `TicketCard.kt`, inside the text column, add `if (isExpanded) { ... }`: description `Text` if not blank (`bodyMedium`, `maxLines = 3`, ellipsis); if `ticket.createdAt > 0L`, `Text(stringResource(R.string.ticket_reported_on, formatDate(ticket.createdAt)), style = bodySmall, color = colorScheme.onSurfaceVariant)`. Previews: `TicketCardExpandedPreview` (same sample tickets with `isExpanded = true`, one with a long description, one with `createdAt = 0L`), portrait and landscape; keep the collapsed preview from slice 2.
   Commit: `feat(F-002): show description and date in expanded TicketCard`.
   Verify: build + lint (no `StringFormatMatches`, `MissingTranslation`); expanded previews add description and date to the collapsed content; switch the preview locale to `pt` (`@Preview(locale = "pt")` temporarily, or the emulator language) and check the PT labels; grep checks from slices 1-2 still pass.

Record each slice's outcome (and any deviation, e.g. Q1 alternative chosen) in `state.md`.

## Conventions
- Stateless composables: data in, lambdas out; `modifier: Modifier = Modifier` as the last parameter with a default, applied to the root.
- Text through `stringResource`; no literals in `Text(...)` or `contentDescription`. EN and PT added in the same commit, snake_case keys.
- Colours and text styles from `MaterialTheme.colorScheme` / `MaterialTheme.typography`; the only raw colour is `CityFixGreen` defined in `ui/theme/Color.kt` (if Q1 default) and `Color.White` as its text colour.
- Inline `.dp` sizes, `sp` only through typography.
- Exhaustive `when` over enums, no `else`.
- Previews `private`, wrapped in `CityFixTheme`, sample `Ticket`s with distinct ids and fixed values (no `System.currentTimeMillis()`).
- KDoc one line on each public function; comments only where the reason is not obvious (e.g. why `createdAt == 0L` is skipped).
- No new dependencies.

## Do not touch
`model/` (D-001, Diogo's), `MainActivity.kt`, `ui/navigation/`, `ui/screens/`, `viewmodel/`, `data/`, `Theme.kt`, `Type.kt`, `gradle/libs.versions.toml`, `app/build.gradle.kts`, `AndroidManifest.xml`, `google-services.json`, other tasks' work folders.

## Files
Paths relative to `app/src/main/`; Kotlin package root `java/pt/isec/amov/cityfix/`.

| Path | Change | Why |
| --- | --- | --- |
| `java/pt/isec/amov/cityfix/ui/screens/ListScreen.kt` | New: `ListScreen`, private preview data and previews | Public active tickets list (slice 1), empty state (slice 2) |
| `java/pt/isec/amov/cityfix/ui/screens/.gitkeep` | Delete | Folder now has a real file (slice 1) |
| `res/values/strings.xml` | Add `ticket_list_empty` under a `<!-- Ticket list -->` comment | Empty-state message, EN (slice 2) |
| `res/values-pt/strings.xml` | Add the same key | Empty-state message, PT (slice 2) |

String keys (EN / PT), Q5 default:
- Slice 2: `ticket_list_empty` No active issues to show. / Não há ocorrências ativas para mostrar.

## Interfaces
Package `pt.isec.amov.cityfix.ui.screens`:
```kotlin
/**
 * Public active tickets (brief I.1.B), shown in the order given; filtering, sorting and the
 * anonymous projection happen upstream (TicketsViewModel.tickets). Expanded cards also show the description.
 */
@Composable
fun ListScreen(
    tickets: List<Ticket>,
    isExpanded: Boolean,
    onTicketClick: (String) -> Unit,   // receives Ticket.id (Q6)
    modifier: Modifier = Modifier,
)
```
Uses `TicketCard(ticket, isExpanded, onClick, modifier)` from `ui/components/TicketCard.kt` unchanged.

Caller (D-006, not in this task):
```kotlin
composable<TicketListRoute> {
    ListScreen(
        tickets = tickets,              // vm.tickets.collectAsStateWithLifecycle().value, collected above the NavHost
        isExpanded = expanded,          // rememberSaveable in MainActivity
        onTicketClick = { id -> navController.navigate(TicketDetailRoute(id)) },
        modifier = Modifier.padding(innerPadding),
    )
}
```

## Steps
1. **List with data.** Create `ui/screens/ListScreen.kt`:
   - `ListScreen` as in Interfaces, body for now: `LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(tickets, key = { it.id }) { ticket -> TicketCard(ticket = ticket, isExpanded = isExpanded, onClick = { onTicketClick(ticket.id) }) } }`. Import `androidx.compose.foundation.lazy.items` (the `List` overload).
   - `private val previewTickets`: 6 `Ticket`s, ids `"p1"`..`"p6"`, statuses only OPEN / INFO_REQUESTED / IN_PROGRESS, mixed `Situation`s, `scope = Scope.PUBLIC`, `categoryName` and short `description` set, fixed `createdAt` (e.g. `1_792_000_000_000L` minus steps, newest first), no `creatorId`, `followerIds`, `messages` or `location` (Q4 default).
   - Previews, all `private`, wrapped in `CityFixTheme`:
     - `ListScreenPreview`: `@Preview(showBackground = true)` and `@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,orientation=landscape")`; `ListScreen(previewTickets, isExpanded = false, onTicketClick = {})`.
     - `ListScreenExpandedPreview`: `@Preview(showBackground = true)`; `isExpanded = true`.
   - Delete `ui/screens/.gitkeep`.
   Commit: `feat(F-003): add ListScreen with lazy ticket list`.
   Verify: `./gradlew assembleDebug`, `./gradlew lint` (same earlier warnings only); grep `key = \{ it.id \}` in `ListScreen.kt` matches; grep `location|creatorId|followerIds|messages|isActive|sorted|filter` in `ListScreen.kt` is empty. By hand in Android Studio: previews render, landscape scrolls, interactive tap shows ripple.
2. **Empty state.** Add `ticket_list_empty` to both string files (same commit). In `ListScreen`, wrap the body in `if (tickets.isEmpty()) { ... } else { LazyColumn(...) }`; the empty branch is `Box(modifier = modifier.fillMaxSize().padding(horizontal = 32.dp), contentAlignment = Alignment.Center) { Text(text = stringResource(R.string.ticket_list_empty), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center) }`. `modifier` is applied to the root of whichever branch is drawn (only one runs).
   Previews: `ListScreenEmptyPreview` with `@Preview(showBackground = true)`, the landscape `spec:` preview and `@Preview(showBackground = true, locale = "pt")`; `ListScreen(emptyList(), isExpanded = false, onTicketClick = {})`.
   Commit: `feat(F-003): show empty state in ListScreen`.
   Verify: build + lint (no `MissingTranslation`); grep `ticket_list_empty` appears in both string files; by hand: empty previews show the centred message in EN and PT, portrait and landscape.

Record each slice's outcome and any deviation in `state.md`.

## Conventions
- Stateless: no `remember`, `mutableStateOf`, ViewModel, repository or Firebase in `ListScreen.kt`; data in, lambda out.
- `modifier: Modifier = Modifier` last, applied once to the root.
- No filtering or sorting in the screen; render the list as received.
- `LazyColumn` with `items(tickets, key = { it.id })`; no `forEach` inside `item {}`.
- Text through `stringResource`; EN and PT in the same commit; snake_case keys.
- Colours and styles from `MaterialTheme`; `dp` for layout, text sizes through typography.
- Previews `private`, in `CityFixTheme`, distinct ids, fixed values (no `System.currentTimeMillis()`); landscape via the same `spec:` device string as F-002.
- One-line KDoc on the public function.
- No new dependencies.

## Do not touch
`ui/components/` (F-002, done), `model/`, `viewmodel/`, `data/` (Diogo's), `ui/navigation/`, `MainActivity.kt`, `ui/theme/`, `gradle/libs.versions.toml`, `app/build.gradle.kts`, `AndroidManifest.xml`, `google-services.json`, other tasks' work folders.

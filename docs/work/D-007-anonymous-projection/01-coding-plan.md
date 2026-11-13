# D-007: Coding plan

Assumes Gate 1 approves the recommended answers (Q1 option A, Q2 keep `createdAt`, Q3 required `showExpandToggle` + local flag, Q4 keep the state, Q5/Q6 no edits to Francisco's files). If Gate 1 picks option B or C for Q1, slice 2 is replanned.

## Files
| Path | Change | Why |
| --- | --- | --- |
| `app/src/main/java/pt/isec/amov/cityfix/model/TicketPrivacy.kt` | `toAnonymousView()` also sets `description = ""` and `statusHistory = emptyList()`; the per-entry history mapping is removed; KDoc rewritten as the projection contract | Enforce I.1.C below the UI (F-005 S4, decision 0007) |
| `app/src/main/java/pt/isec/amov/cityfix/ui/navigation/MainTopAppBar.kt` | New required `showExpandToggle: Boolean`; toggle shown only on LIST when true; KDoc; preview helper takes the flag; new "list, no toggle" preview | Q1 option A |
| `app/src/main/java/pt/isec/amov/cityfix/MainActivity.kt` | `val canExpandCards = false` in `CityFixApp`, passed as `showExpandToggle` and as `isExpanded = canExpandCards && isExpanded` to `ListScreen` | Phase 1: every user is anonymous; brief I.1.B |

No change to `data/LocalTicketsRepository.kt` (its two calls to `toAnonymousView()` stay), `data/TicketsRepository.kt`, `viewmodel/TicketsViewModel.kt`, the screens, `TicketCard.kt`, strings or Gradle files.

## Interfaces

Projection (signature unchanged):
```kotlin
fun Ticket.toAnonymousView(): Ticket = copy(
    description = "",
    location = location.coarse(),
    creatorId = "",
    followerIds = emptyList(),
    messages = emptyList(),
    statusHistory = emptyList(),
    attachments = attachments.map { it.copy(uploadedById = "") },
)
```

KDoc content (wording for the implementer to keep short and plain):
- Copy safe for anonymous users (brief I.1.B, I.1.C; CLAUDE.md; decisions 0006, 0007). Also the content of the Firestore public document (F-011).
- Dropped: `description` (free text, may hold personal data), `statusHistory` (I.1.C shows only media and map; its first entry holds the creator id), `creatorId`, `followerIds`, `messages`, every `Attachment.uploadedById`. Location rounded to `ANONYMOUS_COORDINATE_DECIMALS`.
- Kept: `id`, `situation`, `scope`, `currentStatus` (status badge), `createdAt` (ordering; never shown to anonymous users), `categoryId`, `categoryName`, the attachments' `id`, `type`, `file`, `thumbnail`, `uploadState`.
- The result is a view, not a valid ticket (no history entry): never store it as a full ticket, never compute stats, lifecycle rules or 10 m grouping from it.
- Media files pass through; photo GPS is removed in `LocalTicketsRepository.reportTicket` (keep the existing sentence about F-009 and the media task).
- Update this when `Ticket`, `Attachment` or `StatusHistory` gains a field.

Top bar:
```kotlin
fun MainTopAppBar(
    screen: TopBarScreen,
    canNavigateBack: Boolean,
    isExpanded: Boolean,
    showExpandToggle: Boolean,
    onBackClick: () -> Unit,
    onToggleExpandedClick: () -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
)
```
- `actions`: `if (screen == TopBarScreen.LIST && showExpandToggle) { ... }`, the existing `IconButton` unchanged.
- KDoc: "The toggle appears on the list only, and only when [showExpandToggle]; anonymous users never get it (brief I.1.B, D-007). The add button everywhere except the report form."

Call site in `CityFixApp`:
```kotlin
// Phase 1 has no login, so every user is anonymous: brief I.1.B lists only category,
// situation and thumbnail, so no expanded cards (D-007). The Auth task sets this by role.
val canExpandCards = false
var isExpanded by rememberSaveable { mutableStateOf(false) }
...
MainTopAppBar(..., isExpanded = isExpanded, showExpandToggle = canExpandCards, ...)
...
ListScreen(tickets = tickets, isExpanded = canExpandCards && isExpanded, ...)
```

## Steps

### Slice 1: projection drops description and history
- Edit `model/TicketPrivacy.kt` as in Interfaces. No other file.
- Commit: `feat(D-007): drop description and history from the anonymous projection`
- Verify:
  - `./gradlew assembleDebug` and `./gradlew lint`: 0 errors, 16 warnings (no new one).
  - Grep `toAnonymousView` in `app/src`: still called only in `data/LocalTicketsRepository.kt` (2 calls) and defined in `TicketPrivacy.kt`.
  - Read `LocalTicketsRepository.loadStats()`: `ticketsList.stats()` on the raw list, unchanged.
  - Emulator (if available): dashboard 3 issues / 67% / 3 days; list order `sample-1`, `sample-2`; detail unchanged; status badges unchanged (OPEN, IN_PROGRESS).

### Slice 2: hide the expand toggle for anonymous users
- Edit `ui/navigation/MainTopAppBar.kt`: parameter after `isExpanded`, condition, KDoc. `BarPreview` gets `showExpandToggle: Boolean = true`, so the existing list previews keep the toggle; add `ListAnonymousBarPreview()` with `BarPreview(TopBarScreen.LIST, canNavigateBack = true, showExpandToggle = false)` under `@OrientationPreviews`. Pass the flag through in `BarPreview`'s `MainTopAppBar` call.
- Edit `MainActivity.kt` as in Interfaces.
- Commit: `feat(D-007): hide the expand toggle for anonymous users`
- Verify:
  - `./gradlew assembleDebug` and `./gradlew lint`: 0 errors, 16 warnings. If lint or the compiler warns about the constant condition, record it in `state.md`; switching to `private const val CAN_EXPAND_CARDS = false` at file level is an accepted fallback (same meaning), not a plan change.
  - Emulator: list in portrait and landscape, EN and PT: no toggle, add button still there; cards collapsed (no description, no "Reported on"); rotate on the list: still collapsed, scroll kept; dashboard, detail and report unchanged; new PUBLIC report with a photo: in the list, thumbnail, no description; its detail shows no description/date; dashboard total 4, active 3.
  - Previews (Android Studio): list bar with and without the toggle; `TicketCard`/`ListScreen` expanded previews still render.

## Conventions
- Kotlin and Compose as in `kotlin-conventions` and `.claude/rules/kotlin.md`: stateless bar, state above the `NavHost`, no hard-coded strings (no new strings here), `modifier` stays the first optional parameter.
- Plain code; comments say why, with the brief section or task id.
- Conventional Commits with the task id, no attribution. Do not stage `.idea/`.
- Run the `verify` skill (assembleDebug + lint) before each commit.

## Do not touch
- `ui/components/TicketCard.kt`, `ui/screens/ListScreen.kt`, `ui/screens/ShowScreen.kt` (Francisco's files, Q5).
- `data/TicketsRepository.kt`, `data/LocalTicketsRepository.kt`, `viewmodel/*`, `model/TicketsList.kt`, `model/Ticket.kt`.
- Strings, the manifest, Gradle files, `google-services.json`, Firestore/Storage rules.
- Decision records and docs (Docs phase; see `03-docs-plan.md`).
- `docs/work/D-007-anonymous-projection/state.md` is updated by the orchestrator, not the implementer's code commits.

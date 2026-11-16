# D-013 coding plan

Read `00-plan.md` first. Gate 1 answers (Q1-Q13) may change the parts marked "if Q...". Slices 4, 5 and 6 touch Francisco's files: agree the signatures (Q2) and the `ShowScreen` change (Q3, Q4) with him before starting slice 4.

## Files
| Path | Change | Why |
| --- | --- | --- |
| `app/src/main/java/pt/isec/amov/cityfix/model/TicketFilter.kt` | new | `TicketFilter`, `filteredFor` (pure; slice 1) |
| `app/src/main/java/pt/isec/amov/cityfix/viewmodel/OperatorPanelUiState.kt` | new | `categories`, `filter`, `showMap` (slice 2; `showMap` slice 8) |
| `app/src/main/java/pt/isec/amov/cityfix/viewmodel/OperatorPanelViewModel.kt` | new | Collects `allCategories()`, filter events (slice 2) |
| `app/src/main/java/pt/isec/amov/cityfix/viewmodel/OperatorPanelViewModelFactory.kt` | new | Plain factory (slice 2) |
| `app/src/main/java/pt/isec/amov/cityfix/ui/screens/OperatorPanelScreen.kt` | new | Stateful + stateless panel, previews (slice 2) |
| `app/src/main/java/pt/isec/amov/cityfix/ui/screens/PlaceholderScreen.kt` | delete (if Q10) | Last caller gone (slice 2) |
| `app/src/main/java/pt/isec/amov/cityfix/ui/navigation/Destinations.kt` | edit (KDoc) | `OperatorPanelRoute` no longer a placeholder (slice 2) |
| `app/src/main/java/pt/isec/amov/cityfix/viewmodel/OperatorTicketUiState.kt` | new | `PersonContact`, `OperatorDialog`, state (slices 3, 5, 6) |
| `app/src/main/java/pt/isec/amov/cityfix/viewmodel/OperatorTicketViewModel.kt` | new | Thread, people, dialogs, proof (slices 3, 5, 6) |
| `app/src/main/java/pt/isec/amov/cityfix/viewmodel/OperatorTicketViewModelFactory.kt` | new | Plain factory (slice 3) |
| `app/src/main/java/pt/isec/amov/cityfix/ui/screens/OperatorTicketScreen.kt` | new | Stateful (launchers) + stateless detail, previews (slices 3, 5, 6) |
| `app/src/main/java/pt/isec/amov/cityfix/ui/screens/ShowScreen.kt` | edit (Francisco's, additive, if Q3/Q4) | `responsibleNames` param; RESOLUTION line on `MediaTile` (slice 4) |
| `app/src/main/java/pt/isec/amov/cityfix/model/TicketLifecycle.kt` | edit | `needsOperatorGeofence(now)` (D-014 hook; slice 5) |
| `app/src/main/java/pt/isec/amov/cityfix/data/TicketsRepository.kt` | edit (Francisco's, Q2) | `startWork` (slice 5), `resolveTicket` (slice 6), `askForClarification` KDoc (slice 5) |
| `app/src/main/java/pt/isec/amov/cityfix/data/FirestoreTicketsRepository.kt` | edit (Francisco's, Q2) | Implementations; `commitStatusChange(newAttachments)` (slices 5, 6) |
| `app/src/main/java/pt/isec/amov/cityfix/data/FirestoreMapping.kt` | edit (Francisco's, one word) | `Attachment.toDocument()` `private` -> `internal` (slice 6) |
| `app/src/main/java/pt/isec/amov/cityfix/MainActivity.kt` | edit | Panel destination (slice 2); operator branch of `TicketDetailRoute` (slice 3) |
| `app/src/main/res/values/strings.xml`, `app/src/main/res/values-pt/strings.xml` | edit | `operator_panel_*`, `operator_ticket_*`, `operator_action_*`, `resolve_*`, `ticket_history_by`, `ticket_media_proof`, `cd_*` (slices 2-6, 8) |
| `app/src/main/java/pt/isec/amov/cityfix/ui/components/TicketsMap.kt` | new (optional) | Several exact markers (slice 8) |
| `TicketsRepository.kt`, `FirestoreTicketsRepository.kt`, `FirestoreMapping.kt` | edit (optional) | `reportsFor(ticketId)` + `toJoinerReport()` (slice 9) |

## Interfaces

```kotlin
// model/TicketFilter.kt (pure Kotlin, no imports outside model)
/** The operator panel's filter and order (brief I.3.E). null = all. */
data class TicketFilter(
    val status: TicketStatus? = null,
    val categoryId: String? = null,
    val newestFirst: Boolean = true,
)

/**
 * The tickets that match [filter] at [now]: status compared with effectiveStatus(now) (an expired
 * RESOLVED is CLOSED), category by id, ordered by createdAt (newest or oldest first), id as tie-break.
 */
fun List<Ticket>.filteredFor(filter: TicketFilter, now: Long): List<Ticket>

// model/TicketLifecycle.kt (addition, slice 5)
/**
 * D-014 hook: true while the operator's 20 m geofence should watch this ticket (brief II.3):
 * effectively IN_PROGRESS and with a real location. INFO_REQUESTED pauses it.
 */
fun Ticket.needsOperatorGeofence(now: Long): Boolean

// data/TicketsRepository.kt (additions; signatures to agree with Francisco, Q2)
/**
 * The operator starts work (brief II.3, I.3.E): IN_PROGRESS with an optional [note], one batch
 * (ticket, publicTickets status, ticketLocations, stats). False when the lifecycle rejects it.
 * Returns once the write is queued.
 */
fun startWork(ticket: Ticket, by: User, note: String? = null): Boolean

/**
 * The operator resolves [ticket] with [proof] (brief I.3.E: photo/video of the finished work):
 * prepares each file (AttachmentUploader), then one batch with RESOLVED, the proof appended
 * (phase RESOLUTION) and the companions, then one upload worker. False when the lifecycle
 * rejects it or no proof could be prepared; nothing is written then. Returns once queued.
 */
suspend fun resolveTicket(ticket: Ticket, by: User, proof: List<Attachment>, note: String? = null): Boolean

// askForClarification: unchanged signature; KDoc gains "Also the operator's request for information
// (D-013): same batch, the text is the justification and a message by the operator."

// data/FirestoreTicketsRepository.kt (private change)
private fun commitStatusChange(
    before: Ticket,
    after: Ticket,
    message: Map<String, Any>? = null,
    newAttachments: List<Attachment> = emptyList(),   // D-013: appended with FieldValue.arrayUnion
)

// viewmodel/OperatorPanelUiState.kt
data class OperatorPanelUiState(
    /** Every category, inactive too (old tickets), sorted by name (CategoriesRepository). */
    val categories: List<Category> = emptyList(),
    val filter: TicketFilter = TicketFilter(),
    /** Slice 8 only. */
    val showMap: Boolean = false,
)

class OperatorPanelViewModel(categories: CategoriesRepository) : ViewModel() {
    val uiState: StateFlow<OperatorPanelUiState>
    fun onStatusFilter(status: TicketStatus?)
    fun onCategoryFilter(categoryId: String?)
    fun onToggleSort()
    fun onToggleMap()          // slice 8
}

// viewmodel/OperatorTicketUiState.kt
/** What the operator sees of one person (brief I.3.E: author contacts). Never shown to citizens. */
data class PersonContact(val name: String, val email: String, val phone: String, val isOperator: Boolean)

/** The operator actions that open a dialog first. */
enum class OperatorDialog { REQUEST_INFO, START_WORK }

data class OperatorTicketUiState(
    val messages: List<Message> = emptyList(),
    val draft: String = "",
    /** uid -> contact; absent = not loaded yet; null = not available (offline, missing). */
    val people: Map<String, PersonContact?> = emptyMap(),
    val openDialog: OperatorDialog? = null,
    val dialogText: String = "",
    @StringRes val dialogError: Int? = null,
    /** The Resolve section is open. */
    val resolving: Boolean = false,
    val proof: List<Attachment> = emptyList(),
    val resolveNote: String = "",
    val isCopying: Boolean = false,
    val isResolving: Boolean = false,
    @StringRes val resolveError: Int? = null,
) {
    val canAddProof: Boolean get() = proof.size < MAX_PROOF_ATTACHMENTS && !isCopying && !isResolving
    val canConfirmResolve: Boolean get() = proof.isNotEmpty() && !isCopying && !isResolving
}

const val MAX_PROOF_ATTACHMENTS = 3

class OperatorTicketViewModel(
    tickets: TicketsRepository,
    users: UsersRepository,
    attachmentStorage: AttachmentStorage,
    ticketId: String,
    operator: Operator,
) : ViewModel() {
    val uiState: StateFlow<OperatorTicketUiState>
    fun onDraftChange(text: String)
    fun sendMessage()
    fun watchPeople(ids: Set<String>)
    fun onRequestInfoClick()
    fun onStartWorkClick()
    fun onDialogTextChange(text: String)
    fun onDialogDismiss()
    fun confirmDialog(ticket: Ticket)
    fun onResolveClick()
    fun onMediaPicked(uris: List<Uri>)
    fun onPhotoTaken(path: String)
    fun onRemoveProof(attachment: Attachment)
    fun onResolveNoteChange(text: String)
    fun onCancelResolve()
    fun resolve(ticket: Ticket)
    fun closeIfExpired(ticket: Ticket)
}

// ui/screens
@Composable fun OperatorPanelScreen(tickets: List<Ticket>, viewModel: OperatorPanelViewModel, onTicketClick: (String) -> Unit, modifier: Modifier = Modifier)
@Composable fun OperatorPanelContent(tickets: List<Ticket>, now: Long, state: OperatorPanelUiState, onStatusFilter: (TicketStatus?) -> Unit, onCategoryFilter: (String?) -> Unit, onToggleSort: () -> Unit, onTicketClick: (String) -> Unit, modifier: Modifier = Modifier)
@Composable fun OperatorTicketScreen(ticket: Ticket?, operator: Operator, viewModel: OperatorTicketViewModel, modifier: Modifier = Modifier)
@Composable fun OperatorTicketContent(ticket: Ticket?, operator: Operator, now: Long, state: OperatorTicketUiState, /* one lambda per event */ modifier: Modifier = Modifier)

// ui/screens/ShowScreen.kt (additive, slice 4)
fun ShowScreen(
    ticket: Ticket?,
    audience: DetailAudience,
    now: Long,
    modifier: Modifier = Modifier,
    /** D-013: responsibleId -> name, operator view only; "By <name>" in the history. Empty = not shown. */
    responsibleNames: Map<String, String> = emptyMap(),
    participantContent: @Composable ColumnScope.() -> Unit = {},
)
```

`MainActivity` (shape only):

```kotlin
composable<OperatorPanelRoute> {
    RequireAccess(role = role, allowed = setOf(AccessRole.OPERATOR), onDenied = goHome) {
        val panelViewModel: OperatorPanelViewModel =
            viewModel(factory = OperatorPanelViewModelFactory(app.categoriesRepository))
        OperatorPanelScreen(
            tickets = tickets,   // allTickets() for an operator (TicketsViewModel.setUser)
            viewModel = panelViewModel,
            onTicketClick = { id -> navController.navigate(TicketDetailRoute(id)) { launchSingleTop = true } },
        )
    }
}

composable<TicketDetailRoute> { entry ->
    // ... id, ticket, user, audience as today
    when {
        user == null || audience == DetailAudience.PUBLIC -> ShowScreen(...)          // unchanged
        user is Operator -> {
            // Inside the branch: scoped to this entry; the contacts and messages listeners end with it.
            val operatorViewModel: OperatorTicketViewModel = viewModel(
                factory = OperatorTicketViewModelFactory(app.ticketsRepository, app.usersRepository, app.attachmentStorage, id, user),
            )
            OperatorTicketScreen(ticket = ticket, operator = user, viewModel = operatorViewModel)
        }
        else -> { /* F-014 TicketDetailViewModel + TicketDetailScreen, unchanged */ }
    }
}
```

## Steps

### Slice 1: model, filter rule
- `model/TicketFilter.kt`: the data class and `filteredFor` as above:
  - `filter { (filter.status == null || it.effectiveStatus(now) == filter.status) && (filter.categoryId == null || it.categoryId == filter.categoryId) }`;
  - `sortedWith(compareByDescending<Ticket> { it.createdAt }.thenBy { it.id })` for newest first, `compareBy<Ticket> { it.createdAt }.thenBy { it.id }` otherwise.
  - KDoc: brief I.3.E; why effective status (decision 0011); never call on a projected ticket (it has no history, so `effectiveStatus` is the stored status; harmless but meaningless).
- Commit: `feat(D-013): add the operator panel filter rule`
- Verify: `./gradlew assembleDebug`, `./gradlew lint`. Hand check table in `state.md`: 3 sample tickets x {All, OPEN, IN_PROGRESS, RESOLVED, CLOSED at `t0 + 6 d`} x {newest, oldest}.

### Slice 2: operations panel
- `OperatorPanelUiState`, `OperatorPanelViewModel` (`init { viewModelScope.launch { categories.allCategories().collect { list -> _uiState.update { it.copy(categories = list) } } } }`; `onStatusFilter`/`onCategoryFilter` -> `update { it.copy(filter = it.filter.copy(...)) }`; `onToggleSort` flips `newestFirst`), plain factory as `CategoriesViewModelFactory`.
- `OperatorPanelScreen` (stateful): collects state, `val now = System.currentTimeMillis()`, `val shown = remember(tickets, state.filter) { tickets.filteredFor(state.filter, now) }` passed to the content. `OperatorPanelContent`:
  - header `Column`: a `Row(Modifier.horizontalScroll(...))` of `FilterChip`s "All" + one per `TicketStatus` (labels from the existing status strings used by `StatusBadge`; selected = `filter.status == it`); an `ExposedDropdownMenuBox` "Category" with "All categories" + `state.categories` (inactive ones suffixed with the existing "Inactive" string); an `AssistChip` with `Icons.Filled.ArrowDownward`/`ArrowUpward` and "Newest first"/"Oldest first";
  - `LazyColumn(contentPadding = 16.dp, spacedBy(8.dp))`, `items(shown, key = { it.id })`: `TicketCard(ticket, isExpanded = true, onClick = { onTicketClick(ticket.id) }, status = ticket.effectiveStatus(now))` and, for `Scope.PRIVATE`, a `Text(operator_panel_private)` label under it (both in one `Column`);
  - empty states: `operator_panel_empty` ("No tickets yet.") when `tickets` is empty, `operator_panel_no_match` ("No tickets match these filters.") otherwise;
  - width `widthIn(max = 600.dp)` centred, so landscape is not stretched; the header scrolls with the list (put it as the first `item`) so it never takes the whole landscape height.
  - Previews: 4 synthetic tickets (OPEN PUBLIC, INFO_REQUESTED PRIVATE, IN_PROGRESS, RESOLVED at `t0` with `now = t0 + 6 d`), filters All / CLOSED / no match; EN, PT, landscape.
- `MainActivity`: replace the placeholder (shape above). `Destinations.kt`: KDoc "Operator home: the operations panel (D-013)."
- If Q10: delete `PlaceholderScreen.kt` and `placeholder_message` (EN + PT) after checking with Grep that nothing else uses them; remove the import.
- Strings: `operator_panel_filter_all`, `operator_panel_category_label`, `operator_panel_category_all`, `operator_panel_sort_newest`, `operator_panel_sort_oldest`, `operator_panel_private`, `operator_panel_empty`, `operator_panel_no_match`, `cd_operator_panel_sort`.
- Commit: `feat(D-013): add the operations panel with filters and sort`
- Verify: build, lint; emulator as operator: home shows every ticket including another citizen's PRIVATE one; each filter; sort; rotation keeps the filter; landscape; a citizen and an anonymous user cannot reach it (menu has no entry; `RequireAccess` sends them home).

### Slice 3: operator detail, read-only
- `OperatorTicketUiState` (thread and people fields), `OperatorTicketViewModel`:
  - `init` collects `tickets.messagesFor(ticketId)` into `messages`;
  - `onDraftChange`, `sendMessage()` (`if (tickets.sendMessage(ticketId, draft, operator)) draft = ""`);
  - `watchPeople(ids)`: a private `watched = mutableSetOf<String>()`; for each id not in it and not blank and not `SYSTEM_RESPONSIBLE_ID`, add it and `viewModelScope.launch { users.observeUser(id).collect { profile -> _uiState.update { it.copy(people = it.people + (id to profile.toContact())) } } }`; `toContact()` maps `Found(user)` to `PersonContact(user.name, user.email, user.phone, user is Operator)`, anything else to `null`;
  - `closeIfExpired(ticket)` -> `tickets.closeExpiredTicket(ticket)`.
- Factory: plain, takes the five arguments.
- `OperatorTicketScreen` (stateful): collects state; `LaunchedEffect(ticket) { ticket?.let(viewModel::closeIfExpired) }`; `val ids = ticket?.let { setOf(it.creatorId) + it.followerIds + it.statusHistory.map { h -> h.responsibleId } }.orEmpty()`; `LaunchedEffect(ids) { viewModel.watchPeople(ids) }`.
- `OperatorTicketContent`: `ShowScreen(ticket, DetailAudience.OPERATOR, now, modifier.imePadding()) { ContactsSection(...); HorizontalDivider(); MessageThread(messages, operator.id, draft, canSend = ticket != null && !ticket.isLocked(now), ...) }`.
  - `ContactsSection`: heading "Contacts"; "Author" with `ContactRow(people[creatorId])`; "Followers" with one row each, or "No followers."; `ContactRow` shows name (`bodyLarge`), e-mail and `+351 <phone>` (`bodyMedium`), `mergeDescendants`; `null` -> `operator_ticket_contact_unavailable`; absent -> a small "Loading..." text.
- `MainActivity`: the `user is Operator` branch (shape above); import `Operator` already present.
- Strings: `operator_ticket_contacts_title`, `operator_ticket_author`, `operator_ticket_followers`, `operator_ticket_no_followers`, `operator_ticket_contact_unavailable`, `operator_ticket_contact_loading`, `operator_ticket_phone` ("+351 %1$s").
- Commit: `feat(D-013): show the operator ticket detail with contacts and thread`
- Verify: build, lint; emulator: from the panel open a ticket: exact map, description, full history, all media, author and followers' contacts, thread; send a message (citizen sees "Municipality"); offline open with a cached ticket shows contacts from the cache or "Contact not available"; a citizen opening the same ticket still sees F-014's view with no contacts.

### Slice 4: history by operator and proof label (if Q3 / Q4; Francisco's `ShowScreen.kt`)
- `ShowScreen`: new `responsibleNames: Map<String, String> = emptyMap()` before `participantContent` (keeps the trailing-lambda call sites compiling), passed to `LocationPane` -> `StatusHistorySection` -> `HistoryRow`; in `HistoryRow`, after the system line: `responsibleNames[entry.responsibleId]?.let { Text(stringResource(R.string.ticket_history_by, it), bodySmall, onSurfaceVariant) }`.
- `MediaTile`: if `attachment.phase == AttachmentPhase.RESOLUTION`, a `Text(R.string.ticket_media_proof)` line under the type label.
- `OperatorTicketContent`: `responsibleNames = state.people.mapNotNull { (id, c) -> c?.let { id to it.name } }.toMap()`.
- Strings: `ticket_history_by` ("By %1$s" / "Por %1$s"), `ticket_media_proof` ("Resolution proof" / "Prova da resolução").
- Update `ShowScreenFullPreview` with one RESOLUTION attachment; add an operator preview with names.
- Commit: `feat(D-013): name the operator in the history and label proof media`
- Verify: build, lint; operator detail shows "By <name>"; the citizen's detail of the same ticket shows the dates but no names, and the "Resolution proof" label once resolved. Tell Francisco (state log).

### Slice 5: Request information and Start work
- `TicketsRepository` / `FirestoreTicketsRepository`: `startWork` (as Interfaces; implementation `val after = ticket.transition(TicketStatus.IN_PROGRESS, by, System.currentTimeMillis(), note) ?: return false; commitStatusChange(ticket, after); return true`); `askForClarification` KDoc line in the interface.
- `TicketLifecycle.kt`: `needsOperatorGeofence(now) = effectiveStatus(now) == TicketStatus.IN_PROGRESS && location != Coordinates()`.
- `OperatorTicketUiState`/`ViewModel`: `openDialog`, `dialogText`, `dialogError`; `onRequestInfoClick` / `onStartWorkClick` open the dialog with empty text; `confirmDialog(ticket)`:
  - REQUEST_INFO: blank -> `operator_request_info_error_blank`; else `tickets.askForClarification(ticket, operator, text)`;
  - START_WORK: `tickets.startWork(ticket, operator, dialogText.ifBlank { null })`;
  - false -> `operator_action_error_not_allowed`; true -> close.
- `OperatorTicketContent`: `val moves = ticket?.allowedTransitions(operator, now).orEmpty()`; a `FlowRow` (or `Row` with wrap) of buttons above the contacts: `INFO_REQUESTED in moves` -> `OutlinedButton` "Request information"; `IN_PROGRESS in moves` -> `Button` "Start work"; `RESOLVED in moves` -> `Button` "Resolve" (wired in slice 6; hidden until then). Private `OperatorTextDialog` (as F-014's `TextDialog`): REQUEST_INFO title "Request information", text "The citizen sees this message and the ticket waits for their answer.", label "What do you need?"; START_WORK title "Start work?", text "The ticket moves to In progress and the citizens are told.", label "Note (optional)".
- Strings: `operator_action_request_info`, `operator_action_start_work`, `operator_action_resolve`, `operator_request_info_title`, `_text`, `_label`, `_confirm`, `operator_request_info_error_blank`, `operator_start_work_title`, `_text`, `_label`, `_confirm`, `operator_action_error_not_allowed`. Reuse `dialog_cancel`.
- Commit: `feat(D-013): let operators request information and start work`
- Verify: build, lint; OPEN: both buttons; Request information with blank text shows the error; with text: history entry (time, "By <name>", the text), thread message, citizen's device updates live; INFO_REQUESTED: only Start work; Start work: IN_PROGRESS in history, `ticketLocations.active` still true, `publicTickets` status mirrored (PUBLIC); rotation with a dialog open keeps the text; offline actions show at once.

### Slice 6: Resolve with proof
- `FirestoreMapping.kt`: `private fun Attachment.toDocument()` -> `internal fun Attachment.toDocument()` (KDoc: "also the resolve batch's arrayUnion, D-013").
- `FirestoreTicketsRepository`:
  - `commitStatusChange(..., newAttachments)`: `val fields: Map<String, Any> = if (newAttachments.isEmpty()) after.toStatusFields() else after.toStatusFields() + ("attachments" to FieldValue.arrayUnion(*newAttachments.map { it.toDocument() }.toTypedArray()))`; `batch.update(ticketRef, fields)`. KDoc: why `arrayUnion` (does not overwrite a worker's concurrent update of the creator's entries).
  - `resolveTicket` (steps in `00-plan.md`), on `Dispatchers.IO`: dry-run transition with the raw proof -> `prepare` each -> require one PENDING -> real transition -> `commitStatusChange(ticket, after, newAttachments = after.attachments.drop(ticket.attachments.size))` -> `if (after.attachments.any { it.phase == RESOLUTION && it.uploadState == PENDING }) uploader.enqueue("$TICKETS/${after.id}", after.id, false)`.
- `OperatorTicketViewModel` (needs `attachmentStorage`):
  - `onResolveClick` -> `resolving = true`; `onCancelResolve` -> delete draft files (`File(it.file).delete()` on `Dispatchers.IO` via `viewModelScope.launch`), `resolving = false`, clear draft;
  - `onMediaPicked(uris)`: as `ReportViewModel.onMediaPicked`: `isCopying = true`, copy `uris.take(remaining)`, recheck the limit when the copy returns (delete extras), add as `Attachment(id = File(path).name, type = if (FileUtils.isVideoPath(path)) VIDEO else IMAGE, file = path)`;
  - `onPhotoTaken(path)` adds one IMAGE (the screen deletes it when no slot is left);
  - `onRemoveProof` deletes the file;
  - `resolve(ticket)`: return unless `canConfirmResolve`; set `isResolving = true, resolveError = null` before `launch`; `val ok = tickets.resolveTicket(ticket, operator, proof, resolveNote.ifBlank { null })`; ok -> `resolving = false, proof = emptyList(), resolveNote = "", isResolving = false` (files now belong to the upload; do not delete); not ok -> `isResolving = false, resolveError = resolve_error_failed`;
  - `onCleared()`: delete proof files unless the last `resolve` succeeded (as `ReportViewModel` with `submitted`).
- `OperatorTicketScreen` (stateful): `PickMultipleVisualMedia` / `PickVisualMedia` (`ImageAndVideo`, `maxItems = remaining`, single picker for the last slot) and `TakePicture` with `tempCameraPath` in `rememberSaveable`, above the orientation branch; `launchCamera` / captured handling copied from `ReportScreen`'s private helpers (`FileUtils.createTempImageFile`), with a "Camera not available" notice via `resolveError`.
- `OperatorTicketContent`: when `state.resolving` replace the action buttons with `ResolveSection`: heading "Proof of the finished work", hint "Add at least one photo or video.", `AttachmentGallery(state.proof, onRemoveProof, removeEnabled = !state.isResolving)`, "Add from gallery" / "Take photo" (`enabled = state.canAddProof`), `OutlinedTextField` "Note (optional)", the error, and `TextButton` Cancel + `Button` "Confirm resolution" (`enabled = state.canConfirmResolve`), a `CircularProgressIndicator` inside it while `isResolving`.
- Strings: `resolve_title`, `resolve_hint`, `resolve_add_gallery`, `resolve_take_photo`, `resolve_note_label`, `resolve_confirm`, `resolve_cancel`, `resolve_error_failed` ("The proof could not be prepared, or this ticket can no longer be resolved."), `resolve_camera_unavailable`, `cd_resolve_busy`.
- Commit: `feat(D-013): resolve tickets with proof media uploaded in the background`
- Verify: build, lint; IN_PROGRESS -> Resolve: Confirm disabled with no proof; gallery photo + camera photo; Confirm: RESOLVED, history "By <name>", proof tiles "Upload pending" then uploaded; console (00-plan acceptance); `publicTickets.thumbnails` unchanged; WorkManager: one ENQUEUED job (App Inspection); **offline**: airplane mode, Resolve, close the app, network back -> uploaded; citizen sees RESOLVED and the proof; rotation with picked proof and typed note; Cancel deletes the draft files (Device Explorer `files/images`).

### Slice 7: polish and checks
- Previews of `OperatorTicketContent`: OPEN (two buttons), INFO_REQUESTED, IN_PROGRESS, resolve section empty and with proof, request-info dialog with error, CLOSED at `t0 + 6 d` (no actions, locked thread), contacts unavailable; EN, PT, landscape.
- Accessibility: headings, `mergeDescendants` on contact rows, 48dp buttons, content descriptions for icon-only controls and the busy indicator; TalkBack reads filter chips' selected state (M3 default).
- PT wording review ("ocorrência", "Pedido de informações", "Em curso", "Resolvida" matching the existing status strings).
- Run the device checks below and record them in `state.md`.
- Commit: `feat(D-013): polish the operator screens` (only if code changed).
- Verify: build, lint; D1-D12.

### Slice 8 (optional, Q12): panel map
- `ui/components/TicketsMap.kt`: `TicketsMap(tickets: List<Ticket>, onTicketClick: (String) -> Unit, modifier)`: `GoogleMap` with `rememberCameraPositionState` centred on `PRACA_REPUBLICA` at zoom 13; one `Marker(state = remember(t.id, t.location) { MarkerState(LatLng(...)) }, title = t.categoryName, onInfoWindowClick = { onTicketClick(t.id) })` per ticket with a non-(0, 0) location; placeholder in previews (`LocalInspectionMode`, as `TicketMap`). Operators only (exact coordinates).
- `OperatorPanelViewModel.onToggleMap`, a "List"/"Map" `SegmentedButton` row in the header; map fills the space under the header.
- Strings: `operator_panel_view_list`, `operator_panel_view_map`, `cd_operator_panel_map`.
- Commit: `feat(D-013): add a map view to the operations panel`
- Verify: build, lint; markers for the filtered tickets; tap info window -> detail; rotation keeps the mode.

### Slice 9 (optional, Q12): joiners' reports
- `TicketsRepository.reportsFor(ticketId): Flow<List<JoinerReport>>` over `tickets/{id}/reports` (`listen`, sorted by `createdAt`), `data class JoinerReport(val citizenId, val description, val createdAt, val attachments)` in `TicketsRepository.kt`, mapping `toJoinerReport()` in `FirestoreMapping.kt` (Francisco's; agree first).
- `OperatorTicketViewModel` collects it into `reports`; a "Reports from followers" section (name from `people`, date, description, `AttachmentImage` tiles).
- Commit: `feat(D-013): show the followers' reports to operators`
- Verify: build, lint; a joined ticket shows the second citizen's description and media.

### Device checks (slice 7; none run yet)
| # | Check |
| --- | --- |
| D1 | Operator home = panel; PRIVATE ticket of another citizen listed with "Private" |
| D2 | Status chips (expired RESOLVED under Closed), category filter (inactive category listed), sort, rotation |
| D3 | Detail: exact map, history, all media, author + followers' contacts; offline "Contact not available" |
| D4 | Citizen (participant) and anonymous on the same ticket: no contacts, no names |
| D5 | Buttons per status (OPEN / INFO_REQUESTED / IN_PROGRESS / RESOLVED / CLOSED) |
| D6 | Request information: blank error; entry + message; citizen sees it live (second device) |
| D7 | Start work: IN_PROGRESS entry "By <name>"; `needsOperatorGeofence` true (log or debugger, until D-014) |
| D8 | Resolve disabled with no proof; with proof: console fields, stats, `ticketLocations.active` false, no public thumbnail |
| D9 | Resolve offline, app closed, network back: proof uploaded; citizen sees the proof |
| D10 | Closed / expired ticket: no actions, locked thread; CLOSED entry written once on open |
| D11 | Rotation with each dialog / resolve section open; landscape on both screens |
| D12 | Build + lint after each slice; new warnings listed |

## Conventions
- MVVM as the rest of the app: private `MutableStateFlow` + `asStateFlow()`, `update { it.copy(...) }`, collection in `viewModelScope`; `collectAsStateWithLifecycle()` in the stateful screen; previews on the stateless `XxxContent`.
- ViewModels built by their own plain `ViewModelProvider.Factory` and created **inside** the guard / operator branch with `viewModel(factory = ...)`, so listeners end with the back-stack entry.
- The ticket is never stored in a ViewModel: the screen passes the current one from `TicketsViewModel`'s list (F-014 pattern).
- Every status change goes through D-008 `transition` inside the repository; callers never `copy(currentStatus = ...)`. Offered actions come only from `allowedTransitions(operator, now)`.
- Firestore writes not awaited, failures logged (F-011, F-014); no transactions (offline). `suspend` only where `prepare` needs it.
- `model/` stays pure Kotlin (no Android imports).
- No literal UI text; EN in `values/strings.xml`, PT in `values-pt/strings.xml`. Status labels reuse the existing status strings.
- 48dp targets via M3 components, headings marked, errors as text; landscape via `ShowScreen`'s panes and a 600dp cap on the panel.
- No personal data in routes, logs, previews or state files; preview data synthetic. Never real test e-mails, phones or UIDs in git.
- Commits: Conventional Commits with `D-013`, no attribution lines.

## Do not touch
- `TicketDetailScreen.kt`, `TicketDetailViewModel*.kt`, `TicketDetailUiState.kt`, `MessageThread.kt`, `TicketCard.kt`, `TicketMap.kt`, `ListScreen.kt`, `MyTicketsScreen.kt` (Francisco's; reused as they are).
- `AttachmentUploader.kt`, `UploadWorker.kt`, `AttachmentStorage.kt`, `ImageCompression.kt`, `FileUtils.kt`, `ReportViewModel*.kt`, `ReportScreen.kt` (reused; the camera helpers are copied, not moved).
- `TicketsRepository.kt`, `FirestoreTicketsRepository.kt`, `FirestoreMapping.kt`, `ShowScreen.kt` beyond the changes listed (Q2-Q4, agreed with Francisco).
- `TicketsViewModel*.kt`, `SessionViewModel.kt`, `Session.kt`, `RoleAccess.kt`, `MainTopAppBar.kt` (no new route or menu entry), `CategoriesRepository*.kt`.
- `operatorTargets` in `TicketLifecycle.kt` (unless Q8 = add INFO_REQUESTED -> OPEN).
- `DebugSeed.kt`, `SampleTickets.kt`; `AndroidManifest.xml`; Gradle files.
- `firestore.rules`, `storage.rules` (Q9), `google-services.json`; no `firebase deploy`.

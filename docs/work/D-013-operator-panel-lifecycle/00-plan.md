# D-013: Operator: operations panel and ticket lifecycle
Type: feature · Task file: docs/tasks/phase-2/D-013-operator-panel-lifecycle.md
Labels: -

## Problem
- Brief I.3.E: the operator needs an operations panel with every report, its coordinates, the author's contacts and the full history with photos and videos, and needs lifecycle control: status changes with timestamps, requesting information, recording the start of work, and uploading a photo/video of the finished work. Brief II.2-II.5: the transition order and the locked CLOSED state. The PDF was not reread in this session; the wording comes from the task file and the overview's "Brief status" note (I.3.E checked by Diogo on 2026-11-11, II not checked).
- Today `OperatorPanelRoute` (the operator's home) is still `PlaceholderScreen(top_bar_title_operator_panel)` inside `RequireAccess(OPERATOR)`; it is the last placeholder (D-012 handover).
- What already exists and is reused:
  - `TicketsViewModel.setUser(Operator)` already listens to `allTickets()` (every full ticket, newest first, F-011). So the operator's shared `tickets` list is already complete: **no `operatorTickets()` read is needed**.
  - D-008 `model/TicketLifecycle.kt`: `allowedTransitions(by, now)`, `transition(to, by, now, justification, proof)` (RESOLVED needs non-empty fresh proof, stamped `phase = RESOLUTION`, `uploadedById = by.id`), `effectiveStatus`, `isLocked`, `closeIfExpired`. D-008 already says no separate `addResolutionProof` is needed.
  - F-014 (Francisco) `FirestoreTicketsRepository`: private `commitStatusChange(before, after, message?)` (one batch: ticket status fields, `publicTickets` status mirror for PUBLIC, `ticketLocations.active = isActive`, `stats/summary` increments, optional message); public `askForClarification(ticket, by: User, question)`, which is a transition to INFO_REQUESTED **plus** the text as a message in the same batch, and works for any `User` (the lifecycle decides). `messageDocument` already stores `byOperator`. So **Request information needs no new repository code.**
  - F-014 `detailAudience(user, ticket)` already returns `OPERATOR` for operators, and `ShowScreen(ticket, OPERATOR, now) { slot }` already shows the description, exact map (`TicketMap(exact = true)`), the full history (dates, justifications, "Closed automatically") and all media. Today an operator who opens a ticket from the public list gets F-014's `TicketDetailScreen` (thread only, no actions).
  - F-013 `AttachmentUploader.prepare(attachment, uploadedById)` + `enqueue(documentPath, ticketId, addPublicThumbnail)` and `UploadWorker` (uploads only entries whose file is on this device). F-013 handover: prepare each proof before the RESOLVED batch, enqueue `("tickets/<id>", id, false)` once after it.
  - F-007 `AttachmentStorage.copyToInternalStorage(uris)`, `FileUtils.createTempImageFile`, `AttachmentGallery`; D-009 `UsersRepository.observeUser(uid)` (operators may read `users` in the draft rules); D-012 `CategoriesRepository.allCategories()` (Diogo's).
- Gaps: no panel, no filters, no operator actions, no proof flow, the operator cannot see the author's or followers' contacts, and the history does not say which operator made each change.

## Outcome
- **Operations panel** (`OperatorPanelRoute`, operator home): every ticket (PUBLIC and PRIVATE, any status) from the shared list, as expanded `TicketCard`s (category, situation, thumbnail, description, date) with the **effective** status badge and a "Private" label for PRIVATE tickets.
  - Filters: status (All, Open, Information requested, In progress, Resolved, Closed; on `effectiveStatus(now)`, so an expired RESOLVED ticket is under Closed) and category (All + every category from `allCategories()`, inactive included, so old tickets can still be found). Sort by report date, newest first (default) or oldest first. A "No tickets match these filters." empty state.
  - Filter and sort survive rotation (ViewModel state). Optional (slice 8): a List / Map switch with one exact marker per ticket.
- **Operator ticket detail** (the existing `TicketDetailRoute`, operator branch, Q1): `ShowScreen` with `DetailAudience.OPERATOR`, plus in its slot:
  - **Contacts:** the author (name, e-mail, phone) and each follower, read live from `users/{uid}`; "Contact not available" when the profile cannot be read (offline, nothing cached);
  - **History with the operator:** each entry shows its date/time (already) and "By <name>" for the person responsible (Q3, an additive parameter on `ShowScreen`); proof media are labelled "Resolution proof" (Q4);
  - **Actions**, exactly `allowedTransitions(operator, now)`:
    - **Request information** (from OPEN or IN_PROGRESS): a dialog with a required text; the text becomes the history justification **and** a message in the thread, in one batch (`askForClarification`, reused);
    - **Start work** (from OPEN or INFO_REQUESTED): a confirmation dialog, then IN_PROGRESS (optional note, Q6); this is the state D-014's geofence reads (`needsOperatorGeofence`);
    - **Resolve** (from IN_PROGRESS): opens a proof section: add photos/videos from the gallery or take a photo (1 to 3), optional note; **Confirm is disabled with no proof**; on confirm the proof is compressed, stored with the transition in one batch, and uploaded by WorkManager when there is a network;
  - the **message thread** (F-014 `MessageThread`), the operator labelled "You", replies sent as the municipality;
  - a closed ticket (stored CLOSED or RESOLVED for 5 days or more) shows **no actions and no message input**; opening an expired RESOLVED ticket writes the CLOSED entry once (`closeExpiredTicket`, as F-014).
- Each transition writes `tickets/{id}` and `publicTickets/{id}` (status only, PUBLIC tickets), `ticketLocations.active` and `stats/summary` in one batch (F-014's `commitStatusChange`), with one history entry `(status, now, justification?, operator uid)`. Citizens' and anonymous listeners show the new status live with no app change.
- Portrait and landscape (ShowScreen's two panes), EN + PT, 48dp targets, content descriptions.

## Acceptance checks
- [ ] **Operator sees private tickets and contacts; a citizen never does:** the panel lists PRIVATE tickets of other citizens; the operator detail shows the author's and followers' name, e-mail and phone. A citizen (participant or not) and an anonymous user opening the same ticket get F-014's REGISTERED view or the PUBLIC view, with no contacts and no "By <name>" (the names map is empty for them, and `observeUser` for other users is called only inside the operator branch).
- [ ] **Filters and sort:** each status chip shows only tickets with that effective status (an expired RESOLVED ticket under Closed); the category filter shows only that category; All + All shows everything; the sort toggle reverses the order; the choice survives rotation.
- [ ] **Only valid transitions are offered:** OPEN shows Request information + Start work; INFO_REQUESTED shows Start work; IN_PROGRESS shows Request information + Resolve; RESOLVED (inside 5 days) and CLOSED show none.
- [ ] **Each transition appears in the history with time and operator:** after each action the history gets one entry with the badge, date/time, "By <operator name>" and, for Request information, the text; the Request information text also appears in the thread, labelled "Municipality" for the citizen.
- [ ] **Resolve is impossible without proof media:** Confirm stays disabled with no proof; a proof that cannot be prepared (for example an undecodable image) and no other proof shows an error and writes nothing.
- [ ] **The proof uploads offline-safe:** with the operator offline (airplane mode), Resolve with a camera photo: the ticket shows RESOLVED at once with the proof tile "Upload pending"; after the network returns (app closed or not) the tile becomes the uploaded image on the operator's and the citizen's devices; Storage has `tickets/<id>/<uuid>.jpg` and `_thumb.jpg`. The proof thumbnail never goes to `publicTickets.thumbnails`.
- [ ] **The citizen sees the new status live:** with the creator's app open on My tickets or the detail (second device), each operator action changes the badge and history within seconds; an anonymous user's list drops a PUBLIC ticket once it is RESOLVED.
- [ ] Console after Resolve: `tickets/{id}` has `currentStatus: RESOLVED`, the new history entry by the operator uid, the proof entries appended (`phase: RESOLUTION`); `publicTickets/{id}.currentStatus` RESOLVED (PUBLIC only); `ticketLocations/{id}.active` false; `stats/summary` resolved +1 and active -1.
- [ ] **Closed tickets are read-only:** a stored CLOSED or 5-day-expired RESOLVED ticket shows no action buttons and the "locked" note instead of the message input.
- [ ] Citizens and anonymous users never reach the panel (`RequireAccess(OPERATOR)`, menu unchanged).
- [ ] Rotation keeps filters, open dialogs and their text, the proof draft and the typed note; landscape shows nothing clipped on the panel and the detail.
- [ ] EN and PT strings for every new text; `./gradlew assembleDebug` and `./gradlew lint` pass after each slice, with no new lint warnings beyond those listed in `state.md` (baseline from D-012: 0 errors, 22 warnings).

## Approach

### Data: where the operator's writes go (Q2, needs Francisco)
- The operator's moves must reuse F-014's private `commitStatusChange` (one batch, stats, mirror). Copying it into a new Diogo-owned repository (the D-012 pattern) would duplicate the batch and the counters logic in two places, which is worse than a small edit; the F-014 handover and decision 0018 also ask for "a new public repository function over `commitStatusChange`". So, unlike D-012, D-013 **adds two methods to Francisco's `TicketsRepository` / `FirestoreTicketsRepository`** and changes one word in `FirestoreMapping.kt`:
  - `fun startWork(ticket: Ticket, by: User, note: String? = null): Boolean`: `ticket.transition(IN_PROGRESS, by, now, note)` then `commitStatusChange(ticket, after)`; false when the lifecycle rejects it.
  - `suspend fun resolveTicket(ticket: Ticket, by: User, proof: List<Attachment>, note: String? = null): Boolean` (on `Dispatchers.IO`):
    1. dry run `ticket.transition(RESOLVED, by, now, note, proof)` with the raw draft attachments; null -> false, nothing prepared (files kept in the draft);
    2. `uploader.prepare(it, by.id)` for each proof; if none is PENDING (all FAILED) -> false (the originals are still there, `prepare` deletes only on success);
    3. `after = ticket.transition(RESOLVED, by, now, note, prepared)`;
    4. `commitStatusChange(ticket, after, newAttachments = after.attachments.drop(ticket.attachments.size))`: the batch adds `"attachments" to FieldValue.arrayUnion(...)` of the new proof entries next to `toStatusFields()`. `arrayUnion` appends without rewriting the array, so an `UploadWorker` of the creator updating its own entries at the same moment is not overwritten by this batch;
    5. after `commit()` (not awaited, queued offline), `uploader.enqueue("tickets/<id>", id, false)` once. `false`: proof thumbnails never go to `publicTickets` (decision 0010, REPORT phase only).
  - `commitStatusChange` gains one optional parameter `newAttachments: List<Attachment> = emptyList()`; the three existing callers are unchanged.
  - `FirestoreMapping.kt`: `private fun Attachment.toDocument()` becomes `internal` (used by the arrayUnion); nothing else.
  - **Request information** reuses `askForClarification(ticket, operator, text)` unchanged (it is the same batch); only its KDoc in the interface gets "also the operator's request for information (D-013)". Alternative: a delegating `requestInformation(...)` alias, more surface for no behaviour.
  - **Not added (task file superseded):** `operatorTickets()` (= the existing `allTickets()`), `addResolutionProof` (folded into `resolveTicket`, as D-008 decided), a generic `changeStatus(to)` (it would let a caller move to RESOLVED without proof or to INFO_REQUESTED without the message; explicit methods mirror F-014's `reopenTicket` / `askForClarification`).
- Concurrency: last write wins (F-014 Q12); no transaction (transactions fail offline, brief IV). Two operators acting on one ticket at the same time can each append an entry and the later batch's `statusHistory` replaces the earlier one. Known limit, as F-014.
- `resolvedAt` (Firestore-only) is **not written** (decision 0018 Q7: times from the history entry). The draft rules would refuse that batch if they were ever deployed (`resolvedAtIsServerTime`); they are not (Q9).

### Panel: `OperatorPanelScreen` (+ `OperatorPanel{UiState,ViewModel,ViewModelFactory}`)
- Tickets: the shared `tickets` from `TicketsViewModel` (already `allTickets()` for operators), passed in like `ListScreen` / `MyTicketsScreen`, so the panel and the detail read one source of truth and no second listener on `tickets` is opened (Q5).
- Filter state and categories in a route-scoped `OperatorPanelViewModel(categories: CategoriesRepository)`: `OperatorPanelUiState(categories, filter: TicketFilter, showMap)`; events `onStatusFilter(status?)`, `onCategoryFilter(categoryId?)`, `onToggleSort()`, `onToggleMap()` (slice 8).
- The rule itself is pure, in `model/TicketFilter.kt`: `data class TicketFilter(val status: TicketStatus? = null, val categoryId: String? = null, val newestFirst: Boolean = true)` and `fun List<Ticket>.filteredFor(filter: TicketFilter, now: Long): List<Ticket>` (effective status, category id, `createdAt` order with the id as tie-break). No Android imports, so it can be explained and checked by hand like D-002's rules.
- Layout: a filter header (a horizontally scrolling row of `FilterChip`s for status, an `ExposedDropdownMenuBox` for category, a sort `AssistChip` with an arrow icon and "Newest first"/"Oldest first"), then a `LazyColumn` (`key = { it.id }`) of `TicketCard(isExpanded = true, status = effectiveStatus(now))`, width capped at 600dp and centred in landscape (as D-012's lists). A small "Private" text label under PRIVATE cards is drawn by the panel next to the card, not inside `TicketCard` (Francisco's), unless Francisco prefers a parameter.
- Tap -> `TicketDetailRoute(id)`.

### Detail: `OperatorTicketScreen` (+ `OperatorTicket{UiState,ViewModel,ViewModelFactory}`)
- Route (Q1): **no new route**. In `composable<TicketDetailRoute>` the branch for `audience == OPERATOR` (only when `session.ticketsUser is Operator`) renders `OperatorTicketScreen` with its own ViewModel; participants keep F-014's `TicketDetailScreen`, everyone else `ShowScreen(PUBLIC)`. One route, one view per role (decision 0007's idea). The ticket still comes from the shared list, so the detail recomposes when the status changes.
- `OperatorTicketViewModel(tickets: TicketsRepository, users: UsersRepository, attachmentStorage: AttachmentStorage, ticketId: String, operator: Operator)`, created inside the branch (scoped to the entry, its listeners end with it):
  - thread: collects `messagesFor(ticketId)`, `draft`, `sendMessage()` (the same three lines as `TicketDetailViewModel`, Q7);
  - people: `watchPeople(ids: Set<String>)` starts one `observeUser(uid)` collection per new id (creator, followers, history responsible ids except `"system"`) into `people: Map<String, PersonContact>` (`data class PersonContact(name, email, phone, isOperator)` in the UiState file; `null` for Unavailable/Missing). The screen calls it from `LaunchedEffect(ids)`, so a new follower appears without reopening;
  - dialogs: `OperatorDialog { REQUEST_INFO, START_WORK }` with `dialogText`, `dialogError`; `confirmDialog(ticket)` calls `askForClarification` or `startWork`;
  - proof: `resolving: Boolean`, `proof: List<Attachment>`, `resolveNote`, `isCopying`, `isResolving`, `resolveError`; `onResolveClick()`, `onMediaPicked(uris)` (copies with `AttachmentStorage`, limit `MAX_PROOF_ATTACHMENTS = 3`), `onPhotoTaken(path)`, `onRemoveProof(att)`, `onResolveNoteChange`, `onCancelResolve()` (deletes the draft files), `resolve(ticket)` (sets `isResolving` before `launch`, so a double tap does nothing; on true closes the section and clears the draft without deleting the files, which now belong to the upload; on false shows `resolveError`). `onCleared()` deletes unsubmitted proof files (as `ReportViewModel`);
  - `closeIfExpired(ticket)` -> `closeExpiredTicket` (as F-014).
- `OperatorTicketScreen(ticket, operator, viewModel)` (stateful: launchers for `PickMultipleVisualMedia`/`PickVisualMedia(ImageAndVideo)` and `TakePicture` with a `rememberSaveable` temp path, above the orientation branch, as `ReportScreen`) -> `OperatorTicketContent(ticket, operator, now, state, ...)` (stateless, previews): `ShowScreen(ticket, OPERATOR, now, responsibleNames = ...) { ContactsSection; ActionsRow or ResolveSection; MessageThread }` plus the dialog.
  - Actions from `ticket.allowedTransitions(operator, now)`: INFO_REQUESTED -> "Request information", IN_PROGRESS -> "Start work", RESOLVED -> "Resolve". Locked -> nothing, `canSend = false`.
  - `ResolveSection`: heading "Proof of the finished work", `AttachmentGallery(proof, onRemove)` (capped height, safe inside the scroll), "Add from gallery" and "Take photo" buttons (disabled at 3 or while copying), an optional note field, Cancel and "Confirm resolution" (enabled only with at least one proof and not busy), and the error text.

### `ShowScreen` additions (Q3, Q4; Francisco's file, additive)
- `responsibleNames: Map<String, String> = emptyMap()` passed down to `HistoryRow`, which adds "By %1$s" when the entry's `responsibleId` is in the map (never the id). F-014's callers pass nothing, so citizens see no change.
- `MediaTile` adds the line "Resolution proof" for `phase == RESOLUTION` (all audiences; not personal data; D-008 review N2).
- Fallback if Francisco says no: the operator screen shows a short "Changes by" list in its slot instead, and proof is not labelled.

### D-014 hook (geofence)
- Only state, no geofence code: `fun Ticket.needsOperatorGeofence(now: Long): Boolean = effectiveStatus(now) == TicketStatus.IN_PROGRESS && location != Coordinates()` in `model/TicketLifecycle.kt` (Diogo's). D-014 takes the operator's `tickets` and filters with it; INFO_REQUESTED pauses it by construction (F-014 handover). Start work writes IN_PROGRESS through the normal batch, so D-014 needs no other signal. "Work started" notifications are F-016's (status-change listener).

### Navigation and shell
- `composable<OperatorPanelRoute>`: `OperatorPanelScreen(tickets, viewModel, onTicketClick)` inside the existing `RequireAccess(OPERATOR)`. `Destinations.kt`: the `OperatorPanelRoute` KDoc loses "Placeholder". `PlaceholderScreen.kt` then has no caller; keep or delete (Q10).
- `composable<TicketDetailRoute>`: the operator branch above. No new route, no `TopBarScreen` change.

### Rules
- No edit and no deploy (Q9). The drafts already allow the operator's moves, the messages and `users` reads; they would refuse the resolve batch without `resolvedAt`, which is already true for F-014's reopen (decision 0018 Q7).

## Architecture impact
- `model/`: new `TicketFilter.kt` (pure); `TicketLifecycle.kt` gains `needsOperatorGeofence`.
- `data/` (Francisco's files, Q2): `TicketsRepository` + `FirestoreTicketsRepository` gain `startWork` and `resolveTicket`; `commitStatusChange` gains `newAttachments`; `FirestoreMapping.Attachment.toDocument()` becomes `internal`; `askForClarification` KDoc.
- `viewmodel/`: `OperatorPanel{UiState,ViewModel,ViewModelFactory}`, `OperatorTicket{UiState,ViewModel,ViewModelFactory}` (with `OperatorDialog`, `PersonContact`), route-scoped, plain factories.
- `ui/`: `OperatorPanelScreen.kt`, `OperatorTicketScreen.kt`; `ShowScreen.kt` two additive changes (Q3, Q4); optional `components/TicketsMap.kt` (slice 8).
- `MainActivity.kt`: panel destination, operator detail branch. No new route, no rules, no Gradle change.

## Failure modes
| Situation | Behaviour |
| --- | --- |
| Operator offline, Request information / Start work | Batch queued; the panel, detail and history update at once from the cache; the citizen sees it when both are online |
| Operator offline, Resolve | Proof prepared locally; batch queued; ticket RESOLVED at once with "Upload pending"; `UploadWorker` waits for a network (even after the app is closed), retries if the batch is not on the server yet |
| All proof media fail to prepare (undecodable image) | `resolveTicket` returns false; "The proof could not be prepared." ; nothing written; draft kept |
| Some proof fails to prepare | Resolved with the others; the failed one is stored FAILED ("Upload failed"), as F-013 does for reports |
| Status changed by someone else meanwhile (dialog open on a stale ticket) | The ticket passed to `confirmDialog`/`resolve` is the current one from the list; if the move is no longer allowed the lifecycle rejects it: "This action is no longer available." Offline concurrent changes: last write wins (Q12 of F-014) |
| Operator leaves the detail while Resolve is preparing | `viewModelScope` is cancelled; already-prepared files may be orphaned and nothing is written (known limit; rare, preparation takes about a second per photo) |
| Proof file lost before upload (app data cleared) | Entry stays PENDING forever (F-013 limit) |
| Two workers (creator's and operator's) update `attachments` at once | Pre-existing F-013 get-then-update race; the resolve batch itself uses `arrayUnion` and does not add to it |
| Contact profile unreadable (offline, nothing cached) or missing | "Contact not available" for that person; rest of the detail works |
| A history entry by `"system"` | "Closed automatically" (F-014), never looked up as a user |
| Category filter on a deleted or renamed category | Filter is by `categoryId`; renamed shows the new name in the dropdown, cards keep the ticket's snapshot |
| Ticket at (0, 0) (hand-made) | "Location not available" (ShowScreen); excluded from the optional map and from `needsOperatorGeofence` |
| Missing Maps key | Grey tiles (F-012 limit) |
| Rules deployed one day | The resolve batch is refused (no `resolvedAt`); logged only. Not the case in this delivery (test mode) |

## Risks and open questions

### Risks
- **R1 Francisco's files.** Slices 4, 5 and 6 edit `TicketsRepository.kt`, `FirestoreTicketsRepository.kt`, `FirestoreMapping.kt` and `ShowScreen.kt`. Diogo must agree the signatures with Francisco before slice 5 (task file). Expect merge conflicts in those files, `MainActivity.kt`, both `strings.xml` and the overview.
- **R2 Deadline (2026-11-22).** Target was 2026-11-18 and D-014 depends on this. Slices 1-7 are the must; 8 and 9 are optional and can be dropped without touching the rest.
- **R3 Device checks need two devices** (operator + citizen) and the operator/citizen test accounts; the offline resolve check needs airplane mode and a real or emulated camera.
- **R4 Rules not deployed.** "Only operators change status / see contacts" is enforced by the app (role branch, `RequireAccess`, ViewModel scope), as in D-012. Say so at the defence.
- **R5 Last write wins** for concurrent status changes (F-014 Q12), and the F-013 attachments race. Documented, not fixed.
- **R6 Brief II not checked** in the PDF (overview "Brief status"); the transition table is D-008's reading.

### Open questions (Gate 1)
1. **Q1 Operator detail route.** Recommended: no new route; the operator branch of `TicketDetailRoute` renders `OperatorTicketScreen` (one route, one view per role; ids from the panel and from the public list both land there). Alternative: a new `OperatorTicketRoute(ticketId)` guarded by `RequireAccess(OPERATOR)` plus a `TopBarScreen` value, leaving F-014's operator view reachable from the public list.
2. **Q2 Repository methods (agree with Francisco).** Recommended signatures, added to his `TicketsRepository`: `fun startWork(ticket: Ticket, by: User, note: String? = null): Boolean` and `suspend fun resolveTicket(ticket: Ticket, by: User, proof: List<Attachment>, note: String? = null): Boolean`; Request information reuses `askForClarification` (KDoc line only); private `commitStatusChange` gains `newAttachments`; `Attachment.toDocument()` becomes `internal`. No `operatorTickets` (= `allTickets`) and no `addResolutionProof`. Alternative: a new Diogo-owned `OperatorTicketsRepository` with its own batch (no edit to his files, but the stats/mirror batch duplicated).
3. **Q3 Operator name in the history** ("each one appears in the history with time and operator"). Recommended: an additive `responsibleNames: Map<String, String> = emptyMap()` on Francisco's `ShowScreen`, "By <name>" only when given (operators only). Alternative: a separate "Changes by" list in the operator slot (no edit to `ShowScreen`, but the history shows twice).
4. **Q4 Proof label.** Recommended: "Resolution proof" line on `MediaTile` for RESOLUTION media, for every audience (D-008 N2). Alternative: no label.
5. **Q5 Panel source.** Recommended: the shared `TicketsViewModel.tickets` (already `allTickets()` for operators) filtered by a pure `filteredFor`; the panel ViewModel holds only the filter and categories. Alternative: the panel ViewModel collects `allTickets()` itself (a second listener; filtered list in its UiState).
6. **Q6 Start work.** Recommended: a confirmation dialog with an optional note (stored as justification), since there is no undo. Alternative: one tap, no dialog, no note.
7. **Q7 Thread in the operator ViewModel.** Recommended: `OperatorTicketViewModel` has its own thread (three short functions copied from `TicketDetailViewModel`), so one screen has one ViewModel. Alternative: reuse `TicketDetailViewModel` for the thread next to the operator one (two ViewModels per screen).
8. **Q8 INFO_REQUESTED -> OPEN** (F-014 handover). Recommended: **no change**; the operator's only move out of INFO_REQUESTED stays Start work (brief II order; D-008 table), and the operator answers in the thread. Alternative: add INFO_REQUESTED -> OPEN for operators in `operatorTargets` and in the draft rules (rules edit needs Diogo's OK).
9. **Q9 Rules.** Recommended: **no edit, no deploy**. The drafts would refuse the resolve batch without `resolvedAt` (same mismatch as F-014's reopen); updating them (accept a missing `resolvedAt`, or write `serverTimestamp()` on resolve) needs Diogo's explicit approval and Francisco's agreement. Alternative: write `resolvedAt: FieldValue.serverTimestamp()` on `tickets` and `publicTickets` in the resolve batch so the drafts would pass (unused by the app).
10. **Q10 `PlaceholderScreen.kt`.** Recommended: delete it (and its string if unused elsewhere) in slice 2, since `OperatorPanelRoute` was its last caller. Alternative: keep it for later placeholders.
11. **Q11 Proof limits.** Recommended: 1 to 3 media, gallery photo/video or camera photo (no video recording, as F-007), optional note. Alternative: exactly one photo.
12. **Q12 Optional slices.** Recommended: do slice 8 (panel map) only if slices 1-7 are done and checked by 2026-11-19; slice 9 (joiners' `reports/{uid}` descriptions and media, F-012 handover) only if time remains. Both are droppable.
13. **Q13 Who may see "By <name>".** Recommended: operators only (citizens see the time and "Municipality" via the thread, not operator names). Alternative: citizens see "By the municipality" on operator entries.

## Out of scope
- The geofence itself, background location, proximity alerts (D-014); notifications of status changes or messages (F-016).
- Assigning tickets to operators, editing a ticket's data, deleting tickets or media, retrying FAILED uploads, video recording, upload progress.
- Calling or e-mailing the author from the app (intents); exporting the panel.
- Deploying or editing `firestore.rules` / `storage.rules` (unless Q8/Q9 are approved otherwise).
- Fixing the F-013 attachments race or the last-write-wins history.
- Optional unless Q12 says so: the panel map (slice 8) and the joiners' reports (slice 9).

## Slices
1. **Model: filter rule.** `model/TicketFilter.kt` (`TicketFilter`, `filteredFor`). No caller yet; build green.
2. **Operations panel.** `OperatorPanel{UiState,ViewModel,ViewModelFactory}`, `OperatorPanelScreen` (filters, sort, list, empty state), wired into `composable<OperatorPanelRoute>`, tap opens `TicketDetailRoute`; `PlaceholderScreen` removed if Q10; strings.
3. **Operator detail, read-only.** `OperatorTicket{UiState,ViewModel,ViewModelFactory}` (thread, people), `OperatorTicketScreen` (ShowScreen OPERATOR + contacts + thread, closed = locked), `closeIfExpired`, operator branch in `composable<TicketDetailRoute>`; strings.
4. **History by operator and proof label** (if Q3/Q4). Additive `responsibleNames` and the RESOLUTION line in `ShowScreen`; the operator screen passes the names.
5. **Request information and Start work.** `startWork` in the repository; `askForClarification` KDoc; `needsOperatorGeofence` in `TicketLifecycle.kt`; dialogs and buttons from `allowedTransitions`.
6. **Resolve with proof.** `resolveTicket`, `commitStatusChange(newAttachments)`, `Attachment.toDocument()` internal; proof draft, launchers, `ResolveSection`, cleanup.
7. **Polish and checks.** Landscape, accessibility, previews (each status, locked, resolve section with and without proof, dialog with error), the "Private" label, PT strings review; device checks D1-D12.
8. **Optional: panel map.** `components/TicketsMap.kt` (one exact marker per filtered ticket, tap opens the detail) and a List/Map toggle in the panel.
9. **Optional: joiners' reports.** `reportsFor(ticketId)` in the repository and a "Reports from followers" section (description, media) in the operator detail.

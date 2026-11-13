# F-011: Firestore tickets repository and public projection
Type: feature · Task file: docs/tasks/phase-2/F-011-firestore-tickets-repository.md
Labels: (none)

## Problem
Tickets and categories live in memory (`LocalTicketsRepository`, decision 0009), so nothing is shared between devices and a new process starts again from the samples. Brief IV wants tickets and categories in Firestore with the native offline cache. F-010 set up Firebase (`CityFixApplication.firestore` with the persistent cache) and fixed the layout (decision 0010), but nothing reads or writes it yet. Anonymous users must read only `publicTickets` (rules cannot hide fields; CLAUDE.md, decision 0006 "Public projection contract").

## Outcome
- `data/FirestoreTicketsRepository` implements `TicketsRepository`; `CityFixApplication` uses it; `LocalTicketsRepository` is deleted (Q4).
- Reads are live: Firestore snapshot listeners exposed as Kotlin `Flow`s, collected by the ViewModels into their own `StateFlow`s (class pattern, P08). A report on one device shows up on another without a refresh; reads come from the cache offline.
- Reads depend on the audience: anonymous -> `publicTickets` (active only); citizen -> `tickets` where `participantIds` contains their id, plus active `publicTickets`; operator -> all `tickets`.
- `reportTicket` writes one batch: `tickets/{id}`, `ticketLocations/{id}` (`active: true`), `publicTickets/{id}` for PUBLIC tickets (built only from `toAnonymousView()`), and the `stats/summary` counters. The batch is queued offline; the form closes without waiting for the server.
- Categories come from `categories` (active only) as a live list; a debug-only seed writes the sample categories (and, Q5, the sample tickets) once into an empty database.
- The dashboard (I.1.A) reads `stats/summary`: counters kept with `FieldValue.increment` in the same batch as every ticket change (Q3).
- Until D-009 there is no login: a single stand-in "current user" in `CityFixApplication` decides the audience and the creator id (Q1).

## Acceptance checks
Checked on devices (two emulators, or emulator + phone, both with `app/google-services.json`). Full list with steps in `01-coding-plan.md`, "Device check list".
- [ ] A report made on one device appears on another (list and dashboard) within seconds, with no refresh.
- [ ] In airplane mode, after a cold start, the list and dashboard load from the cache; a new report closes the form at once, shows in the local list, and reaches the console (and the other device) after the network returns.
- [ ] As anonymous (current user = null), no listener or read targets `tickets/` or `ticketLocations/` (checked with temporary Firestore debug logging, not committed, plus code review); the list shows category, situation, status badge and the thumbnail slot only (real thumbnails arrive with F-013, see Risks).
- [ ] `publicTickets/{id}` in the console has only `categoryId`, `categoryName`, `situation`, `currentStatus`, `coarseLocation`, `createdAt`, `thumbnails`; a PRIVATE report has no `publicTickets` document but has `tickets/{id}` and `ticketLocations/{id}`.
- [ ] Dashboard: total tickets (private included), % active, average repair time, matching `stats/summary`; with the seeded samples: total 3, active 67 %, 3 days.
- [ ] The report form lists active categories only (no "Graffiti").
- [ ] As citizen (stand-in `Citizen`, ACTIVE): own PRIVATE ticket and public tickets listed. As operator (stand-in `Operator`): every ticket, RESOLVED and PRIVATE included.
- [ ] Rotation keeps the list and an open form; no duplicate listeners (one log line per snapshot).
- [ ] `./gradlew assembleDebug` and `./gradlew lint` pass after every slice.

## Approach

### Class material first (the person's standing rule)
What the course taught and how F-011 uses it:
- **09_Storage + T8 `AMOVStorageDemo`:** `FirebaseFirestore.getInstance()`, `collection().document().set(hashMap)`, `collection().get()` with `addOnSuccessListener`/`addOnFailureListener`, manual mapping with `doc.getString(...)`, and `db.batch()` + `batch.delete` + `batch.commit()` (the demo's "clear all"). Slides name "real-time listeners" and "automatic offline sync" but show no code for them.
  - Followed: the single app instance (`app.firestore`, F-010; equivalent to `getInstance()` but with the cache settings applied first), `hashMapOf`/`mapOf` documents, manual mapping with `getString`/`getLong`/`get` (no `toObject()`), `batch()` + `commit()` with `addOnFailureListener`, documents set by id.
- **08_Coroutines_Flows_Services:** cold `flow { }`, `collect`, "cancelling the collector aborts the emitter", `StateFlow` for UI state, `MutableStateFlow` + `asStateFlow()` + `update` in a ViewModel.
- **P07/P08 (Contacts_#3):** the ViewModel owns private `MutableStateFlow`s, fills them in `viewModelScope`, the screen uses `collectAsStateWithLifecycle()`; factories receive the repository (decision 0009).

**Beyond class (flagged):**
1. **`addSnapshotListener`** (live query). The demo only uses one-shot `get()`. Needed because acceptance check 1 ("appears on another device") needs live updates, and the listener is how Firestore serves the offline cache on every change. Reason accepted in the task file ("snapshot listeners").
2. **`callbackFlow { ... awaitClose { registration.remove() } }`** to turn a listener into a `Flow`. Not in the slides; it is the `flow { }` builder for callback APIs. Reason: the listener is removed automatically when `viewModelScope` cancels the collector (the slide "cancelling collectors aborts emitters"), so there is no manual `onCleared` bookkeeping, and the task file asks for `Flow`. Alternative kept as Q2: callback functions returning a small `Subscription` handle, closer to `addOnSuccessListener`.
3. **`combine`** of two flows for the citizen list (own tickets + public ones). One operator, explained in KDoc.
4. **`FieldValue.increment`** and `SetOptions.merge()` for the dashboard counters (Q3).
5. **Not used, on purpose:** transactions (fail offline; decision 0010), `await()` (a `commit()` never completes offline, so awaiting it would freeze the form; see Failure modes), `stateIn`/`flatMapLatest` (the ViewModel keeps the class `MutableStateFlow` pattern), security-rule deploys (decision 0012 Q5).

### Repository API (decision 0009 amended)
`TicketsRepository` becomes (full signatures in `01-coding-plan.md`):
- `publicTickets(): Flow<List<Ticket>>` - active `publicTickets`, already in the projected shape, newest first.
- `participantTickets(userId): Flow<List<Ticket>>` - full tickets where `participantIds` contains `userId`.
- `allTickets(): Flow<List<Ticket>>` - every full ticket (operators).
- `stats(): Flow<DashboardStats>` - from `stats/summary`.
- `activeCategories(): Flow<List<Category>>` - `categories` where `active == true`, sorted by name with `Collator`.
- `suspend fun reportTicket(..., creatorId: String): String?` - same validation and EXIF step as today; the creator id becomes a parameter so the repository holds no session.
- `getTicketById` and `loadStats`/`loadPublicTickets` are removed (no caller for the first; flows replace the others).

Queries need no composite index: `whereIn("currentStatus", [OPEN, INFO_REQUESTED, IN_PROGRESS])`, `whereArrayContains("participantIds", uid)` and `whereEqualTo("active", true)` are single-field; sorting is done on the client with the existing `newestFirst` order (via `TicketsList(...).getAllSorted()`), so no `orderBy` and no index link in Logcat.

### Mapping (all in `data/`, decision 0010)
`data/FirestoreMapping.kt` holds every model <-> Firestore conversion, written by hand like the demo:
- `Ticket.toTicketDocument(): Map<String, Any?>` - the 0010 `tickets` fields: enums by name, `location` as `GeoPoint`, `participantIds = [creatorId] + followerIds`, `statusHistory` as a list of maps (`justification` omitted when null), `attachments` as `{id, type, phase, storagePath, thumbnailPath?, uploadState, uploadedById}` where `storagePath` = `Attachment.file` (a local path while PENDING, per the model's KDoc; F-013 replaces it). No `resolvedAt` on create.
- `Ticket.toPublicTicketDocument()` - **calls `toAnonymousView()` first** and copies only the 7 allowed keys; `coarseLocation` from the projected location; `thumbnails` = REPORT-phase `thumbnail`s that are not null (empty until F-013).
- `Ticket.toTicketLocationDocument()` - `categoryId`, `situation`, `location` (exact), `active = currentStatus != CLOSED`.
- `DocumentSnapshot.toTicket()`, `DocumentSnapshot.toPublicTicket()` (scope PUBLIC, attachments rebuilt from `thumbnails`), `DocumentSnapshot.toCategory()`, `DocumentSnapshot.toDashboardStats()`. Missing fields read as model defaults; unknown enum names fall back to the default instead of crashing; a missing `phase` reads as REPORT (decision 0011).
- `statsIncrements(before: Ticket?, after: Ticket): Map<String, FieldValue>` - see "Dashboard".

### Audience and creator id until D-009 (Q1)
No Firebase anonymous auth (brief I.1, decision 0010). `CityFixApplication` gets one stand-in:
```kotlin
/** F-011 stand-in for the signed-in user until D-009: null = anonymous. */
val currentUser: User? = null
```
- Anonymous = `null`, citizen = a `Citizen(id = DEV_CITIZEN_ID, registrationStatus = ACTIVE)`, operator = an `Operator(id = DEV_OPERATOR_ID)`. To test another audience the person edits this one line and reinstalls (device check list).
- The audience is the type of `currentUser` (sealed `User`, decision 0003), so no new enum.
- Creator id for reports: `currentUser?.id ?: DEV_CITIZEN_ID`. Anonymous users keep reporting as in phase 1 (the "+" stays) so reports can be tested before login; D-009 hides "+" for anonymous users and passes the real uid.
- `TicketsViewModel` gets `fun setUser(user: User?)` that cancels the previous listener job and starts the right flows; `init` calls it with the factory's user. D-009 calls `setUser` when Auth changes. This is the seam D-009 replaces.

### TicketsViewModel (Diogo's file; tell him)
- Keeps `tickets: StateFlow<List<Ticket>>` and `stats: StateFlow<DashboardStats>` (D-003 Q6: two flows, no loading state).
- `setUser(user)`: `listenJob?.cancel()`, then `listenJob = viewModelScope.launch { ... collect { _tickets.value = it } }` with:
  - `null` -> `repository.publicTickets()`;
  - `Citizen` -> `combine(repository.participantTickets(id), repository.publicTickets())`: own full tickets replace the public copy with the same id, then newest first;
  - `Operator` -> `repository.allTickets()`.
- `stats` collected once in `init` (same for every audience).
- `refresh()` is removed (listeners make it pointless); `MainActivity` no longer calls it after a report. `getTicketById` unchanged (searches the current value).
- Screens are unchanged: `MainActivity` keeps `DetailAudience.PUBLIC` and `canExpandCards = false`; mapping the role to `REGISTERED`/`OPERATOR` and the expand toggle is D-009's (decision 0007). Safe because a citizen's view of other people's tickets comes from `publicTickets` (already projected).

### Writes
`reportTicket` (on `Dispatchers.IO`): validate and trim as today -> strip photo GPS EXIF (`FileUtils.removeImageLocation`, F-009; the step decision 0006 requires of a Firebase repository) -> `val id = firestore.collection("tickets").document().id` (auto id, offline) -> build the `Ticket` (OPEN, one history entry by `creatorId`, attachments PENDING with `uploadedById = creatorId`, location (0, 0) until F-012) -> one `firestore.batch()` with `set` on `tickets/{id}`, `ticketLocations/{id}` and, if PUBLIC, `publicTickets/{id}`, plus `set(stats/summary, statsIncrements(null, ticket), SetOptions.merge())` -> `commit().addOnFailureListener { Log.w(...) }` -> return `id` immediately. The local cache applies the batch at once, so every listener on this device sees the ticket before the server does.

### ticketLocations (Q6)
Written now in the create batch (`active: true`, exact location, which is (0, 0) until F-012 adds GPS), so every ticket already has the shape decision 0010 and the drafted create rule (`existsAfter(ticketLocations)`) expect. F-012 adds the real location and the grouping read; `findNearby` already ignores (0, 0).

### Dashboard (Q3)
Brief I.1.A: average repair time, total tickets, % active, for anonymous users who cannot read private tickets. Decision 0005 Q9: totals include private tickets.
- **Recommended: counters in `stats/summary`** - `totalTickets`, `activeTickets`, `resolvedTickets`, `repairTimeSumMillis`, changed with `FieldValue.increment` in the **same batch** as every ticket write. The increment is the difference of the existing pure rule `TicketsList.stats()` applied to the ticket before and after the write: `statsIncrements(before, after)` = `stats(after) - stats(before)` field by field. So the dashboard keeps decision 0005's exact meaning (active = OPEN/INFO_REQUESTED/IN_PROGRESS, repair time = last RESOLVED - createdAt, only for inactive tickets), reopen subtracts the earlier repair, the automatic close changes nothing. Works offline (increments queue), one tiny document cached for everyone, no aggregation queries. Average = `repairTimeSumMillis / resolvedTickets` in `toDashboardStats()`.
- F-011 writes only the create (+1 total, +1 active). D-013 (operator transitions) and F-014 (reopen) must add `statsIncrements(before, after)` to their batches: handover.
- Alternatives: (b) statistics over `publicTickets` only (has `createdAt` and `resolvedAt`, so repair time works) - simplest, but excludes private tickets, against 0005 Q9; (c) an operator's device recomputes `stats/summary` from all tickets on each snapshot - stale when no operator app is open, and none exists before D-013; (d) Firestore `count()`/`average()` aggregations - server-only (no offline), and anonymous users could not query `tickets` under the drafted rules.

### Categories and seed (Q5)
- `ReportViewModel` collects `repository.activeCategories()` in `init` into `uiState.categories`; the form may show an empty dropdown for a moment on first launch. `submit()` keeps rejecting an id not in the list.
- `data/DebugSeed.kt`: `seedIfEmpty(firestore)` called from `CityFixApplication.onCreate()` only when the app is debuggable (`ApplicationInfo.FLAG_DEBUGGABLE`). It reads `stats/summary` with `Source.SERVER`; only if the document does not exist does it write one batch: `SampleCategories.all` (graffiti inactive) and, if Q5 = yes, `SampleTickets.all` through the same mapping + `statsIncrements(null, ticket)` (sample-3 gets `resolvedAt` = its RESOLVED entry as a `Timestamp`, seed only). Offline the server read fails, so nothing is written. File and call removed before delivery (D-015 checklist).

### Alternatives considered
- Keep one-shot `get()` (demo) with `refresh()`: no live updates, so a report on device A does not appear on B until B refreshes (fails check 1).
- Repository-owned `StateFlow`s / `stateIn`: rejected again (decision 0009: the ViewModel owns its state, as in Contacts_#3).
- `toObject<Ticket>()`: needs Firestore field names equal to model names and cannot map `GeoPoint` to `Coordinates` or `storagePath` to `file`; manual mapping matches the demo.
- Firebase Anonymous Auth for anonymous users: rejected by brief I.1 and decision 0010.

## Architecture impact
- `data/`: new `FirestoreTicketsRepository.kt`, `FirestoreMapping.kt`, `DebugSeed.kt` (temporary); `TicketsRepository.kt` API changes from suspend loads to flows; `LocalTicketsRepository.kt` deleted. `SampleTickets`/`SampleCategories` stay (previews and seed).
- `viewmodel/`: `TicketsViewModel` (+Factory gets the user), `ReportViewModel` (+Factory gets the creator id). Public API change: `refresh()` and the synchronous categories are gone.
- `CityFixApplication`: the repository is Firestore-backed; new `currentUser` stand-in; debug seed call.
- `MainActivity`: no `refresh()` after a report; factories receive the user / creator id.
- `model/TicketPrivacy.kt`: KDoc only (the EXIF step is now in `FirestoreTicketsRepository`). Diogo's file.
- Firestore: first real data in `categories`, `tickets`, `publicTickets`, `ticketLocations`, `stats/summary`. `firestore.rules` draft gets a `stats/summary` block only if the person agrees (Q7; not deployed either way).
- Cross-branch contract for Diogo: the new `TicketsRepository` API, `TicketsViewModel.setUser`, `statsIncrements` for D-013's batches, `currentUser` seam for D-009.

## Failure modes
- **Awaiting a commit offline.** `batch.commit()` completes only when the server acknowledges; offline that is "later". `reportTicket` returns the id right after `commit()` and only logs failures. Cost: a write the server later rejects (only possible once rules are deployed, or after the test-mode expiry on 2026-12-10) is not shown to the user; logged with `Log.w`.
- **Listener error** (permission denied after 2026-12-10, network edge cases): logged, the flow keeps its last value (no `close(error)`, so the ViewModel's collector does not throw and crash the app). The list stays as last seen.
- **Empty cache + offline at first install:** lists, dashboard and categories are empty; the form cannot be submitted (no category). Accepted; brief IV's offline promise applies after the first sync.
- **Malformed documents** (edited in the console): unknown enum names and missing fields fall back to defaults; a document without a `currentStatus` reads as OPEN. Never throws.
- **Duplicate listeners on rotation:** prevented because listeners live in `viewModelScope` (ViewModel survives rotation) and `setUser` cancels the previous job.
- **Counter drift:** if a future write path forgets `statsIncrements`, `stats/summary` drifts from the tickets. Mitigated by one helper and the D-013/F-014 handover; repair: delete `stats/summary` and re-run the seed logic (debug) or recompute by hand.
- **Clock:** `createdAt` and the first history `dateTime` are device time (as in phase 1 and decision 0010).

## Risks and open questions
Each with a recommended default for Gate 1.

- **Q1. Audience and creator id before D-009.** Recommended: the `currentUser: User?` stand-in in `CityFixApplication` (default `null` = anonymous; edit one line to test citizen/operator), creator id `currentUser?.id ?: DEV_CITIZEN_ID` (`"dev-citizen-1"`), anonymous reporting kept until D-009, `TicketsViewModel.setUser` as D-009's seam. Alternatives: a debug-only "View as" menu in the top bar (switch without reinstalling; needs strings in EN/PT and UI work), or wait for D-009 (blocks F-012/F-013).
- **Q2. Flow (callbackFlow) or callbacks for reads.** Recommended: `callbackFlow` (beyond class, reason above). Alternative closer to class: `fun listenPublicTickets(onChange: (List<Ticket>) -> Unit): Subscription` with `fun interface Subscription { fun cancel() }`, the ViewModel keeping the handles and cancelling them in `onCleared()` and in `setUser`.
- **Q3. Dashboard source (I.1.A).** Recommended: `stats/summary` counters with `FieldValue.increment` from `statsIncrements(before, after)` in every ticket batch. Alternatives (b)-(d) above.
- **Q4. `LocalTicketsRepository`: keep or delete.** Recommended: delete. Previews use `SampleTickets` directly, nothing else uses it, and keeping it means rewriting it for the flow API with no consumer. Decision 0009 records it.
- **Q5. Seed content.** Recommended: debug-only automatic `seedIfEmpty` (guarded by a server read of `stats/summary`) writing the categories **and** the three sample tickets, because without an operator UI (D-013) nothing can resolve a ticket and the repair-time figure would stay empty. Removed before delivery. Alternatives: categories only; a debug button on the dashboard (needs strings and UI); typing the categories in the console.
- **Q6. `ticketLocations` now or in F-012.** Recommended: now, in the create batch (shape of decision 0010; location (0, 0) until F-012).
- **Q7. Rules draft for `stats/summary`.** Decision 0010 says F-011 adds them; CLAUDE.md says to ask before changing rules. Recommended: add a small draft block (read: everyone; write: active citizens and operators, only the four counter keys, values `int`), **not deployed** (decision 0012 Q5). Alternative: leave the path denied in the draft and record the gap in 0010.
- **Q8. Decision number.** Recommended: a new record `0013-firestore-repository-and-dashboard-counters.md`, after checking with Diogo that he has not taken 0013 on `dev/Diogo`.
- **Risk: thumbnails.** Until F-013 uploads media, `publicTickets.thumbnails` is empty and remote devices cannot open local paths, so the anonymous card shows its placeholder. Acceptance check 3's "thumbnail" is fully met only after F-013.
- **Risk: test data with stand-in ids** (`dev-citizen-1`, `citizen-1`) stays in the shared database; D-015 decides whether to clean it before the defence.
- **Risk: Diogo's files** (`TicketsViewModel.kt`, `TicketPrivacy.kt` KDoc) change on `dev/Francisco`; tell him before he edits them on `dev/Diogo` (D-009 also touches `MainActivity`/`TicketsViewModel`), to avoid merge conflicts.
- **Risk: decision 0009 consequence "saved flag"** (rotation mid-save) is not fixed here; with an immediate return after `commit()` the window is a few milliseconds of EXIF stripping. Recorded, out of scope.

## Out of scope
Location, 10 km gate, grouping and map (F-012); media compression, upload, thumbnails, video metadata (F-013); messages, "my tickets" UI and reopen (F-014); operator lifecycle writes, `publicTickets` status mirror and `resolvedAt` (D-013); category management (D-012); login, role-based `DetailAudience` and expand toggle (D-009); notifications (F-016); deploying rules.

## Slices
Each leaves `assembleDebug` and `lint` green and becomes one commit.
1. **Mapping.** `data/FirestoreMapping.kt`: model -> document maps (tickets, publicTickets via `toAnonymousView()`, ticketLocations), snapshot -> model (full ticket, public ticket, category, stats), `statsIncrements`. No callers yet.
2. **Firestore repository.** `data/FirestoreTicketsRepository.kt`: the five read flows (`callbackFlow` + snapshot listener) and the `reportTicket` batch with stats increments. Standalone class, not wired yet.
3. **Switch.** `TicketsRepository` to the flow API, `FirestoreTicketsRepository` implements it, delete `LocalTicketsRepository`, `currentUser` stand-in in `CityFixApplication`, `TicketsViewModel(+Factory)` with `setUser`, `ReportViewModel(+Factory)` with the categories flow and creator id, `MainActivity` without `refresh()`, `TicketPrivacy.kt` KDoc.
4. **Debug seed.** `data/DebugSeed.kt` + the debuggable-only call in `CityFixApplication.onCreate()`.
5. **Rules draft** (only if Q7 = yes). `stats/summary` block in `firestore.rules`, not deployed.

# F-011 coding plan
Written against the recommended Gate 1 defaults (Q1-Q8 in `00-plan.md`). If the person picks another option, the affected step is rewritten before coding and the change is logged in `state.md`.

## Files
| Path | Change | Why |
| --- | --- | --- |
| `app/src/main/java/pt/isec/amov/cityfix/data/FirestoreMapping.kt` | new | All model <-> Firestore conversion in `data/` (decision 0010); `statsIncrements` (Q3) |
| `app/src/main/java/pt/isec/amov/cityfix/data/FirestoreTicketsRepository.kt` | new | Live reads (snapshot listeners as `Flow`) and the report batch |
| `app/src/main/java/pt/isec/amov/cityfix/data/TicketsRepository.kt` | edit | Suspend loads -> flows; `reportTicket` gets `creatorId`; synchronous categories -> flow |
| `app/src/main/java/pt/isec/amov/cityfix/data/LocalTicketsRepository.kt` | delete | Q4: no consumer; previews use `SampleTickets` |
| `app/src/main/java/pt/isec/amov/cityfix/data/DebugSeed.kt` | new (temporary) | Q5: one-off seed of sample categories (+ tickets) in debug builds; removed before delivery |
| `app/src/main/java/pt/isec/amov/cityfix/CityFixApplication.kt` | edit | Firestore repository, `currentUser` stand-in and dev ids (Q1), seed call in `onCreate` |
| `app/src/main/java/pt/isec/amov/cityfix/viewmodel/TicketsViewModel.kt` | edit (Diogo's file) | Audience-aware listeners, `setUser`, no `refresh()` |
| `app/src/main/java/pt/isec/amov/cityfix/viewmodel/TicketsViewModelFactory.kt` | edit (Diogo's file) | Passes the user |
| `app/src/main/java/pt/isec/amov/cityfix/viewmodel/ReportViewModel.kt` | edit | Categories flow; passes `creatorId` |
| `app/src/main/java/pt/isec/amov/cityfix/viewmodel/ReportViewModelFactory.kt` | edit | Passes `creatorId` |
| `app/src/main/java/pt/isec/amov/cityfix/MainActivity.kt` | edit | Factories get user / creator id; `onSubmitted` drops `refresh()` |
| `app/src/main/java/pt/isec/amov/cityfix/model/TicketPrivacy.kt` | edit, KDoc only (Diogo's file) | The EXIF step now lives in `FirestoreTicketsRepository.reportTicket`; the projection is the content of `publicTickets` |
| `firestore.rules` | edit, only if Q7 = yes | `stats/summary` draft block; not deployed |

No Gradle change: `firebase-firestore` and the BOM are already in the catalog (F-010). `kotlinx-coroutines-play-services` stays unused here (no `await()`).

## Interfaces

### `data/TicketsRepository.kt`
```kotlin
interface TicketsRepository {
    /** Active publicTickets (anonymous projection), newest first. Never reads tickets/. */
    fun publicTickets(): Flow<List<Ticket>>
    /** Full tickets whose participantIds contain [userId] (creator or follower), newest first. */
    fun participantTickets(userId: String): Flow<List<Ticket>>
    /** Every full ticket, newest first (operators). */
    fun allTickets(): Flow<List<Ticket>>
    /** Dashboard figures from stats/summary (decision 0005 meaning; private tickets counted). */
    fun stats(): Flow<DashboardStats>
    /** Active categories (I.3.E), sorted by name with Collator. */
    fun activeCategories(): Flow<List<Category>>
    /** Same validation and result as phase 1; [creatorId] is the reporter. Returns once the batch is queued. */
    suspend fun reportTicket(
        description: String,
        categoryId: String,
        categoryName: String,
        situation: Situation,
        scope: Scope,
        attachments: List<Attachment>,
        creatorId: String,
    ): String?
}
```

### `data/FirestoreMapping.kt` (top-level, `internal` where only `data/` uses it)
```kotlin
const val TICKETS = "tickets"; const val PUBLIC_TICKETS = "publicTickets"
const val TICKET_LOCATIONS = "ticketLocations"; const val CATEGORIES = "categories"
const val STATS_SUMMARY = "stats/summary"

fun Ticket.toTicketDocument(): Map<String, Any?>
fun Ticket.toPublicTicketDocument(): Map<String, Any?>   // calls toAnonymousView() first; 7 keys only
fun Ticket.toTicketLocationDocument(): Map<String, Any?>
fun Category.toCategoryDocument(): Map<String, Any?>      // seed only

fun DocumentSnapshot.toTicket(): Ticket
fun DocumentSnapshot.toPublicTicket(): Ticket             // scope PUBLIC, attachments from thumbnails
fun DocumentSnapshot.toCategory(): Category
fun DocumentSnapshot.toDashboardStats(): DashboardStats   // missing doc -> DashboardStats()

/** stats(after) - stats(before) as FieldValue.increment per counter; before = null for a new ticket. */
fun statsIncrements(before: Ticket?, after: Ticket): Map<String, Any>
```
`publicTickets` keys: `categoryId`, `categoryName`, `situation`, `currentStatus`, `coarseLocation` (GeoPoint), `thumbnails` (List<String>), `createdAt`. `stats/summary` keys: `totalTickets`, `activeTickets`, `resolvedTickets`, `repairTimeSumMillis`.

### `CityFixApplication`
```kotlin
val ticketsRepository: TicketsRepository by lazy { FirestoreTicketsRepository(firestore) }
/** F-011 stand-in until D-009: null = anonymous; Citizen(DEV_CITIZEN_ID, ACTIVE) or Operator(DEV_OPERATOR_ID) to test. */
val currentUser: User? = null
/** Reporter id until D-009: the stand-in's id, or DEV_CITIZEN_ID for anonymous testing. */
val reporterId: String get() = currentUser?.id ?: DEV_CITIZEN_ID
companion object { const val DEV_CITIZEN_ID = "dev-citizen-1"; const val DEV_OPERATOR_ID = "dev-operator-1" }
```

### `TicketsViewModel`
```kotlin
class TicketsViewModel(private val repository: TicketsRepository, user: User?) : ViewModel() {
    val tickets: StateFlow<List<Ticket>>
    val stats: StateFlow<DashboardStats>
    /** Restarts the ticket listeners for [user]: null = anonymous. D-009 calls this on sign-in/out. */
    fun setUser(user: User?)
    fun getTicketById(id: String): Ticket?   // unchanged
}
```

### `ReportViewModel`
`ReportViewModel(repository, attachmentStorage, creatorId: String)`; factory gains `creatorId`.

## Steps

### Slice 1: mapping
- Create `data/FirestoreMapping.kt` with the functions above.
  - Enums written with `.name`; read with a small private helper `inline fun <reified E : Enum<E>> enumOrDefault(name: String?, default: E)` (`enumValues<E>().firstOrNull { it.name == name } ?: default`), so a bad value never throws.
  - `toTicketDocument`: `participantIds = listOf(creatorId) + followerIds` (distinct); history maps with `justification` only when non-null; attachment maps `{id, type, phase, storagePath = file, thumbnailPath = thumbnail (omitted when null), uploadState, uploadedById}`; `location = GeoPoint(lat, lon)`. No `resolvedAt`, no `messages` (subcollection, F-014), no `id` field (document id).
  - `toPublicTicketDocument`: `val view = toAnonymousView()`; read every value from `view`; `thumbnails = view.attachments.filter { it.phase == REPORT }.mapNotNull { it.thumbnail }`. KDoc: the only builder of a public document (decision 0006 contract).
  - `toTicket`: `id = doc.id`; numbers via `getLong(...) ?: 0L`; `location` from `getGeoPoint`; lists via `get(...) as? List<*>` then `filterIsInstance<Map<*, *>>()`; missing `phase` -> REPORT; `file = storagePath`, `thumbnail = thumbnailPath`. Firestore-only fields (`participantIds`, `resolvedAt`) are not mapped into the model (decision 0010).
  - `toPublicTicket`: `scope = PUBLIC`, `location` from `coarseLocation`, `attachments = thumbnails.mapIndexed { i, path -> Attachment(id = "thumb-$i", file = path, thumbnail = path, uploadState = UPLOADED) }`. Everything else at defaults (blank author, empty history), the same shape `toAnonymousView()` produces.
  - `toDashboardStats`: `averageRepairTimeMillis = if (resolved > 0) sum / resolved else null`.
  - `statsIncrements`: `val a = TicketsList(listOf(after)).stats()`, `val b = before?.let { TicketsList(listOf(it)).stats() } ?: DashboardStats()`; repair sum of a one-ticket stats = `averageRepairTimeMillis ?: 0`; returns `FieldValue.increment(delta)` for each of the 4 keys (zeros included; harmless).
- Commit: `feat(F-011): add Firestore mapping for tickets, categories and stats`
- Verify: `./gradlew assembleDebug`, `./gradlew lint`; grep that `toAnonymousView()` is called in `toPublicTicketDocument` and nowhere else in `data/` (besides the deleted file later).

### Slice 2: Firestore repository (not wired)
- Create `data/FirestoreTicketsRepository(private val firestore: FirebaseFirestore)` (no interface yet; same member names and signatures as the future interface so slice 3 only adds `: TicketsRepository` and `override`).
  - Private helper `fun <T> Query.listen(map: (QuerySnapshot) -> T): Flow<T> = callbackFlow { val registration = addSnapshotListener { snapshot, error -> if (error != null) { Log.w(TAG, "listen failed", error); return@addSnapshotListener }; if (snapshot != null) trySend(map(snapshot)) }; awaitClose { registration.remove() } }` and the same for a `DocumentReference` (stats). KDoc explains `callbackFlow`/`awaitClose` for the defence.
  - `publicTickets()`: `collection(PUBLIC_TICKETS).whereIn("currentStatus", ACTIVE_STATUS_NAMES)`, map `toPublicTicket()`, sort with `TicketsList(it).getAllSorted()`.
  - `participantTickets(userId)`: `collection(TICKETS).whereArrayContains("participantIds", userId)`, `toTicket()`, sorted.
  - `allTickets()`: `collection(TICKETS)`, `toTicket()`, sorted.
  - `stats()`: `document(STATS_SUMMARY)`, `toDashboardStats()` (missing -> zeros).
  - `activeCategories()`: `collection(CATEGORIES).whereEqualTo("active", true)`, `toCategory()`, sorted by name with `Collator.getInstance()`.
  - `reportTicket(...)`: `withContext(Dispatchers.IO)`; validation and EXIF strip copied from `LocalTicketsRepository`; `val ref = firestore.collection(TICKETS).document()`; build the `Ticket` (`id = ref.id`, OPEN, `createdAt = now`, one `StatusHistory(OPEN, now, null, creatorId)`, attachments PENDING + `uploadedById = creatorId`); `firestore.batch().apply { set(ref, ticket.toTicketDocument()); set(ticketLocations/{id}, ...); if (scope == PUBLIC) set(publicTickets/{id}, ...); set(document(STATS_SUMMARY), statsIncrements(null, ticket), SetOptions.merge()) }.commit().addOnFailureListener { Log.w(TAG, "report not saved", it) }`; return `ref.id`. Comment: why no `await()` (offline commit completes only on sync).
  - `ACTIVE_STATUS_NAMES` derived from the model, not retyped: `TicketStatus.entries.filter { Ticket(currentStatus = it).isActive }.map { it.name }`, so "active" keeps one definition (decision 0005).
- Commit: `feat(F-011): add Firestore tickets repository`
- Verify: build + lint (the class is unused; no lint error expected for unused Kotlin classes).

### Slice 3: switch the app to Firestore
- `TicketsRepository.kt`: new interface (above) and KDoc (decisions 0009, 0010).
- `FirestoreTicketsRepository`: `: TicketsRepository`, `override` on each member.
- Delete `LocalTicketsRepository.kt`.
- `CityFixApplication`: repository line, `currentUser`, `reporterId`, dev ids (above).
- `TicketsViewModel`: constructor `(repository, user: User?)`; `private var ticketsJob: Job? = null`; `init { viewModelScope.launch { repository.stats().collect { _stats.value = it } }; setUser(user) }`; `setUser` cancels `ticketsJob`, clears nothing (keeps the last list until the first snapshot) and launches the `when (user)` described in `00-plan.md` (`null` / `is Citizen` / `is Operator`, exhaustive, no `else`). Citizen merge: `combine(repository.participantTickets(user.id), repository.publicTickets()) { own, public -> val ownIds = own.map { it.id }.toSet(); TicketsList(own + public.filter { it.id !in ownIds }).getAllSorted() }`. Remove `refresh()`. Update the class KDoc (no longer "phase 1, everything anonymous").
- `TicketsViewModelFactory(repository, user)`.
- `ReportViewModel`: constructor adds `creatorId: String`; `_uiState` starts with `ReportUiState()`; `init { viewModelScope.launch { repository.activeCategories().collect { list -> _uiState.update { it.copy(categories = list) } } } }`; `submit` passes `creatorId`. KDoc updated.
- `ReportViewModelFactory(repository, attachmentStorage, creatorId)`.
- `MainActivity`: `TicketsViewModelFactory(app.ticketsRepository, app.currentUser)`, `ReportViewModelFactory(app.ticketsRepository, app.attachmentStorage, app.reporterId)`; remove `ticketsViewModel.refresh()` from `onSubmitted` (comment: the listener shows the new ticket from the local cache). Detail audience and `canExpandCards` unchanged (comment mentions D-009).
- `TicketPrivacy.kt`: KDoc lines that name `LocalTicketsRepository.reportTicket` now name `FirestoreTicketsRepository.reportTicket`, and "content of the Firestore public document" names `toPublicTicketDocument`. No code change.
- Commit: `feat(F-011): read and write tickets through Firestore`
- Verify: build + lint; grep shows no `LocalTicketsRepository`, `loadPublicTickets`, `loadStats`, `refresh()` left; device checks D1-D9 below.

### Slice 4: debug seed
- `data/DebugSeed.kt`: `object DebugSeed { fun seedIfEmpty(firestore: FirebaseFirestore) }` - `document(STATS_SUMMARY).get(Source.SERVER).addOnSuccessListener { if (!it.exists()) writeSeed(firestore) }.addOnFailureListener { Log.i(...) }`; `writeSeed`: one batch with `SampleCategories.all` (`categories/{id}`, `toCategoryDocument()`), `SampleTickets.all` (tickets + ticketLocations + publicTickets for PUBLIC + `statsIncrements(null, ticket)` merged into `stats/summary`), and for RESOLVED samples `resolvedAt = Timestamp(Date(resolvedAtMillis))` added to the ticket and public maps (seed only; real ones are D-013's server timestamps). Header comment: "DEBUG ONLY. Delete this file and its call before delivery (D-015)."
- `CityFixApplication.onCreate()`: `super.onCreate(); if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) DebugSeed.seedIfEmpty(firestore)`.
- Commit: `chore(F-011): seed sample categories and tickets in debug builds`
- Verify: build + lint; device check D1.

### Slice 5: rules draft (only if Q7 = yes)
- `firestore.rules`: replace the reserved comment with `match /stats/{docId} { allow read: if true; allow write: if docId == 'summary' && (isActiveCitizen() || isOperator()) && request.resource.data.keys().hasOnly(['totalTickets','activeTickets','resolvedTickets','repairTimeSumMillis']); }` plus a comment that increments cannot be bounded in rules and that the file stays a draft. Do **not** deploy.
- Commit: `docs(F-011): draft rules for stats/summary`
- Verify: build + lint (unaffected); re-read the file.

## Device check list
Two emulators (or emulator + phone), both with `app/google-services.json`, same debug build unless stated. Console open on Firestore Data. Record results in `state.md`.

| # | Setup | Steps | Expected |
| --- | --- | --- | --- |
| D1 | Empty database (or no `stats/summary`), device A online | Install, open | Console: 4 categories (graffiti `active: false`), 3 tickets, 3 `ticketLocations`, 3 `publicTickets`, `stats/summary` = 3 / 2 / 1 / 259200000. Second launch on B does not seed again |
| D2 | A and B anonymous (`currentUser = null`) | Open dashboard, list, detail | Total 3, 67 % active, 3 days; list `sample-1`, `sample-2`; detail PUBLIC as before |
| D3 | A, B online | A reports a PUBLIC ticket with a photo | A: form closes, snackbar, ticket in list at once. B: appears in list and total +1 without touching anything. Console: `tickets/{id}` (participantIds = [`dev-citizen-1`], one OPEN entry, attachment PENDING with a local storagePath), `ticketLocations/{id}` active true, `publicTickets/{id}` with exactly the 7 keys, `stats/summary` total and active +1 |
| D4 | A online | A reports a PRIVATE ticket | Not in either anonymous list; total +1 on both; console has `tickets` + `ticketLocations`, no `publicTickets` doc |
| D5 | A airplane mode | Kill A, cold start | Dashboard and list from cache; report form shows categories |
| D6 | A still offline | Report a PUBLIC ticket | Form closes at once; ticket in A's list; not in console. Turn network on: appears in console and on B within seconds; no duplicate |
| D7 | A anonymous, temporary `FirebaseFirestore.setLoggingEnabled(true)` (not committed) | Open dashboard, list, detail; filter Logcat `Firestore` | Listen targets only `publicTickets`, `stats/summary`, `categories` (when the form is opened); no `tickets` or `ticketLocations` path |
| D8 | Rebuild A with `currentUser = Citizen(DEV_CITIZEN_ID, registrationStatus = ACTIVE)` | Open list | D4's PRIVATE ticket listed with the public ones; no duplicates for D3's ticket |
| D9 | Rebuild A with `currentUser = Operator(DEV_OPERATOR_ID)` | Open list | Every ticket, `sample-3` (RESOLVED) and PRIVATE included |
| D10 | A anonymous | Rotate on list and on a half-filled form | List kept, form kept, one snapshot log per change (no duplicate listeners) |
| D11 | Both | `./gradlew assembleDebug`, `./gradlew lint` | Pass |

Restore `currentUser = null` before the slice-3 commit (or before review if the checks run later).

## Conventions
- `kotlin-conventions` and `.claude/rules/kotlin.md`: MVVM, ViewModels own `MutableStateFlow`s, repository behind an interface, anonymous privacy enforced in the repository layer (anonymous code paths never query `tickets`/`ticketLocations`), structured concurrency (`viewModelScope`, no `GlobalScope`), no main-thread blocking (EXIF on `Dispatchers.IO`; Firestore calls are async).
- Always `app.firestore` (F-010 handover), never `Firebase.firestore`/`getInstance()` elsewhere.
- Field names as decision 0010; enums by `name`; times `Long`; `GeoPoint` only in `data/`.
- Batches, never transactions, for writes that must work offline. No `await()` on writes.
- KDoc on each public function with the brief/decision reference; comments explain the beyond-class pieces (`callbackFlow`, `combine`, `FieldValue.increment`).
- No user-visible strings added (the seed has no UI). If Q1 picks the "View as" menu, strings go to `values/` and `values-pt/`.
- Commits: Conventional Commits with `F-011`, no attribution.

## Do not touch
- `app/google-services.json`, keys, Firebase console settings; no `firebase deploy` (rules stay undeployed, decision 0012 Q5).
- `model/TicketLifecycle.kt`, `model/TicketsList.kt`, `model/DashboardStats.kt`, `model/Attachment.kt`, `model/Ticket.kt` (Diogo's contracts; reused, not changed). `TicketPrivacy.kt`: KDoc only.
- Screens and components (`ui/`): no change; `DetailAudience` and `canExpandCards` stay as they are (D-009).
- `storage.rules`, `firebase.json`.
- `main` branch.

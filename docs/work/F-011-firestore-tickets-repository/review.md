# F-011 review

Range: `62626ab..HEAD` (c629a64) on `dev/Francisco`. Reviewer: reviewer agent, 2026-11-13.
Read: `00-plan.md`, `01-coding-plan.md`, `03-docs-plan.md`, `state.md` (full Log, including the person's typed-getter switch and the removal of `readOrNull`), the task file, the brief (I.1.A-C, IV), decisions 0005, 0006, 0009, 0010, 0011, 0012, `docs/architecture/overview.md`, `.claude/rules/kotlin.md`, `kotlin-conventions`, the class material (09_Storage, 08_Coroutines_Flows_Services, P07/P08) and the T8 `AMOVStorageDemo` `MainActivity.kt`.

## Verdict
**Approve with should-fix.** No blocking findings. 0 blocking, 2 should-fix, 6 nits. Device checks D1-D10 are still open (no device); they are not blocking for the review but must run before `/push-task` is treated as done.

Build and lint (run by the reviewer): `./gradlew assembleDebug lint` passes; lint 0 errors, 18 warnings, all pre-existing (dependency versions, unused colours, one redundant label), none in the F-011 files.

## What was checked and holds
- **Anonymous reads.** With `currentUser == null`, `TicketsViewModel.setUser(null)` collects only `publicTickets()`; `stats()` reads `stats/summary` (counts only); `ReportViewModel` reads `categories`. No anonymous path reads `tickets/` or `ticketLocations/`. `toPublicTicketDocument()` calls `toAnonymousView()` first and copies only the 7 keys of decision 0010; `toPublicTicket()` rebuilds a ticket with blank author, no description, no history and the coarse location. CLAUDE.md rule respected in the repository layer.
- **Report batch vs decision 0010.** One `WriteBatch`: `tickets/{id}`, `ticketLocations/{id}` (`active: true`), `publicTickets/{id}` only for PUBLIC, and `stats/summary` with `SetOptions.merge()`. No `resolvedAt` on create, `participantIds` = creator + followers, attachments PENDING with `phase`. `commit()` is not awaited; failures logged. Offline behaviour matches brief IV.
- **Counters vs decision 0005.** `statsIncrements` is `stats(after) - stats(before)` of the existing pure rule, so "active", "resolved" and repair time keep 0005's meaning (private tickets counted, Q9; reopen subtracts the earlier repair; RESOLVED -> CLOSED changes nothing). Create = +1 total, +1 active.
- **D-008 compatibility.** `AttachmentPhase` written and read (missing -> REPORT, decision 0011); `statusHistory` keeps `responsibleId` as a plain string, so `SYSTEM_RESPONSIBLE_ID` needs no special case; `resolvedAt` is written only by the seed (D-013 owns the real one).
- **MVVM / StateFlow.** ViewModels still own private `MutableStateFlow`s filled in `viewModelScope` (Contacts_#3 pattern, decision 0009); composables untouched; `when (user)` is exhaustive over the sealed `User`.
- **Listener cleanup.** `callbackFlow { ... awaitClose { registration.remove() } }`; collectors live in `viewModelScope`, `setUser` cancels the previous job, `ReportViewModel` is scoped to its route, so rotation does not duplicate listeners and leaving the form removes the categories listener.
- **Class material.** `hashMapOf` documents, typed getters with `?: default` (as `doc.getString("name") ?: ""` in the demo), `db.batch()` + `commit()` as in the demo's "clear all", `app.firestore` as the single instance. Beyond class (`addSnapshotListener`, `callbackFlow`, `combine`, `FieldValue.increment`) is flagged in KDoc and was accepted at Gate 1 (Q2, Q3).
- **DebugSeed.** One file plus one `if` in `CityFixApplication.onCreate()` guarded by `FLAG_DEBUGGABLE`; marked "DEBUG ONLY ... before delivery (D-015)". Easy to remove (file, call, import).
- **Diogo's files.** `TicketsViewModel(+Factory)` and the `TicketPrivacy.kt` KDoc changed only as planned, after the person told Diogo (Log). `model/` rules untouched.
- **Strings, accessibility, orientation, permissions.** No UI change and no user-visible text added (log messages only), so nothing for `values/` / `values-pt/`; no permission or layout impact.
- **Rules.** `stats/{docId}` block drafted as the coding plan said, not deployed (decision 0012 Q5).

## Blocking
None.

## Should-fix

### S1. `setUser` keeps the previous audience's tickets on screen until the new listener answers
`TicketsViewModel.setUser` (app/src/main/java/pt/isec/amov/cityfix/viewmodel/TicketsViewModel.kt) cancels the old job but keeps `_tickets` ("The last list stays until the first snapshot of the new listener arrives"). This method is the seam D-009 will call on sign-out. After an operator or citizen signs out, `tickets` still holds full tickets (description, `creatorId`, `followerIds`, exact location) while the app is anonymous, until `publicTickets` emits; with an empty cache offline that can be indefinitely. CLAUDE.md: anonymous users must never see author data or exact coordinates. The current UI renders PUBLIC audience everywhere, so nothing leaks today, but D-009 will switch the detail audience by role and must not inherit this. It is also easier to explain ("new user, new list").
**Fix:** clear the list before starting the new listener, and update the KDoc:
```kotlin
fun setUser(user: User?) {
    ticketsJob?.cancel()
    _tickets.value = emptyList()   // never show one audience's tickets to another
    ticketsJob = viewModelScope.launch { ... }
}
```
KDoc: "Restarts the ticket listener for [user] (null = anonymous) and clears the current list first, so tickets of the previous user are never shown to the next one." The initial call from `init` is unaffected (the list is already empty).

### S2. The `listen` KDoc describes listener errors inaccurately
`FirestoreTicketsRepository.listen` (both overloads) says "A listener error is logged and the flow keeps its last value instead of failing". A `Flow` holds no value; and after Firestore reports an error to a snapshot listener, that listener is terminated and gets no further events. So the real behaviour is: the flow stays open but silent, the ViewModel's `StateFlow` keeps the last list, and live updates stop until the collector is restarted (new ViewModel or `setUser`). The 00-plan "Failure modes" and the report notes repeat the same idea; at the defence this is the likely question ("what happens when the read fails?").
**Fix (comment only):** "On a listener error Firestore stops the listener; we log it and do not close the flow, so the collector never throws. The ViewModel's StateFlow keeps the last list, but no more updates arrive until the flow is collected again." Use the same wording in the overview / decision 0013 when the docs phase writes them.

## Nit

### N1. Citizen merge filters duplicates twice
In `setUser`, `is Citizen ->` builds `own + public.filter { it.id !in ownIds }` and then `TicketsList(...)`. `TicketsList.add` already rejects a duplicate id and keeps the first one, and `own` comes first, so `TicketsList(own + public).getAllSorted()` gives the same result. **Fix (optional):** drop `ownIds` and the filter and comment "TicketsList keeps the first copy of an id, so the citizen's full ticket wins over its public copy"; or keep the explicit filter and leave it, it is just redundant.

### N2. `ACTIVE_STATUS_NAMES` builds dummy tickets
`TicketStatus.entries.filter { Ticket(currentStatus = it).isActive }` is clever for a one-line list. **Fix (optional):** `listOf(TicketStatus.OPEN, TicketStatus.INFO_REQUESTED, TicketStatus.IN_PROGRESS).map { it.name }` with "same statuses as Ticket.isActive (decision 0005)". If kept, the KDoc already explains why; fine either way.

### N3. Short names in `statsIncrements`
`val a` / `val b` make the subtraction harder to read aloud. **Fix:** `afterStats` / `beforeStats`.

### N4. Two comments in Diogo's files are now only true for anonymous users
`ui/screens/ShowScreen.kt:381` ("Already in anonymous form ..., like TicketsViewModel.tickets") and `model/TicketsList.kt:19` ("In-memory tickets for phase 1"). Since F-011, `TicketsViewModel.tickets` holds full tickets for citizens and operators, and `TicketsList` is a pure helper over Firestore results. **Fix:** tell Diogo, or with his OK change to "like TicketsViewModel.tickets for anonymous users" and "Tickets list with the pure rules (like ContactsList in P07)". Comment only.

### N5. The counter "repair" in 00-plan Failure modes would give wrong totals
00-plan says drift can be repaired by deleting `stats/summary` and re-running the seed. `seedIfEmpty` would then rewrite the 3 samples and set the counters to the samples only, dropping every real ticket. **Fix:** in the docs phase (overview, decision 0013), describe the repair as "recompute the four counters by hand from `tickets/` in the console", not the seed.

### N6. `resolvedAt` reading is left to D-013/F-014 but 0010 says clients use it
Decision 0010 says clients compute the reopen window, close-on-read and 24 h warning from the Firestore `resolvedAt`; `toTicket()` drops it and the model's `Ticket.resolvedAt()` uses the RESOLVED history entry. Correct for F-011 (nothing reads the window yet), but the gap should not be forgotten. **Fix:** add one line to the F-011 handover in the overview for D-013/F-014: "`toTicket()` ignores `resolvedAt`; decide whether to map it (Firestore-only field) or keep using the history entry, and update 0010 accordingly."

## Open device checks (not blocking)
D1-D10 from `01-coding-plan.md` were not run (no device attached): D1 seed, D2 anonymous dashboard/list/detail, D3 public report across devices, D4 private report, D5 offline cold start, D6 offline report + sync, D7 anonymous listen targets (temporary logging), D8 citizen view, D9 operator view, D10 rotation without duplicate listeners. Run D1 first on an empty database; restore `currentUser = null` after D8/D9. D11 (build + lint) passes.

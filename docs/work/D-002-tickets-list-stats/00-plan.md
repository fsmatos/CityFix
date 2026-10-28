# D-002: TicketsList and dashboard stats
Type: feature · Task file: docs/tasks/phase-1/D-002-tickets-list.md
Labels: none

## Problem
D-001 gave us the domain types but no rules over them. Phase 1 has no Firebase, so D-003 (`TicketsViewModel`) needs an in-memory collection of tickets, like `ContactsList` in P07, that it can hold, sort and query. F-004 (`DashboardScreen`, Francisco) needs a `DashboardStats` value to draw. The brief also asks for three rules that do not need Firebase:
- Anonymous dashboard (brief I.1.A): average repair time, total number of issues, % active issues.
- Anonymous list (brief I.1.B): public, active issues only.
- Automatic grouping (brief I.1, "Obs"; decision 0002, Option A): a report with the same category and situation within 10 m of an existing active issue joins that ticket instead of creating a new one.

## Outcome
- `utils/Distance.kt`: a pure Haversine helper, `haversineMeters(lat1, lon1, lat2, lon2): Double`. It is reused later by the 20 m geofence and the 10 km anonymous radius.
- `model/DashboardStats.kt`: an immutable value with the three dashboard numbers. It cannot produce NaN or divide by zero.
- `model/TicketsList.kt`: an in-memory collection with `rawList`, `add`, `findById`, `getAllSorted`, `publicActive()`, `findNearby(location, categoryId, situation)` and `stats()`, plus one shared `Ticket.isActive` predicate.
- `model/` still has no Android or Firebase imports. Its only new import is `pt.isec.amov.cityfix.utils.haversineMeters`, which is pure Kotlin.
- JUnit 4 tests under `app/src/test/` cover the three rules (Q1, decided at Gate 1).

## Acceptance checks
- [ ] `./gradlew assembleDebug` and `./gradlew lint` pass after every slice.
- [ ] `findNearby` returns a ticket about 9 m away, and returns null for one about 11 m away. A different `categoryId` or a different `situation` at 9 m returns null. So do a RESOLVED ticket at 9 m, a CLOSED ticket at 9 m and a ticket at the default (0, 0) location. A report next to a resolved ticket therefore creates a new ticket. (Checked by unit test.)
- [ ] `publicActive()` excludes PRIVATE tickets, RESOLVED tickets and CLOSED tickets. It keeps PUBLIC tickets in OPEN, INFO_REQUESTED and IN_PROGRESS.
- [ ] `stats()` does not count RESOLVED or CLOSED tickets as active, but does count them in `totalTickets`.
- [ ] `stats()` on an empty list returns `totalTickets = 0`, `activeTickets = 0`, `activeRatio = 0.0` and `averageRepairTimeMillis = null`, with no exception and no NaN.
- [ ] `stats()` on a list with tickets gives the expected values: the average covers only tickets with a RESOLVED history entry, and `activeRatio` = active / total.
- [ ] `TicketsList` never exposes its internal mutable list. Changing a list returned by `rawList` or `getAllSorted()` does not change the collection.
- [ ] `grep -rn "^import" app/src/main/java/pt/isec/amov/cityfix/model/` shows only `pt.isec.amov.cityfix.utils.*`. `utils/Distance.kt` imports only `kotlin.math.*`.
- [ ] `./gradlew testDebugUnitTest` passes.
- [ ] `DashboardStats` fields (decided at Gate 1, Q2) are sent to Francisco before the push after slice 2, so he can code F-004.

## Approach
**`TicketsList` follows P07's `ContactsList` pattern.** It is a plain class holding a private `MutableList<Ticket>`, built with optional initial tickets (D-003 passes its 3 samples). The ViewModel owns one instance and publishes snapshots through `StateFlow`. Composables never see `TicketsList` (D-003 acceptance check).
- Every read returns a new read-only `List` (`toList()`, `filter`, `sortedWith`), so `UiState` never holds a list that changes afterwards.
- Alternative considered: an immutable `TicketsList` whose `add` returns a new instance. It is purer, but it differs from the course example and is harder to explain. See Q4.

**Members:**
- `val rawList: List<Ticket>`: a copy of all tickets in insertion order.
- `fun add(ticket: Ticket): Boolean`: appends the ticket and returns `true`. It returns `false` and changes nothing when `ticket.id` is blank or already present, because lists key on `id` (F-003). It does not do grouping. The caller (D-003, and later the repository) calls `findNearby` first and decides whether to follow or create. See Q5.
- `fun findById(id: String): Ticket?`: `firstOrNull`, so it returns null for an unknown id. D-003's `getTicketById` relies on this.
- `fun getAllSorted(): List<Ticket>`: newest first (`createdAt` descending), with `id` as a tie-break so the order is stable. No `Collator` is needed because the sort is not on text. See Q6.
- `fun publicActive(): List<Ticket>`: `scope == PUBLIC && isActive`, sorted the same way as `getAllSorted()`.
- `fun findNearby(location: Coordinates, categoryId: String, situation: Situation): Ticket?`: the nearest ticket that meets all of these conditions:
  - it is active;
  - it has the same `categoryId` (review note C1: an id, not a `Category`) and the same `situation`;
  - it is at most 10.0 m away by Haversine (inclusive, "raio de 10mt").
  - It returns null when `location` is the default (0, 0) or `categoryId` is blank. Tickets whose own location is (0, 0) are never matched (C1).
  - Scope is not filtered. See Q7.
- `fun stats(): DashboardStats`: see below.
- `companion object { const val GROUPING_RADIUS_METERS = 10.0 }`, so the rule has a name at the defence.

**One definition of "active" (Gate 1, Q3).** A public extension property in `TicketsList.kt`: `val Ticket.isActive`. It is true only when `currentStatus` is OPEN, INFO_REQUESTED or IN_PROGRESS, so RESOLVED and CLOSED tickets are not active.
- Why: "% ocorrências ativas" on the dashboard (brief I.1.A) means problems not yet repaired. The 20 m geofence on an "ocorrência ativa" (brief I.2, "Obs") makes no sense for a ticket that is already resolved.
- This is stricter than the task spec's "public, not closed" and review note C1's "not CLOSED". The deviation was chosen at Gate 1.
- It is written as an allow-list of the three statuses, so a status added later is inactive until someone decides otherwise.
- `publicActive()`, `findNearby()` and `stats()` all use it, so the list, the grouping rule and the "% active" figure always agree. A new report next to a RESOLVED ticket creates a new ticket. F-002 and F-004 can reuse the property.

**Distance in `utils/`.** CLAUDE.md and the overview put distance maths in `utils/`.
- `haversineMeters` takes four `Double`s, not `Coordinates`. That way `utils` does not depend on `model`, and the only dependency is `model -> utils`, with no cycle.
- Earth radius: 6 371 000 m (mean radius). At 10 m the Haversine error is far below GPS error.
- Alternative: the formula inside `model/`. It would work, but the geofence (20 m) and the anonymous radius (10 km) also need it, and CLAUDE.md names `utils/` for it.

**`DashboardStats`** (decided at Gate 1, Q2). The brief requires only three values: average repair time, total and % active. `resolvedTickets` is kept as an extra:
```kotlin
data class DashboardStats(
    val totalTickets: Int = 0,
    val activeTickets: Int = 0,
    val resolvedTickets: Int = 0,               // tickets that entered the average
    val averageRepairTimeMillis: Long? = null,  // null = no ticket resolved yet
) {
    val activeRatio: Double                     // 0.0..1.0
        get() = if (totalTickets == 0) 0.0 else activeTickets.toDouble() / totalTickets
}
```
- `activeRatio` is computed in one guarded place, so it cannot drift from the counts and cannot be NaN. F-004 formats it with `NumberFormat.getPercentInstance()`, which expects 0..1 and is locale-aware (EN "45%", PT "45 %").
- `averageRepairTimeMillis` is in epoch-millisecond units like every other time in the model (decision 0003). `null` means "no data yet", so the UI shows a placeholder instead of "0 days". F-004 formats it as days/hours.
- `activeTickets` and `resolvedTickets` let the UI show "12 of 30" or "based on 8 repairs" for free.
- The no-arg `DashboardStats()` is the empty state for previews.

**`stats()` rules** (review note C2):
- `totalTickets`: the size of the whole list.
- `activeTickets`: the count where `isActive` is true.
- Repair time of one ticket: the **last** RESOLVED `StatusHistory.dateTime` minus `createdAt` (`lastOrNull`, never `first`/`last`). The last entry is used because, after a reopen, the repair really ended at the final resolution. See Q8.
- A ticket is skipped from the average when:
  - it has no RESOLVED entry;
  - `createdAt <= 0` (unset time from a default `Ticket()`);
  - the duration is negative (bad data).
- The average is a `Long` integer division of the sum by `resolvedTickets`, and it is only computed when `resolvedTickets > 0`.
- Scope is not filtered: the stats cover every ticket in the list. See Q9.

**Verification (Gate 1, Q1: commit the tests).** JUnit 4.13.2 is already in the version catalog, `app/build.gradle.kts` already has `testImplementation(libs.junit)`, and `app/src/test/.../ExampleUnitTest.kt` exists. Plain JVM tests therefore need no Gradle change and run with `./gradlew testDebugUnitTest`.

## Architecture impact
- New file in `utils/` (the first one; its `.gitkeep` is deleted) and two new files in `model/`. No Gradle, manifest, resource or UI changes.
- New package dependency `model -> utils`. Both are pure Kotlin. This is worth one line in the overview so nobody adds Android code to `utils/Distance.kt` later.
- `DashboardStats` and the `TicketsList` signatures become a cross-branch contract (F-004 and D-003), like the D-001 types.
- The Firebase phase replaces `TicketsList` as the source of truth with a repository. The rules (`isActive`, the 10 m match, stats maths) should stay as pure functions the repository calls, so they are not rewritten.

## Failure modes
- **Default data.** A `Ticket()` has id `""`, location (0, 0) and `createdAt = 0`:
  - `add` rejects the blank id;
  - `findNearby` ignores (0, 0) on both sides;
  - `stats()` skips `createdAt <= 0`.
- **Empty list.** `stats()` returns `DashboardStats()`, and `findNearby`, `publicActive` and `getAllSorted` return null or an empty list.
- **Floating-point edge at exactly 10 m.** The comparison is `<=`. Tests use about 9 m and about 11 m, so the result does not hinge on rounding.
- **Duplicate follower or self-follow.** `findNearby` may return a ticket the reporter created or already follows. The caller must check `creatorId`/`followerIds` before appending (documented in KDoc; implemented in D-003 or later).
- **`currentStatus` out of sync with history.** "Active" reads `currentStatus` and the repair time reads the history (decision 0004). Sample data in D-003 must keep them consistent (C3).
- **Large lists.** `findNearby` is O(n) with trigonometry per ticket. That is fine for phase 1. In the Firebase phase, pre-filter by category, situation and a lat/lon bounding box in the query.

## Risks and open questions
Gate 1 (Diogo, 2026-10-28) decided every question below. None is still open.
- **Q1. Unit tests: decided, commit them.** Slice 6 adds one plain JUnit 4 file, `app/src/test/java/pt/isec/amov/cityfix/model/TicketsListTest.kt`, run with `./gradlew testDebugUnitTest`. JUnit is already set up, so no dependency is added. CLAUDE.md's "no automated tests" line becomes inaccurate. Updating it is the person's call, not part of this task.
- **Q2. `DashboardStats` fields: decided as proposed.** They are `totalTickets`, `activeTickets`, `resolvedTickets` (an extra; the brief requires only average repair time, total and % active), `averageRepairTimeMillis: Long?` and the computed `activeRatio` (0..1). Francisco still has to be sent this block before the push after slice 2.
- **Q3. Does RESOLVED count as "active"? Decided: no.** Active means OPEN, INFO_REQUESTED or IN_PROGRESS.
  - Why: "% ocorrências ativas" (brief I.1.A) means problems not yet repaired, and the 20 m geofence on an "ocorrência ativa" (brief I.2, "Obs") makes no sense for a resolved ticket.
  - This deviates from the task spec's "not closed" and review note C1. It lives only in `Ticket.isActive`, so `publicActive()`, `findNearby()` and `stats()` all follow.
  - A new report next to a RESOLVED ticket creates a new ticket. Reopening within 5 days stays a separate path (later task).
- **Q4. Decided (recommendation):** a mutable internal list with read-only copies out, as in P07.
- **Q5. Decided (recommendation):** `add` returns `false` and changes nothing for a blank or duplicate id. Assigning ids to tickets created in phase 1 (for example a UUID) is D-003's job.
- **Q6. Decided (recommendation):** `getAllSorted()` puts the newest first by `createdAt`, then sorts by `id`.
- **Q7. Decided (recommendation):** grouping matches any scope. The ticket keeps its own scope, so a private ticket still never reaches anonymous users. Remaining risk: a follower may see a private ticket's description.
- **Q8. Decided as proposed:** repair time runs from `createdAt` to the last RESOLVED entry.
- **Q9. Decided, confirmed by the brief:** the brief says "nº total de ocorrências", not "públicas", so `stats()` counts every ticket, private ones included. Risk for the Firebase phase: anonymous users cannot read private tickets, so the stats then need an aggregate source, such as a stats document or Firestore `count()` with rules that allow it.
- **Q10. Decided (recommendation):** Haversine lives in `utils/Distance.kt`, so `model/` imports `pt.isec.amov.cityfix.utils.haversineMeters`, which is still pure Kotlin.
- **Risk: cross-branch contract.** Renaming any `TicketsList` member or `DashboardStats` field after Francisco merges breaks F-004 and F-003. Mitigation: tell him the fields before pushing.

## Out of scope
- Adding the reporter as a follower or creating a ticket after `findNearby` (D-003 or the Firebase repository).
- Ticket state transitions, the 5-day reopen window, the 24 h alert, the 20 m geofence and the 10 km anonymous radius check. The helper in `utils/` is ready for them, but they are not built here.
- `update` or `remove` on `TicketsList` (add them in D-003 if needed).
- Sample data (D-003), UI formatting of the stats (F-004) and strings.
- Filtering personal fields for anonymous users. That stays a repository-layer concern (decision 0003). `publicActive()` only filters which tickets are shown, not which fields.
- Changes to D-001 files, Gradle and CLAUDE.md.

## Slices
Ordered small steps; each leaves the build green and becomes one commit.
1. `utils/Distance.kt` with `haversineMeters`, and delete `utils/.gitkeep`.
2. `model/DashboardStats.kt`. Tell Francisco the fields, then push after this slice so F-004 can start.
3. `model/TicketsList.kt`: storage, `rawList`, `add`, `findById`, `getAllSorted`, `isActive` and `publicActive()`.
4. `findNearby` with the 10 m rule.
5. `stats()`.
6. `app/src/test/.../model/TicketsListTest.kt` with the acceptance-check tests.

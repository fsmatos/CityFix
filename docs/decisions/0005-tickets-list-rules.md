# 0005: Tickets list rules (active, 10 m grouping, dashboard stats)

Status: accepted (D-002, Gate 1 on 2026-10-28; S1 review fix on 2026-10-28)

## Context
Phase 1 has no Firebase, but three rules from the brief do not need it:
- the anonymous dashboard (brief I.1.A): average repair time, total number of issues ("nº total de ocorrências") and % active issues ("% ocorrências ativas");
- the anonymous list (brief I.1.B): "Listagem de ocorrências públicas ativas";
- automatic grouping (brief I.1, "Obs"; [decision 0002](0002-grouped-reports-single-ticket.md), Option A): a report with the same category and situation within 10 m of an existing active issue joins that ticket.

All three depend on what "active" means. The task spec (`docs/tasks/phase-1/D-002-tickets-list.md`) said "public, not closed", and the D-001 review note C1 said "not CLOSED". The brief also uses "ocorrência ativa" in I.2, "Obs": the 20 m geofence alerts authors when an operator comes near an active issue.

The rules live in `model/TicketsList.kt` (`TicketsList`, `Ticket.isActive`), `model/DashboardStats.kt` and `utils/Distance.kt` (`haversineMeters`), and are covered by `app/src/test/java/pt/isec/amov/cityfix/model/TicketsListTest.kt`.

## Decision
- **Active = OPEN, INFO_REQUESTED or IN_PROGRESS (Gate 1, Q3).** RESOLVED and CLOSED are not active.
  - Why: "% ocorrências ativas" (I.1.A) means problems not yet repaired; the anonymous list of "ocorrências públicas ativas" (I.1.B) should not show repaired problems; and a 20 m geofence alert on an "ocorrência ativa" (I.2, "Obs") makes no sense for a ticket an operator has already resolved.
  - This deliberately deviates from the task spec ("not closed") and D-001 review note C1 ("not CLOSED").
  - It is one shared predicate, `val Ticket.isActive`, written as an allow-list of the three statuses, so a status added later stays inactive until someone decides otherwise. `publicActive()`, `findNearby()` and `stats()` all use it, so the anonymous list, the grouping and the "% active" figure never disagree. F-002/F-003/F-004 should reuse it instead of writing a second check.
- **Grouping match rule (`findNearby(location, categoryId, situation)`).**
  - Matches on `categoryId` (an id, not a `Category`, review note C1) and `situation`, and only active tickets.
  - Distance by Haversine, at most `GROUPING_RADIUS_METERS = 10.0`, inclusive ("raio de 10mt"). If several tickets qualify, the nearest wins.
  - Scope is not filtered (Q7): a report can join a private ticket. The ticket keeps its own scope, so a private ticket still never reaches anonymous users.
  - The default location (0, 0) is never a match, neither as the query nor as a candidate; a blank `categoryId` returns null.
  - It only finds the ticket. Adding the reporter to `followerIds` (and not adding the creator or a duplicate) is the caller's job (D-003, later the repository).
- **Repair time (Q8).** For one ticket: the **last** RESOLVED `StatusHistory.dateTime` minus `createdAt`, in milliseconds. A ticket is left out of the average when it is active (for example reopened after a resolve; review S1), has no RESOLVED entry, has `createdAt <= 0`, or gives a negative duration. The average is `Long` integer division and is `null` when no ticket qualifies, so the UI shows a placeholder instead of "0 days".
- **Dashboard scope (Q9).** `stats()` covers every ticket, private ones included, because I.1.A says "nº total de ocorrências", not "públicas".
- **`DashboardStats` fields (Q2).** `totalTickets`, `activeTickets`, `resolvedTickets`, `averageRepairTimeMillis: Long?` and the computed `activeRatio` (0.0..1.0, 0.0 when there are no tickets, never NaN). The brief requires only three values; `resolvedTickets` (tickets that entered the average) is an extra so the UI can say "based on N repairs".
- **Haversine in `utils/` (Q10).** `haversineMeters(lat1, lon1, lat2, lon2)` takes four `Double`s, so the only package dependency is `model -> utils`, with no cycle. Mean Earth radius 6 371 000 m; the intermediate `a` is clamped to 0..1 so near-antipodal points cannot produce NaN. `utils/` must stay pure Kotlin because `model/` imports it.
- **In-memory collection (Q4-Q6).** `TicketsList` follows P07's `ContactsList`: a private mutable list, and every read (`rawList`, `getAllSorted()`, `publicActive()`) returns a new list. `add` rejects a blank or duplicate id. Sorted lists are newest first by `createdAt`, then by `id`. Rejected: an immutable collection whose `add` returns a new instance (purer, but further from the course material).
- **Unit tests for pure `model/` rules (Q1).** Plain JUnit 4 tests on the JVM (`./gradlew testDebugUnitTest`), 24 tests in `TicketsListTest.kt`. JUnit was already in the version catalog, so no dependency was added. No instrumented or UI tests. This is a deliberate exception to the project's earlier "no automated tests" stance.

## Consequences
- A new report next to a RESOLVED ticket creates a new ticket instead of following it. Reopening within the 5-day window is a separate path (a later task).
- A RESOLVED ticket in its 5-day reopen window disappears from the anonymous list and stops counting as active.
- A reopened ticket (status OPEN again) counts as active and adds no repair time until it is resolved again; then its last RESOLVED entry is used.
- `publicActive()` filters **which tickets**, not **which fields**: each `Ticket` still carries `creatorId`, `followerIds`, `messages` and the exact `location`. Anything serving anonymous users must project the ticket (drop those fields, coarsen the location), as required by CLAUDE.md and [decision 0003](0003-domain-model-representation.md). In phase 1 that projection is D-003's job.
- In the Firebase phase anonymous users cannot read private tickets, so the dashboard needs an aggregate source the security rules allow (a stats document, or Firestore `count()` aggregation).
- `findNearby` is O(n) with trigonometry per ticket. Fine in memory; the Firebase repository should pre-filter candidates in the query (category, situation, bounding box or geohash) and keep reusing these pure rules.
- `DashboardStats` and the `TicketsList` signatures are a cross-branch contract (F-003, F-004, D-003). Renaming a member breaks the other branch.
- Sample data must keep `currentStatus` equal to the last history entry ([decision 0004](0004-denormalised-current-status.md)), because "active" reads `currentStatus` and the repair time reads the history.

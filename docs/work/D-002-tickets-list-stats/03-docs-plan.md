# D-002: Docs plan

## Docs to update
- `docs/architecture/overview.md`:
  - "Layers (MVVM)": add one sentence that `utils/` is pure Kotlin (no Android) because `model/` imports it (`haversineMeters`).
  - "Code mapping": add a short "Rules (D-002)" paragraph:
    - `TicketsList` is the phase-1 in-memory source, owned by `TicketsViewModel`. The Firebase phase replaces it with a repository that reuses the same rules.
    - `Ticket.isActive` is true for OPEN, INFO_REQUESTED or IN_PROGRESS only. RESOLVED and CLOSED are not active (Gate 1, Q3).
    - The 10 m grouping is `findNearby`: same `categoryId` + `situation`, active, at most 10 m by Haversine, (0, 0) ignored, any scope. A report next to a RESOLVED ticket creates a new ticket.
    - `DashboardStats` lists its fields and units (millis, ratio 0..1, null average when nothing is resolved).
- `docs/tasks/phase-1/README.md`: no edit by the docs-writer. If the agreed `DashboardStats` fields differ from `01-coding-plan.md`, tell the person, so that F-004's task can be checked.
- `CLAUDE.md` / kotlin-conventions "Testing": JUnit tests are committed (Gate 1, Q1), so point out to the person that "No automated tests" is now inaccurate. The person edits it; the docs-writer does not.

## Decisions to record
- `docs/decisions/0005-tickets-list-rules.md` (new; context, decision, consequences). Record the Gate 1 answers (Diogo, 2026-10-28):
  - **What "active" means (Q3).** Active = `currentStatus` is OPEN, INFO_REQUESTED or IN_PROGRESS. RESOLVED and CLOSED are not active.
    - Cite the brief:
      - I.1.A, the anonymous dashboard's "% ocorrências ativas": problems not yet repaired.
      - I.1.B, "Listagem de ocorrências públicas ativas".
      - I.2, "Obs": the 20 m geofence alerts authors when an operator nears an "ocorrência ativa", which makes no sense for a repaired problem.
    - Record that this deviates from the task spec ("public, not closed") and the D-001 review note C1 ("not CLOSED").
    - It is one shared allow-list predicate (`Ticket.isActive`) used by `publicActive()`, `findNearby()` and `stats()`.
    - Consequences: a new report next to a RESOLVED ticket creates a new ticket; RESOLVED tickets in their 5-day reopen window leave the anonymous list; reopening is handled by its own later task.
  - The grouping match rule: an id instead of a `Category`, the 10 m threshold inclusive, nearest wins, scope handling (Q7), and (0, 0) ignored.
  - The repair-time definition: the last RESOLVED entry minus `createdAt`, which tickets are skipped, and a nullable average (Q8).
  - Dashboard scope: all tickets, private ones included, because brief I.1.A says "nº total de ocorrências", not "públicas". The consequence is that the Firebase phase needs an aggregate source for anonymous users (Q9).
  - `DashboardStats` fields (Q2): the brief requires only three values; `resolvedTickets` is an extra.
  - Haversine in `utils/` and the `model -> utils` dependency (Q10).
- Unit tests (Q1): add a short section, or a separate `0006-unit-tests-for-model-rules.md` if the person prefers. It records that plain JUnit 4 tests cover pure `model/` rules only, with no instrumented or UI tests, as a deliberate exception to "no automated tests".

## Report notes
Append a "D-002: Tickets list and dashboard rules" section to `docs/report/notes.md`:
- **In-memory collection as in P07.** `TicketsList` mirrors `ContactsList`. It is a private mutable list, and every public member returns a copy, so `StateFlow`/`UiState` only hold immutable snapshots. Alternative rejected: an immutable list type with `add` returning a new instance (purer, but further from the course material).
- **10 m automatic grouping (Option A).**
  - The Haversine formula with a mean Earth radius of 6 371 km. At 10 m the error is negligible next to GPS accuracy (about 3-10 m), which is itself a limitation worth stating.
  - Match rules: the same category id and situation, an active ticket, and the nearest within 10 m. A default location (0, 0) is never matched.
  - The function only finds the ticket. Following it (adding to `followerIds`) is the caller's job, and it must not add the creator or a duplicate.
- **One definition of "active".** Active means OPEN, INFO_REQUESTED or IN_PROGRESS. It is shared by the anonymous list, the grouping and the "% active" stat, so the three never disagree.
  - Explain why RESOLVED is not active (brief I.1.A, I.2 "Obs" geofence) and why this deviates from the task spec's "not closed".
  - Trade-off: a resolved ticket in its 5-day reopen window disappears from the anonymous list, and a new report next to it creates a new ticket instead of reopening it.
- **Dashboard maths.**
  - The average repair time is the last RESOLVED entry minus `createdAt`, averaged over tickets that were resolved.
  - When nothing has been resolved the value is null rather than 0, so the UI does not claim "0 days".
  - `activeRatio` is guarded against zero tickets (no NaN) and formatted with a locale-aware percent formatter in the UI.
  - Integer `Long` maths avoids `average()` returning NaN.
- **Pure rules, easy to test.** The rules have no Android or Firebase dependency, so they run on the JVM. Plain JUnit tests prove the 9 m / 11 m boundary, the RESOLVED/CLOSED/private filter and the zero-ticket case.
- **Improvement opportunities:**
  - In the Firebase phase, pre-filter grouping candidates by category, situation and a lat/lon bounding box (or geohash) in the Firestore query, instead of scanning every ticket.
  - The grouping check must also run offline against cached tickets and stay idempotent on sync (decision 0002).
  - Anonymous users cannot read private tickets, so dashboard totals need a stats document or Firestore `count()` aggregation that the rules allow.
  - Consider a median repair time, or excluding reopened time, if averages are skewed by outliers.
  - Reuse `haversineMeters` for the 20 m geofence check and the 10 km anonymous radius.

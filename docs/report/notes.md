# Technical report notes

Raw material for the PDF technical report: technical solutions, options chosen and rejected, improvement opportunities. One section per task; each points to the decision records it relies on.

## D-001: Domain model (classes and enums)
Decisions: [0002](../decisions/0002-grouped-reports-single-ticket.md), [0003](../decisions/0003-domain-model-representation.md), [0004](../decisions/0004-denormalised-current-status.md). Mapping table: `docs/architecture/overview.md`, "Code mapping".

### Option A in code
- One `Ticket` with `creatorId` and `followerIds`; no `Group` class anywhere (decision 0002).
- A report with the same category + situation within 10 m of an active ticket adds the reporter to `followerIds` instead of creating a ticket. The check (D-002) compares `categoryId`, `situation` and `location`, ignores CLOSED tickets and must not count the default (0, 0) location as a match. (Superseded at D-002 Gate 1: RESOLVED tickets are ignored too; see the D-002 section and decision 0005.)
- `followerIds` does not contain the creator. Notifications (status change, info request, 20 m geofence, 24 h before closing) go to `creatorId + followerIds`.
- A follower's extra media is attached to the shared ticket with `Attachment.uploadedById`.
- Option B (one ticket per citizen plus `Grupo`) is kept as a diagram for comparison.

### Why `currentStatus` is duplicated
- It is a denormalised read model. The history (`statusHistory`) is the audit trail: the brief asks for full traceability of every transition with timestamps.
- `currentStatus` is what lists, the dashboard (% active, average repair time) and Firestore `whereEqualTo("currentStatus", ...)` queries read. Firestore cannot query the last element of an array or subcollection.
- Cost: one extra field per write. Gain: a list does not read every ticket's history.
- Risk: the two going out of sync. Handled by changing them only through one transition function (pure Kotlin) and one batched Firestore write.
- Transition times (RESOLVED for the 5-day reopen and 24 h alert; RESOLVED minus `createdAt` for average repair time) still come from the history.

### Inheritance without a role field
- `sealed interface User` with data classes `Citizen` and `Operator` keeps the diagram's specialisation of `Utilizador` and gives exhaustive `when (user)` in the UI and navigation.
- Rejected: `open class User` (data classes extend it badly) and a single `User(role = ...)`.
- Firestore does not handle inheritance: a `role` string is stored in the document and used to choose the class when reading.

### Privacy by design
- Users are referenced by id, so a ticket document never contains name, NIF, contacts or home coordinates.
- `Scope` defaults to `PRIVATE`: a ticket that forgets to set its scope is never public.
- Fields an anonymous view must never show: `Ticket.location` (coarse map zoom only), `creatorId`, `followerIds`, `messages`, and `Citizen` personal data including `georeferencing`. The model documents this; the repository layer enforces it.
- The 10 km radius of `Municipality` (Pr. Republica) limits where anonymous users may be, not where tickets may be.

### Category as id + name snapshot
- `Ticket.categoryId` + `Ticket.categoryName`. Lists show the category offline and without a lookup; an operator renaming a category does not rewrite old tickets.
- Rejected: embedding `Category` (stale copy of `active`), and the id alone (lookup everywhere; phase 1 has no category source).

### Pure model
- No Android or Firebase types in `model/`. The rules in D-002 (10 m grouping, 5-day reopen, dashboard stats) are plain Kotlin functions that are easy to explain and check by hand.
- Enums are stored by constant name; the names are a persisted contract. UI labels come from `strings.xml` (EN + `values-pt`), never from `.name`.

### Time as epoch millis
- `Long` epoch milliseconds, UTC, default `0L`.
- Beat `com.google.firebase.Timestamp` (Firebase in `model/`), `java.time.Instant` (needs API 26 or core-library desugaring with minSdk 24; Firestore does not map it) and `java.util.Date` (mutable, clumsier maths).
- Durations become subtractions (5 days = `5 * 24 * 60 * 60 * 1000L`). The UI formats with a locale-aware formatter (`DateFormat.getDateTimeInstance`).
- The `0L` default keeps previews deterministic (`Ticket() == Ticket()`).

### No-arg construction and immutability
- All parameters have defaults: previews and sample data in one line, and Firestore's no-arg constructor requirement is met.
- Trade-off: a default instance does not meet the diagram's `1..*` multiplicities (no history entry, no attachment), ids are `""` and coordinates are (0, 0). Creation logic enforces the invariants, and sample data sets distinct ids and real coordinates.
- `val` properties keep `UiState` immutable. Firestore's `toObject()` cannot fill `val`s, so the Firebase phase adds a mapping step in `data/`.

### Improvement opportunities
- Mark attachments as report media or resolution proof (RESOLVED requires proof).
- Store an FCM token per user.
- Use a public reduced-field ticket collection for anonymous users, so personal data and exact coordinates never reach their device.
- Use one document per NIF to guarantee NIF uniqueness.
- Move messages and history to subcollections when tickets grow (Firestore's 1 MB document limit); `StatusHistory` then needs an id.
- Add a `participantIds` field (creator + followers) so "my tickets" is one `whereArrayContains` query instead of two.
- Add the diagram associations left out in phase 1: category per municipality (`define`), operator municipality (`pertence a`), validated-by (`valida`), managed-by (`gere`).
- Use `FieldValue.arrayUnion` for `followerIds` so a repeated report never adds a duplicate follower.
- Show a placeholder on other devices until an attachment's `uploadState` is `UPLOADED` (before that, `file` is a local URI only the author's device can open).

## D-002: Tickets list and dashboard rules
Decision: [0005](../decisions/0005-tickets-list-rules.md). Code: `model/TicketsList.kt`, `model/DashboardStats.kt`, `utils/Distance.kt`; tests: `app/src/test/java/pt/isec/amov/cityfix/model/TicketsListTest.kt`.

### In-memory collection as in P07
- `TicketsList` mirrors `ContactsList`: a private mutable list, and every public member (`rawList`, `getAllSorted()`, `publicActive()`) returns a new list, so `StateFlow`/`UiState` only ever hold snapshots that do not change afterwards.
- `add` rejects a blank or duplicate id (lists key on `id`); sorted lists are newest first by `createdAt`, then by `id`, through one shared comparator.
- Rejected: an immutable list type whose `add` returns a new instance. Purer, but further from the course material and harder to explain at the defence.

### 10 m automatic grouping (Option A)
- Haversine formula with a mean Earth radius of 6 371 km, in `utils/Distance.kt` so the 20 m geofence and the 10 km anonymous radius can reuse it. At 10 m the formula's error is negligible next to GPS accuracy (about 3-10 m); that GPS error is the real limitation of the rule and is worth stating in the report.
- The intermediate value is clamped to 0..1, so rounding on near-antipodal points cannot give NaN.
- Match rules: same category id and situation, an active ticket, at most 10 m (inclusive), nearest wins, any scope. A default location (0, 0) is never matched on either side.
- `findNearby` only finds the ticket. Following it (adding to `followerIds`) is the caller's job, and it must not add the creator or a duplicate.
- Alternative: the formula inside `model/`. It works, but CLAUDE.md puts distance maths in `utils/`; taking four `Double`s keeps the dependency one-way (`model -> utils`).

### One definition of "active"
- Active means OPEN, INFO_REQUESTED or IN_PROGRESS: one allow-list predicate, `Ticket.isActive`, shared by the anonymous list, the grouping and the "% active" stat, so the three never disagree.
- Why RESOLVED is not active: "% ocorrências ativas" (brief I.1.A) and "ocorrências públicas ativas" (I.1.B) are about problems not yet repaired, and the 20 m geofence on an "ocorrência ativa" (I.2, "Obs") makes no sense for a ticket already resolved. This deviates on purpose from the task spec's "not closed" (decided at Gate 1).
- Trade-off: a resolved ticket in its 5-day reopen window disappears from the anonymous list, and a new report next to it creates a new ticket instead of reopening it.

### Dashboard maths
- Average repair time = last RESOLVED entry minus `createdAt`, averaged over tickets that are not active and were resolved. A reopened ticket that is open again counts as active and adds no repair time until it is resolved again (review fix S1).
- Tickets with `createdAt <= 0` or a negative duration are skipped as bad data.
- When nothing has been resolved the value is `null` rather than 0, so the UI does not claim "0 days".
- `activeRatio` is computed in one place, guarded against zero tickets (no NaN), and formatted in the UI with a locale-aware `NumberFormat.getPercentInstance()`.
- Integer `Long` maths instead of `average()`, which returns NaN on an empty list.
- Totals include private tickets: the brief says "nº total de ocorrências", not "públicas".

### Privacy
- `publicActive()` filters which tickets anonymous users see, not which fields. Each ticket still carries `creatorId`, `followerIds`, `messages` and the exact location, so the anonymous screens need a projection (drop those fields, coarsen the location). The KDoc says so explicitly.

### Pure rules, easy to test
- No Android or Firebase dependency, so the rules run on the JVM. 24 plain JUnit 4 tests (`./gradlew testDebugUnitTest`) prove the 9 m / 11 m boundary north and east, the longitude term of Haversine (1° at 60°N is about 55 597 m), the RESOLVED/CLOSED/private filters, the reopened-ticket case, the sort order, the defensive copies and the zero-ticket case.
- A detail worth explaining: `sortedWith` returns a fixed-size list, so calling `clear()` on it throws `UnsupportedOperationException`. The copy tests for `getAllSorted()`/`publicActive()` therefore overwrite an element and check that the collection is unchanged.

### Improvement opportunities
- In the Firebase phase, pre-filter grouping candidates in the Firestore query (category, situation and a lat/lon bounding box or geohash) instead of scanning every ticket; then apply the same Haversine check.
- The grouping check must also run offline against cached tickets and stay idempotent on sync (decision 0002); `FieldValue.arrayUnion` avoids duplicate followers.
- Anonymous users cannot read private tickets, so dashboard totals need a stats document (updated by the transition write or a Cloud Function) or a Firestore `count()` aggregation that the security rules allow.
- Serve anonymous users a reduced-field public projection (or collection) instead of full tickets.
- Consider a median repair time, or excluding the time a ticket spent reopened, if outliers skew the average.
- Reuse `haversineMeters` for the 20 m geofence check and the 10 km anonymous radius.

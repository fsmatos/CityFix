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
Decision: [0005](../decisions/0005-tickets-list-rules.md). Code: `model/TicketsList.kt`, `model/DashboardStats.kt`, `utils/Distance.kt`.

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

### Pure rules
- No Android or Firebase dependency, so the rules could run on the JVM. The project has no automated tests (the JUnit tests first written for D-002 were removed); the rules are checked manually.

### Improvement opportunities
- In the Firebase phase, pre-filter grouping candidates in the Firestore query (category, situation and a lat/lon bounding box or geohash) instead of scanning every ticket; then apply the same Haversine check.
- The grouping check must also run offline against cached tickets and stay idempotent on sync (decision 0002); `FieldValue.arrayUnion` avoids duplicate followers.
- Anonymous users cannot read private tickets, so dashboard totals need a stats document (updated by the transition write or a Cloud Function) or a Firestore `count()` aggregation that the security rules allow.
- Serve anonymous users a reduced-field public projection (or collection) instead of full tickets.
- Consider a median repair time, or excluding the time a ticket spent reopened, if outliers skew the average.
- Reuse `haversineMeters` for the 20 m geofence check and the 10 km anonymous radius.

## D-003: TicketsViewModel
Decision: [0006](../decisions/0006-anonymous-projection-phase-1.md). Code: `viewmodel/TicketsViewModel.kt`, `model/TicketPrivacy.kt`, `data/SampleTickets.kt`. Overview: `docs/architecture/overview.md`, "ViewModel (D-003)".

### MVVM as in P07
- The ViewModel owns the in-memory `TicketsList`; composables never touch it.
- State goes out as read-only `StateFlow`s (a private `MutableStateFlow` plus `asStateFlow()`): `tickets` and `stats`. Events come in as plain functions: `getTicketById` and `reportTicket`. After a successful report, one private `refresh()` recomputes both flows.
- State survives rotation because a `ViewModel` outlives the activity's configuration change.
- No coroutines are launched (no `viewModelScope`): all work is synchronous and in memory. The only Android import is `androidx.lifecycle.ViewModel`, and no dependency was added.
- Two flows instead of one `UiState`: the list and the dashboard are separate screens and phase 1 has no loading or error state.

### Privacy by projection
- Phase 1 has no login, so every viewer is anonymous. `publicActive()` chooses the tickets; `Ticket.toAnonymousView()` chooses the fields. It blanks `creatorId`, `followerIds`, `messages`, every `StatusHistory.responsibleId` and `justification`, and every `Attachment.uploadedById`, and rounds the location to 2 decimals (about 1 km, the brief's "coarse map zoom").
- Hidden leaks found on the way: the first OPEN history entry's `responsibleId` is the creator's id (found during planning), and `StatusHistory.justification` is free text that can name the author (found at review). Blanking `creatorId` alone would not have been enough; this is a good example for the report of why a privacy rule needs a field-by-field check against the model.
- It keeps the `Ticket` type, so the screen signatures agreed between the two authors did not change. Rejected: a `PublicTicket` class (type-safe, but duplicated and breaking the agreed signatures) and projecting in the screens (the rule must hold below the UI).
- Rejected for the location: 3 decimals (about 100 m, still a street) and zeroing (clashes with "(0, 0) = unset").
- `getTicketById` searches only the visible list, so knowing an id does not open a private or resolved ticket.
- Limits: media files are not covered (EXIF GPS and Storage paths must be handled by the media-upload task), and coarse locations collapse onto one grid point (a future map must cluster markers).

### Sample data
- 3 tickets near Pr. Republica in `data/SampleTickets.kt` (the layer Firestore replaces later): `sample-1` OPEN, `sample-2` IN_PROGRESS, `sample-3` RESOLVED 3 days after creation.
- Fixed UTC epoch constants, each with its date in a comment, instead of `System.currentTimeMillis()`, so the dashboard is reproducible: total 3, active 2 (about 67%), average repair time 3 days.
- Each sample keeps `currentStatus` equal to its last history entry (decision 0004), and the locations are hundreds of metres apart, so they never group.

### Reporting a ticket
- `reportTicket` trims description, category id and category name and returns `null` when any is blank, so the form must handle a rejected report. Otherwise it creates a ticket with a `UUID` id, OPEN status and one OPEN history entry, and returns the id.
- No 10 m grouping yet: the provisional form has no location, so `findNearby` could never match. A fake location was rejected because fake points would group with each other.

### One shared instance
- The ViewModel is created once above the `NavHost` (D-006) and its state is passed down. If each destination called `viewModel()` itself, each back-stack entry would get its own instance and a reported ticket would not show in the list.
- The detail screen takes its ticket from the collected `tickets` list rather than a one-off `getTicketById` call, so it updates when the list changes.

### Verification
- D-003 has no automated tests (Diogo's decision). It was verified by `./gradlew assembleDebug` and `./gradlew lint` after every slice, by code review (which recomputed every sample epoch constant and the expected dashboard figures), and by running the app once D-006 wires the screens.

### Improvement opportunities
- A `TicketsRepository` interface with a Firestore and an in-memory implementation, so the ViewModel no longer owns the data.
- One `UiState` with loading and error states once data is remote.
- `SavedStateHandle` or Firestore offline persistence so reported tickets survive process death (today the samples come back).
- Wire `findNearby` once reports carry a GPS location: follow the existing ticket, unless the reporter is the creator or already follows it.
- A public reduced-field Firestore collection fed by the same `toAnonymousView()` projection.
- Separate, unprojected views for registered citizens and operators.
- Strip EXIF location at image compression and keep user ids out of Storage paths.
- Cluster or offset coarse markers on the anonymous map.
- Show category names from string resources for known `categoryId`s (`pavement`, `lighting`, `furniture`) and fall back to the stored `categoryName`, so sample categories are translated on a PT device.

## D-004: Navigation routes
No decision record (Gate 1). Code: `ui/navigation/Destinations.kt`, `gradle/libs.versions.toml`, `build.gradle.kts`, `app/build.gradle.kts`. Overview: `docs/architecture/overview.md`, "Navigation (D-004)".

### Type-safe navigation
- The four phase-1 destinations are `@Serializable` route types (Navigation Compose 2.8+, as in the course) instead of string routes such as `"detail/{ticketId}"`: `DashboardRoute`, `TicketListRoute` and `ReportTicketRoute` are `data object`s, and `TicketDetailRoute(ticketId: String)` is a `data class`.
- The compiler checks argument names and types, so a typo or a wrong type is a build error, not a runtime crash.
- The destination rebuilds the arguments with `backStackEntry.toRoute<TicketDetailRoute>()` (D-006).
- Rejected: string routes (not type-safe, not the course stack) and a `sealed interface Route` over all four (Navigation's `hasRoute<T>()` works on concrete classes, and nothing needs one type yet).

### Only the id in the route
- `TicketDetailRoute` carries only the ticket id. The back stack, which is saved across rotation and process death, never holds ticket content, so there is no stale copy and no personal data in saved state.
- The screen reads the current ticket from the shared `TicketsViewModel` (the collected `tickets`), so it always shows the latest version.
- Rejected: passing the whole `Ticket` (needs a custom `NavType` and puts ticket data in saved state).
- The route is for ids from the visible list only: the id returned by `reportTicket` may belong to a PRIVATE ticket that the anonymous list does not contain.

### Build setup
- The Kotlin Serialization compiler plugin generates a serializer for each route; the `kotlinx-serialization-core` runtime provides the `@Serializable` annotation. Without the plugin the code still compiles but navigation fails at runtime ("Serializer for class ... is not found").
- Under AGP 9 built-in Kotlin there is no `kotlin-android` plugin; the serialization plugin is applied like the Compose compiler plugin, with `version.ref = "kotlin"` so it always matches the compiler.
- The runtime is pinned to 1.9.0, the line built for Kotlin 2.2, because the project uses Kotlin 2.2.10; newer releases are built with Kotlin 2.3 and may not be read cleanly. Lint flags both as `NewerVersionAvailable`; the warnings are accepted.
- The runtime is declared explicitly rather than taken transitively from `navigation-compose`, so D-004 builds on its own and the direct use is documented. `navigation-compose` itself is added in D-006, which must check that it does not raise the runtime past 1.9.x (Gradle picks the highest requested version).

### Improvement opportunities
- Deep links to a ticket (for example from an FCM status-change notification) with `TicketDetailRoute`.
- Nested navigation graphs per role (anonymous, registered citizen, operator) in later phases.
- Bump kotlinx-serialization together with Kotlin 2.3.

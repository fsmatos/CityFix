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

## F-002: StatusBadge and TicketCard
No decision record (the choices follow the course style and existing rules). Details: `docs/architecture/overview.md`, "Shared components".

### Technical solutions
- Stateless Compose components (data in, events out) in `ui/components/`, reused by the list (F-003) and the detail screen (F-005); previewable with sample tickets in portrait, landscape and `locale = "pt"`.
- `StatusBadge`: `Surface` + `Text`, one colour per `TicketStatus` from the theme roles (primary, tertiary, secondary, inverse surface) plus `CityFixGreen` for RESOLVED.
- `TicketCard`: M3 `Card(onClick)`, which gives the whole card a large accessible touch target and the ripple for free. Collapsed: thumbnail, category, situation, badge. Expanded: adds description (max 3 lines) and date.
- Placeholder thumbnail: a vector drawable (Material "image" outline) tinted in Compose, so no `material-icons-extended` dependency for one icon.
- Dates: `java.text.DateFormat` (medium, device locale). Works on minSdk 24 without core-library desugaring; `createdAt == 0L` (unset) is skipped instead of showing 1 Jan 1970.

### Options chosen
- Collapsed content taken from brief I.1.B (category, situation, thumbnail) plus the status badge; expanded adds only description and date.
- PT labels verbatim from the brief: Aberto, Pedido de informações, Em resolução, Resolvido, Encerrado; Dano, Avaria, Destruição, Roubo.
- One global expand/collapse toggle (top bar, D-005/D-006) instead of per-card state: the card stays stateless and the screen owns the flag (to survive rotation with `rememberSaveable`).
- Text label on every badge, so status is not conveyed by colour only (accessibility).
- Exhaustive `when` for enum labels (`statusLabelRes`, `situationLabelRes`), so a new constant cannot ship without a translation; never `enum.name`. The helpers return a plain `Int` without `@StringRes`, to match the course material.
- CLOSED badge in `inverseSurface`/`inverseOnSurface` and thumbnail box in `surface`: `surfaceVariant` (first choice) was almost the same as the card container and did not stand out (review S2).
- `CityFixGreen` with white text for RESOLVED: the only raw colour used outside the theme, because the M3 scheme has no green role.
- The card reads only `categoryName`, `situation`, `currentStatus`, `description` and `createdAt`: no location, author, followers or messages.

### Rejected
- Interactive chips (`AssistChip`, `FilterChip`) for the badge: they look clickable and add a touch target that does nothing.
- `material-icons-extended` for one placeholder icon (large dependency).
- Coil before there are real images to load.
- Labels from `enum.name` (not translatable; the names are a persisted contract).

### Improvement opportunities
- Real thumbnails with Coil from `Attachment.thumbnail` once uploaded (placeholder while `uploadState != UPLOADED`).
- Relative dates ("3 days ago").
- Status icons in addition to colour and text.
- Custom container colours for a softer badge palette, and dark-theme badge colours (with a green role in the theme instead of the raw `CityFixGreen`).
- Placeholder `contentDescription` that does not announce a photo when there is none (review N1).

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
- The runtime started at 1.9.0 and moved to 1.11.0 on 2026-10-31, to match the version used in class. It raises `kotlin-stdlib` to 2.3.20 at runtime, while the compiler stays at Kotlin 2.2.10; the build has no compiler warnings, so Kotlin was not changed. Lint flags both as `NewerVersionAvailable`; the warnings are accepted.
- The runtime is declared explicitly rather than taken transitively from `navigation-compose`, so D-004 builds on its own and the direct use is documented. `navigation-compose` itself is added in D-006, which must check that it does not raise the runtime past 1.11.0 (Gradle picks the highest requested version).

### Improvement opportunities
- Deep links to a ticket (for example from a status-change notification) with `TicketDetailRoute`.
- Nested navigation graphs per role (anonymous, registered citizen, operator) in later phases.
- Bump kotlinx-serialization together with Kotlin 2.3.

## F-003: ListScreen
No decision record (Gate 1 kept the defaults). Code: `ui/screens/ListScreen.kt`, `ticket_list_empty` in `values/` and `values-pt/`. Overview: `docs/architecture/overview.md`, "Screens".

### Technical solutions
- Stateless screen `ListScreen(tickets, isExpanded, onTicketClick, modifier)` over a `LazyColumn`, which composes only the visible rows.
- Stable keys: `items(tickets, key = { it.id })`, so Compose keeps each item's identity (and state) when the list changes, instead of matching rows by position. Ids are guaranteed unique and non-blank upstream (`TicketsList.add`).
- Reuses `TicketCard` (F-002) for every row; the screen itself reads only `ticket.id`.
- Empty state as a separate branch (`tickets.isEmpty()`): a centred, localised `Text` in `bodyLarge`/`onSurfaceVariant`, with horizontal padding so long PT text wraps.
- `modifier` as the last parameter, applied to the root of the drawn branch, so the caller applies the `Scaffold` inner padding (edge-to-edge).
- Scroll position survives rotation through the default saveable `rememberLazyListState()`; the list itself lives in the ViewModel.
- Previews with private sample data (6 active PUBLIC tickets, already in anonymous form): collapsed in portrait and landscape, expanded, and empty in portrait, landscape and PT.

### Options chosen
- Filtering and the anonymous projection kept upstream: `TicketsList.publicActive()` holds the single "active" rule in `model/`, and `TicketsViewModel.tickets` applies `toAnonymousView()`, so privacy is enforced before the UI and the screen renders the list as received.
- The click passes only the id (`(String) -> Unit`), matching the type-safe `TicketDetailRoute(ticketId: String)`.
- One layout for both orientations (Gate 1, Q3): the card already fills the width with ellipsis, and the list scrolls in landscape.
- Gate 1, Q1: no stateful wrapper; the agreed signature makes `ListScreen` the stateless half and D-006 collects `vm.tickets` in `composable<TicketListRoute>` (this is the stateful half graded by brief V).
- Gate 1, Q2: `isExpanded` is passed through, so expanded cards show the description. This goes beyond brief I.1.B for anonymous users; the person accepted it, and D-005/D-006 must hide or force off the toggle for anonymous users once roles exist (handover recorded in the overview).

### Rejected
- Filtering by `Scope.PUBLIC`/`isActive` in the screen: duplicates the D-002 rule and could drift from it.
- Passing the ViewModel (or a `UiState`) to the screen: breaks the agreed signature and the D-003 contract (one ViewModel above the `NavHost`), and the screen would no longer be previewable with plain data.
- A `(Ticket) -> Unit` click callback: tempts the caller to put the ticket in the route, which D-004 forbids.
- A stateful `ListScreen(viewModel)` + `ListContent` split in F-003: would change the agreed signature for all screens.

### Improvement opportunities
- Two-column adaptive grid (`LazyVerticalGrid(GridCells.Adaptive(320.dp))`) in landscape and on tablets.
- Pull-to-refresh and filters by category/status once Firestore is in.
- Empty state with an illustration and a "report an issue" action.
- Hide the expand toggle (description) for anonymous users once roles exist. (Done earlier by D-007: hidden for everyone in phase 1, `canExpandCards` is the hook for the role check.)
- Display-cutout insets in landscape (`WindowInsets.displayCutout`/`safeDrawing`).
- PT preview with data and a landscape expanded preview (review N1, N2).

## D-005: MainTopAppBar
No decision record (docs plan). Code: `ui/navigation/MainTopAppBar.kt`, `res/values/strings.xml`, `res/values-pt/strings.xml`. Overview: `docs/architecture/overview.md`, "Top bar (D-005)".

### Stateless top bar
- The bar receives the screen and `canNavigateBack`, and sends clicks out as callbacks. It can be previewed with no `NavController`, and `MainActivity` stays the only place that knows about navigation.
- Rejected: reading `NavBackStackEntry` in the bar, which needs `navigation-compose` now and ties the previews to a `NavController`.

### Enum vs plain flags
- `TopBarScreen` keeps the "which control on which route" rule in one file, where it can be previewed. Plain flags (`showToggle`, `showAdd`, `title`) would move that rule into `MainActivity`.
- Limit of the enum: only the title is exhaustive. An exhaustive `when` makes a new screen a compile error until it has a title, but the controls use `if`, so a new screen silently gets the add button. Do not claim at the defence that the compiler forces a decision about the controls.
- Rejected: a `sealed interface` over the routes. It needs edits to `Destinations.kt`, and D-006 would still have to pass the current route in.

### Back arrow from the back stack, not the route
- This is why `canNavigateBack` is separate from `screen`: the arrow depends on whether there is somewhere to go back to, not on which screen is showing.
- Trap for D-006: `previousBackStackEntry` is not Compose state, so it must be read in the same scope as `currentBackStackEntryAsState()`, or the arrow can go stale.

### Icons
- `material3` 1.4 has no icons, so we use `material-icons-core`, not the much larger extended library. `KeyboardArrowUp`/`KeyboardArrowDown` replace `ExpandLess`/`ExpandMore`, which are only in the extended library.
- The BOM sets 1.7.8, the library's frozen last release.
- Rejected: hand-copied vector drawables (four more XML files and `painterResource` calls, and the course style uses `Icons`).

### Accessibility and localisation
- Content descriptions name the action, and the toggle's description changes with its state. `IconButton` gives 48dp targets (from its documented behaviour, not measured), and `AutoMirrored` flips the back arrow in right-to-left languages.
- The Portuguese strings say "ocorrência(s)", the term the brief uses with users and the one in Francisco's strings. The first version said "tickets" and "problema", and the review changed it (S1), so one list does not show "Tickets" as its title and "ocorrências" in its empty state.
- The title has `maxLines = 1` with an ellipsis, for long Portuguese titles in narrow bars.

### Add button by role (requirement, not an option)
- The brief lets only registered citizens open tickets, so the Auth phase must hide the add button by role. D-005 shows it to everyone because phase 1 has no login.

### Verification
- D-005 has no automated tests. It was verified by `./gradlew assembleDebug` and `./gradlew lint` after every slice (0 errors, the 13-warning baseline) and by code review. The previews have not been rendered yet, and the bar cannot run on a device until D-006.

### Improvement opportunities
- Move to Material Symbols vectors, as the icons library is no longer updated.

## F-004: DashboardScreen
No decision record (Gate 1 kept the defaults except Q4). Code: `ui/screens/DashboardScreen.kt`, the `dashboard_*` strings and plurals in `values/` and `values-pt/`. Overview: `docs/architecture/overview.md`, "Screens". Relies on decision [0005](../decisions/0005-tickets-list-rules.md) for what the figures count.

### Technical solutions
- Stateless screen `DashboardScreen(stats, onSeeTicketsClick, modifier)` that only formats `DashboardStats`; the figures are computed once, upstream, by `TicketsList.stats()` and exposed by `TicketsViewModel.stats`.
- Locale-aware `NumberFormat` (integer for the total, percent for % active), with the locale taken from `LocalConfiguration`, so the `locale = "pt"` preview and the device agree ("67%" in EN, "67 %" in PT). `Locale.getDefault()` would ignore the preview locale (F-002 review S3).
- Average repair time split into whole days and hours (both truncated) and shown with Android plurals ("3 days", "1 day and 4 hours", "5 hours", "Less than 1 hour"); `null` shows "No repairs yet" instead of "0 days". These are the first `<plurals>` in the project. PT needs `one`/`many`/`other`, and the PT `one` category also covers 0, which no call site passes.
- No defensive guards in the UI: the screen relies on the model's documented contract (`activeRatio` never NaN and 0.0 with no tickets, a `null` average when nothing is resolved). Each rule has one owner, and a model regression shows up in the zero-ticket preview instead of being hidden.
- Supporting lines: "Based on N repairs" under the average, and "N of M issues" under % active. The second is pluralised on `totalTickets`, but its numbers are pre-formatted strings (`%1$s`/`%2$s`) from the same integer format as the total card, so "M" always looks like the total above it.
- The percent rounds to whole numbers, so 1 of 300 shows "0%" and 299 of 300 shows "100%"; the "N of M issues" line gives the exact figure, which is a reason it exists (review N5).
- Landscape: one `Row` with `height(IntrinsicSize.Min)` and `weight(1f).fillMaxHeight()` cards, so the three cards have equal widths and heights even when a PT label wraps or only some cards have a supporting line.
- `semantics(mergeDescendants = true)` on each card, so TalkBack reads label, value and supporting line as one item.
- `verticalScroll` on the root, so nothing is clipped at large font scales or in landscape.
- The M3 `Button` is 40dp tall visually and gets a 48dp minimum touch target from `minimumInteractiveComponentSize` (review N4).

### Options chosen
- Q1: no stateful wrapper, as in F-003; D-006 collects `vm.stats`.
- Q2: whole days and hours, truncated, with plurals.
- Q3: "0%" with zero tickets, because the model defines `activeRatio = 0.0`; the "0" total next to it makes the meaning clear.
- Q4 (changed from the default): both supporting lines; "N of M issues" omitted with zero tickets. The % label says "not yet repaired", because RESOLVED is not active.
- Q5: a "See active issues" button calling `onSeeTicketsClick`, because D-005's top bar has no list action. D-006 wires it to `navigate(TicketListRoute)`.
- Q6: a row of three cards in landscape, orientation from `LocalConfiguration` (as in P07).
- Q7: private preview data: `previewStats` (30 / 12 / 8), the zero state, the sample figures (3 / 2 / 1) and a single-ticket state (1 / 0 / 1, 30 min) for the singular plurals and "Less than 1 hour". The single-ticket preview has 0 active of 1, not 1 of 1, because a resolved ticket is never active (D-002 contract); a review suggestion of 1 of 1 was adjusted for that reason.
- Formatting stays in the screen, not in `utils/`: it needs string resources, and `utils/` must stay pure Kotlin because `model/` imports it (decision 0005).
- `NumberFormat`s are created on each composition, without `remember(locale)` (review N2, skipped by the person): the formatters are cheap, the screen recomposes only when `stats` or the configuration changes, and the screen stays free of `remember` apart from the scroll state, which is easier to explain.
- Three explicit `StatCard` calls per orientation instead of a helper taking a `cardModifier`: the helper added a `ModifierParameter` lint warning, and one shared `modifier` would have been applied to three children.

### Rejected
- Decimal days ("3.2 days"): awkward plural rules and less clear than days and hours.
- `FlowRow` for the cards: less predictable and harder to explain than choosing Column or Row.
- Window size classes / `BoxWithConstraints` in phase 1: orientation is enough for a phone.
- Recomputing stats in the screen from tickets: duplicates decision 0005 and the screen would need every ticket, private ones included.
- A duplicate NaN/clamp guard in the UI: hides model bugs, two owners for one rule.
- "0 of 0 issues" with zero tickets: repeats the total and the percent.
- "—" for % active with zero tickets: the model documents 0.0.

### Known difference: figures vs list (review N1)
- The figures count every ticket, private ones included (decision 0005: the brief says "nº total de ocorrências"), while "See active issues" opens the public active list (`publicActive()`). With the PUBLIC-only samples they match; once private tickets exist, "12 of 30 issues" can lead to a list of 9.
- The person kept the button wording (Gate 1, Q5). It is recorded in the overview as a handover and an answer for the defence.

### Verification
- F-004 has no automated tests. It was verified by `./gradlew assembleDebug` and `./gradlew lint` after every slice and after the review fixes (0 errors, the 13-warning baseline, no `MissingTranslation`/`MissingQuantity`), by the plan's greps (no recomputing, no guards) and by code review, which traced the duration maths. The manual Android Studio checks (previews in EN/PT and landscape, TalkBack, the interactive tap) are still pending, so the task's acceptance checks are not ticked yet.

### Improvement opportunities
- Align the figures and the button: rename it "See public issues", or show public-only figures next to the totals, or explain the difference in the UI.
- A median repair time, or excluding outliers.
- Small charts (active vs resolved).
- Per-category figures.
- Window-size-based layouts for tablets (`WindowSizeClass`).
- One decimal in the percent (`maximumFractionDigits = 1`) if whole-number rounding hides small values.
- An aggregate Firestore stats document (or `count()` aggregation allowed by the rules) for anonymous users in the Firebase phase, since they cannot read private tickets (decision 0005).
- Display-cutout insets in landscape, as for the list.

## F-005: ShowScreen
Decision: [0007](../decisions/0007-ticket-detail-by-audience.md) (amends [0006](../decisions/0006-anonymous-projection-phase-1.md)). Code: `ui/screens/ShowScreen.kt`, `ui/components/CoarseLocationMap.kt`, `formatDateTime` in `utils/DateFormatting.kt`, the `ticket_detail_*`, `ticket_media_*`, `ticket_map_*` and `cd_ticket_map` strings in `values/` and `values-pt/`. Overview: `docs/architecture/overview.md`, "Screens" and "Shared components".

### Technical solutions
- One stateless screen `ShowScreen(ticket, audience, modifier)` with `enum class DetailAudience { PUBLIC, REGISTERED, OPERATOR }`. The brief defines three detail views (I.1.C anonymous, I.2.E registered on their own tickets, I.3.E operator), and the enum maps them one to one. An exhaustive `when (audience)` means a later task adding messages or coordinates cannot compile until it decides for every audience.
- The caller decides the audience, never the screen: anonymous or a registered citizen on someone else's ticket -> PUBLIC (I.1.C, I.2.D); the citizen who created or follows the ticket -> REGISTERED (I.2.E); any operator -> OPERATOR (I.3.E). The relation to the ticket matters, not only the role (review S3).
- Privacy in layers: the projection upstream (`toAnonymousView()`, decision 0006) blanks author fields and rounds the location; the screen reads `description` and `statusHistory` only in the non-PUBLIC branches; the location is read once and goes only into the map component, which never prints coordinates. The review checked this with greps on property access and a privacy preview fed with the raw, unprojected `sample-2`.
- The map behind one component, `CoarseLocationMap(location, modifier)`, so a library swap touches one file. In phase 1 it is a static placeholder (see "Known gaps").
- Honest media placeholders: a tile per attachment with a type icon, "Photo"/"Video" and "Not available yet", reading only `attachment.type`. Phase 1 has no real files, so no image loader and no fake photo.
- Locale-aware date and time with `java.text.DateFormat.getDateTimeInstance(MEDIUM, SHORT, locale)` (minSdk 24, no desugaring), the locale from `LocalConfiguration` so the PT previews show PT dates; device time zone; `0L` (unset) is skipped.
- History in stored order with no sort: `currentStatus` duplicates the last entry (decision 0004), so a sort in the UI could hide a data bug instead of showing it.
- Two-pane landscape: the same two private composables (`DetailsPane`, `LocationPane`) are stacked in one scrolling column in portrait and placed side by side, each scrolling on its own, in landscape. No section is written twice.
- Scroll kept across rotation: the two `rememberScrollState()` calls are hoisted to the top of the screen, after the `null` check, so both orientations use the same call sites. When they sat inside each branch, `rememberSaveable` found no saved value after rotation and the page jumped to the top (review S2). A good example for the defence of how saveable state is keyed by call site.
- Accessibility: the category and section titles are headings; each history row and media tile is one TalkBack item; decorative icons have no description next to a text label.

### Options chosen
- Q1: a required three-value enum, no default; in `ui/screens/` (Gate 1), not `model/`.
- Q2 (interpretation of "apenas com imagem(ns) e/ou vídeo(s) e mapa"): for the defence, the literal reading leaves an unlabelled map and photos. Gate 1 chose I.1.B's list fields (category, situation); at review (N2) the person added the status badge so the detail matches the public list card. The anonymous detail shows nothing the list does not already show for that ticket.
- Q3: history as stored, oldest first.
- Q4: medium date + short time, `LocalConfiguration` locale, device time zone; non-PUBLIC only.
- Q5: two panes in landscape (left: header, description, media; right: map, history).
- Q6: type-icon tiles, no Coil yet.
- Q7: a static map placeholder; no library, key or manifest change (the person's choice at Gate 1).
- Q12: "Reported on" only for REGISTERED/OPERATOR (not in I.1.B or I.1.C).
- Media tile box in `surfaceVariant`: `surface` equals the screen background, so the tile was invisible (review S1). The same colour was rejected in F-002 for the opposite reason (it matched the card container).

### Rejected
- Revision 1 of the plan: description and history for anonymous users. Rejected at Gate 1, because the brief is followed strictly (I.1.C "apenas").
- `isPublicView: Boolean` (cannot express three views); reading the role inside the screen (not stateless, not previewable, and it needs the creator/follower relation); `DetailAudience` in `model/`; a default `audience = PUBLIC`.
- Coordinate text, a pin on the rounded point, an interactive map; a sample photo shown as the ticket's photo; projecting in the screen; `LazyColumn` for a handful of history rows inside a scrolling page.
- osmdroid and Google Maps Compose for now (see "Known gaps").

### Known gaps
- **No real map (brief I.1.C "mapa com zoom afastado").** Accepted knowingly at Gate 1: no dependency, permission or key in this task, and the no-key option (osmdroid) is archived. To close it, replace the body of `CoarseLocationMap` with osmdroid (no key; `INTERNET` + `ACCESS_NETWORK_STATE`; `AndroidView` + `onRelease { onDetach() }`; User-Agent and OSM attribution) or Google Maps Compose (needs a Maps API key kept out of git). Either way: fixed zoom about 14, gestures off, a ~600 m circle instead of a pin, a placeholder in previews through `LocalInspectionMode`.
- **Description and history hidden only in the UI (review S4).** `toAnonymousView()` still passes them through; Diogo's projection change should drop them (decision 0007 amends 0006). (Closed by D-007 on 2026-11-10: the projection drops both; see the D-007 section.)
- **List/detail inconsistency.** The expanded anonymous list card shows the description that the detail hides (F-003 Gate 1 Q2). To be decided together with the projection change. (Closed by D-007: the toggle is hidden and the cards forced collapsed for anonymous users.)
- Media tiles never show real images on a device in phase 1: the visible samples have no attachments.
- The non-anonymous view is preview-only until the Auth task.

### Verification
- F-005 has no automated tests. It was verified by `./gradlew assembleDebug` and `./gradlew lint` after every slice and after the review fixes (0 errors, the 13-warning baseline, no `ModifierParameter` or `MissingTranslation`), by the plan's privacy greps (no author fields, `description`/`statusHistory` only in non-PUBLIC branches, `location` read once, no `latitude|longitude`), and by code review. The manual Android Studio checks (previews in EN/PT and landscape, TalkBack) are still pending, so the task's acceptance checks are not ticked yet.

### Improvement opportunities
- A real coarse map (osmdroid or Google Maps Compose) in `CoarseLocationMap`; a maintained library if osmdroid is chosen; offline tiles.
- Real media with Coil and thumbnails once uploaded.
- Exact map, coordinates and author contacts for operators (I.3.E); messages for registered citizens (I.2.E).
- A "no longer public" message when a watched ticket gets RESOLVED, instead of "Ticket not found".
- Clustering coarse markers on a future overview map (projected tickets collapse onto one grid point).
- `formatDate` should take a locale, like `formatDateTime`, with `TicketCard` passing the `LocalConfiguration` locale; today a PT preview of the card and of the detail can format dates in different languages (review N7).
- Merge the map description and caption into one TalkBack item, and make the description honest while there is no map (review N6, skipped).

## F-006: ReportScreen
Decision: [0008](../decisions/0008-report-form-state.md) (related to [0006](../decisions/0006-anonymous-projection-phase-1.md)). Code: `ui/screens/ReportScreen.kt`, `viewmodel/ReportUiState.kt`, `viewmodel/ReportViewModel.kt`, `data/SampleCategories.kt`, the `report_*` and `scope_*` strings in `values/` and `values-pt/`. Overview: `docs/architecture/overview.md`, "Report form (F-006)", "Screens" and "Shared components".

### Technical solutions
- **The P08 pattern.** One immutable `ReportUiState` with a computed `isValid`, exposed by a `ReportViewModel` as a `StateFlow`; events in as plain functions; the ViewModel created per route with `viewModel()`, so the draft survives rotation and is discarded when the user leaves. Plan revision 1 used `rememberSaveable` in the screen; Gate 1 moved to the class pattern (Diogo's doc "Dicas da aula P08 para o CityFix").
- **Errors as `@StringRes` ids in the state.** The ViewModel never holds text, the messages are localised by the screen, and the ViewModel has no Compose or Context import (checked by grep).
- **Live per-field validation.** The description error follows every keystroke; choosing a category, situation or visibility clears its error; Submit sets the error of every field still missing and shows a summary. An untouched form shows no red until the user tries to submit.
- **One gate.** `submit()` builds the state with every field's error and then checks `isValid` (P08 slide 15). The first version repeated the rules in `submit()`; the review (S2) noticed that F-007 would then add the attachment rule to `isValid` only and a report without media would still pass. A good example for the defence of "one owner per rule".
- **Double-submit lock and the `onSubmitResult` round trip.** `submit()` returns the `ReportSubmission` and sets `submitted`; the caller creates the ticket with `TicketsViewModel.reportTicket` and returns whether it worked; `onSubmitResult(false)` unlocks and shows "could not be submitted", `true` keeps the lock. The ViewModel never references the other ViewModel, and no factory is needed.
- **Nothing preselected.** `Situation?` and `Scope?` start `null`, so the user must choose (I.2.E "preenchimento obrigatório") and a default never lands in the 10 m grouping key or publishes a report by accident.
- **Active categories only (I.3.E).** `data/SampleCategories` has three active categories and an inactive "Graffiti"; the ViewModel keeps the active ones, and `submit()` rejects an id not in the list. With no categories the field is disabled and says so, and that message wins over "Choose a category." (review S1).
- **`ExposedDropdownMenuBox`** (P08 slide 32) with a read-only text field, opted in with `@OptIn(ExperimentalMaterial3Api::class)` on the private composable. The material3 1.4.0 sources were checked in the Gradle cache: the non-deprecated anchor is `menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled)`, and `enabled = false` hides the expand action from TalkBack when the list is empty.
- **Accessible radio groups.** `selectableGroup()` plus one `selectable(role = RadioButton)` row of at least 48dp per option, with `RadioButton(onClick = null)`, so the whole row is one TalkBack item with its state; section titles are headings; text fields carry `error` semantics with the real message; form-level messages are polite live regions.
- **Keyboard with edge-to-edge.** `adjustResize` was already in the manifest, but with `enableEdgeToEdge` the window does not shrink, so Compose applies `imePadding()`. Lesson learned: it must go **before** `verticalScroll`. Outside the scroll container it shrinks the viewport, so the focused field is scrolled above the keyboard; inside it (the plan's order) it only adds blank space after the content. The caller must also `consumeWindowInsets(innerPadding)` so the navigation-bar inset is not added twice.
- **Landscape in two panes** (left: category, situation; right: description, media note, visibility, Submit), each scrolling on its own, written once as private composables and used by both orientations; the two scroll states are hoisted (F-005 review S2).

### Options chosen and rejected
- As in decision 0008: route-scoped `ReportViewModel` over `rememberSaveable` in the screen, form state in `TicketsViewModel`, a ViewModel holding another, or a constructor-injected reporter (needs a factory); nothing preselected over the doc's `DAMAGE`/`PRIVATE`; Submit always enabled over `enabled = isValid`.
- Radio groups for situation and visibility over a dropdown (hides options, extra tap) and `SingleChoiceSegmentedButtonRow` (four PT labels such as "Destruição" do not fit a portrait row, and a segmented row with nothing selected looks odd). Dropdown only for the category, whose list is municipality data of any length.
- `collectAsState()` until D-006 adds `lifecycle-runtime-compose`, so the two branches do not both edit the version catalog.
- No `SavedStateHandle`: the draft is lost on process death, as `TicketsViewModel`'s reports are.

### Submit control (Gate 1 Q6 and Q8)
- **Full-width "Submit report" button at the end of the form, not the P08 slide-20 FAB.** The FAB is the class pattern and matches brief V's "botões flutuantes ... dinâmicos", but on this form it floats over the content (the slide needs a bottom spacer), is icon-only (needs a `contentDescription`, no visible label), needs a `Scaffold` nested in D-006's, and in landscape covers the right pane with the description, near the keyboard. The button is in reading and TalkBack order with a visible label. A top-bar check action was also rejected (Diogo's `MainTopAppBar`, and the validation result would have to be hoisted to D-006). The "dynamic" part of brief V is still met by the top bar hiding the add button on this route.
- **Always clickable, P08-style.** Slide 20 keeps the Save FAB clickable and only changes its colour with `isValid`; slide 15 returns early when invalid. F-006 goes one step further: instead of a silent return, Submit shows every missing field's error and an announced summary. The optional `isValid` colour cue was left off at Gate 1; it would be a hint only, never the only signal.

### Known gaps
- **No image or video (brief I.2.E "deve ser carregada uma imagem/vídeo"), open until F-007.** (Closed in code by F-007; see that section.) The form shows a note that attachments come later and will then be required. F-007 (report attachments, following P09; the person writes the task file) adds the system photo picker (`PickVisualMedia.ImageAndVideo`, no permission), the camera (`TakePicture` + `FileProvider`), local copies in `filesDir`, Coil previews, and the "at least one" rule in `isValid`. It needs an attachments parameter on `TicketsViewModel.reportTicket` (Diogo's file). Thumbnails, compression, EXIF stripping and the upload stay in the upload phase.
- No location, so no 10 m grouping (decision 0006); the location task adds it with the runtime permission.
- Anonymous users can open the form until the Auth task hides the add button by role.
- Category names are shown as stored, not translated.
- Process death loses the draft.
- Nothing has run on a device yet: D-006 wires the route.

### Verification
- F-006 has no automated tests. It was verified by `./gradlew assembleDebug` and `./gradlew lint` after every slice and after the review fixes (0 errors, the 13-warning baseline, no deprecation warning from the dropdown API), by greps (no Compose or Context in the ViewModel files, no ViewModel in `ReportContent`, no `data`/navigation import in the screen, `TicketsViewModel.kt` unchanged) and by code review, which traced `submit()` on an empty form. The Android Studio previews, the accessibility inspector, TalkBack and the emulator checks (rotation, keyboard, insets) are still pending.

### Improvement opportunities
- `SavedStateHandle` in `ReportViewModel` so a draft survives process death.
- In the Firebase phase, a categories repository (active categories of the municipality) and ticket creation through a repository, injected with a `ViewModelProvider.Factory` (the doc's "later" list); then a `suspend` submit with a progress state ("Sending...") instead of the synchronous `Boolean`.
- Impact-detection prefill (brief I.2.F): **built by F-015** as the route argument `ReportTicketRoute(fromImpact = true)` (Damage + Pavement), not a `prefill` event; see "F-015: Impact detection".
- A description length limit with a character counter, if the data layer needs one.
- Defence checks from the doc: rotate a half-filled form (values and errors stay), force-close the app (the draft is gone, by design), leave and reopen (empty).
- Nits left open by the person's choice (review), with their fixes:
  - N1: the `error` semantics on the radio-group column are never read (not focusable); remove them or comment that the visible error text is what TalkBack reads;
  - N2: a second failed Submit may not be re-announced by the live region; if so, move focus to the first field with an error (a `FocusRequester` per field), which also helps keyboard users; check that the description error is not read twice;
  - N3: the dropdown closes on rotation (its `rememberSaveable` is keyed under a different parent per orientation); acceptable, or use `remember` and drop the claim;
  - N4: only the left scroll position carries over a rotation from portrait;
  - N5: write the submit wiring (`submit()?.let { onSubmitResult(onSubmit(it)) }`) as three explained statements;
  - N6: PT summary "Faltam preencher campos obrigatórios." could read "Há campos obrigatórios por preencher.";
  - N7: a PT preview for the submit-failed state;
  - N8: `@StringRes` on `scopeLabelRes`, and move it to `ui/components/` if another screen uses it.
- Optional `isValid` colour cue on Submit (P08 slide 20), as a hint next to the errors.

## D-006: MainActivity with Scaffold and NavHost
Decisions: no new record; follows [0006](../decisions/0006-anonymous-projection-phase-1.md), [0007](../decisions/0007-ticket-detail-by-audience.md) and [0008](../decisions/0008-report-form-state.md). Code: `MainActivity.kt`, one line in `ui/screens/ReportScreen.kt`, `report_submitted` in `values/` and `values-pt/`, the catalog and `app/build.gradle.kts`. Overview: `docs/architecture/overview.md`, "App shell (D-006)".

### Technical solutions
- **One composition root.** `CityFixApp` is the stateful layer over the stateless screens, with each piece of state hoisted to the lowest level that serves all its users. It is the only code that knows about navigation.
- **Three kinds of state, three reasons to survive rotation.** `isExpanded` (UI state shared by the bar and the list) is in `rememberSaveable`, which saves it in the instance state. Data is in ViewModels, which outlive the recreated activity. The back stack is restored by `rememberNavController`. The manifest has no `configChanges`: the design relies on normal recreation.
- **Type-safe routes** with `toRoute<T>()` to read the id and `hasRoute<T>()` to map the destination to the bar's enum.
- **Back-arrow trap.** `previousBackStackEntry` is not Compose state, so it is read in the same scope as `currentBackStackEntryAsState()`. A change of the current entry recomposes that line, and the arrow cannot go stale.
- **Two ViewModels at two scopes.** `TicketsViewModel` belongs to the activity and `ReportViewModel` to the back-stack entry. For the defence: `viewModel()` uses `LocalViewModelStoreOwner`, which is the activity above the `NavHost` and the `NavBackStackEntry` inside `composable`. That is why the same call gives different lifetimes. With activity scope, the old values and the `submitted` lock would come back on the next visit.
- **One-way hand-off between ViewModels.** The form passes a `ReportSubmission` to a callback that returns `Boolean`, so neither ViewModel holds the other.
- **Snackbar state above the `NavHost`,** because the destination that triggers it is popped at once. The message is resolved in the composable body, since `stringResource` cannot be called from `launch`.
- **Named arguments on `reportTicket`,** because three of the parameters are `String` and the order differs from `ReportSubmission`: positional arguments could compile with fields swapped.
- **Insets.** Edge-to-edge with `Scaffold(contentWindowInsets = WindowInsets.safeDrawing)`, and one `padding(innerPadding).consumeWindowInsets(innerPadding)` on the `NavHost`, in that order, so `imePadding()` in the report form does not add an inset twice. Screens receive no `modifier` from the caller, an intended difference from the screens' contract snippets. For the defence: why the order matters.
- **Audience `PUBLIC` in phase 1, and three layers of privacy:** the list filter (`publicActive()`), the projection (`toAnonymousView()`) and the UI branch (`when (audience)`). The third layer is doing work that belongs to the second; see Known gaps. (Until D-007, which moved that work into the projection.)

### Options chosen and rejected
- `navigation-compose` 2.9.8 over the latest 2.10.2: its transitive requirement of Compose `runtime` 1.11.2 would pass our limit of 1.11.0. A good example of how Gradle's "highest requested version wins" affects a choice. 2.10.0 and 2.10.1 were not checked.
- The same rule has a visible side effect: lifecycle 2.11.0 pulls `runtime` and `ui` to 1.11.0, above the BOM's 1.10.4, while `foundation` stays at 1.10.4 (Gate 1 Q5, accepted if it ran cleanly). A BOM bump or moving lifecycle down would change more than this task needs.
- Explicit lifecycle dependencies rather than relying on transitive ones, because the code imports them directly.
- `launchSingleTop` on all forward navigation, for double taps.
- `CityFixApp` in `MainActivity.kt`, as the task names, rather than its own file in `ui/navigation/`.
- One confirmation text for both scopes (Q4): there is no "my tickets" screen to point to.
- `safeDrawing` once on the `Scaffold`, rather than default insets plus `displayCutout` padding on each of the four screens.
- Rejected: navigating to the new ticket (a PRIVATE one is not visible); `getTicketById` for the detail (a snapshot that would not update); `ReportViewModel` at activity scope; a one-shot event in a ViewModel for the confirmation (an event type and a collector to save a 4-second message); `configChanges` to avoid recreation.

### Known gaps
- **Anonymous list shows more than the brief allows, open until [D-007](../tasks/phase-1/D-007-anonymous-projection.md), before delivery.** The expand toggle is wired unconditionally as D-006's task requires (Gate 1 Q2), so a device now shows the free-text description and "Reported on" in the anonymous list. The PUBLIC detail hides both and brief I.1.B leaves them out. Blanking `description` alone would not close this, as `createdAt` stays in the projection, so D-007 decides the toggle too. (Closed by D-007 on 2026-11-10: toggle hidden, cards forced collapsed.)
- **`description` and `statusHistory` are hidden from anonymous users only in the UI** (F-005 review S4), against the rule that privacy is enforced below the UI. D-007 also closes this and amends decision 0006. (Closed by D-007: the projection drops both.)
- Anonymous users can open the report form and see the add button until the Auth task (decision 0008).
- A double tap on the back arrow goes back two screens (review N1, left as planned); `launchSingleTop` guards forward navigation only.
- Rotating while the snackbar shows loses it, and a reported ticket is lost on process death, in which case a route that still holds its id shows "Ticket not found". Both accepted; neither was run.
- A future route silently gets the dashboard title, because `toTopBarScreen()` needs an `else`.

### Verification
- D-006 has no automated tests. `./gradlew assembleDebug` and `./gradlew lint` passed on every slice: 0 errors, 14 warnings (baseline 13, plus one `GradleDependency` for `navigation-compose`). The exception is one edit in slice 3 (the blank line in `values-pt/strings.xml`), after which lint was not rerun until slice 4. The review reran both with `--rerun-tasks`, before slice 6.
- `:app:dependencies --configuration debugRuntimeClasspath` resolved `kotlinx-serialization-core` 1.11.0, `runtime` 1.11.0, `ui` 1.11.0, `foundation` 1.10.4, `material3` 1.4.0, `navigation-compose` 2.9.8, both lifecycle artifacts 2.11.0 and `kotlin-stdlib` 2.3.20, none above a limit.
- Code review: 0 blocking, 1 should-fix (S1, fixed by adding the D-007 task file), 3 nits. N1 was left as planned, N2 stays a manual check and N3 is closed in the docs. Slice 6 came after the review, so the reviewer did not see it. The `null` branch of `onSubmit` cannot be reached from the form and was checked by reading the code.
- Emulator, 2026-11-02, AVD `Iphone_Diogo`, driven through adb, not by hand:
  - First run: no crash and no `NoSuchMethodError`, so the Compose `ui` 1.11.0 and `foundation` 1.10.4 mix runs on the emulator.
  - Passed: dashboard -> list -> detail -> report; system back and arrow through report -> detail -> list -> dashboard; arrow absent on the dashboard; rotation keeps all four screens; expanded state kept on rotation (3 runs); half-filled form kept on rotation and empty after leaving; empty submit shows every field error and stays on the form; PUBLIC report (snackbar, in the list, total 3 -> 4); PRIVATE report (snackbar, no "not found", not in the list, total 4 -> 5); form empty after a submit; anonymous detail shows only header, media and map; PT strings on the dashboard.
  - Cutout: with the tall cutout overlay (126 px) in both landscape rotations, the bar's controls clear the cutout on the dashboard and list, so the `MainTopAppBar` line was not needed. Detail and report were not checked under the cutout.
  - **Unexplained result:** in the first expanded-list rotation test the app showed the dashboard afterwards, with no crash in the log. Three repeats from a clean start, including that exact sequence, did not reproduce it. The cause is unknown. The first run's script was not instrumented, so it may be a test artefact or a rare real loss of the screen. "Rotation keeps the screen" therefore rests on the repeats, not on every run.
  - Not checked: double taps; back on the dashboard after the final build (it left the app in the earlier run); the flash after rotation (review N2, needs frame-level viewing); keyboard overlap and double gap in landscape; the PT snackbar text; a real device.
- Earlier checks this first run closed: the Compose mix (slice 3's stop condition), the F-005 anonymous detail on screen, and the F-006 form's values on rotation and its reset after leaving. Partly closed: the cutout for the bar (D-005 N5, dashboard and list only). Still open: the content under the cutout (F-003 N3), the keyboard, TalkBack (F-006 N2) and the Android Studio previews and accessibility inspector, none of which `state.md` records as run.

### Improvement opportunities
- Instrument the rotation test and repeat it, to explain or rule out the unexplained result.
- Guard the back arrow with the entry's lifecycle (`currentState == Lifecycle.State.RESUMED`), the fix in review N1. `dropUnlessResumed` does not work above the `NavHost`, as it would read the activity's lifecycle. The cost is that a deliberate second tap waits for the crossfade.
- Keep the snackbar across rotation with a one-shot event.
- A different confirmation for PRIVATE reports once a "my tickets" screen exists.
- Align the Compose BOM with the resolved versions when Kotlin moves to 2.3, and move to Navigation 2.10 at that point too.
- Nested graphs per role and deep links from FCM (already in the D-004 notes).
- Screen transitions.
- `LazyColumn` content drawn behind the navigation bar, using `innerPadding` as `contentPadding`.

## F-007: Report attachments
Decision: no new record; see the note in [0008](../decisions/0008-report-form-state.md). Code: `utils/FileUtils.kt`, `ui/components/AttachmentGallery.kt`, `ui/screens/ReportScreen.kt`, `viewmodel/ReportUiState.kt`, `viewmodel/ReportViewModel.kt`, `res/xml/file_paths.xml`. Overview: `docs/architecture/overview.md`, "Report attachments (F-007)".

**Nothing here has run on a device or emulator yet.** The behaviour below is what the code is written to do.

### Technical solutions
- **No `CAMERA` permission.** `TakePicture` hands the capture to the system camera app through a `FileProvider` Uri, so the app needs no permission. It would fail if `CAMERA` were ever declared in the manifest and not granted: the launch then throws `SecurityException`, which `launchCamera` catches to show a notice. The merged manifest has none.
- **`maxItems` must be above 1.** `PickMultipleVisualMedia` requires it, so the last free slot uses the single `PickVisualMedia`.
- **The fallback picker ignores the limit.** Where the photo picker is missing, `ACTION_OPEN_DOCUMENT` ignores `maxItems`. So the real rule sits in the callbacks: `take(remainingAttachments)` for picks, a slot check for the camera. The review found the camera check missing (S1): a fast tap on both buttons could reach 4. A good example for the defence of a rule with two entrances.
- **Position-based `rememberSaveable`.** The launchers and `tempCameraPath` sit in `ReportScreen`, above the orientation branch. Inside a pane their saved key would differ after a rotation and the result would be lost. Same lesson as F-005 review S2.
- **Grid height cap.** `heightIn(max = 240.dp)` must stay: a lazy grid measured with unbounded height crashes inside a `verticalScroll` column.
- **Pinning Coil.** 3.0.4 is the P09 version. 3.5+ needs Kotlin 2.4 and Compose foundation 1.11+, past the project's limits. 3.4.0 is the fallback if 3.0.4 crashes on Compose `ui` 1.11.
- **The type in the file name.** A `VID_` prefix gives the type back from a saved path. Names are always generated, so the source's display name cannot spoof a prefix or traverse a path.

### Options chosen
- **Copy on the main thread (Gate 1 Q13, the person's decision).** Literal P09 (slide 26): the launcher callback copies synchronously, with no coroutine and no "Adding..." state. It stays close to the class material and keeps `ReportViewModel` no-arg with no `Context`. It is an accepted exception to `.claude/rules/kotlin.md` line 15. The cost: a large video can freeze the UI or cause an ANR, and the risk is unbounded, because there is no video size limit (Q7).
- **Differences from P09:**
  - real extensions and a `VID_` prefix, not a fixed `.jpg`, because the form takes videos;
  - `rememberSaveable` and no `!!`, unlike slide 7, because Test Case 3 needs it;
  - no `coil-network-okhttp`, unlike slide 16: local files only, no `INTERNET`;
  - `coil-video` added, and remove buttons on the grid;
  - the limit counts across picks, not per pick;
  - `List<Attachment>`, not `List<String>`, because the model already has it.

### Known limits
- **The form can freeze** during a large copy (see above).
- **Metadata is kept.** Local files hold EXIF and video location metadata. They must be stripped before `MediaTile` draws real files, not only at upload, or the anonymous detail would hold exact coordinates. Decision 0006 and F-007's 00-plan assumed upload time, which is too late if the detail draws local files first.
- **Orphaned files.** Attachments added before a process death are lost and their files stay on disk; so do the files of in-memory tickets after a restart (Q3, Q10).
- **Device-local paths.** `Attachment.file` is an absolute path, valid only on this device and install, until the upload phase.
- **Nested scroll in narrow landscape.** With 2 columns the grid scrolls inside the scrolling pane. By calculation the third tile's remove button shows without scrolling; dragging from the grid into the pane is unchecked.
- **Auto Backup.** The media in `filesDir` is included, because the backup rules were left unchanged (Q10).
- **Unknown MIME type (review N3).** On the `ACTION_OPEN_DOCUMENT` fallback, a video whose provider reports no `video/*` type is stored as `IMG_`: it shows as a photo with the placeholder.
- **No media on the detail or the card yet.** `ShowScreen` and `TicketCard` did not change (Q11), so a new ticket shows "Not available yet". (Fixed by F-009, with photo GPS stripping; see that section.)

### Verification
- F-007 has no automated tests. `./gradlew assembleDebug` and `./gradlew lint` passed on every slice and after the review fixes: 0 errors, 16 warnings (the baseline 14 plus two accepted Coil `NewerVersionAvailable`).
- Static checks by the review: the merged manifest has no `CAMERA` or `INTERNET`, `ReportViewModel` has no `Context`, and all 389 Compose references in Coil 3.0.4 resolve. That rules out a `NoSuchMethodError` on those references; it does not prove the images draw.
- **No acceptance check beyond build, lint and static checks has run.** No device or emulator was used, previews were not rendered, and the S1 and S2 fixes came after the review and are unverified at runtime. The checks left are in `docs/work/F-007-report-attachments/review.md`, "Manual checks left to the person".

### Improvement opportunities
- **Move the copy off the main thread.** The planned alternative was `viewModel.viewModelScope` with `Dispatchers.IO`, because a rotation would cancel `rememberCoroutineScope` and lose the picks. It needs an "Adding..." state, and the limit must count copies in flight. A video size limit would also bound the risk.
- Compression and metadata stripping (brief V), for images and videos, before any file is drawn for other users.
- Real media in `ShowScreen` and `TicketCard`, after the stripping.
- Video capture with `CaptureVideo` (Q8).
- A newer Coil, together with the Kotlin and Compose upgrades.
- The photo picker backport entry in the manifest (Q9), so API 24-29 gets the limit in the picker too.
- `SavedStateHandle` for the draft, and a clean-up of unreferenced files in `filesDir/images`.
- Review nits not fixed: N1 (a notice can outlive its cause; add `onAttachmentNoticeDismissed()`), N2 (`getType` and `mediaFolder` run outside the `try`, so "null on any failure" is not literal), N5 (add a `submitted` check to `onRemoveAttachment`), N6 (`@param:StringRes` for the 7 compiler warnings).
- (Done by F-008: the gallery copy now runs off the main thread; see that section.)

## F-008: Tickets repository
Decision: [0009](../decisions/0009-tickets-repository.md) (supersedes in part [0006](../decisions/0006-anonymous-projection-phase-1.md) and [0008](../decisions/0008-report-form-state.md)). Code: `CityFixApplication.kt`, `data/TicketsRepository.kt`, `data/LocalTicketsRepository.kt`, `data/AttachmentStorage.kt`, `viewmodel/TicketsViewModel(+Factory).kt`, `viewmodel/ReportViewModel(+Factory).kt`, `ReportUiState.kt`, `ReportScreen.kt`, `MainActivity.kt`, manifest. Overview: "Layers", "ViewModel (D-003)", "Report form (F-006)", "Report attachments (F-007)", "App shell (D-006)".

**Not run on a device or emulator yet.** Build and lint pass (0 errors, 16 warnings, baseline); the acceptance checks in `docs/work/F-008-tickets-repository/review.md` are open.

### Why a repository before Firebase
- `.claude/rules/kotlin.md` puts the anonymous filter in the repository layer; until F-008 `TicketsViewModel` called `toAnonymousView()`. Now it is called only in `LocalTicketsRepository`, so D-007's projection change lands in one place.
- The ViewModels depend on the `TicketsRepository` interface, so the Firebase phase adds `FirestoreTicketsRepository` without rewiring screens or ViewModels.
- Model: the class project Contacts_#3 (P08): `ContactsApp`, `ContactsRepository`, `ContactsViewModel(+Factory)`, `EditContactViewModel(+Factory)`.
- Where we differ from Contacts_#3, one sentence each for the defence:
  - an interface (Contacts_#3 has a concrete class): the Firebase reason above;
  - a `Mutex` over the cache: the IO pool can run a load and a report at once, and `TicketsList` is not thread-safe; `getTicketById` is locked too (review S1), unlike `getContactById`;
  - no JSON file: memory only, the samples come back after process death;
  - the `submitted` lock is set before `launch` (Contacts_#3 sets `isSaving` inside `launch`, so it has a double-tap window);
  - the gallery copy runs off the main thread (Contacts_#3 copies in the launcher callback).
  - Same as Contacts_#3: the factories use `@Suppress("UNCHECKED_CAST") ... as T`; the planned `modelClass.cast(...)` gave a compiler warning.

### Manual DI
- `CityFixApplication` (registered with `android:name`) holds one repository and one `AttachmentStorage` `by lazy`. `MainActivity` reads `application as CityFixApplication` once in `onCreate` and passes it to `CityFixApp(app)`, which builds `TicketsViewModelFactory` and `ReportViewModelFactory`.
- Why not Hilt or Koin: two ViewModels and two dependencies; a DI framework adds a Gradle plugin, annotations and generated code that both authors would have to explain, and the course uses manual factories.
- Failure modes: a missing `android:name` crashes the cast at start; `viewModel()` without a factory crashes (no no-arg constructor).

### Who owns state
- Each ViewModel owns its `MutableStateFlow`s and fills them from `suspend` repository calls in `viewModelScope` (`TicketsViewModel.init { refresh() }`), as `ContactsViewModel` does. The repository returns values, not flows.
- Cost: on cold start the flows start empty, so the dashboard and list can show the empty state for one frame; after process death on the detail route, "Ticket not found" flashes for one frame. Accepted in phase 1, no loading state.
- `activeCategories()` stays synchronous so the dropdown has its list on the first frame.
- Tickets now live as long as the process (the repository is in the `Application`), not the activity.

### Async save
- `ReportViewModel.submit(onSuccess)` validates, sets `submitted` and `isSaving` synchronously, then saves through the suspend `reportTicket` in `viewModelScope`. A double tap returns at the guard. `null` unlocks the form with an error.
- `onSuccess` takes no id (D-006 rule: a PRIVATE ticket must never be opened). `MainActivity`'s callback: `refresh()`, snackbar, `navigateUp()`, like `saveContact { refreshContacts(); popBackStack() }`.
- Known callback risks (accepted, as in Contacts_#3): rotation mid-save runs the old lambda on the old `NavController`, so the new screen keeps the form open and locked with no snackbar (`refresh()` still runs); back mid-save may skip the snackbar and `refresh()`, and if cancelled before the add, `onCleared` keeps the draft files (review N4). The save takes milliseconds in memory.
- `isSaving` is not read by the UI yet (`submitted` locks the form); it is there for a progress indicator later (review N1).

### Gallery copy off the main thread
- `data/AttachmentStorage` copies on `Dispatchers.IO`, launched from `ReportViewModel.onMediaPicked` in `viewModelScope`, so a rotation does not cancel it; leaving the form cancels it and the copied files are deleted.
- Async copy reopened F-007's limit race (review B1): two media results arriving back to back could exceed 3. Fix: the limit is checked again when each copy returns, against the slots free then; extra copies are deleted with the limit notice; copies returning after a submit are deleted. A `copiesInProgress` counter keeps Submit and the add buttons disabled until the last copy ends. A good example for the defence of how moving work off the main thread turns a synchronous check into a race.

### Improvement opportunities
- `FirestoreTicketsRepository` behind the same interface, with listeners instead of `refresh()` (also fixes the two separate locks in `refresh()`, review N2). (Done by F-011.)
- A separate `CategoriesRepository` (categories per municipality).
- A `saved` flag in `ReportUiState` consumed by a `LaunchedEffect` instead of the `onSuccess` callback; `onCleared` keyed on "saved" rather than "submitted" (review N4, N7).
- An `isLoading` state to hide the first empty frame.
- JSON persistence on `Dispatchers.IO`, as in Contacts_#3, if tickets must survive process death before Firestore.
- A progress indicator and a "Copying..." string while `isCopyingAttachments` (review N5); a video size limit.
- `AttachmentStorage` as an interface when the WorkManager upload task touches it (review N9); move `getType` inside the `try` in `FileUtils` so a `SecurityException` cannot escape the copy (review N6).

## F-009: Show attachment images on the card and the detail (bug fix)
Decision: no new record; [0006](../decisions/0006-anonymous-projection-phase-1.md) media point amended (2026-11-08). Code: `ui/components/AttachmentImage.kt` (new), `ui/components/AttachmentGallery.kt`, `ui/components/TicketCard.kt`, `ui/screens/ShowScreen.kt`, `utils/FileUtils.kt`, `data/LocalTicketsRepository.kt`, `model/TicketPrivacy.kt` (KDoc only), version catalog. Overview: "Shared components", "Screens" (`ShowScreen`), "Media is outside the projection".

**Only a smoke check ran on the emulator** (install, list, a sample detail, no crash). Reporting with a camera photo or gallery video and checking the card, the detail and the stripped EXIF has not run (review S3).

### Root cause
- F-007 saved real files and the path reached the ticket (`Attachment.file`, kept by `toAnonymousView()`), but the two F-005 placeholders never read it: the card read the never-set `thumbnail`, and `MediaTile` drew only a type icon. The data path was right; the screens were not.
- Lesson: when a later task makes data real, check every consumer built against the placeholder.

### Technical solutions
- **One shared `AttachmentImage`**, the Contacts_#3 `ContactAvatar` pattern: one composable that branches on a blank path and is reused by the list and the detail. Here it serves the form gallery, the card and the detail, so the video-frame logic (`VideoFrameDecoder` per request) exists once. Unlike `ContactAvatar` it takes its size from the caller's modifier, handles video, and takes a localised description from the caller.
- **`SubcomposeAsyncImage`** with `loading`/`error` slots that draw the same small tinted icon as a blank file. A painter placeholder was untinted and stretched by Crop (review N1). `colorFilter` was not used, because it would tint the photo too.
- **Photo GPS removed in the repository.** `FileUtils.removeImageLocation` clears every EXIF GPS tag with `androidx.exifinterface` 1.4.2 (32 constants, since `ExifInterface` has no call to clear the whole block); make, model and capture time stay. It runs in `reportTicket`, the one point the camera and gallery paths both pass through, on IO, before the ticket becomes visible. The form shows the file before stripping, but that is the author's own draft, and a decoded bitmap carries no EXIF.
- **Failure keeps the report** (Gate 1 Q2): a HEIC that cannot be rewritten keeps its tags rather than losing the user's report.

### Known limits
- Video location metadata is kept (deferred to the media task, Gate 1 Q1), and so is location in an XMP packet (review N4).
- A failed strip is silent: no log line (review N2).
- The `TicketsRepository.reportTicket` contract does not say an implementation must strip GPS, so a Firebase repository could forget it (review N3).
- The card's description reads "Ticket photo" also when the first attachment is a video (review N5).
- The card has its own placeholder for "no attachment" next to `AttachmentImage`'s blank-file one (review N6, kept as a deliberate distinction).

### Improvement opportunities
- Strip video location with a remux (`MediaMuxer` or Media3 Transformer), and handle XMP.
- Real thumbnails (`Attachment.thumbnail`) generated at compression, so the list does not decode full photos (Coil downsamples, but still reads the whole file).
- A `Log.w` on a failed strip; one KDoc line on `TicketsRepository.reportTicket`; a "Ticket video" description.

## F-010: Firebase setup and Firestore schema
Decisions: [0010](../decisions/0010-firestore-schema.md) (layout and rules design), [0012](../decisions/0012-firebase-plan-notifications-close-maps.md) (plan, notifications, 5-day close, maps), [0009](../decisions/0009-tickets-repository.md) amended. Code: `CityFixApplication.kt`, `firestore.rules`, `storage.rules`, `firebase.json`, version catalog, both Gradle files. Overview: "Firebase (F-010)", "Setup", "Handover (F-010)".

**The rules are drafts, checked by reading only** (three review rounds; no Rules Playground, no emulator), and not deployed. The smoke test and the offline check ran on the emulator on 2026-11-10 and passed (online write/read, cache read after restart, offline write synced on reconnect).

### Technical solutions
- **One Firebase container in `Application`.** `CityFixApplication` holds `auth`, `firestore`, `storage` and `messaging` `by lazy`, next to the repositories: manual DI as `ContactsApp` in Contacts_#3 (decision 0009). No Hilt/Koin: four properties are easier to explain than a DI framework.
- **Persistent Firestore cache set explicitly** (`persistentCacheSettings { }`, default 100 MB) inside the `lazy` block, so it is applied once and before any other Firestore call (brief IV, offline). It is the Android default, but writing it makes the choice visible for the defence.
- **Public projection as a separate collection.** Firestore rules secure whole documents, not fields, so anonymous users cannot read a filtered `tickets`. `publicTickets/{id}` holds only whitelisted keys (category, situation, status, coarse location, thumbnails, dates), written only from `toAnonymousView()` in the same batch as the ticket. The rules reject any other key, so author ids, descriptions and exact coordinates cannot get there.
- **`ticketLocations` for the 10 m grouping.** Citizens cannot read other citizens' tickets, and the public copy has only coarse locations, so a minimal collection (category, situation, exact location, `active`) readable by signed-in active citizens and operators feeds the grouping check.
- **Grouping enforced in rules as far as rules can.** A citizen joins a ticket only in one batch that writes their own report `tickets/{id}/reports/{uid}` and adds their uid to `followerIds`/`participantIds`; the rule checks (with `getAfter`) that the report has the ticket's category and situation and lies inside an ~11 m bounding box (0.0001 deg latitude, 0.00013 deg longitude at 40.2 N). The exact circle is a client check.
- **5-day window on server time.** The RESOLVED write sets `resolvedAt` with `FieldValue.serverTimestamp()`, and the rule demands `resolvedAt == request.time`, so no client can backdate it. Reopen (`<= resolvedAt + 5 d`) and close (`> resolvedAt + 5 d`) compare it with the server time of the later write. Found at review round 2: the first version anchored the window on the history entry's client-written `dateTime`, so an operator could skip the citizen's reopen window by sending a backdated entry. A good defence example of "both ends of a time check must be trusted".
- **Append-only history in rules.** Each status change appends exactly one entry by the caller (`responsibleId` = self, `status` = new `currentStatus`, earlier entries equal via a list slice, `dateTime` not before the previous one and at most 5 minutes ahead of server time). The status must be one of the five `TicketStatus` names and must change. CLOSED locks every write path, Storage uploads included.
- **Operators prove the domain with a verified e-mail.** `isOperator()` needs `email_verified == true` and an e-mail in `municipality/main.emailDomain`. Review round 1 found that without it anyone could sign up as `x@<domain>` and read every citizen's NIF and phone.
- **Storage cross-service rules within the documented limit** of two Firestore documents per evaluation: each rule reads the ticket once (and checks `scope == 'PUBLIC'` instead of `exists(publicTickets/...)`) plus the caller's user document.

### Options taken
- `statusHistory` and `attachments` as **arrays** in the ticket, so `currentStatus` and the history change atomically in one write (decision 0004); a ticket has few entries.
- `messages` as a **subcollection**: it grows without limit, is read only in the detail and never by anonymous users.
- **NIF uniqueness** through `nifs/{ROLE}_{nif}` created in the registration transaction together with `users/{uid}`; each rule checks the other (`getAfter`), so a NIF cannot be squatted.
- **One `municipality/main` document** (name, lowercase `emailDomain`, centre, 10 km radius); it also maps the diagram's `define` and `pertence a`.
- **Batches offline, transactions only online** (registration, upload worker), because transactions fail without a network and reporting must work offline.
- **Storage paths without user ids** (`tickets/{ticketId}/{attachmentId}...`), so a public media URL reveals no author.
- **Media for anonymous users** through cross-service Storage rules on the ticket's scope.
- `participantIds` (creator + followers) so "my tickets" is one `whereArrayContains` and one rule guards ticket, messages and media.
- `Long` times and enum names kept (decision 0003); `resolvedAt` is the one `Timestamp`, because only a server timestamp is trusted by rules. `GeoPoint` only in `data/`.
- **The four Gate decisions** (decision 0012): Blaze with a budget alert (cost guard; Storage is impossible on Spark); no Cloud Functions (no server code in a second language that both authors must defend, no deploy pipeline); 5-day close computed on read; osmdroid (no key, so the ZIP builds and the map renders for the grader). Also: `google-services.json` out of git and copied into the ZIP by hand; test mode until rules are deployed; SMS MFA with test numbers.
- **Firebase BoM 34.19.0, not 35.1.0:** 35.x libraries carry Kotlin 2.4 metadata, which our Kotlin 2.2.10 compiler cannot read. Upgrading Kotlin would have touched every plugin; stepping the BoM down was the plan's fallback. One accepted lint `GradleDependency` warning.

### Rejected
- One `tickets` collection with field filtering for anonymous users (rules cannot hide fields).
- Subcollections for history and attachments (a batch per transition, extra reads).
- Two merged queries (`creatorId ==`, `followerIds array-contains`) instead of `participantIds`.
- A free "follow" button (the brief makes grouping automatic, I.2 obs.); grouping only PUBLIC tickets (the brief ignores scope).
- A Cloud Function for grouping, push, the 24 h warning or the scheduled close; Google `maps-compose` (key in `local.properties`, which the ZIP excludes); Spark; production mode from day one; TOTP MFA.

### Limits to state honestly
- **Blaze is needed only because of the 2024-2026 Storage pricing change** (default buckets need Blaze since 2024-10-30; Spark lost access on 2026-02-03). A billing account and a budget alert are part of the setup.
- **Notifications without a server work only while some client runs** (the app or its WorkManager worker); the 24 h warning is scheduled locally.
- **The 5-day close is computed on read**, not by a server: the ticket shows as CLOSED at once, but the CLOSED write happens when the first client sees it.
- **Not enforceable in rules:** the exact 10 m circle (bounding box only, rules have no trigonometry); the truth of the reported location (a citizen could copy a ticket's location from `ticketLocations` and join any ticket, PRIVATE included, which follows from the brief's automatic grouping); edits of existing attachment entries (rules cannot loop over lists); `coarseLocation` rounding; video metadata stripping (F-013).
- **The 10 km anonymous radius is checked on the client only** (anonymous requests carry no location).
- **An offline RESOLVED gets the sync time as `resolvedAt`**, not the tap time; the history entry keeps the device time, so the two can differ. A device clock more than 5 minutes fast cannot write status changes.
- **Signed-in active citizens can read exact locations, category and situation of every ticket** through `ticketLocations` (no author, description or media). Accepted for Option A.
- **`google-services.json` is handled outside git** and copied into the ZIP by hand.
- **The rules are drafted but not deployed (decision 0012 Q5, amended 2026-11-13).** They were checked by reading only, never run in the Rules Playground or the emulator. The database runs in test mode (expires 2026-12-10), open to anyone with the project config, so the privacy and lifecycle rules hold only in the app code. Chosen because this is a school project that will not reach production; deploying the drafted rules is the first improvement for a real deployment.
- **Smoke test only:** the offline check proved the cache with one test document; real offline flows (reporting with no network, the upload worker) are tested in F-011/F-013.

### Verification
- `./gradlew assembleDebug lint` after every slice and every review round: BUILD SUCCESSFUL, 0 errors, 17 warnings (16 before plus `GradleDependency` for `firebase-bom`).
- `git check-ignore -v app/google-services.json` matches `.gitignore`; the file was never staged or committed; no key or project id in the diff.
- Three review rounds on the rules (one blocker, then one should-fix per round, all fixed); syntax checked against the rules reference by reading.

### Improvement opportunities
- **Cloud Functions** for push on status change, the 24 h warning and the scheduled close (and server-side grouping with the exact circle).
- **App Check**, so only the real app can use the project config.
- **Rules unit tests with the Firebase Emulator Suite** (`@firebase/rules-unit-testing`), covering each allow/deny case the reviews traced by hand.
- **Geohash queries** for the 10 m grouping instead of loading `ticketLocations`.
- Upgrade Kotlin to 2.4 and the Firebase BoM to 35.x together.
- `stats/summary` counters (or `count()` aggregations the rules allow) for the anonymous dashboard (F-011). (Done by F-011: counters, decision 0013.)
- A note in the `CityFixApplication` KDoc that `Firebase.firestore` must never be called directly (review N6), and the remaining round-1 nits (key whitelists per role, active category and at least one attachment on ticket create).

## D-007: Anonymous projection and expand toggle
Decision: [0006](../decisions/0006-anonymous-projection-phase-1.md), amended 2026-11-10 (closes F-005 review S4 and the list/detail consistency point of [0007](../decisions/0007-ticket-detail-by-audience.md)). Code: `model/TicketPrivacy.kt`, `ui/navigation/MainTopAppBar.kt`, `MainActivity.kt`. Overview: "ViewModel (D-003)", "Top bar (D-005)", "App shell (D-006)".

**The emulator and Android Studio preview checks have not run yet** (review S1). Build and lint pass (0 errors, 16 warnings, baseline); the rest was checked by reading the code.

### Technical solutions
- **Privacy in layers, now complete for phase 1.** The list filter (`publicActive()`) picks which tickets; the projection (`toAnonymousView()`) picks which fields, now including `description` and `statusHistory`; the UI shows only the I.1.B/I.1.C fields. The third layer no longer does the second's job, which was the gap since F-005.
- **Why `currentStatus` survives without the history.** It is a denormalised copy (decision 0004), and this is where that pays off: the status badge needs no history, so the whole history can be dropped. A good defence example of a denormalisation chosen for queries that also helps privacy.
- **Dropping the whole history instead of blanking each entry.** The per-entry blanking of `responsibleId` and `justification` (D-003) is gone; one `emptyList()` also removes the creator's id hidden in the first OPEN entry. Simpler code, fewer fields to remember.
- **Why the dashboard does not change.** Stats are computed from the raw tickets inside the repository (`loadStats()`) and leave it as aggregate counts, so no per-ticket data is exposed and clearing the history in the projection cannot affect the average repair time. The list order is also safe: `publicActive()` sorts before the projection.
- **Toggle hidden, not given an empty expanded state.** `MainTopAppBar` takes a required `showExpandToggle` (no default, so the caller must decide by role, like `DetailAudience` in `ShowScreen`). `CityFixApp` has one `val canExpandCards = false`, the hook for the role check in D-009, passed to the bar and to the list as `canExpandCards && isExpanded`. The button is removed, not disabled, so TalkBack never reaches an inert control.
- **Why the `&&` stays** although `isExpanded` cannot become true today: once D-009 makes the flag depend on the role, a registered user who expanded the cards and logs out keeps `isExpanded = true` in `rememberSaveable`, and the `&&` still forces the anonymous cards collapsed.
- **Why `createdAt` stays in the projection.** Ordering in Firestore (`orderBy` in F-011); it is not shown to anonymous users, and it is not the author's personal data.
- **The projection as the public document.** The same pure function builds the Firestore `publicTickets` document (F-011), so phase 1 and phase 2 share one rule. Firestore cannot hide fields inside a document, so a separate collection is the only way. Decision 0006 now holds the field table and the rules F-011 must keep (a view, not a valid ticket; no stats, lifecycle or grouping from it; rewrite on change; missing fields read as defaults).

### Options chosen and rejected (Gate 1)
- Q1, option A: hide the toggle and force the cards collapsed for anonymous users. Matches I.1.B exactly, keeps the list and the detail consistent, changes only Diogo's files.
- Rejected B: an expanded PUBLIC card with only allowed fields (changes Francisco's files; a design the brief does not ask for; a button that does nothing visible).
- Rejected C: zeroing `createdAt` so the card skips the date (the toggle again does nothing visible; ordering needs the field; `0L` means "not set", decision 0003).
- Q2: keep `createdAt`; rounding it to the day was code for little gain.
- Q4: keep `isExpanded` and the toggle code for registered citizens and operators.
- Q5/Q6: no edits to Francisco's files or decision 0007; handovers recorded in the overview.
- Rejected again: a `PublicTicket` type (decision 0006), and keeping a history with only status and date (I.1.C says "apenas").

### Known limits (Gate 1 Q7, recorded in decision 0006 with owners)
- Camera photos are named `CAPTURE_<epoch millis>.jpg`, so the file name carries the capture time (F-013).
- Photo EXIF keeps make, model and capture time (F-013 / media task).
- Video location metadata and XMP location are not removed (media task).
- Coarse locations collapse onto one grid point; a map must cluster (F-012).
- D-006's "expanded state kept on rotation" check cannot run on a device until roles exist; the previews still render the expanded card.

### Verification
- No automated tests. `./gradlew assembleDebug` and `./gradlew lint` passed after both slices and after review fix N1 (0 errors, 16 warnings, none in the changed files).
- Review greps: every read of `description`, `statusHistory` and `createdAt` in `app/src` is either in an expanded card (unreachable for anonymous users) or in a non-PUBLIC detail branch; nothing reads the history of a projected ticket; stats run on the raw list.
- Pending (review S1): the emulator checks (no toggle in portrait/landscape and EN/PT, collapsed cards, `sample-1`/`sample-2` detail, a new PUBLIC report with a photo, dashboard 3 / 67% / 3 days then 4 total / 3 active) and the previews (three `MainTopAppBar` list previews, `TicketCard`/`ListScreen` expanded).

### Improvement opportunities
- A separate `PublicTicket` type would prove the rule at compile time (rejected for now in 0006, because it duplicates `Ticket` and breaks the agreed screen signatures).
- Round `createdAt` to the day if dates ever become sensitive.
- Neutral camera file names (`IMG_<UUID>.jpg`) and Storage paths without times or user ids (F-013).
- Strip video and XMP location, and optionally make/model/time EXIF (media task).
- One KDoc line on `Ticket.currentStatus` saying projected tickets keep it without a history (review N2). (Done by D-008.)

## D-008: Ticket lifecycle rules
Decision: [0011](../decisions/0011-ticket-lifecycle-rules.md) (builds on [0004](../decisions/0004-denormalised-current-status.md) and [0006](../decisions/0006-anonymous-projection-phase-1.md)). Code: `model/TicketLifecycle.kt`, `model/AttachmentPhase.kt`, `model/Attachment.kt`. Overview: "Lifecycle (D-008)".

**Brief status (updated 2026-11-11).** The brief PDF could not be opened in the D-008 sessions; the rules came from the task files. The brief section I text Diogo pasted in D-009 now checks I.3.E ("pedido de dados" is an operator action; resolving needs "carregamento de foto/vídeo do trabalho concluído") and I.2.E (reopening among the citizen's own tickets, "se dentro de 5 dias após conclusão"); the I.2 Obs ("acompanham") supports followers reopening as a reading, not literally. Sections II, II.2 and III (automatic close after 5 days, lock, information requests, 24 h warning) are still not checked: check them in the PDF before writing them into the report as the brief's wording.

### The state machine
| From (effective) | Operator | Citizen (creator or follower, `ACTIVE`) |
| --- | --- | --- |
| OPEN | INFO_REQUESTED, IN_PROGRESS | - |
| INFO_REQUESTED | IN_PROGRESS | - |
| IN_PROGRESS | INFO_REQUESTED, RESOLVED | - |
| RESOLVED (< 5 days) | - | OPEN (reopen, with justification) |
| CLOSED (stored, or RESOLVED >= 5 days) | - | - |

Diagram for the report (draw it; text version):
```
OPEN --op--> INFO_REQUESTED <--op--> IN_PROGRESS --op--> RESOLVED --citizen, < 5 d--> OPEN
  \--op--------------------------->/                        \--time, >= 5 d (system)--> CLOSED (locked)
```
- Nobody closes by hand; CLOSED comes from time only. A citizen's question is a message, not a status change (Gate 1 Q1).

### Technical solutions
- **One pure function changes the status.** Decision 0004 keeps `currentStatus` next to the history; `Ticket.transition` is the only place that changes both (one `copy`, one history entry with time, user and justification), so the citizen and operator screens apply the same rules and the history is a full audit trail (brief I.3.E timestamps, not checked against the brief).
- **`allowedTransitions(by: User, now)`** works on the effective status and includes the role (the type of the sealed `User`), the relation (creator or follower), the registration and the window. The UI offers exactly this set and `transition` accepts only it, so the screen and the rule cannot disagree.
- **Reopening and closing without stored fields.** Both are computed from the last RESOLVED entry. CLOSED is shown on read (`effectiveStatus`) and written lazily by `closeIfExpired` (Spark plan, no server), timestamped at the exact closing moment (`resolvedAt + 5 d`) rather than when someone noticed, with `responsibleId = "system"`.
- **Consistent boundary.** `<` in `canReopen`, `>=` in `effectiveStatus`, on the same difference: at exactly 5 days the ticket is closed and not reopenable, never both.
- **Proof of resolution.** `AttachmentPhase { REPORT, RESOLUTION }` answers phase-1 risk 1. The proof is passed to the RESOLVED transition and appended in the same operation, so an offline queued write stays consistent and a re-resolve after a reopen needs new proof.
- **Refusals as `null`**, like `reportTicket` and `TicketsList.add`; exhaustive `when` with no `else` for the operator moves, so a new status does not compile until its moves are decided.
- **Safe on projected tickets.** A projection has no history, so every function returns `null`/`false`/empty instead of a wrong answer.
- **Nothing else changed.** RESOLVED and CLOSED are both inactive, and a later CLOSED entry does not move the last RESOLVED time, so the public list, the grouping and the dashboard are unaffected.

### Options chosen and rejected (Gate 1, 2026-11-11)
- Q2: creator and followers may reopen (Option A); rejected creator only.
- Q5: `allowedTransitions(by, now)`; rejected the task's static `allowedTransitions(status, role)`.
- Q6: `AttachmentPhase` added in D-008 (decision 0011), F-010 reuses it; rejected waiting for F-010 or a predicate parameter.
- Q7: `Ticket?`; rejected throwing with `require`.
- Q8: proof as a parameter of the RESOLVED transition; rejected "the ticket has any RESOLUTION attachment".
- Q9: no committed tests; Q10: no new sample tickets (previews use a fixed `t0`).
- Rejected: an `object TicketLifecycle`, a manual CLOSED transition for operators.

### Defence notes
- `canReopen` is time-only; the Reopen button uses `OPEN in allowedTransitions(user, now)` (review S1).
- `closingWarningAt()` can be in the past for an expired ticket not yet closed; F-016 checks `!isLocked(now)` (review S2).
- In `transition`, `isReopen` also tests `currentStatus == RESOLVED`, which is always true when `to == OPEN` (OPEN is only allowed through `canReopen`). Kept on purpose: it states the rule in the code (review N4).

### Verification
- No automated tests (Q9). `./gradlew assembleDebug` and `./gradlew lint` passed after each slice and after the review fixes (0 errors, 16 warnings, baseline). Greps: no imports and no `System.currentTimeMillis` in `TicketLifecycle.kt`. The implementer and the reviewer each walked the T1-T8 / R1-R16 table (boundaries at 4 d 23 h, 5 d, 5 d 1 h; proof rules; roles; projected tickets; one history entry per transition). Nothing has run on a device: no screen calls the rules yet.

### Limits and improvement opportunities
- Device clocks decide the window; a Firestore rule on `request.time` or a Cloud Function would make it server-side.
- Concurrent transitions are last-write-wins; a transaction or a precondition on `currentStatus` fixes it (F-011/D-013).
- History can be out of time order with offline queues (`now` earlier than the last entry is accepted).
- Old RESOLUTION proof stays on a reopened, public ticket; screens should label or filter by `phase`.
- No automated tests by project choice; the pure functions would be easy to cover with JUnit.
- A `PublicTicket` type and a sealed result type (instead of `null`) would let the compiler enforce more, and tell the UI why a move was refused.

## D-009: Login, MFA, password recovery and role navigation
Decision: [0015](../decisions/0015-session-role-navigation-mfa.md) (also amends [0006](../decisions/0006-anonymous-projection-phase-1.md) on `canExpandCards`). Code: `data/AuthRepository.kt`, `data/FirebaseAuthRepository.kt`, `data/UsersRepository.kt`, `data/FirestoreUsersRepository.kt`, `viewmodel/Session.kt`, `viewmodel/SessionViewModel.kt`, the Login / Forgot password / Account setup ViewModels, `ui/navigation/RoleAccess.kt`, the new screens, `MainActivity.kt`. Overview: "Auth and session (D-009)", "Roles", "Handover (D-009)".

Brief (section I text pasted by Diogo; the PDF itself was unreadable in the session): I.2.B and I.3.B ask for "e-mail/password, com duplo fator (MFA) e recuperação de credenciais por e-mail" for both the registered citizen and the operator; I.1.B and I.2.D define what the public list shows and that citizens keep the anonymous features; I.2.A leaves a citizen "pendente até validação"; I.3.E puts the operator's full view in the "Painel de Gestão Operacional".

Brief I.1 (same paste): the 10 km limit around Pr. Republica applies to the non-authenticated user.

**Nothing has run on a device or in the console yet.** Build and lint pass (0 errors; 18 warnings before the 2026-11-14 merge of F-011/F-012, 20 after it, all version or unused-resource warnings). Three review rounds, approved (round 3 covered the merge and the session wiring).

### Technical solutions
- **A sealed `Session` drives navigation.** Seven states (`Loading`, `Anonymous`, `NeedsEmailVerification`, `NeedsSecondFactor`, `ProfileMissing`, `ProfileUnavailable`, `SignedIn(user)`), checked in a fixed order, and one `AccessRole` per home screen. Every navigation and visibility rule is a plain `when` on it, so the compiler forces a decision for every state and the code reads like the table in the overview.
- **Two sources combined in one ViewModel.** `AuthRepository.account` (Auth state) and `UsersRepository.observeUser(uid)` (the `users/{uid}` listener) stay separate; `SessionViewModel` combines them with `flatMapLatest`, so logging out or switching account **cancels** the previous Firestore listener. The task asked for `currentUser: Flow<User?>`, which cannot express "e-mail not verified", "no second factor" or "profile not loaded".
- **Redirect keyed on the role, with a saveable "last role".** One `LaunchedEffect(role)` after the `NavHost` sends the user to their home on login, logout or activation and clears the back stack. The last handled role is in `rememberSaveable`, so rotation and process recreation keep the back stack; on a fresh cold start (nothing restored) it goes home unless home is already the start destination. A good defence example of a bug found in review: the plan said "only record the first value", which left operators on the public dashboard after a cold start (B1).
- **Guarding in layers.** Hidden entry points (add button, overflow menu by role) -> `RequireAccess` on every role destination (back-stack leftovers, future deep links) -> the Firestore rules, which describe the server enforcement but stay undeployed drafts (decision 0012 Q5), so in this delivery the app layers are the enforcement. The same "UI is a second layer" idea as the anonymous projection.
- **The session drives the tickets (2026-11-14).** `Session.ticketsUser` (active citizen or operator, else `null`) goes to `TicketsViewModel.setUser` from a `LaunchedEffect`; `setUser` skips a call with the same role and id, so rotation, process death and profile-only changes (`fcmTokens`) keep the Firestore listener and the list instead of blanking it. The report's creator id is the signed-in citizen's uid. F-011's hand-edited stand-in user is gone.
- **Log out clears first, then signs out.** One `logOut` helper calls `setUser(null)` before `signOut()`, so the frame after Log out never draws the previous user's own or operator-only tickets (review R3-S1). A good defence example of ordering: reacting to the session change alone leaves one frame of stale data.
- **10 km gate as an overlay, only for the anonymous user (brief I.1).** `showGate = role == ANONYMOUS && !onAuthScreen && status != INSIDE`: never while the session loads, never for a signed-in user, lifted on the login screens its Log in opens. It is drawn in a `Box` over the `Scaffold` (opaque `Surface` for touches, `clearAndSetSemantics` for TalkBack, no bar controls), so the `NavHost` stays composed and the gate's Log in has a graph to navigate in, which F-012's early `return` lacked.
- **MFA flows** with the Firebase API: sign-in `FirebaseAuthMultiFactorException` -> `MultiFactorResolver` -> `PhoneAuthProvider.verifyPhoneNumber` (hint + resolver session) -> `PhoneAuthProvider.getCredential` -> `PhoneMultiFactorGenerator.getAssertion` -> `resolveSignIn`; enrolment `multiFactor.session` -> `verifyPhoneNumber` (phone + session) -> `getAssertion` -> `multiFactor.enroll`.
- **Callbacks turned into a suspend call.** `verifyPhoneNumber`'s callbacks are wrapped in `suspendCancellableCoroutine` inside `withTimeoutOrNull(60 s)`, so the ViewModel writes a straight `val result = repository.sendCode(...)` and a lost callback becomes a timeout with Resend instead of a hang.
- **Firebase types stay in `data/`.** The resolver is wrapped in an opaque `SecondFactorChallenge` (internal constructor), held in a private ViewModel field, never in `UiState` or saved state. The `Activity` that reCAPTCHA needs is passed through the call and never stored.
- **E-mail verification + token refresh** before enrolment: Firebase requires a verified e-mail for MFA, and the operator rule checks `email_verified`, so "I have verified" runs `reload()` + `getIdToken(true)` (the F-010 handover).
- **Offline profile vs missing profile.** A non-existent `users/{uid}` that comes from the cache is reported as "unavailable" (offline), not "missing", so an offline user is never told their account is not set up.
- **Structured concurrency kept in the repository.** Every `catch (e: Exception)` around `await()` first rethrows `CancellationException` (review S3), so closing a screen cancels the call instead of turning it into an error.

### Options chosen and why (Gate 1, 2026-11-11)
- **SMS with console test numbers, not TOTP** (decision 0012): no authenticator app at the defence, no SMS cost, fixed codes.
- **MFA mandatory for both roles** (Q1; I.2.B, I.3.B): enrolment happens in D-009's Account setup after the first login (verify e-mail, phone, code).
- **Console-seeded test accounts until D-010** (Q9): no seeding code to defend, and client code could not write an operator profile under the rules.
- **Pending citizens may log in** (Q2) and get a waiting home with the anonymous features; activation switches them live.
- **No expand toggle for anyone** (Q7, Diogo's deviation from the plan's "operators only"): the cards and the detail render only public fields for every role (even now that a citizen's list holds their own tickets and an operator's all), so an expanded card would add only the date; the full views are F-014 and the D-013 panel (I.3.E).
- **Pending citizens are not gated by the 10 km limit** (review R3-N3): brief I.1's literal reading limits only the non-authenticated user. Cost: once registration (D-010) exists, registering lifts the limit before an operator validates the account. The stricter reading is a one-line change Diogo may still make.
- **No extra session store** (Q5): Firebase Auth persistence and the Firestore cache already keep the session and profile offline (brief IV); a DataStore copy would be a second source of truth.
- **Login, Forgot password and Account setup as routes; the code as a step of Login** (Q6), because the resolver is not serialisable.
- **Rejected:** per-role graphs (crash on an unknown route), a session-driven `startDestination` (resets the graph), a `CompositionLocal` session (hidden dependency), optional MFA, an e-mail one-time code (needs a server), `setAppVerificationDisabledForTesting` (documented for phone sign-in, not MFA).

### Limits to state honestly
- **Login needs the network**; only an already-cached session works offline.
- **Rotation while the code is being sent** can lose the Firebase callback (bound to the activity); the 60 s timeout and Resend recover. Not yet observed on a device.
- **reCAPTCHA on non-Play builds:** debug builds may show a reCAPTCHA page; whether it appears on our AVD is not yet recorded.
- **One second factor per user:** a lost phone needs a reset in the console; there is no self-service recovery.
- **MFA is enforced only in the app.** The rules stay undeployed drafts (decision 0012 Q5, amended 2026-11-13), so no server check of `sign_in_second_factor` (or of any role) exists; the database is in test mode until 2026-12-10.
- **Real SMS cost and the region policy** apply to real numbers only; test numbers send nothing.
- **The F-011/F-012 stand-in user: done (2026-11-14, `46ee41f`).** It was replaced by the signed-in session (`Session.ticketsUser` -> `TicketsViewModel.setUser`, the creator id from the session, the gate's Log in wired) and removed from `CityFixApplication`. A follow-up slice Diogo approved after the merge, not in the original plan.
- **An involuntary sign-out** (token revoked, account deleted) bypasses the `logOut` helper, so one frame of the previous user's list (public fields only) can still show before it is cleared.
- **Pending citizens outside 10 km** can browse the public screens (see options above). System Back still acts on the screen behind the gate (matters only if the gate appears mid-session).
- **Placeholder screens** for My tickets (F-014), Profile (D-011), Operations panel (D-013), Approvals and Categories (D-012).
- **A short gap after the second factor:** the session is still the previous state until the profile snapshot arrives (review N7); nothing is visible, but code reacting to login must wait for `SignedIn`.
- **The verification e-mail is sent once per screen entry**, not once per account (review N4), which can hit the rate limit after several restarts.
- **Firebase `errorCode` strings** for wrong and expired codes come from the docs and are not yet confirmed in Logcat.

### Verification
- No automated tests (project choice). `./gradlew assembleDebug` and `./gradlew lint` after every slice and every review round: 0 errors, 18 warnings before the merge and 20 after it (the baseline each time, no new warning).
- Static checks before each commit: `app/google-services.json` ignored and never staged; no Firebase import outside `data/` and `CityFixApplication.kt`; `Firebase.firestore` only in `CityFixApplication.kt`; no phone number or code in the diff; EN/PT string keys match.
- **Not run yet (device and console, for Diogo):** the console steps; login per role landing on different homes (including cold start as operator, setup user and pending citizen); wrong / right code at sign-in and enrolment; the verify and reset e-mails arriving; pending -> ACTIVE live switch; airplane-mode cold start; rotation and process death on each step; R2 and R3; PT strings and TalkBack on screen; the gate (anonymous outside 10 km sees it, Log in opens Login, signed-in users are not gated); citizen own + public and operator all tickets, public list after Log out with no stale frame; a new report's `creatorId` equals the citizen's uid. Which AVD (Google Play image or not) and the results go here once run. Rules: not deployed, by decision (0012 Q5). Never write test numbers, codes, UIDs or e-mails here.

### Improvement opportunities
- A second factor or TOTP as a backup, so a lost phone does not lock the account.
- Deploy the rules, with the server-side `sign_in_second_factor` check.
- Clear the tickets on any sign-out inside `TicketsViewModel` (for example from the Auth state itself), so an involuntary sign-out has no stale frame either.
- Gate pending citizens too, if the stricter reading of brief I.1 is preferred.
- Deep-link-safe guards (today `RequireAccess` covers the back stack; deep links are not declared).
- The SplashScreen API instead of the in-app progress gate.
- SMS Retriever / User Consent API to fill the code automatically.
- A re-authentication flow for sensitive actions (D-011 password change; `RECENT_LOGIN_REQUIRED`).
- Send the verification e-mail once per account (remember the last send), and confirm the `errorCode` strings.

## F-011: Firestore tickets repository and public projection
Decisions: [0013](../decisions/0013-firestore-reads-and-dashboard-counters.md) (reads per audience, stand-in user, flows, counters, seed), [0009](../decisions/0009-tickets-repository.md) and [0010](../decisions/0010-firestore-schema.md) amended. Code: `data/TicketsRepository.kt`, `data/FirestoreTicketsRepository.kt`, `data/FirestoreMapping.kt`, `data/DebugSeed.kt`, `CityFixApplication.kt`, `viewmodel/TicketsViewModel(+Factory).kt`, `viewmodel/ReportViewModel(+Factory).kt`, `MainActivity.kt`, `firestore.rules` (`stats` block). Overview: "Firestore repository (F-011)", "Handover (F-011)", "ViewModel (D-003)", "Report form (F-006)".

**The device checks D1-D10 have not run yet** (no device attached). Build and lint pass (0 errors, 18 warnings, all from before F-011); the review read the code and approved with two should-fix items, both applied.

### Technical solutions
- **Offline-first writes.** A report is one batched write (`tickets`, `ticketLocations`, `publicTickets` for PUBLIC, and the `stats/summary` counters). The Firestore cache queues batches offline, while transactions fail without a network, so no transaction is used. `commit()` is not awaited: it completes only when the server acknowledges, so awaiting it offline would keep the form open. The form closes at once and the snapshot listener shows the ticket from the local cache; the batch reaches the server when the network returns. The new id comes from `collection("tickets").document()`, which is created locally.
- **Privacy by collection, not by field.** Firestore rules cannot hide fields in a document, so anonymous users read a separate collection, `publicTickets`. One function builds it: `toPublicTicketDocument()` calls `toAnonymousView()` first and copies 7 keys (category id and name, situation, status, coarse location about 1 km, thumbnails, created time). The anonymous code path never queries `tickets` or `ticketLocations`, so the privacy rule holds in the repository layer (`.claude/rules/kotlin.md`), not only in the UI.
- **Reads per audience.** Anonymous: active `publicTickets`. Citizen: their own full tickets (`participantIds` array-contains their id) combined with the public ones; the full copy wins. Operator: every ticket. The audience is the type of the sealed `User` (no role enum), with `null` for anonymous.
- **Dashboard without a server.** Anonymous users cannot read private tickets, but the dashboard counts them (decision 0005). So four counters live in `stats/summary` and change in the same atomic batch as the ticket, with `FieldValue.increment`. The increment is computed by the existing pure rule: `statsIncrements(before, after)` = `stats(after) - stats(before)` on one-ticket lists, so the definitions of "active" and "repair time" exist once. Increments work offline (they queue) and two devices writing at once do not overwrite each other.
- **Live data.** Snapshot listeners wrapped in `callbackFlow { ... awaitClose { registration.remove() } }`, collected in `viewModelScope`. Listeners survive rotation with the ViewModel and are removed when it is cleared, with no `onCleared` bookkeeping. A report on one device appears on another without a refresh, so `refresh()` is gone.
- **Listener errors.** On a listener error Firestore stops that listener; the error is logged and the flow is not closed, so the collector never throws. The flow sends nothing more and the ViewModel keeps the last list until the flow is collected again.
- **No composite indexes.** Every query is single-field (`whereIn`, `whereArrayContains`, `whereEqualTo`); the newest-first sort runs on the client with the existing `TicketsList` order.
- **Manual mapping as in class.** Documents are `hashMapOf`, read back with the typed getters and the model default (`getString(...) ?: ""`, as in the T8 demo). `toObject()` was not used: field names differ from the model, and `GeoPoint` is not `Coordinates`.
- **Clearing on user change** (review S1). `setUser` clears the list before starting the new listener, so after a sign-out (D-009) an anonymous user never sees the previous user's full tickets, even offline with an empty cache.

### Class material vs beyond class
- **From class:** one Firestore instance (`getInstance()` in class, `app.firestore` with the cache settings here), `set`/`get` with success and failure listeners, typed getters like `doc.getString`, and `db.batch()` + `commit()` (T8 demo "clear all", 09_Storage); `Flow`, `collect` and ViewModel-owned `MutableStateFlow` + `asStateFlow()` (08, P08).
- **Beyond class, accepted at Gate 1:** `addSnapshotListener` (live updates and offline cache on every change), `callbackFlow`/`awaitClose` (the listener as a `Flow`, removed when the collector is cancelled, which is the 08 slide "cancelling the collector aborts the emitter"), `combine` (the citizen's two lists), `FieldValue.increment` with `SetOptions.merge()` (counters).

### Options chosen and rejected (Gate 1, 2026-11-13)
- Q1: a `currentUser: User?` stand-in in `CityFixApplication` (edited by hand) and `TicketsViewModel.setUser` as D-009's seam; rejected a debug "View as" menu and waiting for D-009.
- Q2: `callbackFlow`; rejected callbacks returning a `Subscription` handle (closer to class, more bookkeeping).
- Q3: counters in `stats/summary`; rejected stats over `publicTickets` only (leaves out private tickets), an operator's device recomputing the counters (stale when no operator app runs), and `count()`/`average()` aggregations (server only, no offline).
- Q4: `LocalTicketsRepository` deleted; nothing else used it.
- Q5: debug-only seed of categories and tickets, so a resolved ticket exists for the repair time before D-013; removed before delivery.
- Q6: `ticketLocations` written now, (0, 0) until F-012.
- Q7: draft rules for `stats/summary`, not deployed.
- After Gate 1 the person switched the readers from casts to the typed getters (the class style), and removed a guard that skipped wrong-typed documents: such a field can only come from a hand edit in the console.

### Known limits
- Device checks D1-D10 not run yet.
- Thumbnails are empty in the anonymous list until F-013 uploads the media; other devices show the card placeholder.
- Test data with stand-in ids (`dev-citizen-1`, `citizen-1`, `citizen-2`) is in the shared test database.
- `DebugSeed` must be removed before delivery (D-015 checklist). Done in D-015 (2026-11-17, `6c0fbf0`).
- The `currentUser` seam is temporary; D-009 replaces it with the signed-in user.
- A write the server later refuses (only once rules are deployed, or after the test mode ends on 2026-12-10) is only logged, not shown.
- Counters drift if a future write path forgets `statsIncrements` (D-013 and F-014 must add it). Repair: recompute the four counters by hand from `tickets/` in the console.
- With an empty cache and no network at first install, the lists, dashboard and categories are empty.
- `toTicket()` does not read `resolvedAt` yet; the model uses the RESOLVED history entry (D-013/F-014 decide).

### Verification
- `./gradlew assembleDebug lint` after every slice and after the review fixes: 0 errors, 18 warnings (17 after F-010 plus one newer-version check, none in the F-011 files).
- Greps: no `LocalTicketsRepository`, `loadPublicTickets`, `loadStats` or `refresh()` left; `toAnonymousView()` is called in `data/` only by `toPublicTicketDocument()`.
- Review: the anonymous path reads only `publicTickets`, `stats/summary` and `categories`; the report batch matches decision 0010; the counters keep decision 0005's meaning; listeners are removed by `awaitClose`.

### Improvement opportunities
- Deploy and test the security rules (decision 0012 Q5), including the `stats/summary` block.
- A Cloud Function or scheduled job that recomputes `stats/summary` from the tickets and detects drift.
- Show the user when a queued offline write is later refused by the server.
- Real authentication instead of the stand-in (D-009). Done on 2026-11-14 (`46ee41f`).
- Thumbnails for the public list (F-013).
- A loading state, so the first frame before the first snapshot does not show "no tickets".
- Restart a listener after an error (for example when the user reopens the screen or the network returns) instead of waiting for a new collection.

## F-012: Location, anonymous gate, 10 m grouping and map
Decision: [0014](../decisions/0014-location-gate-grouping-map.md) (amends [0002](../decisions/0002-grouped-reports-single-ticket.md), [0010](../decisions/0010-firestore-schema.md), [0012](../decisions/0012-firebase-plan-notifications-close-maps.md) Q6). Code: `data/LocationRepository.kt`, `model/AnonymousArea.kt`, `utils/LocationPermissions.kt`, `viewmodel/LocationGateViewModel.kt`, `ui/screens/LocationGateScreen.kt`, `ui/components/TicketMap.kt`, `CoarseLocationMap.kt`, `LocationDeniedMessage.kt`, `data/FirestoreTicketsRepository.kt`, `data/FirestoreMapping.kt`, `viewmodel/ReportViewModel.kt`, `ui/screens/ReportScreen.kt`, `MainActivity.kt`. Overview: "Location, gate and grouping (F-012)", "Handover (F-012)".

**The device checks D1-D14 and D10b have not run yet** (no device or emulator), and the real Maps key is not pasted yet. Build passes; lint 0 errors, 21 warnings, none from F-012 code. The review approved with one should-fix (precise location in the form), applied with three nits.

### Technical solutions
- **One GPS fix.** `LocationRepository.currentLocation()` calls `FusedLocationProviderClient.getCurrentLocation(PRIORITY_HIGH_ACCURACY, null).await()` and returns `Coordinates?` (`null` = no permission or no fix). A report and the gate each need one position, so there are no continuous updates. The function checks the permission itself, so it cannot throw `SecurityException`.
- **Permission at app start, as in class.** `MainActivity` requests FINE and COARSE with `RequestMultiplePermissions` from `LaunchedEffect(Unit)`. A denial never crashes: the gate and the form show a message with "Try again" and "Open settings" (the only way back after "don't ask again").
- **The 10 km gate (brief I.1).** A pure rule in `model/` (`isInsideAnonymousArea`, Haversine to Pr. Republica, at most 10 000 m) and a small ViewModel with five states (checking, denied, no fix, outside, inside). `MainActivity` shows the gate instead of the app for anonymous users who are not inside. It is a client check only: security rules cannot know where the phone is (decision 0010). It runs at start and on "Try again".
- **Precise location for reports.** On Android 12+ the user can grant "Approximate"; that fix is about 2 km off and snapped to a grid, so two unrelated reports in the same cell would get the same point and group (found in review, S1). The form asks for precise location and has its own message; the gate accepts either, because 10 km is far larger than the error.
- **Grouping without geo queries (brief I.2 Obs.).** Firestore filters `ticketLocations` by category, situation and `active` (three equality filters, no composite index); the 10 m Haversine test (`findNearby`, D-002) runs in the app over those few documents, nearest wins. Offline the query answers from the local cache.
- **Why `ticketLocations` exists.** Citizens cannot read other people's tickets, and `publicTickets` has only coarse locations, so the grouping reads a small collection with category, situation, exact location and `active`. Signed-in citizens can therefore see where every ticket is, but not who reported it, its description or its media (decision 0010).
- **Joining a ticket.** A match first checks the reporter's own tickets (`participantIds`): if they already created or follow it, nothing is written ("You already follow this issue nearby"). Otherwise one batch writes their report to `tickets/{id}/reports/{uid}` and adds their uid with `FieldValue.arrayUnion` to `followerIds` and `participantIds`. The form shows one message per outcome (`ReportOutcome`: created, joined, already following).
- **Scope and the joiner's media (the authors' reading).** The brief groups by category/situation and 10 m only and says nothing about PUBLIC/PRIVATE. Grouping ignores scope, so a PRIVATE report can join a PUBLIC ticket, whose photos anonymous users see (brief I.1.C). So the joiner's description and photos stay in their own `reports/{uid}` document, readable only by them and operators; the shared ticket gains only the follower.
- **`active` follows `isActive`.** `ticketLocations.active` is true only for OPEN, INFO_REQUESTED and IN_PROGRESS, so a report next to a RESOLVED ticket creates a new one (decision 0005).
- **The anonymous map never shows the exact place.** `publicTickets` stores the location rounded to 2 decimals (decision 0006). The map shows a 750 m circle (the rounding can move a point by up to about 700 m) at zoom 14, with no marker and every gesture off, so it cannot be zoomed in. No coordinates as text.
- **Google Maps with the key in the manifest.** `maps-compose` 6.1.0, `GoogleMap` + `rememberCameraPositionState`, as in class. The key is the `com.google.android.geo.API_KEY` meta-data, so it is in the ZIP and the grader's build shows the map.

### Class material vs beyond class
- **As in Contacts_#3 / P10:** `play-services-location` 21.3.0 and the `FusedLocationProviderClient`; FINE + COARSE requested at start in `MainActivity` with `LaunchedEffect(Unit)` + `RequestMultiplePermissions`; `maps-compose` 6.1.0 with the key in the manifest; `GoogleMap` with a `Marker` at zoom 15 (our exact map, for operators); `checkSelfPermission` (class 02).
- **Beyond class:** one-shot `getCurrentLocation` with `await()` (the class uses continuous `requestLocationUpdates` in `ProximityService`); the 10 km gate; the coarse map with `MapUiSettings` gestures off and a `Circle` instead of a marker; "Open settings" after "don't ask again"; the grouping query plus the pure `findNearby`; `FieldValue.arrayUnion`.

### Options chosen and rejected
- Gate 1 (2026-11-14): Q1 centre and radius as code constants (rejected: reading `municipality/main`, which does not exist and adds a listener and offline cases); Q2 check at start and on Try again (rejected: on every resume, continuous updates); Q3 "Log in" hidden until D-009; Q4 fix taken when the form opens and on Refresh (rejected: at the Submit tap, which makes Submit wait for GPS); Q5 joiner media in `reports/{uid}`; Q6 `active = isActive`; Q7 reporter's own coordinates shown with 5 decimals; Q8 play-services-location 21.3.0 (class version, not 21.4.0); Q9 zoom 14, circle (600 m, later 750 m), no gestures.
- Q5 alternatives rejected: appending the joiner's media to the shared ticket (publishes private photos); appending except PRIVATE-into-PUBLIC (needs scope in `ticketLocations` and a second rule); grouping only same-scope reports (a condition the brief does not have, two tickets for one problem).
- Query alternatives rejected: geohash (a new field and hand code or a library) and a latitude/longitude bounding box (range filters on two fields need a composite index). With only active tickets of one category and situation the candidate list is small.
- Changed by the person after Gate 1, to follow the class practical: Google Maps Compose instead of osmdroid (no key, but archived in Nov 2024, a View library in `AndroidView`, and not what the class uses); the key in the manifest instead of `local.properties` (the ZIP excludes it); the permission requested at start instead of a rationale panel at the point of use. Also no try/catch around the grouping query (Firestore answers from the cache offline).
- Review: precise location required in the form (S1), the dead denied error removed (N2), circle 600 -> 750 m (N3), the gate's ineffective vertical centring removed (N4). N1 (relaunch on rotation) skipped: same as Contacts_#3.

### Known limits
- Device checks D1-D14 and D10b, and the "grant Approximate, open the form" check, not run yet (no device).
- The real Maps key is not pasted yet: until then the map shows grey tiles. The ZIP must contain the real key, and the key must not have an Android-app (package + SHA-1) restriction, because the grader's build is signed with a different debug key and the map would be blank. Anyone with the ZIP can use the key; an API restriction (Maps SDK for Android only) and the billing budget alert limit the cost.
- Offline, the grouping sees only tickets in the local cache: a report next to a ticket this device never loaded creates a second ticket. Two citizens reporting the same spot offline at the same time also make two tickets.
- Users who grant only approximate location cannot submit reports (they can still use the app as anonymous users).
- A rotation re-asks the permission, as in class; Android answers at once when already granted. A rotation while the first dialog is open can show "denied" for a moment.
- Without a try/catch, a failing grouping query (for example once rules are deployed) would crash the app instead of keeping the form open.
- Other followers do not see a joiner's description or photos; only the joiner and operators can.
- GPS accuracy (about 3-10 m) is of the same order as the 10 m rule, so two reports of the same problem can miss each other, or two close problems can group.

### Verification
- `./gradlew assembleDebug lint` after every slice and after the review fixes: 0 errors; 21 warnings at the end (the F-012 additions are the accepted newer-version warnings for play-services-location and maps-compose).
- The merged manifest was checked in slice 1, while osmdroid was in (it added no storage permissions).
- Review: gate maths and constants, anonymous privacy (map coarse, no coordinates text, `publicTickets` rounded), the grouping query and join batch, the outcome per case, denial without a crash, MVVM split, EN + PT strings.

### Improvement opportunities
- Re-check the gate on resume or with location updates, so leaving the area during use is noticed.
- A map of all public issues for anonymous users, with clustered coarse circles.
- Server-side grouping (a Cloud Function) to avoid duplicate tickets created offline.
- Read the centre and radius from `municipality/main` once several municipalities exist.
- Geohash queries if the number of tickets grows.
- Show the joiner's reports (description, media) to the other participants, with the joiner's consent for private ones.
- A rationale screen before the system dialog, and asking for location at the point of use.
- Ask once per process (`rememberSaveable` flag) so a rotation does not re-ask.

## F-013: Image compression, thumbnails and WorkManager uploads
Decision: [0017](../decisions/0017-media-preparation-and-uploads.md) (amends [0010](../decisions/0010-firestore-schema.md)). Code: `utils/ImageCompression.kt`, `data/AttachmentUploader.kt`, `data/UploadWorker.kt`, `data/FirestoreMapping.kt`, `data/FirestoreTicketsRepository.kt`, `CityFixApplication.kt`, `ui/components/AttachmentImage.kt`, `ui/screens/ShowScreen.kt`. Overview: "Media upload (F-013)", "Handover (F-013)".

**The device checks D1-D11 have not run yet.** The default Storage bucket must first be confirmed in test mode. Build and lint pass (0 errors, 22 warnings). The review approved with two should-fix items, both applied: one worker per document (S1) and stale KDocs (S2).

### Technical solutions
- **Compress on the device at submit (brief IV, V).** Before the ticket is saved, each photo is decoded, turned upright from its EXIF rotation, scaled to a long side of at most 1600 px and saved as JPEG quality 80, plus a 256 px thumbnail (JPEG 70). Re-encoding writes a new file with no EXIF at all, so the stored photo carries no GPS position, camera model or capture time. Videos get a thumbnail from one frame (`MediaMetadataRetriever`) and are uploaded as recorded, without compression.
- **Large photos without running out of memory.** The size is read first (`inJustDecodeBounds`), then the photo is decoded at a smaller size (`inSampleSize`, a power of 2) before the exact scaling. A 12 MP photo is never fully decoded.
- **Neutral names.** Every file is renamed to a random UUID, so neither the local name nor the Storage path (`tickets/{ticketId}/{uuid}.jpg`, `{uuid}_thumb.jpg`) holds a user id or the capture time.
- **Background upload with WorkManager, as in class 08.** After the Firestore batch, one `OneTimeWorkRequest` for the document (ticket or the joiner's report) is enqueued with a network constraint and exponential backoff from 30 s. The `CoroutineWorker` uploads each pending file and its thumbnail to Firebase Storage (`putFile`, `downloadUrl`), writes the download URLs and UPLOADED into the entry, and deletes the local copies. The work survives the app being closed and the phone restarting. A report made in airplane mode shows "Upload pending" and is uploaded when the network returns, without opening the app.
- **One worker per document, so writes never overlap.** Firestore cannot change one element of an array, so the worker reads the document, replaces the entry and writes the list back (a plain `get()` then `update()`, as in the class demo). The worker handles the entries one at a time, so its writes for a document never overlap.
- **Failure.** An exception makes the worker return `Result.retry()`; the next run continues with the entries still pending. After 5 attempts the remaining entries are marked FAILED and the detail says "Upload failed".
- **Each device uploads only its own files.** The worker uploads entries that are PENDING and whose file exists on this device, so an operator's device adding resolution proof (D-013) never touches the citizen's pending entries.
- **Public thumbnails.** For a PUBLIC ticket, each uploaded thumbnail URL is added to `publicTickets.thumbnails` with `FieldValue.arrayUnion` in the same batch as the entry update. Anonymous users load them with Coil (`coil-network-okhttp`, as in P09). A joiner's media stay in their own report and never reach `publicTickets` (decision 0014).
- **Upload state in the detail.** Each media tile shows "Upload pending" or "Upload failed" (EN and PT) until it is uploaded. Lists draw the thumbnail, so they load small images.

### Class material vs beyond class
- **As in class:** slides 08 (`CoroutineWorker`, `inputData`, `Result.retry()`, `Constraints.Builder()`, `WorkManager.getInstance(context).enqueue`); T8 AMOVStorageDemo (`bitmap.compress(JPEG, quality, stream)`, plain `get()` then `update()`); P10 Challenge I (`BitmapFactory.decodeFile`, `Bitmap.createScaledBitmap`, reading the app container from `applicationContext`); P09 (`coil-network-okhttp`).
- **Beyond class:** `inSampleSize`, the EXIF rotation, `MediaMetadataRetriever`, Firebase Storage `putFile` + `downloadUrl` (09_Storage only names Storage), exponential backoff, `FieldValue.arrayUnion`.

### Options chosen and rejected
- Gate 1 (2026-11-14): Q1 plain get-then-update, no transaction (the class approach; rejected: `runTransaction`, not taught); Q2 download URLs in the entries (rejected: Storage paths, which need a `getDownloadUrl()` call before drawing); Q3 delete local copies after upload; Q4 no video metadata removal (rejected: an MP4 remux with `MediaExtractor`/`MediaMuxer`, beyond class, not asked by the brief); Q5 `removeImageLocation` removed (re-encoding drops EXIF); Q6 no UPLOADING state; Q7 plain `enqueue` as on slides 08 (rejected: unique work); Q8 indicator in the detail tiles (the form's gallery shows only the draft); Q9 no video size check; Q10 `work-runtime-ktx` 2.12.0; Q11 drafted rules left unchanged; Q12 Storage bucket in test mode.
- Compress at submit, rejected: compress inside the worker or in a chain of workers (the ticket would first store the large original with its GPS EXIF, and the paths would change after the batch).
- Review S1 changed one worker per attachment to one worker per document: with 2-3 photos the per-attachment workers ran in parallel and one write overwrote the other's entry, leaving it pending forever. Also rejected for this: a transaction (not taught), and a map keyed by attachment id (a schema change for every reader).
- The task file asked for reuse of `removeImageLocation`, the UPLOADING state, unique work per attachment and the indicator in `AttachmentGallery`; Gate 1 (Q5-Q8) and review S1 replaced these.

### Known limits
- Device checks D1-D11 not run yet; typical sizes (camera original vs uploaded photo vs thumbnail) to fill in after D1.
- Videos are not compressed and keep their recording metadata, which may include the place where they were recorded. The brief asks to hide the author's data on screen (I.1.C), not to clean files; photos lose it anyway when re-encoded.
- WorkManager stops a worker after about 10 minutes, so a very large video on a slow connection may never finish uploading.
- Settings > Force stop pauses the uploads until the app is opened again (swiping it away or rebooting does not).
- Download URLs are readable by anyone who has the link, whatever the Storage rules say.
- Until the reporter's device uploads, other devices show a placeholder; if that device loses the files first, the entry stays pending.
- The earlier overwrite risk (two workers writing the same array) is fixed by the per-document worker.
- If the drafted rules were ever deployed: a joiner's `reports/{uid}` update would be refused (that rule allows no update), and a retry that re-uploads a file already in Storage would be refused (create-only). The rules are not deployed (test mode).

### Verification
- `./gradlew assembleDebug lint` after every slice and after the review fixes: 0 errors, 22 warnings (new and accepted: `UseKtx` on `createScaledBitmap`, kept because P10 uses it; `NewerVersionAvailable` for `coil-network-okhttp`, same version as the other Coil artifacts).
- Review: compression order and sizes, Storage paths without user ids, the document each upload writes, public thumbnails only for the creator's media of PUBLIC tickets, anonymous privacy, EN + PT strings.
- To run: D1 (sizes, upright), D2 (URLs and public thumbnail), D3 (airplane mode, 2 photos, app swiped away), D4 (big video, app killed while running), D5 (no EXIF, no uid in paths), D7 (joiner), D8 (FAILED), D9 (anonymous), D10 (portrait/landscape, PT), D11 (local copies deleted).

### Improvement opportunities
- A retry button for FAILED uploads.
- Upload progress in the UI (`setProgress`).
- A long-running foreground worker or resumable upload sessions for large videos.
- Video transcoding to a lower bitrate, which would also remove the recording metadata.
- Full media (not only thumbnails) for anonymous users.
- Deploy the Storage rules (`image/*` under 10 MB, `video/*` under 100 MB, no uploads to CLOSED tickets), fixing the two gaps above.
- Cloud Functions to make thumbnails on the server.
- Unique work per document, if the same device could ever enqueue twice for one document.

## D-010: Registration (citizen and operator)
Decision: [0016](../decisions/0016-registration.md). It also amends [0015](../decisions/0015-session-role-navigation-mfa.md) (session) and suggests an amendment to [0010](../decisions/0010-firestore-schema.md). Code: `utils/Validation.kt`, `data/UsersRepository.kt`, `data/FirestoreUsersRepository.kt`, `data/AuthErrors.kt`, `data/ProfilePhotoUploader.kt`, `data/ProfilePhotoWorker.kt`, `viewmodel/Register{UiState,ViewModel,ViewModelFactory}.kt`, `ui/screens/RegisterScreen.kt`, `MainActivity.kt`, `firestore.rules` (one draft line). Overview: "Registration (D-010)", "Handover (D-010)".

Brief: I.2.A (citizen: name, e-mail, phone, NIF, georeferencing, optional photo, NIF unique for the role, formats validated, "pendente até validação", impact detection choice); I.3.A (operator: e-mail in the council's domain, "fica logo ativo").

**Nothing has run on a device yet.** Build and lint pass (0 errors, 22 warnings, the baseline). Two review rounds: round 1 asked for changes (B1, S1-S3) and round 2 approved.

### Technical solutions
- **NIF check digit as a pure function.** 9 digits, the first not 0. `sum = d1*9 + d2*8 + ... + d8*2`, `r = sum % 11`, and the check digit is 0 when `r < 2`, else `11 - r`. It sits in `utils/Validation.kt` with no Android imports, so it can be explained and checked by hand at the defence (`123456789` is valid).
- **One file of input rules.** E-mail, operator domain, phone (2/9 prefix), mobile (9 prefix, for MFA), NIF, password and name live in one pure file. Login, Forgot password, Account setup and Register share it, so the same e-mail is never accepted on one screen and refused on another.
- **Real-time validation, P08 style.** One `@StringRes` error per field, recomputed as the user types once the field is not empty. Submit builds every error plus a summary and gates on a computed `isValid`. The ViewModel holds no text.
- **Registration = Auth user + one Firestore transaction, with compensation.** The flow:
  1. Create the Auth user.
  2. In `runTransaction`, read `nifs/{ROLE}_{nif}`. If it exists, abort. Otherwise write it and `users/{uid}`.
  3. If the transaction fails, delete the Auth user again. The delete runs under `withContext(NonCancellable)`, so leaving the screen cannot cut it off.
- **Why a transaction and not a batch.** Uniqueness needs a read before the write, and a batch cannot read. Registration needs the network anyway (sign-up is online), so the transaction's online-only nature costs nothing.
- **Keeping the form alive while Firebase signs the new user in.** `createUserWithEmailAndPassword` signs in at once, and the D-009 redirect would have moved to Account setup and destroyed the Register screen (and its running transaction). A `holdRedirect` flag in `rememberSaveable` stops the redirect while the form registers, and after a failure until the deleted account reaches the session. It survives rotation, and after process death it drops by itself. A good defence example of a race found in planning (R1) and tightened in review (B1, S1).
- **A started registration always finishes.** The ViewModel calls `register` under `NonCancellable`, Back is swallowed by a `BackHandler` and the Up arrow is hidden. Without this, Back during sign-up left an orphan account, and Back during the transaction could reserve a NIF for good (review B1).
- **`SavedStateHandle` for the form, never for passwords.** Role, name, e-mail, phone, NIF, the impact switch, both coordinates and the photo path survive process death; the passwords do not.
- **Profile photo through WorkManager after the commit**, reusing F-013's `ImageCompression` (1600 px, JPEG, no EXIF). The worker uses a fixed file name so a retry overwrites, and it treats permission errors as final instead of retrying them five times (review S3).

### Options chosen and why (Gate 1, 2026-11-14)
- **The operator's `users/{uid}` at registration, not after verification (Q1).**
  - Why: the NIF clash and the cleanup happen while the user is still on the form; there is one code path; and there is no local copy of a half-finished registration.
  - Safety: the session checks e-mail verification and MFA before the profile, and `isOperator()` in the draft rules still needs `email_verified`.
  - Cost: one line of the shared draft rules changed (not deployed).
- **Role-prefixed NIF ids** (`CITIZEN_<nif>`, `OPERATOR_<nif>`): a citizen and an operator may share one NIF, as "unique for the role" reads.
- **`photoPath` = the https download URL (Q4)**, as F-013 does for ticket media. Profile and validation screens load it with Coil directly.
- **Council domain read from `municipality/main` (Q9)**, not a constant. The council's data stays in one document, as the domain model's `Municipio.dominioEmail` says.
- **The impact switch is only stored** (`impactDetection`); F-015 (Francisco) uses it. (This line first said D-014; corrected by D-011.)
- **Phone: mobile or landline (Q6)**; **password: 8 characters with a letter and a digit, plus a confirmation (Q7)**; **NIF: any valid check digit (Q5)**, not only individuals' prefixes.
- **Rejected:** ViewModel orchestration of two repositories (Q13; the cleanup rule would depend on a screen's scope); an inline upload (blocks on a slow network, breaks "uploads through WorkManager"); a batch (no read, so no uniqueness); deferring the operator document (local store, two paths); the map pick (slice 6, skipped).

### Class material vs beyond class
- **As in class:** P08 form validation and `EditContactViewModel` (state, errors, `isValid`); P09 gallery picker and camera with FileProvider; P10 location with the permission at the point of use; slides 08 WorkManager (`CoroutineWorker`, network constraint, `Result.retry()`).
- **Beyond class:** Firestore transactions, `NonCancellable` cleanup after a cancelled coroutine, the redirect hold, the NIF mod-11 algorithm, `BackHandler`, `StorageException` / `FirebaseFirestoreException` error codes to tell final failures from transient ones.

### Limits to state honestly
- **Orphan Auth users (Q11).** If the network drops or the app dies between sign-up and the transaction, an account without a profile can remain. It sees Account setup, then Account problem, and the e-mail cannot be used again until the user is deleted in the console.
- **Registration needs a network.**
- **The rules are not deployed** (test mode). NIF uniqueness and the domain rule rely on the app's transaction and checks.
- **`nifs` documents reveal which uid owns a NIF** to any signed-in user.
- **The photo is best effort and can be dropped silently.** This happens after 5 failures, for an undecodable file, after a logout before the upload, or when the rules refuse it. A refused Firestore update after a successful upload leaves `photo.jpg` in Storage without `photoPath`. A transient "unauthenticated" answer is treated as final. A process death right after Submit can delete the queued file (logged). There is no retry UI.
- **The user cannot leave the screen while Firebase hangs** (the price of B1; the SDK timeouts bound it).
- **After a missing council domain, Submit needs a second tap** once the domain loads.
- **Testing operators needs a real inbox** in the configured `emailDomain`. For the defence the domain is a test domain, not the council's.
- **Pending citizens are not gated** by the 10 km rule (Q12), and anyone can now register.

### Verification
- No automated tests (project choice). `./gradlew assembleDebug` then `./gradlew lint` before every Kotlin commit and after both review rounds: 0 errors, 22 warnings, the baseline, none new. EN and PT key sets equal.
- Checked by reading: the NIF weights and the `r < 2` rule, the order create -> transaction -> delete, the photo enqueued only after the commit, no passwords saved, and nothing personal readable by anonymous users.
- **Not run yet (device, Diogo):**
  - invalid e-mail, phone or NIF blocking Submit with no Auth user created;
  - two citizens with one NIF (no Auth user left, no flash of Account setup), and a citizen and an operator sharing one NIF;
  - an operator outside the domain;
  - PENDING citizen and active operator homes after setup;
  - rotation everywhere, including during Submit;
  - Back right after Submit;
  - an operator Submit offline on a fresh install, then online;
  - "Use my location" and a denial;
  - photo from the gallery and the camera (`photoPath` URL, `photo.jpg` in Storage);
  - offline Submit;
  - PT strings, TalkBack and landscape.

  Never write test e-mails, NIFs of real people, phone numbers or UIDs here.

### Improvement opportunities
- A "delete my account and start again" action on the Account problem screen, for orphan accounts.
- Prefill the MFA phone in Account setup with the registration phone.
- Server-side registration (a Cloud Function) to make sign-up and the profile one atomic step, and to keep `nifs` unreadable by clients.
- Stricter NIF prefixes for individuals; a password strength meter.
- A retry button and a message for a dropped profile photo; delete the Storage object when the Firestore update is refused; retry a transient "unauthenticated" answer once.
- Re-submit automatically when the council domain finishes loading.
- A map pick to adjust the citizen's location (slice 6).
- **Housekeeping:** two decision records share the number 0015 (`0015-media-preparation-and-uploads.md`, `0015-session-role-navigation-mfa.md`). D-010 used 0016, and the authors should renumber one. **Resolved 2026-11-15 (F-014):** F-013's record was renamed to `0017-media-preparation-and-uploads.md`; D-009 keeps 0015.

## D-011: Profile editing
Decision: [0019](../decisions/0019-profile-editing.md). It amends [0015](../decisions/0015-session-role-navigation-mfa.md) (session e-mail sync, re-authentication) and [0016](../decisions/0016-registration.md) (shared fields, unique photo work), and suggests an amendment to [0010](../decisions/0010-firestore-schema.md). Code: `ui/components/AccountFields.kt`, `ui/screens/ProfileScreen.kt`, `ui/screens/ChangeCredentialScreen.kt`, `viewmodel/Profile*`, `viewmodel/CredentialChange*`, `viewmodel/SessionViewModel.kt`, `data/UsersRepository.kt`, `data/FirestoreUsersRepository.kt`, `data/ProfilePhotoUploader.kt`, `data/ProfilePhotoWorker.kt`, `data/AuthRepository.kt`, `data/FirebaseAuthRepository.kt`, `firestore.rules` (draft). Overview: "Profile (D-011)", "Handover (D-011)".

Brief: I.2.C (the citizen may not change name, NIF, georeferencing), I.3.C (the operator may not change name, NIF), I.2.A / I.2.F (impact detection is a profile choice).

### Technical solutions
- **Shared stateless field components.** The Register fields moved to `ui/components/AccountFields.kt` (`AccountTextField` with a new `readOnly` flag, `PasswordField`, `ImpactDetectionRow`, `ProfilePhotoPicker`). Register and Profile use the same components; the locked fields on Profile are the same text field with `readOnly = true`, so there is no keyboard and TalkBack says "read only". The move was a pure refactor (slice 1), checked by the reviewer as verbatim.
- **An allow-list in code.** `updateProfile` builds its map only from `phone` and, for a `Citizen`, `impactDetection`; the photo goes through the worker; the e-mail has its own path. Name, NIF and georeferencing have no write path at all.
- **Save without `await()`.** The Firestore `update()` is queued in the local cache and returns at once, also offline (as F-011's report batch). The `users/{uid}` snapshot listener that feeds the session sees the cached write immediately, so the impact setting changes in `Session.SignedIn.user` the moment Save is pressed, without a network.
- **Two listeners, one cache.** Profile listens to `users/{uid}` itself, next to the session's listener; both are served by the same cache, so this costs no extra server reads. The editable values are taken from the first snapshot only, so later snapshots (the photo worker, an operator's validation) never overwrite what the user types.
- **Re-authentication of an MFA account.** `reauthenticate(EmailAuthProvider.getCredential(email, password))` throws `FirebaseAuthMultiFactorException`, exactly as sign-in does. The same resolver flow (hint -> `PhoneAuthOptions` with the MFA session -> `verifyPhoneNumber` -> SMS code -> `PhoneMultiFactorGenerator.getAssertion` -> `resolver.resolveSignIn`) completes it, then `updatePassword` or `verifyBeforeUpdateEmail` runs at once. The `signIn` code was refactored to share one private `challengeOf(e)`.
- **The e-mail change and why the Firestore copy is synced from the session.** `verifyBeforeUpdateEmail` only sends a link; the Auth e-mail changes whenever the link is opened, maybe on another device. The session owns both values (the Auth account and the profile), so it compares them on the first profile snapshot after each sign-in or reload and writes `users/{uid}.email` when they differ. It runs whether or not Profile is reopened, which D-012 needs (the operator sees the current contact e-mail).
- **A sync that cannot loop.** The first version compared on every snapshot. The review found two loops: a stale second device writing the old e-mail back against the up-to-date one, and a refused write retrying without end (the listener reverts the cache, the next snapshot mismatches again). Checking only the first snapshot of each account branch removes both. A good defence example of a reactive feedback loop.
- **WorkManager unique work for the photo.** `enqueueUniqueWork("profile_photo_<uid>", REPLACE, ...)`: a newer photo cancels an older upload, so two quick saves cannot finish in the wrong order.
- **A cache key for an overwritten image.** The photo is overwritten at the same Storage path, so its download URL may not change, and Coil keys its memory and disk caches by URL. The worker also writes `photoUpdatedAt`, and Profile uses `"<url>#<photoUpdatedAt>"` as the Coil cache key; the "uploading" notice clears on that version, not on the URL.
- **Back during a running change.** A `changing` flag makes Back do nothing while `updatePassword` / `verifyBeforeUpdateEmail` runs, so the form cannot be resubmitted mid-change (review N1).

### Options chosen and why (Gate 1, 2026-11-15)
- **Profile phone separate from the SMS factor (Q1).** The phone is contact data from registration; the factor was enrolled in Account setup and D-010 never prefilled it, so the two may already differ. Re-enrolling on a phone change would add two SMS flows and an unenrolment, with a lockout risk, for something the brief does not ask. A hint under the field says so.
- **Password + SMS for every re-authentication (Q2):** Firebase requires it for an enrolled account.
- **Sync from the session plus a reload on Profile (Q3):** see above.
- **Draft rules allow-list (Q4):** the old deny-list forbade any `email` change, so the sync would be refused once the rules are deployed. Not deployed (Q5).
- **One Save for phone, photo and impact (Q6)**, rather than a switch that writes at once: one write path.
- **No stored-photo removal (Q7):** it would need a Storage delete.
- **Unique work with REPLACE (Q8).**
- **Pending citizens may edit and see the switch (Q9);** the detector runs only for active citizens.
- **Routes, not dialogs, for the e-mail and password changes (Q10):** the non-serialisable `MultiFactorResolver` and rotation are easier in a route-scoped ViewModel, as on Login. One ViewModel serves both routes (`CredentialChange { EMAIL, PASSWORD }`).
- **Single centred column in landscape (Q11), a Log out button on Profile (Q12), coordinates only for the georeferencing (Q13).**
- **Rejected:** an immediate impact switch; dialogs; syncing from the Profile ViewModel (would not run unless Profile is reopened); a `pendingEmail` field (a second source of truth); MFA re-enrolment; photo removal; a two-pane landscape.

### Class material vs beyond class
- **As in class:** P08 form state with `@StringRes` errors and a computed validity; P09 `PickVisualMedia` and `TakePicture` with the FileProvider; the WorkManager pattern of slides 08 / P10 (`CoroutineWorker`, network constraint); type-safe Navigation routes; `SavedStateHandle` through the factory's `CreationExtras`.
- **Beyond class (Firebase docs):** MFA re-authentication through `MultiFactorResolver`; `verifyBeforeUpdateEmail` and its out-of-band lifecycle; `enqueueUniqueWork` with `ExistingWorkPolicy.REPLACE`; Coil `memoryCacheKey` / `diskCacheKey`; Firestore rules `changedKeys().hasOnly(...)`.

### Limits to state at the defence
- **The rules are not deployed**, so a direct write to name or NIF succeeds against the test-mode database (R8). The allow-list can be shown only in the draft (Rules Playground; not run). The draft also does not type-check the allowed fields (review N5, R2-N2): suggested `is bool` / `is string` / `is int` / phone regex terms are in decision 0019.
- **The operator domain is enforced by the client only** for the Auth e-mail (R7): no Cloud Functions, and `isOperator()` checks only `email_verified`.
- **No MFA phone change, no photo removal, no account deletion.**
- **Device findings (2026-11-16):**
  - R1: the session survives the MFA re-authentication; Profile is still signed in after a password change.
  - R2: opening the e-mail link signs this device out at the next reload (reopening Profile), and the user lands on the anonymous dashboard **without a message**.
  - R3: e-mail enumeration protection hides "address already in use" at request time; the app says the link was sent, and the clash can only show when the link is opened.
  - R4: the `photoUpdatedAt` cache key works; the new photo shows after an overwrite.
  - R6 (revert link may unenrol MFA) comes from the Firebase docs and was not tried.
- **The e-mail check runs once per subscription (R2-N1).** After more than 5 s in the background the session flow resubscribes (`WhileSubscribed(5_000)`), so a second device with a stale account can write the old e-mail once per return to the foreground, until its token is refused. No loop; accepted. The `SessionViewModel` KDoc says this cannot happen and should be reworded.
- **`currentEmail` is read once** in the change screen (N6). **Two photos saved before the first upload ends** can briefly show the first one (R2-N3).

### Verification
- No automated tests (project choice). `./gradlew assembleDebug` then `./gradlew lint` before every Kotlin commit and after both review rounds: 0 errors, 22 warnings (the baseline ids, none new). EN and PT key sets equal.
- Two review rounds: round 1 approve with should-fix (S1 the sync loop, S2 the stale photo, S3 the device checks; N1-N6), round 2 approve (3 nits).
- **Device checks run (2026-11-16, emulator, test numbers, PT locale):** read-only fields; phone error and Save online; Save offline with a rotation before Save; landscape; camera and gallery photo with the new image shown after upload; wrong current password; the full password change with the SMS code and Back right after Confirm; the e-mail checks before Firebase; an address already in use (R3); the e-mail link and the sign-out (R2); login with the new e-mail and the new password.
- **Impact detection** was checked through the session/profile value only (switched off offline, still off after reopening Profile); F-015 does not exist yet, so the detector stopping is not shown.
- **Still open:** the console values (`phone`, `impactDetection`, `photoPath` + `photoUpdatedAt`, `users/{uid}.email` after the change); Register with a photo after the worker change; two devices on one account; the old password refused at login; the Rules Playground.

  Never write test e-mails, phone numbers or UIDs here.

### Improvement opportunities
- Deploy the rules with the allow-list, plus the type checks (N5, R2-N2).
- A Cloud Function (or a blocking function) to enforce the operator domain on the Auth e-mail.
- An MFA phone change with re-enrolment (enrol new, unenrol old, both after re-authentication).
- Photo removal with a Storage delete.
- A message after the involuntary sign-out caused by the e-mail link ("Your e-mail changed; sign in with the new address").
- A truly once-per-account e-mail check (compare the checked `AuthAccount` by identity in the ViewModel), and collect `auth.account` in the change screen (N6).
- A retry UI for a dropped profile photo; a two-pane landscape profile (P07).
- (Done in D-011 itself: the `photoUpdatedAt` cache key, first listed here as "if R4 shows a stale photo".)

## F-014: My tickets, message thread and reopen
Decision: [0018](../decisions/0018-citizen-tickets-messages-reopen.md). Overview: "Citizen tickets, messages and reopen (F-014)", "Handover (F-014)".

### Technical solutions
- **Messages** in the `tickets/{id}/messages` subcollection: live listener, served from the cache offline, writes queued with `document().set()` (brief IV).
- **One batch per status change** (reopen, automatic close, clarification): ticket and history, `publicTickets` mirror, `ticketLocations.active`, dashboard counters. Status and history change together (decision 0004); rules from D-008. No server code (decision 0012).
- **One audience function** (`detailAudience`) decides PUBLIC, REGISTERED or OPERATOR. Anonymous users never create the messages ViewModel, so they never read messages.
- **Close on read:** an expired RESOLVED ticket is written CLOSED when a participant opens it.

### Options chosen (Gate 1)
- Brief II.2 read as: state 2 "Pedido de Informações" also covers the citizen's clarification request, so it sets INFO_REQUESTED (Diogo agreed; amends decision 0011).
- Exact map, full history and the operator audience prepared, as the brief says (I.1.C, I.3.E, II.2, II.4).
- Rejected: a plain message for the clarification; a top-bar icon for My tickets; an expand toggle on My tickets.

### Known limits
- Device checks D1-D15 not run yet (test accounts pending).
- Concurrent offline changes: last write wins.
- All followers of a ticket share one thread.
- An operator's answer moves INFO_REQUESTED to IN_PROGRESS, even if the question was asked while OPEN (D-013 decides).

### Improvement opportunities
- Deploy security rules for messages and status writes.
- Transactions or a `currentStatus` precondition against concurrent changes.
- Unread counters and notifications (F-016).
- Paginate long threads.
- A detail listener on the single ticket instead of the list.
- `hasPendingWrites` to mark messages not yet sent.

## D-012: Approvals and categories
Decision: [0020](../decisions/0020-operator-approvals-and-categories.md). It amends [0015](../decisions/0015-session-role-navigation-mfa.md) (the placeholders become real screens; new `PendingCitizenRoute`) and suggests amendments to [0010](../decisions/0010-firestore-schema.md) and [0013](../decisions/0013-firestore-reads-and-dashboard-counters.md). Code: `data/UsersRepository.kt`, `data/FirestoreUsersRepository.kt`, `data/CategoriesRepository.kt`, `data/FirestoreCategoriesRepository.kt`, `viewmodel/PendingCitizens*`, `viewmodel/CitizenApproval*`, `viewmodel/Categories*`, `ui/screens/PendingCitizensScreen.kt`, `ui/screens/CitizenApprovalScreen.kt`, `ui/screens/CategoriesScreen.kt`, `MainActivity.kt`. Overview: "Approvals and categories (D-012)", "Handover (D-012)".

Brief: I.3.D (view a registration with name, georeferencing and NIF, and activate it; the wording is the task file's, the PDF was not reread, plan R6), I.3.E (create, activate, deactivate and edit categories; citizens report only on active ones).

### Technical solutions
- **Activation without a push or a restart.** The citizen's session already listens to `users/{uid}` (D-009). When the operator writes `registrationStatus: "ACTIVE"`, the citizen's snapshot turns the role from `PENDING_CITIZEN` into `CITIZEN`, the role redirect opens the dashboard and "+" appears. No code was added on the citizen's side: a good defence example of the reactive design (state flows from the database to the UI, not commands between devices).
- **The pending list as a live query.** One equality filter (`registrationStatus == "PENDING"`) and `snapshots()`, so a new registration or another operator's activation shows without a refresh. The oldest-first order is a client-side sort on `createdAt`; an `orderBy` on a second field would need a composite index, one filter uses the automatic one.
- **One write with exactly the rule's three keys.** `registrationStatus`, `validatedBy` (the operator's uid) and `validatedAt` (epoch ms), the shape the draft rule allows; idempotent, so two operators cannot corrupt it.
- **Category changes never rewrite tickets.** A ticket keeps its `categoryName` snapshot (decision 0003), so a rename or deactivation is one small write on `categories/{id}`; old tickets still show their name. The report form's `activeCategories()` listener follows the change live: a deactivated category leaves the open form, a renamed one shows its new name.
- **Admin writes work offline.** None of the writes is awaited: they go to the persistent cache, the listeners show them at once, and Firestore sends them when online (as F-011 and D-011).
- **Guarding a double tap in the ViewModel.** `activate()` needs the confirm dialog open and closes it before writing, so a fast double tap writes, shows the snackbar and goes back once (review S2). Validation and the editor dialog also live in the ViewModel, so rotation keeps the typed name.

### Options chosen and why (Gate 1, 2026-11-16)
- **Activate only, no Reject (Q2):** the brief asks only to activate; a REJECTED status needs a new enum value, a session state and a citizen screen. A wrong registration stays pending (console delete).
- **Detail with contact data and photo (Q3):** name, NIF and the map (brief) plus e-mail, phone and photo, so the operator can check a doubtful registration.
- **Oldest first (Q4)**, a queue; **a confirmation dialog (Q5)**, because there is no undo; **`validatedAt` as client epoch ms (Q6)**, like every other time in the app.
- **A separate `CategoriesRepository` (Q1):** Diogo's new files, so Francisco's `TicketsRepository` and report form are untouched; only a KDoc line in his mapping file changed.
- **Name rules (Q7):** trimmed, required, at most 40 characters, unique ignoring case (inactive ones included). **No delete (Q8):** deactivation is the safe delete.
- **No `SavedStateHandle` (Q9), no rules edit (Q10), title "Registration" (Q11), pending citizens still not gated (Q12).**
- **An extra detail route and two ViewModels** instead of the task's single `OperatorAdminViewModel`: the same list -> detail pattern as tickets, each listener scoped to its own screen.
- **Rejected:** Reject / REJECTED; one admin ViewModel; a list/detail two-pane with in-ViewModel selection (needs a `BackHandler`, Up behaves differently per orientation); writes in `TicketsRepository`; category delete; a server timestamp for `validatedAt`; a second `role` filter.

### Class material vs beyond class
- **As in class:** type-safe Navigation routes with an argument (`PendingCitizenRoute(citizenId)`, `toRoute`); ViewModel + `StateFlow` + a plain `ViewModelProvider.Factory` (Contacts_#3); `LazyColumn` with keys; `AlertDialog` and the FAB (P08); `@StringRes` validation errors (P08); the Maps Compose marker (Contacts_#3, F-012's `TicketMap`); `Collator` sorting and the two-pane landscape (P07).
- **Beyond class (Firebase docs):** Firestore `snapshots()` flows on a query, not only on a document; not-awaited writes queued in the persistent cache; `toggleable(role = Role.Switch)` with a state description for TalkBack.

### Limits to state at the defence
- **The rules are not deployed (R1).** Only the app (menu, `RequireAccess`, ViewModels inside the guard, one write path) stops a citizen from writing categories or activating themselves; against the test-mode database any client could. The draft rules show the server design, and even they check the validation's changed keys, not the values (review N3; suggested terms in decision 0020).
- **Duplicate names** are possible offline with an empty cache or with two operators at once: the check is client-side (R5).
- **No Reject and no category delete** (by choice).
- **A rename while a citizen's form is open** keeps the old name on that new ticket (R4, Francisco's form).
- **A hand-edited wrong-typed `users` document** makes the strict `toProfile()` throw, which empties the pending list and ends its listener until the screen is reopened (R2-N1, pre-existing mapping).
- **The confirm dialog stays open** if another operator activates the same citizen meanwhile (R2-N2, rare).

### Verification
- No automated tests (project choice). `./gradlew assembleDebug` then `./gradlew lint` after each of the five slices and both review rounds: 0 errors, 22 warnings (the baseline ids, none new). EN and PT key sets equal apart from the three non-translatable keys.
- Two review rounds: round 1 approve with should-fix (S1 dialog focus crash risk, S2 double tap, S3 switch description; N1-N6), all should-fixes and N1, N2, N5, N6 fixed; round 2 approve (two nits, R2-N1 and R2-N2, recorded as limits).
- **Device checks: none run yet.** Still open: the list and empty state; the detail and map; Activate and the console fields; the **two-device activation** (citizen waiting screen -> dashboard with "+", report submitted); Activate offline; double tap on Activate; category add / rename / toggle; validation errors; the report form after a deactivation and a rename, with old tickets keeping their name; rotation with each dialog open; landscape; citizens and anonymous users sent home.

  Never write test e-mails, phone numbers or UIDs here.

### Improvement opportunities
- Deploy the rules, with a tightened `categories` block (`keys().hasOnly(['name', 'active'])`, `name is string && name.size() > 0`, `active is bool`, `delete: if false`) and the validation value checks (N3).
- A Reject with a reason, shown to the citizen, and a notification on activation or rejection (F-016).
- Category delete with a "used by N tickets" check.
- A list/detail two-pane for approvals in landscape.
- Server-side unique category names (a `categoryNames/{lowercase}` index document written in a transaction, like `nifs`).
- Lenient `toProfile()` (`runCatching` per document) and closing the confirm dialog when the citizen stops being PENDING (R2-N1, R2-N2).
- Refresh the selected category's name in the report form at submit (R4).

## D-013: Operations panel and lifecycle
Decision: [0021](../decisions/0021-operator-panel-and-lifecycle.md). It amends [0011](../decisions/0011-ticket-lifecycle-rules.md) (D-013 is the operator caller, no INFO_REQUESTED -> OPEN) and [0020](../decisions/0020-operator-approvals-and-categories.md) (the panel reads `allCategories()`), and suggests amendments to [0017](../decisions/0017-media-preparation-and-uploads.md) and [0018](../decisions/0018-citizen-tickets-messages-reopen.md). Code: `model/TicketFilter.kt`, `model/TicketLifecycle.kt`, `viewmodel/OperatorPanel*`, `viewmodel/OperatorTicket*`, `ui/screens/OperatorPanelScreen.kt`, `ui/screens/OperatorTicketScreen.kt`, `ui/screens/ShowScreen.kt`, `data/TicketsRepository.kt`, `data/FirestoreTicketsRepository.kt`, `data/FirestoreMapping.kt`, `MainActivity.kt`. Overview: "Operations panel and lifecycle (D-013)", "Handover (D-013)".

Brief: I.3.E (operations panel with every report, coordinates, the author's contacts and the full history with media; status changes with timestamps, request for information, start of work, photo/video of the finished work), II.2-II.5 (transition order and the locked CLOSED state; section II not checked in the PDF).

### Technical solutions
- **One source of truth for tickets.** The panel and the detail read the shared `TicketsViewModel` list (already `allTickets()` for an operator), so no second listener is opened and the detail updates live when any device changes the status. The filter is a pure function on the **effective** status: an expired RESOLVED ticket counts as CLOSED with no write.
- **Every action goes through D-008's `transition`.** The UI offers only `allowedTransitions(operator, now)`, and the repository runs `transition` again before writing, so a stale screen or a forged call cannot store an invalid move. One batch per move keeps `tickets`, `publicTickets`, `ticketLocations` and `stats/summary` consistent (decision 0004).
- **Request information = status + message in one batch** (reusing `askForClarification`): the text is the history justification and appears in the citizen's thread as "Municipality".
- **Offline-safe resolve.** The proof is compressed locally (F-013), the batch waits in the Firestore cache, and WorkManager uploads with a network constraint, even after the app is closed. The proof is appended with `FieldValue.arrayUnion`, so the resolve does not overwrite another device's upload update of the same array. A dry run of the transition before any file work means a rejected move prepares nothing.
- **Privacy by construction, not only by hiding.** The author's and followers' contacts are read live from `users/{uid}` only inside the operator branch of the detail destination, in a ViewModel that is never created for a citizen; the history names map is empty for everyone else.
- **A D-014 hook in the pure model:** `needsOperatorGeofence(now)` (effectively IN_PROGRESS, real location), so the geofence needs no extra signal.

### Options chosen and why (Gate 1, 2026-11-16)
- **Methods in Francisco's repository (Q2)** instead of a second repository: `startWork` and `resolveTicket` over his `commitStatusChange`, so the batch and counter logic exist once. Small additive edits to his files, agreed through the handover.
- **No new route (Q1):** one detail route, one view per role (decision 0007's idea); tickets opened from the panel or the public list land on the same screen.
- **Panel on the shared list (Q5)**, pure filter in `model/`.
- **Start work with a confirmation and optional note (Q6)**, because there is no undo; **one ViewModel per screen (Q7)**, with its own thread.
- **Names in the history for operators only (Q3, Q13); "Resolution proof" label for everyone (Q4)** (not personal data).
- **No INFO_REQUESTED -> OPEN (Q8):** the brief's order; the operator answers in the thread.
- **No rules edit, no `resolvedAt` (Q9); 1 to 3 proof media, no video recording (Q11); optional map and joiners' reports only if time remained (Q12): not done.**
- **Rejected:** a separate operator repository with its own batch; a generic `changeStatus(to)` (could skip proof or the message); `addResolutionProof` as a separate write; a second `allTickets()` listener; a new detail route; rewriting the whole `attachments` array.

### Class material vs beyond class
- **As in class:** Navigation with an argument (`TicketDetailRoute(ticketId)`), ViewModel + `StateFlow` + a plain factory (Contacts_#3), `LazyColumn` with keys, `FilterChip` and the exposed dropdown, `AlertDialog`, `TakePicture` + `FileProvider` and `PickVisualMedia` (P09), Coil, the Maps Compose marker, WorkManager `CoroutineWorker` with constraints (slides 08).
- **Beyond class:** Firestore batches with `FieldValue.increment` and `FieldValue.arrayUnion`, snapshot listeners as flows, the composable slot in `ShowScreen`, `stateDescription` / `clearAndSetSemantics` so TalkBack reads the "Private" label once with the card.

### Limits to state at the defence
- **The rules are not deployed (R4):** only the app stops a citizen from changing status or seeing contacts. The draft rules would even refuse the resolve batch, because it writes no `resolvedAt` (F-014's reopen has the same problem); changing them needs Diogo's OK.
- **Concurrent status changes are last-write-wins** (no transaction: transactions fail offline). F-013's attachments race is unchanged.
- **Process death loses the proof draft**, and its local copies stay on the device (review N5); only rotation is covered. Files may also be orphaned if the operator leaves during preparation.
- **`onCleared` deletes the draft's files on the main thread** (review N2): `viewModelScope` is already cancelled there and there is no application scope; at most 3 small local files.
- No video recording, no upload retry or progress; `PlaceholderScreen` kept (still used by the pending-validation screen).
- **Not done:** the panel map (slice 8) and the joiners' reports in the operator detail (slice 9).

### Verification
- No automated tests (project choice). `./gradlew assembleDebug` then `./gradlew lint` after each of the seven slices and both review rounds: 0 errors, 22 warnings (baseline, none new); EN/PT key parity. Slice 1's filter checked by hand with a 3-ticket table (state log).
- Two review rounds: round 1 approve with should-fix (S1, the Resolve section outliving its move; N1-N6), S1 and N1, N3, N4, N6 fixed, N2 kept with a comment, N5 recorded as a limit; round 2 approve, no new findings.
- **Device checks: none run yet (D1-D11 open).** They need two devices (operator + citizen) and airplane mode for the offline resolve: panel and "Private"; filters, sort, rotation; detail contacts and "Contact not available"; no contacts or names for citizens and anonymous users; buttons per status; Request information live on the citizen's device; Start work; Resolve and the console fields; offline resolve with the app closed; closed tickets read-only; rotation and landscape.

  Never write test e-mails, phone numbers or UIDs here.

### Improvement opportunities
- Deploy rules matching these batches (accept a missing `resolvedAt`, or write it with `serverTimestamp()` on resolve).
- A transaction or a precondition on `currentStatus` when online, against concurrent changes.
- Ticket assignment to operators.
- Tap-to-call and e-mail intents on the contacts.
- The panel map with marker clustering (slice 8) and the joiners' reports (slice 9).
- Counts per status on the filter chips.
- Keep the proof draft in `SavedStateHandle` (paths only) so process death does not orphan files; an application scope for cleanup off the main thread.

## D-014: Operator 20 m geofence and proximity alert
Decision: [0022](../decisions/0022-operator-proximity-alert.md). It adds the optional `tickets/{id}.lastOperatorNearbyAt` to the Firestore layout ([0010](../decisions/0010-firestore-schema.md), suggested amendment line only) and keeps [0012](../decisions/0012-firebase-plan-notifications-close-maps.md)'s "no Cloud Functions". Code: `model/OperatorProximity.kt`, `model/Ticket.kt`, `model/TicketPrivacy.kt`, `viewmodel/OperatorProximityViewModel.kt` (+ factory), `MainActivity.kt`, `data/LocationRepository.kt`, `data/TicketsRepository.kt`, `data/FirestoreTicketsRepository.kt`, `data/FirestoreMapping.kt`. Overview: "Operator proximity (D-014)", "Handover (D-014)".

Brief (wording from the task file; the PDF was not reread in this task): I.3 Obs and II.3, an operator within 20 m of an IN_PROGRESS ticket alerts its authors; III, the proximity alert is a notification (delivered by F-016); V, background location among the permissions (**not covered**, see below).

### Technical solutions
- **Continuous location as a Flow, not a service.** `LocationRepository.locationUpdates()` wraps P10's `requestLocationUpdates` in a `callbackFlow`; `awaitClose` removes the callback, so cancelling the collector is the only "stop" needed. The collector runs under `repeatOnLifecycle(STARTED)`, so the GPS is on only while the operator has the app open.
- **Exact 20 m check in pure Kotlin.** Haversine on each fix against every ticket that `needsOperatorGeofence` (D-013's hook: effectively IN_PROGRESS, real location), inclusive at 20 m. Fixes less accurate than 25 m are dropped, so a poor fix cannot put the operator inside a 20 m circle.
- **Entry edge plus an hourly throttle.** Only an outside -> inside change records anything; the stored `lastOperatorNearbyAt` is a throttle shared by all operators (1 h), and an in-memory map covers the gap before the local cache echoes the write. The edge state lives in a ViewModel, so rotation is not a new entry.
- **One field as the seam to the notifications.** The citizens already listen to their full tickets (`participantTickets`), so a single field on the ticket reaches them with no new listener, query, index or read rule. F-016 turns a fresh, increased value into a local notification (five-point contract in the overview).
- **Offline by default.** The single-field `update` is not awaited; it lands in the Firestore cache at once (the operator's own list sees it, so no second write) and syncs later.

### Options compared (Gate 1 Q1): Geofencing API vs a foreground Flow
| | Geofencing API (`GeofencingClient` + `BroadcastReceiver`) | Foreground Flow + Haversine (chosen) |
| --- | --- | --- |
| Works with the app closed or the screen off | Yes (OS-managed, up to 100 geofences per app) | No |
| Accuracy at 20 m | Poor: Google recommends a radius of at least 100-150 m; a 20 m circle can miss or fire late | Exact distance on each fix, poor fixes filtered |
| ENTER latency | About 2-3 min (the OS batches fixes) | One interval (about 5 s) |
| Permission | `ACCESS_BACKGROUND_LOCATION` ("Allow all the time", Android 10+; its own settings screen on 11+) | FINE only, already requested at start |
| Lifecycle code | Register / remove on every list change and logout; lost on reboot and app update (BOOT receiver or re-register); receiver with `goAsync()` | One flow under `repeatOnLifecycle` |
| Emulator testing | Needs Play location services; routes work with a delay | `adb emu geo fix`, immediate |
| Brief V | Covered | Not covered |

- **Why the Flow for the deadline:** it meets the 20 m rule exactly, is quick to test with two emulators and short to explain at the defence.
- **The hybrid as an improvement:** geofences on top for the background, calling the same pure rule and the same repository write, so the stored throttle removes duplicates between the two paths.
- **Rejected:** a location foreground service (P10's `ProximityService`: persistent notification, `FOREGROUND_SERVICE_LOCATION`, overlaps F-016's channel work); Cloud Functions (decision 0012); a `proximityAlerts` collection (second listener, `whereIn` limit of 30, new rules, a query for the throttle); a server timestamp (null in the cache offline, breaks the throttle); storing the operator id or position; recording whenever inside (three alerts for three hours on site).

### Privacy by design
- Only a time is stored. No operator id, coordinates or distance leave the operator's device; logcat has no coordinates.
- Anonymous users never read `tickets`; `publicTickets` gets an explicit 7-key copy, so the field cannot reach it; `toAnonymousView()` clears it as well.
- A projected (anonymous) ticket can never be written: the rule requires a non-empty history (review S1).

### Throttle without a server
- The stored field (shared by every operator), the in-memory map and the entry edge.
- Limits: two operators entering at once while offline can both write (last write wins); device clock skew moves the hour. F-016's freshness and "last notified" checks are the second line.

### Class material vs beyond class
- **As in class:** P10's `FusedLocationProviderClient.requestLocationUpdates` and `LocationCallback`; `checkSelfPermission` (class 02); ViewModel + factory (Contacts_#3).
- **Beyond class:** `callbackFlow` + `awaitClose` instead of a service; `repeatOnLifecycle(STARTED)` inside a `LaunchedEffect` keyed on the operator and the permission; `LifecycleResumeEffect` to re-read the permission after Settings; the Haversine distance check and the accuracy filter; a single-field Firestore `update` as a cross-device signal.

### Limits to state at the defence
- **Foreground only.** Brief V's background location is not covered: nothing is recorded with the app closed, in the background or with the screen off (Gate 1 Q1/Q2).
- **An arrival made while the app was in the background is not counted** (review R2-N2): an entry counts only if the operator was seen outside the circle while the app was in the foreground. Pressing Home at ticket A, walking away, coming back to A and reopening the app gives no record. This is the price of not counting rotation or Home-and-return as new entries (review N1).
- **No delivery yet:** the citizen's notification is F-016's; without it, D-014 is verified in the console only.
- **The draft rules would refuse the write:** no rule allows an update of only `lastOperatorNearbyAt`. Test mode accepts it; a rule is needed before any deploy (needs Diogo's OK).
- Client-side throttle (offline operators, clock skew); an operator without precise location just gets no alert (log only, Q10); every signed-in operator is watched while the app is open.

### Verification
- No automated tests (project choice). `./gradlew assembleDebug` then `./gradlew lint` after each of the five slices and both review rounds: 0 errors, 22 warnings (baseline, none new). The pure rules checked by hand with a table (15 m in, 25 m out, exactly 20 m in, statuses, (0, 0), 59 / 60 min, future value, null, projected ticket) in the state log.
- Two review rounds: round 1 approve with should-fixes (S1 projected ticket, S2 missing 2 s minimum interval, S3 hand-offs; N1-N5), S1, S2 and N1-N5 fixed; round 2 approve, two docs nits (R2-N1 file ownership, R2-N2 the background-arrival limit), handled in the docs.
- **Device checks: none run yet (D1-D11 open).** Two emulators and `adb emu geo fix`: entry from 100 m to 15 m, no record for other statuses or at 25 m, throttle within the hour, a second operator, staying inside (debug-shortened interval), Home / return / Log out, citizens and anonymous, permission states, offline, no coordinates anywhere, and the notification (F-016).

  Never write test e-mails, phone numbers or UIDs here.

### Improvement opportunities
- Background coverage: the Geofencing API on top (hybrid, with an operator-only rationale for `ACCESS_BACKGROUND_LOCATION`), or a location foreground service while the operator is "on duty".
- A server-side throttle: deployed rules (operator only, IN_PROGRESS, `hasOnly(['lastOperatorNearbyAt'])`, at least 1 h after the old value) or a Cloud Function.
- A per-operator "on duty" switch, and a note on the panel when precise location is missing.
- Adaptive update intervals (slower when far from every active ticket) to save battery.
- Count a background arrival by forgetting `inside` after a long time in the background.
- Done by F-022: the watch runs in a location foreground service with background location.

## D-015: Final pass and delivery
**Code done (2026-11-20): slices 1-6 coded and reviewed (two rounds).** Still open: running the ZIP script, the console steps, the test accounts, the ZIP from `main` and the upload (see "To do"). For F-017: Diogo hands these notes to Francisco. Decision: [0025](../decisions/0025-delivery-package.md) (delivery package). Overview: "Top bar (D-005)", "App shell (D-006)" (report FAB), "Profile (D-011)" (phone on read), "Setup", "Delivery (D-015)", and the D-015 notes in "Code structure", "Firebase (F-010)" and the F-011/F-012/F-015 handovers.

Brief: V (languages, orientation, dynamic bars and FABs, accessibility, MVVM) and VI (ZIP name and contents).

### Technical solutions
- **Clean-up before delivery (slices 1-3, done).**
  - The debug seed (`DebugSeed.kt`, F-011) and the phase-1 sample categories (`SampleCategories.kt`, F-006) were deleted, with the `onCreate()` override in `CityFixApplication`, which is now only the lazy container (`6c0fbf0`). Categories now come only from the operator's Categories screen (D-012).
  - `SampleTickets` stays, as preview data only, for one privacy preview (Gate 1 Q2).
  - The unused `placeholder_message` string was removed from both locales, and `PlaceholderScreen` now requires its `messageRes` (`a3fe6fb`), so there is no silent default text.
  - Lint warnings cleared: the redundant `android:label` on `<activity>` (the `<application>` label still names the app in the launcher) and the template `res/values/colors.xml`, which nothing referenced (`ade184a`).
  - Stale comments that still mentioned the seed were reworded (review S1, comment only).
- **Report entry as a dynamic FAB (slice 5, `953f577`; Gate 1 Q5, brief V "botões flutuantes dinâmicos").** The top-bar "+" was replaced by an `ExtendedFloatingActionButton` "Report an issue" / "Reportar ocorrência" in `MainActivity`'s `Scaffold`. It shows only for an active citizen (`session.canReport`), only on Dashboard, List and My tickets, and never behind the anonymous 10 km gate. So the app has two dynamic FABs: the citizen's report FAB and the operator's "Add category" on Categories (shown once the list has loaded). The text label is the accessible name, so the icon has no description; the FAB is 56 dp high. `List` and `My tickets` got 88 dp of bottom padding (review S1, `1946dcc`) so the FAB never covers the last card. The top bar keeps the other dynamic parts: title per destination, back arrow from the back stack, the menu by role, Log in / Log out.
- **Phone shown once with +351 (slice 4, `f6a4cb1`).** Accounts created by hand in the Firebase console stored `phone` as "+351 9...", while the app writes 9 digits and the screens add "+351", so the operator's Contacts showed "+351 +351...". Fix on read: the pure `nationalPhoneDigits` (`utils/Validation.kt`) drops whitespace and a leading `+351` / `00351`, applied in `FirestoreUsersRepository.toProfile()`, the only mapper from `users/{uid}`, so every screen gets 9 digits. Writes are unchanged (the phone is written only when the user edits it). Side effect: console-made accounts can now save their profile (before, their phone failed validation).
- **Lint.** 0 errors, 17 warnings (2026-11-20), all left on purpose:
  - 8 `GradleDependency` and 7 `NewerVersionAvailable`: newer library versions exist (3 of them published after 2026-11-17). No upgrades right before delivery; some newer versions also need a newer Kotlin or Compose (overview "App shell", Dependencies).
  - 1 `AndroidGradlePluginVersion`: same reason.
  - 1 `UseKtx` in `ImageCompression.kt` (Francisco's file): `Bitmap.createScaledBitmap` kept as in P10.
  - Slices 1-3 had cleared `RedundantLabel` and the 7 `UnusedResources`; slice 5 removed `cd_report_ticket` instead of leaving a new `UnusedResources`.
- **Languages (re-checked after the F-015/F-016 merge, slice 4).** English default (`values/`), Portuguese from the device setting (`values-pt/`). Key diff: 341 translatable keys in each locale, none missing or extra (EN has 344 with the 3 `translatable="false"`: `app_name` and the two phone-prefix strings, Gate 1 Q3); 4 plurals in both. No hard-coded UI text outside previews (greps for `Text("`, literal `contentDescription`, notification titles/texts/channels, `Toast`): F-016's channels and notification texts use resources. Category names are data typed by operators, so they are not translated.
- **Accessibility (code check, slice 4).** 6 `contentDescription = null` uses, each decorative or labelled by visible text (Categories FAB label, sort chip semantics, attachment label, preview map icon, play overlay, `PlaceholderScreen` icon). No `remember { mutableStateOf }` holds typed input (the two in `MainActivity` are permission flags). Top-bar buttons are 48 dp.
- **Orientation.** Form and dialog state lives in ViewModels (`StateFlow<UiState>`, some with `SavedStateHandle`); small UI flags and the camera temp path use `rememberSaveable`, so rotation keeps typed data.

### Device checks (emulator, 2026-11-17 and 2026-11-20)
Emulator API 37, 1080x2400 at 420 dpi, system language pt-PT; debug APK; driven with adb, logins and console reads by Diogo.
- **Clean-up:** cold start with no seed in Logcat and no crash; "Registo pendente" texts unchanged; launcher label "CityFix" in every locale.
- **Rotation** (portrait -> landscape -> portrait): pending screen, Dashboard, list, ticket detail and the 10 km gate keep their content; a half-typed Profile phone ("9123456") survived two rotations, and leaving without Save changed nothing.
- **PT / EN** (per-app locale switched to `en-US`): every visible string on those screens is translated; top-bar icons have descriptions; top-bar buttons are 48 dp.
- **Anonymous:** at Pr. Republica the dashboard, list and detail work and the detail shows only the ~1 km area (no author, no exact point); in Lisbon the gate blocks with "Try again" / "Log in"; back in Coimbra "Try again" opens the dashboard.
- **Operator geofence (D-014):** 100 m and 25 m write nothing; 15 m sets `lastOperatorNearbyAt` once and nothing else; leaving and entering again within the hour keeps the value; no coordinates in Logcat.
- **Phone fix:** the operator's Contacts for a console-made citizen show "+351 917234568", one prefix.
- **Not run:** TalkBack spoken order, Accessibility Scanner contrast, the registered citizen's screens (report, My tickets), operator screens other than the panel, the report FAB per role and in landscape, notifications on this branch.

### Findings (not fixed in code)
- **Category names are English seed data.** Removing the seed code deleted no Firestore data, so the categories written by the old seed ("Public lighting", "Pavement", ...) still show in English with the app in PT. Fix in the console (Q4 clean-up, Diogo): rename them in Portuguese, keeping the document id `pavement` (F-015's impact prefill looks it up). Improvement: per-locale category names.
- **The map's TalkBack label comes from the Maps SDK** ("Mapa do Google Maps", in the system language), not from our strings, so it does not follow the per-app locale. Improvement: wrap the map with our own `semantics` description.
- The map tiles load with this machine's debug key, which does not prove that the key has no SHA-1 restriction (R4).

### Test accounts (report section 9)
Passwords, e-mails, MFA test numbers and codes go **only in the PDF**, never in git (Gate 1 Q9). Table for the PDF (Francisco fills the values from the authors' private note):

| Role | E-mail | Password | MFA test number | Fixed code | What to try |
| --- | --- | --- | --- | --- | --- |
| Active citizen | (PDF only) | (PDF only) | (PDF only) | (PDF only) | report with photo, My tickets, thread, reopen, Profile, impact suggestion |
| Pending citizen | (PDF only) | (PDF only) | (PDF only) | (PDF only) | waiting screen, Profile; activated by the operator |
| Operator | (PDF only) | (PDF only) | (PDF only) | (PDF only) | panel, Request information, Start work, Resolve with proof, Approvals, Categories, 20 m alert |

Note for the grader: the build is signed with their own debug key, whose SHA-1 is not registered in Firebase, so SMS MFA may show a reCAPTCHA browser step (R3). It normally works on an emulator with Chrome.

### How to run (for the PDF)
- Unzip, open `CityFix/` in Android Studio (it writes `local.properties` with `sdk.dir`), let Gradle sync, run on a device or emulator with Google Play services.
- `app/google-services.json` is included in the ZIP; the Maps key is in the manifest.
- On macOS/Linux, `chmod +x gradlew` before running `./gradlew` from a shell (the ZIP made on Windows has no execute bit; review N4). Android Studio is not affected.
- The database is in test mode until 2026-12-10 (2027-03-01 if the Q7 console change is made before the upload; say which).
- For the anonymous mode, the emulator location must be within 10 km of Pr. Republica, Coimbra.

### Delivery package
- `scripts/make-delivery-zip.ps1` (slice 6, `d8e5881`, `2e7862a`): `git archive` of `main`, adds `google-services.json` and the PDF from parameters, checks the excluded folders twice, zips with `tar.exe` (forward slashes), deletes the staging copy, and builds the unzipped copy with `gradlew.bat assembleDebug`. With `-Ref main` it refuses a local `main` that differs from `origin/main`. Option `-ExcludeAgentFiles` leaves out `.claude/`, `CLAUDE.md` and `docs/work/` (Q8, open). Rejected: zipping the working folder, committing `google-services.json`, `Compress-Archive` (backslash separators on PS 5.1). Details in decision 0025.

### To do (before 2026-11-22 23:59)
- Run the ZIP script once (`-Ref dev/Diogo -SkipBuild`, then without `-SkipBuild`); if the execution policy blocks it, write "manual ZIP" in the checklist and follow slice 6 steps 2-7 by hand.
- Q8 with Francisco: keep or leave out `.claude/`, `CLAUDE.md`, `docs/work/`.
- Q4 and Q7 console changes (Diogo): delete the stand-in users, rename the seed categories (keep id `pavement`); move the test-mode date of Firestore and Storage to 2027-03-01.
- R4: check in the Google Cloud console that the Maps key has no Android-app restriction.
- Build the ZIP from `main` after the final merges, record the full SHA, build the unzipped copy, install it, log in, see the map.
- Upload to Nonio and record the time in `state.md`.

### Limits to state at the defence
- The security rules are drafts and not deployed: privacy, MFA and role guards are enforced in the app only.
- Notifications are local, with no server; the operator geofence works in the foreground only.
- The stand-in test users (`dev-citizen-1`, `citizen-1`, `citizen-2`) are still in the test database until Diogo deletes them in the console (Q4). The seeded sample tickets are kept on purpose, so the dashboard counters match the list (deleting tickets does not update `stats/summary`).
- A profile phone stored as "+351..." by the console stays that way in Firestore until the user edits it; only the display is normalised.
- The device sweep did not cover TalkBack order, contrast or every screen of every role (list above).

### Verification
- No automated tests (project choice). `./gradlew assembleDebug` and `./gradlew lint` after every slice and review fix: pass, 0 errors (14 warnings after slices 1-3, 17 after slice 4 because of 3 new version notices). `app/src` has no reference to `DebugSeed`, `SampleCategories`, `placeholder_message`, `cd_report_ticket` or the template colours.
- Two review rounds, both "approve with should-fix": round 1 (S1 stale KDoc, S2 overview lines) and round 2 (S1 FAB over the last card, fixed; S2 the script has never run, open; S3 `main` vs `origin/main`, fixed). The `-ExcludeAgentFiles` pathspec was checked with `git archive` (298 files, none excluded wrongly).
- The script itself: PowerShell parser only (0 errors); not executed (execution policy `AllSigned`).

### Improvement opportunities
- Automated UI tests for rotation and TalkBack.
- A `WindowSizeClass`-based layout for tablets.
- Localised category names (per-locale fields on `categories`).
- Our own accessible description on the map instead of the Maps SDK's label.
- Normalise the stored phone with a one-off write (or on the next Save) instead of only on read.
- Deployed security rules.
- A CI job that builds the ZIP and checks it.

## F-015: Impact detection
Decision: [0023](../decisions/0023-impact-detection.md). Overview: "Impact detection (F-015)", "Handover (F-015)". Brief I.2.F.

### Technical solutions
- **As in class:** the slides' `PotholeDetector` (`TYPE_ACCELEROMETER`, `SENSOR_DELAY_GAME`, limits 6..14 m/s²) and the `Sensors.kt` demo lifecycle (`DisposableEffect` + `LifecycleEventObserver`: register on ON_START, unregister on ON_STOP).
- **Total acceleration** `sqrt(x² + y² + z²)` outside 6..14 m/s² = impact; 30 s cooldown so one bump gives one suggestion.
- **MVVM:** sensor in `utils/ImpactDetector`, owned by `ImpactViewModel`; the composable only says when to start and stop.
- **Suggestion, never a silent report:** long snackbar "Report" -> `ReportTicketRoute(fromImpact = true)` with Damage + Pavement + current location.
- **No runtime permission:** the accelerometer needs none.

### Why not the slide's Z axis alone
- An upright phone reads Z near 0, below 6, so `z < 6` would fire all the time (seen on the emulator).
- Cost: total acceleration also reacts to non-vertical jolts (phone put down hard), a departure from the brief's "eixo vertical" (decision 0023).

### Verification
- Emulator checks in `docs/work/F-015-impact-detection/state.md` (jolt `adb emu sensor set acceleration 0:0:20`, back to rest `0:9.8:0`; cooldown, background stop, landscape, prefill).

### Improvement opportunities
- Foreground service for detection with the app closed (P10 `ProximityService` pattern).
- Only the vertical part of the jolt (project on gravity).
- Keep the suggestion across a screen rotation (the snackbar scope belongs to the composition).
- Movement check with GPS speed, so a dropped phone at rest does not count.
- Threshold set in the profile.
- Attach the detected location and time to the ticket.
- Done by F-022: foreground service, vertical projection, GPS speed check, profile threshold.

## F-016: Notifications
Decision: [0024](../decisions/0024-notifications.md). Overview: "Notifications (F-016)", "Handover (F-016)". Brief III, V, II.2-II.4.

### Technical solutions
- **No server (decision 0012):** the app compares the tickets it reads with one stored "last checked" time per user and shows local notifications.
- **App open:** a Firestore listener that only accepts server-confirmed lists (`isFromCache` skipped), so cached data never advances the last-check time; the worker reads with `Source.SERVER`.
- **App closed:** a 15 min periodic `CoroutineWorker` (citizens and operators), network constraint.
- **Pure rule** `Ticket.eventsFor(...)` in `model/`: status, message, operator nearby, 24 h warning, Encerrado; never one's own action.
- **Messages seen by the ticket listener:** `lastMessageAt` / `lastMessageAuthorId` on the ticket, written in the same batch as the message.
- **24 h warning and Encerrado:** time checks on each run, no scheduled job per ticket.
- **FCM:** F-016 had the P11 service for console test messages; F-019 removed it (decision 0027).
- **Privacy:** title = category; no coordinates or operator data; Log out clears the tray.

### Class material vs beyond class
- Class: channels and the `POST_NOTIFICATIONS` request (P10, P11); WorkManager slides 10/11; SharedPreferences slides 09.
- Beyond class: `singleTop` + `onNewIntent` for the tap; unique periodic work.

### Options chosen and rejected (Gate 1, 2026-11-20)
- Client-only delivery, not a Cloud Function (class shows FCM reception only).
- Worker for operators too (Q2 changed at Gate 1).
- One time per user, not per ticket; time check, not a one-time job per ticket.
- Permission at session start, after the location prompt (Android shows one request at a time).

### Limits to state at the defence
- No real push: the app or the worker must run; Doze can delay the worker beyond 15 min.
- Device clocks differ: an event dated before the last check is missed.
- Closed-app alerts up to ~15 min late, including the 24 h warning.
- Draft rules would refuse the new fields (test mode accepts).

### Verification
- Device checks listed in `docs/work/F-016-notifications/state.md` (two emulators, App Inspection "run now", shortened `CLOSING_WARNING_MS`).

### Improvement opportunities
- Cloud Function on status change or new message -> FCM (real push, no delay; needs FCM back, see F-019).
- Per-user notification settings (per channel or per ticket).
- Message preview in the notification.
- Exact 24 h alarm (`AlarmManager` or a one-time work per ticket).
- Rationale before the permission request.

## F-019: FCM removed
Decision: [0027](../decisions/0027-local-notifications-only.md). Overview: "FCM removed (F-019)". Brief III.

### Technical solutions
- Notifications are created on the device from Firestore data (listener while open, WorkManager worker while closed), as P10 creates them from GPS. No push server, no FCM.
- Removed the FCM receiver, the token storage, the `fcmTokens` field and the dependency.

### Limits to state at the defence
- **Deviation from the brief:** brief III asks for local and remote (push) notifications; only local ones remain, by the authors' choice.

### Improvement opportunities
- FCM sent by a Cloud Function on status change or new message (real push, no 15 min delay).

## F-018: Map pinch zoom, phone at sign-up, full-size media

### Technical solutions
- Ticket map inside a scrolling page: the map reports finger down/up (`pointerInput`, Initial pass) and the page turns its `verticalScroll` off meanwhile, so pinch zoom works.
- Phone verified by SMS once at setup and **linked** to the account (`linkWithCredential`), not enrolled as MFA ([decision 0026](../decisions/0026-phone-verified-at-signup.md)).
- Tap a photo/video tile: full-screen `Dialog`; photo with Coil and pinch zoom, video with `VideoView` + `MediaController`.

### Class material vs beyond class
- Class: Compose `Dialog`, Coil, `AndroidView`, Firebase Auth.
- Beyond class: `VideoView`/`MediaController` (no video playback in class), Initial-pass pointer input, phone linking.

### Limits to state at the defence
- **Brief deviation:** I.2.B/II.2.B ask for MFA at login; login is now e-mail/password only. Chosen for usability (fewer SMS); the phone is still proven by SMS at registration.
- One account per phone number; accounts from before F-018 still get the SMS at login.

### Improvement opportunities
- Optional MFA the user can turn on in Profile (enrol the linked phone as a factor).
- Media3/ExoPlayer for streaming and better controls; swipe between attachments in the viewer.

## F-020: Sign-up "Send Code" crash (fix)
Decision: [0026](../decisions/0026-phone-verified-at-signup.md) (amendment line). Overview: "Sign-up phone code (F-018, fixed by F-020)".

### Technical solutions
- `requireSmsValidation(true)` is valid only with a multi-factor session: kept for the MFA code (sign-in and re-auth, the code is always typed), dropped for the sign-up send.
- Sign-up accepts `onVerificationCompleted` (instant verification / auto-retrieval) and links the phone at once, instead of waiting 60 s for an `onCodeSent` that never comes.

### Lessons
- F-018 changed one caller of a shared helper (`requestCode`) without rechecking the options the helper set for every caller; build and lint pass because `build()` checks the options only at run time.

## D-019: Coordinates and attachment authors in the operator detail (bug fix)
Decision: amendment to [0007](../decisions/0007-ticket-detail-by-audience.md) (no new record). Code: `ui/screens/ShowScreen.kt`, `ui/screens/OperatorTicketScreen.kt`, strings EN/PT (commit `20a82b0`). Overview: "Operations panel and lifecycle (D-013)", bullet "Coordinates and attachment authors (D-019)".

Brief: I.3.E, the operator sees the ticket "com coordenadas, contactos do autor e histórico completo com fotos e vídeos" and who added them. Found in Diogo's manual test of I.3.E (2026-11-21): the exact location was only a map, and the photo/video tiles never said who attached them. Not a regression; never implemented.

### Technical solutions
- **Exact location as map plus text.** Under the exact map, the operator view prints "Coordinates: 40.21000, -8.41900"; (0, 0) keeps "Map not available" and no line.
- **Who added each photo/video, no new data stored.** `Attachment.uploadedById` was already written to Firestore (report media = creator, joiner media = follower, proof = resolving operator). D-019 adds those ids to the profiles the operator detail already watches (D-013's `watchPeople`) and passes the same id -> name map to the tiles, which show "Added by <name>".
- **Formatting:** `Locale.ROOT` and 5 decimals (about 1 m), so the PT decimal comma does not clash with the comma between latitude and longitude; TalkBack gets "Latitude x, longitude y" instead of the raw string.
- **Accessibility and layout:** the uploader line is merged into the tile's TalkBack item; `maxLines = 2` + ellipsis keeps the 120dp tile bounded; both orientations pass the names (landscape preview added).

### Privacy by audience
- Coordinates text and names appear only in the OPERATOR view (`showCoordinates = audience == OPERATOR`; only `OperatorTicketScreen` passes the names map). Registered citizens and anonymous users see exactly what they saw before.
- Defence in depth: the anonymous projection already blanks `uploadedById` and rounds the location upstream (`TicketPrivacy.kt`), so even a wrong audience could not show them. A map miss shows nothing, never an id.

### Options chosen and rejected (Gate 1, 2026-11-21)
- Reuse `responsibleNames` (KDoc widened) instead of a second `uploaderNames` parameter with the same data (Q1).
- Registered citizen unchanged (Q2): the brief asks coordinates only for the operator.
- Dot decimal in both languages (Q3); new string "Added by" / "Adicionado por" instead of reusing "By" (Q4).

### Limits to state at the defence
- A profile with a blank name renders "Added by " (same as "By " in the history); a one-line fix where the map is built.
- One map has two names in `ShowScreen.kt` (`responsibleNames` publicly, `uploaderNames` in `MediaSection`), accepted at Gate 1.
- Manual acceptance checks (TalkBack in PT, rotation, the three audiences) were still open at review.

### Verification
- `./gradlew assembleDebug` and `./gradlew lint`: pass, 0 errors (18 existing warnings, none in touched files). Review: 0 blocking. Device checks listed in `docs/work/D-019-operator-detail-coordinates/00-plan.md`.

### Improvement opportunities
- Copy the coordinates to the clipboard, or open them in an external maps app (`geo:` intent).
- Uploader name and upload date in the full-screen attachment viewer.
- Rename `responsibleNames` to `peopleNames` in a coordinated refactor with Francisco.

## D-020: Bottom bar keeps the wrong tab and cannot leave a detail (bug fix)
Decision: none (local navigation fix inside D-016's bottom bar). Code: `MainActivity.kt` (commits `31d5123`, `a910361`, `42f55e8`) and the tab root screens (`e83bf81`: Dashboard, Active tickets, Profile, Operator panel, Pending citizens, Categories get a scroll-state parameter). Overview: "App shell (D-006)", bullet "Bottom bar and per-tab back stacks".

Found in Diogo's manual test of I.3.E (2026-11-21): operator Panel -> ticket detail -> Profile -> Panel showed the detail with Profile highlighted, and tapping Panel again could not leave the detail.

### Root cause
- The highlight fell back to the last tab *screen shown*; a restored detail is in no tab's set, so the bar kept Profile.
- Re-selecting the current tab ran the same `popUpTo(home) { saveState = true }` + `restoreState = true`: it popped the detail, saved it and restored it in the same call.

### Technical solutions
- **Per-tab back stacks with two extra rules.** The standard save/restore pattern is kept; on top of it: (1) the highlighted tab is the tab last shown *or tapped* (`lastTab`, set in `openTab` too), so a restored stack highlights its tab; (2) tapping the selected tab is handled explicitly: on its root it only scrolls to the top, deeper it pops to the root with `popBackStack(root, inclusive = false)`.
- **The first tab's root is the role's home** (`homeFor(role)`), so a pending citizen tapping Dashboard on Registration pending stays there (review S1).
- **Scroll to top on reselect** (Gate 1 Q1): a `MutableSharedFlow<Unit>` with no replay and a 1-slot buffer; each tab root hoists its scroll state into the NavHost destination and collects only while its entry is `RESUMED`, so the screen fading out does not react. Scroll positions are saved per back-stack entry.
- **Survives rotation and process death:** `lastTab` is `rememberSaveable` and the NavController saves its own back stack and the per-entry scroll states.

### Privacy point for the defence (review B1)
- Found by the review, present since D-016: logging out cleared only the visible stack. The tab stacks saved by the bottom bar, and the ViewModels attached to them, survived. Citizen A opens Profile, taps Dashboard, logs out; citizen B logs in and taps Profile: Navigation restores A's entry and `viewModel(factory = ...)` returns A's existing `ProfileViewModel` (the factory is only used when no ViewModel exists). B would have seen A's name, NIF, e-mail and phone; the same applied to a half-filled report (submitted under A's id) and to operator detail/approval screens (actions attributed to the previous operator).
- Fix: every user-scoped `viewModel(...)` call has a `key` with the signed-in user's id (`profile-<uid>`, `report-<citizenId>`, `ticket-detail-<uid>`, `operator-ticket-<uid>`, `approval-<operatorId>`, `credential-<uid>`). Another user's restored entry builds a new ViewModel for the current user; the same user keeps theirs.
- Why a key and not clearing the saved stacks at logout: one argument per call, independent of how Navigation stores saved stacks (clearing the home's stack would pop the home itself), and it also covers role changes and guard redirects.
- Honest limit: the old ViewModel stays in memory in that entry's store, never shown, until the entry is popped. `rememberSaveable` in the screens holds only camera temp paths, and a restored detail recomputes its audience for the new user, so the anonymous rules still hold.

### Options chosen and rejected
- Derive the tab from `navController.currentBackStack`: planned, rejected by lint (`RestrictedApi`); the planned fallback (keep `lastTab`, set it on tap) shipped.
- `getBackStackEntry(tab.route)` per tab (public API): rejected in review; it fixes no case that happens today and would loop in the hypothetical case of the home key restoring another tab's stack.
- Drop `restoreState`/`saveState`: fixes "cannot leave" but loses each tab's stack (rejected at plan).
- A counter key for scroll-to-top: `LaunchedEffect(counter)` would fire again on recomposition after coming back; a flow without replay does not.
- Kept (Gate 1 Q2): the list opened from the Dashboard highlights Active.

### Verification
- `./gradlew assembleDebug` and `./gradlew lint`: pass, 0 errors, 17 warnings (unchanged, none in touched files). Review: 1 blocking (B1, pre-existing, fixed), 2 should-fix (fixed), N1/N3 fixed, N2/N4 skipped. Device checks (two users on one device, pending citizen, scroll-to-top per tab, the original repro, rotation) listed in `docs/work/D-020-bottom-bar-back-stack/state.md`, still pending at the docs phase.

### Improvement opportunities
- Nested navigation graphs per tab, so every route belongs to exactly one tab and the highlight comes from the graph, not from `lastTab`.
- Clear the saved tab stacks at logout as well, so the previous user's ViewModels are freed at once (defence in depth on top of the keys).
- A single "user scope" (for example a ViewModelStore per signed-in user) instead of a key on each call.

## D-021: Coordinates in the pending citizen detail (improvement)
Decision: none (operator-only coordinates already follow [0007](../decisions/0007-ticket-detail-by-audience.md) as amended by D-019). Code: `utils/CoordinatesFormat.kt` (new), `ui/screens/CitizenApprovalScreen.kt`, `ui/screens/ShowScreen.kt` (import only) (commit `e94551d`); `RegisterScreen.kt`, `ReportScreen.kt`, `ProfileScreen.kt`, strings EN/PT (`ccdb518`). Overview: "Approvals and categories (D-012)", bullet "Detail"; "Shared components", bullet "Coordinates".

Brief: I.3.D, the operator validates a registration showing "nome, NIF e georreferenciação". Raised by Diogo during the manual test checklist (2026-11-21), after D-019 added the coordinates text to the operator ticket detail: the pending citizen detail had the georeferencing only as a map.

### Technical solutions
- **Georeferencing as exact map plus text,** consistent with the operator ticket detail (I.3.E): under the map, "Coordinates: 40.21000, -8.41900"; TalkBack reads "Latitude x, longitude y". (0, 0) keeps "Location not available" and no line. No new strings: `ticket_coordinates` and `cd_ticket_coordinates` are reused.
- **One formatter for every coordinate text.** D-019's private `formatCoordinate` moved unchanged to `utils/CoordinatesFormat.kt`: `Locale.ROOT`, 5 decimals (about 1 m), so the PT decimal comma never clashes with the comma between latitude and longitude.
- **Own location aligned too (slice 2, added at Gate 1).** Register, Report and Profile printed the user's own fix through `report_location_value` with `%1$.5f`, which follows the device locale (a comma in PT). The placeholders became `%1$s`/`%2$s` and the three callers pass `formatCoordinate(...)`, so every coordinate in the app uses a dot. No string resource formats a float any more.

### Privacy
- Only operators see someone else's coordinates as text: `PendingCitizenRoute` is inside `RequireAccess(OPERATOR)`. Slice 2 only changes the format of texts that show the user's own location, which existed before. No model, repository, Firestore or rules change.

### Options chosen and rejected (plan and Gate 1, 2026-11-21)
- A `formatCoordinates(Coordinates)` returning "lat, lng": rejected, because the strings take latitude and longitude as separate arguments (and TalkBack needs them separately).
- A shared coordinates composable: not done now, because it would rewrite Francisco's block in `ShowScreen.kt` the day before the deadline (review N2: the `Text` block is duplicated in `ShowScreen.ExactLocationSection` and `CitizenApprovalScreen.CitizenLocation`).
- Align Register, Report and Profile: listed as an improvement in the plan, chosen by Diogo at Gate 1 (slice 2).

### Defence note (review N3)
- `report_location_value` (visible text, own location) and `cd_ticket_coordinates` (TalkBack, operator screens) now have the same text, "Latitude %1$s, longitude %2$s". Two keys on purpose: different contexts and owners, so either can change without the other.

### Verification
- `./gradlew assembleDebug` and `./gradlew lint`: pass after both slices, 0 errors, 17 warnings (none in changed files). Review: 0 blocking, 0 should-fix, 3 nits. Device checks (pending citizen detail portrait/landscape, EN/PT, TalkBack, (0, 0); operator ticket detail unchanged; Register/Report/Profile in PT show dots) listed in `docs/work/D-021-citizen-approval-coordinates/state.md`, still pending at the docs phase.

### Improvement opportunities
- `ui/components/CoordinatesText(location)` used by both operator screens (removes the duplicated block, review N2).
- Copy the coordinates to the clipboard, or open them in an external maps app (`geo:` intent), in both operator screens.

## F-021: Bugs from manual testing (fixes)
Overview: "Dark mode and system force dark (F-021)", "Pending citizens list photo (F-021)", `AttachmentViewer` bullet. Work item: `docs/work/F-021-testing-bug-batch/`.

### Technical solutions
- **Photo viewer and rotation.** Saved state (`rememberSaveable`) is tied to its place in the composition. The detail screen uses a `Column` in portrait and a `Row` in landscape, so state kept inside the photo section did not match after rotating and the viewer closed. The open photo id now lives in `ShowScreen`, the same place in both layouts.
- **Dark mode text.** The app has its own dark theme, so it opts out of system force dark (`android:forceDarkAllowed=false`); a Xiaomi phone was recolouring the snackbar text.
- **Pending list thumbnail.** The operator's queue now shows each citizen's profile photo.
- **Tab highlight.** Same bug as Diogo's D-020; D-020's fix (per-tab back stacks kept, highlight follows the stack) was kept at the merge instead of F-021's simpler "tab tap opens its root".

### Not bugs (explained)
- SMS at login for an account created before F-018 (decision 0026: its phone is still an MFA factor).
- Anonymous users not seeing an "In progress" ticket that was PRIVATE (decision 0006).

### Lessons
- A first fix guessed the cause (dialog insets); reproducing on the emulator showed the viewer was closing, not shrinking. Reproduce before fixing.

## F-022: Background geofence and vertical impact detection
Decision: [0028](../decisions/0028-background-tracking-and-vertical-impact.md) (amends [0022](../decisions/0022-operator-proximity-alert.md), [0023](../decisions/0023-impact-detection.md)). Overview: "Background tracking and vertical impact (F-022)". Brief I.2.F, II.3, V. Not checked on a device.

### Technical solutions
- **Vertical acceleration:** `a_v = (a·g)/|g| - |g|`, the accelerometer projected on gravity minus gravity; about 0 at rest in any position.
- **Gravity:** `TYPE_GRAVITY` (slides 10/11), or a low-pass filter `g = 0.8·g + 0.2·a` on phones without it.
- **Why not total magnitude (F-015):** putting the phone down hard or lifting it fast changed `|a|` without any vertical bump.
- **Speed gate:** a stationary drop is not a pothole; GPS speed > 2 m/s from a fix < 10 s old.
- **Threshold:** 3-15 m/s², default 6, Profile slider, SharedPreferences (slides 09), read on every sensor event.
- **One foreground service** (slides 08 `TrackingService`, P10 `ProximityService`): one location request feeds the speed and the operator's 20 m geofence.
- **Impact suggestion:** snackbar when the app is in front, notification otherwise; both open the prefilled report.

### Android rules
- A foreground service must be started while the app is visible (Android 12+): started from `CityFixApp`.
- Type `location` and `FOREGROUND_SERVICE_LOCATION` (Android 14+); `startForeground` within 5 s.
- `ACCESS_BACKGROUND_LOCATION` is a separate request (Android 10+), after foreground location; explained first in a dialog (slides 10/11).

### Options chosen and rejected (Gate 1, 2026-11-22)
- Chosen: one service for both features; threshold on the device; in-app detector removed; D-014 ViewModel replaced by `OperatorGeofence`.
- Rejected: WorkManager (15 min minimum, no sensors); `GeofencingClient` (100-150 m radius, minutes late); two services.

### Class material vs beyond class
- Class: foreground service (slides 08), `ProximityService` and `requestLocationUpdates` (P10), sensors and background location (slides 10/11), SharedPreferences (slides 09).
- Beyond class: projection on gravity, low-pass filter, `shareIn` of one location flow, app-in-front flag.

### Limits to state at the defence
- Impact detection now needs precise location (speed).
- Not restarted after a reboot; OEM battery savers can kill it.
- Not checked on a device or emulator (build and lint only).

### Improvement opportunities
- `BOOT_COMPLETED` receiver to restart after a reboot.
- Battery-saver guidance for OEMs (ignore battery optimisations).
- Threshold per account (Firestore) instead of per device.
- `GeofencingClient` for the operator when the app is killed.
- Save the impact location and time in the prefilled report.

### Hand-off
- Tell Diogo: D-014's `OperatorProximityViewModel` is replaced by `OperatorGeofence` in the service.

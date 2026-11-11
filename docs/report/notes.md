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
- Deep links to a ticket (for example from an FCM status-change notification) with `TicketDetailRoute`.
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
- Impact-detection prefill (brief I.2.F): a `prefill(situation, categoryId)` event that opens the form for a pavement anomaly.
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
- `FirestoreTicketsRepository` behind the same interface, with listeners instead of `refresh()` (also fixes the two separate locks in `refresh()`, review N2).
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
Decision: [0012](../decisions/0012-ticket-lifecycle-rules.md) (builds on [0004](../decisions/0004-denormalised-current-status.md) and [0006](../decisions/0006-anonymous-projection-phase-1.md)). Code: `model/TicketLifecycle.kt`, `model/AttachmentPhase.kt`, `model/Attachment.kt`. Overview: "Lifecycle (D-008)".

**Not checked against the brief.** The brief PDF could not be opened in the D-008 sessions; the rules tagged brief II, II.2, I.2.E, III and I.3.E come from the task files. Check them in the PDF before writing them into the report as the brief's wording.

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
- Q6: `AttachmentPhase` added in D-008 (decision 0012), F-010 reuses it; rejected waiting for F-010 or a predicate parameter.
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

## F-010: Firebase setup and Firestore schema
Decisions: [0010](../decisions/0010-firestore-schema.md) (layout and rules design), [0011](../decisions/0011-firebase-plan-notifications-close-maps.md) (plan, notifications, 5-day close, maps), [0009](../decisions/0009-tickets-repository.md) amended. Code: `CityFixApplication.kt`, `firestore.rules`, `storage.rules`, `firebase.json`, version catalog, both Gradle files. Overview: "Firebase (F-010)", "Setup", "Handover (F-010)".

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
- **The four Gate decisions** (decision 0011): Blaze with a budget alert (cost guard; Storage is impossible on Spark); no Cloud Functions (no server code in a second language that both authors must defend, no deploy pipeline); 5-day close computed on read; osmdroid (no key, so the ZIP builds and the map renders for the grader). Also: `google-services.json` out of git and copied into the ZIP by hand; test mode until rules are deployed; SMS MFA with test numbers.
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
- **Not enforceable in rules:** the exact 10 m circle (bounding box only, rules have no trigonometry); the truth of the reported location (a citizen could copy a ticket's location from `ticketLocations` and join any ticket, PRIVATE included, which follows from the brief's automatic grouping); the order of operator transitions (D-008's state machine); edits of existing attachment entries (rules cannot loop over lists); `coarseLocation` rounding; video metadata stripping (F-013).
- **The 10 km anonymous radius is checked on the client only** (anonymous requests carry no location).
- **An offline RESOLVED gets the sync time as `resolvedAt`**, not the tap time; the history entry keeps the device time, so the two can differ. A device clock more than 5 minutes fast cannot write status changes.
- **Signed-in active citizens can read exact locations, category and situation of every ticket** through `ticketLocations` (no author, description or media). Accepted for Option A.
- **`google-services.json` is handled outside git** and copied into the ZIP by hand.
- **The rules were drafted on 2026-11-10 and deployed on <date> (fill in later).** They were checked by reading only; they have not been run in the Rules Playground or the emulator yet. Until the deploy, the test-mode database is open to anyone with the project config (expires 2026-12-10).
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
- `stats/summary` counters (or `count()` aggregations the rules allow) for the anonymous dashboard (F-011).
- A note in the `CityFixApplication` KDoc that `Firebase.firestore` must never be called directly (review N6), and the remaining round-1 nits (key whitelists per role, active category and at least one attachment on ticket create).

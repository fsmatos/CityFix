# Architecture overview

Seeded from the factory manifest (A1-A4). Update as the app takes shape.

## Components
- One Android app, Kotlin, Jetpack Compose + Material3. minSdk 24, targetSdk 37. Package `pt.isec.amov.cityfix`.
- Single Gradle module `:app`.

## Layers (MVVM)
- `ui/screens/`: stateful screens (collect `StateFlow<UiState>`) and their stateless content composables; `ui/components/` reusable composables; `ui/navigation/` type-safe `@Serializable` routes (D-004) and the stateless `MainTopAppBar` (D-005); `ui/theme/` theme.
- `viewmodel/`: ViewModels and their `UiState`. Phase 1 has one, `TicketsViewModel` (D-003), which exposes two flows instead of one `UiState` (see "ViewModel (D-003)").
- `data/`: repositories over Firebase Auth, Firestore, Storage, FCM; WorkManager workers for media upload. In phase 1 it holds only `SampleTickets` (the 3 sample tickets); repositories come with Firebase.
- `model/`: entities, enums and pure rules (ticket lifecycle, 5-day reopen, 10 m grouping, 10 km anonymous radius, 20 m geofence).
- `utils/`: stateless helpers (date formatting, distance maths; later image compression and thumbnails). Code that `model/` imports (`haversineMeters` in `utils/Distance.kt`) must stay pure Kotlin, with no Android imports; the only package dependency is `model -> utils`.

## Domain model
Chosen model: **Option A, one ticket with several following citizens** (see [decision 0002](../decisions/0002-grouped-reports-single-ticket.md)). A report with the same category + situation within 10 m of an active ticket makes the citizen follow that ticket instead of creating a new one. There is no `Grupo` entity.

![Domain model, Option A](diagrams/CityFix_Dominio_OpcaoA.png)

Entities: `Utilizador` (nome, nif, email, telefone, foto?) specialised into `Municipe` (georreferenciacao, estadoRegisto, detecaoImpactos) and `Operador`; `Municipio` (nome, dominioEmail, centro, raioKm) with its `Categoria`s and operators; `Ticket` (descricao, situacao, alcance, localizacao, estadoAtual, dataCriacao) composed of `Mensagem`, `HistoricoEstado` (1..*) and `Anexo` (1..*, with thumbnail and upload state). Enums: `Situacao`, `Alcance`, `EstadoTicket`, `EstadoRegisto`, `TipoAnexo`, `EstadoUpload`.

### Code mapping
The diagram is implemented in English in `model/` (task D-001), one type per file, with no Android or Firebase imports. Representation choices are in [decision 0003](../decisions/0003-domain-model-representation.md); the duplicated current status is in [decision 0004](../decisions/0004-denormalised-current-status.md).

| Diagram (PT) | Code |
| --- | --- |
| `Utilizador` | `sealed interface User` (id, name, nif, email, phone, photo?) |
| `Municipe` | `data class Citizen : User` (+ georeferencing, registrationStatus, impactDetection) |
| `Operador` | `data class Operator : User` |
| `Municipio` | `Municipality` (name, emailDomain, center, radiusKm = 10.0) |
| `Categoria` | `Category` (name, active) |
| `Ticket` | `Ticket` |
| `Mensagem` | `Message` (text, sentAt, authorId) |
| `HistoricoEstado` | `StatusHistory` (status, dateTime, justification?, responsibleId) |
| `Anexo` | `Attachment` (type, file, thumbnail?, uploadState, uploadedById) |
| `centro`, `localizacao`, `georreferenciacao` | `Coordinates` (latitude, longitude; WGS84) |
| `EstadoTicket`, `Situacao`, `Alcance` | `TicketStatus`, `Situation`, `Scope` |
| `TipoAnexo`, `EstadoUpload`, `EstadoRegisto` | `AttachmentType`, `UploadState`, `RegistrationStatus` |

Enum values: `TicketStatus { OPEN, INFO_REQUESTED, IN_PROGRESS, RESOLVED, CLOSED }`, `Situation { DAMAGE, BREAKDOWN, DESTRUCTION, THEFT }`, `Scope { PUBLIC, PRIVATE }`, `AttachmentType { IMAGE, VIDEO }`, `UploadState { PENDING, UPLOADING, UPLOADED, FAILED }`, `RegistrationStatus { PENDING, ACTIVE }`.

Representation rules:
- Ids are `String`, default `""` (later the Firestore document id, or the Auth uid for users). Previews and sample data must set distinct ids, because lists key on `id`.
- Other entities are referenced by id, never embedded, so a ticket never carries a citizen's personal data. The one exception is the category: `Ticket` stores `categoryId` plus a `categoryName` snapshot.
- Times (`createdAt`, `sentAt`, `dateTime`) are `Long` epoch milliseconds, UTC, default `0L`.
- Enums are persisted by constant name, so the names are a contract and must not be renamed. UI labels come from string resources (EN + `values-pt`), never from `.name`.
- The role is the type: `User` is a sealed interface; there is no `role` field in the model. The data layer writes and reads a `role` string in Firestore.
- Every class can be built with no arguments (all parameters have defaults); all properties are `val` and collections are `List`. Mapping from Firestore documents happens in `data/` later.
- Defaults: `currentStatus = OPEN`, `situation = DAMAGE`, `scope = PRIVATE`, `type = IMAGE`, `uploadState = PENDING`, `registrationStatus = PENDING`, `active = true`, `impactDetection = false`, `Coordinates()` = (0, 0). (0, 0) is not a real location; distance code must not treat it as one.
- A default `Ticket()` does not satisfy the diagram's `1..*` multiplicities (no history entry, no attachment); creation logic enforces them, not the constructor.
- `Ticket.currentStatus` duplicates the last `statusHistory` entry on purpose (decision 0004); both change together.
- `Ticket.followerIds` does not include `creatorId`; notifications go to the creator plus the followers.
- `Ticket.messages`, `statusHistory` and `attachments` are lists on the ticket in phase 1; moving them to subcollections is a Firebase-phase decision.
- Fields an anonymous view must never show: `Ticket.location` (coarse map zoom only), `creatorId`, `followerIds`, `messages`, and all `Citizen` personal data. The full list enforced in phase 1 (including `StatusHistory.responsibleId`/`justification` and `Attachment.uploadedById`) is in "ViewModel (D-003)". The 10 km radius of `Municipality` limits where anonymous users may be, not where tickets may be.

| Diagram association | Model field |
| --- | --- |
| `cria` | `Ticket.creatorId` |
| `acompanha` | `Ticket.followerIds` |
| `classificado em` | `Ticket.categoryId` (+ `categoryName` snapshot) |
| `autor` | `Message.authorId` |
| `responsavel` | `StatusHistory.responsibleId` |
| `carregado por` | `Attachment.uploadedById` |
| composition `Ticket` -> `Mensagem`, `HistoricoEstado`, `Anexo` | `Ticket.messages`, `Ticket.statusHistory`, `Ticket.attachments` |
| `define` (Municipio -> Categoria) | not mapped yet (phase 1 has one municipality) |
| `pertence a` (Operador -> Municipio) | not mapped yet |
| `valida` (Operador -> Municipe) | not mapped yet |
| `gere` (Operador -> Ticket) | not mapped yet |

The four unmapped associations were left out at Gate 1 of D-001 because the phase-1 spec does not list them; they are added in the Firebase/operator tasks.

Rejected alternative, kept for the technical report: [Option B, tickets grouped by a `Grupo`](diagrams/CityFix_Dominio_OpcaoB.png).

### Rules (D-002)
Details and reasons: [decision 0005](../decisions/0005-tickets-list-rules.md).
- `TicketsList` (`model/TicketsList.kt`) is the phase-1 in-memory source of tickets, owned by `TicketsViewModel` (D-003). It keeps a private mutable list and every read returns a copy. The Firebase phase replaces it with a repository that reuses the same pure rules.
- `Ticket.isActive` is true for OPEN, INFO_REQUESTED or IN_PROGRESS only. RESOLVED and CLOSED are not active (Gate 1, Q3). The anonymous list, the grouping and the "% active" stat all use it.
- `publicActive()` returns PUBLIC active tickets, newest first. It filters tickets, not fields: anonymous screens must still drop `creatorId`, `followerIds`, `messages` and the exact location. In phase 1 `TicketsViewModel` does this with `Ticket.toAnonymousView()` (D-003).
- The 10 m grouping is `findNearby(location, categoryId, situation)`: same `categoryId` + `situation`, active, at most 10 m by Haversine (inclusive, nearest wins), (0, 0) ignored, any scope. A report next to a RESOLVED ticket creates a new ticket. Following the found ticket is the caller's job.
- `stats()` returns `DashboardStats` over every ticket, private ones included: `totalTickets`, `activeTickets`, `resolvedTickets` (tickets in the average), `averageRepairTimeMillis` (milliseconds, last RESOLVED entry minus `createdAt`, active tickets skipped, `null` when nothing is resolved) and the computed `activeRatio` (0.0..1.0, never NaN).

### ViewModel (D-003)
Details and reasons: [decision 0006](../decisions/0006-anonymous-projection-phase-1.md). Code: `viewmodel/TicketsViewModel.kt`, `model/TicketPrivacy.kt`, `data/SampleTickets.kt`.
- `TicketsViewModel` owns a private `TicketsList` seeded from `SampleTickets.all`: 3 PUBLIC tickets near Pr. Republica (`sample-1` OPEN, `sample-2` IN_PROGRESS, `sample-3` RESOLVED 3 days after creation), with fixed UTC epoch times so the dashboard figures are reproducible. Their category ids are `pavement`, `lighting` and `furniture`.
- It follows P07's pattern: private `MutableStateFlow`s exposed with `asStateFlow()`, events in as plain functions, no coroutines (all work is synchronous and in memory). No-arg constructor, so the default `viewModel()` factory works. State survives rotation because it lives in the `ViewModel`; process death restores the samples (accepted for phase 1).
- Public API:
  - `tickets: StateFlow<List<Ticket>>`: `publicActive()` mapped through `toAnonymousView()`, newest first. With the samples: `sample-1`, then `sample-2`.
  - `stats: StateFlow<DashboardStats>`: `stats()` over every ticket (decision 0005). With the samples: total 3, active 2, resolved 1, average 3 days (259 200 000 ms).
  - `getTicketById(id): Ticket?`: searches only the current `tickets` value, so unknown, PRIVATE, RESOLVED and CLOSED ids return `null`. It is a snapshot, not a stream.
  - `reportTicket(description, categoryId, categoryName, situation, scope): String?`: trims the three strings and returns `null` if any is blank (or `add` refuses the ticket); otherwise adds a ticket with a `UUID` id, `currentStatus = OPEN`, one OPEN history entry, `creatorId = LOCAL_USER_ID` (`"local-citizen"`), no location and no attachment, refreshes both flows and returns the new id. No 10 m grouping yet: phase-1 reports have no location, so `findNearby` would never match (decision 0006).
- `Ticket.toAnonymousView()` (`model/TicketPrivacy.kt`, pure Kotlin) returns a copy of the same `Ticket` type with:
  - `creatorId` blank, `followerIds` and `messages` empty;
  - every `StatusHistory.responsibleId` blank (the first OPEN entry holds the creator's id) and every `StatusHistory.justification` null (free text that may name the author);
  - every `Attachment.uploadedById` blank;
  - the location rounded to `ANONYMOUS_COORDINATE_DECIMALS = 2` (about 1 km; (0, 0) stays (0, 0)). Nearby tickets collapse onto one grid point (all three samples become (40.21, -8.42)), so a future anonymous map must cluster or offset markers, never use exact coordinates.

  A field added to `Ticket`, `StatusHistory` or `Attachment` later is copied through unchanged, so it must be checked against this list.
- **Projection gap found by F-005 (review S4, handover to Diogo):** brief I.1.C limits the anonymous detail to media and a coarse map, so `toAnonymousView()` must also blank `description` and clear `statusHistory` (keeping `currentStatus`), and decision 0006 must be amended accordingly ([decision 0007](../decisions/0007-ticket-detail-by-audience.md)). **Until then these two fields are hidden from anonymous users only in the UI** (`ShowScreen`'s `when (audience)`), against the CLAUDE.md rule that privacy is enforced below the UI. The change also empties the expanded `TicketCard` description in the anonymous list (F-003 Gate 1 Q2), so decide it together with that card.
- **Media is outside the projection.** `Attachment.file` and `thumbnail` are passed through unchanged. The media-upload task must strip EXIF GPS tags when it compresses images (`utils/`) and must use Storage paths that contain no user id.
- **Contract for D-006:**
  - Create the ViewModel once, above the `NavHost` (in `MainActivity`'s content, `val vm: TicketsViewModel = viewModel()`), and pass state and callbacks down. Calling `viewModel()` inside each destination scopes it to that back-stack entry, so a reported ticket would not appear in the list.
  - Add `lifecycle-viewmodel-compose` (for `viewModel()`) and `lifecycle-runtime-compose` (for `collectAsStateWithLifecycle`); D-003 adds neither.
  - Take the detail ticket from the collected `tickets` value (`tickets.firstOrNull { it.id == id }`, or `remember(id, tickets)`) rather than calling `getTicketById` once, so the detail screen recomposes when the list changes (review N4).
  - Do not navigate to the id that `reportTicket` returns: a PRIVATE report is not in `tickets`, so `getTicketById` returns `null` and the detail screen would show "Ticket not found" (review N1). Show a confirmation (for example a snackbar) instead, and handle a `null` return.
- Screens receive tickets already projected (blank author fields, coarse location), so F-003/F-005 must not try to show the author.

### Navigation (D-004)
Code: `ui/navigation/Destinations.kt`. No decision record (D-004 Gate 1, Q5): type-safe routes are the course default, and the version pin is explained here.
- Four `@Serializable` routes serve the phase-1 flow dashboard -> list -> detail -> report:
  - `data object DashboardRoute` (start destination, dashboard figures);
  - `data object TicketListRoute` (public active tickets);
  - `data class TicketDetailRoute(val ticketId: String)` (one ticket);
  - `data object ReportTicketRoute` (provisional report form).
- `TicketDetailRoute` carries only `ticketId: String` (matching `Ticket.id`), never a `Ticket` or any other field. The detail screen takes the ticket from the collected `tickets` (see "ViewModel (D-003)"), so the back stack never stores ticket content, personal data or coordinates.
- Build setup:
  - the Kotlin Serialization compiler plugin (`org.jetbrains.kotlin.plugin.serialization`, catalog alias `kotlin-serialization`, `version.ref = "kotlin"`) generates the serializers. The root build declares it `apply false` and `:app` applies it, like `kotlin-compose` (AGP 9 built-in Kotlin, no `kotlin-android` plugin);
  - the runtime `kotlinx-serialization-core` 1.11.0 provides `@Serializable`, the version used in class (P08). It pulls `kotlin-stdlib` up to 2.3.20 at runtime, while the compiler stays at Kotlin 2.2.10; the build accepts this with no compiler warnings, so Kotlin was not changed.
- Checking the plugin ran: with AGP 9 built-in Kotlin the compiled classes are under `app/build/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes/` (not `app/build/tmp/kotlin-classes/debug/`). `pt/isec/amov/cityfix/ui/navigation/TicketDetailRoute$$serializer.class` must exist there.
- Accepted lint warnings: lint reports newer versions as `NewerVersionAvailable` (not `GradleDependency`). D-004 adds two, both accepted: `kotlinx-serialization-core` (1.11.0 vs newer, kept to match the class) and the `org.jetbrains.kotlin.plugin.serialization` plugin (2.2.10 vs newer, the same warning `kotlin-compose` already has).
- **Contract for D-006:**
  - Add `androidx.navigation:navigation-compose` (suggested 2.9.8) through the version catalog. D-004 does not add it.
  - After adding it, run `./gradlew :app:dependencies --configuration debugRuntimeClasspath` and check that `kotlinx-serialization-core` still resolves to 1.11.0. `navigation-compose` depends on it too, and Gradle picks the highest version requested, so a newer one would silently change the runtime.
  - Build `NavHost(startDestination = DashboardRoute)` with `composable<DashboardRoute>`, `composable<TicketListRoute>`, `composable<TicketDetailRoute>` and `composable<ReportTicketRoute>`.
  - In the detail destination, read the id with `backStackEntry.toRoute<TicketDetailRoute>().ticketId`.
  - Never navigate to the id returned by `reportTicket` (see "ViewModel (D-003)").

### Top bar (D-005)
Code: `ui/navigation/MainTopAppBar.kt`. No decision record (D-005 docs plan): these are local UI choices, explained here and in the report notes.
- `MainTopAppBar` is stateless: the screen, `canNavigateBack` and `isExpanded` come in as parameters, and clicks go out as callbacks. It imports nothing from `androidx.navigation`, `viewmodel` or `model`, so it needs no navigation library and can be previewed with no `NavController`.
- `TopBarScreen { DASHBOARD, LIST, DETAIL, REPORT }` has one value per route in `Destinations.kt`.
- Controls per route:

  | Route | `TopBarScreen` | Title (EN / PT) | Back arrow | Toggle | Add |
  | --- | --- | --- | --- | --- | --- |
  | `DashboardRoute` | `DASHBOARD` | Dashboard / Painel | if `canNavigateBack` (never in practice: start destination) | no | yes |
  | `TicketListRoute` | `LIST` | Tickets / Ocorrências | if `canNavigateBack` | yes | yes |
  | `TicketDetailRoute` | `DETAIL` | Ticket details / Detalhes da ocorrência | if `canNavigateBack` | no | yes |
  | `ReportTicketRoute` | `REPORT` | Report an issue / Reportar ocorrência | if `canNavigateBack` | no | no |

- The toggle switches the whole list between compact and expanded cards: one value, not one per card. Its icon and content description change with `isExpanded`, and the description names the action a tap will perform.
- Adding a screen: only the title is exhaustive. It comes from a `when` with no `else`, so a new `TopBarScreen` value does not compile until it has a title. The controls use `if`, so the compiler does not ask about them: a new screen silently gets the add button and no toggle. Check both by hand.
- **Requirement for the Auth phase:** the bar must hide the add button by role, because the brief lets only registered citizens open tickets. D-005 shows it to everyone because phase 1 has no login. No task file covers this yet, so it must go into the Auth task when that is written.
- Icons: `androidx.compose.material:material-icons-core`, with no version in the catalog. The Compose BOM sets 1.7.8, the library's frozen last release, so this is not a mismatch with the other Compose libraries. It is needed because `material3` 1.4 no longer brings icons. `KeyboardArrowUp`/`KeyboardArrowDown` are used because `ExpandLess`/`ExpandMore` are only in the extended library.
- Strings: titles use the `top_bar_` prefix and content descriptions the `cd_` prefix. The Portuguese strings say "ocorrência(s)", the brief's term and the one in Francisco's strings, never "ticket" or "problema" (D-005 review S1). The English strings keep "ticket", so the string names do too. D-005 created the first `values-pt/strings.xml`.
- **Merge note (`origin/dev/Francisco`):** his branch also adds `values-pt/strings.xml`, so expect an add/add conflict. Keep both sides, except `app_name`: he marks it `translatable="false"` in `values/strings.xml`, so keep that line and delete `app_name` from `values-pt/strings.xml`, then run lint. Until that merge our file is correct: without `translatable="false"`, lint needs the translation.
- **Contract for D-006:**
  - Place the bar in the `Scaffold`. Nothing calls it before D-006, so it has not run on a device yet.
  - Convert the current destination to `TopBarScreen` with `hasRoute<T>()`.
  - Pass `canNavigateBack = navController.previousBackStackEntry != null`. `previousBackStackEntry` is not Compose state, so reading it alone does not cause a recomposition and the back arrow can go stale. Read it in the same scope as `currentBackStackEntryAsState()`, which `hasRoute` needs anyway.
  - Keep `isExpanded` in `rememberSaveable` above the `NavHost`, and pass it to both the bar and `ListScreen`.
  - Set `onBackClick = navController::navigateUp`; `onAddClick` navigates to `ReportTicketRoute` with `launchSingleTop = true`, as a double tap could otherwise open the form twice.
  - Check landscape on an emulator with a display cutout: no controls should sit under it (D-005 review N5).

## UI foundation
- Theme: the course template trio in `ui/theme/` (`Color.kt`, `Theme.kt`, `Type.kt`). Light only (dark mode is not required by the brief): `CityFixTheme` has one `lightColorScheme` with primary `CityFixBlue`, secondary `CityFixTeal`, tertiary `CityFixOrange`. `Type.kt` stays the template. Screens read `MaterialTheme.colorScheme` and `MaterialTheme.typography`, never raw colours.
- `dynamicColor` defaults to `false`, the one change to the template: on Android 12+ dynamic colour would replace the CityFix colours with the wallpaper palette.
- `MainActivity` calls `enableEdgeToEdge` with `SystemBarStyle.light(TRANSPARENT, TRANSPARENT)` for status and navigation bars. The default style follows the device, so in device dark mode it would draw light icons over the light-only UI; the light style keeps them dark and readable.
- Strings: English default in `values/strings.xml`, Portuguese in `values-pt/strings.xml`, same snake_case keys; the device language picks the file, falling back to English. Composables use `stringResource(R.string.x)`, no literals. `app_name` is `translatable="false"`. Each task adds its strings with the screen that uses them, EN and PT in the same commit (lint `MissingTranslation` catches gaps). Plurals use `<plurals>` with `pluralStringResource(R.plurals.x, count, args...)` (first added by F-004): EN needs `one`/`other`, PT needs `one`/`many`/`other` (`many` equal to `other`; lint accepts it, with no `MissingQuantity`/`UnusedQuantity`). PT `one` also covers 0 (CLDR `i = 0..1`), so a plural that could show 0 would read "0 reparação"; no current call site passes 0. A plural whose numbers are pre-formatted (`dashboard_active_of_total`) uses `%1$s`/`%2$s`, while the quantity argument stays an `Int`.
- Sizes: `dp` for layout, `sp` for text (so text follows the font scale).
- Touch targets: 48dp through Material 3 components (`Button`, `IconButton`, ...), which enforce the minimum size; no custom size constants.
- `contentDescription`: meaningful icons use `stringResource(...)`; `null` only for decorative icons next to text that already says the same.
- Raw colour exception: `CityFixGreen` (`Color.kt`) is used directly by the RESOLVED `StatusBadge`, with `Color.White` text. It is the one place outside `Theme.kt` that reads a raw colour, because the M3 colour scheme has no green role and "resolved" needs a colour distinct from primary, secondary and tertiary.
- Later (report): dark theme, in-app language selector (per-app locales), dynamic colour toggle in settings, custom container colours.

### Shared components
Stateless composables in `ui/components/` (data in, events out; no ViewModel, `remember` or Firebase), reused by the list (F-003) and the detail screen (F-005). Task F-002.
- `StatusBadge(status)`: non-clickable `Surface` + `Text` with one colour and the localised label per `TicketStatus`. Colours: OPEN `primary`, INFO_REQUESTED `tertiary`, IN_PROGRESS `secondary`, RESOLVED `CityFixGreen` (see the exception above), CLOSED `inverseSurface`/`inverseOnSurface` (stands out on the card container). The label is always shown, so status is never conveyed by colour only.
- `TicketCard(ticket, isExpanded, onClick)`: M3 `Card(onClick)`. Collapsed it shows a placeholder thumbnail (vector drawable on a `surface` box), the category name (fallback string if blank), the situation and the `StatusBadge`, the content brief I.1.B lists for the ticket list. Expanded (`isExpanded = true`) it also shows the description (up to 3 lines, skipped if blank) and "Reported on <date>" (skipped if `createdAt == 0L`). `isExpanded` is the global top-bar toggle owned by the screen (D-005/D-006), not per-card state. **Anonymous caveat:** `isExpanded = true` shows the free-text description, which brief I.1.B keeps out of the anonymous list (category, situation, thumbnail) and which may hold personal data; D-005/D-006 must hide or force off the expand toggle for anonymous users once roles exist (see "Screens", `ListScreen`). The card reads only `categoryName`, `situation`, `currentStatus`, `description` and `createdAt`; never `location`, `creatorId`, `followerIds` or `messages`.
- Enum labels: `statusLabelRes` (`StatusBadge.kt`) and `situationLabelRes` (`TicketCard.kt`) map each constant to its `R.string` id (plain `Int`) with an exhaustive `when`, never `.name`; a new constant does not compile until it has a label. PT labels are the brief's wording (Aberto, Pedido de informações, Em resolução, Resolvido, Encerrado; Dano, Avaria, Destruição, Roubo).
- Dates: `utils/formatDate(epochMillis)` uses `java.text.DateFormat.MEDIUM` in the device locale (works on minSdk 24 without desugaring). `utils/formatDateTime(epochMillis, locale)` (F-005) adds a short time and takes the locale as a parameter, so callers pass `LocalConfiguration.current.locales[0]` and PT previews format in PT. The two helpers differ (review N7): `formatDate` should also take a locale, with `TicketCard` passing the `LocalConfiguration` one (follow-up, not done in F-005).
- `CoarseLocationMap(location, modifier)` (F-005): the "Approximate location" section. A heading, a 200dp `surfaceVariant` box with a `LocationOn` icon and `cd_ticket_map`, and the caption "Approximate area (about 1 km)"; "Location not available" for the unset (0, 0) (decision 0003). It never renders coordinates as text. It is a **static placeholder**, chosen at F-005 Gate 1 (no library, no key, no manifest change), so it does not yet meet brief I.1.C's "mapa com zoom afastado". The map is behind this one component so it can be replaced in one file:
  - osmdroid (`org.osmdroid:osmdroid-android` 6.1.x): no key or account; needs `INTERNET` + `ACCESS_NETWORK_STATE`, an `AndroidView { MapView }` wrapper with `onDetach()` in `onRelease`, a User-Agent and visible OSM attribution (tile policy). The repository was archived in Nov 2024;
  - Google Maps Compose (`com.google.maps.android:maps-compose`): native composable, but needs a Maps API key from a Google Cloud project, kept out of git (CLAUDE.md: ask the person first);
  - either way: fixed coarse zoom (about 14), all gestures off, a translucent circle of about 600 m instead of a pin, and a placeholder in previews through `LocalInspectionMode`.

### Screens
Screen composables in `ui/screens/`. In phase 1 they are stateless (data in, events out): the agreed signatures make the screen itself the stateless half, and the stateful half (collecting `TicketsViewModel` flows) lives in D-006's `composable<...>` blocks, with the single ViewModel created above the `NavHost` (see "ViewModel (D-003)").
- `ListScreen(tickets, isExpanded, onTicketClick: (String) -> Unit, modifier)` (`ui/screens/ListScreen.kt`, F-003): the public list behind `TicketListRoute` (brief I.1.B).
  - Renders the list exactly as received. Filtering (PUBLIC + `isActive`), newest-first order and `toAnonymousView()` happen upstream in `TicketsList.publicActive()` and `TicketsViewModel.tickets`; the screen does not filter, sort or check `isActive`, and reads only `ticket.id` itself.
  - `LazyColumn` (16dp content padding, 8dp spacing) with `items(tickets, key = { it.id })`, one `TicketCard` per ticket. Stable keys keep item identity when the list changes; ids are unique and non-blank because `TicketsList.add` refuses others.
  - Empty list: a centred, localised message (`ticket_list_empty`, EN + PT) instead of the `LazyColumn`.
  - A tap calls `onTicketClick(ticket.id)`: only the id, ready for `navigate(TicketDetailRoute(id))`.
  - `modifier` is applied to the root of whichever branch is drawn, so the caller passes the `Scaffold` inner padding.
  - Same layout in portrait and landscape (Gate 1, Q3); the scroll position survives rotation through the default saveable `LazyListState`.
  - No stateful wrapper (Gate 1, Q1): D-006 collects `vm.tickets`. `isExpanded` is passed straight through to every card (Gate 1, Q2).
  - **Handover to D-005/D-006 (F-003 review S1):** `isExpanded = true` shows the free-text description on every card. In phase 1 every user is anonymous, so D-005/D-006 must hide or force off the expand toggle for anonymous users once roles exist (brief I.1.B; CLAUDE.md: anonymous users never see the author's personal data). Until then, wiring the toggle unconditionally shows descriptions in the anonymous list.
  - **Contract for D-006:**
    ```kotlin
    ListScreen(
        tickets = vm.tickets.collectAsStateWithLifecycle().value,
        isExpanded = expanded,
        onTicketClick = { navController.navigate(TicketDetailRoute(it)) },
        modifier = Modifier.padding(innerPadding),
    )
    ```
    In landscape on a device with a camera cutout, check that no card sits under the notch; add `WindowInsets.displayCutout` padding (or `Scaffold(contentWindowInsets = WindowInsets.safeDrawing)`) if needed (F-003 review N3).
- `DashboardScreen(stats, onSeeTicketsClick: () -> Unit, modifier)` (`ui/screens/DashboardScreen.kt`, F-004): the anonymous dashboard behind `DashboardRoute`, the start destination (brief I.1.A). No decision record (Gate 1 kept the defaults except Q4).
  - Formats `DashboardStats` and recomputes nothing: it never reads a `Ticket`. It trusts the D-002 contract (`activeRatio` in 0.0..1.0 and never NaN, 0.0 with no tickets; `averageRepairTimeMillis` `null` when nothing is resolved and never negative) and adds no guard of its own (Gate 1: one owner per rule). Any change to `DashboardStats` must keep that contract, or the fault shows directly in the UI (for example "NaN" in the zero-ticket preview).
  - Three `Card`s in the brief's order:
    - average repair time: whole days and hours, both truncated, with plurals ("3 days", "1 day and 4 hours", "5 hours", "Less than 1 hour"), or "No repairs yet" when `null`. Supporting line "Based on N repairs" (`resolvedTickets`) only when the average is not `null`;
    - total issues: locale-aware integer `NumberFormat`;
    - % active (label "Active issues (not yet repaired)", because RESOLVED is not active): locale-aware percent `NumberFormat` on `stats.activeRatio` (EN "67%", PT "67 %"). Supporting line "N of M issues" (`activeTickets` of `totalTickets`, numbers in the same integer format as the total card, pluralised on `totalTickets`), omitted when `totalTickets == 0`. With zero tickets the card shows "0%" (Q3).
  - The percent rounds to whole numbers (HALF_EVEN), so 1 of 300 shows "0%" and 299 of 300 shows "100%"; the "N of M issues" line underneath gives the exact figure (review N5). Use `maximumFractionDigits = 1` if a finer value is ever wanted.
  - Locale from `LocalConfiguration.current.locales[0]` (API 24), not `Locale.getDefault()`, so the `locale = "pt"` preview and the device agree.
  - Layout: a scrolling `Column` (16dp padding, 12dp spacing). Portrait stacks the cards full width; landscape puts them in one `Row(height(IntrinsicSize.Min))` with `weight(1f).fillMaxHeight()` cards, so they have equal widths and heights (Q6). Orientation from `LocalConfiguration`. `verticalScroll` keeps everything reachable at large font scales, and `rememberScrollState` (UI element state, the only `remember` in the screen) keeps the position across rotation.
  - Each card has `semantics(mergeDescendants = true)`, so TalkBack reads "label, value, supporting line" as one item.
  - A centred M3 `Button` "See active issues" / "Ver ocorrências ativas" below the cards calls `onSeeTicketsClick` (Q5). It is 40dp tall visually, with a 48dp minimum touch target (M3 `minimumInteractiveComponentSize`) (review N4).
  - **Known difference (review N1, wording kept by the person):** the figures count every ticket, private ones included (decision 0005), while the button opens the list of PUBLIC active tickets (`publicActive()`). Today they match because all samples are PUBLIC; once private tickets exist, "12 of 30 issues" can lead to a list of 9. At the defence: the brief's "nº total de ocorrências" is not limited to public ones, while the anonymous list is. If this confuses users, rename the button "See public issues" / "Ver ocorrências públicas" (`dashboard_see_tickets`).
  - Previews (private data, `CityFixTheme`): `previewStats` (30 / 12 / 8, 3 days 4 hours) in portrait, landscape and PT landscape; `DashboardStats()` in portrait, landscape and PT; the sample figures (3 / 2 / 1, exactly 3 days) in EN and PT; a single-ticket preview `DashboardStats(1, 0, 1, 30 min)` in EN and PT for "Less than 1 hour", "Based on 1 repair" and "0 of 1 issue". It uses 0 active of 1, not 1 of 1, because a resolved ticket is never active (D-002 contract). The "hours only" branch ("5 hours") has no preview and is checked by reading the code.
  - No stateful wrapper (Q1, as in F-003): D-006 collects `vm.stats`.
  - **Contract for D-006 (Q5 handover to Diogo):** D-005's top bar has no list action, so dashboard -> list goes through this button.
    ```kotlin
    DashboardScreen(
        stats = vm.stats.collectAsStateWithLifecycle().value,
        onSeeTicketsClick = { navController.navigate(TicketListRoute) },
        modifier = Modifier.padding(innerPadding),
    )
    ```
    As for `ListScreen`, check landscape on a device with a camera cutout and add `WindowInsets.displayCutout` padding (or `Scaffold(contentWindowInsets = WindowInsets.safeDrawing)`) if a card sits under the notch (F-003 review N3).
- `ShowScreen(ticket: Ticket?, audience: DetailAudience, modifier)` (`ui/screens/ShowScreen.kt`, F-005): the ticket detail behind `TicketDetailRoute`. Details and reasons: [decision 0007](../decisions/0007-ticket-detail-by-audience.md).
  - `enum class DetailAudience { PUBLIC, REGISTERED, OPERATOR }` lives in the same file (Gate 1: only this screen and D-006's call use it; move it to `model/` only if the Auth task needs it there). `audience` is required, with no default, so every caller states who is looking; `modifier` stays the first optional parameter (lint `ModifierParameter`). The screen branches with an exhaustive `when (audience)`, so a later task that adds messages or coordinates must decide for each audience to compile.
  - Who gets which audience (the screen only receives it; the Auth/ViewModel layer maps role + creator/follower relation to it, review S3):

    | Audience | Who | Brief |
    | --- | --- | --- |
    | `PUBLIC` | anonymous users, and any registered citizen viewing a public ticket they did not create and do not follow | I.1.C; I.2.D ("acesso às funcionalidades do utilizador anónimo") |
    | `REGISTERED` | the registered citizen who created or follows this ticket | I.2.E ("Detalhe de uma ocorrência, com acesso a troca de mensagens", on "ocorrências (do utilizador)") |
    | `OPERATOR` | any municipal operator | I.3.E ("visualização integral ... com coordenadas, contactos do autor e histórico completo") |

  - Sections per audience:
    - every audience: header (category as the page heading, fallback string if blank; situation; `StatusBadge`), "Photos and videos", "Approximate location";
    - `REGISTERED` and `OPERATOR` also: "Reported on <date, time>" (skipped if `createdAt == 0L`), the description (skipped if blank) and the status history ("No status changes recorded." when empty). In phase 1 the two render the same thing; messages (I.2.E), exact coordinates and author contacts (I.3.E) are later tasks and go into the `when`.
    - The PUBLIC header is brief I.1.B's list fields (category, situation) plus the status badge. Gate 1 Q2 first left the badge out; the person revised it at review (N2) so the detail matches the public list card, which already shows it.
  - `ticket == null`: a centred "Ticket not found" / "Ocorrência não encontrada" for every audience.
  - Privacy: the screen never reads `creatorId`, `followerIds`, `messages`, `responsibleId`, `justification`, `uploadedById`, `Attachment.file`/`thumbnail`, or coordinates as text. `ticket.location` is read once and goes only to `CoarseLocationMap`. `description` and `statusHistory` are read only in the non-PUBLIC branches. The projection itself stays upstream (decision 0006); see the S4 item in "ViewModel (D-003)" for the two fields the projection does not yet drop.
  - Media: one `MediaTile` per attachment in a horizontally scrolling row (type icon on a `surfaceVariant` box, "Photo"/"Video" and "Not available yet"), or "No photos or videos.". It reads only `attachment.type`. Phase 1 has no real files, so it never draws an image that is not the ticket's; the media task replaces the tile body with Coil. The visible samples have no attachments, so on a device the empty message shows.
  - Map: `CoarseLocationMap` is a **static placeholder** (Gate 1 Q7: no map library, no API key, no manifest change). **Known gap against brief I.1.C** ("mapa com zoom afastado"): there is no real map. See "Shared components" for how to close it.
  - Layout: portrait is one `Column` with `verticalScroll` (16dp padding, 16dp spacing). Landscape is a `Row` of two `weight(1f).fillMaxHeight()` panes, each scrolling on its own: left `DetailsPane` (header, description for non-PUBLIC, media), right `LocationPane` (map, then divider + history for non-PUBLIC). Both orientations use the same two private pane composables. Orientation from `LocalConfiguration`. The two `rememberScrollState()`s are hoisted after the `null` check, so both orientations use the same call sites and the details scroll position survives rotation (review S2).
  - Timestamps: `formatDateTime(epochMillis, locale)` (medium date + short time), locale from `LocalConfiguration.current.locales[0]` so the PT previews show PT dates, device time zone. The history is shown in stored order, oldest first, with no sort: `currentStatus` is a copy of the last entry (decision 0004), so a UI sort could hide a data bug.
  - Accessibility: the category and every section title are headings; each history row and each media tile is one TalkBack item (`semantics(mergeDescendants = true)`); decorative icons have `contentDescription = null` next to a text label; the map box has `cd_ticket_map`. Nothing is interactive. Known nit left (review N6): the map description and caption are two TalkBack stops, and the description promises a map that is not there yet.
  - Previews (private, `CityFixTheme`): PUBLIC and REGISTERED with `previewTicket` (already in anonymous form, 3 history entries, an IMAGE and a VIDEO attachment) in portrait, landscape, PT and PT landscape; not-found in the same four; empty history / no media / no location in EN and PT; a privacy preview with the raw `sample-2` (selected by id) in PUBLIC.
  - **Contract for D-006:**
    ```kotlin
    composable<TicketDetailRoute> { backStackEntry ->
        val id = backStackEntry.toRoute<TicketDetailRoute>().ticketId
        ShowScreen(
            ticket = tickets.firstOrNull { it.id == id },
            audience = DetailAudience.PUBLIC,
            modifier = Modifier.padding(innerPadding),
        )
    }
    ```
    `tickets` is the collected `vm.tickets` (see "ViewModel (D-003)"), so the screen updates when the list changes and shows "not found" if the ticket leaves the public list. As for the other screens, check landscape on a device with a camera cutout (F-003 review N3).
  - Other handovers to Diogo: the projection change (S4, "ViewModel (D-003)"); optionally an IMAGE and a VIDEO attachment (`file = ""`) on a visible sample so the tiles show on a device; the Auth task maps role + creator/follower relation to `DetailAudience` as in the table above.

## External services
Firebase Auth (+MFA), Cloud Firestore (offline persistence), Firebase Storage, FCM; Android location/geofencing and sensors.

## Roles
Anonymous citizen (public, active issues only, no personal data), registered citizen, municipal operator.

## References
Brief: `docs/brief/AMov.ProjetoKotlin_PT.pdf`. Notes for the technical report: `docs/report/notes.md`.

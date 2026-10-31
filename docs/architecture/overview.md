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
- `utils/`: stateless helpers (image compression and thumbnails, distance maths). Code that `model/` imports (`haversineMeters` in `utils/Distance.kt`) must stay pure Kotlin, with no Android imports; the only package dependency is `model -> utils`.

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
  - the runtime `kotlinx-serialization-core` 1.9.0 provides `@Serializable`. It is pinned to 1.9.0 because that line is built for Kotlin 2.2, matching the project's Kotlin 2.2.10. Bump it together with Kotlin, not alone.
- Checking the plugin ran: with AGP 9 built-in Kotlin the compiled classes are under `app/build/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes/` (not `app/build/tmp/kotlin-classes/debug/`). `pt/isec/amov/cityfix/ui/navigation/TicketDetailRoute$$serializer.class` must exist there.
- Accepted lint warnings: lint reports newer versions as `NewerVersionAvailable` (not `GradleDependency`). D-004 adds two, both accepted while Kotlin stays at 2.2.10: `kotlinx-serialization-core` (1.9.0 vs newer) and the `org.jetbrains.kotlin.plugin.serialization` plugin (2.2.10 vs newer, the same warning `kotlin-compose` already has).
- **Contract for D-006:**
  - Add `androidx.navigation:navigation-compose` (suggested 2.9.8) through the version catalog. D-004 does not add it.
  - After adding it, run `./gradlew :app:dependencies --configuration debugRuntimeClasspath` and check that `kotlinx-serialization-core` still resolves to 1.9.x. `navigation-compose` depends on it too, and Gradle picks the highest version requested, so a newer one would silently move the runtime past the Kotlin 2.2 pin.
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

## External services
Firebase Auth (+MFA), Cloud Firestore (offline persistence), Firebase Storage, FCM; Android location/geofencing and sensors.

## Roles
Anonymous citizen (public, active issues only, no personal data), registered citizen, municipal operator.

## References
Brief: `docs/brief/AMov.ProjetoKotlin_PT.pdf`. Notes for the technical report: `docs/report/notes.md`.

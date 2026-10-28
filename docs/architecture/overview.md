# Architecture overview

Seeded from the factory manifest (A1-A4). Update as the app takes shape.

## Components
- One Android app, Kotlin, Jetpack Compose + Material3. minSdk 24, targetSdk 37. Package `pt.isec.amov.cityfix`.
- Single Gradle module `:app`.

## Layers (MVVM)
- `ui/screens/`: stateful screens (collect `StateFlow<UiState>`) and their stateless content composables; `ui/components/` reusable composables; `ui/navigation/` routes; `ui/theme/` theme.
- `viewmodel/`: ViewModels and their `UiState`.
- `data/`: repositories over Firebase Auth, Firestore, Storage, FCM; WorkManager workers for media upload.
- `model/`: entities, enums and pure rules (ticket lifecycle, 5-day reopen, 10 m grouping, 10 km anonymous radius, 20 m geofence).
- `utils/`: stateless helpers (date formatting; later image compression and thumbnails, distance maths).

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
- Fields an anonymous view must never show: `Ticket.location` (coarse map zoom only), `creatorId`, `followerIds`, `messages`, and all `Citizen` personal data. The 10 km radius of `Municipality` limits where anonymous users may be, not where tickets may be.

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

## UI foundation
- Theme: the course template trio in `ui/theme/` (`Color.kt`, `Theme.kt`, `Type.kt`). Light only (dark mode is not required by the brief): `CityFixTheme` has one `lightColorScheme` with primary `CityFixBlue`, secondary `CityFixTeal`, tertiary `CityFixOrange`. `Type.kt` stays the template. Screens read `MaterialTheme.colorScheme` and `MaterialTheme.typography`, never raw colours.
- `dynamicColor` defaults to `false`, the one change to the template: on Android 12+ dynamic colour would replace the CityFix colours with the wallpaper palette.
- `MainActivity` calls `enableEdgeToEdge` with `SystemBarStyle.light(TRANSPARENT, TRANSPARENT)` for status and navigation bars. The default style follows the device, so in device dark mode it would draw light icons over the light-only UI; the light style keeps them dark and readable.
- Strings: English default in `values/strings.xml`, Portuguese in `values-pt/strings.xml`, same snake_case keys; the device language picks the file, falling back to English. Composables use `stringResource(R.string.x)`, no literals. `app_name` is `translatable="false"`. Each task adds its strings with the screen that uses them, EN and PT in the same commit (lint `MissingTranslation` catches gaps).
- Sizes: `dp` for layout, `sp` for text (so text follows the font scale).
- Touch targets: 48dp through Material 3 components (`Button`, `IconButton`, ...), which enforce the minimum size; no custom size constants.
- `contentDescription`: meaningful icons use `stringResource(...)`; `null` only for decorative icons next to text that already says the same.
- Raw colour exception: `CityFixGreen` (`Color.kt`) is used directly by the RESOLVED `StatusBadge`, with `Color.White` text. It is the one place outside `Theme.kt` that reads a raw colour, because the M3 colour scheme has no green role and "resolved" needs a colour distinct from primary, secondary and tertiary.
- Later (report): dark theme, in-app language selector (per-app locales), dynamic colour toggle in settings, custom container colours.

### Shared components
Stateless composables in `ui/components/` (data in, events out; no ViewModel, `remember` or Firebase), reused by the list (F-003) and the detail screen (F-005). Task F-002.
- `StatusBadge(status)`: non-clickable `Surface` + `Text` with one colour and the localised label per `TicketStatus`. Colours: OPEN `primary`, INFO_REQUESTED `tertiary`, IN_PROGRESS `secondary`, RESOLVED `CityFixGreen` (see the exception above), CLOSED `inverseSurface`/`inverseOnSurface` (stands out on the card container). The label is always shown, so status is never conveyed by colour only.
- `TicketCard(ticket, isExpanded, onClick)`: M3 `Card(onClick)`. Collapsed it shows a placeholder thumbnail (vector drawable on a `surface` box), the category name (fallback string if blank), the situation and the `StatusBadge`, the content brief I.1.B lists for the ticket list. Expanded (`isExpanded = true`) it also shows the description (up to 3 lines, skipped if blank) and "Reported on <date>" (skipped if `createdAt == 0L`). `isExpanded` is the global top-bar toggle owned by the screen (D-005/D-006), not per-card state. The card reads only `categoryName`, `situation`, `currentStatus`, `description` and `createdAt`; never `location`, `creatorId`, `followerIds` or `messages`.
- Enum labels: `statusLabelRes` (`StatusBadge.kt`) and `situationLabelRes` (`TicketCard.kt`) map each constant to its `R.string` id (plain `Int`) with an exhaustive `when`, never `.name`; a new constant does not compile until it has a label. PT labels are the brief's wording (Aberto, Pedido de informações, Em resolução, Resolvido, Encerrado; Dano, Avaria, Destruição, Roubo).
- Dates: `utils/formatDate(epochMillis)` uses `java.text.DateFormat.MEDIUM` in the device locale (works on minSdk 24 without desugaring).

## External services
Firebase Auth (+MFA), Cloud Firestore (offline persistence), Firebase Storage, FCM; Android location/geofencing and sensors.

## Roles
Anonymous citizen (public, active issues only, no personal data), registered citizen, municipal operator.

## References
Brief: `docs/brief/AMov.ProjetoKotlin_PT.pdf`. Notes for the technical report: `docs/report/notes.md`.

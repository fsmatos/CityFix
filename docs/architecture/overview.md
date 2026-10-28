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

- `utils/`: stateless helpers (date formatting, distance maths; later image compression and thumbnails). Code that `model/` imports (`haversineMeters` in `utils/Distance.kt`) must stay pure Kotlin, with no Android imports; the only package dependency is `model -> utils`.

## External services
Firebase Auth (+MFA), Cloud Firestore (offline persistence), Firebase Storage, FCM; Android location/geofencing and sensors.

## Roles
Anonymous citizen (public, active issues only, no personal data), registered citizen, municipal operator.

## References
Brief: `docs/brief/AMov.ProjetoKotlin_PT.pdf`. Notes for the technical report: `docs/report/notes.md`.

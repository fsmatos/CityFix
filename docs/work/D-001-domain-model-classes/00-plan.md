
# D-001: Domain classes and enums
Type: feature · Task file: docs/tasks/phase-1/D-001-model-classes.md
Labels: none

## Problem
`app/src/main/java/pt/isec/amov/cityfix/model/` is empty (only `.gitkeep`). Every phase-1 task of Francisco (F-002..F-006) and Diogo (D-002, D-003) needs the domain types from the Option A diagram (`docs/architecture/diagrams/CityFix_Dominio_OpcaoA.png`) in English. Without them nobody else can start, so this task goes first and `dev/Diogo` is pushed right after it.

## Outcome
`model/` holds pure Kotlin types (no Android, Compose or Firebase imports) that every phase-1 task can use:
- Enums: `TicketStatus`, `Situation`, `Scope`, `AttachmentType`, `UploadState`, `RegistrationStatus`.
- Value type: `Coordinates`.
- Users: `sealed interface User` with the data classes `Citizen` and `Operator`.
- `Municipality`, `Category`, `Ticket`, `Message`, `StatusHistory`, `Attachment`.

Every class can be built with no arguments (`Ticket()`, `Citizen()`, ...), which keeps previews and sample data short. The shape is ready for Firestore later (String ids, enums stored by name, plain numbers for time, references by id) but there is no Firebase code yet.

## Acceptance checks
- [ ] `./gradlew assembleDebug` and `./gradlew lint` pass after every slice.
- [ ] Every class compiles and can be built with no arguments. Check by compiling a throwaway file that calls `Ticket()`, `Citizen()`, `Operator()`, `Municipality()`, `Category()`, `Message()`, `StatusHistory()`, `Attachment()`, `Coordinates()`, then deleting it before committing.
- [ ] `User` is the base (a sealed interface). `Citizen` and `Operator` are data classes that implement it. There is no `role` field and no single `User` class with a role.
- [ ] `Ticket` follows Option A: one `creatorId` plus a `followerIds` list. There is no `Group`/`Grupo` class anywhere (`grep -ri "class Group\|class Grupo" app/src` finds nothing).
- [ ] `Ticket.currentStatus` exists next to `statusHistory`, and its KDoc gives the reason (lists and filters do not read the history). The reason is also in `03-docs-plan.md` for the report.
- [ ] No file in `model/` imports `android.*`, `androidx.*`, `com.google.firebase.*` or `kotlinx.serialization.*` (`grep -r "^import" app/src/main/java/pt/isec/amov/cityfix/model/` shows only `kotlin`/`java` imports, or none).
- [ ] Enum constants match the task spec exactly: `TicketStatus { OPEN, INFO_REQUESTED, IN_PROGRESS, RESOLVED, CLOSED }`, `Situation { DAMAGE, BREAKDOWN, DESTRUCTION, THEFT }`, `Scope { PUBLIC, PRIVATE }`.

## Approach
**Pure, immutable data classes with defaults.** Every property is a `val` with a default value. Kotlin then generates a public no-arg JVM constructor (all parameters have defaults), which covers both "build with no arguments" and Firestore's no-arg constructor requirement. Collections default to `emptyList()`. Optional fields from the diagram (`photo?`, `thumbnail?`, `justification?`) are nullable and default to `null`.

**Users: `sealed interface User`.** It declares `id`, `name`, `nif`, `email`, `phone` and `photo: String?`. `Citizen` and `Operator` are `data class`es that override them. `Citizen` adds `georeferencing: Coordinates`, `registrationStatus: RegistrationStatus` and `impactDetection: Boolean`.
- Why sealed: a `when (user)` over Citizen/Operator is exhaustive, so the role comes from the type and no `role` field is needed.
- Alternatives rejected: an `open class User` (data classes cannot cleanly extend a class with constructor properties, and you get duplicated parameters and `equals` pitfalls), and a single `User(role = ...)` (forbidden by the task).
- In the Firebase phase the document stores a `role` string written by the data layer (phase-1 README, risk 2). The model does not carry it.

**Ids are `String`, default `""`.** Every entity that becomes a Firestore document (or an item in a keyed list) has `val id: String = ""`. Later this is the Firestore document id, or the Auth uid for users. F-003 needs `key = { it.id }`, and D-004's `TicketDetailRoute(ticketId)` should use `String` to match.

**References to other entities are ids, not embedded objects.** Examples: `Ticket.creatorId`, `Ticket.followerIds`, `Message.authorId`, `StatusHistory.responsibleId`, `Attachment.uploadedById`.
- Privacy: embedding a `Citizen` inside a `Ticket` would copy name, NIF, email, phone and home coordinates into every ticket. That breaks the anonymous rule and Firestore cannot hide fields inside a document.
- Firestore: queries like `whereArrayContains("followerIds", uid)` work on ids.
- Size: the document stays small.
- Category is the exception. The ticket stores `categoryId` plus a `categoryName` snapshot, so the list and card (F-002/F-003) can show the category offline without a categories lookup. See open question Q1.

**Time is `Long` epoch milliseconds (UTC), default `0L`.** This applies to `createdAt`, `sentAt` and `dateTime`.
- Alternatives considered:
  - `com.google.firebase.Timestamp` breaks the "no Firebase in `model/`" rule.
  - `java.time.Instant` needs API 26 or core-library desugaring (minSdk is 24), and Firestore does not map it.
  - `java.util.Date` is mapped by Firestore to a Timestamp, but it is mutable and clumsier for the 5-day and average-repair-time maths.
- `Long` is pure and immutable, makes the arithmetic in D-002 trivial, and Firestore stores it as a number.
- The `0L` default keeps previews deterministic (no clock read in a default). Callers set the real time. See open question Q2.

**Enums are plain Kotlin enums with no labels or resources.** Firestore stores enums by `name`, so constant names become a persisted contract and must not be renamed. UI labels come from `strings.xml` and are mapped in the UI (F-002 `StatusBadge`), so `model/` never touches `R`.

**Default values:**
| Type | Defaults |
| --- | --- |
| `TicketStatus` | `OPEN` |
| `Situation` | `DAMAGE` |
| `Scope` | `PRIVATE` (privacy by default; see Q3) |
| `AttachmentType` | `IMAGE` (values `IMAGE`, `VIDEO`) |
| `UploadState` | `PENDING` (values `PENDING`, `UPLOADING`, `UPLOADED`, `FAILED`, from the diagram's PENDENTE, A_ENVIAR, ENVIADO, FALHOU) |
| `RegistrationStatus` | `PENDING` (values `PENDING`, `ACTIVE`) |
| `Coordinates` | `latitude = 0.0`, `longitude = 0.0` |
| `Municipality` | `radiusKm = 10.0` (from the brief); other fields empty |
| `Category` | `active = true` |
| `Citizen` | `impactDetection = false` |

**`currentStatus` stays on `Ticket` on purpose.** It duplicates `statusHistory.last().status`. Reasons:
- The list, the dashboard (`% active`, `publicActive()` in D-002) and filters read one field instead of scanning the history.
- In Firestore, history will likely be a subcollection, and you cannot query "last element of a list". `whereEqualTo("currentStatus", ...)` needs a top-level field.
- The cost: the two must change together. Every status change appends a `StatusHistory` entry and sets `currentStatus` in the same write (a pure helper later, and a batch/transaction in Firebase). This task only adds the field and the KDoc.

**Ticket parts are lists on `Ticket` for phase 1.** These are `messages`, `statusHistory` and `attachments`, as in the task spec and matching the composition in the diagram. Whether they become subcollections is a Firebase-phase decision. It is noted, not acted on.

**File layout:** one top-level type per file, file named after the type. This is easier to find and explain at the defence.

## Architecture impact
- New files only, all in `model/`. No new dependencies, no Gradle changes. The version catalog has no serialization or Firebase entries, and none are needed here.
- Defines the shared vocabulary that F-002..F-006, D-002, D-003 and D-004 compile against. After this task, renaming a property is a cross-branch change.
- The diagram's associations are mapped as follows:

| Diagram association | Model field |
| --- | --- |
| `cria` | `Ticket.creatorId` |
| `acompanha` | `Ticket.followerIds` |
| `classificado em` | `Ticket.categoryId` (+ `categoryName`) |
| `autor` | `Message.authorId` |
| `responsavel` | `StatusHistory.responsibleId` |
| `carregado por` | `Attachment.uploadedById` |
| `define` | `Category.municipalityId`? (Q4) |
| `pertence a` | `Operator.municipalityId`? (Q4) |
| `valida` | `Citizen.validatedById`? (Q4) |
| `gere` | (not mapped yet, Q4) |

## Failure modes
- **Default ids collide.** Several `Ticket()`s all have `id = ""`. A `LazyColumn` with `key = { it.id }` crashes on duplicate keys. Sample data (D-003) and previews must set distinct ids. This goes in the KDoc of `id`.
- **Default `Coordinates()` is (0, 0)** ("Null Island", in the Atlantic). Distance code (D-002, 10 m grouping and 10 km radius) treats it as a real point. This is acceptable for phase 1 with explicit sample coordinates. Q5 asks whether a nullable location is preferred.
- **A default `Ticket()` breaks the diagram's `1..*` multiplicities** (`statusHistory` and `attachments` are empty, and `currentStatus = OPEN` has no matching history entry). That is the price of no-arg construction. Enforcing the invariants belongs to the creation logic (D-002 and later), not to the constructor. Documented in KDoc.
- **Firestore `toObject()` and `val`.** Firestore's class mapper fills objects through setters or public fields. Kotlin `val` properties have neither, so the values silently stay at their defaults. The no-arg constructor is enough for writing. Reading needs either `var`s or a mapping step in `data/`. This task keeps `val` (immutable state for `UiState`). The Firebase phase decides (Q6).
- **Sealed interface and Firestore.** `toObject(User::class.java)` cannot pick the subclass. The data layer must read `role` and call `toObject(Citizen::class.java)` or `toObject(Operator::class.java)`. Known and accepted (phase-1 README, risk 2).

## Risks and open questions
Risk: Francisco starts F-002..F-006 against these names. If he has guessed other names (`ticket.category.name`, `ticket.creator`), his code won't compile after merging. Mitigation: push `dev/Diogo` right after the last slice and send him the field list from `01-coding-plan.md` (Interfaces).

Open questions for the person:
1. **Q1 Category on Ticket.** Recommended: `categoryId: String` + `categoryName: String` (id reference plus a display snapshot; a rename by the operator does not rewrite old tickets). Alternatives: embed `category: Category` (closest to the spec's wording "category", simplest for phase 1, but copies `active` and goes stale), or `categoryId` only (needs a category lookup everywhere, and phase 1 has no category source). This affects F-002/F-003/F-005/F-006.
2. **Q2 Time representation.** Recommended: `Long` epoch millis with default `0L`. Alternatives: `java.util.Date` (maps to Firestore Timestamp directly), or default `System.currentTimeMillis()` (handy, but makes previews non-deterministic and `Ticket() != Ticket()`).
3. **Q3 Default `Scope`.** Recommended: `PRIVATE`, so a ticket that forgets to set scope is never shown to anonymous users. Alternative: `PUBLIC` (more previews work without setting it).
4. **Q4 Diagram associations not in the task spec.** Should this task also add `Operator.municipalityId`, `Category.municipalityId`, `Citizen.validatedById: String?` and a "managed by" `Ticket.operatorIds` (the `gere` association)? Recommended: leave them out now (phase 1 has one municipality, and the spec does not list them) and add them in the Firebase/operator tasks. They are cheap to add if you prefer the diagram to match one-to-one.
5. **Q5 `Ticket.location` nullable?** Recommended: non-null `Coordinates()` (the brief makes location mandatory). Alternative: `Coordinates?` to tell "not captured yet" apart from (0, 0) while the report form is filled in.
6. **Q6 `val` vs `var` for Firestore reading.** Recommended: `val` now. The Firebase phase maps in `data/`, either by hand from the document map or through small `var` DTOs. Alternative: `var` everywhere now so `toObject()` works directly, at the cost of mutable entities inside `UiState`.
7. **Q7 Is the creator also in `followerIds`?** Recommended: no. `followerIds` holds only the extra citizens, and notifications go to `creatorId + followerIds`. Note for D-002/Firebase: "my tickets" then needs two queries, or a later `participantIds` field.
8. **Q8 `Attachment.file` meaning.** Recommended: a `String` holding a local content/file URI before upload and the Storage path or URL after it (`android.net.Uri` is not allowed in `model/`). The README's risk 1 (photo from the report vs resolution proof) is left for the Firebase phase.

Coordination note (not for this task): D-002 and D-003 still list unit tests in their acceptance checks, which conflicts with the project's no-tests rule. Fix those task files when they are planned.

## Out of scope
- Business rules: the ticket state machine and allowed transitions, the 5-day reopen window, 10 m grouping (Haversine), the 10 km anonymous radius, the 20 m geofence, the Pr. Republica centre coordinates. These belong to D-002 and later tasks.
- `TicketsList`, `DashboardStats` (D-002), sample data (D-003), routes (D-004).
- UI labels and string resources for enums (F-001/F-002).
- Firebase: DTOs, mappers, the `role` field, FCM token, public reduced-field collection, NIF uniqueness documents, attachment phase or proof flag.
- Kotlin Serialization annotations on model classes (routes carry only the id).
- Changing the phase-1 task files or the diagram.

## Slices
Each slice leaves `./gradlew assembleDebug` green and is one commit.
1. **Enums.** Add the six enums and remove `model/.gitkeep`. `feat(D-001): add domain enums`
2. **Coordinates and users.** Add `Coordinates`, `User` (sealed interface), `Citizen` and `Operator`. `feat(D-001): add coordinates and user types`
3. **Municipality and category.** Add `Municipality` and `Category`. `feat(D-001): add municipality and category`
4. **Ticket parts.** Add `Message`, `StatusHistory` and `Attachment`. `feat(D-001): add message, status history and attachment`
5. **Ticket.** Add `Ticket` (Option A followers, `currentStatus` with KDoc), then do the throwaway no-arg compile check. `feat(D-001): add ticket with followers`

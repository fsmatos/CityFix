# D-001: Coding plan

All paths are relative to `app/src/main/java/pt/isec/amov/cityfix/model/`, package `pt.isec.amov.cityfix.model`. Recommended answers to Q1-Q8 in `00-plan.md` are assumed. If the person picks something else at Gate 1, change the matching lines below before coding.

## Files
| Path | Change | Why |
| --- | --- | --- |
| `.gitkeep` | delete (slice 1) | Folder now has real files |
| `TicketStatus.kt` | new | Lifecycle states from the brief (section II) |
| `Situation.kt` | new | Dano / Avaria / Destruicao / Roubo |
| `Scope.kt` | new | Publico / Privado |
| `AttachmentType.kt` | new | Imagem / Video |
| `UploadState.kt` | new | WorkManager upload progress per attachment |
| `RegistrationStatus.kt` | new | Citizen registration pending until an operator validates it |
| `Coordinates.kt` | new | One location type for Ticket, Citizen and Municipality |
| `User.kt` | new | Sealed base of the user types |
| `Citizen.kt` | new | Registered citizen (Municipe) |
| `Operator.kt` | new | Municipal operator |
| `Municipality.kt` | new | Name, email domain, centre, radius |
| `Category.kt` | new | Issue category defined by the municipality |
| `Message.kt` | new | Ticket message thread |
| `StatusHistory.kt` | new | One entry per status transition |
| `Attachment.kt` | new | Image or video with thumbnail and upload state |
| `Ticket.kt` | new | Option A ticket |

## Interfaces
This is the contract Francisco and the later D-tasks consume. Send it to him when `dev/Diogo` is pushed.

```kotlin
enum class TicketStatus { OPEN, INFO_REQUESTED, IN_PROGRESS, RESOLVED, CLOSED }
enum class Situation { DAMAGE, BREAKDOWN, DESTRUCTION, THEFT }
enum class Scope { PUBLIC, PRIVATE }
enum class AttachmentType { IMAGE, VIDEO }
enum class UploadState { PENDING, UPLOADING, UPLOADED, FAILED }
enum class RegistrationStatus { PENDING, ACTIVE }

data class Coordinates(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
)

sealed interface User {
    val id: String
    val name: String
    val nif: String
    val email: String
    val phone: String
    val photo: String?
}

data class Citizen(
    override val id: String = "",
    override val name: String = "",
    override val nif: String = "",
    override val email: String = "",
    override val phone: String = "",
    override val photo: String? = null,
    val georeferencing: Coordinates = Coordinates(),
    val registrationStatus: RegistrationStatus = RegistrationStatus.PENDING,
    val impactDetection: Boolean = false,
) : User

data class Operator(
    override val id: String = "",
    override val name: String = "",
    override val nif: String = "",
    override val email: String = "",
    override val phone: String = "",
    override val photo: String? = null,
) : User

data class Municipality(
    val id: String = "",
    val name: String = "",
    val emailDomain: String = "",
    val center: Coordinates = Coordinates(),
    val radiusKm: Double = 10.0,
)

data class Category(
    val id: String = "",
    val name: String = "",
    val active: Boolean = true,
)

data class Message(
    val id: String = "",
    val text: String = "",
    val sentAt: Long = 0L,          // epoch millis, UTC
    val authorId: String = "",
)

data class StatusHistory(
    val status: TicketStatus = TicketStatus.OPEN,
    val dateTime: Long = 0L,        // epoch millis, UTC
    val justification: String? = null,
    val responsibleId: String = "",
)

data class Attachment(
    val id: String = "",
    val type: AttachmentType = AttachmentType.IMAGE,
    val file: String = "",          // local URI before upload, Storage path/URL after
    val thumbnail: String? = null,
    val uploadState: UploadState = UploadState.PENDING,
    val uploadedById: String = "",
)

data class Ticket(
    val id: String = "",
    val description: String = "",
    val situation: Situation = Situation.DAMAGE,
    val scope: Scope = Scope.PRIVATE,
    val location: Coordinates = Coordinates(),
    val currentStatus: TicketStatus = TicketStatus.OPEN,
    val createdAt: Long = 0L,       // epoch millis, UTC
    val categoryId: String = "",
    val categoryName: String = "",
    val creatorId: String = "",
    val followerIds: List<String> = emptyList(),
    val attachments: List<Attachment> = emptyList(),
    val messages: List<Message> = emptyList(),
    val statusHistory: List<StatusHistory> = emptyList(),
)
```

Notes on the contract:
- `StatusHistory` has no `id`. It is ordered by position or `dateTime` and never rendered as a keyed list item on its own. Add one if the Firebase phase turns it into a subcollection.
- KDoc to write (short, one or two lines each):
  - `Ticket.currentStatus`: "Denormalised copy of the last `statusHistory` entry. Kept so lists, filters and Firestore queries read one field. Update both together."
  - `Ticket.followerIds`: "Option A: citizens who reported the same category + situation within 10 m. They follow this ticket instead of opening a new one. Does not include `creatorId`."
  - `id` fields: "Firestore document id (Auth uid for users). Empty until persisted; sample data must set distinct ids."
  - Time fields: "Epoch milliseconds, UTC."
  - `Ticket` class: "Defaults allow `Ticket()` for previews. A real ticket has at least one attachment and one history entry (brief); creation logic enforces that."
  - `User`: "Sealed: the concrete type is the role (no role field). The Firebase layer stores a `role` string."
  - `Citizen.georeferencing`: "Home location given at registration; personal data, never shown to anonymous users; not editable."

## Steps
Before every commit, run `./gradlew assembleDebug` (it must pass), run `./gradlew lint` (no new errors), and check that the `model/` files have no `android`/`androidx`/`firebase`/`kotlinx.serialization` imports.

1. **Slice 1: enums.**
   - Create the six enum files exactly as in Interfaces, each with a one-line KDoc that names the brief's Portuguese term (for example "Situacao in the brief").
   - Delete `model/.gitkeep`.
   - Commit: `feat(D-001): add domain enums`.
   - Verify: build green, and the constant names match the task spec.
2. **Slice 2: coordinates and users.**
   - Create `Coordinates.kt`, `User.kt`, `Citizen.kt`, `Operator.kt`.
   - Commit: `feat(D-001): add coordinates and user types`.
   - Verify: build green. `User` is `sealed interface`, both subclasses are `data class`, there is no `role` property.
3. **Slice 3: municipality and category.**
   - Create `Municipality.kt`, `Category.kt`.
   - Commit: `feat(D-001): add municipality and category`.
   - Verify: build green.
4. **Slice 4: ticket parts.**
   - Create `Message.kt`, `StatusHistory.kt`, `Attachment.kt`.
   - Commit: `feat(D-001): add message, status history and attachment`.
   - Verify: build green.
5. **Slice 5: ticket.**
   - Create `Ticket.kt` with the KDoc above.
   - Run the no-arg check: temporarily add `model/NoArgCheck.kt` with a top-level `private val check = listOf(Ticket(), Citizen(), Operator(), Municipality(), Category(), Message(), StatusHistory(), Attachment(), Coordinates())`, run `./gradlew assembleDebug`, then delete the file. It is never committed.
   - Run `grep -ri "Group\|Grupo" app/src/main` and expect no class.
   - Commit: `feat(D-001): add ticket with followers`.
   - Verify: build and lint green, the no-arg check compiled, `git status` clean apart from the commit.

After slice 5, push `dev/Diogo` with `/push-task` and tell Francisco that D-001 is ready to merge.

## Conventions
- Pure Kotlin in `model/`: no Android, Compose, Firebase or serialization imports, and no `R`. (kotlin-conventions: "Pure Kotlin entities in `model/`, free of UI frameworks".)
- Immutable: `val` only, `List` (not `MutableList`), so instances are safe inside `UiState`/`StateFlow`.
- Every constructor parameter has a default, so the class has a no-arg constructor.
- English identifiers. KDoc may name the Portuguese term from the brief/diagram so the defence can link the two.
- One top-level type per file, file named after the type. Trailing commas in multi-line parameter lists.
- Enum constant names are persisted (Firestore stores `name`): never rename them after this task without a migration note.
- Conventional Commits with the task ID, no attribution lines.

## Do not touch
- `docs/work/D-001-domain-model-classes/state.md` (the orchestrator updates it).
- `gradle/libs.versions.toml`, `app/build.gradle.kts`: no dependency or plugin changes.
- `ui/`, `viewmodel/`, `data/`, `utils/`, `MainActivity.kt`.
- `google-services.json`, Firestore/Storage rules (none exist yet; do not add them).
- Task files in `docs/tasks/` and the diagrams.

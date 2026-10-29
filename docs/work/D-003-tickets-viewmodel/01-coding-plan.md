# D-003: Coding plan

## Files
| Path | Change | Why |
| --- | --- | --- |
| `app/src/main/java/pt/isec/amov/cityfix/model/TicketPrivacy.kt` | new | Pure rule `Ticket.toAnonymousView()`: anonymous users never get author data or exact coordinates (CLAUDE.md, decision 0003, D-002 review S4) |
| `app/src/main/java/pt/isec/amov/cityfix/data/SampleTickets.kt` | new | The 3 phase-1 sample tickets (no Firebase) |
| `app/src/main/java/pt/isec/amov/cityfix/data/.gitkeep` | delete | The folder now has a real file |
| `app/src/main/java/pt/isec/amov/cityfix/viewmodel/TicketsViewModel.kt` | new | `StateFlow`s of tickets and stats, `getTicketById`, `reportTicket` |
| `app/src/main/java/pt/isec/amov/cityfix/viewmodel/.gitkeep` | delete | The folder now has a real file |

No Gradle, version catalog, resource or test changes.

## Interfaces
These are the signatures; the bodies are a guide. Keep the KDoc short and in plain English.

```kotlin
// model/TicketPrivacy.kt
package pt.isec.amov.cityfix.model

import kotlin.math.pow
import kotlin.math.round

/** Decimal places kept in a location shown to anonymous users: 2 is about 1 km (brief I.1.C, coarse zoom). */
const val ANONYMOUS_COORDINATE_DECIMALS = 2

/**
 * Copy of this ticket that is safe for anonymous users (CLAUDE.md, decision 0003).
 * Blanks creatorId, followerIds, messages, every StatusHistory.responsibleId (the first OPEN
 * entry holds the creator's id) and every Attachment.uploadedById, and rounds the location to
 * [ANONYMOUS_COORDINATE_DECIMALS]. Update this when Ticket gains a field with personal data.
 */
fun Ticket.toAnonymousView(): Ticket = copy(
    location = location.coarse(),
    creatorId = "",
    followerIds = emptyList(),
    messages = emptyList(),
    statusHistory = statusHistory.map { it.copy(responsibleId = "") },
    attachments = attachments.map { it.copy(uploadedById = "") },
)

private fun Coordinates.coarse(): Coordinates {
    val scale = 10.0.pow(ANONYMOUS_COORDINATE_DECIMALS)
    return Coordinates(round(latitude * scale) / scale, round(longitude * scale) / scale)
}
```
- (0, 0) stays (0, 0), so the "unset" meaning is kept.

```kotlin
// data/SampleTickets.kt
package pt.isec.amov.cityfix.data

/** Phase-1 sample tickets near Pr. Republica, Coimbra (no Firebase yet). Times are fixed UTC epoch ms. */
object SampleTickets {
    val all: List<Ticket> = listOf(/* sample-1, sample-2, sample-3 */)
}
```

Sample data. Every ticket has `scope = Scope.PUBLIC`, and `currentStatus` is the status of the last history entry. Before committing, the implementer recomputes each epoch value from its UTC date (2026-10-01T00:00Z = 1_790_812_800_000).

| id | status | situation | categoryId / categoryName | description | location | createdAt (UTC) | history (status @ UTC, responsibleId) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `sample-1` | OPEN | DAMAGE | `pavement` / `Pavement` | `Pothole in the road near Praca da Republica` | 40.2100, -8.4180 | 2026-10-20 09:00 = `1_792_486_800_000` | OPEN @ createdAt, `citizen-1` |
| `sample-2` | IN_PROGRESS | BREAKDOWN | `lighting` / `Public lighting` | `Street light out on Rua Alexandre Herculano` | 40.2075, -8.4210 | 2026-10-15 18:30 = `1_792_089_000_000` | OPEN @ createdAt, `citizen-2`; IN_PROGRESS @ 2026-10-17 10:00 = `1_792_231_200_000`, `operator-1` |
| `sample-3` | RESOLVED | DESTRUCTION | `furniture` / `Urban furniture` | `Broken bench in Jardim da Sereia` | 40.2110, -8.4225 | 2026-10-01 08:00 = `1_790_841_600_000` | OPEN @ createdAt, `citizen-1`; IN_PROGRESS @ 2026-10-02 08:00 = `1_790_928_000_000`, `operator-1`; RESOLVED @ 2026-10-04 08:00 = `1_791_100_800_000`, `operator-1` |

- `creatorId` is the responsibleId of the first OPEN entry.
- `sample-2` has `followerIds = listOf("citizen-3")`, so the projection has something to strip.
- `sample-1` has one `Message(text = "Still there this morning.", sentAt = createdAt + 3_600_000, authorId = "citizen-1")`.
- `sample-3` has one `Attachment(id = "sample-3-photo", file = "", uploadedById = "citizen-1")`.
- Expected `stats()`: total 3, active 2, resolved 1, `averageRepairTimeMillis` = `1_791_100_800_000 - 1_790_841_600_000` = `259_200_000` (3 days).
- Expected `publicActive()` order: `sample-1`, `sample-2`.

```kotlin
// viewmodel/TicketsViewModel.kt
package pt.isec.amov.cityfix.viewmodel

/**
 * Phase-1 tickets for the UI. Owns an in-memory TicketsList seeded with SampleTickets; composables
 * only see the read-only flows below. Survives rotation as a ViewModel. Create it once above the
 * NavHost (D-006) so every screen shares the same instance.
 * Phase 1 has no login, so everything exposed here is the anonymous view.
 */
class TicketsViewModel : ViewModel() {

    private val ticketsList = TicketsList(SampleTickets.all)

    private val _tickets = MutableStateFlow(visibleTickets())
    /** Public active tickets, newest first, projected with toAnonymousView(). */
    val tickets: StateFlow<List<Ticket>> = _tickets.asStateFlow()

    private val _stats = MutableStateFlow(ticketsList.stats())
    /** Dashboard figures over every ticket, private ones included (decision 0005). */
    val stats: StateFlow<DashboardStats> = _stats.asStateFlow()

    /** The visible (projected) ticket with this id, or null: unknown, private, resolved or closed. */
    fun getTicketById(id: String): Ticket? = _tickets.value.firstOrNull { it.id == id }

    // slice 4
    /**
     * Opens a new ticket from the provisional report form (F-006). Returns its id, or null when
     * description or categoryId is blank. No location yet, so no 10 m grouping (00-plan Q8).
     */
    fun reportTicket(
        description: String,
        categoryId: String,
        categoryName: String,
        situation: Situation,
        scope: Scope,
    ): String?

    private fun visibleTickets(): List<Ticket> = ticketsList.publicActive().map { it.toAnonymousView() }

    private fun refresh() {
        _tickets.value = visibleTickets()
        _stats.value = ticketsList.stats()
    }

    companion object {
        /** Phase 1 has no login; stands in for the reporter's user id. */
        const val LOCAL_USER_ID = "local-citizen"
    }
}
```
- Declaration order matters: `ticketsList` must be declared before `_tickets` and `_stats`, because their initialisers read it.
- The class has no constructor parameters, so the default `viewModel()` factory in D-006 can create it.
- `reportTicket` body:
  - return `null` if `description.isBlank() || categoryId.isBlank()`;
  - `val now = System.currentTimeMillis()`;
  - build `Ticket(id = UUID.randomUUID().toString(), description = description.trim(), situation, scope, currentStatus = OPEN, createdAt = now, categoryId, categoryName, creatorId = LOCAL_USER_ID, statusHistory = listOf(StatusHistory(OPEN, now, null, LOCAL_USER_ID)))`. Leave `location` at its default (0, 0): no fake location (Q8);
  - if `!ticketsList.add(ticket)`, return `null`;
  - otherwise `refresh()` and return the id.

## Steps
Before every commit:
- run `./gradlew assembleDebug` and `./gradlew lint` (exit 0, no new lint errors);
- grep the slice's new files for `import android` (allowed only in `viewmodel/`, and only `androidx.lifecycle.ViewModel`; none in `model/` or `data/`).

Write commit messages with no attribution lines, and do not stage `.idea/`.

1. **Anonymous projection.** Create `model/TicketPrivacy.kt` as in Interfaces.
   - Commit: `feat(D-003): add anonymous ticket projection`.
   - Verify: build + lint. By reading the code, check that every personal field listed in `Ticket.kt`, `StatusHistory.kt` and `Attachment.kt` is handled.
2. **Sample tickets.** Create `data/SampleTickets.kt` with the 3 tickets in the table; delete `data/.gitkeep`.
   - Commit: `feat(D-003): add phase-1 sample tickets`.
   - Verify: build + lint. Recompute each epoch value against its UTC date. Check that ids are distinct, that `currentStatus` equals the last history status, that `scope = PUBLIC`, and that the locations are not (0, 0).
3. **ViewModel.** Create `viewmodel/TicketsViewModel.kt` with `tickets`, `stats`, `getTicketById`, `visibleTickets`, `refresh` and `LOCAL_USER_ID` (no `reportTicket` yet); delete `viewmodel/.gitkeep`.
   - Commit: `feat(D-003): add TicketsViewModel with tickets and stats flows`.
   - Verify: build + lint. If `androidx.lifecycle.ViewModel` or `MutableStateFlow` does not resolve, add `androidx-lifecycle-viewmodel-ktx = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-ktx", version.ref = "lifecycleRuntimeKtx" }` to the catalog and `implementation(libs.androidx.lifecycle.viewmodel.ktx)` to `app/build.gradle.kts` in this same commit, and record the deviation in `state.md`.
4. **Report entry point** (only if Gate 1 keeps Q7 = yes). Add `reportTicket` as in Interfaces, with imports for `java.util.UUID`, `StatusHistory`, `Situation`, `Scope` and `TicketStatus`.
   - Commit: `feat(D-003): add reportTicket entry point`.
   - Verify: build + lint. By reading the code: blank input returns `null` before anything is built; the id comes from a UUID; `refresh()` runs only after a successful `add`.

## Conventions
- kotlin-conventions and `.claude/rules/kotlin.md`:
  - immutable `StateFlow` out, events in as functions;
  - private `MutableStateFlow` + `asStateFlow()`;
  - no `GlobalScope`; no coroutine launched at all (everything is synchronous and in memory).
- `model/` and `data/` stay pure Kotlin (only `kotlin.*` and `java.util.UUID`, no `android.*`).
- Sample descriptions and category names are data, like Firestore content later, not UI strings. Keep them in English and ASCII. Screens map enums to string resources, never to `.name`.
- Null-safe lookups (`firstOrNull`), no `!!`.
- Plain code that both authors can explain: no clever flow operators (`combine`, `stateIn`), just `refresh()`.
- Commits: Conventional Commits with `D-003`, no attribution lines.

## Do not touch
- `model/TicketsList.kt`, `model/DashboardStats.kt`, `model/Ticket.kt` and the other D-001/D-002 model files.
- `utils/Distance.kt`.
- `MainActivity.kt`, `ui/`, resources (D-006 and Francisco's tasks).
- `gradle/libs.versions.toml` and `app/build.gradle.kts`, except the slice-3 fallback above.
- `app/src/test/` and `app/src/androidTest/`: D-003 adds no tests.
- `docs/work/D-003-tickets-viewmodel/state.md` (the orchestrator records phases), `google-services.json`, `.idea/`.

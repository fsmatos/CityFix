# D-002: Coding plan

Main sources live under `app/src/main/java/pt/isec/amov/cityfix/`. This plan applies the Gate 1 decisions (2026-10-28) recorded in `00-plan.md`. In particular, Q3 makes "active" mean OPEN, INFO_REQUESTED or IN_PROGRESS, and Q1 commits the JUnit tests.

## Files
| Path | Change | Why |
| --- | --- | --- |
| `utils/.gitkeep` | delete (slice 1) | The folder now has a real file |
| `utils/Distance.kt` | new | Haversine distance in metres. Reused later by the geofence and the 10 km radius |
| `model/DashboardStats.kt` | new | Values for F-004's dashboard (brief I.1.A) |
| `model/TicketsList.kt` | new | In-memory ticket collection (P07 `ContactsList` pattern), the `Ticket.isActive` predicate, and the 10 m rule and stats |
| `app/src/test/java/pt/isec/amov/cityfix/model/TicketsListTest.kt` | new (slice 6) | JUnit 4 tests for the acceptance checks |

## Interfaces
This is the contract that D-003 and F-004 consume. The `DashboardStats` fields were decided at Gate 1 (Q2). Send the block to Francisco before the push after slice 2.

```kotlin
// utils/Distance.kt  (package pt.isec.amov.cityfix.utils; imports kotlin.math.* only)
/** Mean Earth radius in metres used by [haversineMeters]. */
const val EARTH_RADIUS_METERS = 6_371_000.0

/** Great-circle distance in metres between two WGS84 points given in degrees (Haversine formula). */
fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double

// model/DashboardStats.kt  (package pt.isec.amov.cityfix.model)
/** Anonymous dashboard figures (brief I.1.A). Default instance = no tickets. */
data class DashboardStats(
    val totalTickets: Int = 0,
    /** Tickets not yet repaired: OPEN, INFO_REQUESTED or IN_PROGRESS (see [isActive]). */
    val activeTickets: Int = 0,
    /** Tickets that entered the average: they have a RESOLVED history entry. */
    val resolvedTickets: Int = 0,
    /** Mean of (last RESOLVED dateTime - createdAt), in milliseconds. Null when no ticket was resolved. */
    val averageRepairTimeMillis: Long? = null,
) {
    /** Share of active tickets, 0.0..1.0 (format with NumberFormat.getPercentInstance()). Never NaN. */
    val activeRatio: Double
        get() = if (totalTickets == 0) 0.0 else activeTickets.toDouble() / totalTickets
}

// model/TicketsList.kt  (package pt.isec.amov.cityfix.model)
/**
 * A ticket is active while the problem is not yet repaired: OPEN, INFO_REQUESTED or IN_PROGRESS.
 * RESOLVED and CLOSED are not active (brief I.1.A "% ocorrencias ativas"; brief I.2 "Obs", 20 m
 * geofence on an "ocorrencia ativa"). Shared by publicActive(), findNearby() and stats().
 */
val Ticket.isActive: Boolean
    get() = currentStatus == TicketStatus.OPEN ||
        currentStatus == TicketStatus.INFO_REQUESTED ||
        currentStatus == TicketStatus.IN_PROGRESS

/** In-memory tickets for phase 1 (like ContactsList in P07). Returns copies, never the internal list. */
class TicketsList(initial: List<Ticket> = emptyList()) {
    val rawList: List<Ticket>                       // insertion order, copy
    fun add(ticket: Ticket): Boolean                // false if id blank or already present
    fun findById(id: String): Ticket?
    fun getAllSorted(): List<Ticket>                // createdAt desc, then id
    fun publicActive(): List<Ticket>                // PUBLIC && isActive, same order
    fun findNearby(location: Coordinates, categoryId: String, situation: Situation): Ticket?
    fun stats(): DashboardStats

    companion object {
        /** Brief I.1 "Obs": same category + situation within 10 m joins the existing ticket. */
        const val GROUPING_RADIUS_METERS = 10.0
    }
}
```

Implementation notes:
- `isActive` is an allow-list on purpose. Do not write it as `!= RESOLVED && != CLOSED`: a status added later should stay inactive until someone decides otherwise. Because of it, a new report next to a RESOLVED ticket gets null from `findNearby`, and the caller creates a new ticket.
- The constructor runs `initial.forEach { add(it) }`, so the samples go through the same id checks.
- Comparator for both sorted lists: `compareByDescending<Ticket> { it.createdAt }.thenBy { it.id }`. Put it in one `private val` in the class, or in the companion.
- `Coordinates (0, 0)` check: a `private fun Coordinates.isUnset() = latitude == 0.0 && longitude == 0.0` in `TicketsList.kt`.
- `findNearby`:
  1. Return null when `location.isUnset()` or `categoryId.isBlank()`.
  2. Otherwise filter `isActive && categoryId == categoryId && situation == situation && !it.location.isUnset()`.
  3. Map each match to its distance with `haversineMeters`, keep those `<= GROUPING_RADIUS_METERS`, and return `minByOrNull` distance.
  - The KDoc says: "May return a ticket the reporter created or already follows; the caller checks `creatorId`/`followerIds`."
  - Rename the parameters if shadowing the properties is confusing, for example `filter { it.categoryId == categoryId }` is fine in Kotlin, but you can make it explicit.
- `stats()`:
  1. Compute `total` and `active`.
  2. Compute the durations: `mapNotNull` over the tickets.
     - `createdAt <= 0` -> null.
     - `statusHistory.lastOrNull { it.status == RESOLVED }?.dateTime` -> `end`, or null.
     - `end - createdAt`, kept only if it is `>= 0`.
  3. Return `averageRepairTimeMillis = if (durations.isEmpty()) null else durations.sum() / durations.size`.
  - Plain `Long` maths. Use no `average()`, because it returns a `Double` that is NaN on an empty list.
- Haversine body (KDoc names the formula):
  - `dLat = Math.toRadians(lat2 - lat1)`, and `dLon` likewise.
  - `a = sin(dLat/2)^2 + cos(toRadians(lat1)) * cos(toRadians(lat2)) * sin(dLon/2)^2`.
  - Return `2 * EARTH_RADIUS_METERS * atan2(sqrt(a), sqrt(1 - a))`.
  - Use `kotlin.math` (`sin`, `cos`, `sqrt`, `atan2`) and `Math.toRadians`, or a local `* PI / 180`. Pick one and keep `utils` free of Android.

## Steps
Before every commit:
- Run `./gradlew assembleDebug` (it must pass) and `./gradlew lint` (no new errors).
- Run `grep -rn "^import" app/src/main/java/pt/isec/amov/cityfix/model/ app/src/main/java/pt/isec/amov/cityfix/utils/`. Expect only `pt.isec.amov.cityfix.utils.*` in `model/` and `kotlin.math.*` in `utils/`.
- Check that `git config core.hooksPath` prints `.githooks`.

1. **Slice 1: distance helper.**
   - Create `utils/Distance.kt` as in Interfaces and delete `utils/.gitkeep`.
   - Commit: `feat(D-002): add haversine distance helper`.
   - Verify: build and lint are green. Desk check: one degree of latitude at the same longitude is about 111 195 m (`EARTH_RADIUS_METERS * PI / 180`). Slice 6 asserts it.
2. **Slice 2: dashboard stats type.**
   - Create `model/DashboardStats.kt` exactly as in Interfaces (fields decided at Gate 1, Q2).
   - Commit: `feat(D-002): add dashboard stats`.
   - Verify: build and lint are green, and `DashboardStats()` compiles with no arguments.
   - Then tell Francisco the fields, push `dev/Diogo` (`/push-task`), and tell him the type is ready for F-004.
3. **Slice 3: tickets list core.**
   - Create `model/TicketsList.kt` with `isActive`, the class, `rawList`, `add`, `findById`, `getAllSorted`, `publicActive` and the companion constant.
   - Leave `findNearby` and `stats` for the next slices. Do not add `TODO()` stubs.
   - Commit: `feat(D-002): add tickets list with public active filter`.
   - Verify: build and lint are green.
4. **Slice 4: 10 m grouping lookup.**
   - Add `findNearby` and the `isUnset` helper.
   - Commit: `feat(D-002): add 10 m nearby ticket lookup`.
   - Verify: build and lint are green.
5. **Slice 5: stats.**
   - Add `stats()`.
   - Commit: `feat(D-002): compute dashboard stats`.
   - Verify: build and lint are green.
6. **Slice 6: unit tests (Q1, decided at Gate 1).**
   - Create `TicketsListTest.kt` (JUnit 4, `org.junit.Test`, `org.junit.Assert.*`).
   - Anchor point: a real location near Pr. Republica, Coimbra, for example `Coordinates(40.2089, -8.4193)`. The exact centre is not needed here.
   - Offset helper: move north by `meters / 111_195.0` degrees of latitude.
   - Cases:
     - `findNearby`, same category and situation:
       - 9 m -> found;
       - 11 m -> null.
     - `findNearby` at 9 m returns null in each of these cases:
       - other `categoryId`;
       - other `situation`;
       - RESOLVED ticket (a new report next to it creates a new ticket);
       - CLOSED ticket;
       - ticket at (0, 0);
       - query at (0, 0);
       - blank `categoryId`.
     - `findNearby` with two matches returns the nearer one.
     - `publicActive()`:
       - excludes a PRIVATE OPEN ticket, a PUBLIC RESOLVED ticket and a PUBLIC CLOSED ticket;
       - keeps PUBLIC tickets in OPEN, INFO_REQUESTED and IN_PROGRESS.
     - `isActive`: true for OPEN, INFO_REQUESTED and IN_PROGRESS; false for RESOLVED and CLOSED.
     - `stats()`:
       - an empty list returns `DashboardStats()`: `activeRatio == 0.0`, average null, not NaN;
       - a mixed list gives the expected total, active, ratio and average. RESOLVED and CLOSED tickets count in `totalTickets` but not in `activeTickets`, and PRIVATE tickets count too (Q9). Include a ticket with no RESOLVED entry (skipped), a reopened ticket with two RESOLVED entries (the last one is used), and a ticket with `createdAt = 0` (skipped).
     - `add`: rejects a blank id and a duplicate id. A list returned by `rawList` does not change when you add a ticket afterwards.
     - `haversineMeters`: one degree of latitude is about 111 195 m (tolerance 1 m).
   - Commit: `test(D-002): add tickets list rule tests`.
   - Verify: `./gradlew testDebugUnitTest` passes, and build and lint are green.

## Conventions
- Pure Kotlin in `model/` and `utils/`: no `android.*`, `androidx.*`, `com.google.firebase.*` or `kotlinx.serialization.*`, and no `R`.
- Immutable out: public members return read-only copies. Only `TicketsList` holds a `MutableList`, and it is `private`.
- Null-safe lookups: `firstOrNull`, `lastOrNull`, `minByOrNull`. Never `first()`/`last()` on data that may be empty (C2). No `!!`.
- Named constants for the rules (`GROUPING_RADIUS_METERS`, `EARTH_RADIUS_METERS`) with a KDoc that cites the brief.
- Times stay `Long` epoch milliseconds (decision 0003). Formatting belongs to the UI (F-004), not here.
- One top-level type per file. The `Ticket.isActive` extension sits next to `TicketsList` because they belong together.
- Trailing commas in multi-line parameter lists. Plain code over clever code: each function must be easy to explain at the defence.
- Conventional Commits with the task ID and no attribution lines.

## Do not touch
- `docs/work/D-002-tickets-list-stats/state.md` (the orchestrator updates it).
- D-001 model files (`Ticket.kt`, `Coordinates.kt`, etc.). If a change seems needed, stop and record it as a deviation.
- `gradle/libs.versions.toml`, `app/build.gradle.kts` (JUnit is already there).
- `ui/`, `viewmodel/`, `data/`, `MainActivity.kt`, resources.
- `app/src/test/.../ExampleUnitTest.kt` and `app/src/androidTest/` (leave the template files alone).
- CLAUDE.md, skills, task files, decisions (the docs phase handles decisions).
- `google-services.json` and any Firebase rules.

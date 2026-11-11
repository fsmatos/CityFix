# D-008: Coding plan

Written for the recommended answers in `00-plan.md` (Q1-Q11). If Gate 1 picks an alternative, the affected step is noted.

## Files
| Path | Change | Why |
| --- | --- | --- |
| `app/src/main/java/pt/isec/amov/cityfix/model/AttachmentPhase.kt` | new | `enum class AttachmentPhase { REPORT, RESOLUTION }` (Q6) |
| `app/src/main/java/pt/isec/amov/cityfix/model/Attachment.kt` | edit | add `val phase: AttachmentPhase = AttachmentPhase.REPORT` as the last parameter; fix the `id`/`file` KDoc (F-007 handover N7, Q12) |
| `app/src/main/java/pt/isec/amov/cityfix/model/TicketPrivacy.kt` | KDoc only | add `phase` to the attachments' kept fields |
| `app/src/main/java/pt/isec/amov/cityfix/model/Ticket.kt` | KDoc only | `currentStatus`: change it only through `transition`/`closeIfExpired`; a projected ticket has a status with an empty history (D-007 review N2) |
| `app/src/main/java/pt/isec/amov/cityfix/model/TicketLifecycle.kt` | new | constants, time rules, transitions |

## Interfaces
All in package `pt.isec.amov.cityfix.model`. Imports: none outside `model/` (same package, so no import lines at all are expected). Top-level declarations, like `TicketPrivacy.kt`.

```kotlin
/** Phase of an attachment: report media or resolution proof. Stored by name: do not rename. */
enum class AttachmentPhase { REPORT, RESOLUTION }

data class Attachment(
    // ...existing parameters unchanged...
    /** REPORT for media added when reporting; RESOLUTION for the operator's proof (set by transition). */
    val phase: AttachmentPhase = AttachmentPhase.REPORT,
)
```

```kotlin
/** Brief II: a resolved ticket can be reopened for 5 days, then it is closed. Offset from the RESOLVED time. */
const val REOPEN_WINDOW_MS: Long = 5L * 24 * 60 * 60 * 1000          // 432_000_000

/** Brief III: warn 24 h before closing, so 4 days after the RESOLVED time. */
const val CLOSING_WARNING_MS: Long = REOPEN_WINDOW_MS - 24L * 60 * 60 * 1000   // 345_600_000

/** responsibleId of the automatic CLOSED entry: no person made that transition. */
const val SYSTEM_RESPONSIBLE_ID = "system"

/** Time of the last RESOLVED entry while the ticket is RESOLVED or CLOSED; null otherwise or with no such entry. */
fun Ticket.resolvedAt(): Long?

/** True while RESOLVED and less than REOPEN_WINDOW_MS has passed since resolvedAt (a negative gap counts as inside). */
fun Ticket.canReopen(now: Long): Boolean

/** CLOSED for a RESOLVED ticket whose window has ended (>= REOPEN_WINDOW_MS); currentStatus otherwise. */
fun Ticket.effectiveStatus(now: Long): TicketStatus

/** effectiveStatus(now) == CLOSED: no messages, no reopen, no operator action. */
fun Ticket.isLocked(now: Long): Boolean

/** resolvedAt + CLOSING_WARNING_MS while RESOLVED; null otherwise. */
fun Ticket.closingWarningAt(): Long?

/** Statuses `by` may move this ticket to at `now`. Empty for a projected ticket, a locked one, or a blank user id. */
fun Ticket.allowedTransitions(by: User, now: Long): Set<TicketStatus>

/**
 * The ticket after one transition, or null when the move is rejected. Appends one StatusHistory
 * (to, now, trimmed justification or null, by.id) and sets currentStatus in the same copy
 * (decision 0004). RESOLVED needs proof, appended with phase RESOLUTION and uploadedById = by.id.
 */
fun Ticket.transition(
    to: TicketStatus,
    by: User,
    now: Long,
    justification: String? = null,
    proof: List<Attachment> = emptyList(),
): Ticket?

/** RESOLVED ticket whose window has ended -> CLOSED copy with one entry at resolvedAt + REOPEN_WINDOW_MS by SYSTEM_RESPONSIBLE_ID; null otherwise. */
fun Ticket.closeIfExpired(now: Long): Ticket?
```

Private helpers (suggested, keep them small and named):
- `private fun Ticket.isFullTicket(): Boolean` = `statusHistory.isNotEmpty() && statusHistory.last().status == currentStatus`.
- `private fun Ticket.isParticipant(userId: String): Boolean` = `userId == creatorId || userId in followerIds`.
- `private fun operatorTargets(status: TicketStatus): Set<TicketStatus>`: an exhaustive `when (status)` with no `else`, so a new status does not compile until someone decides its moves.

Rules inside `allowedTransitions` (Q2, Q5, Q11):
- empty when `!isFullTicket()` or `by.id.isBlank()`;
- `val status = effectiveStatus(now)`;
- `when (by)`: `is Operator -> operatorTargets(status)`; `is Citizen ->` `setOf(OPEN)` if `by.registrationStatus == ACTIVE && isParticipant(by.id) && canReopen(now)`, else empty.

Rules inside `transition`, in this order, each returning `null`: not in `allowedTransitions(by, now)`; trimmed justification blank when `to == INFO_REQUESTED` or (`to == OPEN` and `currentStatus == RESOLVED`); `to == RESOLVED` and `proof` empty; `to != RESOLVED` and `proof` not empty; a proof id blank, repeated inside `proof`, or already in `attachments`.

If Gate 1 changes:
- Q5 alternative: replace `allowedTransitions(by, now)` by `allowedTransitions(status, role)` plus a `role` parameter type; `transition` then does the window/relation checks itself.
- Q7 alternative: `transition` returns `Ticket` and uses `require(...)` with a message per rule.
- Q8 alternative: drop `proof`; rule 5 becomes `attachments.none { it.phase == RESOLUTION }`.
- Q1 alternative: citizen also gets `INFO_REQUESTED` from OPEN.

## Steps

### Slice 1: attachment phase
- New `AttachmentPhase.kt` with the KDoc above.
- `Attachment.kt`: `phase` as the last parameter. KDoc: `id` = "unique per ticket; the local file name before upload (F-007), later the Storage/Firestore id"; `file` = "absolute local path before upload, Storage path or URL after it".
- `TicketPrivacy.kt` KDoc: kept attachment fields become "id, type, file, thumbnail, uploadState and phase". No code change.
- Commit: `feat(D-008): add attachment phase for resolution proof`
- Verify: `./gradlew assembleDebug`; `./gradlew lint` (0 errors, 16 warnings); grep `Attachment(` shows only named-argument calls (compiles anyway with a trailing default).

### Slice 2: time rules
- New `TicketLifecycle.kt`: file KDoc (brief II, decisions 0004 and 0006: "never call these on a projected ticket"), the three constants (`SYSTEM_RESPONSIBLE_ID` may wait for slice 3), `resolvedAt`, `canReopen`, `effectiveStatus`, `isLocked`, `closingWarningAt`, `isFullTicket`.
- `Ticket.kt`: one KDoc line on `currentStatus`.
- Commit: `feat(D-008): add reopen window and closing rules`
- Verify: build, lint, grep imports; walk table rows T1-T8 below.

### Slice 3: transitions
- Add `SYSTEM_RESPONSIBLE_ID`, `isParticipant`, `operatorTargets`, `allowedTransitions`, `transition`, `closeIfExpired`.
- Commit: `feat(D-008): add ticket status transitions`
- Verify: build, lint, grep imports; walk rows R1-R16 below.

## Verification
No automated tests (Q9). The implementer walks every row by reading the code, writes "walked" with the slice's commit in `state.md`, and the reviewer re-walks them.

Fixtures: `t0 = 1_791_100_800_000` (`sample-3`'s RESOLVED time, 2026-10-04 08:00 UTC). `op = Operator(id = "operator-1")`. `cit = Citizen(id = "citizen-1", registrationStatus = ACTIVE)`. `resolved` = `sample-3` (creator `citizen-1`, last entry RESOLVED at `t0`). `fol = Citizen(id = "citizen-3", ACTIVE)` added to `resolved.followerIds`. `other = Citizen(id = "citizen-9", ACTIVE)`.

| # | Call | Expected |
| --- | --- | --- |
| T1 | `resolved.resolvedAt()` | `t0` |
| T2 | `resolved.canReopen(t0 + 428_400_000)` (4 d 23 h) | true |
| T3 | `resolved.canReopen(t0 + 435_600_000)` (5 d 1 h) | false |
| T4 | `resolved.canReopen(t0 + 432_000_000)` / `effectiveStatus(same)` | false / CLOSED |
| T5 | `resolved.effectiveStatus(t0 + 4 d)` / `isLocked` | RESOLVED / false |
| T6 | `resolved.closingWarningAt()` | `t0 + 345_600_000` |
| T7 | `sample-1` (OPEN): `resolvedAt()`, `closingWarningAt()`, `canReopen(any)` | null, null, false |
| T8 | `resolved.toAnonymousView()`: `resolvedAt()`, `canReopen(t0 + 1 d)` | null, false |
| R1 | `sample-1.transition(IN_PROGRESS, op, now)` | OPEN entry kept, one new IN_PROGRESS entry at `now` by `operator-1`, `currentStatus` IN_PROGRESS |
| R2 | `sample-1.transition(INFO_REQUESTED, op, now, "  Which side?  ")` / with `null` or `"  "` | justification `"Which side?"` / null result |
| R3 | `sample-1.transition(RESOLVED, op, now, proof = ...)` | null (not from OPEN) |
| R4 | `sample-2` (IN_PROGRESS) `.transition(RESOLVED, op, now)` | null (no proof) |
| R5 | `sample-2.transition(RESOLVED, op, now, proof = listOf(Attachment(id = "p1")))` | RESOLVED, attachment `p1` with `phase = RESOLUTION`, `uploadedById = "operator-1"` |
| R6 | R5 with a proof id that is blank or already on the ticket | null |
| R7 | `sample-2.transition(INFO_REQUESTED, op, now, "Photo of the pole?")` | accepted |
| R8 | `sample-1.transition(IN_PROGRESS, cit, now)` | null (citizen) |
| R9 | `resolved.transition(OPEN, cit, t0 + 1 d, "Still broken")` | OPEN, entry by `citizen-1` with the justification; `canReopen` now false, `resolvedAt()` null |
| R10 | R9 by `fol` / by `other` / by `cit` with PENDING registration | accepted / null / null |
| R11 | R9 with no justification | null |
| R12 | `resolved.transition(OPEN, cit, t0 + 435_600_000, "late")` | null (window ended) |
| R13 | a ticket whose last entry is CLOSED: `transition(x, op or cit, now)` for every `x`; `allowedTransitions` | null for all; empty set |
| R14 | `resolved.closeIfExpired(t0 + 6 d)` / `(t0 + 4 d)` / on `sample-1` | CLOSED copy with one entry at `t0 + 432_000_000` by `"system"` / null / null |
| R15 | `resolved.allowedTransitions(op, t0 + 1 d)` | empty (operators do not reopen) |
| R16 | any call on a projected ticket (`toAnonymousView()`): `transition`, `allowedTransitions` | null, empty |

Also check by reading: every accepted path adds exactly one history entry and sets `currentStatus` to `to` in the same `copy`; `operatorTargets` matches the table in `00-plan.md`; `effectiveStatus` and `canReopen` use `<` and `>=` on the same difference.

Grep (each must return nothing):
- `^import (android|androidx|com\.google)` in `model/TicketLifecycle.kt` and `model/AttachmentPhase.kt`;
- `System.currentTimeMillis` in `model/TicketLifecycle.kt` (time is always a parameter).

## Conventions
- Pure Kotlin in `model/` (decision 0003); time is a `Long` parameter, never read inside (deterministic, previewable).
- KDoc on every public declaration, naming the brief section and decision it implements; plain `if`/`when`, no clever chains, so both authors can explain it at the defence.
- Exhaustive `when` with no `else` over `TicketStatus` and over `User`.
- No user-visible strings in `model/` (the justification is user input, not a resource).
- English identifiers; constants in `UPPER_SNAKE_CASE` with `_MS` for milliseconds, as in the task.
- Conventional Commits with `D-008`, no attribution lines.

## Do not touch
- `data/SampleTickets.kt`, `data/TicketsRepository.kt` (Francisco's; Q10), `ui/**` (no screen changes), `model/TicketsList.kt`, `model/DashboardStats.kt` (no effect, see `00-plan.md`), `model/StatusHistory.kt` and `model/TicketStatus.kt` (shapes unchanged).
- `app/build.gradle.kts`, `gradle/libs.versions.toml` (no test dependency, Q9).
- Firestore/Storage rules, `google-services.json`.
- `docs/decisions/0010-*` (F-010's number).

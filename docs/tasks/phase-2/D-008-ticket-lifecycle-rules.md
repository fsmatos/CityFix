# D-008: Ticket lifecycle rules (pure)

Owner: Diogo (`dev/Diogo`) · Layer: `model` · Phase 2 · Status: to do · Target: 2026-11-10
Depends on: - (no Firebase) · Used by: F-014 (reopen), D-013 (operator transitions), F-016 (24 h warning)

## Goal
Put the ticket state machine and its time rules in one pure place, so the citizen and operator screens apply the same rules.

## Why
- Brief II: OPEN → INFO_REQUESTED ↔ IN_PROGRESS → RESOLVED → (reopen within 5 days) or CLOSED after 5 days, locked.
- Phase 1 risk 6: reopening has no field; compute it from the RESOLVED history entry.
- Brief I.3.E: every transition has a timestamp.

## What to build
- **`model/TicketLifecycle.kt`** (pure Kotlin, no Android):
  - `allowedTransitions(status, role): Set<TicketStatus>` (operator: OPEN→INFO_REQUESTED/IN_PROGRESS, INFO_REQUESTED→IN_PROGRESS, IN_PROGRESS→INFO_REQUESTED/RESOLVED; citizen: RESOLVED→OPEN by reopen; decide at Gate 1 whether a citizen question sets INFO_REQUESTED).
  - `Ticket.transition(to, by, now, justification?): Ticket` that appends a `StatusHistory` and updates `currentStatus` together (decision 0004), rejecting invalid moves.
  - `resolvedAt(ticket)`, `canReopen(ticket, now)` (< 5 days), `effectiveStatus(ticket, now)` (RESOLVED older than 5 days → CLOSED), `isLocked(ticket, now)`, `closingWarningAt(ticket)` (resolved + 4 days).
  - Constants `REOPEN_WINDOW_MS`, `CLOSING_WARNING_MS`.
- **Resolution proof rule:** RESOLVED requires at least one RESOLUTION attachment (needs the `phase` field from decision 0010; add `AttachmentPhase` enum if F-010 has not).
- Previews/samples: add a resolved sample ticket at 4 and 6 days to check the screens.
- Decision record and overview update.

## Files
New `model/TicketLifecycle.kt` (maybe `model/AttachmentPhase.kt`); `model/Attachment.kt`, `data/SampleTickets.kt`, docs.

## Acceptance checks
- [x] `canReopen` true at 4 days 23 h after resolution, false at 5 days 1 h. (Code reading: T1-T6 in `docs/work/D-008-ticket-lifecycle-rules/01-coding-plan.md`, walked by the implementer at 2df8cc4 and again by the reviewer; also false at exactly 5 d, where `effectiveStatus` is CLOSED. No test run, Q9.)
- [x] RESOLVED without proof and CLOSED → anything are rejected. (Code reading: R3-R6 and R13, walked by the implementer at 1c297f8 and by the reviewer, for stored and effective CLOSED and both roles.)
- [x] Every transition adds one history entry with the time and the user id. (Code reading plus grep: the only status writes are the single `copy` in `transition` and in `closeIfExpired` (whose entry uses `"system"`, as decided at Gate 1 Q3); no other `StatusHistory(` in `model/`. Checked in the review.)
- [x] No Android import in the file. (Grep: `TicketLifecycle.kt` has no `import` line at all, `AttachmentPhase.kt` none either; checked after slices 2 and 3 and in the review.)

Status note (D-008 docs, 2026-11-11): the brief rules (II, II.2, I.2.E, III, I.3.E) were not checked against the brief PDF, which could not be opened; see decision 0012. Nothing has run on a device: no screen uses the rules yet (F-014, D-013).

## References
Brief I.3.E, II; decision 0004; phase 1 README risks 1 and 6.

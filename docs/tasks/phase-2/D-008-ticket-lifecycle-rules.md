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
- [ ] `canReopen` true at 4 days 23 h after resolution, false at 5 days 1 h.
- [ ] RESOLVED without proof and CLOSED → anything are rejected.
- [ ] Every transition adds one history entry with the time and the user id.
- [ ] No Android import in the file.

## References
Brief I.3.E, II; decision 0004; phase 1 README risks 1 and 6.

# D-008: Docs plan

## Docs to update
- `docs/architecture/overview.md`:
  - "Layers", `model/` bullet: name `TicketLifecycle.kt` as the place for the lifecycle and time rules.
  - "Code mapping" table: `Anexo` row gains `phase`; enum list gains `AttachmentPhase { REPORT, RESOLUTION }`; defaults line gains `phase = REPORT`.
  - New subsection "Lifecycle (D-008)" after "Rules (D-002)": the transition table, the API (signatures in one line each), the justification and proof rules, the 5-day window with the boundary (`<` / `>=`), `closingWarningAt`, `closeIfExpired` with `"system"`, "never on a projected ticket", and why `isActive`, `publicActive()` and `stats()` are unaffected.
  - "How F-014 / D-013 / F-016 use it" bullets: F-014 shows Reopen only when `allowedTransitions(user, now)` contains OPEN (or `canReopen`), asks for a justification, and treats `isLocked` as read-only; D-013 offers exactly `allowedTransitions(operator, now)`, passes the proof attachment to `transition(RESOLVED, ...)` and writes `tickets` + `publicTickets` in one batch; F-016 schedules at `closingWarningAt()` and cancels when the ticket leaves RESOLVED; screens show `effectiveStatus(now)` in the badge for registered and operator views.
  - "ViewModel (D-003)" projection kept-fields list: add `phase`.
- `docs/decisions/0006-anonymous-projection-phase-1.md`: one line under the D-007 contract table that `Attachment.phase` (D-008) is kept (not personal data). Only if Gate 1 keeps `phase` passing through.
- `docs/decisions/0004-denormalised-current-status.md`: one consequence line: the pure transition function exists, `Ticket.transition` in `model/TicketLifecycle.kt` (D-008).
- `docs/tasks/phase-1/README.md`, "Risks to handle in the Firebase phase": mark risks 1 and 6 as handled in the model by D-008 (Firestore mapping still F-011).
- Handover notes for Francisco (in the decision record and the overview, not in his files): F-010's decision 0010 should reference `AttachmentPhase`; F-011 maps `phase` (missing = REPORT), may enforce the window with `request.time` in the rules, and should guard concurrent status writes; F-014/D-013 previews build RESOLVED tickets at a fixed `t0` with `now = t0 + 4 d` / `t0 + 6 d` (Q10); `sample-3` has no RESOLUTION attachment, harmless.

## Decisions to record
- `docs/decisions/0011-ticket-lifecycle-rules.md` (number agreed with Francisco, Q6; 0010 is F-010's schema record). Context: brief II, I.3.E, phase-1 risks 1 and 6, F-010 Gate 1 decision 3, decisions 0004 and 0006. Decision, with the Gate 1 answers and rejected options:
  - transition table and roles (Q1, Q2, Q3), justification rule (Q4);
  - `allowedTransitions(by: User, now)` on the effective status (Q5), `Ticket?` result (Q7);
  - `AttachmentPhase` and proof carried by the RESOLVED transition (Q6, Q8);
  - window maths: offsets from the last RESOLVED entry, strict boundary, clock-skew stance, `closeIfExpired` at `resolvedAt + 5 d` by `"system"`;
  - no committed tests (Q9), no sample change (Q10).
  Consequences: cross-branch contract, Firestore mapping, client-side clock limits, concurrent writes, callers must not `copy(currentStatus = ...)`.

## Report notes
For `docs/report/notes.md` (F-017 builds the PDF from it):
- The state machine as a table and a small diagram (OPEN -> INFO_REQUESTED <-> IN_PROGRESS -> RESOLVED -> OPEN / CLOSED), with who may make each move.
- Why one pure function: decision 0004 keeps `currentStatus` next to the history; `transition` is the only place that changes both, so the citizen and operator screens apply the same rules and the history is a full audit trail (brief I.3.E timestamps).
- Reopening and closing without stored fields: both are computed from the last RESOLVED entry; CLOSED is shown on read and written lazily (Spark plan, no server), at the exact closing time rather than when someone noticed.
- Proof of resolution: `AttachmentPhase` answers phase-1 risk 1; the proof is attached in the same operation as the status change, so offline queued writes stay consistent and a re-resolve needs new proof.
- Limits and improvements: device clocks decide the window (a Cloud Function or rule on `request.time` would make it server-side); concurrent transitions need a transaction; no automated tests by project choice, rules checked by a written table; a `PublicTicket` type and a sealed result type (instead of `null`) would let the compiler enforce more.

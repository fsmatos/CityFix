# 0011: Ticket lifecycle rules

Status: accepted (D-008, Gate 1 on 2026-11-11, Q1-Q11 as recommended; review fixes S1/S2 on 2026-11-11). Number agreed with Francisco: 0010 is F-010's Firestore schema record.

## Context
- **Brief (not checked against the brief).** The brief PDF could not be opened in the D-008 sessions, so these rules come from the task files (D-008, D-013, D-014, F-014, F-016), not the PDF. As those files summarise it: brief II gives OPEN -> INFO_REQUESTED <-> IN_PROGRESS -> RESOLVED -> reopened within 5 days, or CLOSED after 5 days and locked; II.2 has information requests both ways; III has a warning 24 h before closing; I.3.E has a timestamp on every transition and the operator uploading the photo/video of the finished work. Diogo should check brief II, II.2, I.2.E, III and I.3.E in the PDF before the defence.
- Phase-1 README risks 1 (an attachment cannot tell report media from resolution proof) and 6 (reopening has no field; compute it from the RESOLVED history entry).
- [Decision 0004](0004-denormalised-current-status.md): `currentStatus` and `statusHistory` change together through "one pure transition function in `model/`"; transition times come from the history with `lastOrNull`.
- [Decision 0006](0006-anonymous-projection-phase-1.md) as amended by D-007: a projected ticket has an empty history and must never feed lifecycle rules.
- F-010 Gate 1 decision 3 (phase 2 README): closing after 5 days is computed on read, plus a write when a user or worker sees it (or a Cloud Function on Blaze). Not decided yet; this design supports both.
- Three consumers need the same answers: F-014 (citizen reopen), D-013 (operator transitions), F-016 (24 h warning). Separate copies of the rules would disagree.

## Decision
All rules live in `model/TicketLifecycle.kt` (pure Kotlin, no imports, time always a parameter). Code-level detail: `docs/architecture/overview.md`, "Lifecycle (D-008)".

- **Transition table and roles (Q1, Q2, Q3, Q11).**

  | From (effective) | Operator | Citizen (creator or follower, `ACTIVE`) |
  | --- | --- | --- |
  | OPEN | INFO_REQUESTED, IN_PROGRESS | - |
  | INFO_REQUESTED | IN_PROGRESS | - |
  | IN_PROGRESS | INFO_REQUESTED, RESOLVED | - |
  | RESOLVED (less than 5 days) | - | OPEN (reopen) |
  | CLOSED (stored, or RESOLVED for 5 days or more) | - | - |

  - Q1: a citizen's question is a plain message; it does not set INFO_REQUESTED (that status reads "the municipality is waiting for the citizen", and moving IN_PROGRESS back would switch off D-014's geofence). Rejected: citizen OPEN -> INFO_REQUESTED. Brief II.2 not checked.
  - Q2: the creator **and the followers** may reopen (Option A makes followers co-reporters, decision 0002). Rejected: creator only. Brief II / I.2.E not checked.
  - Q3: nobody moves a ticket to CLOSED by hand; it is time-based. Rejected: operators closing early (not in the task files' reading of the brief).
  - Q11: a citizen whose registration is not `ACTIVE` gets no transitions, even if a screen forgets the D-009 gate.
- **Justification (Q4).** Required (non-blank after trim) for INFO_REQUESTED and for the reopen; optional for IN_PROGRESS and RESOLVED; none for the automatic CLOSED. Stored trimmed, `null` when blank.
- **`allowedTransitions(by: User, now)` on the effective status (Q5).** The role is the type of `by` (exhaustive `when` on the sealed `User`, decision 0003), and the set already includes the relation, the registration and the window, so it is exactly what a UI may offer; `transition` accepts only moves in it. Rejected: the task's `allowedTransitions(status, role)` with a role enum (every caller must remember the effective status, the window and the relation).
- **`transition(...)` returns `Ticket?` (Q7).** `null` means rejected, like `reportTicket` and `TicketsList.add`. Rejected: `require` / throwing (a missed check crashes the app).
- **`AttachmentPhase` and proof with the transition (Q6, Q8).** New `enum class AttachmentPhase { REPORT, RESOLUTION }` and `Attachment.phase` (default REPORT, last parameter, persisted by name), added in D-008 rather than waiting for F-010. RESOLVED needs a non-empty `proof` list passed to `transition`; the proof is appended with `phase = RESOLUTION` and `uploadedById = by.id` in the same `copy`, and only fresh proof counts. Proof with another target, or a blank, repeated or already-present id, is rejected. Rejected: "the ticket has any RESOLUTION attachment" (old proof would satisfy a re-resolve after a reopen, and D-013 would need a separate `addResolutionProof`); a predicate parameter instead of a field (moves the problem to D-013).
- **Window maths.**
  - `REOPEN_WINDOW_MS` = 5 d and `CLOSING_WARNING_MS` = 4 d, both offsets from the **last** RESOLVED entry (`resolvedAt()`, only while RESOLVED or CLOSED).
  - Strict boundary: `canReopen` uses `now - resolvedAt < 5 d`, `effectiveStatus` gives CLOSED for `>= 5 d`, so a ticket is never both reopenable and closed.
  - Clock skew: a negative difference counts as inside the window. The device clock decides, unless F-011's rules or a Cloud Function add a server-side check.
  - `closeIfExpired(now)` writes one CLOSED entry at `resolvedAt + 5 d` (when it closed, not when someone noticed) with `responsibleId = SYSTEM_RESPONSIBLE_ID` (`"system"`). Reading `effectiveStatus` works without that write.
  - `canReopen` is time-only; the Reopen button uses `OPEN in allowedTransitions(user, now)` (review S1). `closingWarningAt()` can be in the past; F-016 checks `!isLocked(now)` before warning (review S2).
- **No committed tests (Q9).** The project has no test framework (decision 0005 Q1). Verified by build, lint, grep and the written check table (T1-T8, R1-R16) walked by the implementer and the reviewer.
- **No sample change (Q10).** `data/SampleTickets.kt` is unchanged; F-014/D-013 previews build a RESOLVED ticket at a fixed `t0` with `now = t0 + 4 d` / `t0 + 6 d`.
- Also rejected: an `object TicketLifecycle { ... }` wrapper (top-level functions match `TicketPrivacy.kt`).

## Consequences
- **Cross-branch contract.** `AttachmentPhase`, `Attachment.phase`, the function names and `SYSTEM_RESPONSIBLE_ID` are used by F-010, F-011, F-013, F-014, F-016 and D-013; renaming them breaks Francisco's branch. F-010 uses this enum instead of adding its own, and decision 0010 points here.
- **Firestore mapping (F-011).** Map `phase` by name; a missing field reads as REPORT. Security rules must allow `responsibleId == "system"` for the CLOSED entry at `resolvedAt + 5 d` (a rule requiring `responsibleId == request.auth.uid` would reject it). The rules may enforce the window with `request.time`.
- **Client-side clock limits.** A device clock ahead closes the window early, one behind (or set back) reopens late. `transition` does not reject a `now` earlier than the last entry (offline queues), so the history can be out of time order.
- **Concurrent writes.** Two valid offline transitions on the same ticket are last-write-wins. D-013/F-011 use a transaction or a precondition on `currentStatus`, and write `tickets` and `publicTickets` in one batch.
- **Callers must not `copy(currentStatus = ...)`.** Only `transition` and `closeIfExpired` change status after creation; reviewers of D-013/F-014 check it.
- **Display.** Registered and operator screens show `effectiveStatus(now)`; the history shows `"system"` as a string resource ("Closed automatically" / "Encerrado automaticamente"), not as a user lookup; detail screens label or group attachments by `phase`, because RESOLUTION proof from an earlier resolve stays on a reopened, public ticket (review N1, N2).
- **Unchanged.** `isActive`, `publicActive()`, `findNearby()` and `stats()` do not change: RESOLVED and CLOSED are both inactive, and a later CLOSED entry does not move the last RESOLVED time. `phase` passes through `toAnonymousView()` (not personal data).
- **Stored data.** `sample-3` is RESOLVED with no RESOLUTION attachment; the proof rule applies to transitions, not to stored tickets.

# D-008: Ticket lifecycle rules (pure)
Type: feature · Task file: docs/tasks/phase-2/D-008-ticket-lifecycle-rules.md
Labels: none

## Problem
- No code applies the ticket state machine yet. `Ticket.currentStatus` and `statusHistory` can be changed separately by anyone who calls `copy(...)`, although decision 0004 says they change together in "one pure transition function in `model/`".
- Three consumers need the same rules: F-014 (citizen reopen, Francisco), D-013 (operator transitions, Diogo) and F-016 (24 h warning, Francisco). If each writes its own version, the screens disagree about whether a ticket can be reopened or is closed.
- Reopening has no field of its own (phase 1 README, risk 6). It must be computed from the RESOLVED history entry.
- `Attachment` cannot tell a report photo from resolution proof (phase 1 README, risk 1), so "RESOLVED requires proof" cannot be checked.

### Sources
- Brief II as summarised in the task file: OPEN -> INFO_REQUESTED <-> IN_PROGRESS -> RESOLVED -> reopened within 5 days, or CLOSED after 5 days and locked. II.2: information requests go both ways (F-014). II.3: IN_PROGRESS turns on the 20 m geofence (D-014). II.4: the author is notified on resolution (F-016). Brief I.3.E: every status transition has a timestamp; the operator uploads the photo/video of the finished work.
- **The planner could not read the PDF.** This session has no `pdftotext`/`pdftoppm`, so the quotes above come from the task files (D-008, D-013, D-014, F-014, F-016), not the brief itself. Before Gate 1 the person should check brief II.1-II.5 and I.3.E against Q1-Q4 below, especially whether a citizen can trigger INFO_REQUESTED and whether followers may reopen.
- Decisions 0003 (pure model, `sealed interface User`, `Long` epoch ms), 0004 (status and history change together; times read from the history with `lastOrNull`), 0005 (`isActive`, repair time), 0006 as amended by D-007 (projected tickets have an empty history; lifecycle rules must never run on them).
- Phase 2 README, F-010 Gate 1 decision 3: closing after 5 days is computed on read (a RESOLVED ticket older than 5 days counts as CLOSED and is locked), plus a write when a user or worker sees it, or a Cloud Function on Blaze. Not decided yet; this plan supports both.
- F-010 task: the Firestore schema gives attachments "a `phase` field: REPORT or RESOLUTION" in `docs/decisions/0010-firestore-schema.md`. F-010 is not on `main` yet and 0010 does not exist on this branch.

### Effect on existing code (checked at `e135bc7`)
- `Ticket.isActive` (`model/TicketsList.kt`) treats RESOLVED and CLOSED the same (both inactive). So `effectiveStatus` turning an old RESOLVED into CLOSED changes nothing in `publicActive()`, `findNearby()` or the "% active" figure.
- `stats()` takes the repair time from the **last RESOLVED history entry**, never from `currentStatus`, and skips active tickets. A CLOSED entry written later by `closeIfExpired` comes after the RESOLVED entry, so the repair time and `resolvedTickets` do not change. A reopened ticket is OPEN again, so it is active and left out until it is resolved again (decision 0005, already handled).
- No change is needed in `TicketsList`, `DashboardStats`, `LocalTicketsRepository`, `StatusBadge` or the screens. `sample-3` (RESOLVED on 2026-10-04) is effectively CLOSED today, but no screen shows it: it is not active, so it is not in the anonymous list.
- `Attachment(...)` is always built with named arguments (`ReportViewModel`, `SampleTickets`, `ShowScreen` and `ReportScreen` previews), so a new last parameter with a default compiles everywhere unchanged.

## Outcome
- One pure file, `model/TicketLifecycle.kt`, holds the state machine and the time rules: which transitions a user may make now, the transition itself (history entry and `currentStatus` together), the 5-day reopen window, the effective CLOSED status, the lock, the 24 h warning time and the automatic close.
- `Attachment` gains `phase: AttachmentPhase` (`REPORT` default, `RESOLUTION`), so RESOLVED can require proof.
- Decision record 0011 (number to confirm with Francisco, Q6) and the overview describe the rules and the API for F-014, D-013 and F-016.
- No screen changes: the first callers are F-014 and D-013.

## Acceptance checks
Checked by reading the code and walking the check table in `01-coding-plan.md` ("Verification"), since the project has no automated tests (Q9).
- [ ] With `t0` = the RESOLVED entry's time: `canReopen` is true at `t0 + 4 d 23 h` (428 400 000 ms) and false at `t0 + 5 d 1 h` (435 600 000 ms); at exactly `t0 + 5 d` it is false and `effectiveStatus` is CLOSED, so a ticket is never both reopenable and closed.
- [ ] `effectiveStatus` returns CLOSED and `isLocked` true for a RESOLVED ticket from `t0 + 5 d` on; `closingWarningAt` is `t0 + 4 d` (345 600 000 ms after `t0`).
- [ ] `transition` to RESOLVED with no proof attachment returns `null`; with proof, the proof is appended with `phase = RESOLUTION`.
- [ ] `transition` from CLOSED (stored or effective) to any status returns `null`, for both roles.
- [ ] Every accepted transition adds exactly one `StatusHistory` entry with `dateTime = now`, `responsibleId = by.id`, and the trimmed justification, and sets `currentStatus` to the same status.
- [ ] Each operator and citizen move in the transition table is accepted, and every other move returns `null`.
- [ ] A projected ticket (empty `statusHistory`) gets `null` from `transition`, `resolvedAt` and `closingWarningAt`, `false` from `canReopen`, and an empty `allowedTransitions`.
- [ ] `model/TicketLifecycle.kt` and `model/AttachmentPhase.kt` have no `android.`, `androidx.` or Firebase import (grep).
- [ ] `./gradlew assembleDebug` and `./gradlew lint` pass with no new warning (baseline 0 errors, 16 warnings).

## Approach

### API: extension functions on `Ticket`, user passed as `User`
Same style as `Ticket.isActive` and `Ticket.toAnonymousView()`. The task writes `canReopen(ticket, now)`; here it is `ticket.canReopen(now)`.

The task's `allowedTransitions(status, role)` becomes `ticket.allowedTransitions(by: User, now)` (Q5):
- It works on `effectiveStatus(now)`, not `currentStatus`, so a caller cannot forget the 5-day close and offer actions on an expired ticket.
- The role is the type of `by` (`is Operator`, `is Citizen`, exhaustive `when` over the sealed `User`, decision 0003). No new role enum.
- For a citizen it also checks the relation (creator or follower, Q2), the registration (`ACTIVE`) and `canReopen(now)`, so the set is exactly what the UI may offer.
- `transition` accepts a move only if it is in that set, so the UI and the rule cannot disagree.

### Transition table (Q1, Q2)
| From (effective) | Operator | Citizen (creator or follower, ACTIVE) |
| --- | --- | --- |
| OPEN | INFO_REQUESTED, IN_PROGRESS | - |
| INFO_REQUESTED | IN_PROGRESS | - |
| IN_PROGRESS | INFO_REQUESTED, RESOLVED | - |
| RESOLVED (within 5 days) | - | OPEN (reopen) |
| CLOSED (stored, or RESOLVED for 5 days or more) | - | - |

Nobody moves a ticket to CLOSED by hand (Q3); only `closeIfExpired` writes it.

### `transition(to, by, now, justification, proof): Ticket?`
Returns the new ticket, or `null` when the move is rejected (Q7; the task wrote `: Ticket`). Rejected when:
1. the ticket is not a full ticket: `statusHistory` empty (a projection, decision 0006) or its last entry's status differs from `currentStatus` (decision 0004 broken);
2. `by.id` is blank;
3. `to` is not in `allowedTransitions(by, now)` (this covers CLOSED, the expired window and wrong roles);
4. `to` is INFO_REQUESTED or a reopen (RESOLVED -> OPEN) and the trimmed justification is blank (Q4);
5. `to` is RESOLVED and `proof` is empty, or any proof attachment has a blank id or an id already on the ticket (Q8);
6. `proof` is not empty for any other target.

Accepted: `copy(currentStatus = to, statusHistory = statusHistory + StatusHistory(to, now, justification, by.id), attachments = attachments + proof)`, where each proof attachment is copied with `phase = RESOLUTION` and `uploadedById = by.id`. The justification is stored trimmed, and as `null` when blank (optional for the other moves).

### Time rules
- `REOPEN_WINDOW_MS = 5 days` (432 000 000), `CLOSING_WARNING_MS = REOPEN_WINDOW_MS - 24 h` (4 days, 345 600 000), both offsets from the RESOLVED time.
- `resolvedAt()`: the `dateTime` of the **last** RESOLVED entry (`lastOrNull`, decision 0004), but only while `currentStatus` is RESOLVED or CLOSED; `null` otherwise (a reopened ticket is not resolved). `null` for a projected ticket.
- `canReopen(now)`: `currentStatus == RESOLVED` and `now - resolvedAt < REOPEN_WINDOW_MS`. A negative difference (the device clock behind the operator's) counts as inside the window.
- `effectiveStatus(now)`: CLOSED when `currentStatus == RESOLVED` and `now - resolvedAt >= REOPEN_WINDOW_MS`; `currentStatus` otherwise. Strict `<` in one and `>=` in the other make the boundary consistent.
- `isLocked(now)`: `effectiveStatus(now) == CLOSED`.
- `closingWarningAt()`: `resolvedAt + CLOSING_WARNING_MS` while `currentStatus == RESOLVED`; `null` otherwise. F-016 schedules its job at this time and cancels it when the ticket leaves RESOLVED.
- `closeIfExpired(now): Ticket?`: for a full RESOLVED ticket whose window has ended, a copy with `currentStatus = CLOSED` and one CLOSED entry at `resolvedAt + REOPEN_WINDOW_MS` (the moment it closed, not when someone noticed) with `responsibleId = SYSTEM_RESPONSIBLE_ID` (`"system"`) and no justification; `null` otherwise. This is the "write when a user or worker sees it" of F-010 decision 3; without it, computed-on-read still works.

### Resolution proof and `AttachmentPhase` (Q6)
New `enum class AttachmentPhase { REPORT, RESOLUTION }` in `model/AttachmentPhase.kt` and `Attachment.phase: AttachmentPhase = AttachmentPhase.REPORT`, as the last parameter. Names match F-010's schema text ("`phase` field: REPORT or RESOLUTION"). The default means every existing attachment (reports, samples, previews) is a report photo with no code change. Persisted by name like the other enums (decision 0003). Francisco is told before he writes decision 0010, so F-010 references this enum instead of adding its own.

Proof travels with the RESOLVED transition (Q8): D-013 creates the `Attachment` for the picked/taken media, calls `transition(RESOLVED, operator, now, proof = listOf(att))`, and writes the result in one batch; F-013's worker uploads the file afterwards (upload state PENDING), so it works offline. Only proof added by this transition counts, so a ticket reopened and resolved again needs new proof.

### Privacy
`phase` is not personal data. It passes through `toAnonymousView()`; the KDoc's kept-field list gains `phase` (decision 0006: every new field is checked against the projection). Lifecycle functions refuse or return neutral values on a projected ticket (empty history), and their KDoc says never to call them on one. The line D-007 review N2 asked for goes into `Ticket.currentStatus`'s KDoc.

### Alternatives considered
- `allowedTransitions(status, role)` as in the task: static and simple, but every caller must remember to pass `effectiveStatus` and to check the window and the relation. Kept as an option in Q5.
- `transition` throwing `IllegalArgumentException` (`require`): a missed check crashes the app; the code base returns `null`/`false` for refusals (`reportTicket`, `TicketsList.add`). Option in Q7.
- Proof rule "the ticket has any RESOLUTION attachment": no new parameter, but old proof would satisfy a second resolve after a reopen. Option in Q8.
- `object TicketLifecycle { ... }`: an extra name to explain; top-level functions match `TicketPrivacy.kt`.
- A human CLOSED transition for operators: the brief closes after 5 days, not on demand.

## Architecture impact
- `model/` only: one new rules file, one new enum, one new field on `Attachment`, KDoc lines in `Ticket.kt` and `TicketPrivacy.kt`. Imports stay inside `model/` (no `utils/` needed).
- New cross-branch contract: `AttachmentPhase` and `Attachment.phase` (F-010, F-011 mapping, F-013 uploads, D-013), and the lifecycle API (F-014, D-013, F-016). Renaming them later breaks Francisco's branch.
- `Attachment` gains a field the Firestore mapping (F-011) must read and write; a missing field reads as REPORT.
- Docs: decision record (0011, Q6), overview "Rules" section and code-mapping table, report notes.

## Failure modes
- **Clock skew.** The 5-day window is computed on the device. A citizen whose clock is ahead loses the reopen early; one whose clock is behind (or set back on purpose) can reopen late. The Firestore rules (F-011) can check `request.time` against the RESOLVED time if this matters; noted for F-011.
- **History out of order.** `transition` does not reject `now` earlier than the last entry (devices disagree and writes can be queued offline). `ShowScreen` shows stored order, so such an entry appears after a later one. Accepted; recorded.
- **Concurrent transitions.** Two operators (or an operator and a reopening citizen) acting offline on the same ticket: each `transition` is valid locally, the second write wins in Firestore. D-013/F-011 should use a transaction or a precondition on `currentStatus`; noted for them.
- **Callers that copy by hand.** Nothing stops `ticket.copy(currentStatus = ...)`. The KDoc of `currentStatus` and decision 0004 say to use `transition`; the reviewer of D-013/F-014 checks it.
- **Called on a projected ticket.** Returns `null`/`false`/empty, never a wrong ticket; `effectiveStatus` returns the stored status. Anonymous users never see RESOLVED tickets (`publicActive`), so this does not reach a screen.
- **Stored data that breaks the proof rule.** `sample-3` is RESOLVED with only a REPORT photo. The rule applies to transitions, not to stored tickets; nothing reads "has proof" for display yet.

## Risks and open questions
Each has a recommendation; Gate 1 decides.

- **Q1. Does a citizen's question set INFO_REQUESTED?** INFO_REQUESTED reads "Pedido de informações", the municipality asking the citizen.
  - Recommended: **no.** A citizen's question is only a message (F-014's thread); only operators move to and out of INFO_REQUESTED. If the citizen's question moved a ticket that is IN_PROGRESS back to INFO_REQUESTED, the badge would say the council is waiting for the citizen, and the geofence (D-014, IN_PROGRESS) would switch off. The reply to an info request also does not move the ticket; the operator moves it on to IN_PROGRESS.
  - Alternative: citizen OPEN -> INFO_REQUESTED only. Check brief II.2 first.
- **Q2. Who may reopen?** Recommended: the **creator and the followers**, as an ACTIVE `Citizen`, within the window, with a justification. Option A makes followers co-reporters, and F-014's "My tickets" lists created and followed tickets. Alternative: creator only.
- **Q3. Who may move a ticket to CLOSED?** Recommended: **nobody by hand.** Closing is time-based: `effectiveStatus` shows it on read, and `closeIfExpired(now)` writes the CLOSED entry at `resolvedAt + 5 d` with `responsibleId = "system"`, called by whoever sees the ticket (F-011/D-013 repository, or a worker). CLOSED -> anything is rejected for everyone. Alternative: also let operators close a RESOLVED ticket early (not in the brief).
- **Q4. Which transitions need a justification?** Recommended: **required** (non-blank after trim) for any move to INFO_REQUESTED (the question; D-013 also posts it as a message) and for the reopen (F-014 asks for it); **optional** for IN_PROGRESS and RESOLVED; none for the automatic CLOSED. This matches `StatusHistory.justification`'s KDoc ("reason for reopening or for an info request").
- **Q5. `allowedTransitions` signature.** Recommended: `ticket.allowedTransitions(by: User, now): Set<TicketStatus>` (effective status, relation, registration and window inside). Alternative: the task's `allowedTransitions(status, role)` with a small role enum, leaving those checks to `transition` and the callers. Both F-014 and D-013 have the signed-in `User` from D-009's session.
- **Q6. `AttachmentPhase` and decision 0010 (coordination with Francisco).** F-010 owns decision 0010 (Firestore schema) and may add the same field.
  - Recommended: **add `AttachmentPhase { REPORT, RESOLUTION }` and `Attachment.phase` (default REPORT) in D-008**, record them in D-008's own decision record numbered **0011** ("Ticket lifecycle rules"), and tell Francisco today so F-010 uses this enum and 0010 points to 0011. If Francisco has already added it on `dev/Francisco`, use his names and file and drop slice 1.
  - Alternatives: wait for F-010 (blocks D-008's proof rule; F-010 targeted 11-11); or keep the rule without the field (a predicate parameter), which only moves the problem to D-013.
  - Merge risk: `model/Attachment.kt` is Diogo's (D-001); a parallel edit on `dev/Francisco` would conflict on one line, which is easy to resolve.
- **Q7. Rejection style.** Recommended: `transition` returns `Ticket?` (`null` = rejected), like `reportTicket` and `TicketsList.add`; the UI only offers moves from `allowedTransitions`, so `null` means a stale screen or a bug. Alternative: throw with `require` (the task's `: Ticket`), with callers checking first.
- **Q8. Proof rule.** Recommended: proof is a `proof: List<Attachment>` parameter of `transition`, required for RESOLVED and appended with `phase = RESOLUTION` in the same operation, so only fresh proof counts. Alternative: "the ticket already has at least one RESOLUTION attachment" (as worded in the task); then D-013 adds the proof first (`addResolutionProof`) and a re-resolve after reopen can reuse old proof.
- **Q9. Is a unit test worth adding?** The project has no test framework (no JUnit in the catalog, no `app/src/test`; decision 0005 Q1 removed D-002's tests). Recommended: **no committed test.** Verify by build, lint, grep and the check table in the coding plan, walked by the implementer and the reviewer; F-014 and D-013 then check the rules on a device. Alternative: a throwaway JUnit 4 test run locally and not committed (needs a temporary Gradle change in shared files), or a committed one, which reverses decision 0005 Q1.
- **Q10. Resolved samples at 4 and 6 days.** `data/SampleTickets.kt` is Francisco's (F-008). Fixed-epoch samples cannot be "4 days ago" without reading the clock, and new RESOLVED samples change the dashboard figures D-007's checks recorded (3 issues, 67% active, average 3 days, 1 repair).
  - Recommended: **do not edit `SampleTickets` in D-008.** No screen calls the lifecycle rules yet, and RESOLVED tickets are not in the anonymous list, so the samples would show nothing. Hand over to F-014/D-013: their previews build a RESOLVED ticket at a fixed `t0` and pass `now = t0 + 4 d` and `t0 + 6 d`, so the reopen button shows and hides deterministically.
  - Alternative: add `sample-4`/`sample-5` with times relative to `System.currentTimeMillis()` (with Francisco's OK); the dashboard would then show 5 issues, 40% active and an average that depends on the samples' creation times, and D-007's recorded figures would need updating.
- **Q11 (minor). Pending citizens.** Recommended: a `Citizen` whose `registrationStatus` is not ACTIVE gets no transitions. Cheap, and it holds even if a screen forgets the D-009 gate.

Other risks:
- Day late: the task targeted 2026-11-10; F-014 and D-013 depend on it.
- Brief text not re-read in this session (see Sources).

## Out of scope
- Screens, buttons, ViewModels and repository methods (F-014, D-013); notifications and the 24 h job (F-016); the geofence on IN_PROGRESS (D-014).
- Firestore mapping of `phase`, transactions, security rules on the window (F-010, F-011).
- Showing `effectiveStatus` in `StatusBadge` or the detail (F-014/D-013 pass it in).
- Changing `TicketsList`, `DashboardStats` or `SampleTickets` (Q10).
- Notifying participants of a transition, posting the info request as a message (D-013, F-014, F-016).

## Slices
Each leaves the build green and becomes one commit.
1. **Attachment phase.** `model/AttachmentPhase.kt`; `Attachment.phase` (default REPORT); `Attachment` KDoc fixes (F-007 handover N7/Q12); `phase` in the `toAnonymousView()` kept list. `feat(D-008): add attachment phase for resolution proof`
2. **Time rules.** `model/TicketLifecycle.kt` with the constants, `resolvedAt`, `canReopen`, `effectiveStatus`, `isLocked`, `closingWarningAt`; the D-007 N2 line in `Ticket.currentStatus`'s KDoc. `feat(D-008): add reopen window and closing rules`
3. **Transitions.** `allowedTransitions`, `transition`, `closeIfExpired`, `SYSTEM_RESPONSIBLE_ID`. `feat(D-008): add ticket status transitions`

Docs (decision 0011, overview, report notes) follow in the docs phase (`03-docs-plan.md`).

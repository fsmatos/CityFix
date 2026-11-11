# D-008: Review

Reviewed: `git diff 3298ec5..HEAD` (9bb1cb3, 2df8cc4, 1c297f8, e5affbb) against `00-plan.md`, `01-coding-plan.md`, the task file `docs/tasks/phase-2/D-008-ticket-lifecycle-rules.md`, decisions 0003, 0004 and 0006, the consumer tasks F-014, D-013, D-014 and F-016, `docs/architecture/overview.md` and `kotlin-conventions`. Gate 1 (2026-11-11): Q1-Q11 as recommended. (`state.md` gives Base 66691af. `66691af..3298ec5` only holds the start and plan commits, so the code diff is the same.)

Build: `./gradlew assembleDebug lint --rerun-tasks` pass. Lint: 0 errors, 16 warnings (the baseline), none in `Attachment.kt`, `AttachmentPhase.kt` or `TicketLifecycle.kt`.

## Summary
The code does what the coding plan says, function by function, and the rules run in the planned order. Only `model/` changed: one new file for the rules, one new enum, one new field with a default, and KDoc lines. `TicketLifecycle.kt` has no `import` lines at all, so there is nothing from Android or Firebase. It never calls `System.currentTimeMillis`. Each function is a few plain `if`s and one exhaustive `when` with no `else`, so both authors can explain it at the defence. I found no blocking issue.

### Example table, walked again independently
Fixtures as in `01-coding-plan.md` (`t0 = 1_791_100_800_000`, `sample-3`'s RESOLVED time; `REOPEN_WINDOW_MS = 432_000_000`).
- **T1-T6, the boundaries.** `resolvedAt()` = `t0`.
  - At 4 d 23 h (428 400 000 ms): `canReopen` is true (`< 432 000 000`).
  - At exactly 5 d: `canReopen` is false and `effectiveStatus` is CLOSED. Both test the same difference, one with `<` and one with `>=`, so the two are never true together.
  - At 5 d 1 h: `canReopen` is false.
  - At 4 d: `effectiveStatus` is RESOLVED and `isLocked` is false.
  - `closingWarningAt()` = `t0 + 345 600 000`.
  - `closeIfExpired` at exactly 5 d closes the ticket (`!(x < W)`), which agrees with `effectiveStatus`.
- **T7.** `sample-1` (OPEN): `resolvedAt`, `closingWarningAt` and `canReopen` return null, null and false, because each checks the status first.
- **T8 / R16, a projected ticket.** `resolvedAt` returns null (`lastOrNull` on an empty history). `canReopen` is false. `isFullTicket()` is false, so `allowedTransitions` returns the empty set, and `transition` and `closeIfExpired` return null. `effectiveStatus` returns the stored status, as the file's KDoc says.
- **R1, R2, R7.** Operator moves from OPEN and IN_PROGRESS work. The justification is trimmed. A null or blank justification for INFO_REQUESTED is rejected.
- **R3-R6, the proof rules.**
  - OPEN -> RESOLVED is not in `operatorTargets(OPEN)`, so it is rejected.
  - IN_PROGRESS -> RESOLVED with no proof is rejected.
  - With proof `p1` it is accepted, with `phase = RESOLUTION` and `uploadedById = "operator-1"`.
  - A proof id that is blank, repeated inside `proof`, or already on the ticket is rejected.
  - Proof sent with any other target is rejected.
- **R8, R10, R15.** These are about who may make a move.
  - A citizen on OPEN gets nothing.
  - A follower can reopen. A citizen who is not a participant cannot, and neither can a PENDING one.
  - An operator gets nothing on RESOLVED.
- **R9, R11, R12, reopening.** A reopen adds an OPEN entry by `citizen-1` that holds the justification. After it, `resolvedAt` is null and `canReopen` is false. A reopen with no justification is rejected, and so is one at 5 d 1 h.
- **R13, CLOSED never leaves.**
  - Stored CLOSED: `operatorTargets(CLOSED)` is empty. On the citizen side, `canReopen` needs `currentStatus == RESOLVED`. `closeIfExpired` returns null.
  - Effectively CLOSED (RESOLVED, 6 d): `operatorTargets` is called with the effective status, so it is empty. `canReopen` is false.
  - Nothing ever lets anyone move a ticket to CLOSED.
- **R14.** At `t0 + 6 d` the result has one CLOSED entry at `t0 + 432 000 000` by `"system"`. At `t0 + 4 d`, or on `sample-1`, the result is null.
- **One history entry per transition.** There are two places that write status:
  - `transition`'s single `copy`, which appends one `StatusHistory(to, now, reason, by.id)` and sets `currentStatus = to`.
  - `closeIfExpired`'s single `copy`, which appends one CLOSED entry and sets `currentStatus = CLOSED`.

  Grep shows no other `StatusHistory(` in `model/`. The other matches are `LocalTicketsRepository`'s first OPEN entry, the sample data and the `ShowScreen` preview.

### Does the new `phase` field change anything else? No.
- All five `Attachment(` call sites use named arguments: `ReportViewModel.attachmentFor`, `SampleTickets`, and the `ReportScreen` and `ShowScreen` previews. All of them get `REPORT` from the default. Nothing reads `phase` outside `TicketLifecycle.kt`.
- `isActive`, `publicActive()`, `findNearby()` and `stats()` read `currentStatus` and the last RESOLVED entry. D-008 changes neither: nothing calls the new functions yet, and RESOLVED and CLOSED are both inactive. If `closeIfExpired` adds a CLOSED entry later, it comes after the RESOLVED entry, so the repair time stays the same. The dashboard therefore stays at 3 issues, 67% active, 3 days and 1 repair.
- `toAnonymousView()` lets `phase` through (the KDoc list was updated). `phase` is not personal data. `SYSTEM_RESPONSIBLE_ID` and every justification live in the history, which the projection drops.
- i18n, accessibility, orientation, permissions and MVVM: not affected (there are no strings, UI, permissions or ViewModels in this diff).

### Does the API cover the consumer tasks?
- **F-014.** Reopen uses `allowedTransitions(user, now)` and `transition(OPEN, ...)` with the justification required. Read-only uses `isLocked(now)`. The "citizen question sets INFO_REQUESTED" option was dropped at Gate 1 (Q1). See S1 for how the button should be shown.
- **D-013.** It offers exactly `allowedTransitions(operator, now)`. Resolve passes the proof with the transition (Q8), so D-013's planned `addResolutionProof` method is no longer needed. The docs plan already hands this over. Read-only uses `isLocked`/`effectiveStatus`.
- **F-016.** It schedules at `closingWarningAt()`. See S2.
- **D-014.** It only needs the status to be IN_PROGRESS, which `currentStatus`/`effectiveStatus` give (an expired ticket is never IN_PROGRESS).

### Rules that depend on the brief (PDF not readable in this session)
These come from the task files, not the brief. Diogo should check them in the PDF before the defence:
- Brief II: the operator transition table, in particular OPEN -> IN_PROGRESS directly and no INFO_REQUESTED -> RESOLVED. Also the 5-day window and the "closed and locked" rule.
- Brief II.2: whether the citizen's question moves the status (Q1 says no).
- Brief II or I.2.E: whether followers may reopen, or only the author (Q2).
- Brief III: the 24 h warning before closing (the KDoc of `CLOSING_WARNING_MS` and `closingWarningAt` cites it).
- Brief I.3.E: the proof media requirement.

## Blocking
None.

## Should-fix
- **S1. `canReopen` alone does not mean the reopen will be accepted.** F-014's task text says to show the button "only when D-008's `canReopen(ticket, now)` is true", and the docs plan says "(or `canReopen`)". `canReopen` only checks the time. It does not check the user (an operator, a PENDING citizen or a non-participant) or `isFullTicket()`. Take a broken ticket with `currentStatus = RESOLVED` and history `[OPEN, RESOLVED, OPEN]`: `canReopen` is true there, but `transition` rejects the reopen. A button shown because of `canReopen` could then do nothing.
  Fix (KDoc only, `TicketLifecycle.kt`): add a line to `canReopen` such as "Time rule only. To decide whether to show Reopen, use `TicketStatus.OPEN in allowedTransitions(user, now)`." In the docs phase, drop "(or `canReopen`)" from the F-014 bullet in the overview. Optionally start `canReopen` with `if (!isFullTicket()) return false`, so that a true result always means `transition` will accept it for an ACTIVE participant. T2, T3, T4 and T8 still hold with that guard.
- **S2. `closingWarningAt()` can return a time in the past.** It checks only the stored status. A RESOLVED ticket 6 days old that nobody has closed yet still returns `t0 + 4 d`. If F-016 schedules a one-time job at that time, WorkManager runs it at once and tells the citizen "closes in 24 h" about a ticket that is already locked.
  Fix (KDoc only): add "May be in the past. Before notifying, check `!isLocked(now)` (or `closingWarningAt()!! > now` when scheduling)" to `closingWarningAt`'s KDoc. Also put the same line in the F-016 handover bullet during the docs phase.

## Nit
- **N1. `"system"` as `responsibleId` affects F-011 rules and the history display.** A Firestore rule such as `statusHistory[last].responsibleId == request.auth.uid` would reject the CLOSED write from `closeIfExpired`. An operator/citizen history view that looks up the responsible user's name will find no `users/system` document.
  Fix: in the docs phase, add one line to the F-011 and D-013 handover notes. F-011's rules allow `responsibleId == "system"` only for a CLOSED entry at `resolvedAt + 5 d`. The history UI shows `SYSTEM_RESPONSIBLE_ID` as a string resource ("Closed automatically" / "Encerrado automaticamente") rather than looking up a user.
- **N2. Proof from an earlier resolve stays on a reopened ticket and is shown as if it were report media.** After a reopen the ticket is active again, so it is back in the anonymous list. Its RESOLUTION attachments pass through `toAnonymousView()` and `ShowScreen` shows them next to the report photos with no label. This is not personal data (`uploadedById` is blanked), and `TicketCard` still uses `attachments.firstOrNull()`, which is the report photo because proof is appended.
  Fix: one line in the D-013/F-014 handover (docs phase). Detail screens group or label attachments by `phase`, and the thumbnail keeps using the first REPORT attachment (`attachments.firstOrNull { it.phase == AttachmentPhase.REPORT }`) once proof exists.
- **N3. Decision 0003 still says `Attachment.file` is a "local content URI before upload".** The new KDoc ("absolute local path", F-007 N7) is right, so the decision record now disagrees with the code. The docs plan does not list 0003.
  Fix: in the docs phase, add a one-line amendment to 0003 (or a note in the overview's code-mapping row for `Anexo`): "`file` is an absolute local path before upload (F-007)".
- **N4. In `transition`, `isReopen`'s `currentStatus == TicketStatus.RESOLVED` is always true when `to == OPEN`.** OPEN is only ever allowed through `canReopen`, which needs RESOLVED. Keep it: it states the rule from the KDoc in the code. Be ready to say this at the defence if asked "why both checks". No change needed.

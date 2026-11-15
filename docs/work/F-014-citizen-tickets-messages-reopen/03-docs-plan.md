# F-014 docs plan

## Docs to update
- `docs/architecture/overview.md`:
  - New "Citizen tickets, messages and reopen (F-014)" after F-013: `detailAudience` table, My tickets, thread + `MessageThread` API, `TicketDetailViewModel`, reopen, `commitStatusChange`, close-on-read, class vs beyond class, limits, D1-D15 status (accounts by role only).
  - New "Handover (F-014)": D-013 routes operators to the detail, reuses `MessageThread`, slot, ViewModel and `commitStatusChange` (exposed as `changeStatus`), posts the INFO_REQUESTED reason via `sendMessage(..., operator)`, attachment `phase`. F-016: notify on others' messages and status changes, 24 h warning via `closingWarningAt()`, wait for `SignedIn`. D-010, D-015: nothing. List Diogo's changed files.
  - Update: "ViewModel (D-003)", "App shell (D-006)" (audience, `canExpandCards`); "Navigation (D-004)" table; "Handover (D-009)", "Handover (F-011)" (`resolvedAt`, Q13); "Open: lifecycle fields" point 1 (once Diogo agrees); "Lifecycle (D-008)"; "Screens".
- 0007: exact map, justifications, slot, `detailAudience` from `ticketsUser`, OPERATOR, `now`.
- 0010: `byOperator`, `authorId` = uid, `document().set()`, `resolvedAt` not read, reopen sets `active`; fix line 73 ("0011" -> "0012").
- 0011 (Diogo's): II.2 Context line only with permission, else in F-014's record.
- 0013: `resolvedAt` decided; D-009's stand-in line. 0014: D-009's line (optional).
- D-009's 0015: do not edit; suggested line in the handover.
- Task file: "done" + Gate 1 changes. `docs/report/notes.md`: below.

## Decisions to record
`docs/decisions/0018-citizen-tickets-messages-reopen.md` (Q15): Gate 1 outcome of Q1-Q15 (Q1 brief check, Q2 by D-009), rejected alternatives (incl. top-bar icon), D-013/F-016 contracts, limits.

## Report notes
- Messages subcollection: live, cached, queued writes (`hasPendingWrites` option).
- Reopen and close: one batch each (0004, D-008, mirror, `active`, counters); no server code (0012).
- One `detailAudience`; anonymous never creates the messages ViewModel.
- Brief II.2 reading (Q1).
- Verified with real MFA accounts, operator via REST, real grouping.
- Improvements: rules, transactions, unread counters, pagination, detail listener.

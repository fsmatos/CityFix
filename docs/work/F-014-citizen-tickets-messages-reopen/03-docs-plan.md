# F-014 docs plan

## Docs to update
- `docs/architecture/overview.md`:
  - New section **"Citizen tickets, messages and reopen (F-014)"** after "Media upload (F-013)":
    - `detailAudience` table (null / Citizen participant / Citizen other / Operator -> audience);
    - "My tickets" (filter on `TicketsViewModel.tickets` with `isParticipant`, effective badge, top-bar entry for citizens only);
    - the thread (`tickets/{id}/messages`, live, queued offline, labels, `byOperator`, stateless `MessageThread` API);
    - `TicketDetailViewModel` (per destination, created only for REGISTERED/OPERATOR, so anonymous users never read messages);
    - reopen (the `allowedTransitions` gate, dialog, `transition`);
    - `commitStatusChange` batch (ticket status fields, `publicTickets` mirror, `ticketLocations.active = isActive`, `statsIncrements`, not awaited);
    - close-on-read (`LaunchedEffect` in the detail, `closeIfExpired`, writes once);
    - class material and beyond;
    - known limits (device clock, last-write-wins, possible double CLOSED offline, hand-made test data, the map inside a scroll, followers read each other's messages);
    - device-check status D1-D14.
  - **"Handover (F-014)"**:
    - D-013: reuse `MessageThread(messages, operatorId, ...)` through `ShowScreen`'s `participantContent` slot and `TicketDetailViewModel` (or its pattern); write operator transitions with `commitStatusChange` (expose it on `TicketsRepository` as `changeStatus(before, after)`, or call `transition` then the same batch shape); post the INFO_REQUESTED justification with `sendMessage(..., operator)`; label attachments by `phase`; `detailAudience` already returns OPERATOR;
    - D-009: replace `app.currentUser` in `MainActivity` (detail audience, `showMyTickets`, My tickets filter) with the signed-in user; `TicketsViewModel.setUser` as before; PENDING citizens get no reopen (D-008 Q11) but can still message: decide in D-009 whether they reach the app at all;
    - F-016: notify on new `messages` docs whose `authorId` is not the user (II.2 "a outra parte é notificada") and on status changes; the 24 h warning uses `closingWarningAt()` + `!isLocked(now)`;
    - D-015: nothing new (no files to exclude).
    - List of Diogo's files changed by F-014.
  - Update "ViewModel (D-003)": the "screens still render PUBLIC for everyone" line becomes "since F-014 the detail audience comes from `detailAudience`".
  - Update the "Lifecycle (D-008)" "How F-014 uses it" bullet: done, and how.
  - Add a "Screens" entry for `MyTicketsScreen` and `TicketDetailScreen` (if the overview has one per screen).
- `docs/decisions/0007-ticket-detail-by-audience.md`, amendment (F-014):
  - REGISTERED/OPERATOR get the exact map, justifications, "Closed automatically" and the participant slot;
  - `detailAudience(user, ticket)` is the mapping, and OPERATOR is mapped now;
  - `ShowScreen` gains `now`.
- `docs/decisions/0010-firestore-schema.md`, amendment (F-014):
  - message docs carry `byOperator` (bool);
  - messages are written with `document().set()`, not awaited;
  - `resolvedAt` is not read by clients (the window uses the RESOLVED history entry), which replaces "clients compute the reopen window ... from `resolvedAt`";
  - the reopen batch writes `ticketLocations.active = true` and does not clear `resolvedAt`.
- `docs/decisions/0011-ticket-lifecycle-rules.md`: one line under Context: "Brief II.2 checked in F-014: citizen clarification requests are listed under state 2; Q1 kept, see decision 0016". **This is Diogo's file: ask him or the person first; otherwise record it only in 0016.**
- `docs/decisions/0013-firestore-reads-and-dashboard-counters.md`, Consequences: "`resolvedAt` ... D-013/F-014 decide" -> decided by F-014 (not mapped).
- `docs/tasks/phase-2/F-014-citizen-tickets-messages-reopen.md`: status "done" plus a short note on what Gate 1 changed (author passed to `sendMessage`, Q1 outcome). Only if the task-file convention allows editing status; the earlier tasks did.
- `docs/report/notes.md`: the report notes below.

## Decisions to record
- **`docs/decisions/0016-citizen-tickets-messages-reopen.md`** (number to confirm with Diogo, Q11). Context: brief I.2.E, II.2, II.4, II.5 quoted from the brief. Decision: the Gate 1 outcome of Q1-Q12, in particular:
  - Q1: citizen questions are plain messages. Brief II.2 checked; why D-008's reading was kept;
  - Q2: the top-bar entry and `MyTicketsRoute`;
  - Q3: queued messages, no sending marker;
  - Q4: `byOperator`;
  - Q5: public `isParticipant`;
  - Q6: close-on-read only in the detail;
  - Q7: `resolvedAt` not mapped;
  - Q8/Q9: exact map and OPERATOR audience;
  - Q12: last-write-wins.
  - Rejected: a `ticket(id)` listener, a `myTickets` flow in `TicketsViewModel`, a `LazyColumn` thread, awaiting `commit()`, citizen -> INFO_REQUESTED.
  - Consequences: the D-013/F-016 contracts (`MessageThread` API, `commitStatusChange`, the `participantContent` slot) and the known limits.

## Report notes
For `docs/report/notes.md` (PDF technical report):
- **Messages as a subcollection** read through a snapshot listener: live on both sides, served from the cache offline, and sent with queued writes (`document().set()` without waiting), so a message written in airplane mode shows at once and syncs later. Option: a "sending" marker from `hasPendingWrites` (not done).
- **Reopen and the 5-day close are one batch** each:
  - the ticket's status and history change together (decision 0004), through the pure `transition` / `closeIfExpired` rules (D-008);
  - the public mirror, the grouping flag (`ticketLocations.active`) and the dashboard counters (`FieldValue.increment` deltas) change in the same batch, so every reader agrees;
  - close-on-read: the first participant who opens an expired ticket writes CLOSED, dated when it really closed (`resolvedAt + 5 d`), by `system`; no server code (decision 0012).
- **Audience by role and relation:** the same `ShowScreen` serves anonymous, participant and operator views from one `detailAudience(user, ticket)` function, and the anonymous path never creates the ViewModel that reads messages (privacy enforced in the data path, not only hidden in the UI).
- **Brief reading (II.2):** the citizen's clarification request is a plain message, not a status change; explain why (meaning of "Pedido de Informações", geofence of D-014, notification by F-016).
- **Improvement opportunities:**
  - server-side checks (the drafted rules check the window on server time but are not deployed);
  - transactions or preconditions against concurrent status writes;
  - unread counters and notifications per message (F-016);
  - pagination of long threads;
  - a dedicated document listener for the detail instead of the shared list.

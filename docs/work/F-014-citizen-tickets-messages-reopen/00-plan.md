# F-014: Citizen: my tickets, message thread and reopening
Type: feature · Task file: docs/tasks/phase-2/F-014-citizen-tickets-messages-reopen.md
Labels: (none)

## Problem
- Brief I.2.E (registered citizen): "Listagem de todas as ocorrências (do utilizador), com o estado de cada uma delas"; "Detalhe de uma ocorrência, com acesso a troca de mensagens com os serviços camarários"; "Reabertura de uma ocorrência (se dentro de 5 dias após conclusão)".
- Brief II.2: requests for information go both ways ("O munícipe pode também pedir esclarecimentos ... a outra parte é notificada"); the notification is F-016. II.4: "Durante 5 dias o Munícipe pode reabrir o ticket, justificando a decisão". II.5: "Após 5 dias, o ticket é arquivado e bloqueado, não admitindo mais alterações".
- Today:
  - There is no "my tickets" screen. A citizen stand-in already receives their full tickets (`participantTickets`, F-011), mixed into the general list.
  - `MainActivity` always opens the detail with `DetailAudience.PUBLIC` (no login yet), so even the author never sees the description, the history or a thread.
  - Nothing reads or writes `tickets/{id}/messages` (decision 0010 reserved it for F-014).
  - D-008's lifecycle rules (`allowedTransitions`, `transition`, `isLocked`, `closeIfExpired`) have no caller. No batch writes a status change, so no reopen and no close-on-read (decision 0012 Q4).
- Handover items owned by F-014:
  - `ticketLocations.active = true` in the reopen batch (decision 0014);
  - `statsIncrements(before, after)` and the `publicTickets` status mirror in every status batch (decision 0013);
  - show "Closed automatically" for `SYSTEM_RESPONSIBLE_ID` (decision 0011 N1);
  - badges from `effectiveStatus(now)` (decision 0011);
  - decide whether `resolvedAt` is mapped (decisions 0010, 0013).
- D-009 (login) is not done. The `CityFixApplication.currentUser` stand-in stays (`null` = anonymous).

## Outcome
- **"My tickets"** (citizens only). A top-bar action opens a new route with the tickets the user created or follows, any scope and any status, newest first. Each card shows the effective status badge, so an expired RESOLVED ticket shows CLOSED. There is an empty state.
- **Citizen detail.** On a ticket they take part in, a citizen gets `DetailAudience.REGISTERED`: header with the effective status, "Reported on", description, photos/videos, the exact location (map with a marker), the status history (with "Closed automatically" and justifications), then the participant section with the Reopen button and the message thread. On anyone else's public ticket the view stays PUBLIC (decision 0007).
- **Message thread.** `ui/components/MessageThread` shows the messages oldest first, each with its author label ("You" / "Municipality" / "Citizen") and its date and time, followed by a text field and a Send button. Messages arrive live from `tickets/{id}/messages`. Sending works offline: the write is queued and the message shows at once from the cache. The component is stateless and does not know the role, so D-013 reuses it for operators.
- **Reopen.** The button shows only when `TicketStatus.OPEN in ticket.allowedTransitions(user, now)`. It opens a dialog that asks for a justification (Confirm refuses a blank one). One batch then writes the ticket's `currentStatus` + `statusHistory` (via D-008's `transition`), the `publicTickets` mirror (PUBLIC only), `ticketLocations.active = true` and `statsIncrements(before, after)`.
- **Closed is read-only.** When `isLocked(now)`, there is no Reopen and no input field, only a "closed" note. When a participant opens an expired RESOLVED ticket, the app writes D-008's `closeIfExpired` result with the same batch shape (`active = false`), so Firestore says CLOSED too.
- Anonymous users never see the "My tickets" action, and the PUBLIC detail never creates the detail ViewModel. So the anonymous path never reads `messages`.

## Acceptance checks
Run on an emulator with stand-in users. The full list (D1-D14) with preconditions and the REST commands is in `01-coding-plan.md`.
- [ ] A citizen sees only their own and followed tickets in "My tickets" (citizen-1: sample-1, sample-3, not sample-2; citizen-3 sees sample-2 as a follower).
- [ ] A message sent on one device appears on the other without a refresh, in both directions. Sent in airplane mode, it shows at once and reaches Firestore once the network is back (REST GET).
- [ ] Reopen shows on a ticket resolved 4 days ago and not on one resolved 6 days ago. Confirm needs a non-blank justification. After a reopen, `tickets`, `publicTickets`, `ticketLocations` and `stats/summary` agree (REST GET).
- [ ] An expired RESOLVED ticket (sample-3) shows CLOSED, has no input field and no Reopen, and opening it writes the CLOSED entry by `system` at `resolvedAt + 5 d`.
- [ ] Anonymous users (`currentUser = null`) never see "My tickets", and the detail never shows the thread.
- [ ] Portrait and landscape, EN and PT. The dialog text and the message draft survive rotation.
- [ ] Build and lint pass after every slice (0 errors).

## Approach
Follows class Contacts_#3 / P08:
- A per-destination ViewModel built with a factory inside the `composable<...>` (EditContactViewModel / `EditContactViewModelFactory`), holding a `MutableStateFlow<UiState>` updated with `update { it.copy(...) }`.
- The stateful screen collects with `collectAsStateWithLifecycle` and hands plain values and callbacks to stateless composables (slide 07).
- Type-safe `@Serializable` routes and the `hasRoute` top-bar mapping (D-004/D-005).
- `AlertDialog` (P06, P08 challenge "confirmation AlertDialog") and `OutlinedTextField` (Contacts EditScreen).
- Firestore follows T8 AMOVStorageDemo and slide 09: `hashMapOf` documents, `collection(...).document()` + `set`, `doc.getString(...) ?: ""`, `db.batch()` + `commit()` with `addOnFailureListener`.
- It reuses F-011's live `listen` helper (snapshot listener as a `Flow`), which went beyond class at F-011's Gate.

Departures are marked **beyond class**.

**1. Messages in the repository (`TicketsRepository` / `FirestoreTicketsRepository`).**
- `messagesFor(ticketId): Flow<List<Message>>` listens to `tickets/{id}/messages` with the existing private `Query.listen`, maps with `toMessage()` and sorts by `sentAt` on the client. That matches F-011's "sort on the client, no index" choice; `orderBy("sentAt")` would also need no index, but it is one more API.
- `sendMessage(ticketId, text, author: User): Boolean` trims the text and returns false when it is blank. Otherwise it does `collection(TICKETS).document(id).collection(MESSAGES).document().set(messageDocument)`. The id is made locally, as in `reportTicket`. The call is not awaited and only `addOnFailureListener` logs. Offline, Firestore queues the write and the local cache shows it to the listener at once (Q3). Fields (decision 0010): `text`, `sentAt` (client `Long`), `authorId`, plus `byOperator` (Q4).
- The task's `sendMessage(ticketId, text)` gains the author, because the repository has no user (as `reportTicket(..., creatorId)`).
- `Ticket.messages` (model) stays unused. The thread comes from the subcollection, never from the ticket document (decision 0010).

**2. Status-change batch (reopen and close-on-read).**
- A new private `commitStatusChange(before: Ticket, after: Ticket)` builds one `firestore.batch()`:
  1. `update(tickets/{id}, "currentStatus", "statusHistory")` from `after` (new internal `Ticket.toStatusFields()` in `FirestoreMapping.kt`);
  2. `update(publicTickets/{id}, "currentStatus", ...)` when `scope == PUBLIC` (the mirror always exists for PUBLIC tickets, created in the same batch as the ticket);
  3. `update(ticketLocations/{id}, "active", after.isActive)`: true on reopen, false on close (decisions 0010, 0014);
  4. `set(stats/summary, statsIncrements(before, after), merge())`. On a reopen: active +1, resolved -1, repair sum minus the earlier repair. On a close: all zeros, kept so the rule "every status batch adds it" holds without exceptions.
  - `commit()` is not awaited; failures are only logged (same as `reportTicket`).
- `reopenTicket(ticket, by, justification): Boolean` = `ticket.transition(OPEN, by, now, justification) ?: return false`, then `commitStatusChange`. D-008 already rejects a blank justification, a non-participant, a PENDING citizen and an expired window, so the repository adds no checks of its own (school-project rule).
- `closeExpiredTicket(ticket)` = `ticket.closeIfExpired(now) ?: return`, then `commitStatusChange`. Its CLOSED entry is dated `resolvedAt + 5 d`, by `"system"`.
- The batch rewrites the whole `statusHistory` array from the local copy. That is last-write-wins: no transaction, because transactions fail offline (decision 0010) (Q12).
- `resolvedAt` (Firestore-only) is not mapped and not cleared. The window comes from the RESOLVED history entry, as `DebugSeed` and D-008 already do (Q7).
- D-013 can reuse `commitStatusChange` for operator transitions (handover).

**3. Who sees what (`detailAudience`).**
- A new top-level `fun detailAudience(user: User?, ticket: Ticket?): DetailAudience` next to the enum in `ShowScreen.kt`, with an exhaustive `when` on the sealed `User`:
  - `null` -> PUBLIC;
  - `Citizen` -> REGISTERED when the ticket is not null and they are its creator or a follower (`isParticipant`, Q5), otherwise PUBLIC;
  - `Operator` -> OPERATOR (Q9).
- This is decision 0007's "REGISTERED needs the creator/follower check". Projected public tickets have a blank `creatorId` and no followers, so they always map to PUBLIC.
- "My tickets" uses the same check: `tickets.filter { it.isParticipant(user.id) }`, taken from the `TicketsViewModel.tickets` list `MainActivity` already collects. No new listener and no change to Diogo's `TicketsViewModel`.

**4. `ShowScreen` (stateless, Francisco's F-005 file).**
- New required `now: Long` after `audience`. The header badge shows `ticket.effectiveStatus(now)`. That is safe for PUBLIC too: a projected ticket has an empty history, so `effectiveStatus` returns the stored status.
- New trailing slot `participantContent: @Composable ColumnScope.() -> Unit = {}`, drawn in the location pane after the history, **only** in the REGISTERED/OPERATOR branch of the existing `when (audience)`. So PUBLIC can never show it, and D-013 puts its operator actions and the same thread there. A slot is the Compose content-lambda pattern of `Scaffold(topBar = { ... })` (class 04/07).
- REGISTERED/OPERATOR get the exact map (`TicketMap(exact = true)`, F-012: marker, zoom 15) instead of `CoarseLocationMap` (Q8). PUBLIC is unchanged.
- History rows: a `responsibleId == SYSTEM_RESPONSIBLE_ID` row adds "Closed automatically", and a row with a justification shows it underneath (Q10). This is still REGISTERED/OPERATOR only, because PUBLIC never draws the history.
- KDoc updated: the screen now reads justifications for REGISTERED/OPERATOR. It still never reads author ids, follower ids or uploader ids.

**5. Detail with thread and reopen (new `viewmodel/TicketDetailViewModel.kt` + `TicketDetailUiState.kt` + factory, `ui/screens/TicketDetailScreen.kt`).**
- `TicketDetailViewModel(repository, ticketId, user)`:
  - `uiState` = messages, draft, `isReopenDialogOpen`, justification, `reopenError: Int?` (string res, as `ReportUiState`);
  - `init` collects `messagesFor(ticketId)` in `viewModelScope`;
  - functions: `onDraftChange`, `sendMessage()` (clears the draft when sent), `onReopenClick`, `onJustificationChange`, `onReopenDismiss`, `confirmReopen(ticket)` (blank -> error "Write why..."; repository false -> error "can no longer be reopened"; else closes the dialog), `closeIfExpired(ticket)`.
  - The ticket itself is not copied into the ViewModel. It keeps coming from the live `TicketsViewModel.tickets` list, as today, so the badge flips to OPEN or CLOSED from the listener.
- `TicketDetailScreen(ticket, user, viewModel)` is stateful:
  - it collects the state and reads `now = System.currentTimeMillis()`;
  - `LaunchedEffect(ticket) { ticket?.let(viewModel::closeIfExpired) }` (Q6). After the local write the listener sends the CLOSED ticket and `closeIfExpired` returns null, so it writes once;
  - it calls the stateless `TicketDetailContent(ticket, user, now, state, callbacks...)`.
- `TicketDetailContent` calls `ShowScreen(ticket, detailAudience(user, ticket), now) { ... }`. Inside the slot:
  - the Reopen button when `OPEN in allowedTransitions(user, now)`;
  - `MessageThread(..., canSend = !ticket.isLocked(now))`;
  - the `ReopenDialog` (`AlertDialog` + `OutlinedTextField`) while `isReopenDialogOpen`.
  - Previews at a fixed `t0` with `now = t0 + 4 d` (Reopen shown) and `t0 + 6 d` (CLOSED, read-only), as decision 0011 Q10 suggests.
- `MainActivity`, `composable<TicketDetailRoute>`: compute `detailAudience(app.currentUser, ticket)`.
  - PUBLIC: today's `ShowScreen(...)` call (plus `now`).
  - Otherwise: build `TicketDetailViewModel` with `TicketDetailViewModelFactory(app.ticketsRepository, id, user)` inside the destination (scoped to the back-stack entry, like `ReportViewModel`) and call `TicketDetailScreen`.
  - So an anonymous user never creates the ViewModel and never listens to `messages`, which keeps the rule in the data path, not only in the UI (CLAUDE.md).

**6. Message thread component (`ui/components/MessageThread.kt`, stateless).**
- `MessageThread(messages, currentUserId, draft, canSend, onDraftChange, onSendClick, modifier)`:
  - a "Messages" heading, then the messages as a plain `Column` + `forEach`, because it sits inside the detail's `verticalScroll` and a `LazyColumn` there would crash (infinite height); threads are short;
  - an empty text when there are no messages;
  - each message has its label, text and `formatDateTime(sentAt, locale)`. Own messages are aligned to the end with a different container colour. The whole message is merged for TalkBack;
  - when `canSend`: an `OutlinedTextField` ("Write a message") and a 48 dp `IconButton` with `Icons.AutoMirrored.Filled.Send` (core icons), disabled while the draft is blank;
  - otherwise the note "This ticket is closed. No more messages can be sent.".
- Label: `authorId == currentUserId` -> "You", `byOperator` -> "Municipality", else "Citizen". No user lookup: there is no `users` read before D-009 and it needs no network.

**7. "My tickets" (new `ui/screens/MyTicketsScreen.kt`, route and top bar).**
- `MyTicketsScreen(tickets, now, onTicketClick, modifier)` is stateless: a `LazyColumn` with stable keys over `TicketCard` and the empty state "You have not reported or joined any issue yet.".
- `TicketCard` (Francisco's F-002) gains `status: TicketStatus = ticket.currentStatus`, so this screen passes `effectiveStatus(now)`. Other callers do not change.
- Diogo's files, kept small:
  - `Destinations.kt`: `@Serializable data object MyTicketsRoute`;
  - `MainTopAppBar.kt`: `TopBarScreen.MY_TICKETS`, title "My tickets", and an action `IconButton(Icons.Filled.Person, "My tickets")` shown when the new `showMyTickets` parameter is true and the screen is neither MY_TICKETS nor REPORT;
  - `MainActivity.kt`: `showMyTickets = app.currentUser is Citizen`, `onMyTicketsClick = navigate(MyTicketsRoute) { launchSingleTop = true }`, the `composable<MyTicketsRoute>` that filters `tickets` with `isParticipant(user.id)`, and `toTopBarScreen()` gets `hasRoute<MyTicketsRoute>()`.
- Taps open the existing `TicketDetailRoute`, so the same detail serves both lists (Q2).

**Alternatives considered:**
- A separate `ticket(id)` document listener in the detail ViewModel. Rejected: the ticket is already live in `TicketsViewModel.tickets`, so it would be a second source of truth.
- A `myTickets` flow in `TicketsViewModel`. Rejected: it changes Diogo's ViewModel when a filter is enough.
- A `LazyColumn` thread in its own pane. Rejected: nested scrolling and a different layout per orientation.
- A citizen question moving the ticket to INFO_REQUESTED (Q1).
- Awaiting `commit()` to show "sent". Rejected: offline it never completes until the network returns, as F-011 found.

## Architecture impact
- `data/`:
  - `TicketsRepository` + `FirestoreTicketsRepository`: 4 members (`messagesFor`, `sendMessage`, `reopenTicket`, `closeExpiredTicket`) and the private `commitStatusChange`;
  - `FirestoreMapping`: `MESSAGES`, `messageDocument(...)`, `DocumentSnapshot.toMessage()`, `Ticket.toStatusFields()`.
- `model/` (Diogo's, Q4/Q5): `Message.byOperator: Boolean = false`; `isParticipant` made public in `TicketLifecycle.kt`. Both need Diogo told before the commit.
- `viewmodel/`: new `TicketDetailViewModel`, `TicketDetailUiState`, `TicketDetailViewModelFactory`.
- `ui/`:
  - new `screens/TicketDetailScreen.kt`, `screens/MyTicketsScreen.kt`, `components/MessageThread.kt`;
  - changed `ShowScreen.kt` (`now`, slot, exact map, history labels, `detailAudience`) and `TicketCard.kt` (`status`);
  - Diogo's `navigation/Destinations.kt`, `navigation/MainTopAppBar.kt`, `MainActivity.kt`.
- Strings EN + PT (about 20).
- Firestore: `tickets/{id}/messages/{id}` written for the first time; message docs get `byOperator` (amends decision 0010). No rules deploy and no dependency change.
- New decision record 0016 (number to confirm with Diogo, Q11); amendments to 0007, 0010, 0013.

## Failure modes
- **Status batch fails as a whole** if a document it `update`s is missing (for example a hand-made test ticket without `ticketLocations`). Failures are only logged and the local cache rolls back. All app-created and seeded tickets have the companions.
- **Cold start on the detail route:** `tickets` is empty until the first snapshot, so the screen shows "Ticket not found" for a moment (known, F-011).
- **Listener error** on `messages`: Firestore stops that listener and the thread keeps the last list until the screen is reopened (same as F-011).
- **Clock skew:** the device clock decides the 5-day window and `sentAt` order (decision 0011). The window can open or close a few minutes off.
- **Concurrent writes:** two participants closing the same expired ticket while offline can append two CLOSED entries. A reopen racing an operator change is last-write-wins (Q12).
- **Offline reopen after the window:** test mode accepts it (the drafted rules would refuse it on server time; not deployed, decision 0012 Q5).
- **Message to a ticket closed on another device** while this one was offline: test mode accepts it (rules not deployed).
- **Exact map inside a vertical scroll:** a vertical drag on the map may move the map instead of the page (zoom buttons and two-finger gestures still work).

## Risks and open questions
Recommended defaults in **bold**; Gate 1 decides.
- **Q1 Does a citizen's question change the status?**
  - **No: a plain message (keep D-008 Q1).** Now checked against the brief: II.2 lists the citizen's clarification requests under state 2 "Pedido de Informações", so the brief can be read as "a citizen question sets INFO_REQUESTED". Keep D-008's reading anyway:
    - INFO_REQUESTED then means "the council waits for the citizen";
    - moving IN_PROGRESS back would switch off D-014's geofence;
    - "a outra parte é notificada" is met by F-016 notifying the operator about the new message.
  - Record in decision 0016 that the brief was checked and this reading was chosen.
  - Alternative: an "Ask a question" switch that moves OPEN/IN_PROGRESS -> INFO_REQUESTED, which changes Diogo's transition table (D-008) and D-014.
- **Q2 Entry for "My tickets":** **a top-bar action (person icon) for citizens only, new `MyTicketsRoute` + `TopBarScreen.MY_TICKETS`**, about 15 lines in Diogo's `Destinations.kt`, `MainTopAppBar.kt` and `MainActivity.kt`, after telling Diogo. Alternative: a button on the dashboard (Francisco's file only, but less discoverable from the list).
- **Q3 Offline messages:** **queued writes, not awaited; the message shows at once from the cache; no "sending" marker; failures only logged.** Alternative: a "Sending..." label from `metadata.hasPendingWrites()` (beyond class, a few lines).
- **Q4 Author label:** **add `byOperator: Boolean` to the message document and to `model/Message` (Diogo's D-001 file, default false)** to show "Municipality" vs "Citizen". Alternative: "You" / "Other participant" only (no schema or model change).
- **Q5 Participant check:** **make `isParticipant` public in `model/TicketLifecycle.kt` (delete one `private`, Diogo's D-008 file)**, so the lifecycle, the audience and "My tickets" share one rule. Alternative: an inline `creatorId == id || id in followerIds` in `detailAudience` and the filter (no Diogo change, two copies).
- **Q6 Close-on-read trigger:** **only when a participant (or the operator stand-in) opens the detail.** "My tickets" shows CLOSED from `effectiveStatus` without writing. D-013's panel and F-016's worker add their own triggers. Alternative: also write from "My tickets" for every expired ticket (writes from a list).
- **Q7 `resolvedAt`:** **not mapped; the window stays on the RESOLVED history entry (device clock); the reopen does not clear it.** Amend 0010/0013 so they no longer say clients compute the window from `resolvedAt`. Alternative: map the Firestore `Timestamp` into the window maths (needs a model field or a parameter in Diogo's rules).
- **Q8 Exact location for participants:** **`TicketMap(exact = true)` for REGISTERED and OPERATOR**, as the task says ("exact location"). No coordinate text. Alternative: keep the coarse map for everyone.
- **Q9 Operator audience now:** **`detailAudience` returns OPERATOR for operators**, so the operator stand-in gets description, history, exact map and the thread now. Transitions come with D-013. Alternative: keep PUBLIC for operators until D-009/D-013.
- **Q10 History details:** **show "Closed automatically" for `system` rows and the justification under any row that has one** (REGISTERED/OPERATOR only). Alternative: "Closed automatically" only.
- **Q11 Decision number:** **0016**, if `dev/Diogo` has not used it (confirm with Diogo).
- **Q12 Concurrency:** **last-write-wins, no transaction** (offline first, decision 0010/0011). Accepted for a school project.
- Risk: four of Diogo's files change (`Destinations.kt`, `MainTopAppBar.kt`, `MainActivity.kt`, plus `Message.kt` and `TicketLifecycle.kt` if Q4/Q5 pass). Merge conflicts are likely if D-009 edits `MainActivity` in parallel. Tell him before slice 3.
- Risk: other followers read the thread too. Decision 0010 makes the thread per ticket, readable by all participants, so a citizen's message is visible to co-reporters (author ids are never shown).
- Risk: the day-4 / day-6 device checks need a hand-made RESOLVED entry (no operator UI before D-013). Editing by hand makes `stats/summary` drift unless it is adjusted too (steps in the coding plan).

## Out of scope
- Notifications for new messages and status changes, and the 24 h warning (F-016).
- Operator transitions, resolution proof, and labelling attachments by `phase` (D-013).
- Login, PENDING users and sign-out (D-009). `currentUser` stays a stand-in.
- Unread counters, editing/deleting messages, pagination, attachments in messages.
- Deploying or changing Firestore rules (decision 0012 Q5).
- A loading state on cold start.

## Slices
Each one builds, passes lint (0 errors) and becomes one commit. Strings are added in the slice that first uses them (lint `UnusedResources`).
1. **Messages in the data layer:** `MESSAGES`, `messageDocument`, `toMessage`, `Message.byOperator` (Q4), `messagesFor`, `sendMessage`.
2. **Status batch:** `toStatusFields`, `commitStatusChange`, `reopenTicket`, `closeExpiredTicket`.
3. **ShowScreen for participants:** `now` + effective badge, history labels, exact map, `participantContent` slot. Also `detailAudience` + public `isParticipant` (Q5), and the `MainActivity` detail call using `detailAudience(app.currentUser, ticket)` and `now`.
4. **`MessageThread` component** (stateless, previews).
5. **Detail ViewModel:** `TicketDetailUiState`, `TicketDetailViewModel`, `TicketDetailViewModelFactory` (not wired yet).
6. **Detail screen wiring:** `TicketDetailScreen` / `TicketDetailContent` / `ReopenDialog`, close-on-read effect, the `MainActivity` branch PUBLIC vs participant.
7. **"My tickets":** `MyTicketsScreen`, `TicketCard.status`, `MyTicketsRoute`, `TopBarScreen.MY_TICKETS`, top-bar action, `MainActivity` route.

# F-014 coding plan

Assumes the Q defaults in `00-plan.md`; if Gate 1 changes one, adapt and log it in `state.md`. Paths under `app/src/main/java/pt/isec/amov/cityfix/`.

## Decisions
- User: only `session.ticketsUser` in `CityFixApp` (the `TicketsViewModel.setUser` value), passed down; `null` (anonymous, pending, loading, setup, account problem) -> PUBLIC (0015 Q2, D-008 Q11). Role changes clear the stack (D-009), so the detail ViewModel never outlives its user. No screen or ViewModel reads the session or Auth. `RequireAccess`/`menuItemsFor` reused.
- Messages: oldest first, client sort (F-011, no index); `author` param because the repository has no user; `Ticket.messages` unused (0010).
- Every status batch: ticket, `publicTickets` mirror, `ticketLocations.active` (0014), `statsIncrements` (0013). Reopen: active +1, resolved -1, repair minus the earlier repair; close: zeros. `transition` already rejects blank, non-participant, pending, expired. No transaction (fails offline).
- Badges: `effectiveStatus(now)` (0011), My tickets too (any scope or status). "Closed automatically" = `SYSTEM_RESPONSIBLE_ID` (0011 N1).
- `detailAudience` takes no `Session` (`ShowScreen` stays `model/`-only); projected public tickets have a blank creator -> PUBLIC.
- Close-on-read writes once (the listener returns CLOSED). Anonymous/pending never create the ViewModel; no `RequireAccess` on the detail (anonymous users browse it).
- No author lookup (`UsersRepository` reads only the own profile). My tickets: no new listener. No rules deploy or dependency; message writes and `byOperator` amend 0010.
- Rejected: `ticket(id)` listener (second source of truth); `myTickets` flow in Diogo's `TicketsViewModel`; session in `ShowScreen` (0007/0015); `LazyColumn` thread pane (crashes inside `verticalScroll`); question -> INFO_REQUESTED; awaiting `commit()` (never completes offline); top-bar icon (Q2).
- Q1 reasons: INFO_REQUESTED means "waiting for the citizen"; leaving IN_PROGRESS turns off D-014's geofence; F-016 notifies; I.3.E makes "pedido de dados" an operator action; record the brief check. Alt: "Ask a question" switch (changes D-008, D-014).
- Q notes (alt after "/"): Q3 cache shown, failures logged / `hasPendingWrites` label. Q4 default false / "You"/"Other participant". Q5 in `TicketLifecycle.kt` / inline copies. Q6 operators once D-013, list only shows CLOSED / write from the list. Q7 window from the RESOLVED entry, closes overview "Open: lifecycle fields" point 1 (agree with Diogo), amend 0010/0013 / map the Timestamp. Q8 no coordinate text / coarse for all. Q9 unreachable until D-013 / PUBLIC until then. Q10 REGISTERED/OPERATOR only / label only. Q13 `canExpandCards` false (D-009 Q7) / toggle. Q14 credentials out of git / P ACTIVE temporarily. Q15 0015 = F-013 media + D-009 session, D-010 has 0016 / Diogo renames.
- Out of scope: notifications, 24 h warning (F-016); operator transitions, proof, attachment `phase`, operator route (D-013); login, registration; unread counters, editing, pagination, attachments; rules deploy (0012 Q5); cold-start loading.

## Failure modes
- Missing document (no `ticketLocations`): whole batch fails, logged, cache rolls back.
- Cold start: "Ticket not found" until the first snapshot.
- `messages` listener error stops it; the last list stays.
- Log out on the detail: stack cleared, listener removed; one frame of PUBLIC at most.
- Device clock decides the window and `sentAt` order.
- Two offline closes add two CLOSED entries; reopen vs operator is last-write-wins (Q12).
- Test mode accepts a late offline reopen and messages to closed tickets (0012 Q5).
- Vertical drags on the exact map may move the map.

## Files
`model/Message.kt`, `model/TicketLifecycle.kt`, `MainActivity.kt` (Diogo's); `data/FirestoreMapping.kt`, `data/TicketsRepository.kt`, `FirestoreTicketsRepository.kt`, `ui/screens/ShowScreen.kt`, `ui/components/TicketCard.kt`; KDoc only: `ui/navigation/Destinations.kt`, `ui/screens/PlaceholderScreen.kt` (Diogo's). New: `ui/components/MessageThread.kt`, `viewmodel/TicketDetailUiState.kt`, `TicketDetailViewModel.kt`, `TicketDetailViewModelFactory.kt`, `ui/screens/TicketDetailScreen.kt`, `MyTicketsScreen.kt`. Unchanged: `MainTopAppBar.kt`, `RoleAccess.kt`, `toTopBarScreen()`, `top_bar_title_my_tickets`, `menu_my_tickets`.

Strings (`values{,-pt}`, EN + PT by the implementer): slice 3 `ticket_map_exact_title`, `cd_ticket_map_exact`, `ticket_history_system`; 4 `messages_title`, `messages_empty`, `message_author_you`, `message_author_operator`, `message_author_citizen`, `message_input_label`, `cd_send_message`, `messages_locked`; 5 `reopen_error_blank`, `reopen_error_not_allowed`; 6 `reopen_button`, `reopen_dialog_title`, `reopen_dialog_text`, `reopen_justification_label`, `reopen_confirm`, `reopen_cancel`; 7 `my_tickets_empty`.

## Interfaces
```kotlin
// model
data class Message(val id: String = "", val text: String = "", val sentAt: Long = 0L, val authorId: String = "", val byOperator: Boolean = false)
fun Ticket.isParticipant(userId: String): Boolean = userId == creatorId || userId in followerIds

// data/FirestoreMapping.kt
internal const val MESSAGES = "messages"
internal fun messageDocument(text: String, sentAt: Long, author: User): Map<String, Any> =
    hashMapOf("text" to text, "sentAt" to sentAt, "authorId" to author.id, "byOperator" to (author is Operator))
internal fun DocumentSnapshot.toMessage(): Message   // getString/getLong/getBoolean with defaults
internal fun Ticket.toStatusFields(): Map<String, Any> =
    hashMapOf("currentStatus" to currentStatus.name, "statusHistory" to statusHistory.map { it.toDocument() })

// data/TicketsRepository.kt
fun messagesFor(ticketId: String): Flow<List<Message>>
fun sendMessage(ticketId: String, text: String, author: User): Boolean
fun reopenTicket(ticket: Ticket, by: User, justification: String): Boolean
fun closeExpiredTicket(ticket: Ticket)

// ui
fun detailAudience(user: User?, ticket: Ticket?): DetailAudience
@Composable fun ShowScreen(ticket: Ticket?, audience: DetailAudience, now: Long, modifier: Modifier = Modifier,
    participantContent: @Composable ColumnScope.() -> Unit = {})
@Composable fun MessageThread(messages: List<Message>, currentUserId: String, draft: String, canSend: Boolean,
    onDraftChange: (String) -> Unit, onSendClick: () -> Unit, modifier: Modifier = Modifier)
@Composable fun TicketDetailScreen(ticket: Ticket?, user: User, viewModel: TicketDetailViewModel, modifier: Modifier = Modifier)
@Composable fun TicketDetailContent(ticket: Ticket?, user: User, now: Long, state: TicketDetailUiState,
    onDraftChange: (String) -> Unit, onSendClick: () -> Unit, onReopenClick: () -> Unit,
    onJustificationChange: (String) -> Unit, onReopenConfirm: () -> Unit, onReopenDismiss: () -> Unit,
    modifier: Modifier = Modifier)
@Composable fun MyTicketsScreen(tickets: List<Ticket>, now: Long, onTicketClick: (String) -> Unit, modifier: Modifier = Modifier)
@Composable fun TicketCard(ticket: Ticket, isExpanded: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier,
    status: TicketStatus = ticket.currentStatus)

// viewmodel
data class TicketDetailUiState(val messages: List<Message> = emptyList(), val draft: String = "",
    val isReopenDialogOpen: Boolean = false, val justification: String = "", @StringRes val reopenError: Int? = null)
class TicketDetailViewModel(repository: TicketsRepository, ticketId: String, user: User) : ViewModel() {
    val uiState: StateFlow<TicketDetailUiState>
    fun onDraftChange(text: String); fun sendMessage(); fun onReopenClick(); fun onJustificationChange(text: String)
    fun onReopenDismiss(); fun confirmReopen(ticket: Ticket); fun closeIfExpired(ticket: Ticket)
}
// Used as is (D-009): val Session.ticketsUser: User?
```

## Steps
Each slice: build + lint, then commit.

1. **Messages data layer**: `Message.byOperator` last + KDoc (tell Diogo); `MESSAGES`, `messageDocument`, `toMessage`; `toTicketDocument()` KDoc: `messages` written by `sendMessage`. `messagesFor` = `listen` on `tickets/id/messages`, `sortedBy(sentAt)`. `sendMessage`: trim, false if empty, `document().set(...).addOnFailureListener { Log.w }` not awaited, true. Class KDoc: live, queued offline, never read when anonymous/pending. `feat(F-014): read and send ticket messages in Firestore`.
2. **Status batch**: `toStatusFields()` (private `StatusHistory.toDocument()`). `reopenTicket`: `transition(OPEN, by, now, justification) ?: return false`, then `commitStatusChange`. `closeExpiredTicket`: `closeIfExpired(now) ?: return` (CLOSED by `"system"` at `resolvedAt + 5 d`). Private `commitStatusChange(before, after)`: one `batch()` with `update` ticket status fields, `publicTickets` `currentStatus` if PUBLIC, `ticketLocations` `active = after.isActive`, `set(STATS_SUMMARY, statsIncrements, merge())`; `commit().addOnFailureListener`. KDoc: `resolvedAt` untouched (Q7), whole history rewritten (Q12), D-013 reuses it. `feat(F-014): add the reopen and close-on-read batches`.
3. **ShowScreen for participants**: public `isParticipant` + KDoc "also used by the detail audience and My tickets" (tell Diogo). `detailAudience`: exhaustive `when`, `null` -> PUBLIC, `Citizen` -> REGISTERED if participant else PUBLIC, `Operator` -> OPERATOR. `now` + slot to `DetailsPane`/`LocationPane`; `TicketHeader` badge `effectiveStatus(now)`. REGISTERED/OPERATOR: private `ExactLocationSection` (heading, `TicketMap(exact = true)` 200 dp, cd, `ticket_map_unavailable` for (0,0)), divider, `StatusHistorySection`, slot; PUBLIC keeps `CoarseLocationMap`; landscape: thread under history in the scrolling pane. `HistoryRow` -> `Column` (`mergeDescendants`): row, `bodySmall` "Closed automatically", justification. KDoc: never author/follower/uploader ids. Previews: `now = PREVIEW_CREATED + 3 * DAY_MILLIS`, REGISTERED with a `system` row + justification. `MainActivity`: `ShowScreen(ticket, detailAudience(session.ticketsUser, ticket), System.currentTimeMillis())`; 0007 comment replaces "public fields for every role"; reword the Q7 `canExpandCards` comment (public-only cards, own view F-014, operator D-013; stays false). Verify PUBLIC/privacy previews, D1, D3. `feat(F-014): show the participant detail for own and followed tickets`.
4. **MessageThread**: `Column(spacedBy 8.dp)`: heading (`semantics { heading() }`); empty text or `forEach` private `MessageRow` (`isMine = authorId == currentUserId`); if `canSend`, `OutlinedTextField(weight 1f)` + 48 dp `IconButton(enabled = draft.isNotBlank())` with `AutoMirrored.Filled.Send`, else `messages_locked`. `MessageRow`: `Surface` (`shapes.medium`, `primaryContainer` mine / `surfaceVariant`), end/start-aligned, max 85%; label, text, `formatDateTime(sentAt, locale)` (skip 0L); `mergeDescendants`. Previews: mine/operator/citizen + draft, empty, locked, PT, landscape. `feat(F-014): add the message thread component`.
5. **Detail ViewModel**: `init` collects `messagesFor`; `update { copy }` functions; `sendMessage` clears the draft on true; `onReopenClick` resets text and error; `confirmReopen`: blank -> `reopen_error_blank`, false -> `reopen_error_not_allowed`, else close; `closeIfExpired` -> `closeExpiredTicket`. Factory copies `ReportViewModelFactory` (`@Suppress("UNCHECKED_CAST")`). KDoc: per destination (`EditContactViewModel`), REGISTERED/OPERATOR only, fixed user, ticket not stored. `feat(F-014): add the ticket detail ViewModel`.
6. **Detail wiring**: `TicketDetailScreen`: `collectAsStateWithLifecycle`, `now`, `LaunchedEffect(ticket) { ticket?.let(viewModel::closeIfExpired) }` (Q6, 0012 Q4), then `TicketDetailContent` (`onReopenConfirm = { ticket?.let(viewModel::confirmReopen) }`). Content: `ShowScreen` slot with Reopen `Button` if `OPEN in allowedTransitions(user, now)` and `MessageThread(canSend = !ticket.isLocked(now))`; private `ReopenDialog` (`AlertDialog`, `OutlinedTextField(isError, supportingText)`, Confirm/Cancel). Previews (0011 Q10): creator, RESOLVED at `t0`; `t0 + 4 d`, `t0 + 6 d`, blank error, PT, landscape. `MainActivity`: `null` user or PUBLIC -> `ShowScreen`; else `viewModel(factory = TicketDetailViewModelFactory(app.ticketsRepository, id, user))` + `TicketDetailScreen`; comment: entry-scoped. Verify D3-D10, D12, D13, D15. `feat(F-014): add the message thread and reopen to the ticket detail`.
7. **My tickets**: `TicketCard.status` to `StatusBadge` (KDoc: pass `effectiveStatus(now)`). `MyTicketsScreen` like `ListScreen`: `LazyColumn` (16 dp padding, 8 dp spacing), `items(key = id)`, collapsed cards, centred empty text; previews OPEN, day 4, day 6, empty, PT, landscape. `MainActivity` in `RequireAccess(CITIZEN)`: `session.ticketsUser?.id ?: return@RequireAccess`, `tickets.filter { it.isParticipant(id) }`, tap -> `TicketDetailRoute` (`launchSingleTop`); leave the "Placeholders" group. Diogo's KDoc: `MyTicketsRoute` "Tickets the signed-in active citizen created or follows (F-014)."; `PlaceholderScreen.kt` drops "My tickets F-014". Verify `placeholder_message` still used, D1, D2, D11, D12, D14. `feat(F-014): add the My tickets screen`.

## Device checks
The person does the setup; Claude touches neither the console nor `google-services.json`.
1. Google Play AVD; `adb emu geo fix -8.4190 40.2100`.
2. `./gradlew signingReport`; add Francisco's SHA-1/SHA-256 in Firebase.
3. Accounts (Q14), credentials and UIDs out of git: **A** (active), **P** (pending), **O** (operator) from Diogo (logins, MFA test numbers). **C** (second active): Identity Platform test number `+351 9xx...` + 6-digit code; Auth user (real e-mail); `users/<uid>` with `role` CITIZEN, `registrationStatus` ACTIVE (these decide the session), `name`, `nif` (9 unique digits), `email`, `phone`, `createdAt`, `georeferencing` 40.21,-8.419, `impactDetection` false, `fcmTokens` []; first login verifies e-mail and MFA. Or after D1b use P (ACTIVE, then back).
4. Data: T1 = A's PUBLIC "Urban furniture" with photo; C checks My tickets (D2) then reports the same category + situation at the same fix (joins T1). T2 by A at `-8.41925 40.2100`, T3 at `-8.41950 40.2100`. `sample-3`: `creatorId`, `participantIds` = A.
5. REST: `https://firestore.googleapis.com/v1/projects/<project_id>/databases/(default)/documents`, id never in docs; no token in test mode (until 2026-12-10).

| # | Check | Setup | Expected |
| --- | --- | --- | --- |
| D1 | Logged out | Dashboard, list, T1 | No menu; PUBLIC |
| D1b | Pending | P: menu, browse -> T1 | Profile, Log out only; PUBLIC, no thread |
| D2 | My tickets | A; C before joining | A: T1-T3, sample-3 CLOSED, no sample-1/2, title. C: empty |
| D3 | Participant vs other | A: T1, sample-1 | T1: description, exact map, history, thread, no Reopen; sample-1 PUBLIC |
| D4 | Close-on-read | A: sample-3 | CLOSED, locked, label. REST: by `system` at RESOLVED + 432000000; `active` false; mirror CLOSED; stats same; reopening adds nothing |
| D5 | Send | A on T1 | "You" at once, draft cleared; REST fields, `byOperator` false |
| D6 | Live | C on emulator 2 | "Citizen", live both ways. REST POST to `tickets/<T1>/messages`: O's message (`byOperator` true) shows "Municipality"; no second emulator: POST C's |
| D7 | Offline | A airplane mode | Shown, not in REST; after reconnect in REST and on C |
| D8 | Day 4 | T2: note stats; hand-set RESOLVED `now - 4 d` by O, `active` false, mirror, stats (active -1, resolved +1, repair + (RESOLVED - createdAt)) | Reopen; blank -> error; "Still broken" -> OPEN. REST: entry with justification by A; mirror OPEN; `active` true; stats as noted |
| D9 | Day 6 | T3, `now - 6 d` | No Reopen; CLOSED as D4; stats same |
| D10 | Rotation | D8 dialog, type, rotate | Dialog, text, draft survive |
| D11 | Follower | C | T1 listed; thread, can send, no Reopen |
| D12 | Landscape | Detail, My tickets | Two panes; thread under history; input above keyboard; no clipping |
| D13 | PT | Device PT | New strings PT; title, menu from D-009 |
| D14 | Build | Each slice | 0 lint errors; warnings stay 23 |
| D15 | Log out on detail | A on T1 | Anonymous dashboard, thread never shown (record); Back exits; T1 PUBLIC |

Clean-up (optional): data can stay; restore sample-3 if needed; P back to PENDING.

## Conventions
- Stateful/stateless, previews on stateless, no hard-coded strings, 48 dp, content descriptions, both orientations.
- Class patterns: factory in the destination (Contacts_#3, P08), `hashMapOf` + typed getters + `batch()` (T8, slide 09), `AlertDialog`, `LaunchedEffect`, `@Serializable` routes. KDoc "beyond class" on `listen`, `FieldValue.increment`, the slot.
- No try/catch, extra validation or transactions; status only via `transition`/`closeIfExpired` (0011). Commits with F-014, no attribution.

## Do not touch
Rest of `TicketLifecycle.kt`; `TicketPrivacy.kt`, `TicketsList.kt`, `TicketsViewModel(+Factory)`; D-009's `Session.kt`, `SessionViewModel(+Factory)`, auth/users repositories, `RoleAccess.kt`, `MainTopAppBar.kt` and the session, `logOut`, gate, guards, redirect in `MainActivity.kt`; rules, `google-services.json`, Maps key, version catalog, `DebugSeed.kt`, `SampleTickets.kt`, `ReportScreen`, `ReportViewModel`, upload code.

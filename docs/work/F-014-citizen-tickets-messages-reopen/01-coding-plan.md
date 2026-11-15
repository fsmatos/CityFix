# F-014 coding plan

Assumes the defaults of the Q list in `00-plan.md`. If Gate 1 changes one, adapt the step it touches and record the change in `state.md`. Paths are under `app/src/main/java/pt/isec/amov/cityfix/` unless they start with `app/` or `docs/`.

## Files
| Path | Change | Why |
| --- | --- | --- |
| `model/Message.kt` (Diogo, D-001) | `val byOperator: Boolean = false` (last, with KDoc) | Q4 author label |
| `model/TicketLifecycle.kt` (Diogo, D-008) | `isParticipant` loses `private` (KDoc: "also used by the detail audience and My tickets") | Q5, one participant rule |
| `data/FirestoreMapping.kt` | `MESSAGES`; `messageDocument(text, sentAt, author)`; `DocumentSnapshot.toMessage()`; `Ticket.toStatusFields()` | decision 0010 mapping |
| `data/TicketsRepository.kt` | `messagesFor`, `sendMessage`, `reopenTicket`, `closeExpiredTicket` with KDoc | I.2.E, II.4, II.5 |
| `data/FirestoreTicketsRepository.kt` | the 4 members + private `commitStatusChange(before, after)`; class KDoc | same |
| `ui/screens/ShowScreen.kt` | `now` param, effective badge, `participantContent` slot, exact map for REGISTERED/OPERATOR, history labels, `detailAudience(user, ticket)`; previews | decisions 0007, 0011 |
| `ui/components/MessageThread.kt` (new) | stateless thread + previews | I.2.E, generic for D-013 |
| `viewmodel/TicketDetailUiState.kt` (new) | UI state data class | MVVM (as `ReportUiState`) |
| `viewmodel/TicketDetailViewModel.kt` (new) | messages, draft, reopen dialog, close-on-read | Contacts `EditContactViewModel` pattern |
| `viewmodel/TicketDetailViewModelFactory.kt` (new) | factory | Contacts `EditContactViewModelFactory` |
| `ui/screens/TicketDetailScreen.kt` (new) | stateful `TicketDetailScreen`, stateless `TicketDetailContent`, private `ReopenDialog`; previews day 4 / day 6 | stateful/stateless split |
| `ui/screens/MyTicketsScreen.kt` (new) | stateless list + empty state + previews | I.2.E list |
| `ui/components/TicketCard.kt` | `status: TicketStatus = ticket.currentStatus` param, badge uses it | effective status in My tickets |
| `ui/navigation/Destinations.kt` (Diogo, D-004) | `MyTicketsRoute` | Q2 |
| `ui/navigation/MainTopAppBar.kt` (Diogo, D-005) | `TopBarScreen.MY_TICKETS`, title, `showMyTickets` + `onMyTicketsClick`, preview | Q2 |
| `MainActivity.kt` (Diogo, D-006) | detail destination branch, My tickets route, top-bar params, `toTopBarScreen` | wiring |
| `app/src/main/res/values/strings.xml`, `values-pt/strings.xml` | strings below | brief V (EN default + PT) |

Strings (EN / PT), each added in the slice that first uses it:
- slice 3:
  - `ticket_map_exact_title` "Location" / "Localização";
  - `cd_ticket_map_exact` "Map of the exact location of the issue" / "Mapa da localização exata da ocorrência";
  - `ticket_history_system` "Closed automatically" / "Encerrado automaticamente".
- slice 4:
  - `messages_title` "Messages" / "Mensagens";
  - `messages_empty` "No messages yet." / "Ainda não há mensagens.";
  - `message_author_you` "You" / "Você";
  - `message_author_operator` "Municipality" / "Município";
  - `message_author_citizen` "Citizen" / "Munícipe";
  - `message_input_label` "Write a message" / "Escreva uma mensagem";
  - `cd_send_message` "Send message" / "Enviar mensagem";
  - `messages_locked` "This ticket is closed. No more messages can be sent." / "Esta ocorrência está encerrada. Não é possível enviar mais mensagens.".
- slice 5 (used by the ViewModel):
  - `reopen_error_blank` "Write a justification." / "Escreva uma justificação.";
  - `reopen_error_not_allowed` "This ticket can no longer be reopened." / "Esta ocorrência já não pode ser reaberta.".
- slice 6:
  - `reopen_button` "Reopen" / "Reabrir";
  - `reopen_dialog_title` "Reopen this ticket?" / "Reabrir esta ocorrência?";
  - `reopen_dialog_text` "Explain why the problem is not solved." / "Explique porque é que o problema não está resolvido.";
  - `reopen_justification_label` "Justification" / "Justificação";
  - `reopen_confirm` "Reopen" / "Reabrir";
  - `reopen_cancel` "Cancel" / "Cancelar".
- slice 7:
  - `top_bar_title_my_tickets` "My tickets" / "As minhas ocorrências";
  - `cd_my_tickets` "My tickets" / "As minhas ocorrências";
  - `my_tickets_empty` "You have not reported or joined any issue yet." / "Ainda não reportou nem acompanha nenhuma ocorrência.".

## Interfaces
```kotlin
// model/Message.kt (Diogo; Q4)
data class Message(
    val id: String = "", val text: String = "", val sentAt: Long = 0L, val authorId: String = "",
    /** True when an operator wrote it (the thread labels it "Municipality"); stored in Firestore as `byOperator`. */
    val byOperator: Boolean = false,
)

// model/TicketLifecycle.kt (Diogo; Q5): only the visibility changes
fun Ticket.isParticipant(userId: String): Boolean = userId == creatorId || userId in followerIds

// data/FirestoreMapping.kt
internal const val MESSAGES = "messages"
/** A `tickets/{id}/messages/{id}` document (decision 0010 + byOperator). */
internal fun messageDocument(text: String, sentAt: Long, author: User): Map<String, Any> =
    hashMapOf("text" to text, "sentAt" to sentAt, "authorId" to author.id, "byOperator" to (author is Operator))
internal fun DocumentSnapshot.toMessage(): Message = Message(
    id = id, text = getString("text") ?: "", sentAt = getLong("sentAt") ?: 0L,
    authorId = getString("authorId") ?: "", byOperator = getBoolean("byOperator") ?: false,
)
/** The two fields a status change writes on `tickets/{id}` (decision 0004: always together). */
internal fun Ticket.toStatusFields(): Map<String, Any> = hashMapOf(
    "currentStatus" to currentStatus.name,
    "statusHistory" to statusHistory.map { it.toDocument() },
)

// data/TicketsRepository.kt (additions)
/** Messages of ticket [ticketId], oldest first (tickets/{id}/messages). Participants and operators only. */
fun messagesFor(ticketId: String): Flow<List<Message>>
/** Adds a message by [author] (text trimmed); false when blank. Queued offline; returns at once. */
fun sendMessage(ticketId: String, text: String, author: User): Boolean
/** Reopens [ticket] (brief II.4) through D-008's transition; false when the rules refuse it. One batch. */
fun reopenTicket(ticket: Ticket, by: User, justification: String): Boolean
/** Writes D-008's closeIfExpired result (decision 0012 Q4); does nothing when the ticket has not expired. */
fun closeExpiredTicket(ticket: Ticket)

// ui/screens/ShowScreen.kt
fun detailAudience(user: User?, ticket: Ticket?): DetailAudience
@Composable
fun ShowScreen(
    ticket: Ticket?,
    audience: DetailAudience,
    now: Long,
    modifier: Modifier = Modifier,
    participantContent: @Composable ColumnScope.() -> Unit = {},
)

// ui/components/MessageThread.kt
@Composable
fun MessageThread(
    messages: List<Message>,
    currentUserId: String,
    draft: String,
    canSend: Boolean,
    onDraftChange: (String) -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier,
)

// viewmodel/TicketDetailUiState.kt
data class TicketDetailUiState(
    val messages: List<Message> = emptyList(),
    val draft: String = "",
    val isReopenDialogOpen: Boolean = false,
    val justification: String = "",
    @StringRes val reopenError: Int? = null,
)

// viewmodel/TicketDetailViewModel.kt
class TicketDetailViewModel(repository: TicketsRepository, ticketId: String, user: User) : ViewModel() {
    val uiState: StateFlow<TicketDetailUiState>
    fun onDraftChange(text: String)
    fun sendMessage()
    fun onReopenClick()
    fun onJustificationChange(text: String)
    fun onReopenDismiss()
    fun confirmReopen(ticket: Ticket)
    fun closeIfExpired(ticket: Ticket)
}

// ui/screens/TicketDetailScreen.kt
@Composable fun TicketDetailScreen(ticket: Ticket?, user: User, viewModel: TicketDetailViewModel, modifier: Modifier = Modifier)
@Composable fun TicketDetailContent(
    ticket: Ticket?, user: User, now: Long, state: TicketDetailUiState,
    onDraftChange: (String) -> Unit, onSendClick: () -> Unit,
    onReopenClick: () -> Unit, onJustificationChange: (String) -> Unit,
    onReopenConfirm: () -> Unit, onReopenDismiss: () -> Unit,
    modifier: Modifier = Modifier,
)

// ui/screens/MyTicketsScreen.kt
@Composable fun MyTicketsScreen(tickets: List<Ticket>, now: Long, onTicketClick: (String) -> Unit, modifier: Modifier = Modifier)

// ui/components/TicketCard.kt
@Composable fun TicketCard(ticket: Ticket, isExpanded: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, status: TicketStatus = ticket.currentStatus)

// ui/navigation
@Serializable data object MyTicketsRoute
enum class TopBarScreen { DASHBOARD, LIST, DETAIL, REPORT, MY_TICKETS }
fun MainTopAppBar(..., showMyTickets: Boolean, onMyTicketsClick: () -> Unit, modifier: Modifier = Modifier)
```

## Steps

### Slice 1: messages in the data layer
- Diogo's `model/Message.kt`: add `byOperator` (Q4). Tell Diogo first.
- `FirestoreMapping.kt`: `MESSAGES`, `messageDocument`, `toMessage` (as in Interfaces). Update the `toTicketDocument()` KDoc line "`messages` (subcollection, F-014)" to say it is written by `sendMessage`.
- `FirestoreTicketsRepository`:
  ```kotlin
  override fun messagesFor(ticketId: String): Flow<List<Message>> =
      firestore.collection(TICKETS).document(ticketId).collection(MESSAGES)
          .listen { snapshot -> snapshot.documents.map { it.toMessage() }.sortedBy { it.sentAt } }

  override fun sendMessage(ticketId: String, text: String, author: User): Boolean {
      val clean = text.trim()
      if (clean.isEmpty()) return false
      // document() makes the id locally and set() is queued offline, as in reportTicket.
      firestore.collection(TICKETS).document(ticketId).collection(MESSAGES).document()
          .set(messageDocument(clean, System.currentTimeMillis(), author))
          .addOnFailureListener { Log.w(TAG, "message not saved", it) }
      return true
  }
  ```
- Interface KDoc as in Interfaces. Class KDoc: one paragraph on messages (subcollection, live, queued offline, never read on the anonymous path: only the participant detail creates the ViewModel that calls it).
- Commit: `feat(F-014): read and send ticket messages in Firestore`.
- Verify: build + lint. Nothing calls the new members yet.

### Slice 2: status-change batch (reopen, close-on-read)
- `FirestoreMapping.kt`: `Ticket.toStatusFields()` (it uses the private `StatusHistory.toDocument()` in the same file).
- `FirestoreTicketsRepository`:
  ```kotlin
  override fun reopenTicket(ticket: Ticket, by: User, justification: String): Boolean {
      val after = ticket.transition(TicketStatus.OPEN, by, System.currentTimeMillis(), justification)
          ?: return false
      commitStatusChange(ticket, after)
      return true
  }

  override fun closeExpiredTicket(ticket: Ticket) {
      val after = ticket.closeIfExpired(System.currentTimeMillis()) ?: return
      commitStatusChange(ticket, after)
  }

  /** One batch for a status change (decisions 0010, 0013, 0014): ticket, public mirror, grouping flag, counters. */
  private fun commitStatusChange(before: Ticket, after: Ticket) {
      val batch = firestore.batch()
      batch.update(firestore.collection(TICKETS).document(after.id), after.toStatusFields())
      if (after.scope == Scope.PUBLIC) {
          batch.update(firestore.collection(PUBLIC_TICKETS).document(after.id), "currentStatus", after.currentStatus.name)
      }
      batch.update(firestore.collection(TICKET_LOCATIONS).document(after.id), "active", after.isActive)
      batch.set(firestore.document(STATS_SUMMARY), statsIncrements(before, after), SetOptions.merge())
      // Not awaited, as reportTicket: offline it would wait for the server.
      batch.commit().addOnFailureListener { Log.w(TAG, "status change not saved", it) }
  }
  ```
- KDoc on `commitStatusChange`:
  - `resolvedAt` is neither read nor cleared (Q7);
  - the whole `statusHistory` array is rewritten from the local copy, so it is last-write-wins (Q12);
  - D-013 reuses it for operator transitions.
- Commit: `feat(F-014): add the reopen and close-on-read batches`.
- Verify: build + lint.

### Slice 3: ShowScreen for participants and the audience mapping
- Diogo's `model/TicketLifecycle.kt`: drop `private` from `isParticipant`, KDoc as in Files (Q5). Tell Diogo first.
- `ShowScreen.kt`:
  - `detailAudience` next to the enum:
    ```kotlin
    fun detailAudience(user: User?, ticket: Ticket?): DetailAudience = when (user) {
        null -> DetailAudience.PUBLIC
        is Citizen -> if (ticket != null && ticket.isParticipant(user.id)) DetailAudience.REGISTERED else DetailAudience.PUBLIC
        is Operator -> DetailAudience.OPERATOR
    }
    ```
  - `ShowScreen(ticket, audience, now, modifier, participantContent)`. Pass `now` and the slot down to `DetailsPane` / `LocationPane`.
  - `TicketHeader(ticket, now)` -> `StatusBadge(ticket.effectiveStatus(now))`.
  - `LocationPane`, in the `when (audience)`:
    - PUBLIC -> `CoarseLocationMap(ticket.location)` (unchanged);
    - REGISTERED, OPERATOR -> a private `ExactLocationSection(ticket.location)` (heading `ticket_map_exact_title`, `TicketMap(exact = true)` 200 dp high, `cd_ticket_map_exact`, `ticket_map_unavailable` for (0, 0)), `HorizontalDivider()`, `StatusHistorySection`, then `participantContent()`.
    - Landscape: the right pane keeps its own `verticalScroll`, so the thread sits under the history there. Portrait: one column.
  - `HistoryRow`: after the date, `if (entry.responsibleId == SYSTEM_RESPONSIBLE_ID)` a `bodySmall` "Closed automatically"; `entry.justification?.let { Text(it) }` under the row (Q10). The row becomes a `Column` holding the existing `Row` plus these lines, still `mergeDescendants`.
  - KDoc: REGISTERED/OPERATOR also get the exact map, justifications and the participant slot; still never author, follower or uploader ids; `now` decides the effective status (decision 0011).
  - Previews: pass a fixed `now` (`PREVIEW_CREATED + 3 * DAY_MILLIS`). Add a REGISTERED preview with a `system` CLOSED row and a reopen justification.
- `MainActivity`, `composable<TicketDetailRoute>`:
  ```kotlin
  val ticket = tickets.firstOrNull { it.id == id }
  ShowScreen(
      ticket = ticket,
      audience = detailAudience(app.currentUser, ticket),
      now = System.currentTimeMillis(),
  )
  ```
  Replace the "No login yet: shown as anonymous" comment with "the audience comes from the stand-in user and the creator/follower relation (decision 0007)".
- Commit: `feat(F-014): show the participant detail for own and followed tickets`.
- Verify: build + lint. Previews: PUBLIC unchanged (privacy preview included); REGISTERED shows the exact-map placeholder, the history labels and justifications. Device: D1, D3 (without the thread).

### Slice 4: MessageThread component
- `ui/components/MessageThread.kt`:
  - `Column(spacedBy(8.dp))`;
  - a heading `messages_title` (`semantics { heading() }`);
  - `if (messages.isEmpty())` `messages_empty` (`onSurfaceVariant`), else `messages.forEach { MessageRow(it, isMine = it.authorId == currentUserId, locale) }`;
  - then `if (canSend)` the input `Row` (`OutlinedTextField(value = draft, onValueChange = onDraftChange, label = { Text(message_input_label) }, modifier = Modifier.weight(1f))` + `IconButton(onClick = onSendClick, enabled = draft.isNotBlank())` with `Icon(Icons.AutoMirrored.Filled.Send, cd_send_message)`), else `messages_locked`.
- Private `MessageRow`:
  - a `Surface` (shape `MaterialTheme.shapes.medium`; `primaryContainer` when mine, `surfaceVariant` otherwise) in a `Box` with `Alignment.CenterEnd` / `CenterStart`, max width 85%;
  - inside: the label (`you` / `byOperator` -> operator / citizen), the text, `formatDateTime(sentAt, locale)` (skipped when 0L);
  - `semantics(mergeDescendants = true) {}`.
- KDoc: stateless and role-agnostic (D-013 passes the operator's id); a plain `Column`, because it lives inside the detail's vertical scroll; no author lookup.
- Previews:
  - three messages (mine, operator, other citizen) with `canSend = true` and a draft;
  - empty;
  - locked;
  - PT;
  - landscape.
- Commit: `feat(F-014): add the message thread component`.
- Verify: build + lint, previews.

### Slice 5: detail ViewModel
- `TicketDetailUiState.kt` as in Interfaces.
- `TicketDetailViewModel`:
  ```kotlin
  init {
      viewModelScope.launch {
          repository.messagesFor(ticketId).collect { list -> _uiState.update { it.copy(messages = list) } }
      }
  }
  fun onDraftChange(text: String) = _uiState.update { it.copy(draft = text) }
  fun sendMessage() {
      if (repository.sendMessage(ticketId, _uiState.value.draft, user)) _uiState.update { it.copy(draft = "") }
  }
  fun onReopenClick() = _uiState.update { it.copy(isReopenDialogOpen = true, justification = "", reopenError = null) }
  fun onJustificationChange(text: String) = _uiState.update { it.copy(justification = text, reopenError = null) }
  fun onReopenDismiss() = _uiState.update { it.copy(isReopenDialogOpen = false) }
  fun confirmReopen(ticket: Ticket) {
      val text = _uiState.value.justification
      when {
          text.isBlank() -> _uiState.update { it.copy(reopenError = R.string.reopen_error_blank) }
          !repository.reopenTicket(ticket, user, text) -> _uiState.update { it.copy(reopenError = R.string.reopen_error_not_allowed) }
          else -> _uiState.update { it.copy(isReopenDialogOpen = false, justification = "") }
      }
  }
  fun closeIfExpired(ticket: Ticket) = repository.closeExpiredTicket(ticket)
  ```
  The two error strings are referenced here, so add them in this slice (lint `UnusedResources` sees the `R.string` use).
- `TicketDetailViewModelFactory(repository, ticketId, user)` as `ReportViewModelFactory` (same `@Suppress("UNCHECKED_CAST")` and comment).
- KDoc:
  - per destination, as `EditContactViewModel`;
  - created only for REGISTERED/OPERATOR, so anonymous users never listen to `messages`;
  - the ticket is not stored here: it comes live from `TicketsViewModel.tickets`.
- Commit: `feat(F-014): add the ticket detail ViewModel`.
- Verify: build + lint.

### Slice 6: detail screen wiring (thread, reopen, close-on-read)
- `TicketDetailScreen.kt`:
  ```kotlin
  @Composable
  fun TicketDetailScreen(ticket: Ticket?, user: User, viewModel: TicketDetailViewModel, modifier: Modifier = Modifier) {
      val state by viewModel.uiState.collectAsStateWithLifecycle()
      val now = System.currentTimeMillis()
      // Decision 0012 Q4: the first participant who sees an expired RESOLVED ticket writes CLOSED.
      LaunchedEffect(ticket) { ticket?.let(viewModel::closeIfExpired) }
      TicketDetailContent(ticket, user, now, state,
          onDraftChange = viewModel::onDraftChange, onSendClick = viewModel::sendMessage,
          onReopenClick = viewModel::onReopenClick, onJustificationChange = viewModel::onJustificationChange,
          onReopenConfirm = { ticket?.let(viewModel::confirmReopen) }, onReopenDismiss = viewModel::onReopenDismiss,
          modifier = modifier)
  }
  ```
- `TicketDetailContent`:
  ```kotlin
  ShowScreen(ticket, detailAudience(user, ticket), now, modifier) {
      if (ticket != null) {
          if (TicketStatus.OPEN in ticket.allowedTransitions(user, now)) {
              Button(onClick = onReopenClick) { Text(stringResource(R.string.reopen_button)) }
          }
          MessageThread(state.messages, user.id, state.draft, canSend = !ticket.isLocked(now), onDraftChange, onSendClick)
      }
  }
  if (state.isReopenDialogOpen) ReopenDialog(state.justification, state.reopenError, onJustificationChange, onReopenConfirm, onReopenDismiss)
  ```
- Private `ReopenDialog`: `AlertDialog`
  - title `reopen_dialog_title`;
  - text: a `Column` with `reopen_dialog_text` and `OutlinedTextField(label = reopen_justification_label, isError = error != null, supportingText = error?.let { { Text(stringResource(it)) } })`;
  - `confirmButton = TextButton(onConfirm) { reopen_confirm }`, `dismissButton = TextButton(onDismiss) { reopen_cancel }`, `onDismissRequest = onDismiss`.
- Previews of `TicketDetailContent` (Citizen `"citizen-1"` ACTIVE, ticket RESOLVED at fixed `t0` by `"operator-1"`, creator `"citizen-1"`):
  - `now = t0 + 4 d` -> Reopen shown, thread with input;
  - `now = t0 + 6 d` -> CLOSED badge, no Reopen, locked note;
  - dialog open with the blank error;
  - PT;
  - landscape.
- `MainActivity`, `composable<TicketDetailRoute>`:
  ```kotlin
  val ticket = tickets.firstOrNull { it.id == id }
  val user = app.currentUser
  val audience = detailAudience(user, ticket)
  if (user == null || audience == DetailAudience.PUBLIC) {
      ShowScreen(ticket = ticket, audience = audience, now = System.currentTimeMillis())
  } else {
      // Inside the destination: scoped to this entry, like the report form's ViewModel.
      val detailViewModel: TicketDetailViewModel =
          viewModel(factory = TicketDetailViewModelFactory(app.ticketsRepository, id, user))
      TicketDetailScreen(ticket = ticket, user = user, viewModel = detailViewModel)
  }
  ```
- Commit: `feat(F-014): add the message thread and reopen to the ticket detail`.
- Verify: build + lint, previews, device D3-D10, D12, D13.

### Slice 7: "My tickets" screen and top-bar entry
- `TicketCard`: `status` param last, `StatusBadge(status)`. KDoc: "callers pass `effectiveStatus(now)` for full tickets".
- `MyTicketsScreen`:
  - the same layout as `ListScreen`: `LazyColumn(contentPadding = 16.dp, spacedBy(8.dp))`, `items(tickets, key = { it.id })`, `TicketCard(ticket, isExpanded = false, onClick = { onTicketClick(ticket.id) }, status = ticket.effectiveStatus(now))`;
  - empty state `my_tickets_empty`, centred, as `ListScreen`;
  - KDoc: filtering happens upstream (`isParticipant`); this screen is unreachable for anonymous users (the top-bar action is hidden).
  - Previews: a list with OPEN, a RESOLVED ticket at day 4 and one at day 6 (shown CLOSED); empty; PT; landscape.
- Diogo's `Destinations.kt`: `MyTicketsRoute` with KDoc "Tickets the signed-in citizen created or follows (F-014); never offered to anonymous users".
- Diogo's `MainTopAppBar.kt`:
  - `MY_TICKETS` and `top_bar_title_my_tickets` in the `when`;
  - params `showMyTickets: Boolean`, `onMyTicketsClick: () -> Unit` (before `modifier`);
  - in `actions`, before "+": `if (showMyTickets && screen != TopBarScreen.MY_TICKETS && screen != TopBarScreen.REPORT) IconButton(onMyTicketsClick) { Icon(Icons.Filled.Person, cd_my_tickets) }`;
  - `BarPreview` gets `showMyTickets = true` and a `MyTicketsBarPreview`;
  - KDoc line for the new action.
- Diogo's `MainActivity.kt`:
  - `MainTopAppBar(..., showMyTickets = app.currentUser is Citizen, onMyTicketsClick = { navController.navigate(MyTicketsRoute) { launchSingleTop = true } })`;
  - the destination:
    ```kotlin
    composable<MyTicketsRoute> {
        val user = app.currentUser
        MyTicketsScreen(
            tickets = if (user == null) emptyList() else tickets.filter { it.isParticipant(user.id) },
            now = System.currentTimeMillis(),
            onTicketClick = { id -> navController.navigate(TicketDetailRoute(id)) { launchSingleTop = true } },
        )
    }
    ```
  - `toTopBarScreen()`: `hasRoute<MyTicketsRoute>() -> TopBarScreen.MY_TICKETS`.
- Commit: `feat(F-014): add the My tickets screen and top-bar entry`.
- Verify: build + lint, previews, device D1, D2, D11, D12, D14.

## Device checks (emulator)
**Preconditions:**
- the test-mode database is seeded: `sample-1` OPEN by `citizen-1`; `sample-2` IN_PROGRESS by `citizen-2`, follower `citizen-3`; `sample-3` RESOLVED in October by `citizen-1`, so it is long expired;
- `adb` and the project's Firestore REST base URL `https://firestore.googleapis.com/v1/projects/<projectId>/databases/(default)/documents`, where `<projectId>` is the `project_id` in `app/google-services.json` (do not paste it into docs). Test mode needs no token.

The stand-in user is set by editing `CityFixApplication.currentUser` and rebuilding:
- **A** = `Citizen("citizen-1", registrationStatus = RegistrationStatus.ACTIVE)`;
- **B** = `Operator(CityFixApplication.DEV_OPERATOR_ID)`, on a second emulator (install B there, then switch the line back to A);
- **C** = `Citizen("citizen-3", registrationStatus = RegistrationStatus.ACTIVE)`;
- **N** = `null`.

Revert the line to `null` before the last commit.

| # | Check | Expected |
| --- | --- | --- |
| D1 | N: browse dashboard, list, a detail | No person icon in the top bar anywhere; the detail is the PUBLIC view (no description, history or thread) |
| D2 | A: tap the person icon | "My tickets" lists sample-1 and sample-3 (badge CLOSED), not sample-2; title "My tickets"; no person icon on this screen. With `Citizen("nobody-1", ACTIVE)`: the empty text |
| D3 | A: open sample-1 from My tickets, then sample-2 from the general list | sample-1: description, exact map with a marker, history, thread with input, no Reopen. sample-2: PUBLIC view, no thread |
| D4 | A: open sample-3 (resolved in October) | Badge CLOSED; no Reopen; the "closed" note instead of the input. REST `GET .../tickets/sample-3`: `currentStatus` CLOSED, last `statusHistory` entry CLOSED, `responsibleId` "system", `dateTime` = RESOLVED entry + 432000000. `GET .../ticketLocations/sample-3`: `active` false. `GET .../publicTickets/sample-3`: `currentStatus` CLOSED. `stats/summary` unchanged. History row shows "Closed automatically". Reopening the detail adds no second CLOSED entry |
| D5 | A: send "Is there an estimate?" on sample-1 | Shows at once labelled "You", draft cleared. REST `GET .../tickets/sample-1/messages`: one doc with `text`, `sentAt`, `authorId` "citizen-1", `byOperator` false |
| D6 | B on emulator 2: open sample-1 | Operator sees the thread live (A's message labelled "Citizen"). B sends "Next week" -> appears on A without refresh, labelled "Municipality"; A replies -> appears on B. (No second emulator: `POST .../tickets/sample-1/messages` with `{"fields":{"text":{"stringValue":"Next week"},"sentAt":{"integerValue":"<now ms>"},"authorId":{"stringValue":"dev-operator-1"},"byOperator":{"booleanValue":true}}}` and watch A) |
| D7 | A: airplane mode, send "offline test" | Shown at once; REST GET does not have it; airplane off -> REST GET has it and B shows it |
| D8 | Day 4. Setup in the console on a citizen-1 ticket that is OPEN (sample-1 or a new report by A): note `stats/summary`; set `currentStatus` RESOLVED, append to `statusHistory` `{status: RESOLVED, dateTime: <now - 4 d in ms>, responsibleId: "operator-1"}`; `ticketLocations/<id>.active` false; `publicTickets/<id>.currentStatus` RESOLVED (PUBLIC); adjust `stats/summary` by hand: activeTickets -1, resolvedTickets +1, repairTimeSumMillis + (RESOLVED dateTime - createdAt) | A: My tickets badge RESOLVED; detail shows Reopen. Confirm with a blank field -> "Write a justification." and the dialog stays. "Still broken" -> dialog closes, badge OPEN, Reopen gone. REST: `currentStatus` OPEN, last entry `{OPEN, justification "Still broken", responsibleId "citizen-1"}`; `publicTickets` OPEN; `ticketLocations.active` true; `stats/summary` back to the values noted before setup |
| D9 | Day 6. Same setup with `dateTime` = now - 6 d (and the same stats adjustment) | No Reopen; badge CLOSED; opening the detail writes CLOSED as in D4 (CLOSED entry at RESOLVED + 5 d); stats unchanged by the close |
| D10 | A on the D8 ticket before confirming: open the dialog, type text, rotate | The dialog and the text survive; the message draft also survives rotation |
| D11 | C: My tickets | sample-2 listed (follower); its detail is REGISTERED with the thread; C can send |
| D12 | Landscape on the detail and My tickets | Two panes; the thread under the history in the right pane; the keyboard does not hide the input (the field scrolls into view); nothing clipped |
| D13 | PT device language | All new strings in Portuguese (thread labels, dialog, errors, "Encerrado automaticamente", "As minhas ocorrências") |
| D14 | Build + lint after each slice | 0 errors; no new warnings from F-014 code |

## Conventions
- `.claude/rules/kotlin.md` and `kotlin-conventions`:
  - stateful `TicketDetailScreen` with a stateless `TicketDetailContent`; previews on the stateless one;
  - `StateFlow` UI state with `update { it.copy(...) }`;
  - no hard-coded strings (EN + PT);
  - 48 dp targets (`IconButton`); content descriptions on icons;
  - portrait and landscape.
- Class patterns:
  - factory + `viewModel(factory = ...)` inside the destination (Contacts_#3, P08);
  - `hashMapOf` documents and typed getters (T8);
  - `batch()` + `commit()` with `addOnFailureListener` (T8 "clear all");
  - `AlertDialog` (P06/P08), `OutlinedTextField` (Contacts EditScreen), `LaunchedEffect` (Contacts MainActivity).
- Beyond class (say so in KDoc):
  - the reused `listen` (snapshot listener as a `Flow`);
  - `FieldValue.increment` through `statsIncrements`;
  - the content slot on `ShowScreen` (a Compose content lambda, as `Scaffold`'s `topBar`).
- Keep it short: no try/catch, no extra validation beyond D-008's rules, no transactions (school-project rule).
- Status only through `transition` / `closeIfExpired`; never `copy(currentStatus = ...)` (decision 0011). Every status batch adds `statsIncrements` and the `publicTickets` / `ticketLocations` updates.
- Commits: Conventional Commits with `F-014`, no attribution.

## Do not touch
- The rest of `model/TicketLifecycle.kt`: only the `private` on `isParticipant` goes (Q5).
- `model/TicketPrivacy.kt`, `TicketsList.kt`, `viewmodel/TicketsViewModel(+Factory).kt`.
- `firestore.rules`, `storage.rules`, `app/google-services.json`, the Maps key, the version catalog.
- `DebugSeed.kt`, `SampleTickets.kt`. Device-check data is set by hand in the console, not by the seed.
- `CityFixApplication.currentUser` stays `null` in every commit (stand-ins only for local testing).
- `ReportScreen`, `ReportViewModel`, the upload code.

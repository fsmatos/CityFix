# F-016 coding plan

## Files
| Path | Change | Why |
| --- | --- | --- |
| `utils/Notifications.kt` | new | Channels + `show` (class Firebase demo `showNotification`) |
| `CityFixApplication.kt` | `onCreate` creates channels; `ticketNotifier`, `pushTokens` | One container (decision 0009) |
| `AndroidManifest.xml` | `POST_NOTIFICATIONS`; `singleTop`; FCM `<service>` | P11 manifest |
| `MainActivity.kt` (Diogo) | permission, snackbar, open ticket from extra, watch | ~40 lines |
| `data/PushTokens.kt` | new | Token -> `users/{uid}.fcmTokens` |
| `data/CityFixMessagingService.kt` | new | P11 `FirebaseMessagingService` |
| `model/Ticket.kt`, `data/FirestoreMapping.kt` | `lastMessageAt`, `lastMessageAuthorId` | Q4 |
| `data/FirestoreTicketsRepository.kt` | message writes set both fields; `participantTicketsOnce`, `allTicketsOnce` | Q4, worker |
| `data/TicketsRepository.kt` | `participantTicketsOnce`, `allTicketsOnce` | Worker one-shot `get()` (citizen / operator) |
| `model/TicketNotifications.kt` | new | Pure event rule |
| `data/NotificationPrefs.kt` | new | `lastCheckedAt` per uid (slides 09) |
| `data/TicketNotifier.kt` | new | Rule -> `show`, saves time |
| `data/TicketCheckWorker.kt` | new | Periodic check (Q7) |
| `viewmodel/NotificationsViewModel.kt` + `Factory` | new | Watch, token, worker on/off |
| `res/values/strings.xml`, `values-pt/strings.xml` | ~14 strings | EN + PT |

## Interfaces
```kotlin
// utils/Notifications.kt
const val CHANNEL_TICKETS = "tickets"; const val CHANNEL_MESSAGES = "messages"; const val CHANNEL_PROXIMITY = "proximity"
const val EXTRA_TICKET_ID = "ticketId"
fun createNotificationChannels(context: Context)
fun showNotification(context: Context, channel: String, id: Int, title: String, text: String, ticketId: String?)

// model/TicketNotifications.kt
enum class TicketEventKind { STATUS, MESSAGE, OPERATOR_NEARBY, CLOSING_SOON, CLOSED }
data class TicketEvent(val ticketId: String, val categoryName: String, val kind: TicketEventKind,
                       val status: TicketStatus? = null, val justification: String? = null)
fun Ticket.eventsFor(userId: String, isOperator: Boolean, since: Long, now: Long): List<TicketEvent>

// data/
class NotificationPrefs(context: Context) { fun lastCheckedAt(uid: String): Long?; fun save(uid: String, time: Long) }
class TicketNotifier(context: Context) { fun check(uid: String, isOperator: Boolean, tickets: List<Ticket>, now: Long) }
class PushTokens(messaging: FirebaseMessaging, firestore: FirebaseFirestore) {
    suspend fun register(uid: String); fun save(uid: String, token: String) }
// TicketsRepository
suspend fun participantTicketsOnce(userId: String): List<Ticket>
suspend fun allTicketsOnce(): List<Ticket>
// TicketCheckWorker companion
fun schedule(context: Context, uid: String, isOperator: Boolean); fun cancel(context: Context)

// viewmodel/NotificationsViewModel.kt
class NotificationsViewModel(app: CityFixApplication) : ViewModel() {
    suspend fun watch(user: User, tickets: Flow<List<Ticket>>)  // collect -> notifier.check
    fun onUserChanged(user: User?)                              // token + schedule/cancel worker
}
```
`eventsFor` (skips a projected ticket; citizen only if `isParticipant`):
- STATUS: each `statusHistory` entry after index 0 with `since < dateTime <= now`, `responsibleId != userId`, status not CLOSED.
- MESSAGE: `since < lastMessageAt`, author not `userId`; dropped when a STATUS event of the same ticket has a justification (Q10).
- Operator only (Q2): STATUS and MESSAGE only when the actor (`responsibleId` / `lastMessageAuthorId`) is in `participantIds`, so one operator's action does not notify the others.
- Citizen only: OPERATOR_NEARBY when `lastOperatorNearbyAt > since` and `now - it < PROXIMITY_ALERT_INTERVAL_MS` (0022 contract, Q8); CLOSING_SOON when `closingWarningAt()` is in `(since, now]` and `!isLocked(now)`; CLOSED when `resolvedAt() + REOPEN_WINDOW_MS` is in `(since, now]`.

`TicketNotifier.check`: `since = prefs.lastCheckedAt(uid)`; if null, save `now` and stop (Q3). Otherwise show each event (id = `(ticketId + kind).hashCode()`), then save `now`.

## Steps
1. **Channels, permission, tap.**
   - `Notifications.kt`; channels in `CityFixApplication.onCreate()` (class creates them before notifying).
   - `show`: launch intent with `EXTRA_TICKET_ID`, `FLAG_IMMUTABLE`, `setAutoCancel(true)`; skip when `areNotificationsEnabled()` is false.
   - Manifest: `POST_NOTIFICATIONS`, `android:launchMode="singleTop"`.
   - `MainActivity`: `openTicketId` state from `intent` in `onCreate` and `onNewIntent`; `CityFixApp` navigates to `TicketDetailRoute(id)` once session is not Loading, then clears it.
   - When role becomes CITIZEN or OPERATOR on API 33+ without the permission: launch `RequestPermission`; denied -> snackbar `notifications_denied`.
   - Commit `feat(F-016): add notification channels and permission`.
   - Verify: build + lint; temporary debug call to `showNotification`, tap opens the ticket; revert.
2. **FCM.**
   - `CityFixMessagingService`: `onMessageReceived` reads `notification?.title/body`, else `data["title"/"body"]`, `data["ticketId"]`, calls `showNotification` (tickets channel); `onNewToken` -> `pushTokens.save` if `auth.currentUser`.
   - `PushTokens.register`: `messaging.token.await()` then `update("fcmTokens", FieldValue.arrayUnion(token))`, not awaited, failure logged.
   - `NotificationsViewModel(+Factory)` with `onUserChanged` (token only for now); `LaunchedEffect(session.ticketsUser?.id)` in `CityFixApp`.
   - Commit `feat(F-016): receive FCM messages and save the token`.
   - Verify: token in Firestore; console "Send test message" (custom data `ticketId`) with app in background; tap opens ticket.
3. **Events while the app is open.**
   - `Ticket` fields + `toTicket`; `toAnonymousView` clears them; `sendMessage` becomes a batch (message + both fields); `commitStatusChange` adds them when `message != null`.
   - `TicketNotifications.kt`, `NotificationPrefs` (`getSharedPreferences("notifications", MODE_PRIVATE)`), `TicketNotifier`, strings per kind and status.
   - `watch` in `CityFixApp`: `LaunchedEffect(user?.id) { repeatOnLifecycle(STARTED) { watch(user, ticketsViewModel.tickets) } }`, like D-014.
   - Commit `feat(F-016): notify ticket changes while the app is open`.
   - Verify: two emulators (citizen, operator): start work, info request, message both ways, own message silent; `lastOperatorNearbyAt` set in console notifies once; anonymous gets nothing.
4. **Worker.**
   - `TicketCheckWorker` (`CoroutineWorker`): `auth.currentUser` or success; `isOperator` from input data; operator -> `allTicketsOnce`, citizen -> `participantTicketsOnce`; `ticketNotifier.check(uid, isOperator, ...)`; exception -> `Result.retry()`.
   - `schedule`: `PeriodicWorkRequestBuilder(15, MINUTES)`, `NetworkType.CONNECTED`, input data `uid`/`isOperator`, `enqueueUniquePeriodicWork("ticket-check", UPDATE, ...)`; `onUserChanged` schedules for an active citizen or operator, cancels otherwise (Q2).
   - Commit `feat(F-016): check tickets in the background`.
   - Verify: app swiped away, status changed on the other device (and a citizen message with the operator app closed), App Inspection > Background Task "run now": notification. Temporarily `CLOSING_WARNING_MS = 2 min` on a RESOLVED ticket: warning, then Encerrado; revert.

## Conventions
- Notifications and WorkManager only in `utils`/`data`, called by the ViewModel (kotlin rules); EN+PT strings; class code style, no extra hardening.
- No coordinates or operator data in notifications (0022).
- Conventional Commits with `F-016`, no attribution.

## Do not touch
- `firestore.rules`, `google-services.json`, no deploy.
- D-014 proximity code, `UploadWorker`, registration/profile screens.
- The rest of `MainActivity` (gate, redirect, location).

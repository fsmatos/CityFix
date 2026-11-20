# F-016: Notifications (local and FCM)
Type: feature · Task file: docs/tasks/phase-2/F-016-notifications.md
Labels: none

## Problem
Brief III: notify status changes, info-request messages and replies, the 20 m proximity alert and 24 h before closing (II.2-II.4 say who). V: notification permission. No notification code exists.

## Outcome
A participant citizen is notified when someone else changes the status, writes a message, comes within 20 m (D-014), and 24 h before the close. Tapping opens the ticket. A console test message (FCM) also shows.

## Acceptance checks
- [ ] An operator's status change notifies author and followers, not the operator.
- [ ] A message from the other side notifies; one's own does not.
- [ ] Tapping a notification opens that ticket.
- [ ] Denying the permission does not crash; a snackbar explains it.
- [ ] 24 h warning tested with a shortened constant (reverted).
- [ ] A `lastOperatorNearbyAt` write shows the proximity notification (D-014's D11).
- [ ] Console test message shows with the app in the background.

## Approach (class first)
- **No server** ([decision 0012](../../decisions/0012-firebase-plan-notifications-close-maps.md) point 2): compare the tickets already read with a stored "last checked" time; show local notifications.
- **`utils/Notifications`**: 3 channels + `showNotification` = class Firebase demo (P11) `showNotification`/`createNotificationChannel`.
- **Permission**: `RequestPermission` for `POST_NOTIFICATIONS`, API 33+, as P11 `askNotificationPermission` (Q6).
- **Pure rule `model/TicketNotifications`**: events since the last check, never one's own (Q3, Q10).
- **Messages**: two new ticket fields so the ticket listener sees them (Q4).
- **App open**: `NotificationsViewModel.watch` over the shared `tickets` flow, like D-014's proximity watch.
- **App closed**: periodic `CoroutineWorker` for citizens and operators, WorkManager slides 10/11 + our `UploadWorker` (Q2, Q7).
- **24 h warning, Encerrado**: time checks in the same rule (Q5).
- **FCM**: `CityFixMessagingService` as P11; token in `users/{uid}.fcmTokens` (Q1).
- **Tap**: intent extra `ticketId`; beyond class: `singleTop` + `onNewIntent`.
- Last-check time in `SharedPreferences` (slides 09).

## Architecture impact
New `utils/Notifications`, `model/TicketNotifications`, `data/` `TicketNotifier`, `NotificationPrefs`, `PushTokens`, `TicketCheckWorker`, `CityFixMessagingService`, `viewmodel/NotificationsViewModel(+Factory)`. Two `Ticket` fields (amends [0010](../../decisions/0010-firestore-schema.md)). Diogo's `MainActivity` changes (tell him).

## Failure modes
- Needs the app or worker running; Doze can delay the worker (report limit).
- Device clocks differ: an event dated before the last check is missed.
- App and worker at once: same id per ticket and kind replaces, not duplicates.
- Draft rules would refuse the new fields (test mode accepts).

## Open questions (answered at Gate 1, 2026-11-20)
- Q1 Decision 0012: listener + worker, FCM only for a console test, no Cloud Function? [yes]
- Q2 II.2 "a outra parte e notificada": notify operators of citizen messages, clarification requests, reopens? [yes, app open and worker; no Cloud Function (class shows FCM reception only)]
- Q3 One `lastCheckedAt` per user instead of 0022's per-ticket value; first run notifies nothing? [yes]
- Q4 Messages are a subcollection the ticket listener misses: add `lastMessageAt`/`lastMessageAuthorId` to the ticket in the same write? [yes]
- Q5 24 h warning/Encerrado: time check each run, or the task file's one-time job per ticket (cancel on reopen)? [time check, up to ~15 min late]
- Q6 Class asks at app start; our rule says point of use. [when a citizen or operator session starts]
- Q7 Worker every 15 min (minimum), network constraint, citizens and operators (Q2), cancelled at logout? [yes; only closed-app notifications can be ~15 min late]
- Q8 D-014 contract as written, but trigger "> lastCheckedAt" (Q3)? [confirm]
- Q9 Title = category; body = status, "New message" or the justification; no message text on the ticket? [yes]
- Q10 Info request = status + message: one notification? [status, justification as body]

## Out of scope
Cloud Functions; notification settings; "new ticket" to operators; worker closing expired tickets; FCM topics; Navigation deep links.

## Slices
1. Channels, helper, permission, tap opens ticket.
2. FCM service + token.
3. Events while the app is open.
4. Periodic worker (closed app, 24 h warning, Encerrado).

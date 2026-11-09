# F-016: Notifications (local and FCM)

Owner: Francisco (`dev/Francisco`) · Layer: `data` / `utils` · Phase 2 · Status: to do · Target: 2026-11-19
Depends on: F-011, D-009 · Blocks: D-014 (uses the notification helper)

## Goal
Notify citizens about their tickets: status changes, messages, the 24 h warning before closing, and (through D-014) the operator proximity alert.

## Why
- Brief III: local and remote notifications for status changes, info-request messages and replies, the 20 m proximity alert, and 24 h before final closing.
- Brief II.4: resolution notification to the author.
- Brief V: dynamic notification permission.

## What to build
- **`utils/Notifications`:** channels (tickets, messages, proximity), `POST_NOTIFICATIONS` request on Android 13+, one `notify(...)` helper with a tap that opens the ticket detail (deep link to `TicketDetailRoute`).
- **Delivery, per the F-010 Gate 1 decision:**
  - Blaze: FCM token saved in `users/{uid}.fcmTokens`, `FirebaseMessagingService`, and Cloud Functions on ticket/message writes (functions code in the repo; deploying needs the person's OK).
  - Spark: Firestore listeners on the user's tickets and messages while the app runs, plus a periodic `CoroutineWorker` (15 min) that checks for changes since the last seen time when it is closed. FCM token still registered so the console can send a test push.
- **24 h warning:** for each resolved ticket of the user, a one-time WorkManager job at `resolvedAt + 4 days`, cancelled if the ticket is reopened (or a scheduled Cloud Function on Blaze).
- Do not notify a user about their own action.

## Files
New `utils/Notifications.kt`, `data/NotificationWorker.kt` and/or `data/CityFixMessagingService.kt`, maybe `functions/`; `AndroidManifest.xml`, `MainActivity.kt` (deep link; tell Diogo), strings EN/PT.

## Acceptance checks
- [ ] An operator changing a status (D-013, or by hand in the console before it exists) notifies the author.
- [ ] A new message from the other side notifies; one's own does not.
- [ ] Tapping a notification opens that ticket.
- [ ] Denying the permission does not crash; the app explains the effect.
- [ ] 24 h warning tested by shortening the delay in a debug build.

## References
Brief II, III, V; decision 0010; F-010 Gate 1 decisions 1-3.

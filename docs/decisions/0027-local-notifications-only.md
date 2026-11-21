# 0027: Local notifications only, FCM removed

Status: accepted (F-019, chosen by the person on 2026-11-20). Supersedes the FCM part of [0024](0024-notifications.md) and of [0012](0012-firebase-plan-notifications-close-maps.md) point 2; amends [0010](0010-firestore-schema.md) (`fcmTokens`). Code detail: `docs/architecture/overview.md`, "FCM removed (F-019)".

## Context
- Brief III: "notificações locais e remotas (Push Notifications via Firebase Cloud Messaging ou outra solução)".
- F-016 kept FCM only to receive a test message sent from the Firebase console (P11 pattern); no server ever sent to the stored tokens.
- The real ticket notifications were already local: a Firestore listener while the app is open and `TicketCheckWorker` when it is closed. Class shows local notifications without FCM in P10 (Contacts_#3 `ProximityService`).

## Decision
- Remove FCM: `firebase-messaging`, `CityFixMessagingService`, `PushTokens`, the manifest service, the token save at session start and the `fcmTokens: []` field at registration.
- `'fcmTokens'` leaves the owner allow-list of the draft `firestore.rules` (not deployed).
- Notifications stay as F-016 built them: local only.

### Rejected
- Keep FCM as P11 shows it, for the console test: the person chose not to use it.
- A Cloud Function that sends FCM on status change: against 0012 (no server code).

## Consequences
- **Deviation from the brief:** the "remotas (Push Notifications)" part of brief III is not covered; the report must say so and why.
- Old `users` documents may still hold an `fcmTokens` array; nothing reads or writes it.
- The FCM console test in F-016's open checks no longer applies.

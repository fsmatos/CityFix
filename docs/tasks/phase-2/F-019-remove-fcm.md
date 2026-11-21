# F-019: Remove FCM

Owner: Francisco · Asked in chat on 2026-11-20.

## Goal
Remove Firebase Cloud Messaging from the app. Notifications stay local only: the Firestore watch while the app is open and `TicketCheckWorker` when it is closed, as F-016 built them. This follows the local notifications in class (P10, Contacts_#3 `ProximityService`).

## Context
- F-016 (decision 0024) added FCM only so a test message sent from the Firebase console could be received. No server sends to it.
- Brief III asks for "notificações locais e remotas (Push Notifications via Firebase Cloud Messaging ou outra solução)". The person was told this and confirmed the removal.

## Acceptance checks
- No `firebase-messaging` dependency, `FirebaseMessagingService`, FCM token code or `fcmTokens` written by the app.
- Local ticket notifications still appear (app open and closed), and tapping one still opens the ticket.
- `./gradlew assembleDebug` and `./gradlew lint` pass.
- Docs no longer describe FCM as part of the app.

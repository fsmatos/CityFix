# F-019: Remove FCM
Type: refactor (removal) · Task file: docs/tasks/phase-2/F-019-remove-fcm.md
Labels: none

## Problem
F-016 added FCM (receiving service, token in `users/{uid}.fcmTokens`) only for a console test. The person decided the app should not use FCM.

## Outcome
Notifications are local only, as in P10 / Contacts_#3. No FCM code, dependency or token writes remain.

## Behavior that must not change
- Ticket notifications: the in-app watch (`NotificationsViewModel.watch`) and the 15 min `TicketCheckWorker`, for citizens and operators.
- Channels, the `POST_NOTIFICATIONS` request, tapping a notification opens its ticket, Log out clears the tray.

## Acceptance checks
- [ ] No `firebase-messaging`, `CityFixMessagingService`, `PushTokens` or `messaging` left in the code or manifest.
- [ ] A new registration writes no `fcmTokens` field.
- [ ] A status change still notifies the citizen (app open, and app closed via the worker); the tap opens the ticket.
- [ ] Build and lint pass.

## Approach
Delete the two FCM classes and every reference to them, then update the docs. One new decision record (0027) supersedes the FCM part of 0024 and 0012.
Alternative: keep FCM as P11 shows it (rejected by the person on 2026-11-20).

## Architecture impact
`CityFixApplication` loses `messaging` and `pushTokens`. `NotificationsViewModel.onUserChanged` only schedules or cancels the worker. The other Firebase services are unchanged.

## Failure modes
- A leftover import or reference breaks the build: caught by `assembleDebug`.
- Existing `fcmTokens` arrays stay in old `users` documents; nothing reads them, so they are harmless.

## Risks and open questions
- **R1, brief III:** "remotas (Push Notifications...)" is no longer covered. Accepted by the person; decision 0027 and the report state it.
- **Q1:** remove `'fcmTokens'` from the draft `firestore.rules` allow-list? Suggest **yes** (nothing writes it any more; rules are not deployed). Needs your OK (project rule).
- **Q2:** keep the "FCM" words in old task files and earlier work folders? Suggest **yes**, keep them as history; only living docs change.

## Out of scope
Cloud Functions or any other push service; cleaning old `fcmTokens` from the database; `firebase deploy`.

## Slices
1. Remove FCM from the code, manifest and Gradle (+ the rules line if Q1 = yes).
2. Docs: decision 0027, overview, 0012/0024/0010 amendment lines, CLAUDE.md, report notes, test checklist.

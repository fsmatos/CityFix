# 0024: Notifications

Status: accepted (F-016, Gate 1 on 2026-11-20: Q1, Q3-Q10 as planned; Q2 changed: the worker also covers operators). Review fixes B1, S1-S3 applied. Device checks run on 2026-11-20 (`docs/work/F-016-notifications/state.md`); the 24 h warning and the FCM console test are still to run. Amends [0010](0010-firestore-schema.md) (two ticket fields) and confirms the F-016 contract of [0022](0022-operator-proximity-alert.md) (Q8 change). Code detail: `docs/architecture/overview.md`, "Notifications (F-016)".

## Context
- Brief III: notify status changes, info requests and replies, the 20 m proximity alert and 24 h before the close; brief V: notification permission; II.2-II.4 say who.
- [Decision 0012](0012-firebase-plan-notifications-close-maps.md) point 2: no server, no Cloud Functions.
- Class (P11) covers FCM reception, console sending, channels and the permission; not sending from a server.

## Decision
- **Client-only delivery (Q1).** App open: a tickets listener that skips lists served from the offline cache (`MetadataChanges.INCLUDE`, `isFromCache`), so changes made while closed or offline are not hidden (review B1 and the device check); the worker reads with `Source.SERVER`. App closed: a periodic WorkManager worker. Local notifications only.
- **FCM only for console test messages.** `CityFixMessagingService` as P11; token kept in `users/{uid}.fcmTokens`.
- **Worker (Q2, Q7):** `TicketCheckWorker`, unique periodic work `ticket-check`, every 15 min (the minimum), network constraint, for active citizens and operators; cancelled at Log out.
- **One `lastCheckedAt` per user (Q3)** in SharedPreferences (`notifications`); each check notifies the events in `(lastCheckedAt, now]`, then saves `now`. The first check only saves.
- **Pure event rule** `Ticket.eventsFor(userId, isOperator, since, now)` in `model/TicketNotifications.kt`:
  - never one's own action;
  - citizen: only tickets they take part in; status, message, operator nearby, 24 h warning, Encerrado;
  - operator: status and message only when a participant citizen acted (so operators do not notify each other).
- **Messages (Q4):** new ticket fields `lastMessageAt`, `lastMessageAuthorId`, written in the same batch as the message, because the ticket listener does not see the `messages` subcollection.
- **24 h warning and Encerrado by time check (Q5):** each check tests whether the time passed since the last one; up to ~15 min late with the app closed.
- **Permission (Q6):** asked when a citizen or operator session starts, after the location answer (one request at a time); denial shows a snackbar.
- **Content (Q9, Q10):** title = category; text = new status, "New message", or "status: justification" (an info request is one notification). No coordinates or operator data.
- **D-014 contract (Q8):** confirmed, but the trigger is `lastOperatorNearbyAt > lastCheckedAt` (not a per-ticket value) and less than 1 h old.
- **Tap:** intent extra `ticketId` opens the ticket; `singleTop` + `onNewIntent` when the app is open.
- **Log out clears the tray** (review S1), so the next person does not read the previous user's texts.

### Rejected
- Cloud Function on status change -> FCM: not in the class, against 0012.
- One-time job per ticket for the 24 h warning (cancel on reopen): more code than a time check.
- Per-ticket "last notified" values (0022 contract text): one time per user is enough.
- Operators app-open only (original plan): Gate 1 Q2 asked for the worker too.

## Consequences
- No real push: needs the app open or the worker; Doze can delay the worker past 15 min.
- Device clocks differ: an event dated before the last check is missed.
- App and worker use the same notification id per ticket and kind, so they replace, not add.
- Draft rules would refuse `fcmTokens`, `lastMessageAt`, `lastMessageAuthorId` (test mode accepts); not changed.

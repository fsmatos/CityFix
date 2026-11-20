# F-016 review

Range: `80801af..HEAD` (427da18, fd2c124, bcbc93e, edd308d). Checked against 00-plan, 01-coding-plan, the Gate 1 answers (Q2 changed: the worker covers operators too), brief II.2-II.5, III and V, and kotlin-conventions. Device checks are still pending (state.md Log), so this review is a code reading only.

Checked and found correct:
- Recipients. A user's own actions never notify them (`responsibleId` / `lastMessageAuthorId != userId`). An operator is notified only when a participant citizen acted, so one operator's work does not notify the others. A citizen is notified only on tickets they take part in. The automatic `system` CLOSED entry is skipped and replaced by the time-based CLOSED event.
- Anonymous, pending and setup users: `ticketsUser` is null for them, so there is no watch and the worker is cancelled. `toAnonymousView` clears the two new fields. A notification tapped by an anonymous user goes through `detailAudience`, so it shows the public view or "not found", never private data.
- Firestore writes. `sendMessage` writes the message and `lastMessageAt`/`lastMessageAuthorId` in one batch. `commitStatusChange` adds both fields only when a message is written. Both use the keys of `messageDocument` (`sentAt`, `authorId`).
- Worker. It is unique periodic work with a 15 min period, a network constraint and the uid/isOperator in its input data. `cancel` runs when `ticketsUser` becomes null (Log out). The uid check skips a run after the account changes.
- Tap navigation. A cold start reads the extra only when `savedInstanceState == null`; an app that is already open gets it through `singleTop` + `onNewIntent`. The effect sits after the role redirect and is never reached while the session is Loading.
- Permission. It is asked once per session, only after the location answer and only on API 33+. A denial shows a snackbar, and a rotation does not ask again.
- Text is in EN and PT, and no coordinates or operator data appear in any notification.

## Blocking

**B1. Changes made while the app was closed are lost when the app opens on a stale cached snapshot.**
`viewmodel/NotificationsViewModel.kt:44` together with `data/TicketNotifier.kt:42`.
With offline persistence on, the tickets listener first emits the cached snapshot, then the server one. On a cold start the cached list is as old as the last worker run or app session, which can be 15 min ago or more under Doze. `watch` checks that list right away, finds nothing new, and saves `lastCheckedAt = now`. The server snapshot then brings in, for example, an operator's status change dated a few minutes ago. That date is earlier than `since`, so it is never notified.
To reproduce: close the citizen app, start work on device A, reopen the citizen app within 15 min. You get no notification. This is the two-emulator acceptance check in the plan.
Fix (class-level, a few lines): in `watch`, before `tickets.collect`, check one server read the same way the worker does:
```kotlin
val first = try {
    if (isOperator) app.ticketsRepository.allTicketsOnce()
    else app.ticketsRepository.participantTicketsOnce(user.id)
} catch (e: Exception) { null }
if (first != null) app.ticketNotifier.check(user.id, isOperator, first, System.currentTimeMillis())
tickets.collect { ... }
```
`get()` goes to the server when online, so the cached first emission that follows has nothing new left to consume. Note the reason in state.md.

## Should-fix

**S1. Notifications stay in the tray after Log out, so the next (anonymous) person on the device sees them.**
`MainActivity.kt:292` (`logOut`).
The title is the category, and the text can be an info-request question or a reopen justification. The CLAUDE.md rule says anonymous users must never see personal data. Fix: add `fun onLoggedOut()` to `NotificationsViewModel` that calls `NotificationManagerCompat.from(app).cancelAll()`, and call it from the `logOut` lambda before `signOut()`. Do not put it in `onUserChanged(null)`: that also runs on every app start while the session is Loading, so it would clear the tray at each start.

**S2. MESSAGE is dropped whenever any STATUS event in the window has a justification, even when the message is unrelated.**
`model/TicketNotifications.kt:55`.
Q10 meant "an info request is a status change plus a message: notify once". But `startWork` with a note and `reopenTicket` also carry a justification without writing a message. So "operator starts work with a note, then sends a message" (or a reopen followed by a message) inside one worker window loses the message notification. Fix: drop the MESSAGE only when it is the info-request message itself. `askForClarification` uses the same `now` for both, so replace `events.none { it.justification != null }` with `statusHistory.none { it.dateTime == messageAt && it.justification != null }`.

**S3. Every app start cancels and re-enqueues the worker, so it runs at once, at the same time as the first in-app check.**
`MainActivity.kt:384` with `NotificationsViewModel.kt:30`.
During Loading, `ticketsUser` is null, so `cancel` runs. Then `schedule(UPDATE)` enqueues new work, whose first periodic run is immediate. The worker (background thread) and `watch` (main thread) can then read the same `since` and both show the same event. The second one replaces the first (same id) but alerts again. The Log calls this harmless; it is the only source of duplicate alerts. Fix: in the `LaunchedEffect`, skip the call while `session == Session.Loading` (`if (session != Session.Loading) notificationsViewModel.onUserChanged(...)`), keyed on `session.ticketsUser?.id` and `session is Session.Loading`. A real Log out still cancels.

## Nit

**N1. Unbounded times.** `TicketNotifications.kt:53` (message) and `:62` (nearby) have no `<= now` bound, unlike STATUS. With a sender clock ahead of this one, the same message or proximity alert is shown again on the next check (same id, alerts twice). Fix: add `&& messageAt <= now` and `&& nearbyAt <= now`.

**N2. FCM token after Log out.** It stays in `users/{uid}.fcmTokens`, and the next account on the device adds the same token, so a console test to user A reaches user B. Fix: in the new `onLoggedOut` (S1), call `firestore.collection("users").document(uid).update("fcmTokens", FieldValue.arrayRemove(token))` before signing out. This is optional, since only console tests use the token.

**N3. Reopen from Recents.** Opening the app from Recents after backing out replays the launch intent with `savedInstanceState == null`, so the ticket opens again. Fix: `if (savedInstanceState == null && intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY == 0)` at `MainActivity.kt:180`.

**N4. No rationale before the request.** kotlin-conventions says "rationale first" for runtime permissions; the code asks directly, as P11 does. Either add one sentence to the snackbar flow (`shouldShowRequestPermissionRationale`), or record in state.md that the class pattern was kept on purpose (Gate 1 Q6).

**N5. MVVM.** `NotificationsViewModel` takes the whole `CityFixApplication` and calls `TicketCheckWorker` and `TicketNotifier` directly instead of going through interfaces (kotlin-conventions: "ViewModels depend on repository interfaces"). The `ImpactViewModelFactory(app)` precedent exists, so this is fine for the defence, but be ready to explain it. Optionally pass `TicketsRepository`, `TicketNotifier`, `PushTokens` and a `schedule`/`cancel` pair.

**N6. Icon.** `utils/Notifications.kt` uses `android.R.drawable.ic_dialog_info` as the small icon. An app drawable (monochrome) would look intentional in the status bar.

**N7. Tap while on Account setup.** The tap effect also runs for SETUP/PENDING roles and puts the public detail on top of Account setup. It is harmless (public view), but adding `role == AccessRole.CITIZEN || role == AccessRole.OPERATOR || role == AccessRole.ANONYMOUS` to the guard keeps the setup flow clean.

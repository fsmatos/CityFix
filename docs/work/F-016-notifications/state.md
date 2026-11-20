# F-016: Notifications (local and FCM)
Branch: dev/Francisco · Base: 751ae2c
Remote: origin/dev/Francisco

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-19 | feature, task file `docs/tasks/phase-2/F-016-notifications.md` |
| Plan | done | 2026-11-19 | 00/01/03 plans, 4 slices, Q1-Q10 |
| Gate 1: plan approved | done | 2026-11-20 | approved; Q2 changed: worker also for operators |
| Code | done | 2026-11-20 | 4 slices, 4 commits; device checks pending (see Log) |
| Review | done | 2026-11-20 | review.md: 1 blocking (B1), 3 should-fix, 7 nits |
| Docs | pending | | |
| Pushed | pending | | |
| Merged | pending | | |

## Log
Append-only.
- 2026-11-19: merged origin/dev/Diogo (D-011 to D-015, 81 commits) into dev/Francisco first (751ae2c), as the person chose: F-016 consumes D-014's `lastOperatorNearbyAt` (decision 0022, Q9 contract). Conflicts in MainActivity (imports), overview.md and notes.md resolved keeping both sides; build and lint pass. Firestore `categories` still has id `pavement` after D-015 removed DebugSeed. Started F-016 from the task file (target was 2026-11-19).
- 2026-11-19: Plan written (00-plan, 01-coding-plan, 03-docs-plan). Client-only delivery per decision 0012 (Firestore listener while the app is open + 15 min periodic worker, FCM service for console tests), one `lastCheckedAt` per user in SharedPreferences, pure event rule in `model/TicketNotifications`, new ticket fields `lastMessageAt`/`lastMessageAuthorId`, time checks for the 24 h warning and Encerrado. Class sources: P11 FCM + Firebase demo, slides 10/11 WorkManager, slides 09 SharedPreferences. 4 slices; open questions Q1-Q10 (incl. operators app-open only, D-014 contract adjusted to lastCheckedAt). Next decision record: 0024. Awaiting Gate 1.
- 2026-11-20: Sync with main skipped (0 commits behind). Gate 1 approved. Answers: Q1, Q3-Q10 as planned (Q5 time check, Q6 at session start); Q2 changed: operators also notified by the 15 min worker when the app is closed (new `allTicketsOnce`, `isOperator` as worker input). Plans updated.
- 2026-11-20: Code done, slices 1-4 (427da18, fd2c124, bcbc93e, edd308d); assembleDebug and lint pass before each commit (0 errors, same 17 pre-existing warnings). Deviations from the coding plan:
  - `showNotification` checks `POST_NOTIFICATIONS` with `checkSelfPermission` (as the class demo `triggerSystemNotification`) instead of `areNotificationsEnabled()`, which lint does not accept as a permission check for `notify()`.
  - Permission request (Q6) waits for the start-up location answer (`locationAnswered`), because Android shows one permission request at a time, and is asked once per session (`rememberSaveable` flag, reset when the role is not CITIZEN/OPERATOR) so a rotation does not ask again or repeat the denied snackbar. One extra line in the location callback (Diogo's code).
  - Tap: `openTicketId` read from the launch intent only when `savedInstanceState == null` (a rotation must not reopen it); the navigate effect is keyed on (openTicketId, role) and placed after the role redirect so the home is set first.
  - `NotificationsViewModel.watch` skips lists with no full ticket (empty or only public copies): the shared list starts empty after a user change, and checking it would move `lastCheckedAt` past changes not yet shown.
  - STATUS text: "New status: X", or "X: <justification>" when there is one (Q9/Q10 status + justification in one notification).
  - `TicketCheckWorker` also returns success when `auth.currentUser.uid` differs from the scheduled uid (another account signed in).
  - `TicketNotifier` has its own status -> string map (data must not use `ui/components/statusLabelRes`); `PushTokens` uses the literal `"users"` (the constant is private in `FirestoreUsersRepository`); new helper `lastMessageFields` in `FirestoreMapping`; FCM notification id = `(ticketId ?: tag).hashCode()`.
  - Each app start cancels the worker while the session is Loading (ticketsUser null) and schedules it again (UPDATE) once the user is known; harmless.
  - Not touched: `firestore.rules` (draft rules would refuse `fcmTokens`, `lastMessageAt`, `lastMessageAuthorId`; test mode accepts), `google-services.json`, D-014 code, `UploadWorker`. Tell Diogo: `MainActivity` gained the notification permission, open-from-notification, token/worker effect and watch (about 60 lines), and the manifest `singleTop`.
  - To verify on device (not run here): debug `showNotification` tap opens the ticket (cold start and app open); deny permission -> snackbar, no crash, no second ask on rotation; token appears in `users/{uid}.fcmTokens`; console test message (data `ticketId`) in background and foreground, tap opens ticket; two emulators: start work / info request / messages both ways notify the other side only, own message silent, operator notified only of citizen actions; `lastOperatorNearbyAt` set in console notifies once; anonymous gets nothing; app swiped away + App Inspection "run now" on `ticket-check` notifies a status change and (operator) a citizen message; `CLOSING_WARNING_MS = 2 min` on a RESOLVED ticket gives the warning then Encerrado (revert).
- 2026-11-20: Review (80801af..HEAD) written to review.md. Blocking B1: the in-app watch checks the cached first snapshot on a cold start and saves lastCheckedAt = now, so changes made since the last worker run are never notified (fix: one server read before collecting). Should-fix: clear the tray at Log out (S1), MESSAGE dropped by unrelated justifications (S2), worker cancelled and re-run on every start, which can duplicate alerts (S3). 7 nits.

- 2026-11-20: Review fixes (B1, S1-S3; nits N1-N7 skipped as asked). B1: `NotificationsViewModel.watch` first checks one read like the worker (`allTicketsOnce` / `participantTicketsOnce`; `get()` asks the server when online), because the listener's first list is the offline cache and would move `lastCheckedAt` past changes made while the app was closed; a failed read is skipped. S1: new `onLoggedOut()` clears the tray (`NotificationManagerCompat.cancelAll`), called from the `logOut` lambda only (the ViewModel declaration moved above it). S2: MESSAGE dropped only when a history entry with a justification has the same time as `lastMessageAt` (the info-request message, same `now`). S3: `onUserChanged` not called while the session is Loading, so app start no longer cancels and re-enqueues the worker (the earlier "harmless" note is superseded). assembleDebug and lint pass. Still to verify on device: cold start after a change made while closed notifies once; Log out empties the tray.

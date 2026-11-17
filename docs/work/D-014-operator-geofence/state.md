# D-014: Operator 20 m geofence and proximity alert
Branch: dev/Diogo · Base: f1515bf
Remote: origin/dev/Diogo

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-17 | Task file `docs/tasks/phase-2/D-014-operator-geofence.md` |
| Plan | done | 2026-11-17 | 00/01/03 written; 11 open questions for Gate 1 |
| Gate 1: plan approved | done | 2026-11-17 | Approved by Diogo; Q1-Q11 as recommended; slices 1-5 required, 5b-8 only if time remains by 2026-11-19 |
| Code | done | 2026-11-17 | Slices 1-5 done (f48ea59, 5c1714b, 7bcd3ba, c2324a9, 7efaeb4); optional 5b-8 not started; device checks D1-D10 left to Diogo |
| Review | done | 2026-11-17 | Round 1: approve with should-fixes (S1-S3, N1-N5). Round 2: approve; S1, S2, N1-N5 resolved, S3 open (process), 2 doc nits (R2-N1, R2-N2); see `review.md` |
| Docs | | | |
| Pushed | | | |
| Merged | | | |

## Log
- 2026-11-17 Start: `origin/main` had no commits missing from `dev/Diogo`; Diogo chose not to run `/merge`. Commit type `feat`. Untracked `.idea/` files left out of the commit.
- 2026-11-17 Plan: planner checked the code.
  - Found: `needsOperatorGeofence` has no caller. `LocationRepository` has a one-shot fix only. No notification code, channel, FCM service, `functions/`, `proximityAlerts` or `lastOperatorNearbyAt` exists. F-016 has no work folder. The rules are a draft and not deployed (decision 0012 Q5). Decision 0012 Q3 says no Cloud Functions.
  - Plan: recommended approach B, a foreground location Flow with a 20 m Haversine check, an entry edge and a 1 h throttle. It writes only `tickets/{id}.lastOperatorNearbyAt` (Long), as the seam for F-016, with a 5-point consumer contract.
  - Optional: the Geofencing API + background permission (slices 6-7), local delivery without F-016 (slice 8), and a draft rule (5b, needs Diogo's OK).
  - Francisco's files touched (additive): `FirestoreMapping.kt`, `TicketsRepository.kt`, `FirestoreTicketsRepository.kt`, `LocationRepository.kt`; optional `LocationPermissions.kt` and `firestore.rules`.
  - The brief PDF could not be rendered in this session (R6). No code written.
- 2026-11-17 Gate 1: Diogo approved the plan with Q1-Q11 as recommended (foreground location check, `lastOperatorNearbyAt` field, write in `TicketsRepository`, no background permission without slice 6, draft rule as documentation only and no deploy). Slices 1-5 to implement; optional slices 5b-8 only if 1-5 are done by 2026-11-19.
- 2026-11-17 Code slice 1: added `Ticket.lastOperatorNearbyAt` (KDoc, after `statusHistory`), cleared it in `toAnonymousView()` (KDoc list updated), read it in `toTicket()` (`FirestoreMapping.kt`, Francisco, one line), new pure `model/OperatorProximity.kt`. `toTicketDocument()` does not write the field (only the slice 2 update does). Verify: assembleDebug pass; lint 0 errors / 22 warnings (baseline, no new). Commit: see next entry. Hand check (ticket at 40.2093, -8.4247; Haversine worked out by hand with the same formula):

  | Case | Input | Result |
  | --- | --- | --- |
  | 15 m north | lat + 0.000135 (15.01 m) | inside |
  | 25 m north | lat + 0.000225 (25.02 m) | outside |
  | exactly 20 m | lat + 0.00017986 (20.00 m, `<=`) | inside |
  | OPEN / INFO_REQUESTED / RESOLVED | `needsOperatorGeofence` false | not in the set |
  | location (0, 0) | `needsOperatorGeofence` false | not in the set |
  | last = now - 59 min | gap < 1 h | false |
  | last = now - 60 min | gap = 1 h | true |
  | last = now + 1 min | negative gap | false |
  | last = null | never recorded | true |
- 2026-11-17 Code slice 1 commit: f48ea59.
- 2026-11-17 Code slice 2: `TicketsRepository.recordOperatorNearby(ticket, now): Boolean` (after `resolveTicket`, KDoc as planned) and its `FirestoreTicketsRepository` implementation: `canRecordOperatorNearby` guard, then a single-field `update("lastOperatorNearbyAt", now)`, not awaited, failure logged without coordinates. Both files are Francisco's (additive only). Verify: assembleDebug pass; lint 0 errors / 22 warnings. Deviation: the plan says to tell Francisco before committing; the implementer agent cannot contact him, so Diogo must tell him (signature above; why: the write is the seam F-016 reads, decision Q4 puts it in the repository).
- 2026-11-17 Code slice 2 commit: 5c1714b.
- 2026-11-17 Code slice 3: `LocationRepository.locationUpdates(intervalMs = 5_000)` (Francisco's file, additive): written-out FINE check (empty flow without it), `LocationRequest.Builder(PRIORITY_HIGH_ACCURACY, intervalMs)`, `callbackFlow` with a `LocationCallback` that drops fixes with accuracy > 25 m (`MAX_FIX_ACCURACY_METERS` in a private companion), `awaitClose` removes the callback. No coordinates logged. The "D-014 adds updates here" KDoc line in `currentLocation` now points to `locationUpdates`. Verify: assembleDebug pass; lint 0 errors / 22 warnings (no MissingPermission).
- 2026-11-17 Code slice 3 commit: 7bcd3ba.
- 2026-11-17 Code slice 4: new `viewmodel/OperatorProximityViewModel.kt` (`watch(tickets)`: local `inside` set, entry edge `current - inside`, per-device `recentlyRecorded` map with the 1 h interval, then `recordOperatorNearby`) and `OperatorProximityViewModelFactory.kt` (shape of `LocationGateViewModelFactory`). Minor: the `recentlyRecorded` check is written as `val last = ...; if (last != null && now - last < INTERVAL) continue` instead of the plan's `?.let { } == true`, same behaviour, easier to read. Verify: assembleDebug pass; lint 0 errors / 22 warnings.
- 2026-11-17 Code slice 4 commit: c2324a9.
- 2026-11-17 Code slice 5: `CityFixApp` (`MainActivity.kt`) creates `OperatorProximityViewModel`, keeps `hasPrecise` (refreshed by `LifecycleResumeEffect` and in the start-up permission callback) and runs `LaunchedEffect(operator?.id, hasPrecise) { repeatOnLifecycle(STARTED) { watch(ticketsViewModel.tickets) } }`; an operator without precise location gets one `Log.i` (no coordinates), no UI (Q10). New file-level `TAG = "CityFixApp"`. Verify: assembleDebug pass; lint 0 errors / 22 warnings. Note: the plan's "Do not touch" lists `MainActivity`'s permission callback as F-016's area, but slice 5 explicitly asks to refresh `hasPrecise` there; I added only that one line (`hasPrecise = hasPreciseLocationPermission(context)`) and changed nothing else in the callback or the F-012 request.
- 2026-11-17 Code end: required slices 1-5 done; optional 5b, 6, 7, 8 not implemented (not in scope of this run). `firestore.rules`, `storage.rules`, `google-services.json` and Gradle files untouched; nothing pushed.
  - Left for Diogo by hand: device checks D1-D10 (two emulators, `adb emu geo fix`); D5 needs a temporary debug-shortened `PROXIMITY_ALERT_INTERVAL_MS` (do not commit it). D11 depends on F-016 or slice 8.
  - Tell Francisco: new `TicketsRepository.recordOperatorNearby(ticket, now): Boolean` and field `tickets/{id}.lastOperatorNearbyAt` (Long, time only) as the F-016 seam; no rule in the draft `firestore.rules` allows an update that changes only `lastOperatorNearbyAt` (5b not done), so if those rules are deployed as is the server rejects the write (the local cache keeps it; the failure is only logged). Slice 5b, or an equivalent rule, is needed before deploy.
  - Francisco-owned files touched (additive only): `data/FirestoreMapping.kt` (one line in `toTicket()`), `data/TicketsRepository.kt` (new method), `data/FirestoreTicketsRepository.kt` (implementation + import), `data/LocationRepository.kt` (`locationUpdates`, KDoc line, companion). Not touched: `utils/LocationPermissions.kt` (only imported).
- 2026-11-17 Code slice 5 commit: 7efaeb4.
- 2026-11-17 Review: reviewer read `8ce6cef..HEAD` against the plans, the task file, the overview, the draft rules and the conventions. Verify: assembleDebug pass; lint 0 errors / 22 warnings (baseline). Verdict: approve with should-fixes. S1: a projected `publicTickets` ticket can reach `recordOperatorNearby` in the Log out window and skip the throttle (fix: `statusHistory.isEmpty()` guard in the pure rules). S2: `setMinUpdateIntervalMillis(2_000)` (Q11) is missing and was not recorded as a deviation. S3: tell Francisco and confirm the Q9 contract before push; noted that the draft `firestore.rules` rejects the `lastOperatorNearbyAt`-only update (5b not done, no deploy). Nits N1-N5: re-record after rotation with more than 1 h inside, redundant `hasPrecise` line in the permission callback, silent `requestLocationUpdates` failure, `recentlyRecorded` never cleared, long lines. No code edited, nothing committed.
- 2026-11-17 Review round 1 fixes, should-fixes (S1, S2): `canRecordOperatorNearby` returns false for an empty `statusHistory` (a projected ticket, decision 0006), and `insideOperatorGeofence` filters on `statusHistory.isNotEmpty()` too (KDoc updated). `LocationRepository.locationUpdates` adds `.setMinUpdateIntervalMillis(2_000)` (`MIN_UPDATE_INTERVAL_MS` in the companion), as Gate 1 Q11 approved. Verify: assembleDebug pass; lint 0 errors / 22 warnings (baseline). Commit: ec1cc8e. Hand-check row added to the slice 1 table:

  | Case | Input | Result |
  | --- | --- | --- |
  | projected ticket | IN_PROGRESS, empty `statusHistory`, `lastOperatorNearbyAt` null | not in the set; `canRecordOperatorNearby` false |
- 2026-11-17 Review round 1 fixes, nits (N1-N5): N1/N4: `inside` is now a property of `OperatorProximityViewModel`; `watch(operatorId, tickets)` clears `inside` and `recentlyRecorded` only when the operator id changes (CityFixApp passes `operator.id`), so rotation or Home and return no longer counts as an entry. N2: checked that the permission dialog pauses and resumes the activity, so `LifecycleResumeEffect` already refreshes `hasPrecise`; the line in the `RequestMultiplePermissions` callback is removed (callback back to its F-012 form) and the comment above `hasPrecise` says why. N3: `requestLocationUpdates(...).addOnFailureListener` logs `"location updates not started"` (TAG `LocationRepository`, no coordinates) and calls `close()` without the exception. N5: wrapped the long D-014 lines in `Ticket.kt`, `OperatorProximity.kt`, `LocationRepository.kt`, `TicketsRepository.kt` and `TicketPrivacy.kt` (no added line over 100 characters). Verify: assembleDebug pass; lint 0 errors / 22 warnings (baseline). Commit: e807967. Deviations: none from the review's suggested fixes. Francisco's files touched again (additive or comment-only): `LocationRepository.kt`, `TicketsRepository.kt`, `TicketPrivacy.kt` (KDoc wrap). S3 (tell Francisco, draft rules) is still open for Diogo; `firestore.rules`, Gradle and `google-services.json` untouched; nothing pushed.
- 2026-11-17 Review round 2: reviewer read `484824c..HEAD` (ec1cc8e, e807967, 63d5a23). Verify: assembleDebug pass; lint 0 errors / 22 warnings (baseline). Verdict: approve. S1, S2 and N1-N5 resolved. Regression checks passed: `watch(operatorId, tickets)` resets state only on an operator change, with no concurrent collectors; removing `hasPrecise` from the permission callback is safe because the permission dialog pauses and resumes the activity (callback matches F-012 again); the failure listener ends the flow without a crash or a retry loop; Francisco's `LocationRepository.kt` and `TicketsRepository.kt` changes are additive or KDoc-only. S3 still open for Diogo. New nits, docs only: R2-N1, `TicketPrivacy.kt` is Diogo's (created f0272b8, D-003), not Francisco's as the round 1 fixes entry says; R2-N2, the kept `inside` set means an arrival made while the app was in the background is not counted, so record it as a known limit. No code edited, nothing committed.

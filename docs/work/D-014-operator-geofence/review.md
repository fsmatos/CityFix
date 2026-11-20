# D-014 review

Range: `8ce6cef..HEAD` (f48ea59, 5c1714b, 7bcd3ba, c2324a9, 7efaeb4, log 484824c). Reviewed 2026-11-17 against `00-plan.md`, `01-coding-plan.md`, `state.md` (Gate 1: Q1-Q11 as recommended, slices 1-5), the task file, `docs/architecture/overview.md`, the draft `firestore.rules` and `kotlin-conventions`.

**Verdict: approve with should-fixes.** Nothing blocks. Slices 1-5 match the approved plan (approach B, field `lastOperatorNearbyAt`, write in `TicketsRepository`, foreground only, log-only Q10). S1 is a small hardening fix. S2 and S3 are process or recording gaps to close before `/push-task`.

Verify (run by the reviewer): `./gradlew assembleDebug` pass; `./gradlew lint` 0 errors / 22 warnings (same as the baseline, and no warning in a D-014 file).

## What was checked and is fine
- **Privacy.** No operator id or coordinates are written: there is a single-field `update("lastOperatorNearbyAt", now)` and nothing else. `toAnonymousView()` clears the field. `toPublicTicketDocument()` copies 7 explicit keys, so the field cannot reach `publicTickets`. `toPublicTicket()` never sets it. Citizens get it only on full tickets they participate in, and the draft read rule limits those to participants. No log line has coordinates (`"nearby not saved"`, `"No precise location..."`). No UI shows the field.
- **Writes that could wipe the field.** Only creation uses `set(toTicketDocument())`. Status changes, joins and uploads use `update` with explicit keys, so a later write does not clear `lastOperatorNearbyAt`.
- **Rules (20 m / 1 h).** `<= 20.0` Haversine. `now - last >= 1 h`, and a negative gap gives false (same convention as `canReopen`). `needsOperatorGeofence` excludes OPEN, INFO_REQUESTED, RESOLVED, CLOSED (effective) and (0, 0). The hand-check table in `state.md` matches the code.
- **Entry edge.** `entered = current - inside`, and the set is updated before the writes. A ticket rejected by the stored throttle still joins `inside`, so staying inside does not write once the hour ends (Q6). `recentlyRecorded` covers the gap before the latency-compensated snapshot arrives.
- **Lifecycle.** `callbackFlow` + `awaitClose { removeLocationUpdates }`. The flow is collected under `repeatOnLifecycle(STARTED)` in a `LaunchedEffect(operator?.id, hasPrecise)`, so Home, Log out (operator becomes null) and a change of operator all cancel it. `LocalLifecycleOwner` at `CityFixApp` level is the activity. No `GlobalScope` and no leaked callback. The FINE check runs on every `watch` start, because `locationUpdates()` is called again on each STARTED. Revoking a permission in Settings kills the process, and a grant is picked up by `LifecycleResumeEffect`.
- **Offline.** The write is not awaited and failures are only logged, as in `sendMessage`.
- **Francisco's files.** All changes are additive: one mapping line, an interface method and its implementation with an import, `locationUpdates` plus a companion and a reworded KDoc sentence that the plan asked for. `firestore.rules`, the manifest, Gradle files and keys are untouched.
- **MVVM, i18n, accessibility, orientation.** No new UI and no new user-visible strings, so there is no EN/PT, content-description or landscape impact. The ViewModel holds the rotation-surviving state, and the composable only reads the permission and starts the watch, which is what Gate 1 approved. `LocationRepository` has no interface, as the plan notes.
- **Recorded deviations.** These are logged: the `recentlyRecorded` style change, the one line added to the permission callback, and "tell Francisco" still pending.

## Blocking
None.

## Should-fix

### S1. A projected (anonymous) ticket can reach the write and skip the throttle
`logOut` calls `ticketsViewModel.setUser(null)` synchronously, then `sessionViewModel.signOut()`. The session, and so the cancellation of the watch effect, only changes when the auth listener fires. In that window `tickets.value` can already hold the `publicTickets` projection, because the cache snapshot arrives quickly. If a fix lands in that window, `watch` evaluates projected tickets:
- `currentStatus` IN_PROGRESS with an empty history gives an `effectiveStatus` of IN_PROGRESS, and the location is not (0, 0), so `needsOperatorGeofence` is true;
- the location is rounded to 2 decimals (about 1 km), so the 20 m test runs against a grid point;
- `lastOperatorNearbyAt` is null in the projection, so `canRecordOperatorNearby` returns true and the hourly throttle is skipped. The write would go out after sign-out.

The window is milliseconds and the operator would also need to stand within 20 m of a rounded grid point, so this is unlikely. But it breaks the documented rule in `TicketLifecycle.kt` ("Never call these on a projected ticket"), and the fix is one line.
**Fix:** guard the pure rule, which also protects the repository's re-check, in `model/OperatorProximity.kt`:
```kotlin
fun Ticket.canRecordOperatorNearby(now: Long): Boolean {
    if (statusHistory.isEmpty()) return false // projected ticket (decision 0006): never written
    if (!needsOperatorGeofence(now)) return false
    ...
}
```
Use the same check in `insideOperatorGeofence` (`filter { it.statusHistory.isNotEmpty() && ... }`), then update the KDoc and add a row to the hand-check table.

### S2. Unrecorded deviation: `setMinUpdateIntervalMillis(2_000)` is missing
Q11 was approved as "5 s (minimum 2 s)", and the plan's `locationUpdates` builds the request with `.setMinUpdateIntervalMillis(2_000)`. The code calls `LocationRequest.Builder(PRIORITY_HIGH_ACCURACY, intervalMs).build()`, which defaults the minimum to the interval (5 s), and the Log does not mention it. The effect is small: other apps' fixes are not batched in faster.
**Fix:** either add `.setMinUpdateIntervalMillis(2_000)` to the builder in `LocationRepository.locationUpdates`, or add a Log line in `state.md` that records the omission and the reason. CLAUDE.md requires deviations to be recorded.

### S3. Open hand-offs before push and merge (process; do not fix in code here)
- **Tell Francisco.** The slice 2 "tell Francisco before committing" step was not done, and the Q9 contract (points 1-5) is not yet confirmed. Diogo should send him the `recordOperatorNearby(ticket, now): Boolean` signature, the field and the contract, then record his answer in `state.md` before `/push-task`.
- **Draft rules (noted, not to fix in D-014 code).** The draft `firestore.rules` would reject this write. No `allow update` on `tickets/{ticketId}` accepts `changedKeys().hasOnly(['lastOperatorNearbyAt'])`: the status rule needs `currentStatus` to change and `historyAppended()`, the join rule is for citizens only, and the upload rule allows `attachments` only. Test mode accepts the write today. If the rules are ever deployed without slice 5b (Q5 needs Diogo's explicit OK), the write fails on the server, and only `"nearby not saved"` is logged. Keep this in the F-016 hand-off and in the report.

## Nit

### N1. Rotation (or Home and return) after more than 1 h inside writes again
`inside` is local to `watch`, so it resets whenever the effect restarts. Rotation recreates the activity, the first fix counts as an entry, and `recentlyRecorded` guards only for 1 h. An operator who stays at a site for more than an hour and rotates the phone produces a second record, while acceptance check 5 says "nothing new until the operator leaves and re-enters". Home and return behaves the same way, which the plan accepts (like `INITIAL_TRIGGER_ENTER`).
**Fix:** either note it in `state.md` and the report as a known limit, or move `inside` into the ViewModel as a property that is cleared only when the operator id changes (pass the id to `watch`), so that a restart compares against the last known set.

### N2. `hasPrecise` is refreshed twice after the start-up request
The permission dialog pauses and resumes the activity, so `LifecycleResumeEffect` already refreshes `hasPrecise`. The extra line in the `RequestMultiplePermissions` callback, which the plan lists as F-016's area, is redundant.
**Fix:** drop the line from the callback, so F-016's area stays untouched and there is one fewer merge-conflict point. Alternatively, keep it and add a comment saying it is belt-and-braces. Either way, update the Log entry.

### N3. A `requestLocationUpdates` failure is silent
The `Task` returned by `client.requestLocationUpdates(...)` has no failure listener, for example when Play services are missing or outdated on an emulator. The watch then waits forever with no trace.
**Fix:** `client.requestLocationUpdates(request, callback, Looper.getMainLooper()).addOnFailureListener { Log.w(TAG, "location updates not started", it); close() }`, with a `TAG` and no coordinates. Use `close()` without the exception, so the collector does not crash.

### N4. `recentlyRecorded` is never cleared or pruned
The map is scoped to the activity's ViewModel, so it outlives Log out and a change of operator, and it only grows. This is harmless (the throttle is shared anyway), but it is hard to explain at the defence.
**Fix:** clear it when `watch` starts for a different operator id, or drop entries older than `PROXIMITY_ALERT_INTERVAL_MS` before the check. Alternatively, add a KDoc line saying it is intentionally kept across sessions.

### N5. Long lines
The new KDoc in `model/Ticket.kt` is 130 characters, and `LocationRepository.locationUpdates` has a 106-character `val fine = ...` line. Several other added lines run to 101-104 characters.
**Fix:** wrap them to about 100 characters, as in the surrounding code.

## Round 2

Range: `484824c..HEAD` (ec1cc8e S1/S2, e807967 N1-N5, 63d5a23 docs). Reviewed 2026-11-17.

**Verdict: approve.** All round 1 code findings are resolved and no regression was found. S3 stays open as a process step for Diogo (tell Francisco, confirm the Q9 contract, draft rules) before `/push-task`. Two new nits, both documentation only.

Verify (run by the reviewer): `./gradlew assembleDebug` pass; `./gradlew lint` 0 errors / 22 warnings (baseline; no warning in a D-014 file).

### Round 1 findings
| Finding | Status | Evidence |
| --- | --- | --- |
| S1 projected ticket | resolved | `canRecordOperatorNearby` returns false on an empty `statusHistory`; `insideOperatorGeofence` filters on `statusHistory.isNotEmpty()`. A projection now never enters `inside` nor reaches the repository. Hand-check row added in `state.md`. |
| S2 min interval | resolved | `.setMinUpdateIntervalMillis(MIN_UPDATE_INTERVAL_MS)` (2 000 ms) in `locationUpdates`, with a Q11 comment. |
| S3 hand-offs | open (process) | Not a code fix. Still to do by Diogo before push; recorded in the Log. |
| N1 rotation re-entry | resolved | `inside` is a ViewModel property, cleared only when `operatorId` changes. See R2-N2 for the trade-off it introduces. |
| N2 duplicate `hasPrecise` | resolved | Line removed; `git diff f1515bf..HEAD` shows the `RequestMultiplePermissions` callback is back to its F-012 form. Comment above `hasPrecise` explains why the resume effect is enough. |
| N3 silent failure | resolved | `addOnFailureListener { Log.w(TAG, ..., it); close() }`, no coordinates. |
| N4 `recentlyRecorded` growth | resolved | Cleared with `inside` when the operator id changes. |
| N5 long lines | resolved | The D-014 lines are wrapped to 100 characters or less. |

### Regression checks
- **`watch(operatorId, tickets)` and state reset.** `CityFixApp` passes `operator.id` (smart-cast inside `operator != null`). `LaunchedEffect(operator?.id, hasPrecise)` cancels the old coroutine before it starts the new one, and `repeatOnLifecycle` runs one block at a time, all on the main dispatcher. So `inside` and `recentlyRecorded` are never changed by two collectors at once. Log out, then log in as the same operator, keeps both sets. That is safe: the first snapshot after `setUser` is empty or a projection, which S1 now filters out, so `inside` is cleared on the next fix and the real entry is still caught, with the throttle kept. A different operator starts with empty sets, as intended.
- **Removing `hasPrecise` from the permission callback.** The runtime permission dialog is a separate, translucent activity, so `MainActivity` gets ON_PAUSE and then ON_RESUME. `LifecycleResumeEffect` then reads `checkSelfPermission` again, and it is already up to date. When no dialog is shown (already granted, or "don't ask again"), the permission did not change, so the value from `remember` is still correct. No regression.
- **The failure listener closes the flow.** `close()` ends the `callbackFlow`, `awaitClose` removes the callback (harmless), `collect` returns, and `watch` returns normally. `repeatOnLifecycle` runs it again only on the next STARTED, so there is no busy retry loop. If the flow is already cancelled, `close()` returns false and does nothing. No crash path.
- **Francisco's files.** `LocationRepository.kt` (F-012) gets an additive builder call, a failure listener, a `TAG` and a constant in the private companion, and a KDoc rewrap. `TicketsRepository.kt` (F-008) gets a KDoc rewrap only. No existing behaviour changed in either.
- **Ownership of `TicketPrivacy.kt`.** `git log --diff-filter=A` shows Diogo created it (f0272b8, D-003). The plan (`00-plan.md`, "Changed (Diogo)") and `overview.md` ("Diogo's files changed by F-011/F-013") also list it as Diogo's. Francisco only made KDoc edits to it (090fba9, 3b86ec1). See R2-N1.
- **Privacy, i18n, accessibility, orientation.** Still no new UI, no new strings and no coordinates in any log. The rotation behaviour has improved (N1).

### Blocking
None.

### Should-fix
None new. S3 from round 1 still applies (process).

### Nit

#### R2-N1. `state.md` lists `TicketPrivacy.kt` as Francisco's file
The "Review round 1 fixes, nits" Log entry says "Francisco's files touched again (additive or comment-only): `LocationRepository.kt`, `TicketsRepository.kt`, `TicketPrivacy.kt`". `TicketPrivacy.kt` is Diogo's (D-003, f0272b8), which `00-plan.md` and `overview.md` confirm. This matters when Diogo tells Francisco which of his files changed (S3).
**Fix:** in the docs phase, or with a new Log line (the Log is append-only), record that `TicketPrivacy.kt` is Diogo's and that only `LocationRepository.kt` and `TicketsRepository.kt` are Francisco's in round 1.

#### R2-N2. The kept `inside` set can miss an entry made while the app was in the background
`inside` now survives Home and return. Take an operator at ticket A who presses Home, leaves, comes back to A later and reopens the app. The first fix still has A in `inside`, so that arrival is not recorded. In round 1 it would have been recorded. This follows from the N1 fix and fits Gate 1 Q1 (foreground only), and it is easy to defend, but nothing records it. The same applies to the KDoc on `inside`, which lists Home and return as a benefit only.
**Fix:** state this as a known limit in the docs phase (overview / report, "foreground only" section), for example: "An entry is counted only if the operator was seen outside the circle while the app was in the foreground." Optionally add that sentence to the `inside` KDoc. No code change is needed.

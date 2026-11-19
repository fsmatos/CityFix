# D-014: Operator 20 m geofence and proximity alert
Type: feature · Task file: docs/tasks/phase-2/D-014-operator-geofence.md
Labels: -

## Problem
- Brief I.3 Obs and II.3 (from the task file): when an operator comes within 20 m of an active ticket that is IN_PROGRESS, its authors (creator and followers) are alerted that the council team is in the area. Brief III lists the proximity alert as a notification; brief V lists background location among the permissions. The brief PDF could not be opened in this session (no PDF renderer), so the wording is from the task file. That is risk R6.
- What exists today (checked in the code on 2026-11-17):
  - `model/TicketLifecycle.kt`: `Ticket.needsOperatorGeofence(now) = effectiveStatus(now) == IN_PROGRESS && location != Coordinates()`. Nothing calls it yet (D-013).
  - `TicketsViewModel.tickets`: for an operator this is already `allTickets()`, every full ticket kept live by one listener (F-011, D-013). Start work writes IN_PROGRESS through `commitStatusChange`. INFO_REQUESTED, RESOLVED and CLOSED drop out of `needsOperatorGeofence` by construction (D-013 handover).
  - `data/LocationRepository.kt` (F-012, Francisco): only a one-shot `currentLocation()` (`getCurrentLocation(PRIORITY_HIGH_ACCURACY)` + `await()`). It has no updates yet; its KDoc says "D-014 adds updates here". `play-services-location` 21.3.0 is in the catalog and includes `GeofencingClient`.
  - `utils/Distance.kt`: `haversineMeters(...)`. Its KDoc already names the 20 m geofence.
  - `utils/LocationPermissions.kt` (F-012): FINE + COARSE, `hasPreciseLocationPermission`, `openAppSettings`. `MainActivity` asks for FINE + COARSE at start for every user, with no rationale (F-012, the person's choice).
  - `AndroidManifest.xml`: FINE, COARSE and INTERNET only. There is **no** `ACCESS_BACKGROUND_LOCATION`, `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE*`, receiver or service.
  - Notifications: **none.** There is no `utils/Notifications.kt`, no notification channel, no `FirebaseMessagingService` and no `NotificationWorker`. `firebase-messaging` is a dependency, `CityFixApplication.messaging` is a lazy property that is never used, and `users/{uid}.fcmTokens` exists (empty list at registration, and the owner may write it in the draft rules). F-016 (Francisco) has no work folder.
  - Server: no `functions/` folder. Decision 0012 Q3: **no Cloud Functions**. F-016 delivers by Firestore listeners on `participantIds` plus a WorkManager periodic check.
  - Firestore: there is no `proximityAlerts` collection and no `lastOperatorNearbyAt` field anywhere (code, mapping, rules, docs).
  - Rules: `firestore.rules` is a draft and is **not deployed**. The database stays in test mode through the defence (decision 0012 Q5, amended 2026-11-13). The draft has no rule that lets an operator write a non-status field on `tickets/{id}`.
- Gap: nothing watches the operator's position, nothing records "an operator was near this ticket", and nothing tells the citizens.

## Outcome
- While a signed-in operator has the app open (foreground) with precise location granted, the app gets location updates about every 5 s. For each IN_PROGRESS ticket with a real location (`needsOperatorGeofence(now)`) it checks the distance to the operator.
- When the operator **enters** the 20 m circle of such a ticket (outside -> inside) and no alert was recorded for that ticket in the last hour, the app writes **one field** on that ticket: `tickets/{id}.lastOperatorNearbyAt = now` (epoch ms, Long, the same as every other time in the model). The write has no operator coordinates and no operator id. It goes through the offline cache and is not awaited.
- The throttle is shared by all operators: it reads the stored field, so a second operator (or a second entry by the same operator) within the hour writes nothing. Repeated entries within the hour give one record.
- **Seam for F-016:** the field arrives on the citizen's device through the `participantTickets(uid)` listener that F-016 already plans to use. F-016 turns an increased, fresh value into the local "council team nearby" notification (contract in "Architecture impact"). D-014 itself shows nothing on the citizen device unless optional slice 8 is approved (Q8).
- Operator coordinates are never stored, logged, sent or shown. Citizens and anonymous users receive only a time on a document they already read. Anonymous users do not even get that, because `publicTickets` is untouched.
- Optional (Gate 1, Q1/Q2): the Android Geofencing API on top, for alerts while the app is in the background, with `ACCESS_BACKGROUND_LOCATION` and a rationale (slices 6-7).

## Acceptance checks
- [ ] **Entry within 20 m records one alert:** operator emulator signed in, app open, a PUBLIC or PRIVATE ticket IN_PROGRESS. Move the operator from about 100 m to about 15 m with `adb emu geo fix` or an Extended controls route. Within about 10 s, the console shows `tickets/{id}.lastOperatorNearbyAt` set to the current time. Nothing else on that ticket changes (status, history, `publicTickets`, `ticketLocations`, `stats`).
- [ ] **The author's device shows the alert.** This needs F-016's consumer (or optional slice 8): the creator's and a follower's devices show the proximity notification. Without either, this check stays open, and D-014 is verified by the console check above plus the F-016 contract (Q8).
- [ ] **No record for OPEN, INFO_REQUESTED, RESOLVED or CLOSED tickets, or beyond 20 m:** the same moves near tickets in each of those statuses write nothing. Stopping at about 25 m from an IN_PROGRESS ticket writes nothing.
- [ ] **Throttle:** in, out (to about 60 m) and in again three times within an hour write the field once (its value stays the first time). A second operator device entering within the hour writes nothing. With the throttle constant shortened in a debug build (as F-016 plans for the 24 h warning), a re-entry after the interval writes a new time.
- [ ] **Staying inside does not repeat:** standing at 10 m for longer than the interval (debug-shortened) writes nothing new until the operator leaves and re-enters.
- [ ] **No operator coordinates anywhere:** after the checks, the console has no new document or field with coordinates. `lastOperatorNearbyAt` is a number. `publicTickets/{id}` keeps its fields (`hasOnly` list unchanged). Logcat has no operator coordinates (search for the test latitude).
- [ ] **Only operators, only in the foreground:** a citizen and an anonymous user moving near the ticket write nothing. Pressing Home on the operator stops updates (the location indicator in the status bar goes off) and returning resumes them. Log out stops them.
- [ ] **Permission states:** operator with "Approximate" or denied location: no crash, no writes (logged once). Granting precise location later, then returning to the app, starts the watch.
- [ ] **Offline:** operator in airplane mode enters the circle: the field is set in the local cache at once (the operator's own ticket list sees it, so no second write) and reaches the server when the network returns.
- [ ] Rotation of the operator's screen while inside: no second write (the watcher lives in a ViewModel).
- [ ] `./gradlew assembleDebug` and `./gradlew lint` pass after each slice, with no new warnings beyond the baseline recorded in `state.md` at slice 1 (D-013: 0 errors, 22 warnings).

## Approach

### Q1: Geofencing API or a foreground location Flow (decide at Gate 1)
| | A. Geofencing API (`GeofencingClient` + `BroadcastReceiver`) | B. Foreground location Flow + distance check (recommended) |
| --- | --- | --- |
| Works with the app closed / screen off | Yes (OS-managed), up to 100 geofences per app | No, only while the app is in the foreground |
| 20 m accuracy | Poor. Google recommends a radius of at least 100-150 m, and a 20 m circle can miss or fire late | Exact Haversine on each fix, with a filter for inaccurate fixes |
| Latency | ENTER can take about 2-3 min (the OS batches fixes) | One update interval (about 5 s) |
| Permissions | `ACCESS_BACKGROUND_LOCATION` ("Allow all the time", Android 10+, its own settings screen on 11+), or the geofences never fire | FINE only (already requested at start) |
| Lifecycle code | Register/remove on every list change and on logout; geofences are lost on reboot or app update (needs a BOOT receiver or a re-register on start); the receiver must use `goAsync()` and read the ticket from the cache before it writes | One `callbackFlow` collected under `repeatOnLifecycle(STARTED)` |
| Emulator testing | Needs Google Play location services; routes work but with a delay | `adb emu geo fix` or routes, immediate |
| Brief V "background location" | Covered | Not covered: the report states it as a limitation and an improvement |
| Defence | More moving parts (PendingIntent, receiver, re-registration) | The P10 `requestLocationUpdates` pattern, short |

**Recommendation: B for the core** (slices 1-5). It meets the 20 m rule exactly, is quick to test and explain, and fits the target of 2026-11-20. Optional slices 6-7 add A on top (the hybrid) if Diogo wants brief V's background permission covered and time remains by 2026-11-19. Both paths call the same pure rule and the same repository write, so the throttle (the stored field) removes duplicates between them. A rejected third option is a location foreground service, as in P10's `ProximityService`. It needs a channel, a persistent notification and `FOREGROUND_SERVICE_LOCATION`, overlaps F-016's channel work and still does not need the background permission. It goes in the report as an improvement.

### Q3: the alert record: a field on the ticket (recommended) or a `proximityAlerts` collection
- **Field `tickets/{id}.lastOperatorNearbyAt: Long`** (epoch ms, the operator's device time, like `statusHistory.dateTime`):
  - the citizens' `participantTickets` listener (F-011) already delivers it, so F-016 needs no new listener, query, index or rule for reading;
  - the throttle is a read of the same field from the operator's shared list, which also works offline thanks to latency compensation;
  - `publicTickets` is untouched, so anonymous users never see it;
  - one value, no history, which is all the brief needs.
- Alternative: a `proximityAlerts/{autoId}` collection `{ticketId, at}`. F-016 would need a second listener with a `whereIn(ticketId, ...)` (at most 30 values) or a denormalised `participantIds`, plus new read rules. The throttle would need a query. That is more code for nothing the brief asks for.
- A server timestamp is rejected: before the server acknowledges the write it is null in the cache (offline), which would break the local throttle. Clock skew between devices is accepted (see Failure modes).

### Pure rules (`model/OperatorProximity.kt`, new, Diogo)
- `const val OPERATOR_GEOFENCE_RADIUS_METERS = 20.0` and `const val PROXIMITY_ALERT_INTERVAL_MS = 60 * 60 * 1000L`.
- `fun Ticket.isWithinOperatorGeofence(at: Coordinates): Boolean` = `haversineMeters(...) <= 20.0`. This is the same `utils` helper that `TicketsList.findNearby` already uses from `model/`.
- `fun Ticket.canRecordOperatorNearby(now: Long): Boolean` = `needsOperatorGeofence(now) && (lastOperatorNearbyAt == null || now - lastOperatorNearbyAt >= PROXIMITY_ALERT_INTERVAL_MS)`. A negative gap (this device's clock behind the writer's) counts as inside the hour, so nothing is written. This is the same convention as `canReopen`.
- `fun List<Ticket>.insideOperatorGeofence(at: Coordinates, now: Long): Set<String>`: the ids of the tickets that `needsOperatorGeofence(now)` and `isWithinOperatorGeofence(at)`.

### Watcher (`viewmodel/OperatorProximityViewModel.kt`, new, Diogo)
- Built with `app.locationRepository` and `app.ticketsRepository` (factory, as for the other ViewModels). It has no UiState and nothing to draw: it is a ViewModel so that it survives rotation (no second write) and so that the composable never touches location (`.claude/rules/kotlin.md`).
- `suspend fun watch(tickets: StateFlow<List<Ticket>>)`: collects `locationRepository.locationUpdates()`. For each fix:
  1. `now = System.currentTimeMillis()`; `inside = tickets.value.insideOperatorGeofence(fix, now)`;
  2. `entered = inside - previousInside` (**edge**, Q6), then `previousInside = inside`;
  3. for each entered id, take the ticket from the list. If `canRecordOperatorNearby(now)` and the id is not in `recentlyRecorded` (an in-memory map id -> time, so a second fix arriving before the cache snapshot cannot write twice), call `ticketsRepository.recordOperatorNearby(ticket, now)` and remember it.
- `previousInside` is cleared when `watch` ends (app stopped). On return, the first fix counts as an entry, like the Geofencing API's `INITIAL_TRIGGER_ENTER`. The stored throttle still applies.
- Started from `CityFixApp` (`MainActivity.kt`) only for an `Operator` session with `hasPreciseLocationPermission`, inside `LaunchedEffect(user id, permission) { lifecycleOwner.repeatOnLifecycle(STARTED) { vm.watch(ticketsViewModel.tickets) } }`. Leaving the foreground cancels the collection (and with it `removeLocationUpdates`), and coming back restarts it. Logout changes the key and cancels it. The precise-permission check is re-run on `ON_RESUME`, so granting it in Settings starts the watch without a restart.

### Location updates (`LocationRepository.locationUpdates()`, Francisco's file, additive)
- `fun locationUpdates(intervalMs: Long = 5_000): Flow<Coordinates>` is a `callbackFlow` over `requestLocationUpdates(LocationRequest.Builder(PRIORITY_HIGH_ACCURACY, intervalMs).setMinUpdateIntervalMillis(2_000).build(), callback, Looper.getMainLooper())`, with `awaitClose { removeLocationUpdates(callback) }`. This is the P10 `ProximityService` pattern as a Flow, beyond class.
- It needs FINE: when FINE is missing it returns an empty flow (written-out `checkSelfPermission`, as `currentLocation` does for lint's `MissingPermission`).
- Fixes with `hasAccuracy() && accuracy > 25 m` are dropped (Q7), so a poor fix cannot place the operator inside a 20 m circle. Coordinates are never logged.

### The write (`TicketsRepository.recordOperatorNearby`, Francisco's files, additive; Q4)
- `fun recordOperatorNearby(ticket: Ticket, now: Long): Boolean`: re-checks `ticket.canRecordOperatorNearby(now)` (false -> nothing). Then `firestore.collection(TICKETS).document(ticket.id).update("lastOperatorNearbyAt", now)`, not awaited, with failures only logged, as `sendMessage` does. It returns true once queued.
- `FirestoreMapping.toTicket()` reads `lastOperatorNearbyAt = getLong("lastOperatorNearbyAt")` (absent -> null). `toTicketDocument`, `toStatusFields` and `toPublicTicketDocument` are unchanged, so the field is never written at creation, never copied to `publicTickets`, and a status change does not clear it.
- `Ticket` gains `val lastOperatorNearbyAt: Long? = null` (KDoc: time only, never a location; not in the anonymous projection).
- `model/TicketPrivacy.kt` `toAnonymousView()` is a `copy(...)`, so it would keep the new field. Slice 1 adds `lastOperatorNearbyAt = null` there. `toPublicTicket()` (the mapping from `publicTickets`) never sets it.

### Rules (Q5, needs Diogo's OK; drafted only, never deployed)
- If approved, slice 5b adds one `allow update` to the **draft** `tickets/{id}` block: `isOperator()`; `resource.data.currentStatus == 'IN_PROGRESS'`; `changedKeys().hasOnly(['lastOperatorNearbyAt'])`; the value `is int` and `<= request.time.toMillis() + 300000`; and, when the old value exists, `>= old + 3600000`. That enforces the throttle on the server too. It is documentation only (decision 0012 Q5: test mode, not deployed). No deploy.

## Architecture impact
- New: `model/OperatorProximity.kt`, `viewmodel/OperatorProximityViewModel.kt`, `viewmodel/OperatorProximityViewModelFactory.kt`. Optional: `utils/GeofenceManager.kt`, `data/GeofenceBroadcastReceiver.kt` (slices 6-7) and `utils/ProximityNotification.kt` (slice 8).
- Changed (Diogo): `model/Ticket.kt` (one field), `model/TicketPrivacy.kt` (clear it in the projection), `MainActivity.kt` (start the watcher).
- Changed (**Francisco's files, additive**; tell him before slice 2): `data/FirestoreMapping.kt` (one line in `toTicket`), `data/TicketsRepository.kt` + `data/FirestoreTicketsRepository.kt` (`recordOperatorNearby`), `data/LocationRepository.kt` (`locationUpdates`). Optional: `firestore.rules` (draft, Q5), `utils/LocationPermissions.kt` (background helper, slice 6).
- Firestore schema: decision 0010 gains `tickets/{id}.lastOperatorNearbyAt` (Long, optional) through a new decision 0022.
- **Contract for F-016 (what it must consume):**
  1. Source: the citizen's `participantTickets(uid)` snapshots (the app listener and the periodic worker). Operators and anonymous users never get this notification.
  2. Trigger: `lastOperatorNearbyAt` is present, greater than the last value this device notified for that ticket (stored locally per ticket id), and fresh: `now - value < PROXIMITY_ALERT_INTERVAL_MS`, so a write queued offline for hours, or an old value seen on first install, is recorded as seen but not notified.
  3. Content: "The council team is near your report" plus the category name. No operator name, no coordinates, no distance. Tap -> `TicketDetailRoute(id)`. Channel: F-016's "proximity" channel.
  4. A change in `lastOperatorNearbyAt` alone is **not** a status change. F-016's "work started" detection must keep reading `currentStatus` and history only.
  5. The "work started" notification on IN_PROGRESS (task file, last bullet) is F-016's status-change notification. D-014 adds nothing for it.

## Failure modes
| Situation | Behaviour |
| --- | --- |
| Operator offline | Field written to the cache at once; the shared list sees it (no second write); sent when online. F-016's freshness rule drops it if it arrives more than 1 h late |
| Two operators enter within seconds, before either write syncs | Both write (last write wins, times seconds apart); the citizen may see two alerts unless F-016 also keeps its own per-ticket throttle (contract point 2 covers it: a value under 1 h after the last notified one is skipped) |
| Device clocks differ | Throttle and freshness can be off by the skew; a writer whose clock is far ahead blocks others for longer. Accepted, noted in the report |
| GPS jitter at the boundary | Edge plus stored throttle: at most one record per hour |
| Poor fix (accuracy above 25 m) | Ignored |
| Approximate or denied location | `locationUpdates()` empty; no watch; one log line |
| Location services off | No fixes; nothing happens |
| Ticket goes INFO_REQUESTED / RESOLVED while the operator is inside | Leaves the set; nothing written; coming back to IN_PROGRESS while inside counts as an entry on the next fix |
| Ticket at (0, 0) | `needsOperatorGeofence` false |
| App in background or closed | No watch (approach B). Covered only by optional slices 6-7 |
| Rules deployed one day without slice 5b | The update is refused; logged only |
| Rotation / process death | ViewModel survives rotation; after process death the in-memory set is empty, and the stored throttle prevents a repeat |

## Risks and open questions
- **R1, no delivery without F-016.** Acceptance check 1 depends on Francisco's consumer. Mitigation: the contract above, agreed with him before 11-19, or optional slice 8 (Q8).
- **R2, brief V background location.** Approach B does not use it. Mitigation: optional slices 6-7, or a report note (Q1/Q2).
- **R3, Francisco's files.** Four additive edits (mapping, repository interface and impl, `LocationRepository`). Possible merge conflicts with F-016, which will likely touch `MainActivity`, the repository and the manifest at the same time. Mitigation: tell him the exact signatures (Interfaces in `01-coding-plan.md`) before slice 2.
- **R4, emulator location.** Mock fixes may carry accuracy 0 or none; the filter accepts both. The Maps key may still be the placeholder (grey map), which does not affect this task.
- **R5, deadline.** Core slices 1-5 are small; optional slices only if time remains by 11-19.
- **R6, brief not reread.** The PDF could not be rendered here; Diogo should check I.3 Obs, II.3, III and V wording at Gate 1.

Open questions (recommended answer in bold):
1. **Q1, mechanism:** Geofencing API (A), foreground Flow (B) or hybrid? **B now; A as optional slices 6-7 only if time remains by 11-19.** The trade-offs are in the table above and go in the report.
2. **Q2, `ACCESS_BACKGROUND_LOCATION`:** request it? **Not with B alone** (an unused permission, and "Allow all the time" is a hard sell at the defence). Request it only with slice 6, after foreground location, with a rationale dialog for operators only.
3. **Q3, alert record:** field or `proximityAlerts` collection? **Field `tickets/{id}.lastOperatorNearbyAt` (Long epoch ms), time only.**
4. **Q4, where the write lives:** additive method on Francisco's `TicketsRepository` (as D-013 did), or a Diogo-owned `ProximityRepository` (the D-012 pattern)? **Francisco's `TicketsRepository`** (it is the `tickets/{id}` document and the mapping is there anyway); tell him first.
5. **Q5, draft rules:** add the operator-only, throttled `allow update` for `lastOperatorNearbyAt` to the draft `firestore.rules` (not deployed)? **Yes, as draft only (slice 5b); no deploy.** It needs your explicit OK.
6. **Q6, entry edge:** record only on outside -> inside plus the hourly throttle, or whenever inside plus the throttle? **Edge plus throttle** (an operator working on site for 3 h gives one alert, not three).
7. **Q7, accuracy filter:** drop fixes with accuracy above 25 m? **Yes.**
8. **Q8, local delivery without F-016:** add optional slice 8 (citizen-side consumer with a "proximity" channel and `POST_NOTIFICATIONS` request, written to the contract so F-016 can absorb it)? **Ask Francisco on 11-18. If F-016's listener will not exist by 11-19, do slice 8 using his planned channel id; otherwise skip it and leave acceptance check 1 to F-016.**
9. **Q9, contract with Francisco:** does F-016 accept points 1-5 (freshness 1 h, per-ticket last notified value, no operator data, not a status change)? **Yes; confirm with him and record it in `state.md`.**
10. **Q10, operator without precise location:** show a note on the operator panel, or log only? **Log only now; a report note.** A panel line can come later.
11. **Q11, update interval:** 5 s (minimum 2 s)? **Yes**, about 4 fixes while walking through a 40 m circle at 1.5 m/s.

## Out of scope
- The notification itself, channels, `POST_NOTIFICATIONS`, FCM and the "work started" notification (F-016), except optional slice 8.
- A foreground location service and Cloud Functions (decision 0012).
- Showing operators or their positions on any map; a history of proximity visits; an operator setting to turn the watch off.
- Deploying rules.

## Slices
1. `model`: `Ticket.lastOperatorNearbyAt` (cleared in `toAnonymousView`) + `model/OperatorProximity.kt` (constants, `isWithinOperatorGeofence`, `canRecordOperatorNearby`, `insideOperatorGeofence`) + one mapping line in `toTicket`. Hand check table in `state.md`.
2. `data`: `recordOperatorNearby` in `TicketsRepository` / `FirestoreTicketsRepository` (Francisco's files).
3. `data`: `LocationRepository.locationUpdates()` (Francisco's file).
4. `viewmodel`: `OperatorProximityViewModel` + factory (edge, throttle, in-memory guard).
5. `MainActivity`: start the watcher for operators with precise location under `repeatOnLifecycle(STARTED)`; re-check the permission on resume.
5b. (Q5 approved) draft rule for `lastOperatorNearbyAt` in `firestore.rules`; no deploy.
6. (Optional, Q1/Q2) `ACCESS_BACKGROUND_LOCATION` in the manifest, operator rationale dialog + request after foreground, strings EN/PT.
7. (Optional, Q1) `utils/GeofenceManager.kt` (register 20 m geofences for up to 100 `needsOperatorGeofence` tickets, re-register on list change, remove on logout) + `data/GeofenceBroadcastReceiver.kt` (ENTER -> `goAsync`, read the ticket from the cache, `recordOperatorNearby`).
8. (Optional, Q8) Citizen-side minimal delivery: "proximity" channel, `POST_NOTIFICATIONS`, consumer per the contract, strings EN/PT.

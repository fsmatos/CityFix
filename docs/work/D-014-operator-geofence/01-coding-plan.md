# D-014 coding plan

Core: slices 1-5 (approach B, pending Gate 1 Q1). 5b, 6, 7 and 8 run only if their open question is answered yes. Each slice: `./gradlew assembleDebug` and `./gradlew lint` green, one commit `feat(D-014): ...`, a Log line in `state.md`. There are no automated tests; the device checks D1-D10 are at the end.

## Files
| Path | Change | Why |
| --- | --- | --- |
| `app/src/main/java/pt/isec/amov/cityfix/model/Ticket.kt` | add `lastOperatorNearbyAt: Long? = null` | the alert record (Q3) |
| `app/src/main/java/pt/isec/amov/cityfix/model/TicketPrivacy.kt` | `lastOperatorNearbyAt = null` in `toAnonymousView()` | the projection is a `copy`; keep it minimal |
| `app/src/main/java/pt/isec/amov/cityfix/model/OperatorProximity.kt` | new | pure 20 m / 1 h rules |
| `app/src/main/java/pt/isec/amov/cityfix/data/FirestoreMapping.kt` (Francisco) | one line in `toTicket()` | read the field |
| `app/src/main/java/pt/isec/amov/cityfix/data/TicketsRepository.kt` (Francisco) | new `recordOperatorNearby` | interface for the write (Q4) |
| `app/src/main/java/pt/isec/amov/cityfix/data/FirestoreTicketsRepository.kt` (Francisco) | implement it | single-field update, not awaited |
| `app/src/main/java/pt/isec/amov/cityfix/data/LocationRepository.kt` (Francisco) | new `locationUpdates()` | continuous fixes (F-012 handover) |
| `app/src/main/java/pt/isec/amov/cityfix/viewmodel/OperatorProximityViewModel.kt` | new | the watch loop |
| `app/src/main/java/pt/isec/amov/cityfix/viewmodel/OperatorProximityViewModelFactory.kt` | new | as the other factories |
| `app/src/main/java/pt/isec/amov/cityfix/MainActivity.kt` | start/stop the watch | operator, precise location, foreground |
| `firestore.rules` | (5b, Q5) draft `allow update` | documents the server-side throttle; not deployed |
| `app/src/main/AndroidManifest.xml` | (6/7) `ACCESS_BACKGROUND_LOCATION`, `<receiver>` | Geofencing API only |
| `app/src/main/java/pt/isec/amov/cityfix/utils/LocationPermissions.kt` (Francisco) | (6) `hasBackgroundLocationPermission` | Geofencing API only |
| `app/src/main/java/pt/isec/amov/cityfix/utils/GeofenceManager.kt` | (7) new | register/remove geofences |
| `app/src/main/java/pt/isec/amov/cityfix/data/GeofenceBroadcastReceiver.kt` | (7) new | ENTER -> write |
| `app/src/main/java/pt/isec/amov/cityfix/utils/ProximityNotification.kt` + a citizen consumer | (8) new | local delivery without F-016 |
| `app/src/main/res/values/strings.xml`, `values-pt/strings.xml` | (6, 8) new keys | rationale / notification text |

## Interfaces
```kotlin
// model/Ticket.kt
/** Time (epoch ms) an operator last came within 20 m while IN_PROGRESS (D-014). Time only, never a location. Null = never. */
val lastOperatorNearbyAt: Long? = null

// model/OperatorProximity.kt
const val OPERATOR_GEOFENCE_RADIUS_METERS = 20.0
const val PROXIMITY_ALERT_INTERVAL_MS: Long = 60L * 60 * 1000
fun Ticket.isWithinOperatorGeofence(at: Coordinates): Boolean
fun Ticket.canRecordOperatorNearby(now: Long): Boolean
fun List<Ticket>.insideOperatorGeofence(at: Coordinates, now: Long): Set<String>

// data/TicketsRepository.kt
/**
 * Records that an operator is within 20 m of [ticket] (brief I.3 Obs, II.3; D-014): sets only
 * tickets/{id}.lastOperatorNearbyAt = [now]. No operator id or location is written. False when
 * canRecordOperatorNearby(now) is false; otherwise true once the write is queued. F-016 turns it
 * into the participants' notification.
 */
fun recordOperatorNearby(ticket: Ticket, now: Long): Boolean

// data/LocationRepository.kt
/** Continuous precise fixes every [intervalMs] while collected; empty without FINE. Fixes less accurate than 25 m are dropped. */
fun locationUpdates(intervalMs: Long = 5_000): Flow<Coordinates>

// viewmodel/OperatorProximityViewModel.kt
class OperatorProximityViewModel(
    private val locationRepository: LocationRepository,
    private val ticketsRepository: TicketsRepository,
) : ViewModel() {
    /** Runs until cancelled (repeatOnLifecycle). Reads [tickets].value on each fix. */
    suspend fun watch(tickets: StateFlow<List<Ticket>>)
}
```
`LocationRepository` is a class with no interface. The ViewModel takes it directly, as `LocationGateViewModel` does.

## Steps

### Slice 1: model rules and the field
- `Ticket.kt`: the field with its KDoc, after `statusHistory`.
- `TicketPrivacy.kt`: add `lastOperatorNearbyAt = null,` to the `copy` in `toAnonymousView()`. Update its KDoc list of cleared fields, if it has one.
- `OperatorProximity.kt` (file KDoc: brief I.3 Obs, II.3; pure, time is a parameter):
  - `isWithinOperatorGeofence(at) = haversineMeters(location.latitude, location.longitude, at.latitude, at.longitude) <= OPERATOR_GEOFENCE_RADIUS_METERS`;
  - `canRecordOperatorNearby(now)`: `if (!needsOperatorGeofence(now)) return false; val last = lastOperatorNearbyAt ?: return true; return now - last >= PROXIMITY_ALERT_INTERVAL_MS` (a negative gap gives false);
  - `insideOperatorGeofence(at, now) = filter { it.needsOperatorGeofence(now) && it.isWithinOperatorGeofence(at) }.map { it.id }.toSet()`.
- `FirestoreMapping.kt` `toTicket()`: `lastOperatorNearbyAt = getLong("lastOperatorNearbyAt"),`.
- Hand check (table in `state.md`, as D-013 slice 1): 15 m north of a ticket (lat + 0.000135) inside; 25 m (lat + 0.000225) outside; exactly 20 m inside; OPEN / INFO_REQUESTED / RESOLVED / (0, 0) not in the set; `last = now - 59 min` false, `now - 60 min` true, `now + 1 min` false, `null` true.
- Commit: `feat(D-014): add operator proximity rules and lastOperatorNearbyAt`.
- Verify: build + lint; record the warning baseline.

### Slice 2: the write
- Interface method with the KDoc above, after `resolveTicket`.
- Impl: `if (!ticket.canRecordOperatorNearby(now)) return false`, then `firestore.collection(TICKETS).document(ticket.id).update("lastOperatorNearbyAt", now).addOnFailureListener { Log.w(TAG, "nearby not saved", it) }`, then `return true`. Add one KDoc line saying the update is not awaited, as `sendMessage`.
- Tell Francisco (signature and why) before committing. Log it in `state.md`.
- Commit: `feat(D-014): record operator proximity on the ticket`.
- Verify: build + lint (no caller yet).

### Slice 3: location updates
- `locationUpdates`: written-out FINE `checkSelfPermission` (lint `MissingPermission`). If missing, `return emptyFlow()`. Otherwise `callbackFlow { val callback = object : LocationCallback() { override fun onLocationResult(result: LocationResult) { result.lastLocation?.takeIf { !it.hasAccuracy() || it.accuracy <= MAX_FIX_ACCURACY_METERS }?.let { trySend(Coordinates(it.latitude, it.longitude)) } } }; client.requestLocationUpdates(request, callback, Looper.getMainLooper()); awaitClose { client.removeLocationUpdates(callback) } }`.
- `private const val MAX_FIX_ACCURACY_METERS = 25f` in a companion object. Replace the class KDoc line "D-014 adds updates here" with the real description. No `Log` of coordinates.
- Commit: `feat(D-014): add continuous location updates`.
- Verify: build + lint.

### Slice 4: the watcher ViewModel
- `watch(tickets)`: `val inside = mutableSetOf<String>()` local to the call, so it resets on each start. Inside `locationRepository.locationUpdates().collect { fix -> ... }`:
  - `now`, `current = tickets.value.insideOperatorGeofence(fix, now)`, `entered = current - inside`;
  - `inside.clear(); inside += current`;
  - for each id in `entered`: `val ticket = tickets.value.firstOrNull { it.id == id } ?: continue`; skip if `recentlyRecorded[id]?.let { now - it < PROXIMITY_ALERT_INTERVAL_MS } == true`; `if (ticketsRepository.recordOperatorNearby(ticket, now)) recentlyRecorded[id] = now`.
- `recentlyRecorded` is a `mutableMapOf<String, Long>()` property (survives rotation, lost on process death; the stored field covers that). The collection runs on the main dispatcher; the work per fix is small and nothing blocks.
- The factory follows the `LocationGateViewModelFactory` shape.
- Commit: `feat(D-014): watch operator proximity to in-progress tickets`.
- Verify: build + lint.

### Slice 5: wire it in `CityFixApp`
- `val proximityViewModel: OperatorProximityViewModel = viewModel(factory = OperatorProximityViewModelFactory(app.locationRepository, app.ticketsRepository))`.
- `val operator = session.ticketsUser as? Operator`. `var hasPrecise by remember { mutableStateOf(hasPreciseLocationPermission(context)) }`, refreshed with `LifecycleResumeEffect(Unit) { hasPrecise = hasPreciseLocationPermission(context); onPauseOrDispose { } }` and also in the existing start-up permission callback.
- `val lifecycleOwner = LocalLifecycleOwner.current; LaunchedEffect(operator?.id, hasPrecise) { if (operator != null && hasPrecise) lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { proximityViewModel.watch(ticketsViewModel.tickets) } }`, with a comment (brief II.3, foreground only, Q1).
- When an operator has no precise location: `Log.i` once, with no UI (Q10).
- Commit: `feat(D-014): start the proximity watch for operators`.
- Verify: build + lint, then device checks D1-D10.

### Slice 5b (Q5 yes): draft rule
- In `match /tickets/{ticketId}`, after the upload-state rule: `allow update: if isOperator() && resource.data.currentStatus == 'IN_PROGRESS' && changedKeys().hasOnly(['lastOperatorNearbyAt']) && request.resource.data.lastOperatorNearbyAt is int && request.resource.data.lastOperatorNearbyAt <= request.time.toMillis() + 300000 && (!('lastOperatorNearbyAt' in resource.data) || request.resource.data.lastOperatorNearbyAt >= resource.data.lastOperatorNearbyAt + 3600000);`. Add a comment block (D-014, time only, throttle; draft, not deployed).
- Check the syntax in the console Rules Playground **without publishing**. No `firebase deploy`.
- Commit: `feat(D-014): draft rule for the proximity field`.

### Slice 6 (optional, Q1 hybrid / Q2 yes): background permission
- Manifest `ACCESS_BACKGROUND_LOCATION`. `hasBackgroundLocationPermission(context)` (API 29+; below that, true when FINE is granted).
- For an operator with FINE and no background permission, an `AlertDialog` rationale ("Allow all the time so citizens are told when you are near their report, even with the app closed"). Its button launches `RequestPermission(ACCESS_BACKGROUND_LOCATION)`; on API 30+ the system opens Settings. Shown once per session (`rememberSaveable`). Denial: the foreground watch keeps working.
- Strings EN/PT `proximity_background_*`.
- Commit: `feat(D-014): ask operators for background location`.

### Slice 7 (optional, Q1 hybrid): Geofencing API
- `GeofenceManager(context)`: `sync(tickets: List<Ticket>, now)` registers up to 100 `Geofence.Builder().setRequestId(id).setCircularRegion(lat, lon, 20f).setExpirationDuration(NEVER_EXPIRE).setTransitionTypes(GEOFENCE_TRANSITION_ENTER)` with `INITIAL_TRIGGER_ENTER`, after `removeGeofences(pendingIntent)`. `clear()` runs on logout. The `PendingIntent` is `FLAG_MUTABLE` (required for geofence intents on API 31+).
- `GeofenceBroadcastReceiver`: `GeofencingEvent.fromIntent`. On ENTER, `goAsync()`. For each triggering id, `firestore.collection("tickets").document(id).get(Source.CACHE)`, then `toTicket()`, then `recordOperatorNearby(ticket, now)` (the repository from `context.applicationContext as CityFixApplication`, as the workers do). Call `finish()` in `addOnCompleteListener`.
- Manifest: `<receiver android:name=".data.GeofenceBroadcastReceiver" android:exported="false" />`.
- Synced from `CityFixApp` in `LaunchedEffect(operator?.id, tickets)`, only with background permission.
- Commit: `feat(D-014): add 20 m geofences for in-progress tickets`.

### Slice 8 (optional, Q8): minimal local delivery
- `utils/ProximityNotification.kt`: channel `"proximity"` (F-016's planned id), `notify(context, ticket)` with the category name and a tap `PendingIntent` to `MainActivity` (a deep link only if F-016's route arg exists, otherwise it opens the app).
- `POST_NOTIFICATIONS` in the manifest and in the start-up request, following F-012's handover note: the callback must then read only the location entries.
- A citizen-side `ProximityAlertsViewModel` applies contract points 1-3 to the shared `tickets` (last notified value per ticket in `SharedPreferences`).
- Strings EN/PT. Commit: `feat(D-014): show the proximity alert to participants`.
- It must be handed to Francisco to merge into F-016.

## Device checks (two emulators; never write test e-mails, phones or UIDs here)
Coordinates for `adb emu geo fix <lon> <lat>`: ticket at (lat T, lon L); 15 m = lat T + 0.000135; 25 m = T + 0.000225; 100 m = T + 0.0009.
| # | Steps | Expected |
| --- | --- | --- |
| D1 | Operator signs in, app open, ticket IN_PROGRESS; fix at 100 m, then 15 m | `lastOperatorNearbyAt` set within about 10 s; no other field changes |
| D2 | Same with OPEN, INFO_REQUESTED, RESOLVED tickets; IN_PROGRESS at 25 m | nothing written |
| D3 | In/out/in three times within the hour | field keeps the first value |
| D4 | Second operator device enters within the hour | nothing written |
| D5 | Debug-shortened interval (2 min): stay inside 3 min; then leave and re-enter | nothing new while inside; new value on re-entry |
| D6 | Home button; status bar location icon; back to the app | updates stop / resume; Log out stops them |
| D7 | Citizen and anonymous at 15 m | nothing written |
| D8 | Operator with "Approximate" / denied; then grant precise in Settings and return | no crash, no writes; the watch starts on return |
| D9 | Airplane mode, enter, network back | field in the cache at once, on the server later; one write |
| D10 | Console + logcat | no coordinates in any new field or log; `publicTickets` unchanged |
| D11 | (F-016 or slice 8) creator's and follower's devices | one notification, no operator data; tap opens the ticket |

## Conventions
- Pure rules in `model/` with time as a parameter; the ViewModel reads the clock once per fix.
- Writes are not awaited and are queued offline (`sendMessage` style). Failures are logged without coordinates.
- Structured concurrency: `repeatOnLifecycle` inside `LaunchedEffect`; no `GlobalScope`. `awaitClose` removes the callback.
- KDoc cites the brief section. Plain code the authors can explain (the edge set, the map, one update).
- New user-visible strings only in slices 6 and 8, in EN and PT.

## Do not touch
- `publicTickets` documents and `toPublicTicketDocument()`, and `ticketLocations`.
- `commitStatusChange` and the existing lifecycle functions.
- `firestore.rules` without Q5's OK. No `firebase deploy`. No `google-services.json` or keys.
- F-016's area: the channels, FCM, `MainActivity`'s permission callback (except under slice 8 with Francisco's agreement).
- The F-012 start-up FINE + COARSE request (unchanged).

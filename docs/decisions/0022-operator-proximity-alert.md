# 0022: Operator proximity alert (20 m geofence)

Status: accepted; mechanism amended by F-022 / [0028](0028-background-tracking-and-vertical-impact.md) (foreground service, see Consequences). (D-014, Gate 1 on 2026-11-17, approved by Diogo: Q1-Q11 as recommended; slices 1-5 done; the optional slices 5b (draft rule), 6-7 (background permission and Geofencing API) and 8 (local delivery) not done). Review round 1 (2026-11-17): approve with should-fixes; S1, S2 and N1-N5 fixed (`ec1cc8e`, `e807967`). Review round 2: approve; two docs nits (R2-N1, R2-N2) handled in the docs phase; S3 (tell Francisco, confirm the F-016 contract) still open for Diogo. **No device check has run yet** (D1-D11 open). Amends [decision 0010](0010-firestore-schema.md) (a new optional ticket field; suggested amendment line below, Francisco's record, not edited) and the consequences of [decision 0012](0012-firebase-plan-notifications-close-maps.md) for D-014 (no Cloud Functions, so the throttle and the delivery are client-side). Code-level detail: `docs/architecture/overview.md`, "Operator proximity (D-014)". Plan: `docs/work/D-014-operator-geofence/00-plan.md`.

## Context
- **Brief I.3 Obs and II.3** (wording from the task file; the PDF could not be rendered in the D-014 sessions, plan R6): when an operator comes within 20 m of an IN_PROGRESS ticket, its authors (creator and followers) are alerted that the council team is in the area. **Brief III** lists the proximity alert as a notification; **brief V** lists background location among the permissions.
- **D-013 left a hook:** `Ticket.needsOperatorGeofence(now)` (effectively IN_PROGRESS and a real location), and an operator's shared `TicketsViewModel.tickets` is already `allTickets()` (F-011).
- **F-012's `LocationRepository`** had only a one-shot fix. The manifest has FINE and COARSE, no `ACCESS_BACKGROUND_LOCATION`, no notification permission, receiver or service.
- **No notification code exists**; delivery is F-016's (Francisco), by Firestore listeners on `participantIds` plus a WorkManager check, with **no Cloud Functions** (decision 0012 Q3).
- **Rules are drafts, not deployed** (decision 0012 Q5, amended 2026-11-13); the test-mode database accepts any write.

## Decision
- **Mechanism (Q1): a foreground location Flow with a Haversine check (option B),** not the Android Geofencing API (option A). `LocationRepository.locationUpdates(intervalMs = 5_000)` is a `callbackFlow` over `requestLocationUpdates(PRIORITY_HIGH_ACCURACY)` with `setMinUpdateIntervalMillis(2_000)` (Q11) and `awaitClose { removeLocationUpdates }`; it is empty without FINE. `CityFixApp` collects it only for a signed-in `Operator` with precise location, inside `repeatOnLifecycle(STARTED)`, so it runs only while the app is in the foreground.

  | | A. Geofencing API | B. Foreground Flow (chosen) |
  | --- | --- | --- |
  | App closed / screen off | Works (OS-managed, up to 100 geofences) | Does not work |
  | 20 m accuracy | Poor: Google recommends at least 100-150 m | Exact Haversine on each fix, poor fixes dropped |
  | Latency | ENTER can take about 2-3 min | One interval (about 5 s) |
  | Permission | `ACCESS_BACKGROUND_LOCATION` ("Allow all the time") | FINE only, already requested at start |
  | Lifecycle | Re-register on list change, logout, reboot and app update; receiver with `goAsync()` | One flow under `repeatOnLifecycle` |
  | Emulator testing | Needs Play location services, delayed | `adb emu geo fix`, immediate |
  | Brief V | Covered | Not covered (a stated limit) |

- **No background permission (Q2).** `ACCESS_BACKGROUND_LOCATION` is not requested: with B it would be unused. It belongs to the optional slice 6, with an operator-only rationale, which was not done.
- **The record is one field (Q3): `tickets/{id}.lastOperatorNearbyAt`** (Long, epoch ms, the operator's device time, like `statusHistory.dateTime`). No operator id and no coordinates. `Ticket.lastOperatorNearbyAt: Long? = null`; `toTicket()` reads it; `toTicketDocument`, `toStatusFields` and `toPublicTicketDocument` never write it; `toAnonymousView()` clears it. The participants' existing `participantTickets(uid)` listener delivers it, and `publicTickets` never has it.
- **Pure rules in `model/OperatorProximity.kt`:** `OPERATOR_GEOFENCE_RADIUS_METERS = 20.0` (inclusive), `PROXIMITY_ALERT_INTERVAL_MS = 1 h`, `isWithinOperatorGeofence(at)`, `canRecordOperatorNearby(now)` (full ticket, `needsOperatorGeofence`, last record at least 1 h old; a future value from clock skew gives false) and `insideOperatorGeofence(at, now)`. A projected ticket (empty `statusHistory`, decision 0006) is never in the set and never recorded (review S1).
- **Entry edge plus a shared hourly throttle (Q6).** `OperatorProximityViewModel.watch(operatorId, tickets)` records only tickets that were outside on the previous fix and are inside now. The stored field is the throttle shared by every operator; an in-memory `recentlyRecorded` map covers the moment before the cache snapshot comes back. The `inside` set and the map are ViewModel properties, cleared only when the operator id changes (review N1, N4), so rotation and Home-and-return are not new entries.
- **Accuracy filter (Q7):** fixes with an accuracy radius above 25 m are dropped.
- **The write lives in Francisco's `TicketsRepository` (Q4):** `fun recordOperatorNearby(ticket: Ticket, now: Long): Boolean` re-checks `canRecordOperatorNearby`, then `update("lastOperatorNearbyAt", now)`, not awaited; a failure is only logged (`"nearby not saved"`, no coordinates).
- **Draft rule (Q5): approved as documentation only (slice 5b), not written.** No `firestore.rules` edit and no deploy in D-014.
- **Operator without precise location (Q10):** one `Log.i`, no UI; the operator keeps working without the alert.
- **F-016 contract (Q9)** for the consumer, to be confirmed by Francisco:
  1. Source: the citizen's `participantTickets(uid)` snapshots (app listener and periodic worker). Operators and anonymous users never get this notification.
  2. Trigger: `lastOperatorNearbyAt` present, greater than the last value this device notified for that ticket (stored locally per ticket id), and fresh (`now - value < PROXIMITY_ALERT_INTERVAL_MS`); a stale value is recorded as seen and not notified.
  3. Content: "The council team is near your report" plus the category name; no operator name, coordinates or distance. Tap opens `TicketDetailRoute(id)`, on F-016's "proximity" channel.
  4. A change of `lastOperatorNearbyAt` alone is not a status change.
  5. "Work started" (IN_PROGRESS) is F-016's status-change notification; D-014 adds nothing for it.
  - **F-016 contract confirmed** (2026-11-20, [decision 0024](0024-notifications.md)), with one change to point 2: the trigger is `lastOperatorNearbyAt` after the user's single `lastCheckedAt` (not a per-ticket value) and less than 1 h old.
- **Local delivery without F-016 (Q8):** optional slice 8, not done; the citizen's device shows nothing until F-016 consumes the field.

### Rejected
- **The Geofencing API alone, or the hybrid (A on top of B):** poor accuracy at 20 m, minutes of latency, the "Allow all the time" permission and re-registration code, for the deadline. The hybrid stays an improvement (slices 6-7); both paths would share the same pure rule and the same write, so the stored throttle would remove duplicates.
- **A location foreground service** (P10's `ProximityService`): a persistent notification, `FOREGROUND_SERVICE_LOCATION` and a channel that overlaps F-016's work.
- **Cloud Functions** for the throttle or the push (decision 0012 Q3).
- **A `proximityAlerts/{autoId}` collection:** F-016 would need a second listener (`whereIn` of at most 30 ids or a denormalised `participantIds`), new read rules and a query for the throttle.
- **A server timestamp:** null in the cache until the server acknowledges it, which breaks the local throttle offline.
- **Storing the operator id or position:** not needed by the brief, and personal data the citizens must not get.
- **Recording whenever inside (no edge):** an operator working three hours on site would trigger three alerts.

## Consequences
- **Foreground only; brief V's background location is not covered.** Nothing is recorded while the app is in the background, closed or the screen is off. Stated in the report as a limit and an improvement.
- **An arrival made while the app was in the background is not counted (review R2-N2).** `inside` survives Home and return, so an operator who presses Home at ticket A, walks away, comes back to A and reopens the app is still "inside A" on the first fix. An entry is counted only if the operator was seen outside the circle while the app was in the foreground. This follows from the N1 fix and fits Q1.
- **The throttle is client-side and per device clock.** Two operators entering within seconds while offline can both write (last write wins); a device clock far ahead blocks others for longer. F-016's per-ticket "last notified" and freshness checks (contract point 2) are the second line.
- **The draft rules would refuse the write.** No `allow update` on `tickets/{ticketId}` in the draft `firestore.rules` accepts an update whose only changed key is `lastOperatorNearbyAt` (the status rule needs a `currentStatus` change and an appended history entry; the join rule is for citizens; the upload rule allows `attachments` only). Test mode accepts it today. **Before any deploy** a rule is needed, for example: `isOperator()`, `resource.data.currentStatus == 'IN_PROGRESS'`, `changedKeys().hasOnly(['lastOperatorNearbyAt'])`, the value `is int` and `<= request.time.toMillis() + 300000`, and, when an old value exists, `>= old + 3600000`. Editing the shared rules needs Diogo's explicit OK (and Francisco's agreement); otherwise the write fails on the server and only `"nearby not saved"` is logged.
- **No delivery yet.** The acceptance check "the author's device shows the alert" waits for F-016 (or slice 8).
- **Francisco's files changed (additive):** `data/FirestoreMapping.kt`, `data/TicketsRepository.kt`, `data/FirestoreTicketsRepository.kt`, `data/LocationRepository.kt`. He must be told, and the contract confirmed, before `/push-task` (review S3).
- **Suggested amendment line for decision 0010** (Francisco's record, not edited): "`tickets/{id}.lastOperatorNearbyAt` (Long, optional): time of the last operator arrival within 20 m of an IN_PROGRESS ticket; no operator id or location; never in `publicTickets` (D-014, decision 0022)."
- **Amended by F-022 (2026-11-22, [0028](0028-background-tracking-and-vertical-impact.md)), supersedes Q1 "foreground only" and Q2:** the loop moved from `OperatorProximityViewModel` (deleted, with its factory) to `data/OperatorGeofence`, run by `data/TrackingService`, a location foreground service (slides 08, P10 `ProximityService`). `ACCESS_BACKGROUND_LOCATION` is asked after an explanation dialog, once per session, after notifications (slides 10/11). Same pure rules, write and throttle; `inside` now resets on each service start (the stored 1 h throttle covers it). The R2-N2 limit is gone while the service runs. Diogo still to be told.
- **Not verified (no device check run yet):** D1 entry from 100 m to 15 m writes the field; D2 nothing for OPEN / INFO_REQUESTED / RESOLVED or at 25 m; D3 in/out/in within the hour writes once; D4 a second operator writes nothing; D5 staying inside past a debug-shortened interval writes nothing; D6 Home stops the updates, return resumes, Log out stops; D7 citizens and anonymous write nothing; D8 approximate / denied, then granting precise starts the watch; D9 offline write, one write; D10 no coordinates in Firestore or logcat; D11 the notification (needs F-016 or slice 8).

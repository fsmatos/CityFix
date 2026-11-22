# F-022: Background geofence and vertical impact detection
Type: feature · Task file: docs/tasks/phase-3/F-022-background-geofence-impact.md
Labels: deadline-today

## Problem
- Operator 20 m geofence (D-014) only runs with the app in front; no `ACCESS_BACKGROUND_LOCATION` (brief V).
- Impact (F-015, decision 0023) uses total acceleration, no motion check, fixed threshold, stops when the app closes.

## Outcome
- Impact uses only the vertical component, needs the phone to be moving, and has a citizen-set threshold.
- One foreground service keeps the geofence (operator) and impact detection (citizen) running with the app closed.

## Acceptance checks
- [ ] Citizen, detection on, moving (> 2 m/s): vertical jolt -> snackbar in front.
- [ ] Same jolt while stopped (or phone put down on a table) -> nothing.
- [ ] Moving the threshold slider changes how hard the jolt must be.
- [ ] App in background/closed, citizen moving: jolt -> notification; tap opens the prefilled report.
- [ ] Operator, app in background: entering 20 m of an IN_PROGRESS ticket records the arrival.
- [ ] Background location asked after an explanation dialog (Android 10+); denial keeps the app working.
- [ ] Permanent low-importance notification while the service runs; gone after Log out.
- [ ] `./gradlew assembleDebug` and `./gradlew lint` pass.

## Approach
- Vertical accel (slides 10/11, `TYPE_GRAVITY`): `a_v = (a·g)/|g| - |g|`; low-pass on the accelerometer if no gravity sensor.
- Impact when `|a_v| > threshold` AND last GPS speed > 2 m/s (fix younger than 5 s); 30 s cooldown kept.
- Projection is a pure function in `utils/`, so it can be explained alone.
- Threshold: SharedPreferences (like `NotificationPrefs`), default 6 m/s², range 3..15; slider in Profile under the detection switch.
- Foreground service `type="location"` (slides 08, sheet P10): `CoroutineScope(Dispatchers.IO + SupervisorJob())`, `startForeground` within 5 s.
- Service hosts location updates (geofence + speed) and the impact sensor; started/stopped by MainActivity from the session.
- Started only while the app is visible (Android 12+ rule) and only with FINE granted (Android 14 rule).
- Impact in front -> existing snackbar; not in front -> local notification with `fromImpact` extra.
- Alternatives: WorkManager (too slow for 20 m / sensors); `GeofencingClient` (not taught, tickets change often). Rejected.

## Architecture impact
- New `data/TrackingService.kt`; geofence logic moves from `OperatorProximityViewModel` to a plain class `data/OperatorGeofence.kt`.
- `LocationRepository` gains `locationFixes()` returning `Location` (speed + time); `locationUpdates()` maps it.
- `CityFixApplication` gains `appInForeground` flag and an `impacts` SharedFlow for the in-app snackbar.
- Manifest: `ACCESS_BACKGROUND_LOCATION`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION`, service entry.

## Failure modes
- No GPS speed (indoors, no fix) -> no impact suggested (by design).
- No gravity sensor -> low-pass fallback; no accelerometer -> nothing.
- FINE denied -> service not started; app works as today.
- Notifications denied -> service runs, its notification is hidden; impact notification not shown.
- Background location denied -> service still works while started from the foreground (while-in-use FGS).

## Risks and open questions
- Risk: time. Slices 1-3 (impact maths, speed, threshold) are must-have; 4-6 (service) can be dropped if time runs out, leaving D-014 as today.
- Risk: OEM battery savers can kill the service; accepted.
- Q1 Threshold default/range? Proposed 6 m/s², 3..15, step 0.5.
- Q2 Minimum speed? Proposed 2 m/s (~7 km/h), fix newer than 5 s.
- Q3 Remove the in-app ImpactViewModel detector once the service has it (slice 6)? Proposed yes: service is the single detector, ViewModel only shows the snackbar.
- Q4 Delete `OperatorProximityViewModel` (Diogo's D-014) after the move? Proposed yes, and tell Diogo.
- Q5 Threshold per device (local) not per account (Firestore)? Proposed local.
- Q6 When to ask background location? Proposed once per session after the notifications answer, operator or citizen with detection on.

## Out of scope
- `GeofencingClient`, FCM, restarting the service after reboot, battery tuning.
- Impact detection for operators or anonymous users.

## Slices
1. Vertical projection + gravity sensor in `ImpactDetector` (in-app, as today).
2. GPS speed gate (`locationFixes`, ImpactViewModel feeds speed).
3. Threshold pref + Profile slider. **Stop point if late.**
4. Foreground service + manifest; operator geofence moved into it.
5. Background location explanation dialog + request.
6. Impact inside the service; notification when not in front; in-app path reduced to the snackbar.

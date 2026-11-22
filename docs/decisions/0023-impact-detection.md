# 0023: Impact detection

Status: accepted (F-015, Gate 1 on 2026-11-19, Q1-Q9 answered with the defaults; Q1/Q2 changed by the person after the first emulator run, see below). Amended by F-022 / [0028](0028-background-tracking-and-vertical-impact.md), see the end.

## Context
- Brief I.2.F: a citizen who turned on "Deteção de Impactos" (I.2.A, `Citizen.impactDetection`, [0016](0016-registration.md)) gets a suggestion to report a pavement anomaly after a sharp acceleration "no eixo vertical".
- Class material covers it: slides 10/11 "Permissions_Sensor_Navigation" (Sensor Framework, `PotholeDetector` with `TYPE_ACCELEROMETER`, `SENSOR_DELAY_GAME`, `z > 14 || z < 6`; "always unregister in onPause/onStop") and the demo `SideEffect_BR_Maps_Sensors_Permissions/Sensors.kt` (`DisposableEffect` + `LifecycleEventObserver`).

## Decision
- **Q1/Q2, total acceleration (deviation).** Gate 1 chose the slide's Z axis with its limits. On the emulator an upright phone reads Z near 0 (below 6), so it fired every 30 s. The person chose `sqrt(x² + y² + z²)` outside the same 6..14 m/s²: about 9.8 at rest in any position, so no false alarm when upright, and a vertical bump still triggers it.
- **Departure from the brief:** total acceleration also reacts to non-vertical jolts (putting the phone down hard, lifting it fast), while I.2.F says "eixo vertical". Accepted because it is the simplest orientation-independent check; only the vertical part needs a projection on gravity (improvement in the report notes).
- **Q3:** `utils/ImpactDetector` owns the sensor, `ImpactViewModel` owns the detector; the composable `ImpactDetectionEffect` only calls `start`/`stop` (`.claude/rules/kotlin.md`). Rejected: registering inside the composable as the demo does.
- **Q4:** 30 s cooldown, so one bump gives one suggestion.
- **Q5:** foreground only (ON_START start, ON_STOP stop). Rejected: a foreground service (permanent notification, more code).
- **Q6:** a long snackbar "Report" with dismiss; never an automatic report.
- **Q7:** prefill through the route argument `ReportTicketRoute(fromImpact = true)`: Damage + Pavement + the current location; description and media empty. Rejected: a `prefill(situation, categoryId)` event (F-006 idea).
- **Q8:** Pavement found by id `pavement` (DebugSeed); missing -> the user picks a category.
- **Q9:** no suggestion while on the report form (the cooldown still starts).
- Enabled only for `AccessRole.CITIZEN` with `impactDetection == true`; no runtime permission needed.

## Consequences
- **D-011:** saving the profile toggle updates the session, which starts or stops the detector by itself.
- **D-012/D-015:** keep a category with id `pavement` when DebugSeed is removed.
- **Limits:** no detection with the app closed; the suggestion is lost on a rotation during the snackbar; no movement check, so a dropped phone can trigger it.

## Amendment (F-022, 2026-11-22, [0028](0028-background-tracking-and-vertical-impact.md))
- **Q1/Q2 replaced:** vertical projection on gravity, `(a·g)/|g| - |g|`, gravity from `TYPE_GRAVITY` (slides 10/11) or a low-pass filter; the I.2.F "eixo vertical" departure is fixed.
- **Speed gate:** counts only with GPS speed > 2 m/s from a fix < 10 s old; a dropped phone at rest no longer counts.
- **Threshold:** fixed 6 m/s² (`ImpactDetector.THRESHOLD`), set in code; no user setting (the first F-022 slider was removed, `53a39e6`).
- **Q3/Q5 replaced:** detection runs in `data/TrackingService` (foreground service, slides 08, P10 `ProximityService`); `ImpactDetectionEffect` deleted; app in front -> snackbar, otherwise a notification that opens the prefilled report.
- **Limits fixed:** app closed, sideways jolts, dropped phone. **Limits now:** needs precise location; no restart after a reboot; rotation during the snackbar still loses it.

## Amendment (D-022, 2026-11-22, [0028](0028-background-tracking-and-vertical-impact.md))
- The F-022 speed gate and 6 m/s² threshold are replaced: vertical axis, with gravity, outside 6..14 m/s², no minimum speed.
- A dropped phone at rest can trigger a suggestion again.

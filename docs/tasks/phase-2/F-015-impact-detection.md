# F-015: Impact detection (accelerometer)

Owner: Francisco (`dev/Francisco`) · Layer: `utils` / `ui` · Phase 2 · Status: to do · Target: 2026-11-18
Depends on: D-010 (the `impactDetection` setting), F-012 (location for the suggested report)

## Goal
For citizens who turned it on, detect sharp vertical accelerations (potholes, bumps) and suggest opening a pavement ticket right away.

## Why
- Brief I.2.F; the choice is made at registration (I.2.A) and stored in `Citizen.impactDetection`.

## What to build
- **`utils/ImpactDetector`** over `SensorManager` (`TYPE_LINEAR_ACCELERATION`, or `TYPE_ACCELEROMETER` minus gravity with a low-pass filter), as in the `SideEffect_BR_Maps_Sensors_Permissions` class example. Detect a vertical spike over a threshold (start around 2.5 g, tune on a device), with a cooldown (for example 30 s) so one bump gives one suggestion.
- **Lifecycle:** run only while the user is logged in as a citizen with the setting on and the app is in the foreground (`DisposableEffect` / lifecycle-aware collection). Running in the background would need a foreground service: say so in the report as an improvement.
- **Suggestion:** snackbar or notification "Possible pothole detected. Report it?" that opens the report form with the pavement category and situation DAMAGE preselected and the current location.
- Settings toggle comes from D-011 (profile); this task only reads it.

## Files
New `utils/ImpactDetector.kt`; `MainActivity.kt` or a small app-level composable (tell Diogo), `viewmodel/ReportViewModel.kt` (prefill), strings EN/PT.

## Acceptance checks
- [ ] Shaking the phone hard vertically shows one suggestion; ordinary walking does not.
- [ ] With the setting off, or logged out, the sensor is not registered.
- [ ] Accepting opens a prefilled report.

## References
Brief I.2.A, I.2.F; class example `SideEffect_BR_Maps_Sensors_Permissions`.

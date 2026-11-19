# F-015: Impact detection (accelerometer)
Type: feature · Task file: docs/tasks/phase-2/F-015-impact-detection.md
Labels: none

## Problem
Brief I.2.F: a citizen who turned on "Detecao de Impactos" (chosen at registration, I.2.A, stored as `Citizen.impactDetection` by D-010) should get a suggestion to report a pavement anomaly after a sharp acceleration "no eixo vertical". Nothing reads the setting yet.

## Outcome
An active citizen with the setting on and the app open feels a jolt and sees a snackbar "Possible pothole detected. Report it?". "Report" opens the form with Pavement + Damage chosen and the location being read.

## Acceptance checks
- [ ] A vertical jolt (emulator Virtual sensors, or `adb emu sensor`) shows one snackbar; a second within the cooldown shows nothing.
- [ ] Setting off, logged out, pending citizen or operator: the sensor is never registered (Logcat).
- [ ] App in the background: sensor unregistered; back in front: registered again.
- [ ] "Report" opens the form with Pavement and Damage selected.
- [ ] Portrait and landscape; TalkBack reads the action. No new permission (the accelerometer needs none).

## Approach (class slides 10/11 "Sensor Framework" + `SideEffect_BR_Maps_Sensors_Permissions` demo)
- `utils/ImpactDetector`: the slides' `PotholeDetector` almost as is: `SensorEventListener`, `TYPE_ACCELEROMETER`, `SENSOR_DELAY_GAME`, `start()`/`stop()`, Z axis outside 6..14 m/s² = impact. We add an `onImpact` callback and a cooldown.
- Start/stop exactly like the demo's `Sensors.kt`: `DisposableEffect` + `LifecycleEventObserver` (ON_START start, ON_STOP stop, start at once if already STARTED, `onDispose` stop). Matches the slide "always unregister in onPause()/onStop()".
- `ImpactViewModel` owns the detector and exposes `impactDetected` (Q3).
- `MainActivity` (~15 lines): enabled = active CITIZEN with `impactDetection`; the existing snackbar host shows the suggestion.
- Prefill: `ReportTicketRoute` becomes `data class ReportTicketRoute(val fromImpact: Boolean = false)`; the ViewModel selects Pavement + DAMAGE.
- Not used: `TYPE_LINEAR_ACCELERATION` (virtual, slide 1550; may be missing on the emulator); foreground service (permanent notification, more code).

## Architecture impact
New `utils/ImpactDetector`, `viewmodel/ImpactViewModel(+Factory)`, `ui/components/ImpactDetectionEffect`; `ReportTicketRoute` gets an argument. Diogo's `MainActivity` changes slightly (tell him).

## Failure modes
- No accelerometer: `start` does nothing (`?.let`, as in class).
- Z axis only sees "vertical" when the phone lies flat (dashboard mount, table); upright in a pocket, a bump shows on Y (Q1).
- The "pavement" category is missing: category stays empty, the user picks one.

## Open questions
Answered 2026-11-19: all recommended defaults (in brackets) accepted.
- Q1 Axis: class Z axis (phone flat, as the slide), or any orientation (project on gravity, ~10 more lines, beyond class)? [Z axis, like class; limit noted in the report]
- Q2 Thresholds: the slide's `z > 14 || z < 6` m/s² (about ±0.45 g around gravity) or stronger to avoid false alarms? [class values; tune on a phone if too sensitive]
- Q3 Where the sensor lives: the demo registers it inside the composable; our `.claude/rules/kotlin.md` says composables never touch sensors. [detector class + ViewModel; the effect only calls `start`/`stop`]
- Q4 Cooldown so one bump gives one suggestion. [30 s]
- Q5 "enquanto o utilizador se desloca": foreground only, or a foreground service? [foreground only; service as a report improvement]
- Q6 Suggestion UI: snackbar with "Report", dialog or notification? Brief: "emitindo uma sugestao". [long snackbar with dismiss]
- Q7 Prefill Pavement + DAMAGE + current location; description and media stay empty? [yes]
- Q8 Pavement found by id `pavement` (DebugSeed); Diogo's D-012/D-015 may change the seed. [id `pavement`; tell Diogo]
- Q9 Suggest while already on the report form? [no]

## Out of scope
Profile toggle (D-011); background detection; GPS speed check; automatic report (never silent).

## Slices
1. `ImpactDetector` + `ImpactViewModel`/factory (not wired).
2. `ReportTicketRoute(fromImpact)` + ViewModel prefill.
3. `ImpactDetectionEffect`, MainActivity wiring, snackbar, strings EN/PT.

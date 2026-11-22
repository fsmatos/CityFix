# F-022: Background geofence and vertical impact detection

Owner: Francisco · Asked in chat on 2026-11-22. Touches D-014 (operator geofence, Diogo) and F-015 (impact detection).

## Problems
1. Operator geofence (brief I.3 Obs, II.3) only works while the app is in front: no background location, no `ACCESS_BACKGROUND_LOCATION` (brief V asks for foreground and background location permissions).
2. Impact detection (brief I.2.F, decision 0023) uses total acceleration, has no movement check, a fixed threshold, and stops when the app is closed.

## Wanted
- Geofence keeps working for a logged-in operator with the app in background/closed (foreground service of type `location`, class slides 08 / P10; `ACCESS_BACKGROUND_LOCATION` asked dynamically with an explanation, slides 10/11).
- Impact uses only the vertical component: projection of the acceleration onto the gravity direction (`TYPE_GRAVITY`, slides 10/11, or a low-pass fallback).
- Impact needs motion: recent GPS speed above a minimum, so a stationary phone dropped/put down does not trigger it.
- Configurable threshold (citizen setting).
- Impact detection also with the app closed; the suggestion then arrives as a local notification that opens the prefilled report.

## Acceptance checks
- Operator: app in background, entering a 20 m IN_PROGRESS ticket still records the arrival.
- Citizen with detection on: a vertical jolt while moving suggests a report (snackbar in front, notification when closed); the same jolt while stopped does not.
- Changing the threshold changes the sensitivity.
- `./gradlew assembleDebug` and `./gradlew lint` pass.

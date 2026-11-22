# 0028: Background tracking and vertical impact

Status: accepted (F-022, Gate 1 on 2026-11-22, Q1-Q6 with the proposed defaults). Amends [0022](0022-operator-proximity-alert.md) (foreground only -> foreground service) and [0023](0023-impact-detection.md) (total acceleration -> vertical; foreground only -> service). Code: `09b51ed`, `c28aed1`, `ae0676a`, `6b6d801`. **Not checked on a device.**

## Context
- 0023 used total acceleration: a phone put down hard counted, against brief I.2.F "eixo vertical"; a dropped phone at rest counted too.
- 0022 and 0023 ran only with the app in front; brief V lists background location.
- Class material: slides 08 (`TrackingService`, foreground service, `startForeground`), sheet P10 (`ProximityService` with `requestLocationUpdates`), slides 10/11 (`TYPE_GRAVITY`, `ACCESS_BACKGROUND_LOCATION` asked separately).

## Decision
- **Vertical only:** `a_v = (a·g)/|g| - |g|` (`utils/ImpactMath.kt`); `g` from `TYPE_GRAVITY`, or a low-pass filter (alpha 0.8) without it. Impact when `|a_v| > ImpactDetector.THRESHOLD`.
- **Speed gate (Q2):** counts only if the last GPS fix is < 10 s old (twice the 5 s interval, review S3) and its speed > 2 m/s (about 7 km/h).
- **Threshold (Q1):** fixed 6 m/s², the constant `ImpactDetector.THRESHOLD` (configurable in code). A Profile slider (3-15, `data/ImpactPrefs`) was added first and removed at the person's request (`53a39e6`): not asked by the brief.
- **One foreground service for both features:** `data/TrackingService`, type `location`, permanent notification; one shared fix stream for the speed and the geofence.
  - Operator: `data/OperatorGeofence` (the D-014 loop moved out of `OperatorProximityViewModel`, which is deleted, Q4).
  - Citizen with detection on: `ImpactDetector` in the service; app in front -> `CityFixApplication.impacts` -> snackbar; otherwise a notification that opens the prefilled report (Q3: in-app detector removed).
- **Start/stop from `CityFixApp`** while visible: needs precise location and (operator or detection on); restarted on change, stopped at Log out. `START_REDELIVER_INTENT`.
- **Background location (Q6):** `BackgroundLocationDialog` explains first, then `ACCESS_BACKGROUND_LOCATION`; once per session, after the location and notification answers; denied -> snackbar, the app keeps working in front.

### Rejected
- **WorkManager:** at least 15 min between runs; cannot read a sensor or a 5 s fix stream.
- **`GeofencingClient`:** 100-150 m minimum radius, minutes of latency; no help for the sensor.
- **Two services:** two notifications and two location requests.
- **User setting (slider, Firestore or SharedPreferences):** not asked by the brief; removed after the first version (`53a39e6`).

## Consequences
- Impact detection now needs precise location; without FINE the service does not start.
- Not restarted after a reboot (no BOOT receiver); the user must open the app.
- OEM battery savers can still kill the service.
- The geofence's `inside` set starts empty on each service start; the stored 1 h throttle still prevents repeats.
- **Diogo (D-014):** `OperatorProximityViewModel` (+Factory) replaced by `OperatorGeofence` in the service; still to be told.

# F-022 docs plan

## Docs to update
- `docs/architecture/overview.md`: `TrackingService` (foreground, type location), `OperatorGeofence`, `ImpactPrefs`, the `impacts` flow.
- `docs/decisions/0022-operator-proximity-alert.md`: note that the watch now runs in the service (supersedes Gate 1 Q1 "foreground only").
- `docs/decisions/0023-impact-detection.md`: mark the listed limits as fixed by 0028.
- Only the parts reached if slices 4-6 are dropped.

## Decisions to record
- `docs/decisions/0028-background-tracking-and-vertical-impact.md`: vertical projection on gravity, 2 m/s speed gate, local threshold (default/range), one foreground service for both features, background location asked after explanation; alternatives (WorkManager, GeofencingClient) rejected.

## Report notes
- Formula `a_v = (a·g)/|g| - |g|` and why total magnitude was wrong (put-down phone).
- Why speed: a stationary drop is not a pothole.
- Android rules: FGS started while visible (12+), `FOREGROUND_SERVICE_LOCATION` (14+), background location separate (10+).
- Improvements: restart after reboot, OEM battery savers, per-account threshold, `GeofencingClient`.
- Tell Diogo: D-014 ViewModel replaced by `OperatorGeofence` in the service.

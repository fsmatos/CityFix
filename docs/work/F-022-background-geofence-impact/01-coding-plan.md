# F-022 coding plan

## Files
| Path | Change | Why |
| --- | --- | --- |
| `utils/ImpactMath.kt` | new | Pure `verticalAcceleration` + low-pass `lowPassGravity` |
| `utils/ImpactDetector.kt` | edit | Gravity + accelerometer, vertical check, speed gate, threshold getter |
| `data/LocationRepository.kt` | edit | `locationFixes(): Flow<Location>`; `locationUpdates()` maps it |
| `data/ImpactPrefs.kt` | new | Threshold in SharedPreferences (`"impact"` file) |
| `viewmodel/ImpactViewModel.kt` | edit | S2: feed speed; S6: only relays `app.impacts` to the snackbar |
| `viewmodel/ProfileViewModel.kt` (+Factory) | edit | `impactThreshold` in UiState, `onThresholdChange` |
| `ui/screens/ProfileScreen.kt` | edit | Slider under `ImpactDetectionRow` when detection is on |
| `ui/components/AccountFields.kt` | edit | `ImpactThresholdSlider` stateless composable |
| `data/OperatorGeofence.kt` | new | Plain class with the `watch()` body moved from the ViewModel |
| `viewmodel/OperatorProximityViewModel.kt` (+Factory) | delete | Replaced by `OperatorGeofence` in the service (Q4) |
| `data/TrackingService.kt` | new | Foreground service type location: geofence + impact |
| `utils/Notifications.kt` | edit | `CHANNEL_TRACKING` (low), `CHANNEL_IMPACT`; `EXTRA_FROM_IMPACT` |
| `CityFixApplication.kt` | edit | `appInForeground`, `impacts: MutableSharedFlow<Unit>` |
| `MainActivity.kt` | edit | Start/stop service, background dialog, `EXTRA_FROM_IMPACT` intent |
| `ui/components/BackgroundLocationDialog.kt` | new | Explanation `AlertDialog` before the request |
| `AndroidManifest.xml` | edit | 3 permissions + `<service foregroundServiceType="location">` |
| `res/values/strings.xml`, `values-pt/strings.xml` | edit | Slider, dialog, notification texts |

All paths under `app/src/main/java/pt/isec/amov/cityfix/` except manifest/res.

## Interfaces
- `fun verticalAcceleration(a: FloatArray, g: FloatArray): Float` = `(a·g)/|g| - |g|`; 0 when `|g|` is 0.
- `fun lowPassGravity(g: FloatArray, a: FloatArray, alpha: Float = 0.8f)` updates `g` in place (`g = α·g + (1-α)·a`).
- `ImpactDetector(context, threshold: () -> Float, speed: () -> Float?)`; `start(onImpact)`, `stop()` unchanged.
- `speed()` returns m/s of the last fix if newer than 5 s, else null; impact needs `speed > MIN_SPEED_MPS` (2f).
- `ImpactPrefs(context)`: `threshold(): Float` (default 6f), `save(value: Float)`; `MIN = 3f`, `MAX = 15f`.
- `LocationRepository.locationFixes(intervalMs = 5_000): Flow<Location>` (same accuracy filter as today).
- `OperatorGeofence(ticketsRepository)`: `suspend fun watch(fixes: Flow<Coordinates>, tickets: StateFlow<List<Ticket>>)`.
- `TrackingService.start(context, operatorId: String?, detectImpacts: Boolean)`, `TrackingService.stop(context)` (companion helpers, `startForegroundService` as P10).
- `CityFixApplication.appInForeground: Boolean` (set in `MainActivity.onStart/onStop`), `impacts: MutableSharedFlow<Unit>(extraBufferCapacity = 1)`.

## Steps
1. **Vertical impact.** Add `ImpactMath.kt`; detector registers `TYPE_GRAVITY` (fallback: low-pass on accelerometer) and `TYPE_ACCELEROMETER`; impact when `|a_v| > 6f` (constant for now); keep cooldown. Commit `feat(F-022): detect impacts on the vertical axis only`. Verify: build, lint; phone flat and upright, jolt up/down triggers, putting phone down sideways less so.
2. **Speed gate.** Add `locationFixes()`; `ImpactViewModel.start()` launches a collector in `viewModelScope` saving last speed+time, `stop()` cancels it; pass `speed` lambda. Commit `feat(F-022): require movement before suggesting an impact`. Verify: build, lint; still jolt -> nothing; walking/driving jolt -> snackbar.
3. **Threshold.** `ImpactPrefs`; Profile slider (`Slider`, `valueRange 3f..15f`, `steps` for 0.5, label with value, contentDescription); detector reads `prefs::threshold` on each event. Commit `feat(F-022): let the citizen set the impact threshold`. Verify: low value triggers easily, high value does not. **Stop point if late.**
4. **Service + geofence.** Manifest permissions + service; `TrackingService` (`onStartCommand` reads extras, `startForeground(id, notif, FOREGROUND_SERVICE_TYPE_LOCATION)` on API 29+, else 2-arg; one location collector shared via `shareIn`; `OperatorGeofence.watch` with `allTickets().stateIn(scope)` when `operatorId != null`; `onDestroy` cancels scope). MainActivity: `LaunchedEffect(operator?.id, detectImpacts, hasPrecise)` starts or stops it; Log out stops it. Delete `OperatorProximityViewModel` (+Factory). Commit `feat(F-022): run the operator geofence in a foreground service`. Verify: operator, Home, walk into 20 m of an IN_PROGRESS ticket -> `lastOperatorNearbyAt` written; Log out -> notification gone.
5. **Background permission.** After the notifications answer, on API 29+, if operator or detection on and not granted: `BackgroundLocationDialog`; "Continue" launches `RequestPermission(ACCESS_BACKGROUND_LOCATION)`; once per session (saveable flag, like `askedNotifications`); denial -> snackbar. Commit `feat(F-022): ask for background location after an explanation`. Verify: dialog then system page; denial keeps the app working.
6. **Impact in the service.** Service owns `ImpactDetector` when `detectImpacts`, speed from the shared fixes; on impact: `app.appInForeground` -> `app.impacts.tryEmit(Unit)`, else notification on `CHANNEL_IMPACT` with `EXTRA_FROM_IMPACT`; `ImpactViewModel` only collects `app.impacts`; remove `ImpactDetectionEffect`; MainActivity navigates to `ReportTicketRoute(fromImpact = true)` when the intent has the extra. Commit `feat(F-022): detect impacts with the app closed`. Verify: closed app, moving jolt -> notification -> prefilled report; in front -> snackbar.

## Conventions
- Follow slides 08 / sheet P10 shapes (`serviceScope`, `startForeground` within 5 s); cite them in KDoc.
- English KDoc, strings in both `strings.xml`; no coordinates in logs.
- No defensive hardening; class-level code only.

## Do not touch
- `model/OperatorProximity.kt` rules and `recordOperatorNearby` (reuse as is).
- Firestore rules, `google-services.json`, F-016 worker and `TicketNotifier`.
- Report form prefill logic (`fromImpact` already exists).

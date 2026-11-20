# F-015 coding plan

## Files
| Path | Change | Why |
| --- | --- | --- |
| `utils/ImpactDetector.kt` | new | Slides' `PotholeDetector` + callback + cooldown |
| `viewmodel/ImpactViewModel.kt` | new | Owns the detector; `impactDetected` state |
| `viewmodel/ImpactViewModelFactory.kt` | new | Same pattern as the other factories |
| `ui/navigation/Destinations.kt` | `ReportTicketRoute` -> data class | Carries `fromImpact` |
| `viewmodel/ReportViewModel.kt`, `ReportViewModelFactory.kt` | `fromImpact` param + prefill | Pavement + DAMAGE |
| `ui/components/ImpactDetectionEffect.kt` | new | Demo `Sensors.kt` lifecycle pattern |
| `MainActivity.kt` (Diogo) | ~15 lines | ViewModel, enabled flag, effect, snackbar, `ReportTicketRoute()` |
| `res/values/strings.xml`, `values-pt/strings.xml` | 2 strings | Message + action |

## Interfaces
Deviation (see state.md): total acceleration with `MIN`/`MAX` replaces the Z axis below.
```kotlin
// utils/ImpactDetector.kt  (slides 10/11, "PotholeDetector")
class ImpactDetector(context: Context) : SensorEventListener {
    fun start(onImpact: () -> Unit)   // accelerometer?.let { registerListener(this, it, SENSOR_DELAY_GAME) }
    fun stop()                        // unregisterListener(this)
    companion object {
        const val Z_MAX = 14.0f       // Q2, slide values (m/s²)
        const val Z_MIN = 6.0f
        const val COOLDOWN_MS = 30_000L  // Q4
    }
}

// viewmodel/ImpactViewModel.kt
class ImpactViewModel(context: Context) : ViewModel() {
    val impactDetected: StateFlow<Boolean>
    fun start(); fun stop(); fun onSuggestionShown()
    override fun onCleared()          // detector.stop()
}

// ui/components/ImpactDetectionEffect.kt
@Composable fun ImpactDetectionEffect(enabled: Boolean, onStart: () -> Unit, onStop: () -> Unit)

// Destinations.kt
@Serializable data class ReportTicketRoute(val fromImpact: Boolean = false)
```

`onSensorChanged` as on the slide: `val z = event.values[2]`; if `z > Z_MAX || z < Z_MIN` and `now - lastImpact > COOLDOWN_MS`, set `lastImpact` and call `onImpact()`.

## Steps
1. **Detector and ViewModel (not wired).**
   - Add `ImpactDetector`, `ImpactViewModel` (creates the detector with `applicationContext`), `ImpactViewModelFactory`.
   - KDoc cites brief I.2.F and the slide; one `Log.d("CityFix", ...)` in `start`/`stop` for acceptance check 2.
   - Commit `feat(F-015): add vertical impact detector`.
   - Verify: `./gradlew assembleDebug`, `./gradlew lint`.
2. **Prefill route.**
   - `ReportTicketRoute` becomes a data class; `navigate(ReportTicketRoute)` becomes `ReportTicketRoute()` (top bar "+"); `hasRoute<ReportTicketRoute>()` stays.
   - In the destination, read `entry.toRoute<ReportTicketRoute>().fromImpact` and pass it via the factory.
   - ViewModel `init`: if `fromImpact`, `situation = DAMAGE`; in the categories `collect`, if `categoryId` is blank and the list has id `"pavement"` (Q8), select it. A later user change is kept.
   - Commit `feat(F-015): prefill the report form for an impact`.
   - Verify: build + lint; temporarily navigate with `ReportTicketRoute(true)`, check, revert.
3. **Wire detection and suggestion.**
   - `ImpactDetectionEffect`: copy of the demo's `DisposableEffect(lifecycleOwner, enabled)` with `LifecycleEventObserver` (ON_START -> `onStart`, ON_STOP -> `onStop`), start at once if already STARTED, `onDispose` removes the observer and calls `onStop()`. Does nothing when not enabled.
   - `MainActivity`:
     - `impactViewModel` via its factory.
     - `val detectImpacts = role == AccessRole.CITIZEN && (session.ticketsUser as? Citizen)?.impactDetection == true`.
     - `ImpactDetectionEffect(detectImpacts, impactViewModel::start, impactViewModel::stop)`.
     - `LaunchedEffect(impactDetected)` (slides: "showing a Snackbar" is a side-effect): if true, `onSuggestionShown()`; if the screen is not REPORT, `showSnackbar(message, actionLabel, withDismissAction = true, duration = Long)` in `snackbarScope`; on `ActionPerformed`, `navigate(ReportTicketRoute(fromImpact = true)) { launchSingleTop = true }`.
   - Strings: `impact_suggestion` ("Possible pothole detected. Report it?" / "Possivel buraco detetado. Reportar?"), `impact_report_action` ("Report" / "Reportar").
   - Commit `feat(F-015): suggest a pavement report after an impact`.
   - Verify:
     - Build + lint.
     - Emulator, active citizen with the setting on: Extended controls > Virtual sensors (slide "Testing"), or `adb emu sensor set acceleration 0:0:20` then `0:0:9.8`: one snackbar; repeat at once: none.
     - Home: Logcat "stop"; back: "start". Log out / setting off (console edit): "stop".
     - TalkBack reads message and action. Real phone flat on a table: tap it hard.

## Conventions
- Sensor only in `utils` + ViewModel (kotlin rules); strings EN/PT; no new permission (50 Hz is below the Android 12 `HIGH_SAMPLING_RATE_SENSORS` limit).
- Plain code, no defensive branches beyond "no sensor".
- Conventional Commits with `F-015`, no attribution.

## Do not touch
- Registration / profile code (D-010, D-011), `Citizen.kt`, Firestore rules, `google-services.json`.
- `ReportScreen.kt` (location is already read when the form opens).
- The rest of `MainActivity` (gate, redirect, permissions).

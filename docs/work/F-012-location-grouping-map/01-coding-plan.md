# F-012 coding plan

> **Changed after Gate 1 (2026-11-14, the person's choice; see state.md):** (1) Google Maps Compose `maps-compose` 6.1.0 as in Contacts_#3 instead of osmdroid (removed); (2) the Maps API key in `AndroidManifest.xml` as `com.google.android.geo.API_KEY` meta-data, like class (placeholder committed, the person pastes the key); (3) location requested at app start in `MainActivity` (`LaunchedEffect(Unit)` + `RequestMultiplePermissions`), no rationale panel; denial still shows a message with Try again and Open settings. Text below that contradicts these is superseded.

Effect on the steps: slice 1 got a follow-up commit (osmdroid out, `maps-compose` 6.1.0 and the manifest meta-data in). Slice 3: no `LocationPermissionPanel` and no `NEEDS_PERMISSION`; `MainActivity` owns the start-up launcher and passes the answer to `LocationGateViewModel.onPermissionResult`; the gate's Try again relaunches it; a shared `LocationDeniedMessage` (Try again + Open settings) is used by the gate and the form. Slice 4: the form checks the permission when it opens and has its own launcher only for Try again. Slice 6: `TicketMap` (Google Maps) replaces `OsmMap`; no change in `CityFixApplication`.

## Files
| Path | Change | Why |
| --- | --- | --- |
| `gradle/libs.versions.toml` | add `playServicesLocation = "21.3.0"`, `osmdroid = "6.1.20"`, libraries `play-services-location`, `osmdroid-android` | P10 slide 4; decision 0012 Q6 |
| `app/build.gradle.kts` | `implementation(libs.play.services.location)`, `implementation(libs.osmdroid.android)` | same |
| `app/src/main/AndroidManifest.xml` (Diogo's) | `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `INTERNET` | P10 slide 5; osmdroid tiles |
| `model/AnonymousArea.kt` (new) | `PRACA_REPUBLICA`, `ANONYMOUS_RADIUS_METERS`, `isInsideAnonymousArea()` | brief I.1, pure rule |
| `utils/LocationPermissions.kt` (new) | `LOCATION_PERMISSIONS`, `hasLocationPermission(context)` | shared by gate and form (class 02 check) |
| `data/LocationRepository.kt` (new) | `suspend fun currentLocation(): Coordinates?` over `FusedLocationProviderClient` | P10; reused by D-014 |
| `CityFixApplication.kt` | `locationRepository by lazy`; osmdroid `Configuration` in `onCreate()` | container (decision 0009); OSM tile policy |
| `ui/components/LocationPermissionPanel.kt` (new) | stateless reason + Allow / Try again / Open settings | rationale first, denial |
| `viewmodel/LocationGateViewModel.kt` (new) | `GateStatus`, `LocationGateUiState`, ViewModel, factory (one file, small) | gate logic out of the UI |
| `ui/screens/LocationGateScreen.kt` (new) | stateful `LocationGateScreen` + stateless `LocationGateContent` + previews | gate UI |
| `MainActivity.kt` (Diogo's) | gate wrapper; `ReportViewModelFactory` gets `app.locationRepository`; snackbar per `ReportOutcome` | wiring |
| `viewmodel/ReportUiState.kt` | `location`, `isLocating`, `locationPermissionDenied`, `locationError`; `isValid` needs a location | report location |
| `viewmodel/ReportViewModel.kt` | `locationRepository` parameter; `onLocationPermissionResult`, `refreshLocation`; `submit(onSuccess: (ReportOutcome) -> Unit)` | report location, outcome |
| `viewmodel/ReportViewModelFactory.kt` | `locationRepository` parameter | same |
| `ui/screens/ReportScreen.kt` | permission launcher (above the orientation branch, like the others); `LocationSection` in `DetailsPane`; previews get a location | report location |
| `data/TicketsRepository.kt` | `ReportOutcome` enum; `reportTicket(..., location, creatorId): ReportOutcome?` | grouping result |
| `data/FirestoreTicketsRepository.kt` | location in the ticket; grouping query + `findNearby` + join batch | brief I.2 Obs |
| `data/FirestoreMapping.kt` | `REPORTS` constant; `toTicketLocation()`; `toReportDocument()`; `Attachment.toDocument()` internal; `ticketLocations.active = isActive` | grouping |
| `ui/components/OsmMap.kt` (new) | `OsmMap(location, exact, modifier)` | P10 slide 32 |
| `ui/components/CoarseLocationMap.kt` | the box becomes `OsmMap(location, exact = false)` | brief I.1.C |
| `res/values/strings.xml`, `res/values-pt/strings.xml` | new strings (below), EN + PT in the same commit | brief V |

## Interfaces
```kotlin
// model/AnonymousArea.kt (pure Kotlin; imports only utils.haversineMeters)
val PRACA_REPUBLICA = Coordinates(40.2100, -8.4190)
const val ANONYMOUS_RADIUS_METERS = 10_000.0
fun isInsideAnonymousArea(location: Coordinates): Boolean   // (0, 0) -> false

// utils/LocationPermissions.kt
val LOCATION_PERMISSIONS = arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)
fun hasLocationPermission(context: Context): Boolean          // either one granted

// data/LocationRepository.kt
class LocationRepository(private val context: Context) {
    suspend fun currentLocation(): Coordinates?               // null: no permission or no fix
}

// data/TicketsRepository.kt
enum class ReportOutcome { CREATED, JOINED, ALREADY_FOLLOWING }
suspend fun reportTicket(
    description: String, categoryId: String, categoryName: String,
    situation: Situation, scope: Scope, attachments: List<Attachment>,
    location: Coordinates, creatorId: String,
): ReportOutcome?                                             // null: refused (blank field, no attachment, (0, 0))

// viewmodel/LocationGateViewModel.kt
enum class GateStatus { CHECKING, NEEDS_PERMISSION, DENIED, NO_FIX, OUTSIDE, INSIDE }
data class LocationGateUiState(val status: GateStatus = GateStatus.CHECKING)
class LocationGateViewModel(private val locationRepository: LocationRepository) : ViewModel() {
    val uiState: StateFlow<LocationGateUiState>
    fun onPermissionResult(granted: Boolean)                  // granted -> check(); else DENIED
    fun onPermissionMissing()                                 // NEEDS_PERMISSION (first open, not granted)
    fun check()                                               // CHECKING -> INSIDE / OUTSIDE / NO_FIX
}
class LocationGateViewModelFactory(private val locationRepository: LocationRepository)

// ui/screens/LocationGateScreen.kt
@Composable fun LocationGateScreen(viewModel: LocationGateViewModel, onLoginClick: (() -> Unit)?, modifier: Modifier = Modifier)
@Composable fun LocationGateContent(state: LocationGateUiState, onAllowClick: () -> Unit,
    onTryAgainClick: () -> Unit, onOpenSettingsClick: () -> Unit, onLoginClick: (() -> Unit)?, modifier: Modifier = Modifier)

// ui/components/LocationPermissionPanel.kt
@Composable fun LocationPermissionPanel(denied: Boolean, onAllowClick: () -> Unit,
    onOpenSettingsClick: () -> Unit, modifier: Modifier = Modifier)

// ui/components/OsmMap.kt
@Composable fun OsmMap(location: Coordinates, exact: Boolean, modifier: Modifier = Modifier)

// ReportViewModel
fun onLocationPermissionResult(granted: Boolean)
fun refreshLocation()
fun submit(onSuccess: (ReportOutcome) -> Unit)
```

## Steps

### Slice 1: dependencies and manifest
- Catalog: `playServicesLocation = "21.3.0"` (comment: P10 version), `osmdroid = "6.1.20"`; libraries `play-services-location = { group = "com.google.android.gms", name = "play-services-location", version.ref = "playServicesLocation" }` (P10 slide 4, as written) and `osmdroid-android = { group = "org.osmdroid", name = "osmdroid-android", version.ref = "osmdroid" }`. Both `implementation(...)` lines in `app/build.gradle.kts`, kept in alphabetical order.
- Manifest, above `<application>`: the three `uses-permission` lines.
- Check: `./gradlew assembleDebug`; open `app/build/intermediates/merged_manifests/debug/.../AndroidManifest.xml` (or the Merged Manifest tab) and look for `WRITE_EXTERNAL_STORAGE` / `READ_EXTERNAL_STORAGE` from osmdroid; if present add `<uses-permission android:name="..." tools:node="remove" />` (R2). `./gradlew lint`: 0 errors; expected new warnings `GradleDependency`/`NewerVersionAvailable` for play-services-location (21.4.0 exists); record the count.
- Commit: `build(F-012): add location and osmdroid libraries and location permissions`.

### Slice 2: location building blocks
- `model/AnonymousArea.kt`: KDoc cites brief I.1 and says the coordinates are approximate; `isInsideAnonymousArea` returns false for (0, 0), else `haversineMeters(...) <= ANONYMOUS_RADIUS_METERS`.
- `utils/LocationPermissions.kt`: `ContextCompat.checkSelfPermission(context, it) == PERMISSION_GRANTED` for any of the two.
- `data/LocationRepository.kt`: `private val client = LocationServices.getFusedLocationProviderClient(context)` (P10). In `currentLocation()`: `if (!hasLocationPermission(context)) return null` written as explicit `checkSelfPermission` calls if lint's `MissingPermission` does not accept the helper; then `client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()?.let { Coordinates(it.latitude, it.longitude) }`. KDoc: one fix, not updates (beyond P10; D-014 adds updates here).
- `CityFixApplication`: `val locationRepository: LocationRepository by lazy { LocationRepository(this) }`.
- Check: build, lint (no `MissingPermission`). No caller yet.
- Commit: `feat(F-012): add location repository and anonymous area rule`.

### Slice 3: permission panel and anonymous gate
- `LocationPermissionPanel`: a `Column` with the reason text (`location_permission_reason`), then `denied == false`: Button "Allow location"; `denied == true`: the denied text, Button "Try again" (calls `onAllowClick`) and OutlinedButton "Open settings". Material buttons give 48dp targets.
- `LocationGateViewModel`: `MutableStateFlow` + `asStateFlow()` (as the other ViewModels). `check()` sets CHECKING and launches in `viewModelScope`: `val here = locationRepository.currentLocation()`; `null` -> NO_FIX; else INSIDE/OUTSIDE by `isInsideAnonymousArea(here)`. The fix is not stored (the gate needs only the answer).
- `LocationGateScreen` (stateful): launcher `rememberLauncherForActivityResult(RequestMultiplePermissions()) { result -> viewModel.onPermissionResult(result.values.any { it }) }` (P10 slide 24); `LaunchedEffect(Unit) { if (hasLocationPermission(context)) viewModel.check() else viewModel.onPermissionMissing() }` (runs once per composition entry; a rotation recreates it, so `check()` returns at once when status is already INSIDE/OUTSIDE, keeping the answer). Open settings: `Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))`. "Try again" from DENIED launches the permission request again if still not granted, else `check()`.
- `LocationGateContent` (stateless), centred scrolling `Column` (works in landscape and large fonts): app name, then by status: CHECKING "Checking your location..." + `CircularProgressIndicator`; NEEDS_PERMISSION / DENIED `LocationPermissionPanel`; NO_FIX text + Try again; OUTSIDE "CityFix without an account is only available within 10 km of Praça da República." + Try again; "Log in" `OutlinedButton` under it only when `onLoginClick != null`. Previews: each status, EN + PT, one landscape.
- `MainActivity.CityFixApp` (tell Diogo): create `val gateViewModel: LocationGateViewModel = viewModel(factory = LocationGateViewModelFactory(app.locationRepository))` and collect its state next to the tickets ViewModel; wrap the existing `Scaffold` in `if (app.currentUser == null && gateState.status != GateStatus.INSIDE) { LocationGateScreen(gateViewModel, onLoginClick = null /* D-009 */, Modifier.fillMaxSize().safeDrawingPadding()) } else { Scaffold(...) }`. Comment: "No login yet (D-009): null user = anonymous, brief I.1".
- Strings: `gate_checking`, `gate_outside`, `gate_no_fix`, `gate_try_again`, `gate_log_in`, `location_permission_reason`, `location_permission_denied`, `location_permission_allow`, `location_open_settings`.
- Check: build, lint; device checks D1-D5.
- Commit: `feat(F-012): add location permission and 10 km anonymous gate`.

### Slice 4: report location
- `ReportUiState`: `val location: Coordinates? = null`, `val isLocating: Boolean = false`, `val locationPermissionDenied: Boolean = false`, `@StringRes val locationError: Int? = null`; `isValid` adds `location != null`.
- `ReportViewModel`: new constructor parameter `locationRepository: LocationRepository` (between `attachmentStorage` and `creatorId`). `onLocationPermissionResult(granted)`: denied -> `locationPermissionDenied = true`, `locationError = R.string.location_permission_denied`; granted -> clear both, `refreshLocation()`. `refreshLocation()`: return if `isLocating`; set `isLocating = true`, launch, then `location = fix ?: it.location`, `locationError = if (fix == null) R.string.report_location_unavailable else null`, `isLocating = false`, `withSummaryRefreshed()`. `submit`: `locationError = if (state.location == null) (state.locationError ?: R.string.report_error_location) else null` in `checked`; the `location` local is passed to `reportTicket`. KDoc of the class updated.
- `ReportViewModelFactory`: pass it through. `MainActivity`: `ReportViewModelFactory(app.ticketsRepository, app.attachmentStorage, app.locationRepository, app.reporterId)`.
- `ReportScreen`: the permission launcher next to the media launchers (same position rule); `LaunchedEffect(Unit) { if (hasLocationPermission(context)) viewModel.onLocationPermissionResult(true) }` only when `state.location == null` (rotation does not fetch again); `ReportContent` gets `onAllowLocationClick`, `onOpenSettingsClick`, `onRefreshLocationClick`. New private `LocationSection` in `DetailsPane` before Visibility: heading "Location"; no permission -> `LocationPermissionPanel`; `isLocating` -> "Getting your location..."; a fix -> `"%1$.5f, %2$.5f"` (`report_location_value`) and an OutlinedButton "Refresh location"; `locationError` as error text with `semantics { error(...) }` like the other fields. Whether the panel shows: `!hasPermission` read in `ReportScreen` and passed as a `Boolean` (`showLocationPermission`), because `ReportContent` takes no Context.
- `TicketsRepository.reportTicket` / `FirestoreTicketsRepository.reportTicket`: new `location: Coordinates` parameter before `creatorId`; the refusal check adds `location == Coordinates()`; the ticket gets `location = location`. Return type stays `String?` in this slice (outcome in slice 5). KDoc drops "No location yet (F-012)".
- Previews: existing `ReportUiState` previews get `location = Coordinates(40.19250, -8.41150)`; add one "no permission" and one "locating" preview in EN and PT.
- Strings: `report_location_title`, `report_location_value`, `report_location_locating`, `report_location_refresh`, `report_location_unavailable`, `report_error_location`.
- Check: build, lint; D6-D8.
- Commit: `feat(F-012): capture the GPS location in the report form`.

### Slice 5: 10 m grouping
- `TicketsRepository.kt`: `enum class ReportOutcome { CREATED, JOINED, ALREADY_FOLLOWING }` with KDoc; `reportTicket` returns `ReportOutcome?`.
- `FirestoreMapping.kt`: `internal const val REPORTS = "reports"`; `ticketLocations` `"active" to isActive` (comment: same rule as `findNearby`, decision 0005; amends 0010); `internal fun DocumentSnapshot.toTicketLocation(): Ticket` (id, `categoryId`, `situation`, `location`, `currentStatus = if (getBoolean("active") == true) OPEN else CLOSED`; KDoc: "a minimal ticket so findNearby can be reused; not a real ticket, never store or show it"); `internal fun reportDocument(categoryId, situation, location, description, createdAt, attachments): Map<String, Any?>` (decision 0010 fields plus `attachments`, each via `Attachment.toDocument()` as PENDING with `uploadedById = creatorId`; Q5); `Attachment.toDocument()` becomes `internal`.
- `FirestoreTicketsRepository.reportTicket`, after the EXIF strip:
  ```kotlin
  val match = findMatch(location, cleanCategoryId, situation)
  if (match != null) return@withContext joinTicket(match, ...)
  // else: the current create batch, returning ReportOutcome.CREATED
  ```
  - private `suspend fun findMatch(location, categoryId, situation): Ticket?`: the three-`whereEqualTo` query on `TICKET_LOCATIONS`, `.get().await()`, `TicketsList(docs.map { it.toTicketLocation() }).findNearby(location, categoryId, situation)`. Wrapped in one `try { } catch (e: Exception) { Log.w(TAG, "grouping query failed", e); null }` so a failed query falls back to creating a ticket (00-plan, Failure modes).
  - private `suspend fun joinTicket(match, ...)`: `val mine = firestore.collection(TICKETS).whereArrayContains("participantIds", creatorId).get().await()`; if `mine.documents.any { it.id == match.id }` return `ALREADY_FOLLOWING`. Else one `firestore.batch()`: `set(ticketRef.collection(REPORTS).document(creatorId), reportDocument(..., attachments))`; `update(ticketRef, mapOf("followerIds" to FieldValue.arrayUnion(creatorId), "participantIds" to FieldValue.arrayUnion(creatorId)))`; `commit().addOnFailureListener { Log.w(...) }` (not awaited, as F-011); return `JOINED`. The ticket's `attachments` are not touched (Q5). No `stats/summary`, no `publicTickets`.
  - Class KDoc: the grouping, the query, and why no geo query / composite index.
- `ReportViewModel.submit(onSuccess: (ReportOutcome) -> Unit)`: non-null outcome -> `isSaving = false`, and for `ALREADY_FOLLOWING` delete the draft's local files (nothing references them), then `onSuccess(outcome)`.
- `MainActivity` (tell Diogo): `onSubmitted = { outcome -> val message = when (outcome) { CREATED -> reportSubmittedMessage; JOINED -> reportJoinedMessage; ALREADY_FOLLOWING -> reportAlreadyFollowingMessage }; ... }` with the two new `stringResource` values read next to `reportSubmittedMessage`; `ReportScreen.onSubmitted` type becomes `(ReportOutcome) -> Unit`.
- Strings: `report_joined` ("This issue was already reported nearby. You now follow it." / "Esta ocorrência já tinha sido reportada perto daqui. Passou a acompanhá-la."), `report_already_following` ("You already follow this issue nearby." / "Já acompanha esta ocorrência perto daqui.").
- Check: build, lint; D9-D11 (incl. D10b).
- Commit: `feat(F-012): group reports within 10 m into one ticket`.

### Slice 6: map
- `CityFixApplication.onCreate()`, before the seed: `Configuration.getInstance().apply { userAgentValue = packageName; osmdroidBasePath = File(cacheDir, "osmdroid"); osmdroidTileCache = File(osmdroidBasePath, "tiles") }` (import `org.osmdroid.config.Configuration`; comment: OSM tile policy needs a User-Agent; default path is shared storage).
- `OsmMap`:
  - `if (LocalInspectionMode.current)`: the old placeholder `Box` (`surfaceVariant`, `LocationOn` icon) so previews render.
  - else a `Box(modifier)` with `AndroidView(factory = { ctx -> MapView(ctx).apply { setTileSource(TileSourceFactory.MAPNIK); zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER); setMultiTouchControls(exact); overlays.add(CopyrightOverlay(ctx)) } }, update = { map -> map.controller.setZoom(if (exact) 17.0 else 14.0); map.controller.setCenter(point); replace the previous circle/marker overlay (keep the CopyrightOverlay); map.invalidate() }, onRelease = { it.onDetach() }, modifier = Modifier.matchParentSize())`.
  - Coarse: `Polygon(map).apply { points = Polygon.pointsAsCircle(point, COARSE_RADIUS_METERS /* 600.0 */); fillPaint.color = primary with alpha; outlinePaint.color = primary }`, then a `Box(Modifier.matchParentSize().pointerInput(Unit) { })` on top: it receives the touches (so the map never moves) and consumes nothing (so the page still scrolls). Exact: `Marker(map).apply { position = point; setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM) }`, no cover.
  - Semantics: `contentDescription` = `cd_ticket_map` on the outer `Box` (unchanged string).
- `CoarseLocationMap`: replace the placeholder `Box` with `OsmMap(location, exact = false, Modifier.fillMaxWidth().height(200.dp).clip(MaterialTheme.shapes.medium).semantics { contentDescription = mapDescription })`; KDoc: real map since F-012. Previews unchanged (placeholder via `LocalInspectionMode`).
- Check: build, lint; D12-D14.
- Commit: `feat(F-012): show the coarse location on an OpenStreetMap map`.

### Device checks (emulator; run after the slice that adds each)
Locations: `adb emu geo fix <lon> <lat>` (P10 slide 30). Inside: ISEC `-8.4115 40.1925`. Outside: Figueira da Foz `-8.8600 40.1500` (about 38 km). Set the fix **before** the check (the emulator has no fix after a cold boot, which is D4).
| # | Setup | Expected |
| --- | --- | --- |
| D1 | fresh install, `currentUser = null`, fix inside | system dialog at start -> allow -> "Checking your location" -> dashboard |
| D2 | fix outside, restart app | gate "only within 10 km", no Log in button; Try again after setting an inside fix -> dashboard |
| D3 | deny in the dialog at start (then again, "don't ask again") | denied text, Try again (shows the dialog again until "don't ask again"), Open settings opens the app page; no crash |
| D4 | permission granted, location off in the emulator / no fix | NO_FIX text; no crash |
| D5 | `currentUser = Citizen(DEV_CITIZEN_ID, registrationStatus = ACTIVE)`, fix outside | no gate, dashboard |
| D6 | report form with permission | "Getting your location..." then coordinates; Refresh after a new geo fix shows the new values |
| D7 | report form, permission denied (citizen stand-in, permission reset in settings) | panel in the form; Submit shows the location error and the summary; no crash |
| D8 | rotate the form with a fix | location kept, no new fetch |
| D9 | report A (category X, situation S) at `-8.4115 40.192500` as `dev-citizen-1` | new ticket; console: `tickets`, `ticketLocations` (exact), `publicTickets.coarseLocation` = (40.19, -8.41); dashboard total +1 |
| D10 | rebuild with `currentUser = Citizen("dev-citizen-2", registrationStatus = ACTIVE)`, fix `-8.4115 40.192545` (about 5 m), same X and S | snackbar "already reported nearby"; console: no new ticket; ticket A `followerIds = [dev-citizen-2]`, `participantIds` has both, `reports/dev-citizen-2` exists with its description and attachments; ticket A's `attachments` and `publicTickets/A` unchanged; dashboard total unchanged |
| D10b | as D10 but the second report chosen PRIVATE while ticket A is PUBLIC | joins A; `reports/dev-citizen-2` has the photo; ticket A's `attachments` and `publicTickets/A.thumbnails` unchanged; anonymous detail of A unchanged |
| D11 | D10 again with the same id; then a different situation at the same spot; then the same X+S 50 m away (`40.192950`) | "already follow"; new ticket; new ticket |
| D12 | anonymous detail of ticket A (scope PUBLIC), real Maps key pasted | Google map, circle around (40.19, -8.41), far zoom (14), no marker, no zoom buttons, no coordinates text; drag/pinch do not move it; the page scrolls when dragging over it. With the placeholder key: grey map with the Google logo, no crash |
| D13 | rotate the detail; landscape | map redraws in the right pane, no crash |
| D14 | airplane mode, detail | cached tiles or a grey map; no crash |

## Conventions
- Kotlin conventions skill and `.claude/rules/kotlin.md`: MVVM, stateful/stateless split, strings EN + PT in the same commit, 48dp targets, content descriptions, portrait and landscape.
- Class patterns, named in KDoc where used: P10 (`getFusedLocationProviderClient`, `RequestMultiplePermissions`, `AndroidView { MapView }`, `adb emu geo fix`), class 02 (`checkSelfPermission`), F-011 (batch, not-awaited `commit()`, `Log.w`).
- Beyond class, flagged in KDoc: `getCurrentLocation` + `await()`, osmdroid `Configuration`, `CopyrightOverlay`, `Polygon.pointsAsCircle`, `FieldValue.arrayUnion`, the touch cover.
- Short code, no hardening: the only `try`/`catch` is the grouping query fallback.
- Commits: Conventional Commits with `F-012`, no attribution lines.

## Do not touch
- `firestore.rules`, `storage.rules`, `firebase.json` (not deployed, decision 0012 Q5 amended; data shape kept instead).
- `app/google-services.json`, keys; no Google Maps key.
- `model/TicketsList.kt` (`findNearby` is reused as is; Diogo's file), `model/TicketPrivacy.kt`, `model/TicketLifecycle.kt`, `TicketsViewModel(+Factory).kt`.
- `ShowScreen.kt` (keeps calling `CoarseLocationMap`), `DebugSeed.kt`, `SampleTickets.kt`.
- Background location, services, geofence (D-014).

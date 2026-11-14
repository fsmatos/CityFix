# F-012: Location, permissions, 10 km gate, 10 m grouping and map
Type: feature · Task file: docs/tasks/phase-2/F-012-location-and-map.md
Labels: (none)

> **Changed after Gate 1 (2026-11-14, the person's choice; see state.md):** (1) Google Maps Compose `maps-compose` 6.1.0 as in Contacts_#3 instead of osmdroid (removed); (2) the Maps API key in `AndroidManifest.xml` as `com.google.android.geo.API_KEY` meta-data, like class (placeholder committed, the person pastes the key); (3) location requested at app start in `MainActivity` (`LaunchedEffect(Unit)` + `RequestMultiplePermissions`), no rationale panel; denial still shows a message with Try again and Open settings. Text below that contradicts these is superseded.

## Problem
- Brief I.1: anonymous citizens may use the app only within 10 km of Pr. Republica. Today nothing checks where the device is.
- Brief I.2 Obs: a report with the same category and situation within 10 m of an active ticket must join it ("ocorrencia de grupo"; Option A, decision 0002). Today `reportTicket` has no location (every ticket and `ticketLocations` document is at (0, 0)), so `TicketsList.findNearby` never runs.
- Brief I.1.C: the anonymous detail needs a "mapa com zoom afastado". `CoarseLocationMap` is a static placeholder (F-005 Gate 1 Q7).
- Brief V: location permission must be requested and validated at run time. The app has no location permission at all.

## Outcome
- A `LocationRepository` (FusedLocationProviderClient, as P10) gives the current GPS fix as a suspend call; Diogo reuses it in D-014.
- Foreground location (`ACCESS_FINE_LOCATION` + `ACCESS_COARSE_LOCATION`) is requested when the app starts, as in class (no explanation first; changed after Gate 1), and a denial shows a clear message with "Try again" and "Open settings". Background location stays in D-014.
- While nobody is logged in (`app.currentUser == null`, the F-011 seam), the app starts with a location check: inside 10 km of Pr. Republica the normal app shows; outside, with location denied or with no fix, a blocking gate screen shows instead.
- The report form gets the GPS fix, shows it, and cannot be submitted without one; the ticket, `ticketLocations` and `publicTickets` (rounded, decision 0006) store it.
- Before creating a ticket, the repository looks for an active ticket with the same category and situation within 10 m (through `ticketLocations`). If one exists, the reporter joins it (one batch: `tickets/{id}/reports/{uid}` with the reporter's description and attachments, `arrayUnion` on `followerIds` and `participantIds`; the shared ticket's attachments are not touched) and the snackbar says so; no new ticket and no counter change.
- `CoarseLocationMap` draws a real Google map (Maps Compose, changed after Gate 1): far zoom, no gestures, a ~600 m circle, never a pin. A reusable `TicketMap` also has an exact mode (marker, zoom 15, gestures) for D-013.

## Acceptance checks
Run on an emulator; locations are set with `adb emu geo fix <longitude> <latitude>` (P10 slide 30; longitude first) or the emulator's Extended Controls > Location. Full list (D1-D14) in `01-coding-plan.md`.
- [ ] Location outside 10 km (for example Figueira da Foz, `adb emu geo fix -8.8600 40.1500`): with `currentUser = null` the gate shows; with a citizen stand-in in `CityFixApplication.currentUser` it does not.
- [ ] Location inside 10 km (ISEC, `adb emu geo fix -8.4115 40.1925`): anonymous user sees the dashboard.
- [ ] Two reports with the same category and situation about 5 m apart (`40.192500` and `40.192545`, same longitude), the second from another reporter id, give one ticket with one follower (`followerIds` = the second id) and one `reports/{secondId}` document; the dashboard total grows by 1, not 2.
- [ ] Denying location (gate and report form) shows a clear message, no crash; "Open settings" opens the app's settings page.
- [ ] The anonymous detail map shows a circle at a far zoom, never a pin, never coordinate text; it cannot be panned or zoomed (grey tiles until the person pastes the Maps key).
- [ ] Build and lint pass after every slice (0 errors).

## Approach
Follows class P10 ("GPS, Maps & Adaptive UI") and class 02 (runtime permissions); departures are marked **beyond P10**.

**Libraries (need the person's OK at Gate 1):**
- `com.google.android.gms:play-services-location` **21.3.0**: P10's exact version (21.4.0 is the newest on Google Maven, checked 2026-11-13; kept at the class version, lint will flag it as a newer-version warning).
- `org.osmdroid:osmdroid-android` **6.1.20**: newest on Maven Central (checked 2026-11-13); P10 slide 32 "Open-Source Alternative". Decision 0012 Q6 already chose osmdroid.
- No new library for `await()`: `kotlinx-coroutines-play-services` 1.11.0 is already in the build (F-010).

**Location (`data/LocationRepository`).** `LocationServices.getFusedLocationProviderClient(context)` as in P10's `ProximityService`. One function, `suspend fun currentLocation(): Coordinates?`: returns `null` without the permission (a `checkSelfPermission` in the same function, which lint's `MissingPermission` check also needs), otherwise `getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()` mapped to `Coordinates`, `null` when there is no fix. **Beyond P10:** P10 uses `requestLocationUpdates` with a `LocationCallback` in a service (continuous updates); a report and a gate need one fix, so `getCurrentLocation` + `await()` is shorter and needs no cleanup. D-014 can add `requestLocationUpdates` to the same class. Held by `CityFixApplication` `by lazy`, like the other repositories.

**Permissions.** Manifest: `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` (P10 slide 5) and `INTERNET` (osmdroid tiles; already merged from Firebase, declared now because our code uses it). Request with `rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions())` asking for both, granted if either is true (P10 slide 24, line for line). Check first with `ContextCompat.checkSelfPermission` (class 02). **Departure from P10:** P10 launches the request from `LaunchedEffect(Unit)` as soon as the screen opens; the project rule (`.claude/rules/kotlin.md`, "rationale first; handle denial and don't ask again") wants an explanation first, so a small stateless `LocationPermissionPanel` shows the reason and an "Allow location" button, and after a denial the reason, "Try again" and "Open settings" (`Settings.ACTION_APPLICATION_DETAILS_SETTINGS`). "Open settings" covers "don't ask again" without `shouldShowRequestPermissionRationale` (which needs the Activity).

**10 km gate.**
- Centre and radius: **constants in code** (recommended, Q1): `model/AnonymousArea.kt` with `PRACA_REPUBLICA = Coordinates(40.2100, -8.4190)` (approximate, from public listings of the square; to confirm on openstreetmap.org), `ANONYMOUS_RADIUS_METERS = 10_000.0` and the pure rule `fun isInsideAnonymousArea(location: Coordinates): Boolean` over `haversineMeters`. (0, 0) counts as outside. `municipality/main` does not exist in the database and reading it would add a listener, a missing-document case and an offline first-start case for a value that never changes.
- `viewmodel/LocationGateViewModel` + `LocationGateUiState(status)` with `GateStatus { CHECKING, NEEDS_PERMISSION, DENIED, NO_FIX, OUTSIDE, INSIDE }`; events `onPermissionResult(granted)` and `check()` (reads `LocationRepository`, applies the rule). Stateful `LocationGateScreen(viewModel)` (launcher, permission check) over stateless `LocationGateContent(state, ...)`, in `ui/screens/`.
- Wiring (`MainActivity.kt`, Diogo's file): in `CityFixApp`, `if (app.currentUser == null && gateState.status != GateStatus.INSIDE) LocationGateScreen(...) else` the current `Scaffold`. The gate ViewModel is created above that `if` with its factory, so it survives rotation. The check runs once per app start and on "Try again"; moving out of the area later does not lock the app (Q2).
- "Log in": shown only when an `onLoginClick` is passed; until D-009 `MainActivity` passes `null`, so there is no dead button (Q3). D-009 wires it to the login route.

**Report location.**
- `ReportUiState` gains `location: Coordinates?`, `isLocating`, `locationPermissionDenied` and `@StringRes locationError`; `isValid` also needs `location != null`.
- `ReportViewModel(repository, attachmentStorage, locationRepository, creatorId)` gains `onLocationPermissionResult(granted)` (granted starts `refreshLocation()`) and `refreshLocation()` (sets `isLocating`, then the fix or `report_location_unavailable`). `submit` sets `report_error_location` when there is no fix.
- `ReportScreen` checks the permission when it opens (granted: fetch at once; not granted: show `LocationPermissionPanel`). The new "Location" section shows "Getting your location...", the reporter's own coordinates with 5 decimals (their own position, not someone else's data) and a "Refresh location" button. The fix is taken when the form opens and on refresh, not at the Submit tap, so Submit stays instant (Q4).
- `reportTicket(..., location: Coordinates, creatorId)`: refuses (returns `null`) an unset (0, 0) location; the new ticket gets `location`, so `tickets`, `ticketLocations` (exact) and `publicTickets` (`toAnonymousView()` rounds to 2 decimals) all carry it. No new mapping code: the writers already map `location`.

**10 m grouping (in `FirestoreTicketsRepository.reportTicket`, before building the new ticket).**
1. Query `ticketLocations` with `whereEqualTo("categoryId")`, `whereEqualTo("situation")`, `whereEqualTo("active", true)`, `.get().await()`. Firestore has no geo query; the 10 m test runs in the app on these few documents. Equality-only filters on several fields are served by the automatic single-field indexes, so **no composite index** is needed (if Firestore ever asks for one, the logged error has the console link; the fallback is to drop the `situation` filter, `findNearby` checks it anyway). Offline, `get()` answers from the cache.
2. Map each document to a minimal `Ticket` (`toTicketLocation()`: id, category, situation, location, OPEN when active) and call the existing pure rule `TicketsList(...).findNearby(location, categoryId, situation)` (D-002: active, same category and situation, at most 10 m by Haversine, nearest wins, (0, 0) ignored).
3. No match: create the ticket exactly as today, now with the location.
4. Match: one `get()` of the reporter's own tickets (`whereArrayContains("participantIds", creatorId)`, the F-011 query, allowed by the rules). If the match is among them, nothing is written and the result is `ALREADY_FOLLOWING` (the reporter created or already follows it; `followerIds` must never hold the creator). Otherwise one batch: `tickets/{id}/reports/{creatorId}` (categoryId, situation, exact location, description, createdAt, and the reporter's `attachments` as PENDING with `uploadedById = creatorId`) and an `update` of `tickets/{id}` with `FieldValue.arrayUnion(creatorId)` on `followerIds` and `participantIds`. The shared ticket's `attachments` are **not** changed (Q5): a joiner's media stays in their own report, readable only by them and operators, so a PRIVATE reporter's photos can never reach a PUBLIC ticket, its participants or (through F-013 thumbnails) anonymous users. Grouping itself ignores scope, as the brief's rule (I.2 Obs.) only uses category/situation and 10 m. No `stats/summary` change, no `publicTickets` change. `commit()` not awaited, as F-011.
- Bounding box rejected: with only active tickets of one category and situation the candidate list is small, and a box needs two range filters on different fields (a composite index and inequality limits). Geohash prefix (task file) rejected: a new field, a library or hand code, and boundary cases, for no gain at this size.
- Result: `reportTicket` returns `ReportOutcome?` (`enum class ReportOutcome { CREATED, JOINED, ALREADY_FOLLOWING }`, `null` = refused) instead of the id, which was only a success flag (D-006). `submit(onSuccess: (ReportOutcome) -> Unit)`; `MainActivity` shows "Report submitted." / "This issue was already reported nearby. You now follow it." / "You already follow this issue nearby.".
- `ticketLocations.active` becomes `ticket.isActive` (OPEN, INFO_REQUESTED, IN_PROGRESS) instead of "not CLOSED", so a report next to a RESOLVED ticket creates a new one, as decision 0005 and `findNearby` say (Q6; amends decision 0010; D-013/F-014 must flip it on RESOLVED and on reopen).

**Map (osmdroid).** Superseded after Gate 1: `ui/components/TicketMap.kt` with `GoogleMap` + `rememberCameraPositionState` as Contacts_#3 ShowScreen; coarse = zoom 14, `Circle` 600 m, no `Marker`, gestures off via `MapUiSettings`; exact = `Marker`, zoom 15; no osmdroid `Configuration`. Original text:
- `CityFixApplication.onCreate()` sets osmdroid's `Configuration.getInstance()`: `userAgentValue = packageName` (OSM tile policy) and `osmdroidBasePath` / `osmdroidTileCache` under `cacheDir` (osmdroid's default path is shared external storage, not writable on recent Android). **Beyond P10** (the slide shows only the `MapView`).
- `ui/components/OsmMap.kt`: `OsmMap(location: Coordinates, exact: Boolean, modifier)`. P10 slide 32's `AndroidView(factory = { MapView(ctx).apply { setTileSource(TileSourceFactory.MAPNIK); controller.setZoom(..); controller.setCenter(GeoPoint(..)) } })`, plus `CopyrightOverlay` (attribution), the zoom buttons hidden, and `onRelease = { it.onDetach() }`. Coarse (`exact = false`): zoom 14, a `Polygon.pointsAsCircle(center, 600.0)` circle, and a transparent Compose cover over the map so touches never reach it (no pan, no zoom) while the page still scrolls. Exact: zoom 17, a `Marker`, multi-touch on (D-013). In previews (`LocalInspectionMode`) it draws the current placeholder box instead.
- `CoarseLocationMap` keeps its signature, heading, caption and the "Location not available" case, and calls `OsmMap(location, exact = false)` in place of the box. `ShowScreen` does not change.
- Rejected: Google Maps Compose (P10's main path, and the class example's `maps-compose` 9.0.1): it needs a Maps API key from a Google Cloud project; a key in `local.properties` is missing from the delivery ZIP, so the grader would see a blank map, and a committed key breaks the "no secrets" rule (decision 0012 Q6).

## Architecture impact
- New: `data/LocationRepository.kt`, `model/AnonymousArea.kt`, `viewmodel/LocationGateViewModel.kt` (+ UiState, factory), `ui/screens/LocationGateScreen.kt`, `ui/components/LocationPermissionPanel.kt`, `ui/components/OsmMap.kt`, `utils/LocationPermissions.kt` (permission array + `hasLocationPermission(context)`, Android; `model/` must not import it).
- Changed contract: `TicketsRepository.reportTicket` takes `location` and returns `ReportOutcome?`; `ReportViewModel.submit` passes the outcome; `ReportViewModelFactory` takes the `LocationRepository`.
- Data: tickets and their companions now carry real locations; `ticketLocations.active` follows `isActive`; grouping writes `tickets/{id}/reports/{uid}` for the first time.
- Diogo's files: `MainActivity.kt` (gate wrapper, factory argument, snackbar per outcome) and `AndroidManifest.xml` (permissions). Tell him before the Code phase.
- Privacy: the anonymous path still reads only `publicTickets`; the gate reads only the device's own location; the grouping query on `ticketLocations` runs for the reporter, who until D-009 is the stand-in `dev-citizen-1` (anonymous users can still open the form until D-009 hides it; known, D-009's job).

## Failure modes
- Permission denied or "don't ask again": gate shows DENIED (explanation, Try again, Open settings); the form shows the panel and Submit reports the missing location. No crash.
- Location off or no fix (emulator without a fix): `getCurrentLocation` returns `null`: gate NO_FIX ("Turn on location and try again"), form `report_location_unavailable`.
- Offline: the location still works (GPS); the grouping query reads the cache, so a match against tickets this device has never seen is missed and a duplicate ticket is created (accepted, decision 0002 asked for "against cached data"); the batch is queued as today.
- No network for tiles: osmdroid draws a grey grid (and the cached tiles it has); the caption still says "Approximate area".
- Two citizens reporting the same spot at the same time, both offline: two tickets (no transaction, decision 0010). Accepted.
- Grouping query error (for example a missing index): logged; the report then creates a new ticket, so the user is never blocked (one `try`/`catch` around the query, not hardening: the alternative is a stuck form).

## Risks and open questions
Risks:
- R1: osmdroid is archived (Nov 2024) and is a View library: checked only on the emulator. If `AndroidView` + `MapView` misbehaves (for example inside the scrolling detail), the fallback is a fixed-size map without the cover.
- R2: the osmdroid AAR manifest may merge an old `WRITE_EXTERNAL_STORAGE` permission; check the merged manifest in slice 1 and remove it with `tools:node="remove"` if it appears (not needed with the cache under `cacheDir`).
- R3: play-services-location 21.3.0 next to Firebase BoM 34.19.0's Play services artifacts: Gradle picks the highest common versions; check `assembleDebug` and the first run.
- R4: until D-009 every report comes from the stand-in id, so the grouping acceptance check needs a second reporter id (edit `currentUser` to `Citizen("dev-citizen-2", registrationStatus = ACTIVE)` and rebuild for the second report). Test data in the shared database grows.
- R5: the gate wraps `CityFixApp`'s content in Diogo's file; D-009 will change the same lines (login route). Low conflict risk if he is told now.
- R6: Pr. Republica coordinates are approximate (about 100 m), irrelevant for a 10 km radius but should be confirmed.

Open questions (recommended default first):
- Q1 Centre of the 10 km area: **constants in `model/AnonymousArea.kt`** / read `municipality/main` (needs the document created in the console, a listener and missing/offline handling).
- Q2 When the gate checks: **once per app start and on "Try again"** / also on every resume / continuously (location updates).
- Q3 "Log in" on the gate until D-009: **hidden (nullable `onLoginClick`), D-009 adds it** / shown but disabled / shown and does nothing.
- Q4 When the report form reads the location: **when the form opens and on "Refresh location"; Submit uses the stored fix** / at the Submit tap (Submit waits for GPS).
- Q5 The joining reporter's photos/videos: **decided 2026-11-14 with the person: kept in their own `reports/{uid}` document, never appended to the shared ticket.** The brief (I.2 Obs.) groups by category/situation and 10 m only and says nothing about scope or the joiner's media; appending them (decision 0002's wording) would publish a PRIVATE reporter's photos when the matched ticket is PUBLIC (brief I.1.C shows a public ticket's images to anonymous users). Rejected: appending them except PRIVATE-into-PUBLIC (needs `scope` in `ticketLocations` and a check), grouping only same-scope reports (a condition the brief does not have; two tickets for one problem). Cost: other followers do not see the joiner's media; F-013 must also upload report media and D-013 should show each ticket's reports (handover). Amends decision 0002.
- Q6 `ticketLocations.active`: **`isActive` (false once RESOLVED; D-013/F-014 flip it on resolve and reopen; amend decision 0010)** / keep "not CLOSED" and accept joining a RESOLVED ticket.
- Q7 Show the reporter's coordinates on the form: **yes, 5 decimals** / only "Location captured".
- Q8 Libraries: **play-services-location 21.3.0 (class) and osmdroid-android 6.1.20** / play-services-location 21.4.0 (newest).
- Q9 Coarse map look: **zoom 14, 600 m circle, no gestures** / zoom 13, no circle.

## Out of scope
- Background location, location updates, the 20 m geofence, foreground service (D-014).
- Login, hiding "+" and the form for anonymous users, roles, `DetailAudience` (D-009).
- Operator panel map with exact coordinates (D-013 calls `OsmMap(exact = true)`).
- Uploading media, thumbnails in `publicTickets` (F-013).
- Deploying the Firestore rules (decision 0012 Q5 amended); the drafts are not edited.
- Clustering several coarse tickets on one map (no anonymous map of many tickets exists).

## Slices
Each builds (`./gradlew assembleDebug`), passes `./gradlew lint` with 0 errors, and is one commit.
1. Dependencies and manifest: catalog + Gradle entries for play-services-location and osmdroid; `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `INTERNET`; merged manifest checked (R2).
2. Location building blocks: `model/AnonymousArea.kt`, `utils/LocationPermissions.kt`, `data/LocationRepository.kt`, `CityFixApplication.locationRepository`.
3. Permission panel and anonymous gate: `LocationPermissionPanel`, `LocationGateViewModel` (+ state, factory), `LocationGateScreen`, wiring in `MainActivity`, strings EN/PT.
4. Report location: form state, ViewModel, factory, screen section; `reportTicket` takes and stores the location; strings EN/PT.
5. 10 m grouping: `ReportOutcome`, the `ticketLocations` query + `findNearby` + join batch, `active = isActive`, outcome snackbars in `MainActivity`, strings EN/PT.
6. Map: osmdroid configuration in `CityFixApplication`, `OsmMap`, `CoarseLocationMap` uses it.

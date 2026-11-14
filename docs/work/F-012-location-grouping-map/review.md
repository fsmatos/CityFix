# F-012 review

Range: `3f6c47f..HEAD` (`475f4a6` .. `ae1dee1`), branch `dev/Francisco`. Reviewed against 00-plan, 01-coding-plan, 03-docs-plan, the full state.md Log (the changes after Gate 1: Google Maps Compose 6.1.0, key in the manifest with a placeholder, permission requested at start like Contacts_#3, Q5 joiner media in `reports/{uid}`, no try/catch around the grouping query), the task file, brief I.1 / I.1.C / I.2 Obs / V, class Contacts_#3 (MainActivity permission block, ShowScreen `GoogleMap` + `Marker`) and P10.

Verify (2026-11-14): `./gradlew assembleDebug lint` BUILD SUCCESSFUL; lint 0 errors / 20 warnings (6 NewerVersionAvailable, 5 GradleDependency, 7 UnusedResources, 1 RedundantLabel, 1 AndroidGradlePluginVersion), none from F-012 code.

## Verdict
**Approve with should-fix.** Blocking 0 · Should-fix 1 · Nit 4. The focus points hold:
- 10 km gate: `isInsideAnonymousArea` uses the existing `haversineMeters`, `<= 10_000.0` m around (40.2100, -8.4190) (within about 100 m of the square, irrelevant at 10 km); (0, 0) is outside; the gate is shown only when `app.currentUser == null && status != INSIDE`.
- Anonymous privacy: the detail still reads only `publicTickets` (`toAnonymousView`, 2 decimals); `TicketMap(exact = false)` draws a circle, no `Marker`, gestures and toolbar off, no coordinates text. The only coordinates text is the reporter's own fix in the form.
- 10 m grouping: `findMatch` uses three equality filters on `ticketLocations` and then the pure `TicketsList.findNearby` (active, same category and situation, at most 10 m, nearest wins). `ALREADY_FOLLOWING` comes from the participant query, so the creator never ends up in `followerIds`. The join batch writes only `reports/{uid}` (with the joiner's attachments, after the EXIF strip) and `arrayUnion` on `followerIds`/`participantIds`. The shared ticket's `attachments`, `publicTickets` and `stats/summary` are not touched.
- `reportTicket` refuses (0, 0) and stores the location in `tickets`, `ticketLocations` (exact) and `publicTickets` (rounded, through `toPublicTicketDocument`). `ticketLocations.active = isActive`.
- Denial: the gate and the form show `LocationDeniedMessage` with Try again and Open settings; `LocationRepository` checks the permission itself, so it cannot throw `SecurityException`.
- MVVM: stateful `LocationGateScreen` / `ReportScreen` over stateless `LocationGateContent` / `ReportContent`, with previews of the stateless halves in EN and PT; the gate and form state live in ViewModels, so they survive rotation.
- Strings: all new strings are in `values` and `values-pt`. The changes to `MainActivity` (+51 lines, early `return`, no re-indent) and the manifest (+8 lines) are small.

Consequences of the person's explicit choices (not findings):
- No try/catch: a failing `get()` in `findMatch`/`joinTicket` (for example `PERMISSION_DENIED` once rules are deployed) would crash the app from `viewModelScope` instead of leaving the form open.
- Key in the manifest: whoever has the ZIP can use the key, so restrict it to the package and SHA-1 in Google Cloud.
- Permission at start: there is no rationale before the dialog. The denied text explains afterwards.

## Blocking
None.

## Should-fix

### S1. An "Approximate" location grant breaks the 10 m grouping and the stored location
`ReportScreen` (`askLocation` callback: `permissions.values.any { it }`; `LaunchedEffect`: `hasLocationPermission(context)`) and `LocationRepository.currentLocation()` accept COARSE alone. On Android 12+ the system dialog offers "Approximate" as a normal choice. The fused provider then returns an obfuscated fix that is about 2 km off and snapped to a grid. Two coarse-only citizens reporting the same category and situation anywhere in the same grid cell get the *same* point, so `findNearby` groups unrelated reports (false JOINED). The ticket that operators see (brief II, "coordenadas") is also km off. The gate is fine with a coarse fix (a 10 km radius); the form is not.

Fix (form only; the gate keeps "either"):
- In `ReportScreen`, pass precise permission only. In the launcher callback use `viewModel.onLocationPermissionResult(permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true)`. In the `LaunchedEffect` use `ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED`, for example through a `hasPreciseLocationPermission(context)` next to `hasLocationPermission` in `utils/LocationPermissions.kt`.
- Give the form its own denied text, for example `report_location_precise_needed` ("Reports need your precise location so they can be grouped with nearby reports. Allow precise location.", plus PT). `LocationDeniedMessage` can take the text as a `@StringRes` parameter. This also removes the gate wording ("check that you are within 10 km") from the form, where registered citizens see it.
- Try again on Android 12+ then shows the "upgrade to precise" dialog. Add a device check: grant Approximate, then open the form.

## Nit

### N1. A rotation while the start-up dialog is open can flash DENIED
`MainActivity`: `LaunchedEffect(Unit) { locationPermissionRequest.launch(...) }` runs again after every Activity recreation (log deviation (c)). If the user rotates while the first system dialog is still open, the second request is answered at once with an empty map (Android serves one permission request at a time). `any { it }` is then false, so the gate shows DENIED until the real answer arrives. Fix: ask once per process with `var asked by rememberSaveable { mutableStateOf(false) }` and `LaunchedEffect(Unit) { if (!asked) { asked = true; locationPermissionRequest.launch(LOCATION_PERMISSIONS) } }`. The ViewModel keeps the result, so a rotation needs no new request, and the log note (c) is no longer needed.

### N2. `locationError = R.string.location_permission_denied` on denial is never shown
`ReportViewModel.onLocationPermissionResult(false)` sets it, but `LocationSection` only draws `locationError` in the not-denied branch. `submit` then copies it forward, where it is still not drawn. Fix: on denial set only `locationPermissionDenied = true` (and `locationError = null`). `submit` then falls back to `report_error_location`, as it already does. Update `locationDeniedState` in the previews to match.

### N3. The 600 m circle can miss the real spot
`publicTickets` rounds to 2 decimals. At Coimbra's latitude that moves a point by up to 0.005 deg lat (about 556 m) and 0.005 deg lon (about 425 m), up to about 700 m diagonally. The 600 m circle drawn around the rounded point can therefore exclude the real place, while the caption says "Approximate area". Fix: `COARSE_RADIUS_METERS = 750.0` in `TicketMap.kt`, with the 00-plan Q9 / D12 wording updated. Do not shrink the rounding.

### N4. `Alignment.CenterVertically` on the gate does nothing
`LocationGateContent`: the `Column` is inside `verticalScroll`, which gives it unbounded height, so `Arrangement.spacedBy(16.dp, Alignment.CenterVertically)` cannot centre the content. The content sits at the top in portrait. Fix: either drop the alignment (`Arrangement.spacedBy(16.dp)`) so the code says what it does, or centre it for real by wrapping in `BoxWithConstraints` and giving the column `Modifier.heightIn(min = maxHeight)` before `verticalScroll`'s content.

## Open (not blocking)
- Device checks D1-D14 (and D10b) in 01-coding-plan were not run: no device or emulator, and no real Maps key. In particular, check D12: the page must scroll when dragging over the coarse map with gestures off, and TalkBack must read `cd_ticket_map`, not the map's internal nodes. Also check D13 (rotation of the detail with the map) and the S1 device check once it is fixed.
- The person pastes the Maps key over `PASTE_MAPS_API_KEY_HERE` and restricts it.
- Docs phase (03-docs-plan): amend decisions 0002 (Q5 joiner media), 0010 (`active = isActive`, `reports/{uid}` first writer) and 0012 Q6 (Google Maps instead of osmdroid, key in the manifest), and update the handovers for D-009, D-013, D-014, F-013, F-014 and F-016 listed in the state.md "To do by the person" entry.

# F-012: Location, permissions, 10 km gate, 10 m grouping and map

Owner: Francisco (`dev/Francisco`) · Layer: `utils` / `ui` / `data` · Phase 2 · Status: to do · Target: 2026-11-14
Depends on: F-011 · Blocks: F-015, D-014

## Goal
Give the app the user's location: limit anonymous use to 10 km around Pr. Republica, store the report location, group duplicate reports within 10 m, and show a real map.

## Why
- Brief I.1: anonymous citizens only within 10 km of Pr. Republica.
- Brief I.2 Obs: same category/situation within 10 m becomes a group ticket (Option A, decision 0002). Today `reportTicket` has no location, so grouping never runs.
- Brief I.1.C: map with a far zoom. Today `CoarseLocationMap` is a placeholder.
- Brief V: dynamic location permission request and validation.

## What to build
- **`data/LocationRepository`** (or `utils/LocationHelper`) over `FusedLocationProviderClient`, as in P10 / the `SideEffect_BR_Maps_Sensors_Permissions` class example: current location as a suspend call or Flow. Diogo reuses it in D-014.
- **Permissions:** `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` requested with an explanation and a denied state. Background location is requested in D-014 only.
- **Anonymous gate:** when not logged in, if the device is more than 10 km from `municipality/main` (or location is denied) show a blocking screen with "Log in" instead of the dashboard. Uses `utils/Distance.kt`.
- **Report location:** the report form reads the location on submit (and shows it); `reportTicket` stores it.
- **10 m grouping:** before creating, query open tickets with the same category and situation near the point (geohash prefix or a bounding box query, then exact distance filter); if one is within 10 m, add the reporter to `followerIds` instead of creating a ticket and tell the user.
- **Map component:** real map with a far zoom for anonymous detail (coarse location only), normal zoom for operators (D-013 reuses it). Maps library chosen at F-010 Gate 1 (Maps API key needs the person's OK).

## Files
New `data/LocationRepository.kt`; `ui/components/CoarseLocationMap.kt`, `ui/screens/ReportScreen.kt`, `viewmodel/ReportViewModel.kt`, `data/FirestoreTicketsRepository.kt`, `model/TicketsList.kt` (grouping rule), `AndroidManifest.xml`, `MainActivity.kt` (gate; tell Diogo), strings EN/PT.

## Acceptance checks
- [ ] Emulator location outside 10 km: anonymous user sees the gate; logged-in user does not.
- [ ] Two reports with the same category and situation 5 m apart give one ticket with one follower.
- [ ] Denying location shows a clear message, no crash.
- [ ] The anonymous detail map never shows exact coordinates.

## References
Brief I.1, I.2 Obs, V; decisions 0002, 0006; P10; class example `SideEffect_BR_Maps_Sensors_Permissions`.

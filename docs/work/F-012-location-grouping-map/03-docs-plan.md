# F-012 docs plan

## Docs to update
- `docs/architecture/overview.md`:
  - "Layers (MVVM)": `LocationRepository` in `data/`, `LocationGateViewModel` in `viewmodel/`, `AnonymousArea.kt` in `model/`, `LocationPermissions.kt` in `utils/`; `CityFixApplication` holds `locationRepository` and configures osmdroid.
  - New section "Location, gate and grouping (F-012)": the one-fix `LocationRepository` (and how D-014 extends it), the permission flow (rationale first, departure from P10's auto-request), the gate states and the `currentUser` seam, the grouping query (`ticketLocations` equality filters + `findNearby`, no composite index), the join batch, `ReportOutcome`, the device checks D1-D14 and which ran.
  - "Rules (D-002)": `findNearby` now has a caller (through `toTicketLocation()`).
  - "Firestore repository (F-011)", report batch: the location is real; `ticketLocations.active = isActive`; the grouping branch writes `reports/{uid}` + `arrayUnion`.
  - "Report form (F-006)" and "Screens" (`ReportScreen`): the Location section and the new state fields; remove "No location is captured (Gate 1 Q13)".
  - "Shared components": `CoarseLocationMap` is a real osmdroid map now; add `OsmMap` and `LocationPermissionPanel`; drop the "static placeholder" and "known gap against brief I.1.C" notes in "Shared components" and in `ShowScreen`'s "Map" bullet.
  - "App shell (D-006)": the gate wrapper and `LocationGateViewModel` row in the state table; the outcome snackbars.
  - Dependencies list: play-services-location and osmdroid with versions, the resolved classpath change and the lint warning count.
  - "Handover (F-011)": mark the F-012 item done. New "Handover (F-012)": D-009 (wire "Log in", the gate follows the signed-in user, hide "+" for anonymous), D-013 (`OsmMap(exact = true)`; set `ticketLocations.active = false` on RESOLVED), F-014 (`active = true` on reopen), D-014 (reuse `LocationRepository`, add background permission and updates), D-015 (nothing new).
- Decision 0010: amendment "`ticketLocations.active` = the ticket is active (OPEN, INFO_REQUESTED, IN_PROGRESS)", replacing "true while not CLOSED", with the reason (decision 0005, `findNearby`) and the consequence for D-013/F-014. Add that the follower's attachments go in the same batch (if Q5 keeps the default).
- Decision 0002: Consequences, "check against cached data": done by F-012 (the query reads the cache offline); note the duplicate-ticket limit offline. Amendment (Q5, 2026-11-14): a joiner's media stays in their `reports/{uid}` document instead of the shared ticket, so a reporter's Public/Private choice is never overridden; grouping still ignores scope (brief I.2 Obs.).
- Decision 0014 and report notes: record Q5 as the authors' reading of a case the brief leaves open (scope vs grouping), with the rejected options. Handover: F-013 uploads report media too; D-013 shows each ticket's reports to operators.
- Decision 0012: Q6 consequence "F-012 and D-014: use osmdroid": done for F-012, with the version and the configuration it needed.
- `docs/report/notes.md`: the items below.
- Tell Diogo (no edit to his docs): `MainActivity.kt` and `AndroidManifest.xml` changes; `model/TicketsList.kt:19` comment still says "In-memory tickets for phase 1" (F-011 review N4, still open).

## Decisions to record
- **0014: Location, anonymous gate and 10 m grouping** (new). Contents: centre and radius as code constants instead of `municipality/main` (Q1); gate checked once per start (Q2); one fix with `getCurrentLocation` instead of updates; query-then-filter instead of geohash or bounding box; `ReportOutcome` replaces the returned id; the creator/follower check through the participant query; `ticketLocations.active = isActive` (Q6); osmdroid configuration (User-Agent, cache under `cacheDir`); coarse map look (Q9). Rejected: Google Maps Compose (key not in the ZIP), geohash, bounding box, reading `municipality/main`, auto-request without rationale. Write only what Gate 1 confirms.

## Report notes
- Location as taught in P10 (FusedLocationProvider, `RequestMultiplePermissions`), with a one-shot `getCurrentLocation` because a report needs a single fix.
- The anonymous 10 km rule is a client check only (rules cannot know where the phone is; decision 0010) and is checked at app start.
- Grouping without geo queries: Firestore filters by category, situation and active; the 10 m Haversine test runs on the device over those few documents. Works offline against the cache; two offline reports at the same spot can still make two tickets.
- Why `ticketLocations` exists (citizens cannot read other people's tickets) and what it exposes to signed-in citizens (category, situation, exact location; decision 0010).
- osmdroid instead of Google Maps: no key, so the grader's build from the ZIP shows the map; archived project (Nov 2024), works; OSM tile policy (User-Agent, attribution).
- The anonymous map never shows exact coordinates: the data is rounded before it is stored (decision 0006) and the map shows a 600 m circle at a far zoom with gestures off.
- Improvement opportunities: re-check the gate on resume or with location updates; cluster coarse tickets on a map of all public issues; a server-side grouping (Cloud Function) to avoid offline duplicates; read the centre from `municipality/main` when several municipalities exist; geohash queries for large data sets.

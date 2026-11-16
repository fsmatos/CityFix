# D-014: Operator 20 m geofence and proximity alert

Owner: Diogo (`dev/Diogo`) · Layer: `utils` / `data` · Phase 2 · Status: to do · Target: 2026-11-20
Depends on: D-013, F-012 (location helper), F-016 (notification helper and delivery)

## Goal
When an operator comes within 20 m of an active ticket (IN_PROGRESS), alert its author(s) that the council team is in the area.

## Why
- Brief I.3 Obs: operator closer than 20 m to an active ticket → authors alerted.
- Brief II.3: IN_PROGRESS activates the 20 m geofence and notifies the authors.
- Brief III: proximity alert notification; brief V: background location permission.

## What to build
- **Operator side:** Android Geofencing API (`GeofencingClient`) with 20 m circles around the IN_PROGRESS tickets (max 100), refreshed when the list changes, plus a `BroadcastReceiver` for ENTER events. Alternative: a location Flow from F-012 with a distance check while the app runs (simpler; decide at Gate 1 and note the trade-off in the report).
- **Permissions:** `ACCESS_BACKGROUND_LOCATION` requested separately after foreground location, with an explanation (Android 10+ rules).
- **On enter:** write a `proximityAlerts` entry (ticket id, time, no operator location) or update `tickets/{id}.lastOperatorNearbyAt`; F-016's delivery turns it into a notification for the creator and followers. Throttle: at most one alert per ticket per hour.
- **Hook (exists since D-013):** `Ticket.needsOperatorGeofence(now)` in `model/TicketLifecycle.kt` (effectively IN_PROGRESS and a real location); filter the operator's shared `tickets` with it.
- **On IN_PROGRESS** (D-013): the authors also get the "work started" notification (F-016 status change).

## Files
New `utils/GeofenceManager.kt`, `data/GeofenceBroadcastReceiver.kt`; `AndroidManifest.xml`, `data/FirestoreTicketsRepository.kt` (alert write; with Francisco), strings EN/PT.

## Acceptance checks
- [ ] With the emulator's location route moving an operator to 15 m of an IN_PROGRESS ticket, the author's device shows the alert.
- [ ] No alert for OPEN or RESOLVED tickets, or beyond 20 m.
- [ ] Repeated entries within the hour give one alert.
- [ ] The citizen never receives the operator's coordinates.

## References
Brief I.3 Obs, II.3, III, V; class example `SideEffect_BR_Maps_Sensors_Permissions` (broadcast receivers, location).

# D-014 docs plan

## Docs to update
- `docs/architecture/overview.md`:
  - New "### Operator proximity (D-014)" section after "Handover (D-013)". It covers the mechanism chosen at Gate 1, the pure rules (20 m, 1 h, entry edge), the watcher lifecycle (operator, precise location, `repeatOnLifecycle(STARTED)`), the `lastOperatorNearbyAt` field, the privacy guarantees (time only, no operator id or coordinates, `publicTickets` untouched, projection clears it), the failure-mode table and the device checks still open.
  - New "### Handover (D-014)" section:
    - **F-016:** the five-point contract (source, trigger with the 1 h freshness, content, "not a status change", "work started" is F-016's);
    - **Francisco:** his four files changed (signatures);
    - **D-015:** the demo script (two emulators, `adb emu geo fix` values) and the "foreground only" statement, if B is chosen.
  - "D-013 hook" bullet: "No caller yet" -> called by `insideOperatorGeofence` (D-014).
  - "Handover (F-012)", D-014 bullet: mark what was done (updates added, background permission done or not, per Q2).
  - "Firebase (F-010)" data layout: add the field, and the draft rule if slice 5b is done.
- KDoc: `LocationRepository` (the "D-014 adds updates here" line replaced), `Distance.kt` (20 m now used), `needsOperatorGeofence` (callers).
- `docs/tasks/phase-2/D-014-operator-geofence.md`: tick the acceptance checks that passed. Note the chosen option in "What to build".

## Decisions to record
- **0022: Operator proximity alert** (amends 0010 and 0012's consequences for D-014):
  - the foreground Flow vs Geofencing API choice (Q1) and the background permission (Q2);
  - the record `tickets/{id}.lastOperatorNearbyAt` (Long, the operator's device time) instead of a `proximityAlerts` collection or a server timestamp (Q3);
  - the entry edge plus the shared hourly throttle (Q6), and the 25 m accuracy filter (Q7);
  - the write in `TicketsRepository` (Q4) and the draft rule (Q5);
  - the F-016 contract (Q9) and the optional local delivery (Q8).
  - Rejected: a foreground location service, Cloud Functions, and storing the operator id or position.
- Suggested amendment line for **0010** (Francisco's record, not edited without him): "`tickets/{id}.lastOperatorNearbyAt` (Long, optional), D-014, decision 0022."

## Report notes
(`docs/report/notes.md`, D-014 section)
- **Brief mapping:** I.3 Obs and II.3, the 20 m rule while IN_PROGRESS; III, the proximity alert (delivered by F-016); V, background location (covered or not, per Q1/Q2, and why).
- **Options compared:** the Q1 table (accuracy at 20 m vs the recommended minimum of 100-150 m, ENTER latency, the "Allow all the time" permission, re-registration after reboot, testing). Why B was chosen for the deadline, and the hybrid as an improvement.
- **Privacy by design:** only a time is stored; no operator id or coordinates leave the device; anonymous users never read `tickets`; the projection clears the field; logcat has no coordinates.
- **Throttle without a server:** a stored field shared by all operators, plus an in-memory guard and the entry edge. Its limits: two simultaneous offline operators, and clock skew between devices. F-016's freshness and last-notified checks are the second line.
- **Class material and beyond:** P10's `requestLocationUpdates` (here a `callbackFlow` with `awaitClose`, not a service), `checkSelfPermission`, `repeatOnLifecycle`. Beyond class: the Haversine distance check, the accuracy filter and (if done) `GeofencingClient` + `BroadcastReceiver` with `goAsync`.
- **Improvement opportunities:** a foreground location service or geofences for the background; a server-side throttle (deployed rules or a Function); a per-operator "on duty" switch; adaptive update intervals to save battery.

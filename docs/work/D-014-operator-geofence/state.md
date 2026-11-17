# D-014: Operator 20 m geofence and proximity alert
Branch: dev/Diogo · Base: f1515bf
Remote: origin/dev/Diogo

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-17 | Task file `docs/tasks/phase-2/D-014-operator-geofence.md` |
| Plan | done | 2026-11-17 | 00/01/03 written; 11 open questions for Gate 1 |
| Gate 1: plan approved | done | 2026-11-17 | Approved by Diogo; Q1-Q11 as recommended; slices 1-5 required, 5b-8 only if time remains by 2026-11-19 |
| Code | | | |
| Review | | | |
| Docs | | | |
| Pushed | | | |
| Merged | | | |

## Log
- 2026-11-17 Start: `origin/main` had no commits missing from `dev/Diogo`; Diogo chose not to run `/merge`. Commit type `feat`. Untracked `.idea/` files left out of the commit.
- 2026-11-17 Plan: planner checked the code.
  - Found: `needsOperatorGeofence` has no caller. `LocationRepository` has a one-shot fix only. No notification code, channel, FCM service, `functions/`, `proximityAlerts` or `lastOperatorNearbyAt` exists. F-016 has no work folder. The rules are a draft and not deployed (decision 0012 Q5). Decision 0012 Q3 says no Cloud Functions.
  - Plan: recommended approach B, a foreground location Flow with a 20 m Haversine check, an entry edge and a 1 h throttle. It writes only `tickets/{id}.lastOperatorNearbyAt` (Long), as the seam for F-016, with a 5-point consumer contract.
  - Optional: the Geofencing API + background permission (slices 6-7), local delivery without F-016 (slice 8), and a draft rule (5b, needs Diogo's OK).
  - Francisco's files touched (additive): `FirestoreMapping.kt`, `TicketsRepository.kt`, `FirestoreTicketsRepository.kt`, `LocationRepository.kt`; optional `LocationPermissions.kt` and `firestore.rules`.
  - The brief PDF could not be rendered in this session (R6). No code written.
- 2026-11-17 Gate 1: Diogo approved the plan with Q1-Q11 as recommended (foreground location check, `lastOperatorNearbyAt` field, write in `TicketsRepository`, no background permission without slice 6, draft rule as documentation only and no deploy). Slices 1-5 to implement; optional slices 5b-8 only if 1-5 are done by 2026-11-19.

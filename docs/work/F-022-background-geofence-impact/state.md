# F-022: Background geofence and vertical impact detection
Branch: dev/Francisco · Base: b2d893b
Remote: origin/dev/Francisco

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-22 | task file `docs/tasks/phase-3/F-022-background-geofence-impact.md` |
| Plan | done | 2026-11-22 | 00/01/03 written; 6 slices, stop point after slice 3 |
| Gate 1: plan approved | done | 2026-11-22 | approved; Q1-Q6 defaults |
| Code | pending | | |
| Review | pending | | |
| Docs | pending | | |
| Pushed | pending | | |
| Merged | pending | | |

## Log
Append-only.
- 2026-11-22: Started from the person's two improvement notes (chat). `origin/main` fetched, 0 commits behind. Untracked `.idea/` left as is. Deadline today 23:59.
- 2026-11-22: Plan written (planner). Slices 1-3 (vertical projection, speed gate, threshold slider) are in-app and must-have; 4-6 (foreground service with geofence, background location dialog, impact in service + notification) can be dropped if time runs out. Open questions Q1-Q6 in 00-plan await Gate 1. Not committed.
- 2026-11-22: Gate 1 approved by the person; Q1-Q6 answered with the proposed defaults (threshold 6 m/s² range 3-15; speed > 2 m/s from a fix < 5 s old; in-app detector removed once the service detects; OperatorProximityViewModel deleted and Diogo told; threshold stored on the device; background location asked once per session after notifications).

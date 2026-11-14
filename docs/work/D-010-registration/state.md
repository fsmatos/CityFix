# D-010: Registration (citizen and operator)
Branch: dev/Diogo · Base: 73370ed
Remote: not pushed

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-14 | Task file `docs/tasks/phase-2/D-010-registration.md` |
| Plan | done | 2026-11-14 | 00-plan, 01-coding-plan, 03-docs-plan; 7 slices (6 map pick optional, 7 rules line conditional on Q1); Q1-Q15 for Gate 1 |
| Gate 1: plan approved | done | 2026-11-14 | Approved by Diogo; Q1-Q15 as recommended; slice 7 (draft rules edit, not deployed) approved; slice 6 (map pick) skipped |
| Code | pending | | |
| Review | pending | | |
| Docs | pending | | |
| Pushed | pending | | |
| Merged | pending | | |

## Log
Append-only.

- 2026-11-14 Sync: `origin/main` had no commits missing from dev/Diogo; Diogo chose not to run `/merge`.
- 2026-11-14 Start: task file `docs/tasks/phase-2/D-010-registration.md` (target 2026-11-14), base 73370ed.
- 2026-11-14 Plan: planner wrote the three plans. 7 slices: validation helpers, registration data layer, Register screen + Login link + redirect hold, citizen location (F-012 `LocationRepository`), optional photo via `ProfilePhotoWorker` after the commit, optional map pick, conditional rules line. Key finding: sign-up signs the user in, so D-009's redirect would clear the Register screen and cancel the NIF transaction; fix is a saved `holdRedirect` flag in `CityFixApp`. Recommends creating the operator's `users/{uid}` at registration (deviation from the D-009 handover) and dropping the `email_verified` term of the draft `users` create rule (`firestore.rules:190`), which needs Diogo's OK. Risks R1-R9 (incl. two decision records numbered 0015).
- 2026-11-14 Gate 1: approved by Diogo. Q1-Q15: the recommended answers. Q1: Diogo allows editing the draft `firestore.rules` (drop the `email_verified` term of the `users` create rule) in slice 7; commit only, no `firebase deploy`; Francisco to be told. Slice 6 (map pick) skipped: "use my location" only.

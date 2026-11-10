# D-007: Anonymous projection and expand toggle
Branch: dev/Diogo · Base: a13881d
Remote: origin/dev/Diogo

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-10 | Started after merging origin/main (F-007 to F-009, phase 2 tasks) |
| Plan | done | 2026-11-10 | 00-plan, 01-coding-plan, 03-docs-plan; 2 slices, no tests; Q1-Q7 for Gate 1 |
| Gate 1: plan approved | done | 2026-11-10 | Approved by Diogo: Q1 option A (hide toggle), Q2-Q7 as recommended |
| Code | pending | | |
| Review | pending | | |
| Docs | pending | | |
| Pushed | pending | | |
| Merged | pending | | |

## Log
Append-only.

- 2026-11-10 Merge: origin/main into dev/Diogo as a13881d (50 commits: F-007, F-008, F-009 and the phase 2 task files), approved by Diogo; no conflicts; assembleDebug and lint pass (0 errors, 16 warnings).
- 2026-11-10 Start: task file `docs/tasks/phase-1/D-007-anonymous-projection.md`, also listed in phase 2 with target 11-10. The task file predates F-008 (tickets repository) and F-009 (attachment images); the planner must check its file list against the current code.
- 2026-11-10 Plan: planner wrote the three plans. Only `model/TicketPrivacy.kt` (slice 1) and `MainTopAppBar.kt` + `MainActivity.kt` (slice 2) change; repository, ViewModel and Francisco's screens untouched. Stats run on the raw list, so they stay unchanged. 00-plan has the projection contract table for F-011. Open questions Q1-Q7 for Gate 1.
- 2026-11-10 Gate 1: approved by Diogo. Q1: option A (hide the expand toggle and force cards collapsed for anonymous users). Q2-Q7: the recommended answers (keep `createdAt`; required `showExpandToggle`; keep `isExpanded` and toggle code; no edits to Francisco's files; amend only 0006; record the other leaks as known limits).

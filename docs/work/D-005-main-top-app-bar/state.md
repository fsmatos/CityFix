# D-005: MainTopAppBar
Branch: dev/Diogo · Base: d910a26
Remote: not pushed

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-10-31 | Task file: docs/tasks/phase-1/D-005-main-top-app-bar.md |
| Plan | done | 2026-10-31 | 00-plan, 01-coding-plan, 03-docs-plan; 3 slices, no tests |
| Gate 1: plan approved | done | 2026-10-31 | Approved by Diogo; planner recommendations for Q1-Q8 |
| Code | pending | | |
| Review | pending | | |
| Docs | pending | | |
| Pushed | pending | | |
| Merged | pending | | |

## Log
Append-only.
- 2026-10-31 Start: task D-005 started on dev/Diogo at d910a26. `git fetch origin` done; origin/main has no commits missing from dev/Diogo, and Diogo chose to skip the merge. Untracked `.idea/` files left alone (IDE state, not part of the task).
- 2026-10-31 Plan: planner wrote 00-plan, 01-coding-plan, 03-docs-plan. Stateless `MainTopAppBar` with enum `TopBarScreen` and `canNavigateBack`; adds `material-icons-core` through the BOM; `navigation-compose` stays in D-006. 3 slices, open questions Q1-Q8.
- 2026-10-31 Plan checks by the main session: the planner could not read the brief or Francisco's branch. Q7: the brief does not contradict the route rules and asks for context-dependent icons and top bars. Only registered citizens open tickets, so the Auth phase must hide the add button by role. Q5: `values-pt/strings.xml` already exists on origin/dev/Francisco (693b05d) and is not on origin/main.
- 2026-10-31 Gate 1: approved by Diogo; every open question takes the planner's recommendation. D-005 creates `values-pt/strings.xml` now and accepts a small add/add conflict with Francisco's file at merge time (keep both sides). Hiding the add button by role is a requirement for the Auth phase, not part of D-005.

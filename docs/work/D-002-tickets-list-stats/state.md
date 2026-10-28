# D-002: TicketsList and dashboard stats
Branch: dev/Diogo · Base: 3c4fe6d
Remote: not pushed

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-10-28 | Task file: docs/tasks/phase-1/D-002-tickets-list.md |
| Plan | done | 2026-10-28 | 00-plan, 01-coding-plan, 03-docs-plan written; 6 slices (6th = tests, if approved) |
| Gate 1: plan approved | done | 2026-10-28 | Approved with one change: "active" excludes RESOLVED; JUnit test committed |
| Code | pending | | |
| Review | pending | | |
| Docs | pending | | |
| Pushed | pending | | |
| Merged | pending | | |

## Log
Append-only.
- 2026-10-28 Start: task D-002 started on dev/Diogo at 3c4fe6d. `git fetch origin` done; origin/main has no commits missing from dev/Diogo. Untracked `.idea/` files left alone (IDE state, not part of the task).
- 2026-10-28 Plan: planner wrote 00-plan, 01-coding-plan and 03-docs-plan: `utils/Distance.kt` (Haversine), `model/DashboardStats.kt`, `model/TicketsList.kt`; 5 code slices plus an optional JUnit slice. Open questions Q1-Q10 listed in 00-plan.
- 2026-10-28 Gate 1: approved by Diogo after checking the dashboard against the brief (section I.1.A asks only for average repair time, total tickets and % active). Decisions: Q1 commit a JUnit 4 test (slice 6); Q2 `DashboardStats` fields as proposed, `resolvedTickets` kept as an extra (still to tell Francisco before the push after slice 2); Q3 changed: "active" means not RESOLVED and not CLOSED (open, info requested, in progress), applied through the shared `Ticket.isActive`; Q9 stats include private tickets (the brief says "nº total de ocorrências", not "públicas"). Other questions take the planner's recommendation. Planner asked to update the plans for Q3.

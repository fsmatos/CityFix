# D-002: TicketsList and dashboard stats
Branch: dev/Diogo · Base: 3c4fe6d
Remote: not pushed

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-10-28 | Task file: docs/tasks/phase-1/D-002-tickets-list.md |
| Plan | done | 2026-10-28 | 00-plan, 01-coding-plan, 03-docs-plan written; 6 slices (6th = tests, if approved) |
| Gate 1: plan approved | done | 2026-10-28 | Approved with one change: "active" excludes RESOLVED; JUnit test committed |
| Code | done | 2026-10-28 | Slices 1-6: 25c2835, 4dd9007, 625336a, 5adb7fb, f64b7fe, 33701c9. assembleDebug + lint green after every slice; testDebugUnitTest 17/17 pass |
| Review | pending | | |
| Docs | pending | | |
| Pushed | pending | | |
| Merged | pending | | |

## Log
Append-only.
- 2026-10-28 Start: task D-002 started on dev/Diogo at 3c4fe6d. `git fetch origin` done; origin/main has no commits missing from dev/Diogo. Untracked `.idea/` files left alone (IDE state, not part of the task).
- 2026-10-28 Plan: planner wrote 00-plan, 01-coding-plan and 03-docs-plan: `utils/Distance.kt` (Haversine), `model/DashboardStats.kt`, `model/TicketsList.kt`; 5 code slices plus an optional JUnit slice. Open questions Q1-Q10 listed in 00-plan.
- 2026-10-28 Gate 1: approved by Diogo after checking the dashboard against the brief (section I.1.A asks only for average repair time, total tickets and % active). Decisions: Q1 commit a JUnit 4 test (slice 6); Q2 `DashboardStats` fields as proposed, `resolvedTickets` kept as an extra (still to tell Francisco before the push after slice 2); Q3 changed: "active" means not RESOLVED and not CLOSED (open, info requested, in progress), applied through the shared `Ticket.isActive`; Q9 stats include private tickets (the brief says "nº total de ocorrências", not "públicas"). Other questions take the planner's recommendation. Planner asked to update the plans for Q3.
- 2026-10-28 Code: implementer followed 01-coding-plan slices 1-6, one commit each; `./gradlew assembleDebug` and `./gradlew lint` passed (exit 0, no new errors) before every commit, and the import check showed only `kotlin.math.*` in `utils/` and `pt.isec.amov.cityfix.utils.haversineMeters` in `model/`.
  - Slice 1 (25c2835): `utils/Distance.kt` with `EARTH_RADIUS_METERS` and `haversineMeters`; `utils/.gitkeep` deleted.
  - Slice 2 (4dd9007): `model/DashboardStats.kt` exactly as in the Interfaces block (Gate 1 Q2 fields, guarded `activeRatio`).
  - Slice 3 (625336a): `model/TicketsList.kt` with the `Ticket.isActive` allow-list (OPEN, INFO_REQUESTED, IN_PROGRESS), private mutable list, `rawList` copy, `add` (rejects blank/duplicate id), `findById`, `getAllSorted` and `publicActive` (shared createdAt-desc-then-id comparator), `GROUPING_RADIUS_METERS`.
  - Slice 4 (5adb7fb): `findNearby` (nearest active ticket with same category + situation within 10 m, inclusive) and the private `Coordinates.isUnset()` helper.
  - Slice 5 (f64b7fe): `stats()` with plain `Long` maths; skips tickets with no RESOLVED entry, `createdAt <= 0` or negative duration; uses the last RESOLVED entry.
  - Slice 6 (33701c9): `app/src/test/java/pt/isec/amov/cityfix/model/TicketsListTest.kt`, 17 JUnit 4 tests covering every case listed in the plan. `./gradlew testDebugUnitTest`: 17/17 pass (plus the template ExampleUnitTest), then build and lint green.
  - Deviations: push of dev/Diogo after slice 2 skipped and left to the person (also telling Francisco the `DashboardStats` fields for F-004). `git config core.hooksPath` is not set in this clone (the plan's pre-commit check expects `.githooks`); git config was left unchanged, and the commit messages were written with no attribution lines. The person can run `git config core.hooksPath .githooks` once. No code deviations from the plan.

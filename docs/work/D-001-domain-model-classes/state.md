# D-001: Domain classes and enums
Branch: dev/Diogo · Base: 522011d
Remote: not pushed

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-10-27 | Task file: docs/tasks/phase-1/D-001-model-classes.md |
| Plan | done | 2026-10-27 | 00-plan, 01-coding-plan, 03-docs-plan written; 5 slices |
| Gate 1: plan approved | done | 2026-10-27 | Approved; all open questions take the planner's recommendation |
| Code | done | 2026-10-27 | 5 slices: 922c382, 811fe11, 2cc2df2, fffc6e1, 9c8835e; build + lint green after each |
| Review | pending | | |
| Docs | pending | | |
| Pushed | pending | | |
| Merged | pending | | |

## Log
- 2026-10-27 Start: task D-001 started on dev/Diogo at 522011d. `git fetch origin` was declined by the person, so origin/main was not checked for new commits.
- 2026-10-27 Plan: planner wrote 00-plan, 01-coding-plan and 03-docs-plan (5 slices, 16 files in model/, no Gradle changes).
- 2026-10-27 Gate 1: approved by Diogo. Decisions: Ticket stores `categoryId` + `categoryName` snapshot; `Scope` defaults to `PRIVATE`; diagram links not in the spec (`Operator.municipalityId`, `Category.municipalityId`, `Citizen.validatedById`, operator "manages") left out for now. The other open questions take the recommendation: `Long` epoch-ms times defaulting to `0L`, non-null `Ticket.location`, `val` properties with mapping in `data/` later, creator not in `followerIds`, `Attachment.file` as a String URI/path.
- 2026-10-27 Code: implemented slices 1-5 as in 01-coding-plan (922c382 enums, 811fe11 coordinates and users, 2cc2df2 municipality and category, fffc6e1 message/status history/attachment, 9c8835e ticket). `./gradlew assembleDebug` and `./gradlew lint` passed before each commit. Throwaway `model/NoArgCheck.kt` compiled with all nine no-arg constructors and was deleted (never committed). `grep -ri "Group\|Grupo" app/src/main` finds nothing; `model/` has no imports. Deviations: (1) the coding plan says not to touch state.md, but the orchestrator asked the implementer to record the code phase here; (2) small KDoc additions beyond the plan's list (Portuguese term on each class, `Ticket.categoryName` snapshot reason, `Coordinates` (0, 0) warning, `Citizen.impactDetection`). No contract changes. Note: `core.hooksPath` is not set in this clone, so the commit-msg hook is inactive; the messages were written without attribution lines anyway.

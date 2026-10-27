# D-001: Domain classes and enums
Branch: dev/Diogo · Base: 522011d
Remote: not pushed

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-10-27 | Task file: docs/tasks/phase-1/D-001-model-classes.md |
| Plan | done | 2026-10-27 | 00-plan, 01-coding-plan, 03-docs-plan written; 5 slices |
| Gate 1: plan approved | done | 2026-10-27 | Approved; all open questions take the planner's recommendation |
| Code | pending | | |
| Review | pending | | |
| Docs | pending | | |
| Pushed | pending | | |
| Merged | pending | | |

## Log
- 2026-10-27 Start: task D-001 started on dev/Diogo at 522011d. `git fetch origin` was declined by the person, so origin/main was not checked for new commits.
- 2026-10-27 Plan: planner wrote 00-plan, 01-coding-plan and 03-docs-plan (5 slices, 16 files in model/, no Gradle changes).
- 2026-10-27 Gate 1: approved by Diogo. Decisions: Ticket stores `categoryId` + `categoryName` snapshot; `Scope` defaults to `PRIVATE`; diagram links not in the spec (`Operator.municipalityId`, `Category.municipalityId`, `Citizen.validatedById`, operator "manages") left out for now. The other open questions take the recommendation: `Long` epoch-ms times defaulting to `0L`, non-null `Ticket.location`, `val` properties with mapping in `data/` later, creator not in `followerIds`, `Attachment.file` as a String URI/path.

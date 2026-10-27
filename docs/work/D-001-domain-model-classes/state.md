# D-001: Domain classes and enums
Branch: dev/Diogo · Base: 522011d
Remote: origin/dev/Diogo

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-10-27 | Task file: docs/tasks/phase-1/D-001-model-classes.md |
| Plan | done | 2026-10-27 | 00-plan, 01-coding-plan, 03-docs-plan written; 5 slices |
| Gate 1: plan approved | done | 2026-10-27 | Approved; all open questions take the planner's recommendation |
| Code | done | 2026-10-27 | 5 slices: 922c382, 811fe11, 2cc2df2, fffc6e1, 9c8835e; build + lint green after each |
| Review | done | 2026-10-27 | review.md: approve; 0 blocking, 2 should-fix (KDoc), 6 nits, 4 coordination notes |
| Docs | done | 2026-10-27 | overview Code mapping, decisions 0003 + 0004, docs/report/notes.md |
| Pushed | done | 2026-10-27 | origin/dev/Diogo at deff86a (includes merge of origin/main) |
| Merged | done | 2026-10-27 | |

## Log
- 2026-10-27 Start: task D-001 started on dev/Diogo at 522011d. `git fetch origin` was declined by the person, so origin/main was not checked for new commits.
- 2026-10-27 Plan: planner wrote 00-plan, 01-coding-plan and 03-docs-plan (5 slices, 16 files in model/, no Gradle changes).
- 2026-10-27 Gate 1: approved by Diogo. Decisions: Ticket stores `categoryId` + `categoryName` snapshot; `Scope` defaults to `PRIVATE`; diagram links not in the spec (`Operator.municipalityId`, `Category.municipalityId`, `Citizen.validatedById`, operator "manages") left out for now. The other open questions take the recommendation: `Long` epoch-ms times defaulting to `0L`, non-null `Ticket.location`, `val` properties with mapping in `data/` later, creator not in `followerIds`, `Attachment.file` as a String URI/path.
- 2026-10-27 Code: implemented slices 1-5 as in 01-coding-plan (922c382 enums, 811fe11 coordinates and users, 2cc2df2 municipality and category, fffc6e1 message/status history/attachment, 9c8835e ticket). `./gradlew assembleDebug` and `./gradlew lint` passed before each commit. Throwaway `model/NoArgCheck.kt` compiled with all nine no-arg constructors and was deleted (never committed). `grep -ri "Group\|Grupo" app/src/main` finds nothing; `model/` has no imports. Deviations: (1) the coding plan says not to touch state.md, but the orchestrator asked the implementer to record the code phase here; (2) small KDoc additions beyond the plan's list (Portuguese term on each class, `Ticket.categoryName` snapshot reason, `Coordinates` (0, 0) warning, `Citizen.impactDetection`). No contract changes. Note: `core.hooksPath` is not set in this clone, so the commit-msg hook is inactive; the messages were written without attribution lines anyway.
- 2026-10-27 Review: reviewer checked `git diff a280ab1..HEAD` against 00-plan, 01-coding-plan, Gate 1 decisions, the task spec, brief, overview, decision 0002, the Option A diagram and kotlin-conventions; re-ran `./gradlew assembleDebug` (green). Verdict approve, no blocking findings. Should-fix: S1 `Municipality` KDoc misstates the 10 km rule (it limits anonymous users, not tickets); S2 mark `Ticket.location`, `creatorId`, `followerIds`, `messages` as hidden from anonymous users. Coordination notes C1-C4 for D-002, D-003, F-002, F-005, F-006; nits N1-N6. `origin/dev/Francisco` not fetched, so guessed names on his side could not be checked. No code edited, nothing committed.
- 2026-10-27 Review fixes: S1, S2, N2, N3 applied as KDoc-only changes in 1aa7742 (`fix(D-001): address review comments`); build + lint green. Non-blocking, so no re-review loop.
- 2026-10-27 Docs: docs-writer added a "Code mapping" subsection to docs/architecture/overview.md, decisions 0003 (domain model representation) and 0004 (denormalised currentStatus), and a new shared docs/report/notes.md with the D-001 report notes. Review notes C1-C4, N1, N2, S1 and S2 are reflected in the docs.
- 2026-10-27 Push attempt: verify green (assembleDebug + lint); `git push -u origin dev/Diogo` (approved by Diogo) failed with "remote: Repository not found" for https://github.com/fsmatos/CityFix.git (likely missing access/credentials for the private repo, or a wrong URL). Not pushed; main untouched.
- 2026-10-27 Push: retried after access was fixed; `git push -u origin dev/Diogo` succeeded (522011d..ea006da). Verify not re-run: no code changed since the last green run.
- 2026-10-27 Merge: origin/main (227567e) into dev/Diogo, no conflicts and no file changes (227567e has the same tree as 522011d). Verify not run: no code changed.
- 2026-10-27 Push: `git push` of the origin/main merge succeeded (84342de..deff86a). Verify not run: no code changed.
- 2026-10-27 Merge: dev/Diogo into main (approved by Diogo), no conflicts; verify green (assembleDebug + lint). main not pushed yet.

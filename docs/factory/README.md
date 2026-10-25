# CityFix factory

How AI agents build this project. You decide what to build and approve plans; agents write the code.

## Setup on a fresh clone
```
git config core.hooksPath .githooks
git switch dev/<YourName>      # dev/Francisco or dev/Diogo
```
Then run `/reload-skills` (or restart Claude Code). The hook strips Claude attribution from commit messages.

## Branches
- `main`: integration branch. Nobody works on it directly; the factory never pushes it.
- `dev/Francisco`, `dev/Diogo`: one long-lived branch per person. You work, commit and push only on your own.
- To receive the other person's work: `/merge` (brings `origin/main` into your dev branch). To publish yours: ask for `/merge dev/<you> into main`, then push `main` yourself.

## Commands
| Command | Purpose |
| --- | --- |
| `/feature-dev [task]` | Full flow with Gate 1 |
| `/bug-fix <task>` | Bug flow: reproduce, failing test, minimal fix |
| `/refactor <desc>` | Refactor, behavior unchanged |
| `/start-task`, `/plan-feature`, `/plan-fix`, `/code`, `/review-code`, `/test-code`, `/write-docs` | Individual phases |
| `/push-task` | Commit, verify, push your dev branch (no PR) |
| `/merge [source] [into <target>]` | Merge with conflict resolution (default: `origin/main` into your dev branch) |
| `/finish-task` | Mark a task merged after your work reached `main` |
| `/work-status` | Status of every task on both dev branches and `main` |

## A day in the life of a task
1. Put the task in `docs/tasks/<name>.md`.
2. `/feature-dev <name>` allocates the ID (`F-001` on `dev/Francisco`, `D-001` on `dev/Diogo`) and creates `docs/work/F-001-<slug>/state.md` on your dev branch.
3. The planner writes four plans; you approve at **Gate 1** (the only gate).
4. Implementer codes slice by slice (`verify` before each commit, messages like `feat(F-001): ...`), reviewer reviews, tester tests, docs-writer documents.
5. `/push-task` pushes your dev branch. When ready, `/merge` `origin/main` into it, then ask `/merge dev/<you> into main`, push `main`, and run `/finish-task`.

## Work-item folder
`docs/work/<ID>-<slug>/`: `00-plan.md`, `01-coding-plan.md`, `02-test-plan.md`, `03-docs-plan.md`, `state.md`, `review.md`.

## Regenerating
`docs/factory/manifest.md` is the source of truth. Re-run `/new-project` pointing at it to regenerate the factory.

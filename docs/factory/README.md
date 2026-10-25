# CityFix factory

How AI agents build this project. You decide what to build and approve plans; agents write the code.

## Setup on a fresh clone
```
git config core.hooksPath .githooks
```
Then run `/reload-skills` (or restart Claude Code). The hook strips Claude attribution from commit messages.

## Commands
| Command | Purpose |
| --- | --- |
| `/feature-dev [task] [--worktree]` | Full flow with Gate 1 |
| `/bug-fix <task>` | Bug flow: reproduce, failing test, minimal fix |
| `/refactor <desc>` | Refactor, behavior unchanged |
| `/start-task`, `/plan-feature`, `/plan-fix`, `/code`, `/review-code`, `/test-code`, `/write-docs` | Individual phases |
| `/push-task` | Commit, verify, push the task branch (no PR) |
| `/merge [source] [into <target>]` | Merge with conflict resolution |
| `/finish-task` | Clean up after merge |
| `/work-status` | Status of every task |

## A day in the life of a task
1. Put the task in `docs/tasks/<name>.md`.
2. `/feature-dev <name>` creates the branch `feat/T-001-<slug>` and `state.md`.
3. The planner writes four plans; you approve at **Gate 1** (the only gate).
4. Implementer codes slice by slice (`verify` before each commit), reviewer reviews, tester tests, docs-writer documents.
5. `/push-task` pushes the branch. You merge into `main` (or ask `/merge feat/T-001-<slug> into main`), then `/finish-task`.

## Work-item folder
`docs/work/<ID>-<slug>/`: `00-plan.md`, `01-coding-plan.md`, `02-test-plan.md`, `03-docs-plan.md`, `state.md`, `review.md`.

## Regenerating
`docs/factory/manifest.md` is the source of truth. Re-run `/new-project` pointing at it to regenerate the factory.

---
name: work-item
description: "Work-item folder layout, task IDs, plan templates and state.md procedures. Use when planning, coding, reviewing or documenting a task."
user-invocable: false
---

# Work items

Each task has a folder `docs/work/<ID>-<slug>/`, committed on the person's dev branch:
`00-plan.md`, `01-coding-plan.md`, `03-docs-plan.md`, `state.md`, `review.md` (after review).

Templates: [templates.md](templates.md).

## IDs
There is no tracker. The person provides tasks as Markdown files in `docs/tasks/`; each file is the spec. IDs are per person: the first letter of the dev branch name plus a counter (`dev/Francisco` -> `F-001`, `dev/Diogo` -> `D-001`). To allocate the next one, find the highest ID with your prefix in `docs/work/` on the current branch and on `origin/main`, and add one. A task file may already name its ID: use it.

## state.md procedures
- Update the phase row (status, date, notes) when a phase starts and ends. Statuses: pending, done, skipped, blocked.
- Append to the Log: what each phase did, deviations from the plan and why, review/test loops, merges and conflict resolutions.
- Gate 1 is the only gate: record the person's decision (approve, revise, stop) and the date.
- Labels: `by-hand` means the person implements the coding plan themselves.

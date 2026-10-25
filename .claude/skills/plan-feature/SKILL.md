---
name: plan-feature
description: Runs the planner on a started task and holds Gate 1.
disable-model-invocation: true
argument-hint: "[task-id]"
---

# /plan-feature

Precondition: the task is started (`state.md` exists); otherwise say to run `/start-task` first.

1. Delegate to `planner` to write the four plans from the task file in `docs/tasks/`.
2. Show the summary: outcome, approach, slices, risks, open questions.
3. **Gate 1:** AskUserQuestion: approve, revise (notes go back to the planner), or stop. Record in `state.md`; commit the plans (`docs(<ID>): plan`).

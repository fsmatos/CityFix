---
name: plan-fix
description: Runs the planner in bug mode (reproduction, root cause, minimal fix) and holds Gate 1.
disable-model-invocation: true
argument-hint: "[task-id]"
---

# /plan-fix

Same as `/plan-feature`, with the planner in bug mode: exact reproduction, the failing test that proves it, root cause, minimal fix. Precondition: task started. Ends with Gate 1.

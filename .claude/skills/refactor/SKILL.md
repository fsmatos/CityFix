---
name: refactor
description: "Refactor flow: behavior must not change; tests pass before and after."
disable-model-invocation: true
argument-hint: "<description>"
---

# /refactor

Same flow as `/feature-dev` (see that skill), with commit type `refactor`:

- The planner fills `## Behavior that must not change`.
- Tests pass before and after without edits, apart from moves and renames.
- The reviewer explicitly checks for behavior changes.

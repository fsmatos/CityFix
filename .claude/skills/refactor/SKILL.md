---
name: refactor
description: "Refactor flow: behavior must not change."
disable-model-invocation: true
argument-hint: "<description>"
---

# /refactor

Same flow as `/feature-dev` (see that skill), with commit type `refactor`:

- Step 0 applies: before starting, always ask whether to merge `origin/main` into the dev branch.
- The planner fills `## Behavior that must not change`.
- The reviewer explicitly checks for behavior changes.

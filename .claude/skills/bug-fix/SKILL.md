---
name: bug-fix
description: "Bug flow: reproduce, minimal fix, review, docs."
disable-model-invocation: true
argument-hint: "<task-id | description>"
---

# /bug-fix

Same flow as `/feature-dev` (see that skill), with commit type `fix` and the `planner` in bug mode:

- Step 0 applies: before starting, always ask whether to merge `origin/main` into the dev branch.
- Reproduce first and record exact steps in `00-plan.md` (`## Reproduction`).
- Record the root cause (`## Root cause`).
- Keep the change minimal.

Gate 1 approves the reproduction, root cause and fix plan before code.

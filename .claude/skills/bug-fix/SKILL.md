---
name: bug-fix
description: Bug flow: reproduce, failing test first, minimal fix, review, test, docs.
disable-model-invocation: true
argument-hint: "<task-id | description>"
---

# /bug-fix

Same flow as `/feature-dev` (see that skill), with branch type `fix` and the `planner` in bug mode:

- Reproduce first and record exact steps in `00-plan.md` (`## Reproduction`).
- Write the failing test before any fix.
- Record the root cause (`## Root cause`).
- Keep the change minimal and keep the regression test.

Gate 1 approves the reproduction, root cause and fix plan before code.

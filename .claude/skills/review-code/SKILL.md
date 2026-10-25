---
name: review-code
description: Independent review of a task branch's diff against main.
disable-model-invocation: true
argument-hint: "[task-id]"
---

# /review-code

Precondition: commits on the branch. Delegate to `reviewer` on `git diff main...HEAD`; without plans, it plans from the diff. It writes `review.md`. Blocking findings go back to `implementer-kotlin`, then re-review; stop and ask after three loops. Update the Review row in `state.md`.

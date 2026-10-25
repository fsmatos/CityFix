---
name: review-code
description: "Independent review of a task's changes (from its recorded Base commit to HEAD)."
disable-model-invocation: true
argument-hint: "[task-id]"
---

# /review-code

Precondition: task commits exist (`git log --grep "(<ID>)"`). Delegate to `reviewer` on `git diff <Base>..HEAD`, where Base is the sha recorded in the task's `state.md`, limited to the task's files; without plans, it plans from the diff. It writes `review.md`. Blocking findings go back to `implementer-kotlin`, then re-review; stop and ask after three loops. Update the Review row in `state.md`.

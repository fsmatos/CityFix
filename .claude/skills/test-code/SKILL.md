---
name: test-code
description: "Runs the tester on a task: implements the test plan and runs the suites."
disable-model-invocation: true
argument-hint: "[task-id]"
---

# /test-code

Precondition: no open blocking review findings. Delegate to `tester`. Failures go back to `implementer-kotlin`; stop and ask after three loops. Update the Test row in `state.md`.

---
name: code
description: Implements the approved coding plan, all slices or one.
disable-model-invocation: true
argument-hint: "[task-id] [slice]"
---

# /code

Precondition: Gate 1 approved in `state.md`; otherwise say to run `/plan-feature` first.

Delegate each slice (or only the named one) to `implementer-kotlin`. `verify` runs before each commit; commits follow `git-workflow`. Update the Code row in `state.md`.

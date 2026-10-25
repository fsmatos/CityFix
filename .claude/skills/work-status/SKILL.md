---
name: work-status
description: "One line per task: phase, who owns it, pushed or merged."
disable-model-invocation: true
argument-hint: "[task-id]"
---

# /work-status

Read `state.md` from `docs/work/*/` on the current branch, and from `origin/dev/Francisco`, `origin/dev/Diogo` and `origin/main` with `git show <ref>:docs/work/<ID>-<slug>/state.md` (after `git fetch origin`). Print one line per task: ID, title, current phase, owner dev branch, pushed or not, merged into main or not.

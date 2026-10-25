---
name: work-status
description: One line per task: phase, branch, where it is checked out, pushed or not.
disable-model-invocation: true
argument-hint: "[task-id]"
---

# /work-status

For each local task branch (`<type>/T-...`), read its state with `git show <branch>:docs/work/<ID>-<slug>/state.md` (works without checking out). Print one line: ID, title, current phase, branch, where it is checked out (here, a worktree from `git worktree list`, or nowhere), pushed or not.

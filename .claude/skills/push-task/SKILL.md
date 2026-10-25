---
name: push-task
description: Commits pending work on a task branch, runs verify and pushes the branch with upstream tracking. No PR.
disable-model-invocation: true
argument-hint: "[task-id]"
---

# /push-task

Follow `git-workflow` ("Push").

1. Find the task. If none exists on this branch, create it (ID, slug, `state.md`); on `main` with changes, move them to a new task branch first.
2. Show pending changes and commit them in project style, no attribution.
3. Run `verify`; stop on failure.
4. `git push -u origin <branch>`. Never push `main`, never force-push. No PR is opened.
5. Record the remote branch in `state.md` (Pushed: done).

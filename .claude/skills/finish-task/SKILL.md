---
name: finish-task
description: "Marks a task merged once your dev branch has been merged into main, and syncs your dev branch."
disable-model-invocation: true
argument-hint: "[task-id]"
---

# /finish-task

Follow `git-workflow` ("Finishing"). Precondition: the task's commits are in `origin/main` (`git branch -r --contains <commit>` lists `origin/main`); otherwise refuse and say to merge dev into main first.

1. `git fetch origin`; on `dev/<name>` with a clean tree, merge `origin/main` into it with the `/merge` procedure if it is behind.
2. Mark Merged: done in the task's `state.md` and commit `chore(<ID>): merged`.
3. Never delete the dev branch.

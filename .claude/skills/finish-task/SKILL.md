---
name: finish-task
description: Cleans up after a task branch has been merged into main.
disable-model-invocation: true
argument-hint: "[task-id]"
---

# /finish-task

Follow `git-workflow` ("Finishing"). Precondition: the branch is listed by `git branch --merged main`; otherwise refuse and say to merge first.

1. Worktree: `ExitWorktree`, then `git worktree remove`. In place: `git switch main` (tree must be clean).
2. `git pull --ff-only`, then `git branch -d <branch>` (never `-D`).
3. Ask before deleting the remote branch.
4. Mark Merged in `state.md` if it is still reachable.

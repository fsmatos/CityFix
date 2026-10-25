---
name: start-task
description: Allocates a task ID and slug, creates the task branch (or worktree) and state.md.
disable-model-invocation: true
argument-hint: "[task-id | description] [type] [--worktree]"
---

# /start-task

Follow `git-workflow` ("Where tasks run", "Finding the current task") and `work-item`.

1. Resolve the task: a file in `docs/tasks/`, a given ID, or a description. Allocate the `T-` ID and slug; default type `feat`.
2. In place (default): on `main` with a clean tree, `git fetch origin main`, `git pull --ff-only`, `git switch -c <type>/<ID>-<slug>`. With uncommitted changes ask: carry, stash, or stop. On another non-task branch ask: adopt it, branch from it, or branch from `main`. On an existing task branch, say which task it is and continue it.
3. With `--worktree`: create the worktree from `origin/main`, copy `.worktreeinclude` files, run `./gradlew --version`, enter it with `EnterWorktree`.
4. Create `docs/work/<ID>-<slug>/state.md` from the template; commit `chore(<ID>): start` and stop.

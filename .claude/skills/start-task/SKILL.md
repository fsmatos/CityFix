---
name: start-task
description: "Allocates a task ID and slug on your dev branch and creates state.md."
disable-model-invocation: true
argument-hint: "[task-file | description]"
---

# /start-task

Follow `git-workflow` and `work-item`.

1. Check the current branch is `dev/<name>`. If it is `main` or something else, stop and ask which dev branch to use; never start a task on `main`.
2. Resolve the task: a file in `docs/tasks/` or a description. Allocate the ID (first letter of `<name>` + counter, e.g. `F-001`) and a slug (lowercase, hyphens, max 5 words).
3. Ensure the tree is clean (or ask: commit, stash, stop), `git fetch origin`, and show whether `origin/main` has commits not in the dev branch; offer `/merge` first.
4. Create `docs/work/<ID>-<slug>/state.md` from the template, with `Branch: dev/<name>` and `Base:` the current short sha.
5. Commit `chore(<ID>): start` and stop.

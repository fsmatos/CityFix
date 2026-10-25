---
name: merge
description: Merges any branch into any branch with conflict resolution and verification. Into main only when the person asks.
disable-model-invocation: true
argument-hint: "[source] [into <target>]"
---

# /merge

Follow `git-workflow` ("Merge"). Defaults: source `origin/main` (after a fetch), target the current branch.

1. Preflight: fetch, find where the target is checked out (`git worktree list`), require a clean tree, show incoming commits and diffstat. If the target is `main`, confirm once more with the commits that will land.
2. `git merge --no-ff --no-commit <source>`.
3. Resolve conflicts file by file, delegating large ones to `merge-resolver`. Ask when both sides changed the same behavior.
4. Run `verify` and the full unit tests; show the resolution table for approval.
5. Commit `merge: <source> into <target>` (no attribution). Append to the task's `state.md` log. Do not push; suggest `/push-task`.

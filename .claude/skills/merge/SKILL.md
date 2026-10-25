---
name: merge
description: "Merges any branch into any branch with conflict resolution and verification. Default brings origin/main into your dev branch; dev into main only when you ask."
disable-model-invocation: true
argument-hint: "[source] [into <target>]"
---

# /merge

Follow `git-workflow` ("Merge"). Defaults: source `origin/main` (after a fetch), target the current dev branch.

1. Preflight: fetch, require a clean tree on the target, show incoming commits and diffstat. If the target is `main`, the person must have asked in this conversation; confirm once more with the commits that will land, and require the dev branch to already contain `origin/main`.
2. `git merge --no-ff --no-commit <source>` (switch to the target branch first; never merge into someone else's dev branch).
3. Resolve conflicts file by file, delegating large ones to `merge-resolver`. Ask when both sides changed the same behavior.
4. Run `verify` and the full unit tests; show the resolution table for approval.
5. Commit `merge: <source> into <target>` (no attribution). Append to affected tasks' `state.md` logs. Do not push; suggest `/push-task`.

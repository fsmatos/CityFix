---
name: git-workflow
description: "Branch, commit, push, merge and conflict conventions for CityFix (personal dev branches). Use when committing, pushing or merging."
user-invocable: false
---

# Git workflow

Integration branch `main`, remote `origin`. Each person works directly on their own branch: `dev/Francisco` or `dev/Diogo`. There are no per-task branches and no worktrees.

## Tasks without task branches
A task is an ID plus its folder `docs/work/<ID>-<slug>/`, committed on the person's dev branch. IDs are per person: the first letter of the dev branch name plus a counter (`dev/Francisco` -> `F-001`, `dev/Diogo` -> `D-001`), so two people never clash.

Finding the current task: 1. the `<id>` argument; 2. otherwise the only task in `docs/work/` whose Merged row in `state.md` is not done; 3. if several, ask. Task commits carry the ID in the scope, so they can be found with `git log --grep "(<ID>)"`.

Always work on your own dev branch. If asked to work on `main` or another person's branch, stop and ask. Before switching branches the tree must be clean (commit `wip(<ID>): ...`, stash, or stop).

## Commits
- Conventional Commits with task ID: `feat(F-001): add login screen` (types: feat, fix, refactor, chore, docs, test). Body: what and why, wrapped at 72.
- One commit per slice; run `verify` first.
- Author is the person's own identity. Never `--author`, never change git config identity.
- **No attribution**: no `Co-Authored-By: Claude`, no `Claude-Session:`, no "Generated with Claude Code". The `.githooks/commit-msg` hook strips them; a fresh clone needs `git config core.hooksPath .githooks` once.
- Never amend or rebase pushed commits. Never `--no-verify`.

## Push
`git push -u origin dev/<name>` pushes the person's own dev branch (no PR). Never push `main`, never push another person's dev branch, never force-push.

## Merge
Merge commits, `--no-ff`, never rebase.
- **Default `/merge`:** bring `origin/main` into the current dev branch. This is how you receive the other person's work.
- **dev into main:** only the person, and only when they ask in this conversation (`/merge dev/Francisco into main`). Before it, merge `origin/main` into the dev branch first so the integration is tested on the dev branch.

Steps:
1. Preflight: fetch, clean tree, show `git log --oneline <target>..<source>` and `git diff --stat`.
2. `git merge --no-ff --no-commit <source>`.
3. Conflicts (`git diff --name-only --diff-filter=U`): read base/ours/theirs (`git show :1:`, `:2:`, `:3:`), the commits and plans on each side. Combine independent changes; ask when both sides changed the same behavior. Version catalog/lockfiles: take one side and re-run Gradle. Generated files: regenerate. Delete/modify, renames, binaries: ask.
4. Before committing: `git diff --cached --check`, search for conflict markers, run `verify`, show a resolution table (file, type, resolution, why) for approval.
5. Commit `merge: <source> into <target>`, summarizing the table. Append to the affected tasks' `state.md` log. Don't push.
`git merge --abort` only when the person asks.

## Finishing
After the dev branch is merged into `main`: mark the task Merged in `state.md`. Dev branches are long-lived: never delete them. Then `/merge` `origin/main` back into the dev branch.

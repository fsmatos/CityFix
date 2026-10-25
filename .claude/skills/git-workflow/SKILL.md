---
name: git-workflow
description: Branch, commit, push, merge and conflict conventions for CityFix. Use when committing, pushing or merging.
user-invocable: false
---

# Git workflow

Default branch `main`, remote `origin`. One task, one branch: `<type>/<ID>-<slug>` (type: feat, fix, refactor, chore; slug lowercase, hyphens, max 5 words); folder `docs/work/<ID>-<slug>/`.

## Finding the current task
1. The `<id>` argument. 2. The current branch name. 3. A `state.md` in `docs/work/` whose `Branch:` names the current branch. 4. Otherwise ask.

## Where tasks run
In place by default (task branch in the current checkout). Worktree only with `--worktree`: `git fetch origin main`, `git worktree add .claude/worktrees/<ID>-<slug> -b <type>/<ID>-<slug> origin/main`, copy gitignored files listed in `.worktreeinclude`, run `./gradlew --version`, then `EnterWorktree`. In place, the tree must be clean before switching branches (commit `wip(<ID>): ...`, stash, or stop).

## Commits
- Conventional Commits with task ID: `feat(T-001): add login screen`. Body: what and why, wrapped at 72.
- One commit per slice; run `verify` first.
- Author is the person's own identity. Never `--author`, never change git config identity.
- **No attribution**: no `Co-Authored-By: Claude`, no `Claude-Session:`, no "Generated with Claude Code". The `.githooks/commit-msg` hook strips them; a fresh clone needs `git config core.hooksPath .githooks` once.
- Never amend or rebase pushed commits. Never `--no-verify`.

## Push
`git push -u origin <branch>` pushes the task branch (no PR in this project). Never push `main`; never force-push. On `main` with uncommitted work: create the task, `git switch -c <branch>` (carries the changes), commit, push, then offer `git branch -f main origin/main`.

## Merge
Merge commits, `--no-ff`, never rebase. Only the person merges into `main` (or `/merge` when they ask in this conversation).
1. Preflight: fetch, clean tree, show `git log --oneline <target>..<source>` and `git diff --stat`.
2. `git merge --no-ff --no-commit <source>`.
3. Conflicts (`git diff --name-only --diff-filter=U`): read base/ours/theirs (`git show :1:`, `:2:`, `:3:`), the commits and plans on each side. Combine independent changes; ask when both sides changed the same behavior. Version catalog/lockfiles: take one side and re-run Gradle. Generated files: regenerate. Delete/modify, renames, binaries: ask.
4. Before committing: `git diff --cached --check`, search for conflict markers, run `verify` and the full tests, show a resolution table (file, type, resolution, why) for approval.
5. Commit `merge: <source> into <target>`, summarizing the table. Append to the task's `state.md` log. Don't push.
`git merge --abort` only when the person asks.

## Finishing
After merge into `main`: leave the worktree (`ExitWorktree`, `git worktree remove`) or `git switch main`; `git pull --ff-only`; `git branch -d <branch>` (never `-D`); ask before deleting the remote branch.

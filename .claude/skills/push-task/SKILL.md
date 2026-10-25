---
name: push-task
description: "Commits pending work, runs verify and pushes your own dev branch. No PR."
disable-model-invocation: true
argument-hint: "[task-id]"
---

# /push-task

Follow `git-workflow` ("Push").

1. Check the current branch is `dev/<name>`. On `main` with changes, ask before moving them to the dev branch (`git switch dev/<name>` carries them if no conflict). Never push `main` or someone else's dev branch.
2. Show pending changes and commit them in project style, no attribution.
3. Run `verify`; stop on failure.
4. `git push -u origin dev/<name>` (plain `git push` afterwards). Never force-push. No PR is opened.
5. Record the push in the current task's `state.md` (Pushed: done) and commit that.

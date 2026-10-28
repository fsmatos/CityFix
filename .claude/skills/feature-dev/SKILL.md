---
name: feature-dev
description: "Full feature flow: start, plan, Gate 1, code, review, docs, push offer."
disable-model-invocation: true
argument-hint: "[task-id | description]"
---

# /feature-dev

Follow the `work-item` and `git-workflow` skills. Run in the main conversation; delegate to agents.

0. **Sync with main (always ask first):** before anything else, on your `dev/<name>` branch (Diogo or Francisco), `git fetch origin`, show how many commits `origin/main` has that the branch lacks (`git log --oneline HEAD..origin/main`), and AskUserQuestion whether to merge `origin/main` into the dev branch now. Ask even when it is already up to date. On yes, run the `/merge` procedure (`origin/main` into the dev branch) before starting the task; on no, record the choice in the task's `state.md` Log once it exists.
1. **Load the task:** if an unfinished task exists on the current dev branch, offer to continue it from its `state.md`. Otherwise read the task file in `docs/tasks/` (or take the description), allocate the ID (first letter of the dev branch + counter), pick the slug and commit type.
2. **Start:** run the `/start-task` procedure (on your `dev/<name>` branch). Create `docs/work/<ID>-<slug>/state.md`.
3. **Plan:** delegate to the `planner`. Show a short summary: outcome, approach, slices, risks, open questions.
4. **Gate 1:** AskUserQuestion: approve, revise (notes go back to the planner), or stop. Record the decision in `state.md`. Commit the plans (`docs(<ID>): plan`).
5. **Code:** delegate each slice to `implementer-kotlin`; `verify` runs before each commit.
6. **Review:** `reviewer` writes `review.md`. Blocking findings go back to the implementer, then re-review. After three loops, stop and ask.
7. **Docs:** `docs-writer`.
8. **Push:** ask whether to run `/push-task` now.

`by-hand` label: after Gate 1, stop and tell the person the coding plan is theirs to implement; they resume with `/review-code`.

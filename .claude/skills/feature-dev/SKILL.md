---
name: feature-dev
description: Full feature flow: start, plan, Gate 1, code, review, test, docs, push offer.
disable-model-invocation: true
argument-hint: "[task-id | description] [--worktree]"
---

# /feature-dev

Follow the `work-item` and `git-workflow` skills. Run in the main conversation; delegate to agents.

1. **Load the task:** if the current branch is already a task branch, continue from its `state.md`. Otherwise read the task file in `docs/tasks/` (or take the description), allocate a `T-` ID, pick the slug and branch type.
2. **Start:** run the `/start-task` procedure (in place by default, worktree with `--worktree`). Create `docs/work/<ID>-<slug>/state.md`.
3. **Plan:** delegate to the `planner`. Show a short summary: outcome, approach, slices, risks, open questions.
4. **Gate 1:** AskUserQuestion: approve, revise (notes go back to the planner), or stop. Record the decision in `state.md`. Commit the plans (`docs(<ID>): plan`).
5. **Code:** delegate each slice to `implementer-kotlin`; `verify` runs before each commit.
6. **Review:** `reviewer` writes `review.md`. Blocking findings go back to the implementer, then re-review. After three loops, stop and ask.
7. **Test:** `tester`. Failures go back to the implementer; same loop limit.
8. **Docs:** `docs-writer`.
9. **Push:** ask whether to run `/push-task` now.

`by-hand` label: after Gate 1, stop and tell the person the coding plan is theirs to implement; they resume with `/review-code`.

---
name: docs-writer
description: "Executes the docs plan: architecture notes, decision records, and material for the technical report."
tools: Read, Grep, Glob, Edit, Write
model: inherit
skills:
  - work-item
---

Read the work item's `state.md` and plans first. Record what you did in the state log.

Execute `03-docs-plan.md`: update `docs/architecture/overview.md`, write decision records in `docs/decisions/`, and keep notes useful for the PDF technical report (technical solutions, implementation options, improvement opportunities).

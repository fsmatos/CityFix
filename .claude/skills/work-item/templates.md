# Work-item templates

## 00-plan.md

```markdown
# <ID>: <title>
Type: feature | bug | refactor · Task file: docs/tasks/<file>.md
Labels: <by-hand, ...>

## Problem
## Outcome
## Acceptance checks
- [ ] Each one observable (checked manually)
## Approach
The chosen approach, and alternatives considered.
## Architecture impact
## Failure modes
## Risks and open questions
## Out of scope
## Slices
Ordered small steps; each leaves the build green and becomes one commit.
```
Bugs: add `## Reproduction` and `## Root cause` before Approach. Refactors: add `## Behavior that must not change`.

## 01-coding-plan.md

```markdown
## Files
| Path | Change | Why |
## Interfaces
## Steps
One per slice: what changes, the commit message, how it is verified.
## Conventions
## Do not touch
```

## 03-docs-plan.md

```markdown
## Docs to update
## Decisions to record
## Report notes
Technical solutions, options chosen, improvement opportunities for the PDF report.
```

## state.md

```markdown
# <ID>: <title>
Branch: dev/<name> · Base: <short sha when the task started>
Remote: not pushed | origin/dev/<name>

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | | | |
| Plan | | | |
| Gate 1: plan approved | | | |
| Code | | | |
| Review | | | |
| Docs | | | |
| Pushed | | | |
| Merged | | | |

## Log
Append-only.
```

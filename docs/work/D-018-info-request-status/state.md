# D-018: Information requests: right operator button and answered state
Branch: dev/Diogo · Base: d5d9ea1
Remote: origin/dev/Diogo

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-21 | Bug 7 from Francisco's tests; option A chosen |
| Plan | done | 2026-11-21 | |
| Gate 1: plan approved | done | 2026-11-21 | Diogo approved (option A) |
| Code | done | 2026-11-21 | Slice 1; build and lint green |
| Review | skipped | 2026-11-21 | Small UI fix, deadline; manual check on the phone instead |
| Docs | done | 2026-11-21 | overview.md, note in decision 0021 |
| Pushed | done | 2026-11-21 | 7527f97..fc70993 |
| Merged | done | 2026-11-21 | merge 2accd94 into main (not pushed) |

## Log
- 2026-11-21: plan written. Root cause: ActionsRow always labels IN_PROGRESS "Start work"; nothing compares the request with later messages.
- 2026-11-21: Gate 1 approved by Diogo (option A).
- 2026-11-21: slice 1 coded as planned: `model/InfoRequest.kt` (pendingInfoRequest, isAnswered), `ui/components/InfoRequestCard.kt` with two previews, card first in the slot of OperatorTicketContent and TicketDetailContent, `resumesWork` on ActionsRow and OperatorTextDialog. Deviation: none. Manual checks on the phone still to do.
- 2026-11-21: docs: overview.md components list, amendment note in decision 0021.
- 2026-11-21: pushed dev/Diogo (7527f97..fc70993) after verify (build + lint green).
- 2026-11-21: merged origin/main into dev/Diogo (4313dd9, no file changes), then dev/Diogo into main (2accd94), no conflicts, build and lint green. main not pushed yet.
- 2026-11-21: manual device checks passed (Diogo, emulator + phone): all acceptance checks of this task.

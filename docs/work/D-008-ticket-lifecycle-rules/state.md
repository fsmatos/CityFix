# D-008: Ticket lifecycle rules (pure)
Branch: dev/Diogo · Base: 66691af
Remote: origin/dev/Diogo

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-11 | dev/Diogo already contains origin/main (8b52e5b) |
| Plan | done | 2026-11-11 | 00-plan, 01-coding-plan, 03-docs-plan; 3 slices, no tests; Q1-Q11 for Gate 1 |
| Gate 1: plan approved | done | 2026-11-11 | Approved by Diogo: Q1-Q11 as recommended |
| Code | done | 2026-11-11 | 3 slices: 9bb1cb3, 2df8cc4, 1c297f8; verify green, lint 0 errors / 16 warnings; T1-T8, R1-R16 walked |
| Review | pending | | |
| Docs | pending | | |
| Pushed | pending | | |
| Merged | pending | | |

## Log
Append-only.

- 2026-11-11 Start: task file `docs/tasks/phase-2/D-008-ticket-lifecycle-rules.md` (target 2026-11-10, one day late). No merge needed: dev/Diogo already contains origin/main at 8b52e5b. F-010 (which may add the attachment phase field, decision 0010) is not on main yet.
- 2026-11-11 Plan: planner wrote the three plans. New pure `model/TicketLifecycle.kt` plus `AttachmentPhase` and `Attachment.phase`; no screen, repository, TicketsList or SampleTickets change; stats and the public list unaffected. 3 slices. Deviations from the task file flagged in the plan (extension style, `transition` returns `Ticket?`, `allowedTransitions(by, now)`, `proof` parameter, no SampleTickets change, decision 0011). Limitation: neither the planner nor the orchestrator could open the brief PDF (no PDF tools in the session), so the brief II and I.3.E rules come from the task files; Diogo to check brief II before Q1/Q2.
- 2026-11-11 Gate 1: approved by Diogo with the recommended answers to Q1-Q11 (citizen question is a plain message; creator and ACTIVE followers reopen within 5 days with justification; CLOSED only computed/written as "system"; justification required for INFO_REQUESTED and reopen; `allowedTransitions(by, now)`; `AttachmentPhase` added in D-008 with decision 0011, tell Francisco; `null` on rejected moves; proof passed with RESOLVED; no committed tests; no SampleTickets change; pending citizens get no transitions). Q1/Q2 were not checked against brief II (PDF unreadable in this session).
- 2026-11-11 Code slice 1 (9bb1cb3 `feat(D-008): add attachment phase for resolution proof`): new `model/AttachmentPhase.kt`; `Attachment.phase` (default REPORT, last parameter); `id`/`file` KDoc fixed (F-007 N7); `phase` added to the kept list in `TicketPrivacy.kt` KDoc. All `Attachment(` calls use named arguments. Verify: assembleDebug pass, lint pass (0 errors, 16 warnings, baseline). Import grep empty.
- 2026-11-11 Code slice 2 (2df8cc4 `feat(D-008): add reopen window and closing rules`): new `model/TicketLifecycle.kt` with `REOPEN_WINDOW_MS`, `CLOSING_WARNING_MS`, `resolvedAt`, `canReopen`, `effectiveStatus`, `isLocked`, `closingWarningAt`, private `isFullTicket`; `Ticket.currentStatus` KDoc line (D-007 N2). Verify: assembleDebug pass, lint pass (0 errors, 16 warnings). Grep for Android/Firebase imports and `System.currentTimeMillis`: empty. T1-T8 walked (2df8cc4): all hold.
- 2026-11-11 Code slice 3 (1c297f8 `feat(D-008): add ticket status transitions`): `SYSTEM_RESPONSIBLE_ID`, `allowedTransitions(by, now)`, `transition(...)`, `closeIfExpired`, private `isParticipant` and `operatorTargets` (exhaustive `when`, no `else`). Verify: assembleDebug pass, lint pass (0 errors, 16 warnings). Grep: no imports at all in `TicketLifecycle.kt`, no `System.currentTimeMillis`. R1-R16 walked (1c297f8): all hold; also checked every accepted path adds one history entry and sets `currentStatus` in the same `copy`, `operatorTargets` matches the 00-plan table, `canReopen`/`effectiveStatus` use `<`/`>=` on the same difference.
- 2026-11-11 Code deviations: none in behaviour. Small ones: `SYSTEM_RESPONSIBLE_ID` added in slice 3 (the plan allowed it); `isFullTicket` was added in slice 2 as planned but is first used in slice 3; KDoc for `closingWarningAt` cites "Brief III" as the plan text does (to confirm against the brief in the docs phase). No SampleTickets, screen, repository or docs file changed. Not pushed.

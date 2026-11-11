# D-008: Ticket lifecycle rules (pure)
Branch: dev/Diogo · Base: 66691af
Remote: origin/dev/Diogo

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-11 | dev/Diogo already contains origin/main (8b52e5b) |
| Plan | done | 2026-11-11 | 00-plan, 01-coding-plan, 03-docs-plan; 3 slices, no tests; Q1-Q11 for Gate 1 |
| Gate 1: plan approved | done | 2026-11-11 | Approved by Diogo: Q1-Q11 as recommended |
| Code | pending | | |
| Review | pending | | |
| Docs | pending | | |
| Pushed | pending | | |
| Merged | pending | | |

## Log
Append-only.

- 2026-11-11 Start: task file `docs/tasks/phase-2/D-008-ticket-lifecycle-rules.md` (target 2026-11-10, one day late). No merge needed: dev/Diogo already contains origin/main at 8b52e5b. F-010 (which may add the attachment phase field, decision 0010) is not on main yet.
- 2026-11-11 Plan: planner wrote the three plans. New pure `model/TicketLifecycle.kt` plus `AttachmentPhase` and `Attachment.phase`; no screen, repository, TicketsList or SampleTickets change; stats and the public list unaffected. 3 slices. Deviations from the task file flagged in the plan (extension style, `transition` returns `Ticket?`, `allowedTransitions(by, now)`, `proof` parameter, no SampleTickets change, decision 0011). Limitation: neither the planner nor the orchestrator could open the brief PDF (no PDF tools in the session), so the brief II and I.3.E rules come from the task files; Diogo to check brief II before Q1/Q2.
- 2026-11-11 Gate 1: approved by Diogo with the recommended answers to Q1-Q11 (citizen question is a plain message; creator and ACTIVE followers reopen within 5 days with justification; CLOSED only computed/written as "system"; justification required for INFO_REQUESTED and reopen; `allowedTransitions(by, now)`; `AttachmentPhase` added in D-008 with decision 0011, tell Francisco; `null` on rejected moves; proof passed with RESOLVED; no committed tests; no SampleTickets change; pending citizens get no transitions). Q1/Q2 were not checked against brief II (PDF unreadable in this session).

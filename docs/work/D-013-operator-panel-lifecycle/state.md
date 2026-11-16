# D-013: Operator: operations panel and ticket lifecycle
Branch: dev/Diogo · Base: 954ff6e
Remote: origin/dev/Diogo

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-16 | Task file `docs/tasks/phase-2/D-013-operator-panel-lifecycle.md` |
| Plan | done | 2026-11-16 | `00-plan.md`, `01-coding-plan.md`, `03-docs-plan.md`; 7 slices + 2 optional; Q1-Q13 open |
| Gate 1: plan approved | done | 2026-11-16 | Approved by Diogo; Q1-Q13 as recommended; slice 8 only if 1-7 done by 2026-11-19, slice 9 only if time remains |
| Code | pending | | |
| Review | pending | | |
| Docs | pending | | |
| Pushed | pending | | |
| Merged | pending | | |

## Log
- 2026-11-16 Start: `origin/main` had no commits missing from `dev/Diogo`; Diogo chose not to run `/merge`. Commit type `feat`. Untracked `.idea/` files left out of the commit.
- 2026-11-16 Plan: read the task file, D-014's task file, the overview (D-008, F-011 to F-014, D-012 sections and handovers, routes, roles, rules notes), decisions 0011/0017/0018, `TicketLifecycle.kt`, `TicketsRepository`/`FirestoreTicketsRepository`/`FirestoreMapping`, `AttachmentUploader`/`UploadWorker`, `ShowScreen`, `TicketDetailScreen`/`ViewModel`, `MessageThread`, `TicketMap`, `MainActivity`, `RoleAccess`, `Destinations`, the draft `firestore.rules`/`storage.rules`. Findings that change the task file: the operator's shared list is already `allTickets()` (no `operatorTickets` needed); `askForClarification` already does Request information's batch (status + message) for any user; D-008 makes `addResolutionProof` unnecessary. Proposed instead: `startWork` and `resolveTicket` in Francisco's `TicketsRepository` over his `commitStatusChange` (new optional `newAttachments`, `arrayUnion`), `Attachment.toDocument()` made `internal`, and an additive `responsibleNames` + proof label in his `ShowScreen` (signatures to agree with Francisco, Q2-Q4). No new route (operator branch of `TicketDetailRoute`, Q1). D-014 hook only: `needsOperatorGeofence(now)`. No rules edit or deploy (Q9). Slices 1-7 required, 8 (panel map) and 9 (joiners' reports) optional. Nothing committed.
- 2026-11-16 Gate 1: Diogo approved the plan with Q1-Q13 as recommended (no new detail route; `startWork`/`resolveTicket` in `TicketsRepository` to confirm with Francisco; no rules edit or deploy). Slices 1-7 to implement; slice 8 only if 1-7 are done and checked by 2026-11-19; slice 9 only if time remains.

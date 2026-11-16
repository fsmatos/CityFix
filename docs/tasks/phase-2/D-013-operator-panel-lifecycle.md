# D-013: Operator: operations panel and ticket lifecycle

Owner: Diogo (`dev/Diogo`) · Layer: `ui` / `viewmodel` / `data` · Phase 2 · Status: code done, reviewed, device checks pending (2026-11-16) · Target: 2026-11-18
Depends on: D-008, F-011, F-013 · Uses: F-014's message thread · Blocks: D-014

## Goal
Give operators the full view of every ticket and the controls to move it through its lifecycle.

## Why
- Brief I.3.E: operational panel with all reports, with coordinates, author contacts and full history with photos and videos; lifecycle control: status transitions with timestamps, requesting information, recording the start of work, uploading the photo/video of the finished work.
- Brief II.2-II.5.

## What to build
- **Operations panel:** all tickets (any scope), filters by status and category, sort by date; optional map view with markers (F-012 map component).
- **Operator detail:** exact location on the map, author and followers' contacts (from `users`), full history, all attachments, and F-014's message thread.
- **Actions** limited by D-008's `allowedTransitions` for operators: Request information (with a text that is also posted as a message), Start work (IN_PROGRESS; turns on D-014's geofence for this ticket), Resolve (requires a photo/video picked or taken, uploaded through F-013's `AttachmentUploader` with phase RESOLUTION). Each writes the transition to `tickets` and `publicTickets` (status only) in one batch.
- **Repository methods** (`changeStatus`, `addResolutionProof`, `operatorTickets`): add them to `TicketsRepository` after agreeing the signatures with Francisco.
- Closed tickets (D-008 `effectiveStatus`) are read-only.

## Files
New `ui/screens/OperatorPanelScreen.kt`, `ui/screens/OperatorTicketScreen.kt`, `viewmodel/OperatorTicketsViewModel.kt`; `data/TicketsRepository.kt`, `data/FirestoreTicketsRepository.kt` (with Francisco), navigation, strings EN/PT.

## Acceptance checks
- [ ] The operator sees private tickets and author contacts; a citizen never does.
- [ ] Only valid transitions are offered; each one appears in the history with time and operator.
- [ ] Resolve is impossible without proof media; the proof uploads offline-safe.
- [ ] The citizen sees the new status live.

## References
Brief I.3.E, II; D-008; decisions 0004, 0007, 0010.

## Status notes
- 2026-11-16: Gate 1 approved by Diogo with Q1-Q13 as recommended (`docs/work/D-013-operator-panel-lifecycle/00-plan.md`). Slices 1-7 committed (`ae0bc42`..`bc74b4f`), review round 1 fixes (`a5f4df1`, `12334ec`), round 2 approve. Decision [0021](../../decisions/0021-operator-panel-and-lifecycle.md).
- Deviations from this task file (Gate 1): no `operatorTickets` (the operator's list is already `allTickets()`); no `addResolutionProof` (the proof goes with the RESOLVED transition, D-008); no `changeStatus` (explicit `startWork` and `resolveTicket` instead, plus `askForClarification` reused for Request information); no new detail route (operator branch of `TicketDetailRoute`, Q1); no `OperatorTicketsViewModel` (`OperatorPanelViewModel` and `OperatorTicketViewModel` instead); `PlaceholderScreen` kept.
- Optional work not done: the panel map (slice 8) and the joiners' reports (slice 9).
- Known limits, not fixed: proof draft lost on process death with its files left on the device (N5); `onCleared` deletes on the main thread (N2); the draft rules would refuse the resolve batch (no `resolvedAt`; not deployed, Q9); concurrent status changes last-write-wins.
- Device checks D1-D11 not run (two devices and airplane mode needed); the acceptance boxes above stay unticked until they pass. D12 (build + lint per slice) done.

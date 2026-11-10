# D-013: Operator: operations panel and ticket lifecycle

Owner: Diogo (`dev/Diogo`) · Layer: `ui` / `viewmodel` / `data` · Phase 2 · Status: to do · Target: 2026-11-18
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

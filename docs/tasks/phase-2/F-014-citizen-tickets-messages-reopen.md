# F-014: Citizen: my tickets, message thread and reopening

Owner: Francisco (`dev/Francisco`) · Layer: `ui` / `viewmodel` / `data` · Phase 2 · Status: done (2026-11-15; device checks D1-D15 open) · Target: 2026-11-17
Depends on: F-011, D-008, D-009 · Used by: D-013 (same message thread for operators), F-016

Gate 1 changes (2026-11-15, [decision 0018](../../decisions/0018-citizen-tickets-messages-reopen.md)): a citizen's clarification request sets INFO_REQUESTED (Q1, brief II.2); My tickets entry kept as D-009 left it (Q2); no expand toggle (Q13).

## Goal
Let a registered citizen follow their own tickets, talk to the council on each one, and reopen a resolved ticket within 5 days.

## Why
- Brief I.2.E: list of all the user's tickets with status; detail with message exchange; reopen within 5 days of resolution.
- Brief II.2: info requests go both ways and the other side is notified (notification part in F-016).

## What to build
- **"My tickets" screen** (citizen only): tickets created or followed by the user, any scope, with status badge; empty state. Route and top-bar entry: coordinate with Diogo (navigation is his).
- **Citizen detail:** full detail for the author/followers (exact location, description, history), reusing `ShowScreen` with a citizen audience (decision 0007).
- **Message thread component** `ui/components/MessageThread` + `messages` subcollection read/write in the repository (`messagesFor(ticketId): Flow`, `sendMessage(ticketId, text)`). Generic so D-013 shows the same thread to operators. A citizen asking for clarification can move the ticket to INFO_REQUESTED using D-008's rules (decide at Gate 1 whether the citizen's question changes status).
- **Reopen:** button visible only when D-008's `canReopen(ticket, now)` is true; asks for a justification; adds an OPEN history entry with the justification and updates `currentStatus` in both collections.
- Closed tickets are read-only (no messages, no reopen).

## Files
New `ui/screens/MyTicketsScreen.kt`, `ui/components/MessageThread.kt`, a `viewmodel/TicketDetailViewModel.kt`; `ui/screens/ShowScreen.kt`, `data/TicketsRepository.kt`, `data/FirestoreTicketsRepository.kt`, navigation (with Diogo), strings EN/PT.

## Acceptance checks
- [ ] A citizen sees only their own and followed tickets in "My tickets".
- [ ] Messages appear live on both sides and work offline (queued).
- [ ] Reopen shows on day 4 after resolution and not on day 6; it requires a justification.
- [ ] Anonymous users never reach this screen or the thread.

## References
Brief I.2.E, II.2, II.4, II.5; decisions 0007, 0010; D-008.

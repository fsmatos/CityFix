# F-014: Citizen: my tickets, message thread and reopening
Type: feature · Task file: docs/tasks/phase-2/F-014-citizen-tickets-messages-reopen.md · Labels: (none)

One-page summary for Gate 1. Details: `01-coding-plan.md`.

## Problem
Brief I.2.E, II.2, II.4, II.5: a registered citizen lists their tickets, talks to the council in a thread, and reopens with a justification within 5 days (locked after).
After D-009, `MyTicketsRoute` is a placeholder, the detail is always PUBLIC, and nothing uses `messages` or D-008's `transition`/`closeIfExpired`.

## Outcome
- **My tickets**: tickets the active citizen created or follows, newest first, effective badge, empty state.
- **Participant detail**: description, media, exact map, history (justifications, "Closed automatically"), Reopen, thread. Everyone else sees PUBLIC (0007).
- **Thread**: live, works offline, labels "You" / "Municipality" / "Citizen" with date-time; D-013 reuses it.
- **Reopen**: only inside the 5-day window, non-blank justification, one batch.
- **Closed is read-only**; an expired RESOLVED ticket is written CLOSED when opened.
- Anonymous and pending users never read `messages`.

## Approach
Details: `01-coding-plan.md`.
- The user is always `session.ticketsUser` (D-009); `null` means PUBLIC.
- `messagesFor` / `sendMessage` on the `messages` subcollection, queued offline.
- One status batch for reopen and close-on-read: ticket, `publicTickets`, `ticketLocations.active`, `stats/summary`.
- `detailAudience(user, ticket)` picks PUBLIC, REGISTERED or OPERATOR.
- `ShowScreen` gains `now`, an exact map, history labels and a participant slot.
- `TicketDetailViewModel` per destination, only for participants and operators.
- Stateless `MessageThread` (plain `Column`) and `ReopenDialog`.
- **Ask for clarification (Q1, brief II.2):** a citizen's clarification request is a message that also moves the ticket to INFO_REQUESTED through D-008's `transition` (new citizen move in `TicketLifecycle.kt`, Diogo's file, needs his OK); the council's answer moves it back (D-013).
- `MyTicketsScreen` filters the existing ticket list; navigation unchanged.

**Beyond class:** F-011's `listen` Flow; `FieldValue.increment` via `statsIncrements`; the `ShowScreen` content slot.

## Slices
Each builds, passes lint and is one commit. Acceptance = device checks D1-D15.
1. Messages data layer: `Message.byOperator`, mapping, `messagesFor`, `sendMessage`.
2. Status batch: `toStatusFields`, `commitStatusChange`, `reopenTicket`, `closeExpiredTicket`.
3. `ShowScreen` for participants: `now`, history, exact map, slot, `detailAudience`.
4. `MessageThread` component.
5. Detail ViewModel, UiState, factory.
6. Detail wiring: screen, `ReopenDialog`, close-on-read, "Ask for clarification" (Q1).
7. My tickets: screen, `TicketCard.status`, placeholder replaced.

## Risks
- Q1 changes Diogo's lifecycle rules (citizen -> INFO_REQUESTED) and pauses his D-014 geofence while a ticket waits for information; he must agree before slice 6.
- Diogo's files change (`MainActivity.kt`, `Message.kt`, `TicketLifecycle.kt`, comments in `Destinations.kt`, `PlaceholderScreen.kt`); D-010 may edit `MainActivity.kt`. Tell him before slices 1, 3, 7.
- Followers read each other's messages (one thread per ticket); author ids never shown.
- A missing document (no `ticketLocations`) fails the whole batch.
- Offline races are last-write-wins (Q12).
- Day 4 / day 6 checks need hand-edited RESOLVED entries and stats.
- MFA login needs Francisco's SHA-1/SHA-256 in Firebase and a Google Play AVD.

## Open questions (answered by the person, 2026-11-15)
- Q1 Citizen's clarification request: **follow brief II.2: it sets INFO_REQUESTED** (state 2 "Pedido de Informações" covers both directions); Diogo must agree (his `TicketLifecycle.kt`).
- Q2 My tickets entry: **keep what D-009 decided** (menu, route, title).
- Q3 Offline messages: **queued** (brief IV: messages in Firestore with offline read and write).
- Q4 Author label: **`byOperator` field**.
- Q5 Participant check: **public `isParticipant`**.
- Q6 Close-on-read: **when a participant opens the detail**.
- Q7 Resolve time: **the RESOLVED history entry** (D-008); `resolvedAt` not mapped, not cleared.
- Q8 Location for participants: **respect the brief: exact map** (I.1.C hides it only from anonymous users).
- Q9 Operator audience: **respect the brief: OPERATOR prepared now** (I.3.E data request, II.2 both ways); visible from D-013.
- Q10 History: **respect the brief: every entry with date, justifications and "Closed automatically"** (full lifecycle traceability, I.3.E timestamps, II.4 justification).
- Q11: superseded by Q15.
- Q12 Concurrency: **last write wins**.
- Q13 Expand toggle on My tickets: **no**.
- Q14 Test accounts: **Diogo's A/P/O + a new active citizen C**.
- Q15 Decision number: **F-013's record renamed to 0017; F-014 uses 0018**.

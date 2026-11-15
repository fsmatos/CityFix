# 0018: Citizen tickets, messages and reopen

Status: accepted (F-014, Gate 1 on 2026-11-15, Q1-Q15 answered by the person; Diogo agreed to Q1). Amends [0011](0011-ticket-lifecycle-rules.md) Q1 and the transition table.

## Context
- Brief I.2.E, II.2, II.4, II.5: a registered citizen lists their tickets, exchanges messages with the council, and reopens within 5 days.
- D-008's rules ([0011](0011-ticket-lifecycle-rules.md)) and D-009's session ([0015](0015-session-role-navigation-mfa.md)) existed; nothing used them yet.

## Decision
- **Q1, clarification sets INFO_REQUESTED.** The person's reading of brief II.2: state 2 "Pedido de Informações" covers the citizen's clarification request too. An active participant may move OPEN or IN_PROGRESS to INFO_REQUESTED; the question is the justification and a message, in one batch. Rejected: a plain message (0011's old Q1).
- **Q2:** My tickets entry as D-009 left it (menu, route, title). Rejected: a top-bar icon.
- **Q3, messages queued offline** (brief IV: offline read and write). `tickets/{id}/messages`, live listener, `document().set()` not awaited.
- **Q4:** `Message.byOperator` labels "Municipality"; author ids never shown.
- **Q5:** `isParticipant` made public in `TicketLifecycle.kt`.
- **Q6:** an expired RESOLVED ticket is written CLOSED when a participant opens the detail.
- **Q7:** repair time from the RESOLVED history entry; Firestore `resolvedAt` not read (closes point 1 of the overview's "Open: lifecycle fields").
- **Q8-Q10, as the brief says:** participants see the exact map (I.1.C hides it only from anonymous users); the OPERATOR audience is prepared now (I.3.E, II.2); the history shows every entry with date, justification and "Closed automatically".
- **One status batch** (`commitStatusChange`) for reopen, close and clarification: ticket, `publicTickets` mirror, `ticketLocations.active`, `stats/summary` ([0004](0004-denormalised-current-status.md), [0013](0013-firestore-reads-and-dashboard-counters.md)).
- **Audience:** `detailAudience(session.ticketsUser, ticket)`; only participants and operators create `TicketDetailViewModel` ([0007](0007-ticket-detail-by-audience.md)).
- **Q12:** last write wins. **Q13:** no expand toggle on My tickets. **Q14:** Diogo's A/P/O accounts plus a new citizen C. **Q15:** F-013's record renamed 0017; this is 0018.

## Consequences
- **D-013:** reuse `MessageThread` and the detail with `DetailAudience.OPERATOR`; operator moves go through `commitStatusChange` (via a new public repository function). Answering moves INFO_REQUESTED to IN_PROGRESS, so a ticket asked about while OPEN comes back IN_PROGRESS; D-013 decides whether to add INFO_REQUESTED -> OPEN.
- **D-014:** a clarification on an IN_PROGRESS ticket pauses the geofence until the answer.
- **F-016:** notify the other side (II.2) and the 24 h warning.
- **Limits:** device checks D1-D15 not run (test accounts pending); last write wins offline; followers of a ticket share one thread.

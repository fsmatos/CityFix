# F-014: Citizen: my tickets, message thread and reopening
Branch: dev/Francisco · Base: 206b5f7
Remote: origin/dev/Francisco

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-14 | feature, task file `docs/tasks/phase-2/F-014-citizen-tickets-messages-reopen.md` |
| Plan | done | 2026-11-14 | 00-plan, 01-coding-plan, 03-docs-plan; 7 slices; Q1-Q12 with defaults |
| Gate 1: plan approved | pending | | |
| Code | pending | | |
| Review | pending | | |
| Docs | pending | | |
| Pushed | pending | | |
| Merged | pending | | |

## Log
Append-only.
- 2026-11-14: fetched origin; origin/main had 0 commits missing from dev/Francisco; the person chose not to merge (already up to date). Started F-014 from the task file. D-009 (login) is not done yet (origin/dev/Diogo has nothing after D-008), so F-014 keeps the F-011 `currentUser` stand-in.
- 2026-11-14: Plan written (planner).
  - Read first: the task file, the brief (I.2.E, II.2, II.4, II.5, checked in the brief text), decisions 0007/0010/0011/0012/0013/0014, the overview handovers, the repository/mapping/ViewModel/ShowScreen/navigation/MainActivity code, the Contacts_#3 and T8 demos, and class slides 04/07/08/09 with P06-P10.
  - Approach:
    - messages subcollection with a live listener and queued `document().set()`;
    - one private `commitStatusChange` batch for reopen and close-on-read (ticket status fields, `publicTickets` mirror, `ticketLocations.active = isActive`, `statsIncrements`);
    - `detailAudience(user, ticket)` with the REGISTERED slot in `ShowScreen`;
    - a per-destination `TicketDetailViewModel`, created only for participants and operators;
    - a stateless `MessageThread` for D-013;
    - `MyTicketsScreen` behind a citizen-only top-bar action.
  - 7 slices.
  - Open questions Q1-Q12 for Gate 1. Q1: brief II.2 lists citizen clarification requests under "Pedido de Informações"; the plan recommends keeping D-008 Q1 (plain message).
  - Diogo's files touched if the defaults pass: `Destinations.kt`, `MainTopAppBar.kt`, `MainActivity.kt`, `Message.kt` (Q4), `TicketLifecycle.kt` (Q5). Tell him before slices 1, 3 and 7.
  - Device checks D1-D14 use stand-in users A/B/C/N and console-edited RESOLVED entries for day 4 / day 6.

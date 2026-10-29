# F-003: ListScreen
Branch: dev/Francisco · Base: ca22f44
Remote: origin/dev/Francisco

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-10-29 | |
| Plan | done | 2026-10-29 | 00-plan, 01-coding-plan, 03-docs-plan; 2 slices; Q1-Q6 with defaults |
| Gate 1: plan approved | done | 2026-10-29 | approved with defaults Q1-Q6 after brief check |
| Code | | | |
| Review | | | |
| Docs | | | |
| Pushed | | | |
| Merged | | | |

## Log
Append-only.
- 2026-10-29: fetched origin; origin/main had no commits missing from dev/Francisco. The person chose to run /merge anyway: "Already up to date". Started F-003 from `docs/tasks/phase-1/F-003-list-screen.md`; dependencies D-001 and F-002 are both in the branch.
- 2026-10-29: Plan written (00-plan, 01-coding-plan, 03-docs-plan) after reading the task file, phase-1 index, overview (Shared components, ViewModel D-003, Navigation D-004), F-002 plans/review/state, `TicketCard.kt`, `TicketsViewModel.kt`, `TicketsList.kt`, `SampleTickets.kt`, `Destinations.kt` and both string files. Approach: stateless `ListScreen(tickets, isExpanded, onTicketClick: (String) -> Unit, modifier)`; "active" filtering, newest-first order and the anonymous projection stay upstream (`TicketsList.publicActive()` + `toAnonymousView()` in `TicketsViewModel.tickets`), the screen renders the list as received. Slices: (1) LazyColumn keyed by id with TicketCard + data previews, remove `ui/screens/.gitkeep`; (2) empty state with `ticket_list_empty` (EN + PT) + empty previews. Open questions with defaults: Q1 no stateful wrapper despite the `XxxScreen(viewModel)`/`XxxContent` rule (agreed signature); Q2 `isExpanded` passed through, so expanded cards show the description to anonymous users (F-002 S1 left to screens; flag to Diogo for D-005/D-006); Q3 same LazyColumn in landscape; Q4 private preview list; Q5 empty-state wording; Q6 click passes the id. The brief PDF could not be opened (`pdftoppm` not installed); brief I.1.B/I.1.C wording taken from the F-002 review. Gate 1 pending.
- 2026-10-29: Brief checked against the PDF the person supplied (identical to `docs/brief/AMov.ProjetoKotlin_PT.pdf`, same md5). I.1.B confirmed: list of active public tickets with category, situation and thumbnail. Raised before approval: Q2 (expanded cards show the description, beyond I.1.B/I.1.C for anonymous users) and Q1 (brief V grades the stateless/stateful split). Gate 1 approved by the person with the defaults for Q1-Q6: agreed signature with no stateful wrapper, `isExpanded` passed through with a KDoc note.

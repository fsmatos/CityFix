## Docs to update
- `docs/architecture/overview.md`: add a short "Screens" subsection after "Shared components" (or a first bullet there if F-004..F-006 add theirs later): `ui/screens/ListScreen.kt` (F-003), `ListScreen(tickets, isExpanded, onTicketClick, modifier)`, stateless; renders the list as received (filtering by PUBLIC + `isActive`, newest-first order and `toAnonymousView()` happen in `TicketsList`/`TicketsViewModel`); `LazyColumn` keyed by `Ticket.id`; centred empty-state message; `onTicketClick` gets the id for `TicketDetailRoute`; same layout in portrait and landscape (Q3). Include the D-006 call from `01-coding-plan.md` as the contract, and the Q1/Q2 outcome (no stateful wrapper; `isExpanded` passed through, expanded cards show the description).
- `docs/tasks/phase-1/F-003-list-screen.md`: tick the acceptance checks only after the manual Android Studio checks pass (same rule as F-001/F-002).
- `docs/work/F-003-list-screen/state.md`: phase rows and log.

## Decisions to record
- No ADR expected. Write one only if the person picks a non-default with lasting impact: Q1 alternative (stateful `ListScreen(viewModel)` + `ListContent`, which changes the convention for all screens) or Q2 alternative (anonymous list never expands).

## Report notes
Add a "F-003: ListScreen" section to `docs/report/notes.md`:
- Technical solutions: stateless screen over `LazyColumn` with stable keys (`key = { it.id }`, so Compose keeps item identity when the list changes); reuse of `TicketCard`; empty state as a separate branch; `modifier` parameter so the `Scaffold` padding is applied by the caller.
- Options chosen: filtering and anonymous projection kept upstream (single source of the "active" rule in `model/`, privacy enforced before the UI); click passes only the id, matching the type-safe `TicketDetailRoute`; one layout for both orientations; the Q1/Q2 decisions as taken at Gate 1.
- Rejected: filtering in the screen, passing the ViewModel to the screen, `(Ticket) -> Unit` click callback.
- Improvement opportunities: two-column adaptive grid in landscape/tablets; pull-to-refresh and filters by category/status once Firestore is in; empty state with an illustration and a "report an issue" action; hiding the expand toggle (description) for anonymous users once roles exist.

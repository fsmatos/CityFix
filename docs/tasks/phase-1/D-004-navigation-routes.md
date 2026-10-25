# D-004: Navigation routes

Owner: Diogo (`dev/Diogo`) · Notion plan ID: D4 · Layer: `ui.navigation` · Phase 1 · Status: to do
Depends on: none

## Goal
Type-safe routes for the phase 1 flow, using the kotlin-serialization plugin from the version catalog.

## What to build
`DashboardRoute`, `TicketListRoute`, `TicketDetailRoute(ticketId)`, `ReportTicketRoute`.

## Files
`ui/navigation/Destinations.kt`

## Acceptance checks
- [ ] Routes are `@Serializable` objects/classes.
- [ ] `TicketDetailRoute` carries the ticket id and nothing else.

## References
- Brief: `docs/brief/AMov.ProjetoKotlin_PT.pdf`; domain model: `docs/architecture/overview.md` (Option A, decision 0002).
- Plan and order: [phase 1 index](README.md).

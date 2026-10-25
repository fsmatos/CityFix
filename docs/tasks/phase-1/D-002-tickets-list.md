# D-002: TicketsList and dashboard stats

Owner: Diogo (`dev/Diogo`) · Notion plan ID: D2 · Layer: `model` · Phase 1 · Status: to do
Depends on: D-001

## Goal
In-memory collection of tickets (like `ContactsList` in P07) with the business rules that do not need Firebase.

## What to build
`TicketsList`: `getAllSorted`, `findById`, `add`, `rawList`, `publicActive()` (public, not closed), `findNearby(location, category, situation)` using the Haversine formula with the 10 m rule, and `stats()` returning `DashboardStats` (average repair time, total tickets, % active).

## Files
`TicketsList.kt`, `DashboardStats.kt`

## Acceptance checks
- [ ] Unit tests: `findNearby` returns a ticket at 9 m and not one at 11 m (same category + situation only).
- [ ] Unit tests: `publicActive()` excludes private and closed tickets.
- [ ] Unit tests: `stats()` with zero tickets does not divide by zero.
- [ ] `DashboardStats` fields are agreed with Francisco (needed by F-004).

## References
- Brief: `docs/brief/AMov.ProjetoKotlin_PT.pdf`; domain model: `docs/architecture/overview.md` (Option A, decision 0002).
- Plan and order: [phase 1 index](README.md).

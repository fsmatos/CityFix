# D-003: TicketsViewModel

Owner: Diogo (`dev/Diogo`) · Notion plan ID: D3 · Layer: `viewmodel` · Phase 1 · Status: to do
Depends on: D-001, D-002

## Goal
ViewModel that exposes the tickets to the UI with sample data (no Firebase in phase 1).

## What to build
`TicketsViewModel` with 3 sample tickets, a `StateFlow` of tickets (and stats), and `getTicketById`. State survives rotation.

## Files
`TicketsViewModel.kt`

## Acceptance checks
- [ ] Exposes immutable `StateFlow`; composables never touch `TicketsList` directly.
- [ ] `getTicketById` returns null for an unknown id (UI shows "Ticket not found").
- ~~Unit test with the 3 sample tickets.~~ Removed by Diogo on 2026-10-29: no unit tests.

## References
- Brief: `docs/brief/AMov.ProjetoKotlin_PT.pdf`; domain model: `docs/architecture/overview.md` (Option A, decision 0002).
- Plan and order: [phase 1 index](README.md).

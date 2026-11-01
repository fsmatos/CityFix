# F-002: StatusBadge and TicketCard

Owner: Francisco (`dev/Francisco`) · Notion plan ID: F2 · Layer: `ui.components` · Phase 1 · Status: to do
Depends on: D-001, F-001

## Goal
Reusable stateless components for the list and detail screens.

## What to build
`StatusBadge(status)` and `TicketCard(ticket, isExpanded, onClick)` with a placeholder thumbnail.

## Files
`StatusBadge.kt`, `TicketCard.kt`

## Acceptance checks
- [x] Stateless: no ViewModel inside.
- [x] One badge colour/label per `TicketStatus`, with text (not colour only).
- [x] `@Preview` for each status, collapsed and expanded, portrait and landscape.

## References
- Brief: `docs/brief/AMov.ProjetoKotlin_PT.pdf`; domain model: `docs/architecture/overview.md` (Option A, decision 0002).
- Plan and order: [phase 1 index](README.md).

# F-003: ListScreen

Owner: Francisco (`dev/Francisco`) · Notion plan ID: F3 · Layer: `ui.screens` · Phase 1 · Status: to do
Depends on: D-001, F-002

## Goal
Public active tickets list (category, status, thumbnail).

## What to build
`ListScreen(tickets, isExpanded, onTicketClick)` with an empty state and `key = { it.id }` on the lazy list.

## Files
`ListScreen.kt`

## Acceptance checks
- [ ] Empty list shows an empty-state message.
- [ ] Lazy list uses stable keys.
- [ ] `@Preview` with data and empty, portrait and landscape.

## References
- Brief: `docs/brief/AMov.ProjetoKotlin_PT.pdf`; domain model: `docs/architecture/overview.md` (Option A, decision 0002).
- Plan and order: [phase 1 index](README.md).

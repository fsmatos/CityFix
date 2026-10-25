# F-005: ShowScreen (ticket detail)

Owner: Francisco (`dev/Francisco`) · Notion plan ID: F5 · Layer: `ui.screens` · Phase 1 · Status: to do
Depends on: D-001, F-002

## Goal
Ticket detail with state history.

## What to build
`ShowScreen(ticket?, ...)`: "Ticket not found" when null, otherwise detail and the status history.

## Files
`ShowScreen.kt`

## Acceptance checks
- [ ] Null ticket shows "Ticket not found".
- [ ] Status history shown in order with timestamps.
- [ ] Public view never shows author data or exact coordinates (anonymous rule from the brief).
- [ ] `@Preview` in portrait and landscape.

## References
- Brief: `docs/brief/AMov.ProjetoKotlin_PT.pdf`; domain model: `docs/architecture/overview.md` (Option A, decision 0002).
- Plan and order: [phase 1 index](README.md).

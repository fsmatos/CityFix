# F-004: DashboardScreen

Owner: Francisco (`dev/Francisco`) · Notion plan ID: F4 · Layer: `ui.screens` · Phase 1 · Status: to do
Depends on: D-001, D-002

## Goal
Dashboard for the anonymous citizen.

## What to build
`DashboardScreen(stats, ...)` showing average repair time, total number of tickets and % active.

## Files
`DashboardScreen.kt`

## Acceptance checks
- [ ] Shows the three values from `DashboardStats`.
- [ ] Handles zero tickets (no NaN).
- [ ] `@Preview` in portrait and landscape.

## References
- Brief: `docs/brief/AMov.ProjetoKotlin_PT.pdf`; domain model: `docs/architecture/overview.md` (Option A, decision 0002).
- Plan and order: [phase 1 index](README.md).

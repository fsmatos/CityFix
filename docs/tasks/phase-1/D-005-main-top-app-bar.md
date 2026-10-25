# D-005: MainTopAppBar

Owner: Diogo (`dev/Diogo`) · Notion plan ID: D5 · Layer: `ui.navigation` · Phase 1 · Status: to do
Depends on: D-004

## Goal
Top bar that changes with the current destination.

## What to build
`MainTopAppBar`: back arrow, expand/collapse toggle, and add button, shown depending on the route.

## Files
`ui/navigation/MainTopAppBar.kt`

## Acceptance checks
- [ ] Back arrow only when there is a back stack.
- [ ] Every icon has a `contentDescription` from `strings.xml`.
- [ ] `@Preview` in portrait and landscape.

## References
- Brief: `docs/brief/AMov.ProjetoKotlin_PT.pdf`; domain model: `docs/architecture/overview.md` (Option A, decision 0002).
- Plan and order: [phase 1 index](README.md).

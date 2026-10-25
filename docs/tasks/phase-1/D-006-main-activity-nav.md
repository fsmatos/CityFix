# D-006: MainActivity with Scaffold and NavHost

Owner: Diogo (`dev/Diogo`) · Notion plan ID: D6 · Layer: `ui.screens` · Phase 1 · Status: to do
Depends on: D-003, D-004, D-005, F-003, F-004, F-005, F-006

## Goal
Wire everything: scaffold, nav host and state that survives rotation.

## What to build
`MainActivity` with `Scaffold`, `NavHost`, `MainTopAppBar`, and `rememberSaveable` for the expanded state. Flow: dashboard -> list -> detail -> report.

## Files
`MainActivity.kt`

## Acceptance checks
- [ ] The app navigates dashboard -> list -> detail -> report.
- [ ] Rotating the device keeps the screen and the expanded state.
- [ ] Back works from every screen.

## References
- Brief: `docs/brief/AMov.ProjetoKotlin_PT.pdf`; domain model: `docs/architecture/overview.md` (Option A, decision 0002).
- Plan and order: [phase 1 index](README.md).

# F-001: Theme, strings and accessibility base

Owner: Francisco (`dev/Francisco`) · Notion plan ID: F1 · Layer: `ui.theme / res` · Phase 1 · Status: to do
Depends on: none

## Goal
Material 3 theme and the localisation base used by every screen.

## What to build
Material 3 theme; `strings.xml` in English (default) and `values-pt/strings.xml` in Portuguese; `contentDescription` on every icon.

## Files
`Theme.kt`, `strings.xml` (EN/PT)

## Acceptance checks
- [x] No hard-coded user-visible strings in new code.
- [x] Switching the device language to Portuguese shows Portuguese strings.
- [x] Touch targets at least 48dp.

## References
- Brief: `docs/brief/AMov.ProjetoKotlin_PT.pdf`; domain model: `docs/architecture/overview.md` (Option A, decision 0002).
- Plan and order: [phase 1 index](README.md).

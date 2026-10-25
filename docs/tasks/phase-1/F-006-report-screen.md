# F-006: ReportScreen (provisional)

Owner: Francisco (`dev/Francisco`) · Notion plan ID: F6 · Layer: `ui.screens` · Phase 1 · Status: to do
Depends on: D-001, F-001

## Goal
Provisional form to open a ticket; no attachments or Firebase yet.

## What to build
`ReportScreen` with category, situation, description and scope (public/private), landscape layout, and `@Preview` for all screens.

## Files
`ReportScreen.kt`, previews

## Acceptance checks
- [ ] All four fields present; situation and scope use the enums.
- [ ] Works in landscape without clipped content.
- [ ] Every field has a label and the form is usable with TalkBack.

## References
- Brief: `docs/brief/AMov.ProjetoKotlin_PT.pdf`; domain model: `docs/architecture/overview.md` (Option A, decision 0002).
- Plan and order: [phase 1 index](README.md).

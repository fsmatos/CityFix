# D-021: Show the coordinates in the pending citizen detail
Branch: dev/Diogo · Base: 3f40bcf
Remote: not pushed

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-21 | Raised by Diogo after D-019 |
| Plan | done | 2026-11-21 | 1 slice; formatter to utils/CoordinatesFormat.kt; awaiting Gate 1 |
| Gate 1: plan approved | done | 2026-11-21 | Diogo approved; also align Register, Report and Profile (slice 2) |
| Code | done | 2026-11-21 | 2 slices: e94551d (citizen detail coordinates), slice 2 fix (ccdb518) (own location dot decimal); build + lint pass |
| Review | done | 2026-11-21 | review.md: 0 blocking, 0 should-fix, 3 nits |
| Docs | done | 2026-11-21 | overview (D-012 detail, map privacy note, Shared components "Coordinates", D-013 handover); manual checklist I.3.D; report notes D-021; no decision record; not committed |
| Pushed | | | |
| Merged | | | |

## Log
- 2026-11-21: merged origin/main into dev/Diogo first (044a959, Diogo chose to merge; conflicts resolved and approved).
- 2026-11-21: Plan written (00-plan, 01-coding-plan, 03-docs-plan). One slice: move D-019's private `formatCoordinate` from `ShowScreen.kt` to public `utils/CoordinatesFormat.kt` (body unchanged), import it in `ShowScreen.kt` (only change to Francisco's file), add the coordinates `Text` under the map in `CitizenApprovalScreen.CitizenLocation` reusing `ticket_coordinates` / `cd_ticket_coordinates`. No `formatCoordinates(Coordinates)` overload (strings take lat and lng separately); no shared composable (would rewrite Francisco's block). No new strings. Awaiting Gate 1.
- 2026-11-21: Gate 1 approved by Diogo. Open question answered: also align Register, Report and Profile (own location) with the shared Locale.ROOT formatter, as slice 2 (added to the coding plan before coding).
- 2026-11-21: Code slice 1 (e94551d): `formatCoordinate` moved to `utils/CoordinatesFormat.kt` (body and KDoc unchanged), imported in `ShowScreen.kt` (only change there), coordinates line under the map in `CitizenApprovalScreen.CitizenLocation`. `assembleDebug` and `lint` pass (0 errors, 17 warnings, none in changed files).
- 2026-11-21: Code slice 2 (added at Gate 1; section appended to 01-coding-plan.md before coding). Grep found a single float-placeholder string, `report_location_value` (EN/PT), used by Register, Report and Profile. Placeholders `%1$.5f`/`%2$.5f` became `%1$s`/`%2$s`; the three callers pass `formatCoordinate(...)`. Text otherwise unchanged. `assembleDebug` and `lint` pass (0 errors, 17 warnings, none in changed files).
- 2026-11-21: Deviations: none beyond the Gate 1 addition (slice 2 lifts the plan's "Do not touch" entries for `report_location_value` and the Register/Report/Profile coordinate texts). Device checks pending: operator pending citizen detail (portrait/landscape, EN/PT, TalkBack, (0, 0) case), operator ticket detail unchanged, Register/Report/Profile in PT show dots.
- 2026-11-21: Review of b8beaff..HEAD (e94551d, ccdb518) written to review.md: 0 blocking, 0 should-fix, 3 nits (N1 add ccdb518 to the Code row; N2 duplicated coordinates Text block, shared composable left as an improvement; N3 `report_location_value` and `cd_ticket_coordinates` now have the same text, which is acceptable). Verified all three `report_location_value` callers pass `formatCoordinate` strings and that no `%.Nf` placeholders are left in res/. The operator-only route guard is unchanged. Device checks still pending.
- 2026-11-21: Docs per 03-docs-plan. `docs/architecture/overview.md`: D-012 "Detail (`PendingCitizenRoute`)" bullet now names the coordinates line (D-021, same strings and formatter as D-019, (0, 0) unchanged, operator only); map privacy bullet's D-019 parenthesis extended to the pending citizen detail and the shared formatter; new "Coordinates" bullet under "Shared components" (next to the dates helpers) for `utils/CoordinatesFormat.kt` incl. slice 2 (`report_location_value` now `%1$s`/`%2$s`); D-013 handover note for Francisco (`ShowScreen.kt` import only, `ReportScreen.kt` one call in slice 2). `docs/testing/manual-test-checklist.md` I.3.D: coordinates line added to the expectation (not ticked; device checks pending). `docs/report/notes.md`: D-021 section appended after D-020 (solutions, privacy, options, N3 as defence note, verification, improvements incl. N2's shared composable and geo: intent). No decision record (plan: covered by 0007 as amended by D-019). Deviation: slice 2 was in the plan's improvement list, so it is documented as done rather than as an improvement. Review N1 already resolved (Code row has `ccdb518`). Not committed.
- 2026-11-21: manual device checks passed (Diogo): pending citizen coordinates (EN/PT, rotation, TalkBack, (0,0)), own location dot decimal in Register/Report/Profile, D-019 line unchanged; quick check of F-018 map zoom and sign-up SMS after the merge.

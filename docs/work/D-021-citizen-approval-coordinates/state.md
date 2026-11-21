# D-021: Show the coordinates in the pending citizen detail
Branch: dev/Diogo · Base: 3f40bcf
Remote: not pushed

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-21 | Raised by Diogo after D-019 |
| Plan | done | 2026-11-21 | 1 slice; formatter to utils/CoordinatesFormat.kt; awaiting Gate 1 |
| Gate 1: plan approved | done | 2026-11-21 | Diogo approved; also align Register, Report and Profile (slice 2) |
| Code | done | 2026-11-21 | 2 slices: e94551d (citizen detail coordinates), slice 2 fix (own location dot decimal); build + lint pass |
| Review | | | |
| Docs | | | |
| Pushed | | | |
| Merged | | | |

## Log
- 2026-11-21: merged origin/main into dev/Diogo first (044a959, Diogo chose to merge; conflicts resolved and approved).
- 2026-11-21: Plan written (00-plan, 01-coding-plan, 03-docs-plan). One slice: move D-019's private `formatCoordinate` from `ShowScreen.kt` to public `utils/CoordinatesFormat.kt` (body unchanged), import it in `ShowScreen.kt` (only change to Francisco's file), add the coordinates `Text` under the map in `CitizenApprovalScreen.CitizenLocation` reusing `ticket_coordinates` / `cd_ticket_coordinates`. No `formatCoordinates(Coordinates)` overload (strings take lat and lng separately); no shared composable (would rewrite Francisco's block). No new strings. Awaiting Gate 1.
- 2026-11-21: Gate 1 approved by Diogo. Open question answered: also align Register, Report and Profile (own location) with the shared Locale.ROOT formatter, as slice 2 (added to the coding plan before coding).
- 2026-11-21: Code slice 1 (e94551d): `formatCoordinate` moved to `utils/CoordinatesFormat.kt` (body and KDoc unchanged), imported in `ShowScreen.kt` (only change there), coordinates line under the map in `CitizenApprovalScreen.CitizenLocation`. `assembleDebug` and `lint` pass (0 errors, 17 warnings, none in changed files).
- 2026-11-21: Code slice 2 (added at Gate 1; section appended to 01-coding-plan.md before coding). Grep found a single float-placeholder string, `report_location_value` (EN/PT), used by Register, Report and Profile. Placeholders `%1$.5f`/`%2$.5f` became `%1$s`/`%2$s`; the three callers pass `formatCoordinate(...)`. Text otherwise unchanged. `assembleDebug` and `lint` pass (0 errors, 17 warnings, none in changed files).
- 2026-11-21: Deviations: none beyond the Gate 1 addition (slice 2 lifts the plan's "Do not touch" entries for `report_location_value` and the Register/Report/Profile coordinate texts). Device checks pending: operator pending citizen detail (portrait/landscape, EN/PT, TalkBack, (0, 0) case), operator ticket detail unchanged, Register/Report/Profile in PT show dots.

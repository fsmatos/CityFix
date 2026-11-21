# D-021 review

Scope: `git diff b8beaff..HEAD` (e94551d slice 1, ccdb518 slice 2). Checked against 00-plan,
01-coding-plan (incl. slice 2 added at Gate 1), the task file, CLAUDE.md and kotlin-conventions.
Reviewed from the diff and source only; build/lint taken from state.md, device checks still pending.

## Blocking
None.

## Should-fix
None.

## Nit
- N1. `state.md` Code row says "slice 2 fix" without its hash. Fix: write `ccdb518` there,
  matching `e94551d` for slice 1.
- N2. The coordinates `Text` block (formatter calls, description, `Text` with `semantics`) is now
  duplicated in `ShowScreen.ExactLocationSection` and `CitizenApprovalScreen.CitizenLocation`.
  The plan accepted this so Francisco's file stays mostly untouched. Fix (after the deadline): add
  `ui/components/CoordinatesText(location: Coordinates)` and call it from both. For now, list it
  under the report's improvement opportunities, as 03-docs-plan already says.
- N3. `report_location_value` and `cd_ticket_coordinates` now have the same text
  ("Latitude %1$s, longitude %2$s") in EN and PT. That is fine, because they are used in different
  contexts (visible text vs TalkBack). Fix: none needed. Be ready to explain it at the defence if
  someone asks why there are two strings.

## Checks that passed
- Plan conformance: `formatCoordinate` moved unchanged (body `String.format(Locale.ROOT, "%.5f", value)`
  and KDoc) to `utils/CoordinatesFormat.kt` as a public top-level function. `ShowScreen.kt` only
  removes the private copy and adds the import. Its `java.util.Locale` import is still used by the
  `locale:` parameters. `CitizenLocation` adds the line in the `else` branch after `TicketMap`, as
  in the coding plan, and its KDoc is updated.
- Callers of `report_location_value`: all three (`RegisterScreen` ~l.399, `ReportScreen` ~l.620,
  `ProfileScreen` ~l.208) now pass `formatCoordinate(...)` strings. Nothing outside these files
  uses the string. No other `%.Nf` placeholders are left in `res/` (values, values-pt,
  values-night), so no locale-dependent float formatting remains in string resources.
- i18n: EN and PT were both changed. The rest of each string is unchanged and the positional
  `%1$s`/`%2$s` placeholders match. `ticket_coordinates`/`cd_ticket_coordinates` are reused, with
  no new strings. `Locale.ROOT` keeps the dot decimal in PT, as the acceptance check requires.
- Anonymous users / personal data: `PendingCitizenRoute` is wrapped in
  `RequireAccess(allowed = setOf(AccessRole.OPERATOR))`, so only operators see the citizen's
  coordinates as text. The slice 2 screens (Register, Report, Profile) only show the user's own
  location, and those texts already existed. No new exposure.
- (0, 0): the check `location == Coordinates()` is unchanged, so there is no map and no line.
- Accessibility: the TalkBack description is resolved outside the `semantics` lambda (as in
  D-019) and reads "Latitude ..., longitude ...". `bodyMedium` uses the theme's default color,
  so contrast is fine.
- Orientation: in both layouts `CitizenLocation` is inside a `verticalScroll` column (portrait:
  the page; landscape: the right pane with `locationScroll`), so the extra line can always scroll
  into view and is never clipped. No new state, so rotation behaviour is unchanged.
- MVVM / stateless: the change sits only in stateless private composables and a pure helper in
  `utils/`. No ViewModel, repository, Firestore or rules change.
- Commits: Conventional Commits with the task ID and no attribution.

## Pending (not review findings)
Device checks listed in state.md: operator pending citizen detail in portrait and landscape, in EN
and PT, with TalkBack, and the (0, 0) case; operator ticket detail unchanged; Register, Report and
Profile in PT show dots.

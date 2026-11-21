## Docs to update
- `docs/architecture/overview.md`, D-012 section, "Detail (`PendingCitizenRoute(citizenId)`, Q3)"
  bullet (~line 1013): the georeferencing map is followed by the coordinates text (D-021), same
  format and strings as the operator ticket detail; (0, 0) unchanged.
- `docs/architecture/overview.md`, map privacy bullet (~line 906, "no screen prints someone else's
  coordinates"): extend the D-019 parenthesis to also name the pending citizen detail (operator only,
  D-021).
- `docs/architecture/overview.md`, the file list / utils mention if there is one: add
  `utils/CoordinatesFormat.kt` (`formatCoordinate`, moved from `ShowScreen.kt`).
- Manual test checklist item I.3.D (wherever Diogo keeps it): note the coordinates line.

## Decisions to record
None. Operator-only visibility of coordinates already follows decision 0007 as amended by D-019.

## Report notes
- Brief I.3.D "georreferenciação": shown as an exact map plus text, consistent with the ticket
  detail (I.3.E).
- One formatter in `utils/` (`Locale.ROOT`, 5 decimals, about 1 m) for every operator coordinate
  text, so the PT decimal comma never clashes with the "lat, lng" separator.
- Improvement opportunities: a shared coordinates composable; align the citizen's own location texts
  (Register, Report, Profile) with the same formatter; copy / open in a maps app.

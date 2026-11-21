# D-021: Show the coordinates in the pending citizen detail
Type: feature (improvement) · Task file: docs/tasks/phase-3/D-021-citizen-approval-coordinates.md
Labels: none

## Problem
The operator's pending citizen detail (`CitizenApprovalScreen`, private `CitizenLocation`) shows the
registered location only on the exact map (`TicketMap(exact = true)`). Since D-019 the operator's
ticket detail (`ShowScreen`, `ExactLocationSection`) also prints the coordinates as text, so the two
operator screens are inconsistent. Brief I.3.D: the operator sees the pending citizen's name, NIF and
"georreferenciação".

## Outcome
Under the citizen's map the operator reads "Coordinates: 40.21000, -8.41900" ("Coordenadas: ..." in
PT), formatted by the same function as the ticket detail, with the same TalkBack description.

## Acceptance checks
- [ ] Pending citizen with a location: under the map, "Coordinates: <lat>, <lng>", 5 decimals, dot
      decimal separator, in EN and in PT (device language PT shows "Coordenadas: ..." still with dots).
- [ ] TalkBack on that line reads "Latitude <lat>, longitude <lng>".
- [ ] Pending citizen at (0, 0): "Location not available" as now, no map, no coordinates line.
- [ ] Operator ticket detail (D-019) still shows its coordinates line exactly as before (same text,
      same format); citizen and anonymous ticket detail still show no coordinates.
- [ ] Portrait (one column) and landscape (map in the right pane): the line is visible under the map,
      not clipped; rotation keeps the screen state.
- [ ] `./gradlew assembleDebug` and `./gradlew lint` pass with no new warnings.

## Approach
1. Move D-019's private `formatCoordinate(value: Double)` from `ui/screens/ShowScreen.kt` to a new
   `utils/CoordinatesFormat.kt` as a public top-level function, body unchanged
   (`String.format(Locale.ROOT, "%.5f", value)`), KDoc kept. `ShowScreen.kt` imports it; nothing else
   in that file changes (its `java.util.Locale` import stays: still used by the `locale` parameters).
2. In `CitizenApprovalScreen.kt` `CitizenLocation`, in the `else` branch after `TicketMap`, add the
   same `Text` as D-019: `lat`/`lng` from `formatCoordinate`, `stringResource(R.string.ticket_coordinates, lat, lng)`,
   `bodyMedium`, `Modifier.semantics { contentDescription = stringResource(R.string.cd_ticket_coordinates, lat, lng) }`
   (description resolved outside the semantics lambda, as in D-019).
3. Reuse D-019's strings; no new resources.

Alternatives considered:
- `formatCoordinates(c: Coordinates)` returning "lat, lng": not used, because the strings take lat
  and lng as two separate arguments (`%1$s, %2$s`) and the TalkBack text needs them separately too.
- A shared `CoordinatesText(location)` composable in `ui/components/`: would remove the ~8 duplicated
  lines but means rewriting the block in Francisco's `ShowScreen.kt`; not worth it the day before the
  deadline. Noted as an improvement.
- Leaving a copy of the formatter in each screen: rejected by the task (one shared formatter).

## Architecture impact
- New file `utils/CoordinatesFormat.kt` (stateless helper, fits `utils/`). No model, data, ViewModel,
  navigation or Firestore change.
- `ShowScreen.kt` (Francisco's file): only the private formatter is removed and an import added.

## Failure modes
| Situation | Behaviour |
| --- | --- |
| Location (0, 0) | "Location not available", no map, no line (existing check `location == Coordinates()`) |
| Device language PT (decimal comma) | Dots anyway (`Locale.ROOT`), so "lat, lng" stays unambiguous |
| Negative longitude | "-8.41900" (sign kept by `%.5f`) |

## Risks and open questions
- R1: `ShowScreen.kt` is Francisco's file and may be edited on `dev/Francisco`; the change is two
  lines (remove function, add import), so a merge conflict is unlikely and trivial.
- R2: Strings named `ticket_*` are reused for a citizen; the text ("Coordinates: ...") is neutral, so
  the name is the only oddity. The task asks for reuse, so no rename.
- Q1: none blocking. (Other screens, Register/Report/Profile, format their own fix through
  locale-dependent `%.5f` string resources; aligning them is out of scope, confirm if you want it.)

## Out of scope
- Register, Report and Profile coordinate texts (own location, different strings).
- Copy-to-clipboard / open in maps app; a shared coordinates composable.
- Any change for citizens or anonymous users.

## Slices
1. Move the formatter to `utils/` and show the coordinates line in `CitizenLocation`
   (one commit, about 15 lines).

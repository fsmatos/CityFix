# D-021: Show the coordinates in the pending citizen detail

Type: improvement · Raised by Diogo during the manual test checklist (2026-11-21), after D-019.

## Problem
The operator's pending citizen detail (`CitizenApprovalScreen`, `CitizenLocation`) shows the
registered location only on the exact map. Since D-019 the operator's ticket detail also prints
the coordinates as text, so the two operator screens are inconsistent.

## Brief
I.3.D: the operator sees the pending citizen's name, NIF and "georreferenciação".

## Acceptance
- Under the map, the same line as D-019: "Coordinates: 40.21000, -8.41900" (dot decimal,
  5 decimals, both languages), with the same TalkBack description.
- Reuse D-019's strings (`ticket_coordinates`, `cd_ticket_coordinates`) and one shared formatter
  (move `formatCoordinate` out of `ShowScreen.kt`, e.g. to `utils/`), so both screens format alike.
- Location (0, 0): "Location not available" as now, no coordinates line.
- Operator-only screen; no change for citizens or anonymous users. Portrait and landscape.

# D-019: Show exact coordinates and attachment authors in the operator detail

Type: bug · Found by Diogo during the manual test checklist (2026-11-21), item I.3.E (operator detail).

## Problem
In the operator's ticket detail (`OperatorTicketScreen` -> `ShowScreen` with `DetailAudience.OPERATOR`):
1. The exact location is shown only on the map; the latitude and longitude are never written as text.
2. The photos and videos do not say who attached them. Each attachment stores `uploadedById`
   in Firestore, and the operator screen already has the names (`responsibleNames`, built from
   `state.people`), but the gallery does not use them. Only the history ("By <name>") and the
   messages show names.

## Brief
I.3.E: the operator sees the ticket with its exact location, the author's contacts and the full
history with photos/videos and who added them.

## Acceptance
- The operator detail shows the ticket's latitude and longitude as text (for example
  `40.21000, -8.41900`), next to or under the exact map.
- Each attachment in the operator detail shows the name of the person who attached it; the
  resolution proof shows the operator's name. Unknown or unreadable profile: no name, no crash.
- Anonymous users still never see coordinates as text or any author name (CLAUDE.md rule);
  the registered citizen's detail is unchanged unless the plan decides otherwise.
- New strings in `values/strings.xml` and `values-pt/strings.xml`; works in portrait and landscape;
  TalkBack reads the coordinates and the attachment author.

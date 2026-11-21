## Docs to update
- `docs/architecture/overview.md`, operator detail section (D-013, around "History: `responsibleNames`" and the `ShowScreen.kt` additive line): the operator detail now also shows the coordinates text under the exact map and "Added by <name>" on each photo/video tile; `responsibleNames` also feeds the tiles; watched ids include attachment uploaders.
- `docs/decisions/0007-ticket-detail-by-audience.md`: short amendment note (D-019): OPERATOR additionally sees latitude/longitude as text and the uploader's name per attachment; PUBLIC and REGISTERED unchanged.
- Manual test checklist item I.3.E: mark the two gaps fixed (wherever Diogo keeps the checklist).

## Decisions to record
Amendment to 0007 only (who sees coordinate text and uploader names). No new decision file.

## Report notes
- Brief I.3.E coverage: exact location as map plus text, and who added each photo/video, from `Attachment.uploadedById` resolved with the same live profile lookup as the history names (D-013), no new data stored.
- Privacy by audience: coordinates text and names only in the OPERATOR view; the anonymous projection already blanks `uploadedById` and rounds the location upstream.
- Coordinates formatted with `Locale.ROOT` and 5 decimals (about 1 m) so the PT decimal comma does not clash with the lat/lng separator; TalkBack gets "Latitude x, longitude y".
- Improvement opportunities: copy coordinates / open in an external maps app; uploader name and date in the full-screen viewer.

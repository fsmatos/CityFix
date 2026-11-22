# D-022 review
By the main session (not the independent reviewer agent), diff `6b0c6e5..6db0648`. 0 blocking.

## Checked
- `ACTIVE.screens` has one reader (`currentTab`); the operator has no Active tab, so is unaffected.
- The impact rule stays on the vertical axis (brief I.2.F); nothing of the speed gate is left in the code.
- `TrackingService` still starts the location stream for both roles; the geofence is untouched.
- Only `ApiException` is caught, so coroutine cancellation still propagates; all three callers handle null.

## Accepted risks
- No speed gate: handling or dropping the phone can suggest a report.
- The citizen's location request has no reader now; kept, as the service is of type location (battery cost).
- Not verified on a real phone, in a car, with the screen off, or on a phone without a gravity sensor.

## Report waiting for Francisco's approval
New: `docs/report/AMOV2627.2018013532.2018044613_Relatorio_NOVO.pdf` (and `.docx`).
The files without `_NOVO` are the previous version, untouched. Three passages change:

| Where (page number printed in the new PDF) | Before | Now |
| --- | --- | --- |
| Section 4, "Deteção de impactos" (p. 15) | solavanco vertical com o telemóvel em movimento | solavanco vertical |
| Table 5.1, row I.2.F (p. 22) | acima de 6 m/s², e só em movimento (> 2 m/s) | fora de 6 a 14 m/s² com a gravidade incluída, sem velocidade mínima |
| Section 6.7 (p. 27) | o mesmo; um telemóvel parado que cai não conta | o mesmo intervalo; um telemóvel que cai pode gerar uma sugestão |

The table reflows, so the PDF goes from 39 to 37 pages and the contents page numbers follow.

Once approved, before merging into main and building the ZIP: keep only the approved version.
`git archive main` copies everything in `docs/report` into the ZIP, so both versions would ship.

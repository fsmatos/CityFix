# F-005: ShowScreen (ticket detail)
Branch: dev/Francisco · Base: ad7ca62
Remote: not pushed

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-01 | |
| Plan | done | 2026-11-01 | revision 2 (strict brief): audience parameter, PUBLIC = header + media + coarse map; 6 slices; Q1-Q12 with defaults; Q2 header and Q7 map library/key for the person; Gate 1 pending |
| Gate 1: plan approved | done | 2026-11-01 | approved revision 2: Q7 static placeholder (slice 6 dropped), Q2 category + situation for PUBLIC, DetailAudience in ui/screens |
| Code | | | |
| Review | | | |
| Docs | | | |
| Pushed | | | |
| Merged | | | |

## Log
Append-only.
- 2026-11-01: fetched origin; origin/main had no commits missing from dev/Francisco. The person chose not to merge (already up to date). Started F-005 from `docs/tasks/phase-1/F-005-show-screen.md`; dependencies D-001 and F-002 are in the branch.
- 2026-11-01: Plan written (00-plan, 01-coding-plan, 03-docs-plan).
  - Read: the task file, the phase-1 index, the overview (Code mapping, ViewModel D-003, Navigation D-004, Top bar D-005, UI foundation, Shared components, Screens), decision 0006, `Ticket.kt`, `StatusHistory.kt`, `Coordinates.kt`, `TicketPrivacy.kt`, `TicketsViewModel.kt`, `SampleTickets.kt`, `StatusBadge.kt`, `TicketCard.kt`, `DateFormatting.kt`, `ListScreen.kt`, `DashboardScreen.kt`, both string files, the D-006 task file, F-004's plans, review and state, the F-003 review, and the F-002/D-003 reviews (I.1.C quotes, `formatDate` locale note). The brief PDF could not be read (`pdftoppm` missing); I.1.C wording taken from the F-002 review and decision 0006.
  - Approach: stateless `ShowScreen(ticket: Ticket?, modifier)`; "Ticket not found" for `null`; details (category, situation, badge, "Reported on", description) and status history in stored order with locale-aware date/time; the screen never reads author fields, attachments or location (grep on property access + a privacy preview fed with the raw `sample-2`); new pure `utils/formatDateTime(epochMillis, locale)` with the locale from `LocalConfiguration`; two panes in landscape.
  - Slices: 1. details + not-found + `formatDateTime` + strings + previews (EN/PT, portrait/landscape); 2. status history + empty state + privacy preview; 3. landscape two panes.
  - Open questions with defaults: Q1 no role/`isPublicView` parameter (projection upstream + never read); Q2 no location at all in phase 1 (no map yet); Q2b show the description (as F-003 Q2; goes beyond brief I.1.C); Q3 history as stored, oldest first, no sort (decision 0004); Q4 medium date + short time, device time zone; Q5 two panes in landscape; Q6 no photos/placeholder; Q7 private preview ticket + `SampleTickets.all[1]` raw/projected for the privacy preview; Q8 no callbacks.
  - Handover to Diogo (D-006): `ShowScreen(ticket = tickets.firstOrNull { it.id == id }, modifier = Modifier.padding(innerPadding))` with `id` from `toRoute<TicketDetailRoute>()`; goes into the overview "Screens" entry in the docs phase. Nothing missing in `model/`, `viewmodel/`, `data/` or `ui/navigation/`.
  - Gate 1 pending.
- 2026-11-01: Brief checked at Gate 1 (pdftotext of `docs/brief/AMov.ProjetoKotlin_PT.pdf`). I.1.C limits the anonymous detail to "apenas com imagem(ns) e/ou vídeo(s) e mapa com zoom afastado", hiding the author's personal data and georeferencing. The plan showed the description and the status history to anonymous users and deferred media and the map. Gate 1: revise. The person requires the project to follow the brief strictly; notes sent back to the planner.
- 2026-11-01: Plan revised (revision 2: 00-plan, 01-coding-plan, 03-docs-plan rewritten).
  - Brief quotes (I.1.B, I.1.C, I.2.E, I.3.E) taken from the Gate 1 notes. The planner session had no shell tool, so it could not run `pdftotext` itself.
  - Also read: `Attachment.kt`, `UploadState.kt`, `Municipality.kt`, the version catalog, `app/build.gradle.kts`, `settings.gradle.kts` (mavenCentral present), the manifest (no `INTERNET`), and the drawables. There is no `google-services.json` and no map or image library. The visible samples have no attachments.
  - Approach:
    - stateless `ShowScreen(ticket, audience: DetailAudience, modifier)` with `DetailAudience { PUBLIC, REGISTERED, OPERATOR }`, required, exhaustive `when`; `modifier` stays the first optional parameter for lint;
    - PUBLIC shows the header (category, situation, badge) + media tiles + a coarse map;
    - REGISTERED/OPERATOR add "Reported on", the description and the status history with `formatDateTime` (preview-only in phase 1);
    - media as honest type-icon tiles ("Not available yet"), no Coil;
    - map behind `ui/components/CoarseLocationMap`: fixed zoom, no gestures, circle instead of pin, caption, "Location not available" for (0, 0), placeholder in previews.
  - Slices:
    1. audience + header + not-found + description;
    2. history;
    3. media tiles;
    4. map section with placeholder;
    5. landscape two panes;
    6. real map library + manifest permission, only after Gate 1 answers Q7.
  - Open questions for the person:
    - Q2: header interpretation of "apenas" (default category + situation + badge);
    - Q7: map library and API key (recommend osmdroid: no key, but archived, `INTERNET`, AndroidView; Google Maps Compose needs a Maps key the person must decide on; a static placeholder fails I.1.C), and whether F-005 may edit the manifest;
    - Q6: Coil now or later (default later);
    - Q11: task-file check wording;
    - Q12: "Reported on" hidden for anonymous users.
  - Tension with decision 0006: `toAnonymousView()` keeps `description` and `statusHistory`, so until Diogo extends it those two fields are hidden only in the UI. Handover proposed, and ADR 0007 planned for the docs phase. This also reopens F-003 Q2 (expanded card description).
  - Handovers to Diogo: D-006 call with `audience = DetailAudience.PUBLIC`; projection change; optional sample attachments; manifest `INTERNET` if the manifest is his; Auth task maps the role to `DetailAudience`.
  - Gate 1 pending.
- 2026-11-01: Gate 1 approved by the person (revision 2) with: Q7 static map placeholder, so no library, key or manifest change; slice 6 dropped. The gap against I.1.C's "mapa com zoom afastado" is accepted knowingly and goes into the docs and report. Q2: the PUBLIC header shows category + situation only (I.1.B fields); the status badge only for REGISTERED/OPERATOR. `DetailAudience` stays in `ui/screens/` (used only by ShowScreen and D-006's call). Other questions at their defaults. Decisions added to the top of 00-plan.

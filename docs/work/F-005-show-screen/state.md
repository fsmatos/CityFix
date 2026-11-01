# F-005: ShowScreen (ticket detail)
Branch: dev/Francisco · Base: ad7ca62
Remote: origin/dev/Francisco

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-01 | |
| Plan | done | 2026-11-01 | revision 2 (strict brief): audience parameter, PUBLIC = header + media + coarse map; 6 slices; Q1-Q12 with defaults; Q2 header and Q7 map library/key for the person; Gate 1 pending |
| Gate 1: plan approved | done | 2026-11-01 | approved revision 2: Q7 static placeholder (slice 6 dropped), Q2 category + situation for PUBLIC, DetailAudience in ui/screens |
| Code | done | 2026-11-01 | slices 1-5 committed (bb3f446..8e1b6f1); slice 6 dropped at Gate 1; build + lint at baseline |
| Review | done | 2026-11-01 | approve with should-fixes: 0 blocking, 4 should-fix (S1 tile background, S2 scroll lost on rotation, S3 REGISTERED meaning, S4 UI-only privacy for description/history), 8 nits; build + lint at baseline |
| Docs | done | 2026-11-01 | overview (Screens ShowScreen, Shared components, ViewModel S4), ADR 0007, report notes; 0006 and the task file not edited; not committed |
| Pushed | done | 2026-11-01 | 8725c63 to origin/dev/Francisco |
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
- 2026-11-01: Code slice 1 `bb3f446` feat(F-005): add ShowScreen with audience, header and not-found state.
  - Files: `ui/screens/ShowScreen.kt` (new: `DetailAudience`, `ShowScreen`, `TicketHeader`, `DescriptionSection`, `previewTicket`, previews), `utils/DateFormatting.kt` (`formatDateTime`), strings EN + PT (`ticket_not_found`, `ticket_detail_description_title`).
  - Verify: assembleDebug passes; lint 0 errors, 13 warnings (same IDs as the baseline: 7 UnusedResources on `colors.xml`, 3 NewerVersionAvailable, AndroidGradlePluginVersion, GradleDependency, RedundantLabel).
  - Greps: privacy pattern empty; `\.description\b` only in `DescriptionSection` (called only from the non-PUBLIC branch); no `import android` in `DateFormatting.kt`.
  - Deviation (Gate 1 Q2): `TicketHeader(ticket, showStatus, modifier)` gets a `showStatus` flag, set by an exhaustive `when (audience)` (PUBLIC false, REGISTERED/OPERATOR true), so the badge is hidden for PUBLIC. The coding plan's `TicketHeader(ticket, modifier)` always showed the badge.
  - The whole `previewTicket` (attachments, history, location) is added now so later slices only add sections.
- 2026-11-01: Code slice 2 `96d4823` feat(F-005): show status history to registered citizens and operators.
  - Files: `ShowScreen.kt` (`StatusHistorySection`, `HistoryRow`, divider + section in the REGISTERED/OPERATOR branch, `ShowScreenEmptyHistoryPreview` EN/PT, `ShowScreenPrivacyPreview` with raw `SampleTickets.all[1]`), strings EN + PT (`ticket_detail_history_title`, `ticket_detail_history_empty`).
  - Verify: assembleDebug passes; lint 0 errors, 13 warnings (baseline IDs).
  - Greps: privacy pattern empty; `\.statusHistory\b` once, in the non-PUBLIC branch; no `sortedBy`, `reversed` or `LazyColumn`.
  - No deviation.
- 2026-11-01: Code slice 3 `aa03eda` feat(F-005): show photo and video tiles in ShowScreen.
  - Files: `ShowScreen.kt` (`MediaSection`, `MediaTile`; called after the header for PUBLIC and after the description for REGISTERED/OPERATOR), strings EN + PT (`ticket_media_title`, `ticket_media_empty`, `ticket_media_photo`, `ticket_media_video`, `ticket_media_unavailable`).
  - Verify: assembleDebug passes; lint 0 errors, 13 warnings (baseline IDs).
  - Greps: privacy pattern empty; `\.(file|thumbnail|uploadedById|uploadState)\b` empty.
  - Small additions, not deviations in behaviour: two exhaustive `when (attachment.type)` (label resource and icon); "Not available yet" is centred under the tile.
- 2026-11-01: Code slice 4 `45fe6f4` feat(F-005): add approximate-location section to ShowScreen.
  - Files: `ui/components/CoarseLocationMap.kt` (new: heading, 200dp placeholder box with `LocationOn` and `cd_ticket_map`, caption, "Location not available" for (0, 0), previews EN/PT for both cases), `ShowScreen.kt` (calls it for every audience), strings EN + PT (`ticket_map_title`, `ticket_map_caption`, `ticket_map_unavailable`, `cd_ticket_map`).
  - Verify: assembleDebug passes; lint 0 errors, 13 warnings (baseline IDs).
  - Greps: privacy pattern empty; `latitude|longitude` absent from `ShowScreen.kt`, both string files and also `CoarseLocationMap.kt` (the placeholder only compares with `Coordinates()`); `\.location\b` exactly once in `ShowScreen.kt`; `\.description\b` only in `DescriptionSection`, `\.statusHistory\b` only in the non-PUBLIC branch.
  - Deviations:
    - Gate 1 Q7: the placeholder is the final map. No `LocalInspectionMode` branch (nothing else to render) and the KDoc says so; slice 6 dropped.
    - Portrait layout restructured to read `ticket.location` once: `when (audience)` for the description, then `MediaSection` and `CoarseLocationMap` for all audiences, then a second exhaustive `when (audience)` for divider + history. Same order as the plan (non-PUBLIC: header, description, media, map, divider, history); `MediaSection` is now called once instead of in each branch.
- 2026-11-01: Code slice 5 `8e1b6f1` feat(F-005): split ShowScreen into two panes in landscape.
  - Files: `ShowScreen.kt` only.
  - Verify: assembleDebug passes; lint 0 errors, 13 warnings (baseline IDs; no `ModifierParameter`, no `MissingTranslation`).
  - Greps (final file): privacy pattern and `\.(file|thumbnail|uploadState)\b` empty; no `latitude|longitude`; `\.location\b` once (in `LocationPane`); `\.description\b` only in `DescriptionSection` and `\.statusHistory\b` only in the non-PUBLIC branch of `LocationPane`; `currentStatus` read only in `TicketHeader` under `showStatus`, which is `true` only for REGISTERED/OPERATOR.
  - Deviation: instead of writing the sections twice (portrait column and landscape panes), two private composables `DetailsPane` and `LocationPane` (each `Column(modifier, spacedBy(16.dp))` with one exhaustive `when (audience)`) are used by both orientations: portrait stacks them in one scrolling column, landscape puts them in a `Row` with `weight(1f).fillMaxHeight().verticalScroll(...)`. Same content and order as the plan, and it keeps `ticket.location` read once. `showStatus` is now set in `DetailsPane`'s `when (audience)` (PUBLIC `false`, others `true`), no longer as a separate value in `ShowScreen`.
  - Slice 6 (real map) dropped at Gate 1 (Q7: static placeholder). Known gap against brief I.1.C ("mapa com zoom afastado"): no real map; to record in the docs and the report.
  - Handovers to Diogo (from 00-plan Q10, unchanged): (a) D-006 calls `ShowScreen(ticket = tickets.firstOrNull { it.id == id }, audience = DetailAudience.PUBLIC, modifier = Modifier.padding(innerPadding))`; (b) `toAnonymousView()` should also blank `description` and clear `statusHistory` (amend decision 0006; affects the expanded `TicketCard` and F-003 Q2); (c) optional: IMAGE and VIDEO attachments on a visible sample so the tiles show on a device; (e) the Auth task maps the role *and* the creator/follower relation to `DetailAudience` (REGISTERED only for the citizen who created or follows the ticket, I.2.E; any other registered citizen gets PUBLIC, I.2.D; OPERATOR for any municipal operator, I.3.E; reworded after review S3). (d) (manifest `INTERNET`) no longer needed: no map library.
- 2026-11-01: Review (`review.md`) of `c8b5500..HEAD`. Verdict: approve with should-fixes; nothing blocks.
  - Ran by the reviewer: assembleDebug passes; lint 0 errors, 13 warnings (baseline IDs). All plan greps re-run on the final files and clean. Diogo's folders, shared UI, the manifest and the Gradle files are untouched. Brief extracted with pdftotext: I.1.C met apart from the accepted map gap; I.2.E/I.3.E out of scope.
  - Should-fix: S1 `MediaTile` box uses `surface`, which equals `background`, so the tile is invisible (use `surfaceVariant`). S2 `rememberScrollState()` in different branches loses the scroll position on rotation (hoist two states). S3 REGISTERED must mean creator/follower (I.2.E); other registered citizens get PUBLIC (I.2.D); fix the KDoc and handover (e). S4 description/history are hidden only in the UI; ADR 0007 + overview + raise with Diogo.
  - Nits: N1 comment cites I.1.C for the Gate 1 Q2 choice; N2 badge shown on the anonymous card but not in the detail; N3 no PT landscape not-found preview; N4 `SampleTickets.all[1]` by index; N5 category not a heading; N6 map placeholder description/caption not merged; N7 `formatDate` vs `formatDateTime` locale; N8 task-file wording (Q11).
- 2026-11-01: Review fixes `b60039b` fix(F-005): apply review fixes. Files: `ShowScreen.kt` only.
  - Fixed: S1 `MediaTile` background `surfaceVariant`; S2 `detailsScroll`/`locationScroll` hoisted after the null check and used by both orientations; S3 `DetailAudience` KDoc (REGISTERED = creator/follower, I.2.E; other registered citizens get PUBLIC, I.2.D; OPERATOR = any operator, I.3.E) and handover (e) reworded above; N1 header comment/KDoc now cite Gate 1 Q2 as revised, not I.1.C; N2 (see below); N3 PT-landscape not-found preview; N4 privacy preview uses `first { it.id == "sample-2" }`; N5 category title has `heading()`.
  - N2 changes the Gate 1 Q2 decision (person's choice at review): the status badge is shown to PUBLIC too, consistent with the anonymous list card. `showStatus` removed; the PUBLIC header is category + situation + status badge. "Reported on", the description and the history stay non-PUBLIC only.
  - Left: N6 skipped by the person; N7, S4 and N8 go to the docs phase. `formatDate`, Diogo's folders, the manifest and Gradle untouched.
  - Verify: assembleDebug passes; lint 0 errors, 13 warnings (baseline IDs, no new warnings).
  - Greps: privacy pattern and `\.(file|thumbnail|uploadState)\b` empty; no `latitude|longitude`; `\.location\b` once; `\.description\b` only in `DescriptionSection`; `\.statusHistory\b` only in the non-PUBLIC branch. Changed: `currentStatus` is now read in `TicketHeader` for every audience (by the N2 decision), no longer behind `showStatus`.
- 2026-11-01: Docs phase (03-docs-plan, against the code at `b60039b`). Not committed (the caller commits).
  - `docs/architecture/overview.md`:
    - "Screens": `ShowScreen` entry after `DashboardScreen` (signature, `DetailAudience` table with I.1.C/I.2.D/I.2.E/I.3.E, sections per audience, not-found, privacy, media, map gap, layout and scroll across rotation, timestamps, accessibility, previews, D-006 contract with `audience = DetailAudience.PUBLIC`, other handovers);
    - "Shared components": `formatDateTime` added to the Dates bullet with the N7 follow-up; new `CoarseLocationMap` bullet (static placeholder, I.1.C gap, how to close it with osmdroid or Google Maps Compose);
    - "ViewModel (D-003)": S4 item (projection must also blank `description` and clear `statusHistory`; until then UI-only; affects the expanded `TicketCard`).
  - `docs/decisions/0007-ticket-detail-by-audience.md` (new): context with brief quotes, decision (audience enum passed by the caller, in `ui/screens`), rejected alternatives (isPublicView, role read in the screen, enum in `model/`, default, ...), consequences (S4 projection handover, REGISTERED creator/follower check, map gap, list/detail consistency with the expanded card under F-003 Q2).
  - `docs/report/notes.md`: new "F-005: ShowScreen" section (technical solutions, options chosen, rejected, known gaps, verification, improvement opportunities incl. N7).
  - Deviations from 03-docs-plan (all from Gate 1 / review): no map library section in "External services" (Q7 placeholder, so none chosen; the options are in "Shared components" and ADR 0007); the Q2 header recorded as revised at review (N2: badge shown to PUBLIC); decision 0006 not edited (the ADR format has no "Amended by" convention and 0006 is Diogo's; 0007 states the amendment in its status line).
  - Not done, on purpose: `docs/tasks/phase-1/F-005-show-screen.md` untouched and its checks left unticked (N8/Q11 is the person's call; manual checks pending).
  - Open for the person before `/push-task`: raise S4 (projection change + decision 0006 amendment) with Diogo as a task, together with the list/detail description inconsistency; N8 task-file wording.
- 2026-11-01: N8 decided by the person: reworded the task file check "Status history shown in order with timestamps" to apply to REGISTERED/OPERATOR (previews in phase 1), with PUBLIC following I.1.C (ADR 0007). Checks stay unticked until the manual Android Studio checks.
- 2026-11-01: pushed dev/Francisco to origin (ad7ca62..8725c63) after verify passed (assembleDebug; lint 0 errors, 13-warning baseline). Manual Android Studio checks (previews per audience, PT, landscape, accessibility) still pending; S4 projection change to be raised with Diogo.

# F-005: ShowScreen (ticket detail)
Type: feature · Task file: docs/tasks/phase-1/F-005-show-screen.md
Labels: none
Revision 2 (2026-11-01): Gate 1 asked for the brief to be followed strictly. This replaces revision 1.

## Gate 1 decisions (2026-11-01, approved)
These override the defaults in "Risks and open questions" and the slices where they differ.
- **Q7 map: static placeholder.** No map library, no API key, no manifest change. Slice 4 (`CoarseLocationMap` placeholder body) is the final map; **slice 6 is dropped**. The gap against I.1.C ("mapa com zoom afastado") is recorded in `state.md`, the docs and the report as known and deliberate.
- **Q2 header: category + situation only** for PUBLIC (exactly I.1.B's fields). The status badge is not shown to PUBLIC; REGISTERED/OPERATOR show the badge as well (with "Reported on", description and history).
- **`DetailAudience` stays in `ui/screens/ShowScreen.kt`**: only `ShowScreen` (and D-006's call) use it today; move it to `model/` only if the auth task needs it there.
- Everything else as planned (Q1 required three-value enum, Q3-Q6, Q8-Q12 defaults).

## Problem
Phase 1 needs the screen behind `TicketDetailRoute(ticketId)` (D-004). The brief defines three different detail views:
- **I.1.C, anonymous:** "Detalhe de uma ocorrência pública: ocultando dados pessoais do autor e georreferenciação, apenas com imagem(ns) e/ou vídeo(s) e mapa com zoom afastado."
- **I.2.E, registered citizen:** "Detalhe de uma ocorrência, com acesso a troca de mensagens".
- **I.3.E, operator:** "visualização integral ... (com coordenadas, contactos do autor e histórico completo com fotos e vídeos)" and "transição de estados (com timestamp)".
- For reference, **I.1.B** (anonymous list) shows "categoria, situação e thumbnail".

(Quoted as given in the Gate 1 notes. The planner session had no shell to run `pdftotext` itself; the person should check them against the PDF.)

So the anonymous detail shows **only media and a coarse map**: no description, no status history, no author data, no coordinate text. The task's check "status history shown in order with timestamps" belongs to the non-anonymous detail (operator I.3.E). In phase 1 every user is anonymous and `TicketsViewModel.tickets` gives only projected tickets (decision 0006), so the non-anonymous view can only be shown in previews until the Auth/Firebase phase.

What exists:
- `Ticket` (D-001): `categoryName`, `situation`, `currentStatus`, `createdAt`, `description`, `location: Coordinates`, `attachments: List<Attachment>`, `statusHistory: List<StatusHistory>`.
- `Attachment`: `id`, `type` (IMAGE/VIDEO), `file` (local URI before upload, Storage path/URL after), `thumbnail?`, `uploadState`, `uploadedById`.
- `toAnonymousView()` (D-003) blanks author ids, `messages`, `justification`, `uploadedById` and rounds the location to 2 decimals (about 1 km). It does **not** remove `description` or `statusHistory`.
- Sample data: the two visible samples (`sample-1`, `sample-2`) have **no attachments**. `sample-3` has one with `file = ""`, but it is RESOLVED and never visible. So phase 1 has **no real image or video anywhere**.
- No map or image library in `gradle/libs.versions.toml`; no `INTERNET` permission in the manifest; no `google-services.json`, no Maps API key.
- `StatusBadge`, `statusLabelRes`, `situationLabelRes`, `ic_image_placeholder` (F-002), `material-icons-core` (has `PlayArrow`, `LocationOn`).

## Outcome
A stateless `ShowScreen(ticket: Ticket?, audience: DetailAudience, modifier: Modifier = Modifier)` in `ui/screens/ShowScreen.kt`, with `enum class DetailAudience { PUBLIC, REGISTERED, OPERATOR }` in the same file (Q1).
- `ticket == null`: centred "Ticket not found" / "Ocorrência não encontrada" (all audiences).
- **PUBLIC** (anonymous, the only audience wired in phase 1):
  - header: category, situation and status badge, the same fields the public list card already shows (Q2, interpretation of "apenas", for the person to decide);
  - "Photos and videos": one tile per attachment with a type icon and an honest label ("Photo · not available yet"), never an image that is not the real one; "No photos or videos." when the list is empty (Q6);
  - "Approximate location": a non-interactive map at a fixed coarse zoom, centred on the already-rounded projected location, with an approximate-area circle instead of a pin and **no coordinate text**; "Location not available" when the location is the unset (0, 0) (Q7, Q8);
  - nothing else: no "Reported on", no description, no history, no author data.
- **REGISTERED and OPERATOR** (preview-only in phase 1): the PUBLIC content plus "Reported on <date, time>", the description, and the status history in stored order with a locale-aware date and time per entry ("No status changes recorded." when empty). Messages (I.2.E), exact coordinates and author contacts (I.3.E) stay out of scope; the exhaustive `when (audience)` is where later tasks add them.
- Portrait: one scrolling column. Landscape: two panes, each scrolling on its own (Q5).
- Accessibility: section titles are headings; each history row and each media tile is merged for TalkBack; the map has a content description and no gestures (so it does not fight the page scroll).
- Previews in portrait and landscape, EN and PT (PT landscape included), for PUBLIC and REGISTERED, not-found, empty history/no location, and a privacy preview of the raw unprojected `sample-2` in PUBLIC. Previews show a map placeholder, because a real map cannot render in Android Studio previews.

## Acceptance checks
- [ ] Not-found preview (`ticket = null`) shows only "Ticket not found" / "Ocorrência não encontrada", centred, portrait and landscape (task check 1).
- [ ] PUBLIC previews show only the header (category, situation, badge), "Photos and videos" and "Approximate location". No description, no "Reported on", no history, no coordinates as text (task check 3, brief I.1.C).
- [ ] The privacy preview (PUBLIC, raw `sample-2` with `creatorId`, a follower, `responsibleId = "operator-1"` and exact coordinates) shows none of them, and no description.
- [ ] REGISTERED/OPERATOR previews show the description and the status history in stored order, each entry with its badge and a date and time ("Open", "Info requested", "In progress" for `previewTicket`) (task check 2, now for the non-anonymous view; see Q11).
- [ ] Media tiles show a type icon and "not available yet" text, never a photo-like image; a ticket with no attachments shows "No photos or videos."
- [ ] On a device (after slice 6), the anonymous detail of `sample-1` shows a map around Pr. da República at a distant zoom with an approximate-area circle and no pin; the map cannot be zoomed in or panned; OSM attribution is visible. A ticket at (0, 0) shows "Location not available".
- [ ] `@Preview` exists in portrait and landscape (EN and PT, including PT landscape) for PUBLIC, REGISTERED and not-found; in landscape the two panes sit side by side and scroll independently (task check 4).
- [ ] TalkBack reads each history row and each media tile as one item, the section titles as headings, and the map by its description.
- [ ] Code checks: in `ShowScreen.kt`, `\.(creatorId|followerIds|messages|responsibleId|justification|uploadedById)\b` is empty; `\.description\b` and `\.statusHistory\b` appear only inside the non-PUBLIC branch; `latitude|longitude` appear only in `ui/components/CoarseLocationMap.kt`, and never in a `Text` or string resource.
- [ ] `./gradlew assembleDebug` passes and `./gradlew lint` keeps the baseline (0 errors, 13 warnings) after slices 1-5. Slice 6 adds a dependency; any new warning is listed in `state.md` and accepted or fixed (Q7).

## Approach
**Audience parameter (Q1).** `DetailAudience` is a UI enum in `ShowScreen.kt` with three values, because the brief defines three different views: a `Boolean isPublicView` cannot express that registered citizens get messages but operators also get coordinates and contacts. The screen branches with an exhaustive `when (audience)`, so a later task that adds messages or coordinates must decide for each audience to compile. In phase 1 REGISTERED and OPERATOR render the same thing. `audience` has **no default**: D-006 must state it, and `modifier` stays the first optional parameter (Compose lint `ModifierParameter` wants that).
- Choosing sections in the UI is **not** a projection: the data rule stays below the UI (decision 0006, `toAnonymousView()`). Tension: the projection keeps `description` and `statusHistory`, which brief I.1.C now hides, so for those two fields the anonymous rule would hold only in the UI. CLAUDE.md and `.claude/rules/kotlin.md` say "enforce in the repository layer, not only in the UI". **Handover to Diogo (Q10):** extend `toAnonymousView()` to blank `description` and clear `statusHistory` (keeping `currentStatus`), and amend decision 0006. F-005 does not depend on it: PUBLIC never reads those fields, so it is correct before and after.
- That change also empties the expanded `TicketCard` description in the anonymous list, so it reopens F-003 Gate 1 Q2 (description shown in the expanded list). The strict I.1.B reading ("categoria, situação e thumbnail") agrees with removing it. Flagged for the person; not changed in F-005.

**Header for anonymous users (Q2, interpretation).** "apenas com imagem(ns) e/ou vídeo(s) e mapa" can mean "no other ticket content" (literal) or "of the hidden data, only media and a coarse map remain" (looser). Default: show category, situation and the status badge. They are exactly what the public list card already shows for this ticket, so the detail reveals nothing new, and without them the screen is an unlabelled map and photos. This is an interpretation, and the person decides. The options are none, category + situation (strictly I.1.B's fields), or category + situation + status (the default, matching the card).

**Media without a fake photo (Q6).** Phase 1 has no camera, no Storage and no real file, so an image loader would only ever draw its error drawable. Default: a `MediaTile` per attachment that shows the type icon (`ic_image_placeholder` for IMAGE, `Icons.Filled.PlayArrow` for VIDEO), the type label and "Not available yet". No Coil yet. The media task replaces the tile body with Coil's `AsyncImage(thumbnail ?: file)` once files are `UPLOADED`. Tiles sit in a `Row` with `horizontalScroll`, because a ticket has a few attachments and the row lives inside a vertically scrolling page. The tile reads only `attachment.type` (never `uploadedById`, `file` or `thumbnail`).
- Alternative: add Coil 3 (`io.coil-kt.coil3:coil-compose`, latest 3.x, as taught) now, with `ic_image_placeholder` as placeholder/error. It is the course stack and the Firebase phase needs it, but it adds a dependency with nothing to load in phase 1 and cannot be checked on a device.
- To see tiles on a device in phase 1, a visible sample needs attachments (handover, optional, Q10).

**Coarse map (Q7, Q8).** The map is its own stateless component, `ui/components/CoarseLocationMap(location: Coordinates, modifier)`, so the library choice stays in one file and the screen only passes `ticket.location` (already rounded upstream).
- **Library: the person decides (Q7).** CLAUDE.md requires asking before touching keys or `google-services.json`. The options:

  | Option | Dependency | Key / account | Manifest | Compose fit | Defence | Previews |
  | --- | --- | --- | --- | --- | --- | --- |
  | A. Google Maps Compose (`com.google.maps.android:maps-compose`, 8.x) | yes, pulls Play services maps | **Maps API key** (Google Cloud project with billing), kept out of git (`local.properties` + secrets plugin or manifest placeholder) | `INTERNET`, `<meta-data com.google.android.geo.API_KEY>` | native `GoogleMap {}` composable | idiomatic, likely what the course showed | no render |
  | B. osmdroid (`org.osmdroid:osmdroid-android`, 6.1.x) **(recommended if no key)** | yes, small | none; must set a User-Agent (OSM tile policy) and show attribution | `INTERNET`, `ACCESS_NETWORK_STATE` | `AndroidView { MapView }` wrapper | View interop to explain; the repo was archived in Nov 2024 (no more updates) | no render |
  | C. Static placeholder (no map) | none | none | none | trivial | does **not** meet I.1.C ("mapa") | renders |

  Recommendation: **B** unless the person already has or wants a Maps key. It needs no key, no account and no billing, it is the "OSM" option in the course stack, and the coarse view needs nothing Google-specific. Its costs: an archived library (pin the last 6.1.x and confirm on Maven Central at implementation), a View wrapper to explain at the defence, and the `INTERNET` permission (which Firebase needs anyway). With A, the key question must be answered by the person before slice 6. With C, F-005 knowingly fails I.1.C until a later task.
- **Coarse presentation (Q8), same for every option:** fixed zoom (about 14, a few km across), centred on the projected location, all gestures off (no zoom-in, no pan, no fight with the page scroll), a translucent circle of about 600 m instead of a pin (a pin on a rounded point suggests a precise spot), a caption "Approximate area (about 1 km)", and the tile attribution. A location of (0, 0) shows "Location not available", because (0, 0) means unset (decision 0003) and F-006 reports have no location.
- **Previews:** `LocalInspectionMode.current` makes the component draw a placeholder box (`LocationOn` icon and the caption), because no map view renders in Android Studio previews. The placeholder is also what the component shows from slice 4 until slice 6 adds the library.

**History and timestamps (non-PUBLIC only), kept from revision 1.**
- Order as stored, oldest first, with no sort in the screen. `currentStatus` is the copy of the last entry (decision 0004), so a UI sort could hide a data bug.
- New pure helper `formatDateTime(epochMillis, locale: Locale)` in `utils/DateFormatting.kt` (F-002's file): `java.text.DateFormat` medium date + short time, minSdk 24 with no desugaring.
- Locale from `LocalConfiguration.current.locales[0]`, so PT previews show a PT date (F-002 review S3). Device time zone.
- `dateTime == 0L` and `createdAt == 0L` skip the time text.

**Layout (Q5).**
- Portrait: one `Column` with `verticalScroll`, 16dp padding. PUBLIC shows header, media, map. Non-PUBLIC shows header, "Reported on", description, media, map, divider, history.
- Landscape: a `Row` with two `weight(1f)` panes, each with its own `verticalScroll`:
  - left pane: header (+ "Reported on" and description for non-PUBLIC) + media;
  - right pane: map (+ history for non-PUBLIC).
- Private section composables (`TicketHeader`, `DescriptionSection`, `MediaSection`, `StatusHistorySection`, `HistoryRow`, `MediaTile`) each take `modifier: Modifier = Modifier` as their first optional parameter and apply it once to their own root. This follows the F-004 `ModifierParameter` lesson.
- `rememberScrollState()` is the only remembered state; it is saveable.

**Rejected:**
- `isPublicView: Boolean`: cannot express three audiences.
- An audience default of `PUBLIC`: safe, but hides the decision at the call site, and `modifier` would no longer be the first optional parameter.
- Projecting in the screen: decision 0006.
- Showing the description or history to anonymous users: brief I.1.C.
- Coordinate text: brief I.1.C ("ocultando ... georreferenciação").
- A pin on the rounded point.
- An interactive map: zoom-in defeats "zoom afastado", and it conflicts with the page scroll.
- A sample photo drawable shown as if it were the ticket's photo: dishonest.
- `LazyColumn` for a handful of history rows inside a scrolling page.

## Architecture impact
- New: `ui/screens/ShowScreen.kt` (screen, `DetailAudience`, private sections, previews) and `ui/components/CoarseLocationMap.kt` (the map boundary). Both are Francisco's folders.
- `utils/DateFormatting.kt`: add `formatDateTime`. It stays pure Kotlin.
- Strings EN + PT (`ticket_detail_*`, `ticket_media_*`, `ticket_map_*`, `cd_ticket_map`).
- Slice 6 only, after Gate 1: one library in `gradle/libs.versions.toml` + `app/build.gradle.kts`, and `INTERNET` (+ `ACCESS_NETWORK_STATE` for osmdroid) in `AndroidManifest.xml`. With option A, also a key mechanism, which is the person's call and touches no committed secret.
- No change to `model/`, `viewmodel/`, `data/`, `ui/navigation/` (handovers in Q10).
- ADR needed (docs phase), see 03-docs-plan.

## Failure modes
- **`ticket == null`:** "Ticket not found". Later, an anonymous user watching a ticket that becomes RESOLVED sees it switch to "not found" (the anonymous list holds active tickets only).
- **Unprojected ticket in PUBLIC:**
  - author fields, description and history are never read;
  - the map would centre on the **exact** location. Mitigations: the projection upstream; the fixed coarse zoom, no pin and no gestures limit what the map shows; and the Q10 handover. Optionally the map component could round again itself, but that duplicates the 0006 rule, so it is not the default.
- **No attachments:** "No photos or videos." (the phase-1 case on device).
- **Location (0, 0):** "Location not available", never a map of the Gulf of Guinea.
- **No network:** osmdroid/Google show blank or cached tiles. The caption and circle still render, and the screen does not crash. The brief requires offline data, not offline tiles.
- **OSM tile policy:** a missing User-Agent can get tiles blocked. Set it to the package name, and keep the attribution visible.
- **Map in previews:** a placeholder through `LocalInspectionMode`; the real map can be checked only on a device or emulator.
- **Map lifecycle (osmdroid):** call `onDetach()` in `AndroidView(onRelease = ...)` so the map is released when it leaves the composition. No location overlay is used, so `onResume`/`onPause` are not needed.
- **History order / `currentStatus` mismatch:** shown as stored, so it stays visible.
- **Long PT labels / large font:** every pane scrolls.
- **Preview time zone:** clock times follow the host machine's zone.
- **Lint:** slices 1-5 must keep the baseline. Slice 6 may add `NewerVersionAvailable` or library warnings, which are logged and accepted or fixed. `ModifierParameter` is avoided by parameter order.

## Risks and open questions
Defaults first; Gate 1 confirms or changes each.
- **Q1. Audience parameter.** Default: `enum class DetailAudience { PUBLIC, REGISTERED, OPERATOR }` in `ShowScreen.kt`, a required `audience` parameter, an exhaustive `when`, and REGISTERED = OPERATOR in phase 1. Alternatives: `isPublicView: Boolean`; two values `PUBLIC, FULL`; a default `PUBLIC`.
- **Q2. Header for anonymous users (interpretation of "apenas").** Default: category + situation + status badge (what the list card already shows). Alternatives: no header (literal); category + situation only (I.1.B's fields). **The person decides.**
- **Q3. History order.** Default: as stored, oldest first, no sort.
- **Q4. Timestamps.** Default: `formatDateTime`, medium date + short time, `LocalConfiguration` locale, device time zone; non-PUBLIC only.
- **Q5. Landscape.** Default: two panes (left header/description/media, right map/history), each scrolling.
- **Q6. Media in phase 1.** Default: type-icon tiles with "Not available yet" and no Coil; Coil comes with the media task. Alternative: Coil 3 now with a placeholder/error drawable.
- **Q7. Map library and API key.** **The person decides** (CLAUDE.md: ask before touching keys). Recommendation: osmdroid (no key, archived, `INTERNET` permission, View wrapper). Alternatives: Google Maps Compose with a Maps API key the person creates and keeps out of git; or a static placeholder (fails I.1.C). Any new dependency goes through `gradle/libs.versions.toml`. Also confirm that F-005 may edit `AndroidManifest.xml` for `INTERNET`, or hand it to Diogo.
- **Q8. Coarse presentation.** Default: fixed zoom about 14, no gestures, a 600 m translucent circle, no pin, a caption, attribution, and "Location not available" for (0, 0).
- **Q9. Preview data.** Default:
  - private `previewTicket` (already in anonymous form, 3 history entries, one IMAGE and one VIDEO attachment with `file = ""`, location (40.21, -8.42));
  - `SampleTickets.all[1]` raw in PUBLIC for the privacy preview;
  - `Ticket(id = "empty", categoryName = "Pavement")` for the empty history, no media and no location;
  - `null` for not found.
- **Q10. Handovers to Diogo.**
  - (a) D-006: `ShowScreen(ticket = tickets.firstOrNull { it.id == id }, audience = DetailAudience.PUBLIC, modifier = Modifier.padding(innerPadding))`.
  - (b) A new model task: `toAnonymousView()` also blanks `description` and clears `statusHistory`; amend decision 0006. This affects the expanded `TicketCard` and F-003 Q2.
  - (c) Optional: add an IMAGE and a VIDEO attachment (`file = ""`) to a visible sample, so the tiles show on a device.
  - (d) If the manifest is Diogo's: add `INTERNET`.
  - (e) The Auth task maps the logged-in role to `DetailAudience`.
- **Q11. Task file wording.** The task file's check "Status history shown in order with timestamps" now applies to REGISTERED/OPERATOR (previews only in phase 1), and "Public view ..." gains "only media and coarse map". The person may want to reword `docs/tasks/phase-1/F-005-show-screen.md` (the planner only writes under `docs/work/`).
- **Q12. "Reported on" for anonymous users.** Default: hidden (not in I.1.B or I.1.C); shown to REGISTERED/OPERATOR.

Other risks:
- osmdroid is archived. If it misbehaves with targetSdk 37 or AGP 9, fall back to option A or C at slice 6 and log it.
- A real map can only be verified on a device or emulator with network. Google Maps (A) also needs Play services on the emulator image.
- The brief quotes come from the Gate 1 notes; check them against the PDF.
- The manual Android Studio and device checks can only be done by the person.

## Out of scope
- Messages (I.2.E), exact coordinates and author contacts (I.3.E), operator transitions, reopen and follow.
- Real images and videos (Coil, Storage, thumbnails), unless Q6 picks Coil.
- Wiring the audience from a login (Auth task) and the `NavHost` (D-006).
- Changing `toAnonymousView()`, `SampleTickets` or `TicketCard` (handovers and follow-ups).
- Tablet layouts and window size classes.

## Slices
Each slice is one commit and leaves the build green.
1. **Audience, header, not-found.** `DetailAudience`, `ShowScreen(ticket, audience, modifier)`, not-found branch, `TicketHeader` for all audiences; for non-PUBLIC, `formatDateTime` and "Reported on" + `DescriptionSection`; strings; previews PUBLIC/REGISTERED/not-found in portrait, landscape, PT, PT landscape.
2. **Status history (non-PUBLIC).** `StatusHistorySection`, `HistoryRow`, the empty message, headings; the empty-history preview and the privacy preview.
3. **Media tiles.** `MediaSection` + `MediaTile` (type icon, honest label, merged semantics), the empty message, for all audiences; preview attachments.
4. **Map section with placeholder.** `ui/components/CoarseLocationMap` with the placeholder body (icon, caption) and the (0, 0) "Location not available" branch; used by the screen for all audiences. No dependency yet.
5. **Landscape two panes.** Split into the two panes in landscape; portrait unchanged.
6. **Real map (after the Gate 1 answer to Q7).** Add the library through the catalog and the permissions, and put the real map body in `CoarseLocationMap` when not in inspection mode (fixed zoom, no gestures, circle, attribution, User-Agent). Verify on an emulator. If Q7 picks C, this slice is dropped and the gap against I.1.C is recorded.

Revision 2 (2026-11-01, strict brief).

## Docs to update
- `docs/architecture/overview.md`, "Screens": a `ShowScreen` entry after `DashboardScreen`:
  - the signature `ShowScreen(ticket, audience, modifier)` and `DetailAudience { PUBLIC, REGISTERED, OPERATOR }`, with what each audience shows, quoting I.1.C, I.2.E and I.3.E;
  - the Q2 header interpretation as decided at Gate 1;
  - "not found" for `null`;
  - privacy: the fields the screen reads per audience; the projection stays upstream; the map gets the projected location;
  - media tiles without real images in phase 1 (Q6);
  - the layout in both orientations;
  - **Contract for D-006**: the `composable<TicketDetailRoute>` block with `audience = DetailAudience.PUBLIC`, `innerPadding`, the cutout note (F-003 review N3);
  - the other handovers to Diogo (Q10).
- `docs/architecture/overview.md`, "Shared components":
  - add `CoarseLocationMap(location, modifier)`: fixed distant zoom, no gestures, circle instead of pin, caption, attribution, "Location not available" for (0, 0), placeholder in previews;
  - in the Dates bullet, add `formatDateTime(epochMillis, locale)`.
- `docs/architecture/overview.md`, "Components" or "External services": the map library and permission chosen at slice 6 (Q7), and the OSM tile policy (User-Agent, attribution) or the Maps key handling.
- `docs/architecture/overview.md`, "ViewModel (D-003)": a note that brief I.1.C hides `description` and `statusHistory` from anonymous users, and the handover that `toAnonymousView()` should drop them (until Diogo's change, only the UI enforces it).
- `docs/tasks/phase-1/F-005-show-screen.md`: tick the acceptance checks only after the manual checks pass. If the person rewords the checks (Q11), that is their edit.
- `state.md`: phase rows, log, handovers.

## Decisions to record
- **New ADR `docs/decisions/0007-ticket-detail-by-audience.md`** (needed):
  - context: brief I.1.C, I.2.E and I.3.E define three detail views;
  - decision: `DetailAudience` enum with a required parameter, PUBLIC = header + media + coarse map, non-PUBLIC = + description + history; the Q2 header interpretation; no coordinate text; the coarse map presentation (Q8);
  - the map library chosen (Q7) and why;
  - media placeholders in phase 1 (Q6).
  - Consequences:
    - it **amends decision 0006**: the projection should also drop `description` and `statusHistory` (Diogo's change). Until then those two fields are hidden only in the UI, which goes against the "not only in the UI" rule;
    - it reopens F-003 Gate 1 Q2: the expanded card shows the description to anonymous users, against the strict I.1.B.
  - Rejected: `isPublicView`, a default audience, coordinate text, a pin, an interactive map, fake photos.
- Add one line to decision 0006 pointing to 0007 only if Diogo agrees (it is his decision record); otherwise 0007 references 0006 and the handover stands.

## Report notes
Add "F-005: ShowScreen" to `docs/report/notes.md`:
- Technical solutions:
  - one stateless screen with an audience enum and exhaustive `when`, which maps the brief's three detail views directly;
  - privacy in layers (upstream projection, screen reads per audience, coarse map with no pin and no gestures on a rounded point);
  - the map behind one component (library swap in one file, placeholder in previews through `LocalInspectionMode`);
  - with osmdroid: View interop with `AndroidView` and `onRelease`, the OSM tile policy;
  - honest media placeholders;
  - locale-aware date and time with `java.text`;
  - the two-pane landscape layout.
- Options chosen: the Q1-Q12 outcomes, with the Q2 interpretation of "apenas" spelled out for the defence.
- Rejected: see the ADR. Also revision 1 of this plan (description and history to anonymous users), rejected at Gate 1 for following the brief strictly.
- Improvement opportunities:
  - real media with Coil and thumbnails;
  - an exact map and author contacts for operators (I.3.E);
  - messages for registered citizens (I.2.E);
  - a "no longer public" message when a watched ticket gets RESOLVED;
  - clustering coarse markers on a future overview map;
  - moving to a maintained map library if osmdroid was chosen (it is archived);
  - offline tiles.

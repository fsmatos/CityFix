# 0006: Anonymous projection of tickets in phase 1

Status: accepted (D-003, Gate 1 on 2026-10-29; S1/S2 review fixes on 2026-10-29). Superseded in part by [0009](0009-tickets-repository.md) (F-008, with Diogo's OK): the projection now runs in `LocalTicketsRepository`, not the ViewModel, and Q6/Q11's "no repository yet" no longer holds. Media point amended on 2026-11-08 by F-009. Amended on 2026-11-10 by D-007: the projection also drops `description` and `statusHistory`, the expand toggle is hidden for anonymous users, and the public projection contract for F-011 is recorded (see "Amended 2026-11-10 (D-007)"). Amended on 2026-11-11 by D-009: `canExpandCards` stays `false` for every session, not set by role (see "Amended 2026-11-11 (D-009)" and [decision 0015](0015-session-role-navigation-mfa.md)).

Pointer (F-013, 2026-11-14): the media items are now handled by [decision 0015](0015-media-preparation-and-uploads.md): files get UUID names (no `CAPTURE_` time), photos are re-encoded with no EXIF at all (make, model, capture time and GPS gone; `FileUtils.removeImageLocation` was removed), videos keep their recording metadata (known limit).

## Context
Phase 1 has no login, so every viewer of the app is an anonymous citizen. The brief limits what anonymous users see: the list of "ocorrências públicas ativas" (I.1.B), and a detail with images/videos and a coarse map zoom, "ocultando dados pessoais do autor" (I.1.C). CLAUDE.md makes this non-negotiable: anonymous users must never see the author's personal data or exact coordinates, and the rule is enforced below the UI, not only in the screens.

`TicketsList.publicActive()` (D-002) filters **which tickets** are returned, not **which fields** ([decision 0005](0005-tickets-list-rules.md)). Each returned `Ticket` still carries `creatorId`, `followerIds`, `messages` and the exact `location`. During planning a second leak was found: the first OPEN `StatusHistory` entry's `responsibleId` is the creator's id, so blanking `creatorId` alone is not enough. The review found a third: `StatusHistory.justification` is free text that can name the author.

In phase 1 the lowest layer between the data and the screens is `TicketsViewModel` (D-003), so the projection is applied there, using a pure rule in `model/`.

## Decision
- **Projection as a pure function that keeps the `Ticket` type (Q1).** `fun Ticket.toAnonymousView(): Ticket` in `model/TicketPrivacy.kt`, with no Android imports. It returns a copy with:
  - `creatorId` blank, `followerIds` and `messages` empty (which also drops every `Message.authorId`);
  - every `StatusHistory.responsibleId` blank and every `StatusHistory.justification` null (review S1);
  - every `Attachment.uploadedById` blank;
  - the location coarsened (next point).

  The KDoc lists these fields and says to update the function when `Ticket` gains a personal field.
  - Rejected: a separate `PublicTicket` class. The type would prove the rule at compile time, but it duplicates `Ticket` and breaks the screen signatures agreed with Francisco (`ListScreen(tickets, ...)`, `ShowScreen(ticket?, ...)`, `TicketCard(ticket, ...)`).
  - Rejected: projecting in the screens (F-005's check). The rule must hold below the UI.
- **Location rounded to 2 decimals (Q2).** `ANONYMOUS_COORDINATE_DECIMALS = 2`: about 1.1 km of latitude and 0.85 km of longitude at Coimbra, enough for the brief's "coarse map zoom". (0, 0) stays (0, 0).
  - Rejected: 3 decimals (about 100 m), which still points to a street.
  - Rejected: zeroing the location. It clashes with "(0, 0) = unset" ([decision 0003](0003-domain-model-representation.md)), so map code would have to special-case it everywhere.
- **Media files are not covered (review S2).** The brief requires anonymous users to see the images and videos, so `Attachment.file` and `thumbnail` pass through. The protection belongs where the files are created: the media-upload task strips EXIF GPS tags at compression (`utils/`) and uses Storage paths with no user id.
  - **Amended 2026-11-08 ([F-009](../work/F-009-show-attachment-images/00-plan.md)).** Upload time is too late: since F-009 the card and the detail draw the local files. Photo GPS is now removed at report time, in `LocalTicketsRepository.reportTicket` (`FileUtils.removeImageLocation`, `androidx.exifinterface`), before the ticket is stored. It clears the whole EXIF GPS block; make, model and capture time stay. If the file cannot be rewritten (for example HEIC), the attachment and the report are kept with their tags (Gate 1 Q2), with no log. Known limits: video location metadata (needs a remux; deferred to the media task) and location in an XMP packet are not removed. A Firebase repository must keep the same step. Storage paths with no user id stay with the media-upload task.
- **What the ViewModel exposes (Q3, Q4).**
  - `tickets` = `publicActive()` mapped through `toAnonymousView()`; registered-citizen and operator views are later tasks.
  - `getTicketById` searches only that visible list, so an anonymous user who knows the id of a PRIVATE, RESOLVED or CLOSED ticket cannot open it. Cost: `sample-3` (RESOLVED) cannot be opened in phase 1; it is not in the list anyway.
- **Dashboard stays over every ticket (Q9 of decision 0005).** `stats` returns aggregate counts and an average, with no per-ticket data, so private tickets are counted without being exposed.
- **10 m grouping deferred (Q8).** Phase-1 reports (F-006) have no location input, so a new ticket is at (0, 0) and `findNearby` never matches it. Wiring a follow path now would be dead code. It is added by the task that brings the report location: call `findNearby`; if it finds a ticket whose `creatorId` and `followerIds` do not include the reporter, append the reporter to `followerIds` and return that ticket's id.
  - Rejected: a fake location on new tickets (for example the municipality centre). Two reports at the same fake point would group with each other once grouping is on.
- **Two flows, no repository yet (Q6, Q11).** Gate 1 kept the planner's recommendations: separate `tickets` and `stats` flows instead of one `UiState` (no loading or error states in phase 1), and no `TicketsRepository` interface until Firebase arrives.

## Consequences
- A field added later to `Ticket`, `StatusHistory` or `Attachment` is copied through unchanged. It must be checked against `toAnonymousView()`, and the reviewer compares the KDoc list with the model files.
- Coarse locations collapse onto a grid: all three sample tickets project to (40.21, -8.42). A future anonymous map must cluster or offset markers, never fall back to exact coordinates.
- The Firebase phase cannot hide fields inside a Firestore document, so it writes the same projection into a public reduced-field collection (phase-1 README, risk 3). The pure function is reused there.
- Registered citizens and operators need a separate, unprojected source (another flow or ViewModel); the current `tickets` must not be reused for them. Since D-007 this also holds for the description and the status history, which the projection drops: a registered or operator screen fed from `tickets` would show neither.
- A PRIVATE report disappears from the anonymous list right after submit, and its id cannot be opened with `getTicketById`. D-006 must not navigate to it and should confirm the report another way (for example a snackbar).
- A phase-1 report has no attachment, which does not meet the diagram's `1..*` for `Anexo`. Accepted for the provisional form; creation logic enforces it once media exist.

## Amended 2026-11-10 (D-007)
Task: [D-007](../tasks/phase-1/D-007-anonymous-projection.md); plan and Gate 1: `docs/work/D-007-anonymous-projection/00-plan.md` (approved by Diogo on 2026-11-10, Q1 option A, Q2-Q7 as recommended). Closes F-005 review S4 and the "List/detail consistency" consequence of [decision 0007](0007-ticket-detail-by-audience.md).

### Context
- The projection above kept `description` and `statusHistory`. Brief I.1.C limits the anonymous detail to media and a coarse map, so `ShowScreen` hid the two fields in its PUBLIC branch: privacy enforced only in the UI, against CLAUDE.md and `.claude/rules/kotlin.md`.
- D-006 wired the list's expand toggle for everyone. In phase 1 every user is anonymous, so an expanded `TicketCard` showed the free-text description (which may hold personal data) and "Reported on <date>". Brief I.1.B lists only category, situation and thumbnail. Blanking `description` alone would not close this, because `createdAt` stays in the projection.

### Decision
- **The projection also drops `description` and `statusHistory`.** `toAnonymousView()` sets `description = ""` and `statusHistory = emptyList()`. `currentStatus` is a separate field ([decision 0004](0004-denormalised-current-status.md)), so the status badge still shows. The per-entry blanking of `responsibleId` and `justification` is gone, because the whole history is dropped; dropping it also removes the creator's id held by the first OPEN entry.
  - Rejected: keeping the history with only status and date. I.1.C says "apenas" media and map, and the PUBLIC detail would not show it anyway.
  - Still rejected: a separate `PublicTicket` type (same reasons as above).
- **Kept fields and why.**
  - `id`: opens the detail; a random UUID, not personal.
  - `situation`, `categoryId`, `categoryName`: the I.1.B list fields.
  - `scope`: always PUBLIC after `publicActive()`; harmless, F-011 may omit it.
  - `currentStatus`: the status badge on the card and the detail.
  - `createdAt` (Gate 1 Q2): kept for ordering (Firestore `orderBy` in F-011), **never shown** to anonymous users. It is not the author's personal data. Zeroing it was rejected because `0L` means "not set" ([decision 0003](0003-domain-model-representation.md)), so the projection would fake missing data; rounding it to the day was rejected as code for little gain.
  - `location`: rounded as above.
  - `attachments`: `id`, `type`, `file`, `thumbnail`, `uploadState` pass through (I.1.B thumbnail, I.1.C media); `uploadedById` blank.
- **The expand toggle is hidden for anonymous users (Gate 1 Q1, option A).** `MainTopAppBar` has a required `showExpandToggle: Boolean` (no default, Q3, so the role decision is made at the call site); the toggle shows only when `screen == LIST && showExpandToggle`. `CityFixApp` has one local `val canExpandCards = false` (phase 1: every user is anonymous; D-007 expected D-009 to set it by role, but D-009 kept it `false`, see "Amended 2026-11-11 (D-009)"), passed as `showExpandToggle = canExpandCards` and as `ListScreen(isExpanded = canExpandCards && isExpanded)`. The `isExpanded` `rememberSaveable` and the toggle code stay for registered citizens and operators (Q4).
  - Why the `&&` is kept although the toggle is hidden: once D-009 makes the flag depend on the role, a registered user who expanded the cards and then logs out keeps `isExpanded = true` in `rememberSaveable`; the `&&` still forces the anonymous cards collapsed.
  - Rejected B: keep the toggle and give the expanded PUBLIC card only allowed fields. It changes Francisco's `TicketCard`/`ListScreen`, needs a design the brief does not ask for, and with nothing extra to show it is a button that does nothing visible.
  - Rejected C: change only the projection and also zero `createdAt` (the card skips the date for `0L`). The toggle would again do nothing visible, the public list needs `createdAt` for ordering, and `0L` would fake missing data.
  - Cost: the expanded state cannot be seen on a device in phase 1, so D-006's "expanded state kept on rotation" check cannot run until roles exist; the `TicketCard`/`ListScreen` expanded previews still render it.
- **No change elsewhere.** The two `toAnonymousView()` call sites in `LocalTicketsRepository` (`visibleTickets()`, `getTicketById`), `TicketsViewModel`, the screens and `TicketCard` are unchanged. Stats run on the raw `TicketsList` in `loadStats()`, and `publicActive()` sorts before the projection, so clearing the history cannot change the dashboard figures or the order.

### Public projection contract (for F-011 and D-013)
F-011 writes `publicTickets/{id}` from `toAnonymousView()`, the single function that builds the public document, so phase 1 and phase 2 share one rule. Firestore cannot hide fields inside a document, so a separate collection is the only way.

| Field | In the projection | Why |
| --- | --- | --- |
| `id` | kept | opens the detail; random UUID |
| `situation`, `categoryId`, `categoryName` | kept | brief I.1.B list fields |
| `scope` | kept (always PUBLIC) | harmless; may be omitted |
| `currentStatus` | kept | status badge; activity filter |
| `createdAt` | kept, not shown | ordering (`orderBy`) |
| `location` | rounded to 2 decimals (about 1 km); (0, 0) stays (0, 0) | coarse map only (I.1.C) |
| `attachments` | kept, each with `uploadedById = ""` | I.1.B thumbnail, I.1.C media |
| `description` | `""` | free text, may hold personal data; not in I.1.B/I.1.C |
| `statusHistory` | empty | not in I.1.C; held the creator's id and free-text justifications |
| `creatorId` | `""` | author |
| `followerIds`, `messages` | empty | other citizens, message authors |

`Attachment.phase` (REPORT or RESOLUTION, added by D-008, [decision 0011](0011-ticket-lifecycle-rules.md)) is kept in each attachment: it is not personal data.

Rules the public document must keep:
- A projected ticket is a **view, not a valid ticket**: it has a `currentStatus` with an empty history, which breaks the "at least one history entry" rule and the "update both together" note on `Ticket.currentStatus` (decision 0004). Never write it back to `tickets/`, and never run lifecycle rules (D-008) on it. (`Ticket.kt`'s `currentStatus` KDoc does not mention this yet; add one line next time that file changes, D-007 review N2. Done by D-008 on 2026-11-11.)
- Dashboard stats cannot come from `publicTickets`: no history (no RESOLVED date, so no repair time) and no private tickets. F-011's counters or stats document must read the full data or be maintained on write.
- 10 m grouping (`findNearby`) must run on full tickets: a 1 km grid point is useless at 10 m.
- The public document must be rewritten whenever a field it holds changes: status (D-013), attachments after upload (F-013); deleted or rewritten when the scope or activity changes. That is F-011/D-013's design; D-007 only defines the content.
- Empty values may be written as missing fields instead of `""`/`[]`; the mapping must then read missing fields as the model defaults. Either way, the Firestore rules must not let anonymous users read `tickets/`.
- A field added later to `Ticket`, `Attachment` or `StatusHistory` passes through until someone adds it to the projection; the KDoc of `toAnonymousView()` lists every dropped and kept field so a reviewer can compare it with the model files.

### Known limits left to other tasks (Gate 1 Q7)
None of these reaches the screens as text; they are recorded with their owners.
- **`CAPTURE_<epoch millis>.jpg` file names.** A camera photo's `Attachment.id`/`file` carries the capture time, close to the hidden `createdAt` (gallery copies use `IMG_/VID_<UUID>`). Not personal data, never shown. Owner: F-013 (neutral names, and not carried into Storage paths).
- **EXIF make, model and capture time** stay on photos (F-009 Gate 1). Owner: F-013 / the media task (compression can rewrite or drop them).
- **Video location metadata and XMP location** are not removed (F-009 known limits). Owner: the media task (remux for video, XMP handling).
- **Coarse locations collapse onto one grid point** (consequence above). Owner: the map work (F-012), which must cluster markers.

### Consequences
- Privacy for anonymous users is now enforced in the projection for every field the brief hides; `ShowScreen`'s `when (audience)` and the hidden toggle are a second layer, no longer the only one.
- `ShowScreen` and `TicketCard` needed no change: PUBLIC never read the two fields, and the card reads them only when expanded, which anonymous users can no longer reach.
- Decision 0007 (Francisco's record) still says the projection change is "not yet done"; pointing it here is left to Francisco (Gate 1 Q6).

## Amended 2026-11-11 (D-009)
- **`canExpandCards` stays `false` for every session** (D-009 Gate 1 Q7, Diogo, a deviation from the plan's "operators only"). At Gate 1 the list was always the projected list; since F-011 and D-009's session wiring (2026-11-14) an active citizen's list holds their own plus the public tickets and an operator's holds all, but the cards and the detail still render only the public fields for every role, so an expanded card would add only the date; brief I.1.B lists category, situation and thumbnail, and I.2.D gives registered citizens the anonymous features. The operator's full view is D-013's operations panel (brief I.3.E), not this list. Details: [decision 0015](0015-session-role-navigation-mfa.md).
- The flag, the `isExpanded` `rememberSaveable` and the `canExpandCards && isExpanded` wiring stay in `CityFixApp`, with the comment updated, for a later decision (for example if F-014's "my tickets" list reuses the cards with unprojected tickets). The `&&` reason above still holds if the flag ever becomes role-dependent.
- The D-006 check "expanded state kept on rotation" still cannot run on a device.

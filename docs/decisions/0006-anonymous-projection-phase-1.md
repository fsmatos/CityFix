# 0006: Anonymous projection of tickets in phase 1

Status: accepted (D-003, Gate 1 on 2026-10-29; S1/S2 review fixes on 2026-10-29). Superseded in part by [0009](0009-tickets-repository.md) (F-008, with Diogo's OK): the projection now runs in `LocalTicketsRepository`, not the ViewModel, and Q6/Q11's "no repository yet" no longer holds.

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
- Registered citizens and operators need a separate, unprojected source (another flow or ViewModel); the current `tickets` must not be reused for them.
- A PRIVATE report disappears from the anonymous list right after submit, and its id cannot be opened with `getTicketById`. D-006 must not navigate to it and should confirm the report another way (for example a snackbar).
- A phase-1 report has no attachment, which does not meet the diagram's `1..*` for `Anexo`. Accepted for the provisional form; creation logic enforces it once media exist.

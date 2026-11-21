# D-019: Show exact coordinates and attachment authors in the operator detail
Type: bug · Task file: docs/tasks/phase-3/D-019-operator-detail-coordinates-uploader.md
Labels: none

## Problem
Brief I.3.E: the operator sees the ticket "with its exact location, the author's contacts and the
full history with photos/videos and who added them". In the operator detail (`OperatorTicketScreen`
-> `ShowScreen` with `DetailAudience.OPERATOR`) the exact location is only a map (no latitude and
longitude as text), and the photo/video tiles never say who attached them.

## Outcome
In the operator detail only:
- the "Location" card shows the coordinates as text under the exact map, e.g. `Coordinates: 40.21000, -8.41900`;
- each photo/video tile shows "Added by <name>" when the uploader's profile could be read (the
  resolution proof shows the operator who resolved).
Public and registered-citizen details look exactly as today.

## Acceptance checks
- [ ] Operator opens a ticket: under the exact map, "Coordinates: <lat>, <lng>" with 5 decimals and a dot as the decimal separator, in EN and PT.
- [ ] TalkBack on the coordinates reads "Latitude <lat>, longitude <lng>" (PT: "Latitude ..., longitude ...").
- [ ] Ticket with location (0, 0) (not set): "Map not available" as today, no coordinates line.
- [ ] Each report photo/video shows "Added by <citizen name>"; a follower's added media shows the follower's name; each resolution proof shows "Added by <operator name>" (PT: "Adicionado por ...").
- [ ] TalkBack on a tile reads the type, "Resolution proof" when relevant, the upload line when relevant, and "Added by <name>" as one item.
- [ ] Uploader whose profile cannot be read (offline with nothing cached, deleted profile) or blank `uploadedById`: no "Added by" line, no crash, no id shown.
- [ ] Anonymous user (and a citizen viewing a public ticket they do not follow): no coordinates text, no names (unchanged).
- [ ] Registered citizen on their own ticket: unchanged (exact map, no coordinates text, no "Added by").
- [ ] Portrait and landscape (rotate on the operator detail): both lines visible, nothing clipped, long names ellipsised inside the 120dp tile.
- [ ] `./gradlew assembleDebug` and `./gradlew lint` pass.

## Reproduction
1. Sign in as an operator; open the operator panel and any ticket with photos (ideally one already resolved with proof).
2. Look at the "Location" card: only the map, no latitude/longitude text.
3. Look at "Photos and videos": each tile shows only "Photo"/"Video" (and "Resolution proof"), never who added it.
4. Compare with the history card, which already shows "By <name>" (D-013).

## Root cause
Not a regression; the two items were never implemented:
- `ShowScreen.kt` `ExactLocationSection(location)` deliberately draws only `TicketMap(exact = true)`
  (its KDoc says "never coordinate text"), the same for REGISTERED and OPERATOR.
- `ShowScreen.kt` passes `responsibleNames` only to `LocationPane` -> `StatusHistorySection`.
  `DetailsPane` -> `MediaSection(ticket.attachments)` -> `MediaTile` gets no names, so
  `Attachment.uploadedById` (stored in Firestore, `FirestoreMapping.kt:184/307`) is never used.
- Name lookup: `OperatorTicketScreen` watches profiles for `creatorId + followerIds + history
  responsibleIds`, not for `attachments.uploadedById`. Today every uploader is in that set in
  practice (report media = creator, `FirestoreTicketsRepository.kt:189`; media added when joining =
  a follower, `:443`; proof = the operator who wrote the RESOLVED history entry,
  `TicketLifecycle.kt:134`), but nothing guarantees it (e.g. a follower who later unfollows).

## Approach
Chosen (smallest change, reuses D-013's lookup):
1. `OperatorTicketScreen`: add `t.attachments.map { it.uploadedById }` to the watched ids, so every
   uploader's profile is read by the existing `watchPeople` (blank ids are already skipped). The
   existing `responsibleNames` map (id -> name of readable profiles) then contains every uploader.
2. `ShowScreen`: pass that same map to `DetailsPane` -> `MediaSection` -> `MediaTile`; a tile whose
   `uploadedById` is in the map shows "Added by <name>". Only the operator passes a non-empty map,
   so other audiences show nothing (and the public projection already blanks `uploadedById`,
   `TicketPrivacy.kt:43`).
3. `ShowScreen`: `ExactLocationSection` gets `showCoordinates = audience == DetailAudience.OPERATOR`;
   when true and the location is set, a text line under the map with the coordinates formatted with
   `Locale.ROOT` and 5 decimals (about 1 m), and a TalkBack description "Latitude x, longitude y".

Alternatives considered:
- New `uploaderNames` parameter separate from `responsibleNames`: clearer name, but a second map of
  the same data; rejected for minimal change (see open question Q1).
- Show coordinates also to the registered citizen: the brief only asks it for the operator (I.3.E),
  and the task says keep the citizen view unchanged unless decided; not done (Q2).
- Locale-formatted decimals (PT "40,21000"): with the comma separator between lat and lng it becomes
  ambiguous ("40,21000, -8,41900"); rejected in favour of `Locale.ROOT`.

## Architecture impact
UI only. No model, repository, Firestore document, rules or navigation change. `ShowScreen`'s
public signature does not change (the existing `responsibleNames` gets a wider KDoc meaning:
"names of people on the ticket, operator view only"). Three new strings in EN and PT.

## Failure modes
- Profile listener not yet delivered: id absent from the map, no "Added by" line; appears live when it arrives.
- Profile unreadable (null): no line (as the history today).
- Blank `uploadedById` (old documents, sample tickets): map miss, no line.
- Location (0, 0): existing "Map not available" text, coordinates line skipped.
- Very long name: `maxLines = 2` + ellipsis in the 120dp tile, so the row does not grow unbounded.

## Risks and open questions
- Risk: `ShowScreen.kt` is Francisco's file (F-005); the change is additive but may conflict with his branch. Tell him after the commit.
- Risk: deadline 2026-11-22; the plan is one slice, about 40 lines.
- Q1: keep the parameter name `responsibleNames` (no churn) or rename it to `peopleNames` now that it also feeds the tiles? Plan: keep it, update the KDoc.
- Q2: registered citizen keeps no coordinates text and no "Added by" (plan: unchanged, as the task says). Confirm.
- Q3: format `40.21000, -8.41900` (5 decimals, dot, `Locale.ROOT`) in both languages. Confirm.
- Q4: PT wording "Adicionado por %1$s" (covers "anexo"; photo is feminine in PT). Alternative: reuse the existing "Por %1$s" / "By %1$s" string. Plan: new string "Added by" / "Adicionado por".

## Out of scope
- Coordinates or names for anyone but the operator; copying coordinates to the clipboard or opening an external maps app.
- Showing the upload date per attachment; names inside the full-screen `AttachmentViewer`.
- The operator's proof gallery while resolving (`AttachmentGallery` in `ResolveSection`).

## Slices
1. `fix(D-019): show coordinates and attachment authors in the operator detail` — watched ids +
   ShowScreen coordinates line + "Added by" on tiles + strings + operator preview with uploaders.

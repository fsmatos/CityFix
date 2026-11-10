# 0010: Firestore and Storage layout

Status: accepted (F-010, Gate 1 on 2026-11-10; Diogo agreed the schema before the Gate). Rules drafted in `firestore.rules` and `storage.rules`, not deployed (Q5 in [decision 0011](0011-firebase-plan-notifications-close-maps.md)).

## Context
- Phase 1 kept everything in memory (`LocalTicketsRepository`, [decision 0009](0009-tickets-repository.md)). Every Firebase task (F-011..F-016, D-009..D-014) needs one shared data layout.
- Phase 1 left six schema risks open: attachment phase, FCM token and `role`, a public collection, NIF uniqueness, one municipality document, the reopen window.
- Firestore rules secure whole documents, not fields. Anonymous users must never see the author's personal data or exact coordinates (CLAUDE.md), so the anonymous view cannot be a filtered read of `tickets`.
- Brief IV wants reads and writes to work offline. Firestore transactions fail without a network; batched writes are queued.

## Decision

### Layout
Model types stay as [decision 0003](0003-domain-model-representation.md) defines them (pure Kotlin, `Long` times, enum names as strings). All mapping lives in `data/`. Firestore-only fields (`role`, `fcmTokens`, `participantIds`, the public projection, `ticketLocations`) are not added to `model/`.

| Path | Fields | Who reads | Who writes |
| --- | --- | --- | --- |
| `users/{uid}` | `role` (`CITIZEN`/`OPERATOR`), `name`, `nif`, `email`, `phone`, `photoPath?`, `createdAt`; citizen only: `georeferencing` (GeoPoint), `registrationStatus` (`PENDING`/`ACTIVE`), `impactDetection`, `validatedBy?`, `validatedAt?`; both: `fcmTokens` (array) | the owner; operators | the owner at registration and for editable fields; operators set `registrationStatus`, `validatedBy`, `validatedAt` |
| `nifs/{ROLE}_{nif}` | `uid` | signed-in users (`get` only, to check uniqueness) | created once, in the registration transaction with `users/{uid}`; never updated |
| `municipality/main` | `name`, `emailDomain`, `center` (GeoPoint, Pr. Republica), `radiusKm` (10.0) | everyone, anonymous included | console only |
| `categories/{id}` | `name`, `active` | everyone | operators |
| `tickets/{id}` | `description`, `situation`, `scope`, `location` (GeoPoint), `currentStatus`, `createdAt`, `categoryId`, `categoryName`, `creatorId`, `followerIds`, `participantIds`, `statusHistory` (array of `{status, dateTime, justification?, responsibleId}`), `attachments` (array of `{id, type, phase, storagePath, thumbnailPath?, uploadState, uploadedById}`) | participants; operators | the creator at creation (batch); an active citizen may only add their own uid to `followerIds`/`participantIds`; operators and participants for lifecycle changes |
| `tickets/{id}/messages/{id}` | `text`, `sentAt`, `authorId` | participants; operators | participants; operators (author = self) |
| `publicTickets/{id}` | `categoryId`, `categoryName`, `situation`, `currentStatus`, `coarseLocation` (2 decimals, about 1 km), `thumbnails` (REPORT-phase thumbnail paths), `createdAt`, `resolvedAt?` | everyone | the creator in the same batch as `tickets/{id}` (PUBLIC scope only); operators and participants afterwards |
| `ticketLocations/{id}` | `categoryId`, `situation`, `location` (GeoPoint, exact), `active` | signed-in active citizens; operators; never anonymous users | the creator in the same batch as `tickets/{id}`; `active` later by operators and participants |
| `stats/summary` | reserved for dashboard counters | decided in F-011 | decided in F-011 |
| Storage `tickets/{ticketId}/{attachmentId}.jpg\|mp4`, `tickets/{ticketId}/{attachmentId}_thumb.jpg` | - | everyone when `publicTickets/{ticketId}` exists; otherwise participants and operators | participants and operators (the F-013 upload worker) |
| Storage `users/{uid}/photo.jpg` | - | the owner; operators | the owner |

### Choices
- **`publicTickets` is a separate collection**, written only from `toAnonymousView()` in `data/`. Its rules allow only the listed keys, so author ids, descriptions and exact coordinates cannot be stored there.
- **`participantIds` (Q9)** = creator + followers, kept in sync with `followerIds` in the same write. One `whereArrayContains("participantIds", uid)` query lists "my tickets", and one rule (`request.auth.uid in participantIds`) guards the ticket, its messages and its media.
- **`ticketLocations` (Q8).** The 10 m grouping (Option A, [decision 0002](0002-grouped-reports-single-ticket.md)) needs exact locations of other people's tickets, but citizens cannot read other citizens' `tickets` and `publicTickets` has only coarse locations. `ticketLocations/{id}` holds the minimum for the check (category, situation, exact location, active), written in the same batch as the ticket. F-012 implements the check.
- **Following without reading.** A citizen follows a ticket found through `ticketLocations` with an `arrayUnion` update of `followerIds` and `participantIds`. The rule allows only adding their own uid, so the update works offline and needs no read of the ticket.
- **`statusHistory` and `attachments` are arrays in the ticket.** `currentStatus` and the history must change in one write ([decision 0004](0004-denormalised-current-status.md)), and a ticket has few entries. Each attachment has a `phase` field (`REPORT`/`RESOLUTION`).
- **`messages` is a subcollection.** A thread grows without limit, is read only in the detail and never by anonymous users.
- **Offline writes use batched writes, never transactions.** Reporting and following must work offline. Transactions are used only where the network is required anyway: the registration (NIF uniqueness) and the F-013 upload worker (network constraint).
- **Times stay `Long` epoch milliseconds**, not `Timestamp` (decision 0003): trivial mapping, rules compare numbers.
- **Coordinates are `GeoPoint`**, used in `data/` only.
- **Storage paths have no user ids.** Media of a public ticket are readable by anonymous users through cross-service rules (`firestore.exists` on `publicTickets/{ticketId}`); otherwise `firestore.get` on the ticket checks participants and operators.
- **`stats/summary` is reserved (Q11)**: counters written in the same batch as each ticket change are the likely answer for the anonymous dashboard, which cannot read private tickets. F-011 decides and adds its rules; until then the path is denied.
- **`Attachment.phase` in `model/` is not part of F-010 (Q10).** The Firestore field is fixed here; the `AttachmentPhase` enum is added by the first task that writes a RESOLUTION attachment (D-013), or by F-011 if Diogo agrees.

### Phase 1 risks
1. **Attachment phase:** the `phase` field on each attachment (model change per Q10).
2. **FCM token and role:** `fcmTokens` and `role` exist only in `users/{uid}`; `data/` maps `role` to `Citizen`/`Operator`.
3. **Public collection:** `publicTickets`, built only by `toAnonymousView()`.
4. **NIF uniqueness:** `nifs/{ROLE}_{nif}` created in the registration transaction; the `users` create rule checks it with `getAfter`. Registration is online.
5. **One municipality:** the single document `municipality/main`. The unmapped associations: `define` = every category belongs to it, `pertence a` = every operator belongs to it, `valida` = `validatedBy` on the user, `gere` = `StatusHistory.responsibleId`. An operator registration must use an email in `emailDomain` (rule).
6. **Reopen window:** no field; computed from the RESOLVED entry in `statusHistory` (decision 0004).

### Rejected
- Subcollections for `statusHistory` and `attachments`: a batch for every transition and extra reads for the detail.
- One `tickets` collection read with field filtering for anonymous users: rules cannot hide fields.
- Two queries (`creatorId ==` and `followerIds array-contains`) merged on the client instead of `participantIds`.
- For the 10 m grouping: grouping only against the citizen's own tickets (defeats Option A); letting any citizen read full tickets (exposes descriptions and author ids); a Cloud Function (conflicts with no-server notifications, decision 0011).
- A `{latitude, longitude}` map instead of `GeoPoint`: equally simple, but no type in the console.
- `Timestamp` for times: extra mapping, against decision 0003.

## Consequences
- Every report writes two or three documents in one batch (`tickets`, `ticketLocations`, and `publicTickets` for PUBLIC tickets). Every status change that affects anonymous users updates `publicTickets` too.
- `followerIds` and `participantIds` must always change together.
- The rules use `get`/`exists` per request (user role, ticket participants); Firestore allows 10 per single-document request and 20 per batch, enough for these rules.
- Signed-in active citizens can see exact locations, category and situation of every ticket through `ticketLocations`, but no author, description or media. Accepted for Option A.
- The lifecycle rule in the draft lets any participant change `currentStatus`, `statusHistory` and `attachments` (needed for reopening, the 5-day close on read and upload states). D-008, D-013 and F-013 tighten it per transition.
- The rules do not check that `coarseLocation` is really rounded; `toAnonymousView()` is the only writer by convention.
- The anonymous 10 km radius is not enforced by the rules (anonymous users have no location in the request); it stays a client check.
- The drafts are untested until the person runs them in the console Rules Playground or deploys them with OK (Q5).

# F-011 docs plan

## Docs to update
- **`docs/architecture/overview.md`**
  - "ViewModel (D-003)": replace the `LocalTicketsRepository` description with `FirestoreTicketsRepository` (flows per audience, sources per audience, client-side newest-first sort, no `refresh()`, `setUser`), and the "one empty frame" note (now: until the first cache snapshot).
  - "Report form (F-006)": categories now a live flow from `categories` (active only); `reportTicket` takes `creatorId`; returns once the batch is queued (no wait for the server).
  - "Media is outside the projection": the EXIF step now lives in `FirestoreTicketsRepository.reportTicket` (closes the "Firebase repository must keep this step" item).
  - "Firebase (F-010)": the container now has a reader (`ticketsRepository`), the `currentUser` stand-in, the debug seed and its removal before delivery.
  - New subsection "Firestore repository (F-011)": the read API, mapping file, the report batch (tickets + ticketLocations + publicTickets + stats increments), `stats/summary` counters and `statsIncrements`, failure modes (no `await()`, listener errors keep the last value), the class-material comparison (what is from 09_Storage/T8, what is beyond class and why).
  - "Handover (F-010)" or a new "Handover (F-011)":
    - D-009: replace `CityFixApplication.currentUser`/`reporterId` with the signed-in user, call `TicketsViewModel.setUser` on sign-in/out, hide "+" for anonymous users, map the role and relation to `DetailAudience` and `canExpandCards`.
    - D-013 and F-014: every status batch adds `statsIncrements(before, after)` merged into `stats/summary`, plus the `publicTickets` mirror and (closing) `ticketLocations.active = false` as 0010 says.
    - F-012: `ticketLocations` already exists for every ticket, with (0, 0) until GPS; write the real location in the create batch.
    - F-013: `storagePath` holds the local path while PENDING; replace it with the Storage path and fill `publicTickets.thumbnails`.
    - D-015: delete `data/DebugSeed.kt` and its call; decide whether to clean stand-in test data (`dev-citizen-1`, `citizen-1`).
- **`docs/tasks/phase-2/F-011-firestore-tickets-repository.md`**: status and ticked checks, only if the person wants task files updated (they are the person's files; ask).
- **`docs/report/notes.md`**: the report notes below.

## Decisions to record
- **Amend decision 0009** (repository): Firestore implementation, flow API replaces suspend loads, `LocalTicketsRepository` deleted (Q4), ViewModel still owns its `StateFlow`s, `refresh()` gone, `reportTicket` returns once queued; mark the "Callback risks" consequence as narrowed, the `saved` flag still open.
- **Amend decision 0010** (schema): `stats/summary` decided (four counters, `FieldValue.increment` in the same batch as every ticket change, read by everyone); `ticketLocations` written by F-011 at create; `storagePath` holds the local path while PENDING; rules block for `stats` drafted or not (Q7).
- **New decision 0013** (number to confirm with Diogo, Q8): "Firestore reads per audience and dashboard counters" - Q1 (stand-in user and seam), Q2 (`callbackFlow` vs callbacks, beyond-class reason), Q3 (counters from `statsIncrements`, alternatives rejected), Q5 (debug seed), Q6 (`ticketLocations` now), with the Gate 1 answers.
- **Decision 0006 / 0007**: no content change; add a one-line pointer in 0006's "Public projection contract" that `toPublicTicketDocument()` is the writer (Diogo's record: ask him, or leave the pointer in 0013 only).

## Report notes
- **Offline-first writes:** batched writes queued by the Firestore cache; why no transactions and no `await()` (a commit completes only when the server acknowledges); the form closes immediately and the snapshot listener shows the ticket from the local cache.
- **Privacy by collection, not by field:** anonymous code paths read only `publicTickets`, built by one function (`toAnonymousView()` -> `toPublicTicketDocument()`), 7 keys, coarse location; Firestore rules cannot hide fields.
- **Dashboard without a server:** counters updated in the same atomic batch as the ticket, increments computed as the difference of the existing pure stats rule, so the semantics of decision 0005 hold for private tickets that anonymous users cannot read. Limit: a write path that forgets the increment makes the counters drift.
- **Live data:** snapshot listeners wrapped in `callbackFlow`, collected in `viewModelScope`, so listeners are removed when the ViewModel is cleared and survive rotation.
- **No composite indexes:** single-field queries and client-side sorting.
- **Improvement opportunities:** deploy and test the security rules (decision 0012 Q5); a Cloud Function or scheduled job to recompute `stats/summary` and detect drift; surfacing server-rejected offline writes to the user; real authentication replacing the stand-in (D-009); thumbnails for the public list (F-013).
- **Class material vs. beyond class:** Firestore `set`/`get`/listeners-on-tasks and `batch()` from T8/09_Storage; Flow/StateFlow from 08; beyond class: `addSnapshotListener`, `callbackFlow`, `combine`, `FieldValue.increment`.

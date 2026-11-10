# F-010 docs plan

## Docs to update
- `docs/architecture/overview.md`:
  - **Layers / `CityFixApplication`:** it now also holds `auth`, `firestore` (persistent cache, settings applied once in `lazy`), `storage` and `messaging`. `ticketsRepository` is still local until F-011.
  - **External services:** link to decision 0010; the Firebase plan chosen at Gate 1 (Q1); rules drafted in the repo and their deploy status.
  - **Dependencies (App shell, D-006):** BoM 35.1.0 (auth 25.0.0, firestore 27.1.0, storage 23.0.0, messaging 26.0.1), google-services 4.5.0, coroutines play-services 1.11.0, or whatever slice 1 actually used if R1 or R2 forced a fallback. Add the resolved `debugRuntimeClasspath` diff and the lint warning count after F-010.
  - **Correct stale statements:**
    - "the merged manifest has neither `CAMERA` nor `INTERNET`" (Report attachments, F-007): `INTERNET` now comes from Firebase;
    - "no `coil-network-okhttp` (local files only, no `INTERNET`)": keep the reason for not adding okhttp, drop "no INTERNET".
  - **New "Setup" subsection:**
    - `app/google-services.json` is required to build (the plugin fails without it);
    - where it comes from (the person, shared privately per Q2);
    - `git check-ignore` to confirm it stays out of git;
    - the ZIP step (D-015 copies it in).
  - **Domain model, Code mapping:** the four unmapped associations now map to Firestore fields (0010): `define`, `pertence a`, `valida` (`validatedBy`), `gere` (`responsibleId`). "Moving `messages`, `statusHistory` and `attachments` to subcollections is a Firebase-phase decision" becomes: messages in a subcollection, the other two stay arrays (0010).
- `docs/tasks/phase-2/D-015-final-pass-delivery.md`: add a checklist line "copy `app/google-services.json` into the ZIP and build the unzipped copy" (if Q2 keeps the file ignored). This is Diogo's task file, so tell him; it is a one-line change.
- `docs/tasks/phase-2/README.md`: under "Decisions to take at Gate 1 of F-010", note the answers with a link to 0011 (or 0010).
- `CLAUDE.md`: **do not edit.** Suggest to the person adding "put `app/google-services.json` in place" next to the fresh-clone hook line under Workflow. Their call.

## Decisions to record
- **0010 Firestore schema** (written in slice 3, final wording after Gate 1):
  - the collections and Storage paths table;
  - arrays vs subcollections;
  - batches offline, transactions only online (NIF registration, upload worker);
  - `Long` times and enum names (decision 0003);
  - `GeoPoint` in `data/` only;
  - `publicTickets` built only by `toAnonymousView()`;
  - `participantIds` (Q9), `ticketLocations` (Q8), `stats/summary` reserved (Q11);
  - how each of the six phase 1 risks is closed;
  - status: accepted, with Diogo's agreement date.
- **0011 Firebase plan, notifications, 5-day close, maps** (if Q12 accepted): the four answers, with the Storage-needs-Blaze fact and its source, the cost guard (budget alert), and what each answer means for F-012, F-016, D-008, D-009 and D-014.
- **Decision 0009:** add an "Amended by F-010" line. `CityFixApplication` now also holds the Firebase instances; the manual-DI decision itself is unchanged.

## Report notes
Add to `docs/report/notes.md`:
- **Technical solution:** one Firebase container in `Application` (manual DI, as Contacts_#3). Firestore persistent cache set explicitly. Public projection as a separate collection, because Firestore rules secure documents, not fields.
- **Options taken:**
  - arrays for history and attachments, so `currentStatus` changes atomically (decision 0004);
  - a subcollection for messages;
  - NIF uniqueness through `nifs/{ROLE}_{nif}` in a transaction;
  - one `municipality/main` document;
  - Storage paths without user ids;
  - media access for anonymous users through cross-service Storage rules;
  - the four Gate decisions and why (cost, no server code to defend, the ZIP must build without keys).
- **Limits to state honestly:**
  - Blaze is needed only because of the 2024-2026 Storage pricing change;
  - notifications without a server work only while some client runs (if Q3 stays "no Functions");
  - the 5-day close is computed on read, not by a server;
  - the 10 km anonymous radius is checked on the client only;
  - `google-services.json` is handled outside git;
  - the rules were drafted on 2026-11-10 and deployed on <date> (fill in later).
- **Improvement opportunities:**
  - Cloud Functions for push on status change, the 24 h warning and the scheduled close;
  - App Check;
  - rules unit tests with the Firebase Emulator Suite;
  - geohash queries for the 10 m grouping instead of loading `ticketLocations`.

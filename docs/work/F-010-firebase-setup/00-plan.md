# F-010: Firebase setup and Firestore schema
Type: feature · Task file: docs/tasks/phase-2/F-010-firebase-setup.md
Labels: -

## Problem
The app uses only in-memory data (`LocalTicketsRepository`, decision 0009). Brief IV asks for Firebase Auth, Cloud Firestore with an offline cache and Storage, and brief III asks for FCM. Every other Firebase task (F-011..F-016, D-009..D-014) needs:
- the Firebase libraries in the build;
- one place that creates the Firebase instances;
- a shared data layout.

Phase 1 left six schema risks open (phase 1 README, "Risks to handle in the Firebase phase"):
1. attachment phase;
2. FCM token and `role`;
3. a public collection;
4. NIF uniqueness;
5. one municipality document;
6. the reopen window.

## Outcome
- The app builds with the Firebase BoM and the Google services plugin, and starts with a Firestore instance that keeps its persistent offline cache.
- `CityFixApplication` is the one place that creates `FirebaseAuth`, `FirebaseFirestore`, `FirebaseStorage` and `FirebaseMessaging`, like it creates `ticketsRepository` today.
- Decision 0010 fixes the Firestore and Storage layout, agreed with Diogo.
- `firestore.rules` and `storage.rules` drafts are in the repo. They are **not deployed** in this task.
- The four phase-2 Gate decisions (plan, notifications without a server, 5-day close, maps) are settled at this Gate and recorded.

Nothing changes for the user. The screens still read `LocalTicketsRepository`. Replacing it is F-011.

## Acceptance checks
- [ ] With `app/google-services.json` in place, the app starts on the emulator and the temporary smoke code writes a test document and reads it back (Logcat). The smoke code is **not committed**, and the test document is deleted in the console afterwards.
- [ ] Firestore offline persistence is on: `firestoreSettings.cacheSettings` is a `PersistentCacheSettings` (logged during the smoke run), and in airplane mode a second run reads the test document from the cache (`metadata.isFromCache == true`).
- [ ] `docs/decisions/0010-firestore-schema.md` exists and Diogo agreed to it (date recorded in `state.md`).
- [ ] `git check-ignore -v app/google-services.json` matches the `.gitignore` line. No commit adds `google-services.json`, keys or the smoke code (`git show --stat` for each F-010 commit).
- [ ] `./gradlew assembleDebug` and `./gradlew lint` pass after each slice. Lint adds no new errors, and every new warning is listed in `state.md`.

## Approach

### Console (the person, not Claude)
- Create the Firebase project and add the Android app `pt.isec.amov.cityfix` (the debug SHA-1 is needed later for phone MFA).
- Enable Email/Password Auth, Firestore (region `europe-west`, see Q5 for the start mode), Storage and Cloud Messaging.
- Download `google-services.json` into `app/`.
- Claude does not open, edit or commit that file.
- **Cloud Storage needs the Blaze plan.** Since 30 Oct 2024, creating a default bucket requires Blaze. Since 3 Feb 2026, Spark projects lose access to every bucket (Firebase FAQ "storage changes announced Sept 2024"). Decision 1 is therefore close to forced: see Q1.

### Build (slice 1)
- Catalog entries: the `google-services` plugin, `firebase-bom` and the four libraries without versions (`firebase-auth`, `firebase-firestore`, `firebase-storage`, `firebase-messaging`), plus `kotlinx-coroutines-play-services` for `Task.await()`.
- Since BoM 34 the Kotlin extensions (`Firebase.firestore`, `firestoreSettings { }`) live in the main artifacts, so no `-ktx` artifacts are needed.
- Versions checked on 2026-11-10:

  | Artifact | Version | Source |
  | --- | --- | --- |
  | `com.google.firebase:firebase-bom` | **35.1.0** (21 Oct 2026; auth 25.0.0, firestore 27.1.0, storage 23.0.0, messaging 26.0.1) | Google Maven `maven-metadata.xml` + Firebase Android release notes |
  | `com.google.gms.google-services` | **4.5.0** | Google Maven `maven-metadata.xml` + Firebase "Add Firebase to your Android project" page |
  | `org.jetbrains.kotlinx:kotlinx-coroutines-play-services` | **1.11.0** | Maven Central `maven-metadata.xml` |

- BoM 35.0.0 raised minSdk to 24, which matches ours.
- Neither page says anything about AGP 9 or built-in Kotlin, so slice 1 checks the build on AGP 9.4.1 itself (see Risks).
- **The plugin needs the file.** Once `com.google.gms.google-services` is applied, `processDebugGoogleServices` fails if `app/google-services.json` is missing. From slice 1 on, every build (yours, Diogo's after a merge, and the grader's from the ZIP) needs the file.
- The plugin is applied without a condition. Rejected: applying it only `if (file("google-services.json").exists())`. The build would pass, but the app would crash at start with "FirebaseApp is not initialized", which is harder to diagnose than a clear build error.

### App container (slice 2)
- `CityFixApplication` gains four `by lazy` properties: `auth`, `firestore`, `storage` and `messaging`.
- `firestore` applies `firestoreSettings { setLocalCacheSettings(persistentCacheSettings { }) }` before returning the instance. Settings must be set before the first use, and `lazy` makes this the only path to the instance.
- Persistent caching is the Android default, but the explicit setting documents the brief IV requirement and gives a place to set the cache size.
- Rejected: a separate `AppContainer` class or Hilt/Koin. Decision 0009 chose manual DI in the `Application`, as Contacts_#3 does. Four properties do not justify a new class.
- `ticketsRepository` stays `LocalTicketsRepository` (F-011 swaps it).
- Smoke test: temporary code (in `CityFixApplication.onCreate` with `ProcessLifecycleOwner`, or in `MainActivity` inside a `lifecycleScope.launch`) writes `debug/f010-smoke` with `set(...).await()`, reads it back with `get().await()`, and logs the value, `metadata.isFromCache` and `firestoreSettings.cacheSettings`. It is run, its result goes in `state.md`, and then it is reverted before the commit.

### Schema (decision 0010, to agree with Diogo before Gate 1)
Model types stay as decision 0003 defines them: pure Kotlin, `Long` times, enum names as strings. All mapping lives in `data/`. Firestore-only fields (`role`, `fcmTokens`, `participantIds`, the public projection) are not added to `model/`.

| Path | Fields | Who reads | Who writes |
| --- | --- | --- | --- |
| `users/{uid}` | `role` (`CITIZEN`/`OPERATOR`), `name`, `nif`, `email`, `phone`, `photoPath?`, `createdAt`; citizen only: `georeferencing` (GeoPoint), `registrationStatus` (`PENDING`/`ACTIVE`), `impactDetection`, `validatedBy?`, `validatedAt?`; both: `fcmTokens` (array) | the owner; operators | the owner at registration and for editable fields; operators set `registrationStatus`, `validatedBy` and `validatedAt` |
| `nifs/{ROLE}_{nif}` | `uid` | signed-in users (`get` only, to check uniqueness) | created once, in the registration transaction together with `users/{uid}`; never updated |
| `municipality/main` | `name`, `emailDomain`, `center` (GeoPoint, Pr. Republica), `radiusKm` (10.0) | everyone, anonymous included | console only |
| `categories/{id}` | `name`, `active` | everyone | operators |
| `tickets/{id}` | `description`, `situation`, `scope`, `location` (GeoPoint), `currentStatus`, `createdAt`, `categoryId`, `categoryName`, `creatorId`, `followerIds`, `participantIds` (creator + followers, see Q8), `statusHistory` (array of `{status, dateTime, justification?, responsibleId}`), `attachments` (array of `{id, type, phase (REPORT/RESOLUTION), storagePath, thumbnailPath?, uploadState, uploadedById}`) | participants; operators | creator at creation (batch with `publicTickets`); a citizen may only add their own uid to `followerIds`/`participantIds` (Option A follow); operators for lifecycle changes |
| `tickets/{id}/messages/{id}` | `text`, `sentAt`, `authorId` | participants; operators | participants; operators (author = self) |
| `publicTickets/{id}` | `categoryId`, `categoryName`, `situation`, `currentStatus`, `coarseLocation` (rounded to 2 decimals, about 1 km, as `toAnonymousView()`), `thumbnails` (REPORT-phase thumbnail paths), `createdAt`, `resolvedAt?` (for the average repair time) | everyone | the creator in the same batch as `tickets/{id}` (PUBLIC scope only); operators afterwards |
| Storage `tickets/{ticketId}/{attachmentId}.jpg\|mp4`, `tickets/{ticketId}/{attachmentId}_thumb.jpg` | - | thumbnails and media: everyone when `publicTickets/{ticketId}` exists (cross-service `firestore.exists` in Storage rules); otherwise participants and operators | the uploader (WorkManager, F-013) |
| Storage `users/{uid}/photo.jpg` | - | the owner; operators | the owner |

How each phase 1 risk is handled:
- **1, attachment phase:** a `phase` field on each attachment. The model change (`AttachmentPhase` enum) is a separate decision, see Q10.
- **2, FCM token and role:** `fcmTokens` and `role` exist only in the `users` document. The data layer maps `role` to `Citizen`/`Operator`.
- **3, public collection:** `publicTickets`, built only by `toAnonymousView()`.
- **4, NIF uniqueness:** `nifs/{ROLE}_{nif}` created in a transaction. Registration is online, and transactions do not run offline.
- **5, one municipality:** the single document `municipality/main`. The four unmapped associations are covered as follows: `define` = all categories belong to it, `pertence a` = every operator belongs to it, `valida` = `validatedBy` on the user, `gere` = `StatusHistory.responsibleId`.
- **6, reopen window:** no field. It is computed from the RESOLVED entry in `statusHistory`, as decision 0004 already says.

Choices and rejected alternatives (all go into 0010):
- **`statusHistory` and `attachments` as arrays in the ticket.** `currentStatus` and the history must change in one write (decision 0004), and a ticket has few entries. Rejected: subcollections, which need a batch for every transition and extra reads for the detail.
- **`messages` as a subcollection.** A thread grows without limit, is read only in the detail, and is never shown to anonymous users.
- **Offline writes use batched writes, never transactions.** Transactions fail without a network. `reportTicket` and following a ticket must work offline (brief IV, decision 0002). The F-013 upload worker runs with a network constraint, so it may use a transaction to update one array element.
- **Times stay `Long` epoch milliseconds** in Firestore (decision 0003), not `Timestamp`. Mapping stays trivial, and rules compare numbers.
- **Coordinates as `GeoPoint`** (a Firebase type, used in `data/` only). Rejected: a `{latitude, longitude}` map, which is equally simple but has no type in the console.

### Rules drafts (slice 3)
- `firestore.rules`:
  - anonymous read only on `publicTickets`, `categories` and `municipality`;
  - signed-in users read their own `users` document;
  - operators (`get(users/$(uid)).data.role == 'OPERATOR'`) read all users and tickets;
  - citizens read tickets where they are in `participantIds`;
  - the follow update may change only `followerIds`/`participantIds`, and only by adding `request.auth.uid`;
  - `nifs` docs are create-only;
  - an operator registration must use an email in `municipality/main.emailDomain`.
- `storage.rules`: the paths above. Media read uses `firestore.exists(/databases/(default)/documents/publicTickets/$(ticketId))`.
- `firebase.json` points the CLI at the two files, with no project id and no `.firebaserc`, so a later `firebase deploy --only firestore:rules,storage` is one command. **Deploying needs the person's OK** (CLAUDE.md).
- The person can check the drafts in the console Rules Playground without publishing.

## Architecture impact
- New dependencies:
  - the Google services Gradle plugin (root `apply false`, applied in `:app`);
  - the Firebase BoM with Auth, Firestore, Storage and Messaging;
  - coroutines play-services.
- Firebase merges `INTERNET` and `ACCESS_NETWORK_STATE` into the manifest. The overview's "no `INTERNET`" statement (F-007) stops being true.
- `firebase-messaging` turns on FCM auto-init: the device gets a token at first start. Nothing stores it until D-009/F-016.
- `CityFixApplication` becomes the Firebase container (decision 0009 pattern). D-009's `AuthRepository` and F-011's `FirestoreTicketsRepository` receive the instances from it.
- New repo-root files: `firestore.rules`, `storage.rules`, `firebase.json`.
- New decision record 0010, plus 0011 for the four Gate decisions (see 03-docs-plan).
- No change to `model/`, the ViewModels, the screens or navigation.

## Failure modes
- `google-services.json` missing (fresh clone, Diogo before he gets the file, a ZIP built from a clean checkout): the build fails at `processDebugGoogleServices`. Expected and documented. See Q2 for how the file reaches Diogo and the ZIP.
- Wrong file (another package name): the plugin fails with "No matching client found for package name". Fix in the console.
- Firestore created in production mode before the rules are deployed: the smoke write fails with `PERMISSION_DENIED`. See Q5.
- Settings applied after first use: `IllegalStateException`. Prevented because the `lazy` block is the only path to the instance.
- Storage on Spark: 402/403 on every call (from F-013 on). See Q1.
- A smoke test left in a commit: caught by the acceptance check (`git show --stat`, and `git diff --cached` checked for `f010-smoke` before every commit).

## Risks and open questions

### Risks
- **R1, AGP 9.4.1 with google-services 4.5.0.** Neither the plugin notes nor the setup page mention AGP 9. Older 4.4.x releases used the legacy variant API. If slice 1 fails on AGP 9, try the newest 4.4.x that builds and record it. Do not downgrade AGP.
- **R2, Kotlin 2.2.10 compiler with BoM 35.1.0 and coroutines 1.11.0.** These libraries may be compiled with a newer Kotlin. The 2.2 compiler reads metadata up to 2.3 (`kotlin-stdlib` 2.3.20 already resolves today). If the compiler reports "compiled with an incompatible version of Kotlin", step down to the newest BoM 34.x or coroutines 1.10.2 and record why. Do not raise Kotlin (Compose limits in the overview).
- **R3, dependency drift.** Firebase may raise `kotlinx-coroutines-core`, `androidx.core` or lifecycle transitively. Slice 1 compares `:app:dependencies --configuration debugRuntimeClasspath` before and after and records the changes (as D-006 did).
- **R4, the schema blocks Diogo.** D-009 and D-010 read and write `users` and `nifs`. A change after Gate 1 is a cross-branch change. Hence: **agree 0010 with Diogo before Gate 1.**
- **R5, cost.** Blaze needs a billing account. Mitigation: a budget alert at a small amount, staying inside the no-cost quotas. No Cloud Functions unless Q3 says so.
- **R6, privacy.** The brief PDF could not be rendered in this session (no `pdftoppm`). Brief III and IV were taken from the task files' summaries. Re-read sections III and IV before Gate 1.

### Open questions (recommended default in **bold**)
- **Q1, Firebase plan (phase-2 decision 1).** **Blaze with a budget alert (for example 5 EUR).** Storage, which the brief requires, has no access on Spark since Feb 2026. Blaze also allows Identity Platform MFA (SMS, or TOTP) and Maps if chosen. Stay within the free quotas. Spark would mean no Storage at all, so photos and videos could not be shared.
- **Q2, `google-services.json` to Diogo and into the ZIP.** **Keep it ignored (as now). Share it with Diogo privately (not in git). D-015 copies it into the ZIP by hand, and that step is added to D-015's checklist, because a clean checkout of `main` will not have it.** Firebase says the file is not a secret, so committing it is acceptable if the GitHub repo is private: confirm the repo visibility. If the repo is public, it must stay ignored (CLAUDE.md).
- **Q3, notifications without a server (phase-2 decision 2).** **No Cloud Functions.** Each device listens to Firestore for the tickets it takes part in and shows local notifications, plus a WorkManager periodic check when the app is closed. FCM tokens are stored in `users/{uid}.fcmTokens` and remote reception is shown with a console test message. The report explains the limit. With Blaze from Q1, one small Cloud Function (status change -> FCM) becomes possible, but it adds JS/TS code that both authors must defend. Leave that as an F-016 Gate option if time remains.
- **Q4, closing after 5 days (phase-2 decision 3).** **Computed on read**: a RESOLVED ticket whose RESOLVED entry is more than 5 days old counts as CLOSED and is locked (a pure rule in `model/`, D-008). The first client that sees it (operator panel, citizen list, or the F-016 worker) writes the CLOSED transition in a batch. No Cloud Function.
- **Q5, Firestore start mode and when to deploy the rules.** **Create the database in test mode with an expiry of 2026-11-21 for development and the smoke test. Deploy the drafted rules, with the person's OK, once D-009 login works (target 11-14) and at the latest before the ZIP.** Production mode from day one would block F-011 until Auth exists.
- **Q6, maps (phase-2 decision 4).** **osmdroid**: no key, so the ZIP builds and the map renders for the grader. Requirements (overview "Shared components"): `INTERNET`, a User-Agent, OSM attribution, coarse zoom with gestures off. Google `maps-compose` is the class example, but its key would live in `local.properties`, which the ZIP must exclude, so the grader's build would show a blank map. Note that osmdroid's repository was archived in Nov 2024 (still works; a report note).
- **Q7, MFA factor (needed by D-009, depends on Q1).** **Identity Platform with SMS MFA, using console test phone numbers with fixed codes for development and the defence (no SMS cost).** TOTP is the alternative (also supported on Android, needs an authenticator app). Diogo confirms at D-009's Gate.
- **Q8, who can see exact locations for the 10 m grouping.** `publicTickets` holds only coarse locations, and citizens cannot read other citizens' `tickets`, so a client cannot run the 10 m check against other people's tickets. **Add a `ticketLocations/{id}` collection (`categoryId`, `situation`, `location`, `active`), readable by signed-in active citizens and operators, never by anonymous users, written in the same batch as the ticket.** Alternatives:
  - limit grouping to the citizen's own tickets, which defeats Option A;
  - let any signed-in citizen read full tickets, which exposes descriptions and author ids;
  - a Cloud Function, which conflicts with Q3.

  This decision is needed now because it shapes the schema. F-012 implements it.
- **Q9, `participantIds`.** **Keep it** (creator + followers): one `whereArrayContains("participantIds", uid)` query for "my tickets", and one rule. The cost is one duplicated field, kept in sync with `followerIds` in the same write. Rejected: two queries (`creatorId ==` and `followerIds array-contains`) merged on the client.
- **Q10, the `Attachment.phase` model change.** **Not in F-010.** 0010 fixes the Firestore field. The `AttachmentPhase` enum in `model/` (Diogo's D-001 files) is added by whichever task first writes a RESOLUTION attachment (D-013), or by F-011 if Diogo agrees.
- **Q11, dashboard stats for anonymous users** (they cannot read private tickets). **Decide in F-011**, but 0010 reserves `stats/summary` (counters written in the same batch as each ticket change) as the likely answer.
- **Q12, where the four Gate decisions are recorded.** **A separate decision 0011 ("Firebase plan, notifications, 5-day close, maps")**, so 0010 stays about the data layout.

## Out of scope
- Replacing `LocalTicketsRepository` (F-011).
- Auth screens, MFA, registration (D-009, D-010).
- Uploads (F-013).
- Notifications and storing FCM tokens (F-016).
- Map library (F-012).
- Deploying rules.
- Seeding categories and `municipality/main` (F-011 / console).
- Model changes.

## Slices
1. **Build:** catalog entries, root `apply false`, `:app` plugin and dependencies. Needs `app/google-services.json` locally.
2. **App container:** Firebase instances in `CityFixApplication` with the persistent Firestore cache. The smoke test runs before the commit and is reverted.
3. **Rules drafts and decision 0010:** `firestore.rules`, `storage.rules`, `firebase.json` and `docs/decisions/0010-firestore-schema.md` (plus 0011 if Q12 is accepted). No deploy.

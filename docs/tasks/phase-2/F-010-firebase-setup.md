# F-010: Firebase setup and Firestore schema

Owner: Francisco (`dev/Francisco`) · Layer: build / `data` · Phase 2 · Status: to do · Target: 2026-11-11
Depends on: - · Blocks: every task that uses Firebase. Agree the schema with Diogo before Gate 1.

## Goal
Connect the app to a Firebase project and fix the data layout both of you build on, so the other Firebase tasks can start in parallel.

## Why
- Brief IV: Auth, Cloud Firestore with offline cache, Storage; brief III: FCM.
- Phase 1 risks 2-5 (role field, public collection, NIF uniqueness, one municipality document) are schema questions.

## What to build
- **Console (done by the person, not Claude):** create the project, add the Android app `pt.isec.amov.cityfix`, enable Email/Password Auth, Firestore, Storage, Cloud Messaging; download `google-services.json` into `app/`. CLAUDE.md: Claude asks before touching it and it is never committed if the repo is public (decide: `.gitignore` it and share it privately, or commit it; the ZIP must contain it to build).
- **Build:** Google services plugin, Firebase BoM, `firebase-auth`, `firebase-firestore`, `firebase-storage`, `firebase-messaging`, `kotlinx-coroutines-play-services` (`await()`).
- **`CityFixApplication`:** Firestore with persistent offline cache (brief IV), one place that creates the Firebase instances (simple app container, as `LocalTicketsRepository` is created today).
- **Schema decision record** `docs/decisions/0010-firestore-schema.md`, for example:
  - `users/{uid}` with `role` (`CITIZEN`/`OPERATOR`), profile fields, `registrationStatus`, `impactDetection`, `fcmTokens`.
  - `nifs/{role}_{nif}` → `{uid}`, created in the same transaction as the user (NIF unique per role).
  - `categories/{id}`, `municipality/main` (centre Pr. Republica, radius 10 km, operator email domain).
  - `tickets/{id}` (full ticket, `currentStatus`, `statusHistory`, `attachments` with a `phase` field: REPORT or RESOLUTION), `tickets/{id}/messages/{id}`.
  - `publicTickets/{id}`: only what anonymous users may see (category, situation, status, coarse location, thumbnails, dates for stats).
  - Storage: `tickets/{ticketId}/{attachmentId}.jpg|mp4` and `..._thumb.jpg` (no user ids in paths).
- **Security rules draft** in the repo (`firestore.rules`, `storage.rules`): anonymous read only on `publicTickets`, `categories`, `municipality`; citizens read their own tickets; operators read all. Deploying them needs the person's OK (CLAUDE.md).
- Gate 1 also settles the four decisions listed in `phase-2/README.md` (plan, notifications without a server, 5-day close, maps).

## Files
`build.gradle.kts`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `CityFixApplication.kt`, `firestore.rules`, `storage.rules`, `docs/decisions/0010-*.md`, `docs/architecture/overview.md`.

## Acceptance checks
- [ ] The app starts and writes/reads a test document (removed before commit).
- [ ] Firestore offline persistence is on.
- [ ] Decision 0010 exists and Diogo agreed to it.
- [ ] No secrets committed by accident; `./gradlew assembleDebug` and `lint` pass.

## Out of scope
Replacing the local repository (F-011), Auth screens (D-009).

## References
Brief III, IV; phase 1 README "Risks to handle in the Firebase phase"; decisions 0003, 0004, 0009.

# Phase 2: Firebase, roles and delivery

Goal: everything the brief asks for that phase 1 left out (see phase 1 "Left for later phases"). Delivery: **2026-11-22 23:59**. Every task below has a target date; keep **2026-11-21** free for the final build and the ZIP.

## How the work is split
- **Francisco:** the cloud base, the tickets data, the citizen-side features, media and notifications.
- **Diogo:** the ticket lifecycle rules, authentication and registration, profiles, everything the operator does, and the final pass.
- Each person owns their files. Shared files and who owns them:
  - `MainActivity.kt`, `ui/navigation/*` → Diogo. Francisco adds routes in his tasks; tell Diogo, keep the change small, merge `origin/main` the same day.
  - `data/TicketsRepository.kt` and its Firestore implementation → Francisco. D-013 adds the operator methods: agree the signatures with Francisco first.
  - `data/UsersRepository.kt`, `data/AuthRepository.kt` → Diogo.
  - `app/build.gradle.kts`, `gradle/libs.versions.toml` → whoever needs a library adds it; merge often, these conflict easily.
- **F-010 comes first:** nothing that touches Firebase can start until it is pushed. Diogo starts with D-007 and D-008 meanwhile (no Firebase needed).

## Tasks
| ID | Owner | Layer | Task | Depends on | Target |
| --- | --- | --- | --- | --- | --- |
| D-007 | Diogo | model / ui | Anonymous projection and expand toggle (phase 1, still open) | - | 11-10 |
| D-008 | Diogo | model | Ticket lifecycle rules (pure) | - | 11-10 |
| F-010 | Francisco | build / data | Firebase setup and Firestore schema | - | 11-11 |
| F-011 | Francisco | data / viewmodel | Firestore tickets repository and public projection | F-010, D-007 | 11-13 |
| D-009 | Diogo | data / ui / navigation | Login, MFA, password recovery and role navigation | F-010 | 11-13 |
| D-010 | Diogo | data / ui | Registration (citizen and operator) | D-009 | 11-14 |
| F-012 | Francisco | utils / ui / data | Location, permissions, 10 km gate, 10 m grouping and map | F-011 | 11-14 |
| F-013 | Francisco | data / utils | Image compression, thumbnails and WorkManager uploads | F-011 | 11-15 |
| D-011 | Diogo | ui / viewmodel | Profile editing | D-010 | 11-15 |
| D-012 | Diogo | ui / data | Operator: citizen approval and category management | D-010, F-011 | 11-16 |
| F-014 | Francisco | ui / viewmodel / data | Citizen: my tickets, message thread and reopening | F-011, D-008, D-009 | 11-17 |
| D-013 | Diogo | ui / viewmodel / data | Operator: operations panel and ticket lifecycle | D-008, F-011, F-013 | 11-18 |
| F-015 | Francisco | utils / ui | Impact detection (accelerometer) | D-010, F-012 | 11-18 |
| F-016 | Francisco | data / utils | Notifications (local and FCM) | F-011, D-009 | 11-19 |
| D-014 | Diogo | utils / data | Operator 20 m geofence and proximity alert | D-013, F-012, F-016 | 11-20 |
| F-017 | Francisco | docs | Technical report (PDF) | all coding tasks | 11-21 |
| D-015 | Diogo | all | Final pass: landscape, accessibility, strings, delivery ZIP | all coding tasks | 11-21 |

8 tasks each (Diogo also finishes D-007). The heavy ones: F-011, F-013, F-016 for Francisco; D-009, D-013, D-014 for Diogo.

## Suggested order
1. **Day 1 (11-10):** Francisco does F-010 (the person creates the Firebase project in the console; Claude asks before touching `google-services.json`). Diogo does D-007 and D-008.
2. **11-11 to 11-14:** F-011 then F-012 in parallel with D-009 then D-010.
3. **11-15 to 11-18:** F-013, F-014, F-015 in parallel with D-011, D-012, D-013.
4. **11-19 to 11-20:** F-016 then D-014 (needs F-016's notification helper).
5. **11-21:** F-017 and D-015 together; build the ZIP from a clean checkout of `main`.

Merge `origin/main` into your dev branch at least once a day in this phase, and merge your dev branch into `main` after each task (ask Claude to do it).

## Decisions to take at Gate 1 of F-010 (both of you)
1. **Firebase plan (Spark or Blaze).** Phone/SMS MFA needs Firebase Auth upgraded to Identity Platform, and SMS needs billing. Cloud Functions (server-side push on status change, geofence and the 24 h warning) need Blaze. Check the current Firebase pricing pages before deciding.
2. **Notifications without a server (if Spark):** each device listens to Firestore for its own tickets and shows a local notification, with a WorkManager periodic check when the app is closed. The report must explain this limit.
3. **Closing after 5 days:** computed on read (a RESOLVED ticket older than 5 days counts as CLOSED and is locked) plus a write when a user or worker sees it, or a Cloud Function on Blaze.
4. **Maps:** the class example uses Google `maps-compose` (needs a Maps API key: ask before adding keys) or OSMDroid (no key).

**Answers (F-010 Gate 1, 2026-11-10, both authors; [decision 0012](../../decisions/0012-firebase-plan-notifications-close-maps.md)):**
1. **Blaze with a budget alert** (for example 5 EUR), inside the no-cost quotas. Spark is not possible: Cloud Storage needs Blaze (default buckets since 2024-10-30, no Spark bucket access since 2026-02-03).
2. **Notifications without a server**, even on Blaze: no Cloud Functions. Firestore listeners on `participantIds` + local notifications + a WorkManager periodic check (F-016). They arrive only while some client runs; the report states it. One small Function stays an F-016 Gate option if time remains.
3. **5-day close computed on read** from the server-set `resolvedAt` (D-008's pure rule); the first client that sees it writes CLOSED. The rules allow CLOSED only after 5 days of server time and lock the ticket. The 24 h warning is a local notification (F-016).
4. **osmdroid**, no key, so the ZIP builds and the map renders for the grader.

Also decided at that Gate (decision 0012): Firestore in test mode until 2026-12-10, rules drafted but not deployed (amended 2026-11-13: school project, test mode covers delivery and defence); SMS MFA with console test numbers; `app/google-services.json` stays out of git and is shared privately. Data layout: [decision 0010](../../decisions/0010-firestore-schema.md). Items D-008, D-009/D-010, F-012 and F-013 must follow: "Handover (F-010)" in `docs/architecture/overview.md`.

## Risks carried from phase 1 (see phase 1 README)
Attachment phase (report vs resolution proof), FCM token on `User`, `role` field in Firestore, public collection for anonymous users, NIF uniqueness by transaction, one municipality document, reopening computed from the RESOLVED history entry. All six are closed in [decision 0010](../../decisions/0010-firestore-schema.md) ("Phase 1 risks"); the reopen window now uses the server-set `resolvedAt` instead of the history entry.

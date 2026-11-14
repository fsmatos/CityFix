# 0012: Firebase plan, notifications, 5-day close and maps

Status: accepted (F-010, Gate 1 on 2026-11-10, both authors; Q5 amended 2026-11-13; Q6 amended by F-012 on 2026-11-14, Google Maps Compose instead of osmdroid). These are the four decisions the phase-2 README asked for at Gate 1 of F-010 (Q1, Q3, Q4, Q6), plus the start mode and rules deploy (Q5) and the MFA factor (Q7) that depend on them. The data layout is in [decision 0010](0010-firestore-schema.md).

## Context
- The brief requires Firebase Auth with MFA, Firestore with offline cache, Storage for photos and videos, and FCM.
- Cloud Storage requires the Blaze plan: creating a default bucket needs Blaze since 30 Oct 2024, and Spark projects lost access to every bucket on 3 Feb 2026 (Firebase storage FAQ, "storage changes announced Sept 2024"; checked 2026-11-10).
- Both authors must explain every file at the defence. Server code (Cloud Functions in JS/TS) adds a second language and a deploy pipeline.
- The delivery ZIP excludes `local.properties`, so a key kept there is missing in the grader's build.

## Decision
1. **Plan: Blaze with a budget alert** (for example 5 EUR), staying inside the no-cost quotas (Q1). Storage is not possible on Spark. Blaze also allows Identity Platform MFA.
2. **Notifications without a server: no Cloud Functions** (Q3). Each device listens to Firestore for the tickets it takes part in (`participantIds`) and shows local notifications, plus a WorkManager periodic check when the app is closed. FCM tokens are stored in `users/{uid}.fcmTokens`; remote reception is shown with a console test message. One small Cloud Function (status change -> FCM) stays an F-016 Gate option if time remains.
3. **5-day close: computed on read** (Q4). A RESOLVED ticket whose `resolvedAt` (server timestamp, decision 0010) is more than 5 days old counts as CLOSED and is locked (a pure rule in `model/`, D-008). The first client that sees it (operator panel, citizen list or the F-016 worker) writes D-008's `closeIfExpired` result in a batch: one CLOSED entry by `"system"`, dated 5 days after the RESOLVED entry ([decision 0011](0011-ticket-lifecycle-rules.md)). The rules allow CLOSED only after the 5 days (server time) and lock the ticket afterwards ([decision 0010](0010-firestore-schema.md)).
   - **24 h warning (brief III):** the citizens of a RESOLVED ticket (`participantIds`) get a notification when 24 hours remain before it closes, that is 4 days after the ticket's `resolvedAt` (server timestamp, [decision 0010](0010-firestore-schema.md)). Without a server it is a local notification, scheduled by the device from `resolvedAt` (the Firestore listener and the F-016 periodic worker); it is skipped if the ticket was reopened meanwhile.
4. **Maps: osmdroid** (Q6). No key, so the ZIP builds and the map renders for the grader. Requirements: `INTERNET`, a User-Agent, OSM attribution, coarse zoom with gestures off for anonymous users.
   - **Amended 2026-11-14 (Francisco, F-012, [decision 0014](0014-location-gate-grouping-map.md)): Google Maps Compose replaces osmdroid.** `com.google.maps.android:maps-compose` 6.1.0, the library and version of the class practical Contacts_#3 / P10. The Maps API key goes in `AndroidManifest.xml` as the `com.google.android.geo.API_KEY` meta-data, as in class, so it is committed and travels in the ZIP (not `local.properties`, which the ZIP excludes). A placeholder value is committed until the person pastes the real key. This overrides the CLAUDE.md "never commit secrets" rule for this key, by the person's explicit choice. Consequences: the grader sees the map only if the real key is in the ZIP (D-015); the key should get an API restriction (Maps SDK for Android) but **no** Android-app (package + SHA-1) restriction, because the grader's debug build has a different SHA-1 and the map would be blank. Coarse look kept: far zoom, gestures off, a 750 m circle, no marker. osmdroid was added in F-012 slice 1 and removed again before the map slice.
5. **Firestore start mode: test mode** (Q5; the console's default rules expire on 2026-12-10, 30 days after creation, not 2026-11-21 as planned) for development. **Amended 2026-11-13 (Francisco):** the drafted rules are kept in the repo as written and are **not deployed**; the database stays in test mode through delivery (11-22) and the defence (23-27 Nov), which ends before the 2026-12-10 expiry. Reason: a school project that will not go to production, and the class material covers basic Firestore only, not security rules. The rules files document how the brief's privacy and locking requirements would be enforced on the server. Rejected: simplifying the rules (keeps the full design for the report and defence) and deploying them (testing and debugging time before the deadline).
6. **MFA: Identity Platform with SMS** (Q7), using console test phone numbers with fixed codes for development and the defence (no SMS cost). Diogo confirms at D-009's Gate.
7. **`app/google-services.json` stays ignored** (Q2) and is shared with Diogo privately. D-015 copies it into the ZIP by hand, because a clean checkout does not have it.

### Rejected
- Spark: no Storage, so no photos or videos.
- Cloud Functions for push, the 24 h warning and the scheduled close: JS/TS code both authors must defend, and a deploy step.
- A scheduled server close: needs Functions.
- Google `maps-compose` (the class example): its key would live in `local.properties`, which the ZIP excludes, so the grader would see a blank map. (Reversed by F-012 on 2026-11-14: chosen, with the key in the manifest as in class; see point 4.)
- Production mode from day one: blocks F-011 until Auth exists.
- TOTP MFA: also supported, but needs an authenticator app at the defence.

## Consequences
- A billing account is needed; the budget alert is the cost guard.
- **F-016:** notifications arrive only while some client runs (the app or its worker). The report states this limit. F-016 also owns the 24 h warning before the close (point 3).
- **D-008:** owns the pure "counts as CLOSED" rule; every list and detail uses it, so a stale RESOLVED ticket never looks open.
- **F-012 and D-014:** use osmdroid. The osmdroid repository was archived in Nov 2024; it still works (a report note). (Superseded by F-012: both use Google Maps Compose through `ui/components/TicketMap`; D-013 calls `TicketMap(exact = true)`.)
- **D-015 (since F-012):** the ZIP must contain the real Maps key in `AndroidManifest.xml`, not the placeholder.
- **D-009:** SMS MFA needs the debug SHA-1 in the console and test numbers.
- **D-015:** the ZIP checklist gains "copy `app/google-services.json` and build the unzipped copy".
- Until the rules are deployed, the test-mode database is open to anyone with the project config; the expiry limits it.

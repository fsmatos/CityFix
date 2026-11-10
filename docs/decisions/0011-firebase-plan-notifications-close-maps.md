# 0011: Firebase plan, notifications, 5-day close and maps

Status: accepted (F-010, Gate 1 on 2026-11-10, both authors). These are the four decisions the phase-2 README asked for at Gate 1 of F-010 (Q1, Q3, Q4, Q6), plus the start mode and rules deploy (Q5) and the MFA factor (Q7) that depend on them. The data layout is in [decision 0010](0010-firestore-schema.md).

## Context
- The brief requires Firebase Auth with MFA, Firestore with offline cache, Storage for photos and videos, and FCM.
- Cloud Storage requires the Blaze plan: creating a default bucket needs Blaze since 30 Oct 2024, and Spark projects lost access to every bucket on 3 Feb 2026 (Firebase storage FAQ, "storage changes announced Sept 2024"; checked 2026-11-10).
- Both authors must explain every file at the defence. Server code (Cloud Functions in JS/TS) adds a second language and a deploy pipeline.
- The delivery ZIP excludes `local.properties`, so a key kept there is missing in the grader's build.

## Decision
1. **Plan: Blaze with a budget alert** (for example 5 EUR), staying inside the no-cost quotas (Q1). Storage is not possible on Spark. Blaze also allows Identity Platform MFA.
2. **Notifications without a server: no Cloud Functions** (Q3). Each device listens to Firestore for the tickets it takes part in (`participantIds`) and shows local notifications, plus a WorkManager periodic check when the app is closed. FCM tokens are stored in `users/{uid}.fcmTokens`; remote reception is shown with a console test message. One small Cloud Function (status change -> FCM) stays an F-016 Gate option if time remains.
3. **5-day close: computed on read** (Q4). A RESOLVED ticket whose RESOLVED history entry is more than 5 days old counts as CLOSED and is locked (a pure rule in `model/`, D-008). The first client that sees it (operator panel, citizen list or the F-016 worker) writes the CLOSED transition in a batch. The rules allow CLOSED only after the 5 days (server time) and lock the ticket afterwards ([decision 0010](0010-firestore-schema.md)).
   - **24 h warning (brief III):** the citizens of a RESOLVED ticket (`participantIds`) get a notification when 24 hours remain before it closes, that is 4 days after the RESOLVED entry. Without a server it is a local notification, scheduled by the device from the RESOLVED entry time (the Firestore listener and the F-016 periodic worker); it is skipped if the ticket was reopened meanwhile.
4. **Maps: osmdroid** (Q6). No key, so the ZIP builds and the map renders for the grader. Requirements: `INTERNET`, a User-Agent, OSM attribution, coarse zoom with gestures off for anonymous users.
5. **Firestore start mode: test mode with an expiry of 2026-11-21** (Q5) for development. The drafted rules are deployed, with the person's OK, once D-009 login works (target 2026-11-14) and at the latest before the ZIP.
6. **MFA: Identity Platform with SMS** (Q7), using console test phone numbers with fixed codes for development and the defence (no SMS cost). Diogo confirms at D-009's Gate.
7. **`app/google-services.json` stays ignored** (Q2) and is shared with Diogo privately. D-015 copies it into the ZIP by hand, because a clean checkout does not have it.

### Rejected
- Spark: no Storage, so no photos or videos.
- Cloud Functions for push, the 24 h warning and the scheduled close: JS/TS code both authors must defend, and a deploy step.
- A scheduled server close: needs Functions.
- Google `maps-compose` (the class example): its key would live in `local.properties`, which the ZIP excludes, so the grader would see a blank map.
- Production mode from day one: blocks F-011 until Auth exists.
- TOTP MFA: also supported, but needs an authenticator app at the defence.

## Consequences
- A billing account is needed; the budget alert is the cost guard.
- **F-016:** notifications arrive only while some client runs (the app or its worker). The report states this limit. F-016 also owns the 24 h warning before the close (point 3).
- **D-008:** owns the pure "counts as CLOSED" rule; every list and detail uses it, so a stale RESOLVED ticket never looks open.
- **F-012 and D-014:** use osmdroid. The osmdroid repository was archived in Nov 2024; it still works (a report note).
- **D-009:** SMS MFA needs the debug SHA-1 in the console and test numbers.
- **D-015:** the ZIP checklist gains "copy `app/google-services.json` and build the unzipped copy".
- Until the rules are deployed, the test-mode database is open to anyone with the project config; the expiry limits it.

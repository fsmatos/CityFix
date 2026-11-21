# F-019 docs plan

## Docs to update
- `docs/architecture/overview.md`: remove FCM from the data layer, container, services table, manifest notes and the "Notifications (F-016)" section; add an F-019 line.
- `CLAUDE.md`: drop "FCM" from the Firebase list in the intro.
- `docs/report/notes.md`: notifications are local only; drop the FCM deep-link idea.
- `docs/testing/manual-test-checklist.md` (untracked): drop the FCM console test.

## Decisions to record
- **0027-local-notifications-only.md:** FCM removed (person's decision, 2026-11-20); local notifications as P10; brief III remote part not covered, accepted.
- Amendment lines in 0012, 0024 ("FCM part superseded by 0027") and 0010 ("`fcmTokens` no longer written").

## Report notes
Notifications are generated on the device from Firestore data (listener + WorkManager), with no push server. Improvement: FCM sent by a Cloud Function on status change.

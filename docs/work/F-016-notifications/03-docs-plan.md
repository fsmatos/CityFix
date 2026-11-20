# F-016 docs plan

## Docs to update
- `docs/architecture/overview.md`:
  - New "Notifications (F-016)" section: files, channels, event rule, last-check time, worker, FCM token, tap extra, test steps (console test message, App Inspection, shortened constant).
  - "Handover (F-016)": Diogo's `MainActivity` lines; D-014 contract confirmed (with the Q8 change); new ticket fields for any message writer.
- `docs/decisions/0010-firestore-schema.md`: amendment lines for `lastMessageAt`, `lastMessageAuthorId` and D-014's `lastOperatorNearbyAt`.
- `docs/decisions/0022-operator-proximity-alert.md`: one line, "F-016 contract confirmed" (tell Diogo, his record).

## Decisions to record
- `docs/decisions/0024-notifications.md`:
  - Client-only delivery (listener + 15 min worker), FCM for console tests; link 0012.
  - Event rule, one `lastCheckedAt` per user, never one's own action.
  - Time checks for the 24 h warning and Encerrado.
  - Operators: app-open only.
  - Gate 1 answers.

## Report notes
- `docs/report/notes.md`, new F-016 section:
  - Class sources: P11 FCM service, channels and permission; WorkManager slides; SharedPreferences (slides 09).
  - Limits: no real push without a server; Doze delays the worker; device clocks.
  - Improvements: Cloud Function on status change -> FCM; per-user settings; message preview; exact 24 h alarm.

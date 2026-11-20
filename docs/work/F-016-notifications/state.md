# F-016: Notifications (local and FCM)
Branch: dev/Francisco · Base: 751ae2c
Remote: origin/dev/Francisco

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-19 | feature, task file `docs/tasks/phase-2/F-016-notifications.md` |
| Plan | done | 2026-11-19 | 00/01/03 plans, 4 slices, Q1-Q10 |
| Gate 1: plan approved | done | 2026-11-20 | approved; Q2 changed: worker also for operators |
| Code | pending | | |
| Review | pending | | |
| Docs | pending | | |
| Pushed | pending | | |
| Merged | pending | | |

## Log
Append-only.
- 2026-11-19: merged origin/dev/Diogo (D-011 to D-015, 81 commits) into dev/Francisco first (751ae2c), as the person chose: F-016 consumes D-014's `lastOperatorNearbyAt` (decision 0022, Q9 contract). Conflicts in MainActivity (imports), overview.md and notes.md resolved keeping both sides; build and lint pass. Firestore `categories` still has id `pavement` after D-015 removed DebugSeed. Started F-016 from the task file (target was 2026-11-19).
- 2026-11-19: Plan written (00-plan, 01-coding-plan, 03-docs-plan). Client-only delivery per decision 0012 (Firestore listener while the app is open + 15 min periodic worker, FCM service for console tests), one `lastCheckedAt` per user in SharedPreferences, pure event rule in `model/TicketNotifications`, new ticket fields `lastMessageAt`/`lastMessageAuthorId`, time checks for the 24 h warning and Encerrado. Class sources: P11 FCM + Firebase demo, slides 10/11 WorkManager, slides 09 SharedPreferences. 4 slices; open questions Q1-Q10 (incl. operators app-open only, D-014 contract adjusted to lastCheckedAt). Next decision record: 0024. Awaiting Gate 1.
- 2026-11-20: Sync with main skipped (0 commits behind). Gate 1 approved. Answers: Q1, Q3-Q10 as planned (Q5 time check, Q6 at session start); Q2 changed: operators also notified by the 15 min worker when the app is closed (new `allTicketsOnce`, `isOperator` as worker input). Plans updated.

# F-019: Remove FCM
Branch: dev/Francisco · Base: 9eadce9
Remote: not pushed

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-20 | refactor-like removal, task file `docs/tasks/phase-2/F-019-remove-fcm.md` |
| Plan | done | 2026-11-20 | 00/01/03 plans, 2 slices, Q1-Q2 |
| Gate 1: plan approved | done | 2026-11-20 | approved; Q1 yes (drop `fcmTokens` from draft rules), Q2 as planned |
| Code | pending | | |
| Review | pending | | |
| Docs | pending | | |
| Pushed | pending | | |
| Merged | pending | | |

## Log
Append-only.
- 2026-11-20: Started from the person's request in chat (task file written from it). 0 commits behind `origin/main`. Untracked `.idea/` files and `docs/testing/` left as they were. Plans written; the main session wrote them directly, without the planner agent (small removal, context already gathered). Awaiting Gate 1.
- 2026-11-20: Gate 1 approved as planned. Q1: yes, the owner allow-list in the draft `firestore.rules` loses `'fcmTokens'` (not deployed). Q2: old task files and work folders keep their FCM text as history.

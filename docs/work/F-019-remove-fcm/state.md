# F-019: Remove FCM
Branch: dev/Francisco · Base: 9eadce9
Remote: not pushed

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-20 | refactor-like removal, task file `docs/tasks/phase-2/F-019-remove-fcm.md` |
| Plan | done | 2026-11-20 | 00/01/03 plans, 2 slices, Q1-Q2 |
| Gate 1: plan approved | done | 2026-11-20 | approved; Q1 yes (drop `fcmTokens` from draft rules), Q2 as planned |
| Code | done | 2026-11-20 | slice 1, `8a6a259`; build and lint pass; device check pending |
| Review | skipped | 2026-11-20 | not run (small removal); can run `/review-code` |
| Docs | done | 2026-11-20 | slice 2: decision 0027, overview, 0010/0012/0024 lines, CLAUDE.md, report notes |
| Pushed | pending | | |
| Merged | pending | | |

## Log
Append-only.
- 2026-11-20: Started from the person's request in chat (task file written from it). 0 commits behind `origin/main`. Untracked `.idea/` files and `docs/testing/` left as they were. Plans written; the main session wrote them directly, without the planner agent (small removal, context already gathered). Awaiting Gate 1.
- 2026-11-20: Gate 1 approved as planned. Q1: yes, the owner allow-list in the draft `firestore.rules` loses `'fcmTokens'` (not deployed). Q2: old task files and work folders keep their FCM text as history.
- 2026-11-20: Slice 1 (`8a6a259 refactor(F-019): remove FCM`): deleted `CityFixMessagingService.kt` and `PushTokens.kt`; removed the manifest service, `messaging`/`pushTokens`, the token save, `fcmTokens: []` at registration, the Gradle dependency and `'fcmTokens'` in the draft rules (Q1). `assembleDebug` and `lint` pass. Device check (status change still notifies, tap opens the ticket) not run yet.
- 2026-11-20: Slice 2 (docs): new decision 0027; amendment lines in 0010, 0012, 0024; overview (current-state lines and a new "FCM removed (F-019)" section; historical entries left as written, Q2); `CLAUDE.md`; report notes (F-016 section and a new F-019 section). `docs/testing/manual-test-checklist.md` updated too, but it is untracked, so it is not in the commit. Review phase skipped, not run.

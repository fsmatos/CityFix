# F-018: Map pinch zoom, phone check at sign-up only, full-size media
Branch: dev/Francisco · Base: 26b8a88
Remote: origin/dev/Francisco

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-20 | fix, task file `docs/tasks/phase-2/F-018-map-zoom-phone-signup-media-viewer.md` |
| Plan | done | 2026-11-20 | 00/01/03 plans, 3 slices |
| Gate 1: plan approved | done | 2026-11-20 | approved as written |
| Code | done | 2026-11-20 | 3 slices, 3 commits; device checks pending |
| Review | skipped | | not run this round |
| Docs | done | 2026-11-20 | 0026, 0015 line, overview, report notes |
| Pushed | pending | | |

## Log
Append-only.
- 2026-11-20: Person reported three issues (map pinch zoom, SMS on every login, no full-size media). Asked about the MFA brief requirement; person chose "verify phone at sign-up only" (login without SMS), accepting the deviation from brief I.2.B/II.2.B. Plan written; awaiting Gate 1.
- 2026-11-20: Merged origin/dev/Diogo (18 commits, D-015 FAB, phone +351 fix, delivery ZIP script) into dev/Francisco at the person's request (26b8a88), no conflicts. Diogo took decision 0025, so F-018's record is 0026.
- 2026-11-20: Gate 1 approved as written.
- 2026-11-20: Code done, slices 1-3 (ea98c4c, eefc5af, 93ade90); assembleDebug and lint pass before each commit (0 errors, 18 warnings, all dependency versions plus the old UseKtx). Deviations from the coding plan:
  - Slice 2: `PHONE_IN_USE` sends the setup back to the phone step with the error under the field (the plan said "under the phone field" only). Leftover "second factor" comments in `MfaScreen`, `Validation`, `AccountSetupUiState` reworded. `FACTOR_NAME` removed (enrolment gone).
  - Slice 3: the media viewer state lives in `MediaSection` (`rememberSaveable` open id), not in `ShowScreen`, so nothing is threaded through `DetailsPane`; the viewer survives rotation (video restarts).
  - Not touched: Firestore/Storage rules, `google-services.json`, the Firebase console (Phone provider must stay enabled).
- 2026-11-20: Docs done: decision 0026, amendment line in 0015, overview (`TicketMap` onTouchChange, `MediaViewer`, Firebase line), report notes F-018. Device checks still to run: pinch on the ticket map (portrait and landscape), new account setup then logout/login without SMS, old enrolled account login, phone already used by another account, photo and video full screen, rotate, back.

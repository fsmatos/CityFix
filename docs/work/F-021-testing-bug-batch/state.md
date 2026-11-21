# F-021: Bugs found in manual testing
Branch: dev/Francisco · Base: dc93629
Remote: not pushed

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-21 | bug batch, task file `docs/tasks/phase-2/F-021-testing-bug-batch.md` |
| Plan | done | 2026-11-21 | 00/01/03 plans, up to 6 slices |
| Gate 1: plan approved | done | 2026-11-21 | approved; Q1 = recreate the account (no code) |
| Code | done | 2026-11-21 | bugs 2, 4, 6 coded (`619fee6`, `7cc2eb8`, `90d04c3`); bug 7 waiting for a screenshot |
| Review | pending | | |
| Docs | pending | | |
| Pushed | pending | | |
| Merged | pending | | |

## Log
Append-only.
- 2026-11-21: Started from the person's bug list. Step 0: `origin/main` fetched, 0 commits behind, nothing to merge. Untracked `.idea/` and modified `docs/testing/manual-test-checklist.md` left as they were. Plans written by the main session (context already gathered). Investigation: emulator logcat (no app crash), `publicTickets` read over Firestore REST (no IN_PROGRESS doc; the emulator's In progress ticket is PRIVATE). Awaiting Gate 1.
- 2026-11-21: Gate 1 approved. Q1: bug 1 needs no code; the person recreates the pre-F-018 test account (decision 0026). Slice 5 dropped. Q2: bugs 3 and 5 are re-tested by the person.
- 2026-11-21: Slices 1-3 coded by the main session: viewer `decorFitsSystemWindows = false` (619fee6), tab tap opens the tab root without saved stacks (7cc2eb8), pending list loads `citizen.photo` with icon fallback (90d04c3). `assembleDebug` and `lint` pass (0 errors). Not checked on a device: emulator screencap came back blank after switching to dark mode (UI dump showed the dashboard), so night mode was set back to off. Bug 7 deviation: the snackbar's dark colours are inverseSurface #ECECEA with #2B2B2A text, high contrast on paper, so the cause is unclear; slice 4 waits for the person's screenshot. Installed the build on emulator-5554 (`install -r`, data kept).

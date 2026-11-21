# F-021: Bugs found in manual testing
Branch: dev/Francisco · Base: dc93629
Remote: origin/dev/Francisco

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-21 | bug batch, task file `docs/tasks/phase-2/F-021-testing-bug-batch.md` |
| Plan | done | 2026-11-21 | 00/01/03 plans, up to 6 slices |
| Gate 1: plan approved | done | 2026-11-21 | approved; Q1 = recreate the account (no code) |
| Code | done | 2026-11-21 | bug 2 `619fee6` + `fbf7d25`, bug 6 `90d04c3`, bug 7 `2159a04`; bug 4 `7cc2eb8` superseded by D-020 at merge `d300eb9`; review fixes `ef97e5f` |
| Review | done | 2026-11-21 | review.md: 0 blocking, 2 should-fix, 3 nits |
| Docs | done | 2026-11-21 | overview (viewer, force dark, pending photo), report notes F-021 section |
| Pushed | done | 2026-11-21 | origin/dev/Francisco (`a7dc894`) |
| Merged | pending | | |

## Log
Append-only.
- 2026-11-21: Started from the person's bug list. Step 0: `origin/main` fetched, 0 commits behind, nothing to merge. Untracked `.idea/` and modified `docs/testing/manual-test-checklist.md` left as they were. Plans written by the main session (context already gathered). Investigation: emulator logcat (no app crash), `publicTickets` read over Firestore REST (no IN_PROGRESS doc; the emulator's In progress ticket is PRIVATE). Awaiting Gate 1.
- 2026-11-21: Gate 1 approved. Q1: bug 1 needs no code; the person recreates the pre-F-018 test account (decision 0026). Slice 5 dropped. Q2: bugs 3 and 5 are re-tested by the person.
- 2026-11-21: Slices 1-3 coded by the main session: viewer `decorFitsSystemWindows = false` (619fee6), tab tap opens the tab root without saved stacks (7cc2eb8), pending list loads `citizen.photo` with icon fallback (90d04c3). `assembleDebug` and `lint` pass (0 errors). Not checked on a device: emulator screencap came back blank after switching to dark mode (UI dump showed the dashboard), so night mode was set back to off. Bug 7 deviation: the snackbar's dark colours are inverseSurface #ECECEA with #2B2B2A text, high contrast on paper, so the cause is unclear; slice 4 waits for the person's screenshot. Installed the build on emulator-5554 (`install -r`, data kept).
- 2026-11-21: Bug 7: the person saw white text on a white snackbar. On the emulator in dark mode (impact snackbar triggered with a fake accelerometer bump) the snackbar is light grey with dark text, so the app colours are right; likely cause: the phone's system force dark (Xiaomi app dark mode) recolouring the text. Fix: `android:forceDarkAllowed=false` in both themes (API 29+). Build and lint pass. To confirm on the Mi 9T Pro. Emulator-5554 got the new build; night mode set back to off.
- 2026-11-21: Bug 2 re-opened by the person: 619fee6 did not fix it. Reproduced on emulator-5556: after rotating, the viewer closed (not inset). Real cause: `openedId` was `rememberSaveable` inside `MediaSection`, which is at a different place in ShowScreen's portrait Column and landscape Row, so its saved state did not match after rotation. Fix: `openedId` and the `AttachmentViewer` moved up to `ShowScreen` (same place in both layouts); `MediaSection` gets an `onOpen` callback. Checked on emulator-5556: photo stays open and full screen portrait -> landscape -> portrait. 619fee6 (`decorFitsSystemWindows = false`) kept: the viewer is edge to edge in landscape. Build and lint pass.
- 2026-11-21: Merged `origin/main` (D-019, D-020, D-021) into dev/Francisco. Conflicts: `MainActivity.kt` `openTab` (F-021 bug 4 vs D-020, same bug): person's choice, D-020's version kept (per-tab saved stacks, highlight follows the tab holding the screen, reselect pops to root/scrolls up); F-021's 7cc2eb8 change is superseded. `ShowScreen.kt`: combined, F-021's hoisted `openedId`/`onOpenAttachment`/`onOpen` plus D-019's `responsibleNames`/`uploaderNames`. `PendingCitizensScreen.kt` auto-merged (F-021 photo + D-020 list state). Build and lint pass (0 errors). Checked on emulator-5556 (operator): photo viewer stays open and full screen portrait -> landscape -> portrait; Panel -> detail -> Profile -> Panel shows the detail with Panel highlighted, tapping Panel again shows the panel.
- 2026-11-21: Pushed dev/Francisco to origin (`dc93629..d300eb9`). Not merged into main. Review and docs phases not run yet.
- 2026-11-21: Review (reviewer agent) of 619fee6, fbf7d25, 90d04c3, 2159a04 and the d300eb9 resolution; findings in review.md: 0 blocking, 2 should-fix (AttachmentViewer comment states the wrong cause; Files table / Code row out of date after the deviations), 3 nits. Confirmed nothing of 7cc2eb8 is left in MainActivity.kt. No code edited.
- 2026-11-21: Review fixes (`ef97e5f`): should-fix 1 (AttachmentViewer comment now gives the edge-to-edge reason), should-fix 2 (01-coding-plan Files table and Code row updated), nit 1 (`isNullOrBlank()`; a failed load still leaves the grey box, said in the comment), nit 3 (ShowScreen comment names state hoisting). Nit 2 (preview of the photo branch) skipped: previews have no network image. Build and lint pass.
- 2026-11-21: Docs: `docs/architecture/overview.md` (AttachmentViewer bullet: state in ShowScreen and edge to edge; new bullets on force dark and the pending list photo), `docs/report/notes.md` (F-021 section). No decision record: no architecture choice beyond D-020's, already recorded by Diogo.

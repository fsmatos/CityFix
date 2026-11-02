# D-006: MainActivity with Scaffold and NavHost
Branch: dev/Diogo · Base: 3eb1195
Remote: not pushed

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-02 | Started after origin/main brought in F-001 to F-006 |
| Plan | done | 2026-11-02 | 00-plan, 01-coding-plan, 03-docs-plan; 6 slices, no tests |
| Gate 1: plan approved | done | 2026-11-02 | Approved by Diogo with the planner's recommended answers; S4 goes to its own task |
| Code | done | 2026-11-02 | 6 slices (d359cb2, 8147a46, 9ea90fb, aefec5e, 1181701, ba4ae8d); assembleDebug and lint pass; emulator checks run, see Log |
| Review | done | 2026-11-02 | review.md: 0 blocking, 1 should-fix, 3 nits; slice 6 and emulator checks treated as known open items |
| Docs | done | 2026-11-02 | overview.md, report notes, phase-1 README; no new decision record; task file left unticked, see Log |
| Pushed | | | |
| Merged | | | |

## Log
Append-only.

- 2026-10-31 Start attempted: F-004 to F-006 did not exist yet, so Diogo chose to wait for Francisco. Nothing was started.
- 2026-11-02 Merge: origin/main into dev/Diogo as 3eb1195 (74 commits, F-001 to F-006), approved by Diogo; no conflicts; assembleDebug and lint pass.
- 2026-11-02 Plan: planner wrote the three plans. navigation-compose 2.9.8 (2.10.2 needs Compose runtime 1.11.2); adds lifecycle-viewmodel-compose and lifecycle-runtime-compose; 6 slices.
- 2026-11-02 Gate 1: approved by Diogo, with the recommended answers to Q1-Q7. The expand toggle is wired as the task requires. The S4 projection change (`toAnonymousView()` blanks `description` and clears `statusHistory`) and the toggle's anonymous behaviour go to a separate task, to finish before delivery.
- 2026-11-02 Code: slices 1-5 implemented by the implementer agent, which has no emulator. Commits: d359cb2 (dependencies), 8147a46 (report state), 9ea90fb (scaffold), aefec5e (detail), 1181701 (report and snackbar).
- 2026-11-02 Resolved versions (`:app:dependencies --configuration debugRuntimeClasspath`, slice 1): `kotlinx-serialization-core` 1.11.0, Compose `runtime` 1.11.0, Compose `ui` 1.11.0 (was 1.10.4), `foundation` 1.10.4, `material3` 1.4.0, `navigation-compose` 2.9.8, `lifecycle-viewmodel-compose` 2.11.0, `lifecycle-runtime-compose` 2.11.0, `kotlin-stdlib` 2.3.20. All as expected; none above the 1.11.0 limits.
- 2026-11-02 Verification: assembleDebug and lint pass on slices 1-5. Lint: 0 errors, 14 warnings on every slice (baseline 13, plus navigation 2.9.8 -> 2.10.2). Grep: no `Greeting` or `welcome_message` left in `app/src`. Read the `null` branch of `onSubmit`: returns `false`, with no navigation and no snackbar.
- 2026-11-02 Deviations: (1) the new lint warning has id `GradleDependency`, not `NewerVersionAvailable` as the plan says; same library, count as expected. (2) Slice 3 also removes the blank line left at the top of `values-pt/strings.xml` by deleting `welcome_message`; after that edit assembleDebug was rerun but lint was not, until slice 4 (0 errors, 14 warnings). (3) Slice 3's stop condition (first run of the Compose `ui` 1.11.0 and `foundation` 1.10.4 mix) could not be checked, so slices 4 and 5 were built on an app that has never run. The emulator checks of slices 3-5 are not done.
- 2026-11-02 Not done: slice 6 (`contentWindowInsets = WindowInsets.safeDrawing`, and the `MainTopAppBar` line if needed), left for Diogo as it depends on the emulator check. `model/TicketPrivacy.kt` is untouched; the expand toggle is wired unconditionally (Q2).
- 2026-11-02 Review: reviewer read 3eb1195..HEAD against the plans, decisions 0006-0008, overview, conventions and the brief; reran assembleDebug and lint (0 errors, 14 warnings) and confirmed the resolved versions. Nothing run on a device. Wrote review.md: no blocking, S1 (no task file for the deferred privacy work), N1-N3.
- 2026-11-02 Slice 6: done by the orchestrator as ba4ae8d, because the AVD Iphone_Diogo was available. `contentWindowInsets = WindowInsets.safeDrawing` only; assembleDebug and lint pass (0 errors, 14 warnings). With the tall cutout overlay (126 px) in both landscape rotations, the bar's controls clear the cutout on the dashboard and list, so the `MainTopAppBar` line was not needed. Detail and report were not checked under the cutout. This slice came after the review; the reviewer did not see it.
- 2026-11-02 Emulator checks (Iphone_Diogo, driven through adb, not by hand). First run: no crash and no `NoSuchMethodError`, so the Compose `ui` 1.11.0 and `foundation` 1.10.4 mix runs. Passed: dashboard -> list -> detail -> report; system back and arrow through report -> detail -> list -> dashboard; arrow absent on the dashboard; rotation keeps all four screens; expanded state kept on rotation (3 runs); half-filled form kept on rotation and empty after leaving; empty submit shows every field error and stays on the form; PUBLIC report shows the snackbar, is in the list, total 3 -> 4; PRIVATE report shows the snackbar, no "not found", is not in the list, total 4 -> 5; form empty after a submit; anonymous detail shows only header, media and map; PT strings on the dashboard.
- 2026-11-02 Unexplained: in the first expanded-list rotation test the app showed the dashboard afterwards, with no crash in the log. Three repeats from a clean start, including that exact sequence, did not reproduce it. Cause unknown; the first run's script was not instrumented, so it may be a test artefact or a rare real loss of the screen.
- 2026-11-02 Not checked: double taps; back on the dashboard after the final build (it left the app in the earlier run); title flash after rotation (review N2, needs frame-level viewing); keyboard overlap and double gap in landscape; PT snackbar text; a real device.
- 2026-11-02 Review S1: fixed by adding `docs/tasks/phase-1/D-007-anonymous-projection.md`. N1 left as planned (double tap on the back arrow). N2 and N3 are for Docs or a manual check.
- 2026-11-02 Docs: docs-writer updated overview.md (new "App shell (D-006)", contract blocks replaced, stale lines fixed, N3 fixed by naming `GradleDependency` and limiting the D-004 sentence, D-007 linked), docs/report/notes.md (D-006 section) and docs/tasks/phase-1/README.md (D-007 row, signatures). No decision 0009, as Q5 did not change the plan. Deviation: `D-006-main-activity-nav.md` was left unticked at status "to do", because the plan ticks it once Diogo confirms on the emulator and the checks ran through adb, not by hand. Not committed (no shell).

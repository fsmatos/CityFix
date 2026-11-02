# D-006: MainActivity with Scaffold and NavHost
Branch: dev/Diogo · Base: 3eb1195
Remote: not pushed

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-02 | Started after origin/main brought in F-001 to F-006 |
| Plan | done | 2026-11-02 | 00-plan, 01-coding-plan, 03-docs-plan; 6 slices, no tests |
| Gate 1: plan approved | done | 2026-11-02 | Approved by Diogo with the planner's recommended answers; S4 goes to its own task |
| Code | pending | 2026-11-02 | Slices 1-5 committed (d359cb2, 8147a46, 9ea90fb, aefec5e, 1181701); assembleDebug and lint pass. Slice 6 and all emulator checks are left for Diogo |
| Review | | | |
| Docs | | | |
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

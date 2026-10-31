# D-005: MainTopAppBar
Branch: dev/Diogo · Base: d910a26
Remote: not pushed

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-10-31 | Task file: docs/tasks/phase-1/D-005-main-top-app-bar.md |
| Plan | done | 2026-10-31 | 00-plan, 01-coding-plan, 03-docs-plan; 3 slices, no tests |
| Gate 1: plan approved | done | 2026-10-31 | Approved by Diogo; planner recommendations for Q1-Q8 |
| Code | done | 2026-10-31 | 3 slices; verify passed on each; lint 0 errors, 13 warnings; preview pane check still to do by hand |
| Review | pending | | |
| Docs | pending | | |
| Pushed | pending | | |
| Merged | pending | | |

## Log
Append-only.
- 2026-10-31 Start: task D-005 started on dev/Diogo at d910a26. `git fetch origin` done; origin/main has no commits missing from dev/Diogo, and Diogo chose to skip the merge. Untracked `.idea/` files left alone (IDE state, not part of the task).
- 2026-10-31 Plan: planner wrote 00-plan, 01-coding-plan, 03-docs-plan. Stateless `MainTopAppBar` with enum `TopBarScreen` and `canNavigateBack`; adds `material-icons-core` through the BOM; `navigation-compose` stays in D-006. 3 slices, open questions Q1-Q8.
- 2026-10-31 Plan checks by the main session: the planner could not read the brief or Francisco's branch. Q7: the brief does not contradict the route rules and asks for context-dependent icons and top bars. Only registered citizens open tickets, so the Auth phase must hide the add button by role. Q5: `values-pt/strings.xml` already exists on origin/dev/Francisco (693b05d) and is not on origin/main.
- 2026-10-31 Gate 1: approved by Diogo; every open question takes the planner's recommendation. D-005 creates `values-pt/strings.xml` now and accepts a small add/add conflict with Francisco's file at merge time (keep both sides). Hiding the add button by role is a requirement for the Auth phase, not part of D-005.
- 2026-10-31 Code baseline: lint before D-005 had 0 errors, 13 warnings.
- 2026-10-31 Code slice 1 (icons dependency): added `material-icons-core` to the catalog and `app/build.gradle.kts`. `assembleDebug` pass, `lint` pass, 0 errors, 13 warnings. `debugRuntimeClasspath` shows `material-icons-core` 1.7.8; `androidx.compose.ui:ui` stays at 1.10.4 and `kotlinx-serialization-core` at 1.9.0.
- 2026-10-31 Code slice 2 (strings): `git diff origin/main origin/dev/Francisco -- app/src/main/res` shows Francisco adds `values-pt/strings.xml` and edits `values/strings.xml`, with no `top_bar_` or `cd_` names, so no name clash. Nothing fetched from his branch, as decided at Gate 1. Added the strings to `values/strings.xml` and created `values-pt/strings.xml`. `assembleDebug` pass, `lint` pass, 0 errors, 21 warnings: the 8 extra are the expected `UnusedResources` for the new strings. No `MissingTranslation`.
- 2026-10-31 Code slice 3 (top bar): created `ui/navigation/MainTopAppBar.kt` with `TopBarScreen`, `MainTopAppBar` and the previews. All four icons resolve from `material-icons-core`, so the vector drawable fallback was not needed. `assembleDebug` pass, `lint` pass, 0 errors, 13 warnings (back to baseline; none in the new file). No imports from `androidx.navigation`, `viewmodel` or `model`.
- 2026-10-31 Code not verified: the Android Studio preview pane check from slice 3 was not done, because previews need the IDE. Diogo still has to check each screen in both orientations against the table in 00-plan.
- 2026-10-31 Code deviations: no functional ones. `BarPreview` uses named arguments, and the preview functions use block bodies rather than the plan's one-line expression bodies. The plan's "do not touch state.md" was overridden by the task instruction to record this log here.

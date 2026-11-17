# D-015: Final pass: landscape, accessibility, strings, delivery ZIP
Branch: dev/Diogo · Base: 69fa4a6
Remote: not pushed

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-17 | Task file `docs/tasks/phase-2/D-015-final-pass-delivery.md` |
| Plan | done | 2026-11-17 | 00-plan, 01-coding-plan, 03-docs-plan; 6 slices (3 now, 3 after merges or Gate answers), 10 open questions |
| Gate 1: plan approved | done | 2026-11-17 | Approved by Diogo; Q1-Q10 as recommended; slices 1-3 now, 4-6 after the merges |
| Code | | | |
| Review | | | |
| Docs | | | |
| Pushed | | | |
| Merged | | | |

## Log
- 2026-11-17 Start: `origin/main` had no commits missing from `dev/Diogo`; Diogo chose not to run `/merge`. Commit type per slice (`fix`, `chore`, `refactor`). Untracked `.idea/` files left out of the commit.
- 2026-11-17 Plan (planner). Read the task file, brief V/VI (PDF), CLAUDE.md, the kotlin rules, decisions 0009/0012/0013/0014, the overview's D-015 handovers, F-017's task file, `strings.xml` in both locales, `MainActivity`, `CityFixApplication`, `DebugSeed`, `Sample*`, `PlaceholderScreen`, `MainTopAppBar`, the manifest, `colors.xml`/`themes.xml`, and the last lint report. Grepped `ui/` for literal text, `contentDescription = null`, `remember` vs `rememberSaveable`, and touch-target sizes. What was found:
  - The EN/PT keys match, apart from 3 `translatable="false"` entries; `app_name` is one of them, so it is not really missing.
  - There is no hard-coded UI text outside previews, and every null `contentDescription` is on a decorative icon.
  - Form state is in ViewModels or `rememberSaveable`.
  - `LocalTicketsRepository` was already deleted (F-011).
  - To remove: `DebugSeed` and its call; `SampleCategories` (dead after that); the `placeholder_message` default; stale comments.
  - Lint baseline is 0 errors and 22 warnings: 13 version notices, `RedundantLabel`, 7 `UnusedResources`, and `UseKtx` (Francisco's).
  - F-015/F-016/F-017 are not on this branch yet, so the strings and screen sweep is repeated after the merges.
  - Francisco's files touched (`CityFixApplication`, `DebugSeed`, `SampleCategories`, shared manifest and strings) were inferred from work items, because the planner had no shell for `git log`. Confirm them before coding.
  - Risks raised:
    - Firestore/Storage test mode ends 2026-12-10 (Q7).
    - The grader's SMS MFA may fall back to reCAPTCHA (R3).
    - The Maps key must not be SHA-1 restricted (R4).
  - Not committed.
- 2026-11-17 Gate 1: Diogo approved the plan with Q1-Q10 as recommended (slices 1-3 now; keep `SampleTickets`; no `app_name` in `values-pt`, the task file is corrected instead; citizen FAB (slice 5) and ZIP script (slice 6) approved; console changes (Q4, Q7) and passwords (Q9, PDF only) are Diogo's by hand). Ownership checked with `git log`: `CityFixApplication.kt`, `DebugSeed.kt`, `SampleCategories.kt`, `AndroidManifest.xml`, `colors.xml` are Francisco's; `SampleTickets.kt`, `PlaceholderScreen.kt` are Diogo's.

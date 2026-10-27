# F-001: Theme, strings and accessibility base
Branch: dev/Francisco · Base: eff85d0
Remote: not pushed

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-10-25 | |
| Plan | done | 2026-10-27 | revised to 2 slices (course alignment) |
| Gate 1: plan approved | done | 2026-10-27 | approved with defaults Q1-Q3 |
| Code | | | |
| Review | | | |
| Docs | | | |
| Pushed | | | |
| Merged | | | |

## Log
Append-only.
- 2026-10-25: started; origin/main has 1 commit not yet in dev/Francisco (not merged).
- 2026-10-25: plan written (00-plan, 01-coding-plan, 02-test-plan, 03-docs-plan) from the task file and a read of Theme/Color/Type, MainActivity, strings, themes.xml, libs.versions.toml. Open questions Q1-Q4 in 00-plan.md. No code changed. Awaiting Gate 1.
- 2026-10-25: tests dropped from the workflow by the person's instruction. Plans revised: removed StringsParityTest, ContrastTest, DimensTest, 02-test-plan and all unit-test/testDebugUnitTest mentions; acceptance checks are now manual (lint MissingTranslation, device in Portuguese, dark mode, font scale, 48dp by code review, contrast by eye plus a quick calculation noted here). Still 3 slices, each verified by assembleDebug + lint; palette, dynamic colour off and other decisions unchanged. Q4 (androidTest) removed. Gate 1 remains pending: the person wants time to review.
- 2026-10-27: Gate 1 decision: revise. Person's feedback: no dark mode (not a must/should in the brief) and follow the course material (survey of Teoricas, P01-P08, ContactsApp_Prof, Exemplos). Plans revised: removed dark scheme, `darkTheme` param, `values-night/themes.xml`, dark checks and contrast calculations; removed full typography scale (Type.kt stays template), `Dimens.kt`/48dp constant (48dp now via M3 components, code review) and the colors.xml cleanup. Theme keeps the template trio, light only, `dynamicColor` default false (the one deviation from the template, so brand colours show). Strings: snake_case keys, `app_name` `translatable="false"`, only `welcome_message` added now (shared actions deferred, Q2). Added "Course alignment" section to 00-plan. Now 2 slices (strings; light colour scheme). Open questions: Q1 palette, Q2 shared strings, Q3 placeholder. No code changed. Gate 1 still pending.
- 2026-10-27: Gate 1 approved by the person with the proposed defaults: Q1 palette primary 0xFF1565C0, secondary 0xFF00695C, tertiary 0xFFBF360C; Q2 only `welcome_message` now; Q3 keep the translated placeholder.

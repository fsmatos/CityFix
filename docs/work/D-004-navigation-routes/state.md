# D-004: Navigation routes
Branch: dev/Diogo · Base: 9e9c229
Remote: not pushed

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-10-29 | Task file: docs/tasks/phase-1/D-004-navigation-routes.md |
| Plan | done | 2026-10-29 | 00-plan, 01-coding-plan, 03-docs-plan; 2 slices, no tests |
| Gate 1: plan approved | done | 2026-10-29 | Approved as written; planner recommendations for Q1-Q6 |
| Code | done | 2026-10-29 | 2 slices: db86ab9, 8b04fa6; build and lint pass |
| Review | pending | | |
| Docs | pending | | |
| Pushed | pending | | |
| Merged | pending | | |

## Log
Append-only.
- 2026-10-29 Start: task D-004 started on dev/Diogo at 9e9c229. `git fetch origin` done; origin/main has no commits missing from dev/Diogo, and Diogo chose to skip the merge. Untracked `.idea/` files left alone (IDE state, not part of the task). Note: the task says to use the kotlin-serialization plugin "from the version catalog", but `gradle/libs.versions.toml` has no serialization plugin or library yet, and there is no Navigation Compose dependency either.
- 2026-10-29 Plan: planner wrote 00-plan, 01-coding-plan, 03-docs-plan. Adds the Kotlin Serialization plugin (version.ref kotlin) and `kotlinx-serialization-core` 1.9.0 to the catalog and `:app`; `ui/navigation/Destinations.kt` with `DashboardRoute`, `TicketListRoute`, `ReportTicketRoute` (`data object`) and `TicketDetailRoute(ticketId: String)`. `navigation-compose` deferred to D-006 (recommended). 2 slices, no test slice, no decision record recommended (0007 free). Open questions Q1-Q6.
- 2026-10-29 Gate 1: approved by Diogo as written; every open question Q1-Q6 takes the planner's recommendation (navigation-compose in D-006, serialization 1.9.0, `ticketId`, no sealed interface, no decision record, Gradle files edited).
- 2026-10-29 Code: implemented both slices from 01-coding-plan. Slice 1 `db86ab9 build(D-004): add kotlin serialization plugin and runtime` (catalog: `kotlinxSerialization = "1.9.0"`, `kotlinx-serialization-core`, `kotlin-serialization` plugin with version.ref kotlin; root `apply false`; `:app` applies the plugin and adds the runtime). The plugin applies fine under AGP 9.4.1 built-in Kotlin 2.2.10. Slice 2 `8b04fa6 feat(D-004): add type-safe navigation routes` (`ui/navigation/Destinations.kt` with the four routes as in Interfaces; `.gitkeep` deleted). Verify after each slice: `./gradlew assembleDebug` BUILD SUCCESSFUL, `./gradlew lint` BUILD SUCCESSFUL with 0 errors, 13 warnings. The only new warning is `NewerVersionAvailable` for kotlinx-serialization-core (1.9.0 vs 1.12.0). It is accepted because of the Kotlin 2.2.10 pin (00-plan Q2). The plan expected the id `GradleDependency`, but lint reports it as `NewerVersionAvailable`. The other 12 warnings were already there. Plugin check: `TicketDetailRoute$$serializer.class` is generated. Deviation: with AGP 9 built-in Kotlin the classes go to `app/build/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes/`, not the `app/build/tmp/kotlin-classes/debug/` path in the coding plan. No code deviations. `.idea/` not staged.
- 2026-10-29 Code (correction): D-004 added two lint warnings, not one. There were 11 before. The new ones are the newer-version warnings for `kotlinx-serialization-core` (1.9.0 vs 1.12.0) and for the `org.jetbrains.kotlin.plugin.serialization` plugin (2.2.10 vs 2.4.21). The plugin warning comes from `version.ref = "kotlin"` and is the same one `kotlin-compose` already has. Both are accepted: Kotlin stays at 2.2.10 (00-plan Q2, Out of scope).

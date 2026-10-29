# D-004: Review

Reviewer: reviewer agent · Date: 2026-10-29
Diff: `git diff 931c26c..HEAD` (db86ab9, 8b04fa6, 7952055, 103a954). `state.md` records Base 9e9c229. The two commits in between, 723ba28 (start) and 931c26c (plan), are docs only and come before the code, so this review leaves them out.
Checked against: 00-plan, 01-coding-plan, 03-docs-plan, task file `docs/tasks/phase-1/D-004-navigation-routes.md`, overview.md, kotlin-conventions, CLAUDE.md. Gate 1 approved Q1-Q6 as the planner recommended.
Verification: `./gradlew assembleDebug lint` gives BUILD SUCCESSFUL. `lint-results-debug.xml` has 13 issues, all `Warning`, with 0 errors. The two new ones are `NewerVersionAvailable` for `kotlinx-serialization-core` (1.9.0 vs 1.12.0) and for `org.jetbrains.kotlin.plugin.serialization` (2.2.10 vs 2.4.21). These match the correction entry in `state.md`. The serialization plugin really is applied: `TicketDetailRoute$$serializer.class` and `TicketDetailRoute$Companion.class` exist under `app/build/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes/pt/isec/amov/cityfix/ui/navigation/`. There are no tests, by Diogo's decision, and this review does not ask for any.

## Verdict
**Approve.** Nothing blocks and nothing needs fixing. The four changed files match the coding plan's Interfaces section line for line. The two recorded deviations (the AGP 9 `built_in_kotlinc/` output path and the lint id `NewerVersionAvailable` instead of `GradleDependency`) concern verification, not code, and both are explained. Two nits below are forward notes for Docs and D-006.

## What was checked and holds
- **Catalog.** `kotlinxSerialization = "1.9.0"` and the `kotlinx-serialization-core` library are added. The `kotlin-serialization` plugin uses `version.ref = "kotlin"`. They are appended after the existing entries, and no existing version (`agp`, `kotlin`, `composeBom`) changed.
- **Gradle files.** The root build declares the plugin `apply false`. `:app` applies it through `libs.plugins.kotlin.serialization` and adds `implementation(libs.kotlinx.serialization.core)` after `lifecycle.runtime.ktx`, as planned. Neither file has hard-coded coordinates. There is no `kotlin-android` plugin, which is correct for AGP 9 built-in Kotlin.
- **Routes.** `Destinations.kt` is in `pt.isec.amov.cityfix.ui.navigation`, and all four routes have `@Serializable`. `DashboardRoute`, `TicketListRoute` and `ReportTicketRoute` are `data object`s, which give a readable `toString()` and `equals`. `TicketDetailRoute` has exactly one property, `ticketId: String`, with no default, and it matches `Ticket.id: String` (`model/Ticket.kt:10`). There is no `model` import, no sealed interface (Q4) and no custom `NavType`.
- **`.gitkeep`** in `ui/navigation/` is deleted, as planned.
- **Anonymous rule (CLAUDE.md).** No route carries personal data or coordinates. The back stack holds only a ticket id, so saved state never stores ticket content. The KDoc warning about `reportTicket` (a PRIVATE report is not in `tickets`) matches overview.md line 104 and `getTicketById` at line 90.
- **Scope.** Untouched, as required: `MainActivity.kt`, `res/`, `model/`, `viewmodel/`, `data/`, `utils/`, the test source sets, `.idea/` and `google-services.json`. `navigation-compose` was not added (Q1, it moves to D-006).
- **MVVM, stateless/stateful, permissions, i18n, accessibility, orientation.** Not applicable: the diff has no UI, strings, resources or permissions. Route objects keep no screen state, and `TicketDetailRoute` is serialisable, so the back stack survives rotation and process death once D-006 wires it.
- **Can it be explained at the defence?** Yes. Each line maps to a plan reason:
  - the plugin generates the serializers;
  - the runtime provides the annotation;
  - 1.9.0 is pinned to Kotlin 2.2.10;
  - the detail route carries only the id.

## Blocking
None.

## Should-fix
None.

## Nit
- **N1. D-006 may silently raise the serialization pin.** `navigation-compose` depends on `kotlinx-serialization-core` itself. Gradle picks the highest version requested, so if the 2.9.x line D-006 picks asks for something newer than 1.9.0, the runtime moves past the Kotlin 2.2 pin with no warning. That brings back the metadata risk in 00-plan Q2. Fix: in the Docs phase, add one line to the overview's D-006 contract: "after adding `navigation-compose`, run `./gradlew :app:dependencies --configuration debugRuntimeClasspath` and check that `kotlinx-serialization-core` still resolves to a version built for Kotlin 2.2 (1.9.x)".
- **N2. The coding plan still names the old class path and lint id.** `01-coding-plan.md` mentions `app/build/tmp/kotlin-classes/debug/` and a `GradleDependency` warning. `state.md` records both deviations, so nothing is hidden. But a reader who follows the plan for D-006's serializer check will look in the wrong folder. Fix: when Docs writes the "Navigation (D-004)" subsection in overview.md, give the AGP 9 path (`app/build/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes/`) and say that newer-version warnings show up as `NewerVersionAvailable`. Leave the approved plan as it is.

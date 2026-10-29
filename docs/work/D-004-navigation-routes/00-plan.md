# D-004: Navigation routes
Type: feature · Task file: docs/tasks/phase-1/D-004-navigation-routes.md
Labels: none

## Problem
The phase-1 flow (dashboard -> list -> detail -> report) needs destinations that D-005 (`MainTopAppBar`) and D-006 (`NavHost` in `MainActivity`) can refer to. The course stack (kotlin-conventions, P07) uses Navigation Compose 2.8+ with `@Serializable` route classes, not string routes. The task says to use "the kotlin-serialization plugin from the version catalog", but `gradle/libs.versions.toml` has no serialization plugin or library yet. There is no Navigation Compose dependency either. `ui/navigation/` only has a `.gitkeep`.

## Outcome
- The version catalog declares the Kotlin Serialization compiler plugin (`org.jetbrains.kotlin.plugin.serialization`, `version.ref = "kotlin"`) and the `kotlinx-serialization-core` runtime. `:app` applies the plugin and depends on the runtime.
- `ui/navigation/Destinations.kt` declares four `@Serializable` routes:
  - `data object DashboardRoute`
  - `data object TicketListRoute`
  - `data class TicketDetailRoute(val ticketId: String)`
  - `data object ReportTicketRoute`
- Nothing navigates yet. D-006 adds `navigation-compose` and the `NavHost` (Q1).

## Acceptance checks
- [ ] `gradle/libs.versions.toml` has a `kotlin-serialization` plugin with `version.ref = "kotlin"`, and a `kotlinx-serialization-core` library with its own version (Q2). Both are used through `libs.` aliases. No hard-coded coordinates in `build.gradle.kts`.
- [ ] The root `build.gradle.kts` declares the plugin with `apply false`, and `app/build.gradle.kts` applies it.
- [ ] `Destinations.kt` is in package `pt.isec.amov.cityfix.ui.navigation`. Every route has `@Serializable`. The three routes with no arguments are `data object`s.
- [ ] `TicketDetailRoute` has exactly one property, `ticketId: String`. That matches `Ticket.id: String` in `model/`. It carries no `Ticket` and no other field.
- [ ] No route carries personal data or coordinates (CLAUDE.md anonymous rule).
- [ ] `./gradlew assembleDebug` and `./gradlew lint` pass after each slice, with no new lint errors.

## Approach
1. **Gradle setup (slice 1).** Add to the catalog:
   - version `kotlinxSerialization = "1.9.0"`;
   - library `kotlinx-serialization-core` (`org.jetbrains.kotlinx:kotlinx-serialization-core`);
   - plugin `kotlin-serialization` (`org.jetbrains.kotlin.plugin.serialization`, `version.ref = "kotlin"`).

   Then declare the plugin `apply false` in the root build, apply it in `:app`, and add `implementation(libs.kotlinx.serialization.core)`.

   The plugin generates the serializers. The `@Serializable` annotation itself lives in the runtime library, so the runtime is needed for `Destinations.kt` to compile. Declaring it explicitly, instead of relying on a transitive dependency of `navigation-compose`, keeps D-004 independent of D-006. It also documents the direct use of the library.

   AGP 9 has built-in Kotlin, so there is no `kotlin-android` plugin. The serialization plugin is a Kotlin compiler plugin, applied the same way as `kotlin-compose` already is.
2. **Routes (slice 2).** Create `ui/navigation/Destinations.kt` with the four routes and a short KDoc each, and delete `ui/navigation/.gitkeep`.
   - `TicketDetailRoute` carries only the id. The screen looks the ticket up in the collected `tickets` (D-003 contract), so the back stack never stores ticket content.
   - Its KDoc says it is for ids from the visible list only, and never for the id that `reportTicket` returns: a PRIVATE report is not in `tickets` (overview, "ViewModel (D-003)", review N1).

Alternatives considered:
- **String routes** (`"detail/{ticketId}"`). Not type-safe, and not the course stack.
- **A `sealed interface Route`** that all four implement. It would give D-005 one type to switch on, but Navigation's `hasRoute<T>()` works on the concrete classes. It also adds a concept the task does not ask for. It can be added later if D-005 needs it (Q4).
- **Passing the whole `Ticket`** as a route argument. This needs a custom `NavType` and puts ticket data in the saved back stack, and the task forbids it.
- **Adding `navigation-compose` here** (Q1).
- **Relying on `navigation-compose` to pull `kotlinx-serialization-core` in transitively.** It would only work once D-006 lands, and the build would break before then.

## Architecture impact
- First real file in `ui/navigation/`, and the first use of kotlinx.serialization in the project.
- New build dependencies: one compiler plugin and one small runtime library (`kotlinx-serialization-core`, about 400 KB before shrinking). Release builds have optimization disabled (`optimization { enable = false }`), so there are no keep rules to worry about yet.
- **Contract for D-005/D-006:**
  - D-006 adds `androidx.navigation:navigation-compose` (Q1, suggested 2.9.8). It builds `NavHost(startDestination = DashboardRoute)` with `composable<DashboardRoute>`, `composable<TicketListRoute>`, `composable<TicketDetailRoute>` and `composable<ReportTicketRoute>`.
  - The detail destination reads `backStackEntry.toRoute<TicketDetailRoute>().ticketId`.
  - It does not navigate to `TicketDetailRoute` after `reportTicket`.
- Package dependencies: `ui.navigation` imports nothing from `model/` (the id is a plain `String`).

## Failure modes
- **The serialization plugin does not apply under AGP 9 built-in Kotlin**, or its version does not match the Kotlin Gradle plugin that AGP uses. Mitigation: it uses `version.ref = "kotlin"`, like `kotlin-compose`, which already works. Slice 1's build shows the problem before any route code exists. If it fails, stop and record it in `state.md`; do not change the AGP or Kotlin versions without asking.
- **Runtime/compiler mismatch.** A `kotlinx-serialization-core` built with a newer Kotlin than 2.2.10 can fail with metadata errors. Mitigation: use 1.9.0, which is built with Kotlin 2.2 (Q2).
- **`@Serializable` without the plugin applied.** The code compiles, but navigation fails at runtime with "Serializer for class ... is not found". Mitigation: slice 1 applies the plugin before slice 2 uses it. The reviewer checks `app/build.gradle.kts`. D-006 confirms it at runtime.
- **Lint `GradleDependency` warning** saying a newer kotlinx-serialization exists (1.10/1.11). This is a warning, not an error. Accept it and note why in `state.md` (Q2).
- **Someone navigates to the id from a PRIVATE report**, and the detail screen shows "Ticket not found". Mitigation: KDoc on `TicketDetailRoute` and the D-006 contract in the overview.

## Risks and open questions
Recommendations are in **bold**.

- **Q1. Does `navigation-compose` belong to D-004 or D-006?** D-004's files are only `Destinations.kt`, and its checks only need `@Serializable`. D-006 owns the `NavHost` and lists D-004 as a dependency. **D-006.** D-004 adds only the serialization plugin and runtime. D-006 adds `navigation-compose` (suggested `2.9.8`, the current stable 2.9 release, so it does not need the 2.10 line). One exception: if D-005 decides `MainTopAppBar` should read the `NavBackStackEntry` itself rather than take plain flags, D-005 adds the dependency instead. Recommendation for D-005: keep the top bar stateless, with parameters such as `canNavigateBack` and `showAdd`, and let D-006 compute them, so D-005 needs no navigation library either.
- **Q2. Which `kotlinx-serialization-core` version?**
  - (a) `1.9.0`, built for Kotlin 2.2;
  - (b) `1.10.0` or `1.11.0`, the newer releases, based on Kotlin 2.3.x.

  **(a) 1.9.0**: it matches the project's Kotlin 2.2.10. I could not confirm that a 2.2.10 compiler reads 1.10+/1.11 metadata cleanly. Upgrade together with Kotlin later. Open: if Diogo wants to bump Kotlin to 2.3.x, that is a separate task.
- **Q3. Is the route property `ticketId` or `id`?** The task writes `TicketDetailRoute(ticketId)`. **`ticketId`**: it is explicit at the call site (`TicketDetailRoute(ticketId = ticket.id)`). Its type is `String`, matching `Ticket.id` (decision 0003).
- **Q4. Should the routes share a `sealed interface`?** **No** (see Alternatives). Revisit in D-005 only if it needs one.
- **Q5. Should D-004 record a decision?** The choices are the course default (type-safe routes) and a version pin. **No new decision file**: record them in `overview.md` and the report notes. If Diogo wants one, the next free number is `0007`.
- **Q6. The task's Files list names only `Destinations.kt`.** This plan also edits `gradle/libs.versions.toml`, the root `build.gradle.kts` and `app/build.gradle.kts`, which the task's goal requires. **Accept.**
- Risk: none of the routes can be exercised until D-006 wires the `NavHost`. Until then, verification is build, lint and code review.

## Out of scope
- Automated tests of any kind (Diogo's decision).
- `navigation-compose`, `NavHost`, `NavController`, `toRoute`, and `MainActivity` changes (D-006).
- `MainTopAppBar` and its route-dependent icons (D-005).
- Deep links, nested graphs, bottom navigation, and routes for later phases (login, map, operator).
- Bumping Kotlin, AGP or the Compose BOM.

## Slices
1. `build(D-004): add kotlin serialization plugin and runtime`: catalog entries, root `apply false`, `:app` plugin and dependency.
2. `feat(D-004): add type-safe navigation routes`: `ui/navigation/Destinations.kt`; delete `ui/navigation/.gitkeep`.

Each slice is verified with `./gradlew assembleDebug` and `./gradlew lint`, and becomes one commit.

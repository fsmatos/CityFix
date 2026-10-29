# D-004: Coding plan

## Files
| Path | Change | Why |
| --- | --- | --- |
| `gradle/libs.versions.toml` | edit | Add the `kotlinxSerialization` version, the `kotlinx-serialization-core` library and the `kotlin-serialization` plugin |
| `build.gradle.kts` (root) | edit | Declare the serialization plugin with `apply false`, like the other plugins |
| `app/build.gradle.kts` | edit | Apply the plugin and add the runtime dependency |
| `app/src/main/java/pt/isec/amov/cityfix/ui/navigation/Destinations.kt` | new | The four `@Serializable` routes |
| `app/src/main/java/pt/isec/amov/cityfix/ui/navigation/.gitkeep` | delete | The folder now has a real file |

No resource, `MainActivity` or test changes.

## Interfaces

Version catalog additions (keep the existing entries and their order; append these):

```toml
[versions]
kotlinxSerialization = "1.9.0"

[libraries]
kotlinx-serialization-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-core", version.ref = "kotlinxSerialization" }

[plugins]
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

Root `build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
```

`app/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}
// dependencies { ... }
    implementation(libs.kotlinx.serialization.core)
```
- Put the new `implementation` line after `libs.androidx.lifecycle.runtime.ktx`, before the test dependencies.

`ui/navigation/Destinations.kt`:

```kotlin
package pt.isec.amov.cityfix.ui.navigation

import kotlinx.serialization.Serializable

// Type-safe Navigation Compose routes for the phase-1 flow: dashboard -> list -> detail -> report.
// The NavHost that uses them is built in MainActivity (D-006).

/** Start destination: dashboard figures. */
@Serializable
data object DashboardRoute

/** List of public active tickets. */
@Serializable
data object TicketListRoute

/**
 * Detail of one ticket. Carries only the id; the screen takes the ticket from the collected
 * `tickets` list. Use ids from that list only, never the id returned by `reportTicket`
 * (a PRIVATE report is not in the anonymous list).
 */
@Serializable
data class TicketDetailRoute(val ticketId: String)

/** Provisional report form. */
@Serializable
data object ReportTicketRoute
```
- No default value for `ticketId`: a detail route without an id makes no sense.
- No `model` import: `Ticket.id` is a `String`, and the route only needs that type.

## Steps
Before every commit:
- run `./gradlew assembleDebug` and `./gradlew lint` (exit 0, no new lint errors; a `GradleDependency` "newer version available" warning for kotlinx-serialization is expected, see 00-plan Q2);
- do not stage `.idea/`;
- write commit messages with no attribution lines.

1. **Gradle setup.** Edit the catalog, the root `build.gradle.kts` and `app/build.gradle.kts` as in Interfaces.
   - Commit: `build(D-004): add kotlin serialization plugin and runtime`.
   - Verify: Gradle sync, build and lint. The build must show the plugin resolving at Kotlin 2.2.10. If the plugin fails to apply under AGP 9 built-in Kotlin, stop and record it in `state.md`; do not change AGP or Kotlin versions without asking Diogo.
2. **Routes.** Create `ui/navigation/Destinations.kt` as in Interfaces; delete `ui/navigation/.gitkeep`.
   - Commit: `feat(D-004): add type-safe navigation routes`.
   - Verify: build and lint. By reading the code:
     - all four routes have `@Serializable`;
     - three are `data object`;
     - `TicketDetailRoute` has a single `String` property, `ticketId`.

     Optional check of the plugin: confirm that `app/build/tmp/kotlin-classes/debug/pt/isec/amov/cityfix/ui/navigation/` contains `TicketDetailRoute$$serializer.class`. If it is missing, the plugin is not applied.

## Conventions
- kotlin-conventions: Navigation Compose with `@Serializable` route classes, routes in `ui/navigation/`.
- All dependencies go through the version catalog (`libs.`); no inline coordinates.
- Plain code: no sealed hierarchy, no custom `NavType`, no helper extensions (00-plan Q4).
- Routes carry no personal data or coordinates.
- Commits: Conventional Commits with `D-004`, no attribution lines.

## Do not touch
- `MainActivity.kt` (D-006), `ui/theme/`, `ui/screens/`, `ui/components/`, resources.
- `model/`, `viewmodel/`, `data/`, `utils/`.
- The existing `kotlin`, `agp` and `composeBom` versions, and every other catalog entry.
- `app/src/test/` and `app/src/androidTest/`.
- `docs/work/D-004-navigation-routes/state.md` (the orchestrator records phases), `google-services.json`, `.idea/`.

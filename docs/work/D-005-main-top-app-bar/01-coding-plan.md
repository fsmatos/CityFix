# D-005: Coding plan

## Files
| Path | Change | Why |
| --- | --- | --- |
| `gradle/libs.versions.toml` | edit | Add the `androidx-compose-material-icons-core` library (no version; the BOM sets it) |
| `app/build.gradle.kts` | edit | Add the icons dependency |
| `app/src/main/res/values/strings.xml` | edit | Titles and content descriptions in English |
| `app/src/main/res/values-pt/strings.xml` | new | The same strings in Portuguese, plus `app_name` |
| `app/src/main/java/pt/isec/amov/cityfix/ui/navigation/MainTopAppBar.kt` | new | `TopBarScreen`, `MainTopAppBar` and previews |

No changes to `MainActivity.kt` or `Destinations.kt`.

## Interfaces

Version catalog (append after `androidx-compose-material3`):

```toml
androidx-compose-material-icons-core = { group = "androidx.compose.material", name = "material-icons-core" }
```

`app/build.gradle.kts` (after `libs.androidx.compose.material3`):

```kotlin
implementation(libs.androidx.compose.material.icons.core)
```

`values/strings.xml` (add after `app_name`):

```xml
<string name="top_bar_title_dashboard">Dashboard</string>
<string name="top_bar_title_list">Tickets</string>
<string name="top_bar_title_detail">Ticket details</string>
<string name="top_bar_title_report">Report an issue</string>
<string name="cd_navigate_back">Go back</string>
<string name="cd_expand_tickets">Expand tickets</string>
<string name="cd_collapse_tickets">Collapse tickets</string>
<string name="cd_report_ticket">Report an issue</string>
```

`values-pt/strings.xml`:

```xml
<resources>
    <string name="app_name">CityFix</string>
    <string name="top_bar_title_dashboard">Painel</string>
    <string name="top_bar_title_list">Tickets</string>
    <string name="top_bar_title_detail">Detalhes do ticket</string>
    <string name="top_bar_title_report">Reportar problema</string>
    <string name="cd_navigate_back">Voltar</string>
    <string name="cd_expand_tickets">Expandir tickets</string>
    <string name="cd_collapse_tickets">Recolher tickets</string>
    <string name="cd_report_ticket">Reportar problema</string>
</resources>
```

`ui/navigation/MainTopAppBar.kt`:

```kotlin
package pt.isec.amov.cityfix.ui.navigation

// imports: Compose, Material3, material icons, stringResource, Preview, R, CityFixTheme

/** The screen the top bar is showing. One value per route in Destinations.kt. */
enum class TopBarScreen { DASHBOARD, LIST, DETAIL, REPORT }

/**
 * App top bar. Stateless: the caller says which screen is showing and receives the clicks.
 * The toggle appears on the list only; the add button everywhere except the report form.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopAppBar(
    screen: TopBarScreen,
    canNavigateBack: Boolean,
    isExpanded: Boolean,
    onBackClick: () -> Unit,
    onToggleExpandedClick: () -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val titleRes = when (screen) {
        TopBarScreen.DASHBOARD -> R.string.top_bar_title_dashboard
        TopBarScreen.LIST -> R.string.top_bar_title_list
        TopBarScreen.DETAIL -> R.string.top_bar_title_detail
        TopBarScreen.REPORT -> R.string.top_bar_title_report
    }

    TopAppBar(
        title = {
            Text(
                text = stringResource(titleRes),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        modifier = modifier,
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_navigate_back),
                    )
                }
            }
        },
        actions = {
            if (screen == TopBarScreen.LIST) {
                IconButton(onClick = onToggleExpandedClick) {
                    Icon(
                        imageVector = if (isExpanded) {
                            Icons.Filled.KeyboardArrowUp
                        } else {
                            Icons.Filled.KeyboardArrowDown
                        },
                        contentDescription = stringResource(
                            if (isExpanded) R.string.cd_collapse_tickets else R.string.cd_expand_tickets
                        ),
                    )
                }
            }
            if (screen != TopBarScreen.REPORT) {
                IconButton(onClick = onAddClick) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.cd_report_ticket),
                    )
                }
            }
        },
    )
}
```
- `IconButton` already gives a 48dp touch target.
- The toggle's description names the action the tap will perform, not the current state.
- `when` on the enum has no `else`, so a new screen will not compile until it has a title.

Previews, at the end of the file (private, each repeated in both orientations):

```kotlin
private const val PORTRAIT = "spec:width=411dp,height=891dp"
private const val LANDSCAPE = "spec:width=411dp,height=891dp,orientation=landscape"

@Preview(name = "Portrait", device = PORTRAIT, showBackground = true)
@Preview(name = "Landscape", device = LANDSCAPE, showBackground = true)
private annotation class OrientationPreviews

@Composable
private fun BarPreview(screen: TopBarScreen, canNavigateBack: Boolean, isExpanded: Boolean = false) {
    CityFixTheme {
        MainTopAppBar(screen, canNavigateBack, isExpanded, {}, {}, {})
    }
}

@OrientationPreviews @Composable private fun DashboardBarPreview() = BarPreview(TopBarScreen.DASHBOARD, canNavigateBack = false)
@OrientationPreviews @Composable private fun ListCollapsedBarPreview() = BarPreview(TopBarScreen.LIST, canNavigateBack = true)
@OrientationPreviews @Composable private fun ListExpandedBarPreview() = BarPreview(TopBarScreen.LIST, canNavigateBack = true, isExpanded = true)
@OrientationPreviews @Composable private fun DetailBarPreview() = BarPreview(TopBarScreen.DETAIL, canNavigateBack = true)
@OrientationPreviews @Composable private fun ReportBarPreview() = BarPreview(TopBarScreen.REPORT, canNavigateBack = true)
```
Format each preview over several lines in the real file. Only the annotation class is shared; do not add a separate previews file.

## Steps
Before every commit:
- run `./gradlew assembleDebug` and `./gradlew lint` (exit 0, no new lint errors);
- do not stage `.idea/`;
- write commit messages with no attribution lines.

1. **Icons dependency.** Edit the catalog and `app/build.gradle.kts`.
   - Commit: `feat(D-005): add material icons core dependency`.
   - Verify: Gradle sync, build and lint. Confirm that `./gradlew :app:dependencies --configuration debugRuntimeClasspath` shows `material-icons-core` at 1.7.8, and that the `androidx.compose.ui` and `kotlinx-serialization-core` versions have not changed.
2. **Strings.** First run `git diff origin/main origin/dev/Francisco -- app/src/main/res` (00-plan Q5). If F-001's strings are there, reuse names or merge first, and record it in `state.md`. Then edit `values/strings.xml` and create `values-pt/strings.xml`.
   - Commit: `feat(D-005): add top bar strings`.
   - Verify: build and lint, with no `MissingTranslation` error. The unused strings may give an `UnusedResources` warning until slice 3.
3. **Top bar.** Create `MainTopAppBar.kt` as in Interfaces, including the previews.
   - Commit: `feat(D-005): add MainTopAppBar`.
   - Verify: build and lint. In the Android Studio preview pane, check each screen in both orientations against the table in 00-plan: no back arrow on `DashboardBarPreview`, toggle only on the two list previews, arrow direction flips between them, no add button on `ReportBarPreview`. Read the imports: nothing from `androidx.navigation`, `viewmodel` or `model`.
   - If an icon does not resolve, stop and record it in `state.md`; use a vector drawable for that icon (00-plan Q3). Do not add `material-icons-extended` without asking.

## Conventions
- kotlin-conventions and `.claude/rules/kotlin.md`: stateless composable, `modifier` parameter, previews on the stateless composable, strings in EN + PT, content descriptions, 48dp targets.
- All dependencies through the version catalog; no inline coordinates and no version on BOM-managed libraries.
- Plain code: one enum, one composable, simple `when` and `if`. No configuration objects or helper classes.
- Commits: Conventional Commits with `D-005`, no attribution lines.

## Do not touch
- `MainActivity.kt`, and `navigation-compose` (D-006).
- `Destinations.kt` and the other catalog entries, including `composeBom`, `kotlin` and `agp`.
- `ui/theme/`, `ui/screens/`, `ui/components/`, `model/`, `viewmodel/`, `data/`, `utils/`.
- `app/src/test/`, `app/src/androidTest/`.
- `docs/work/D-005-main-top-app-bar/state.md` (the orchestrator records phases), `google-services.json`, `.idea/`.

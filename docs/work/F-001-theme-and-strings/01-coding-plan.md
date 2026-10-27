## Files
| Path | Change | Why |
| --- | --- | --- |
| `app/src/main/res/values/strings.xml` | `app_name` gets `translatable="false"`; add `welcome_message` (EN) | Localisation base, course practice |
| `app/src/main/res/values-pt/strings.xml` | New: `welcome_message` in Portuguese | PT by device language |
| `app/src/main/java/pt/isec/amov/cityfix/MainActivity.kt` | `Greeting(modifier)` uses `stringResource(R.string.welcome_message)`; drop the `name` param; update preview | No hard-coded text |
| `app/src/main/java/pt/isec/amov/cityfix/ui/theme/Color.kt` | Replace the six purple vals with three CityFix colours | Brand palette |
| `app/src/main/java/pt/isec/amov/cityfix/ui/theme/Theme.kt` | Remove dark scheme and `darkTheme`; `lightColorScheme(primary, secondary, tertiary)`; `dynamicColor` default `false`, dynamic branch light only | Light-only theme showing the brand colours |

`Type.kt`, `res/values/themes.xml` and `res/values/colors.xml` are not changed (template as taught). Touch `colors.xml` only if lint fails on it, and record that in `state.md`.

## Interfaces
- `CityFixTheme(dynamicColor: Boolean = false, content: @Composable () -> Unit)` in `pt.isec.amov.cityfix.ui.theme`.
- `Color.kt`: top-level vals `CityFixBlue`, `CityFixTeal`, `CityFixOrange` (values per Q1 in 00-plan).
- `Theme.kt`: `private val LightColorScheme = lightColorScheme(primary = CityFixBlue, secondary = CityFixTeal, tertiary = CityFixOrange)`; the commented "Other default colors to override" block from the template may stay.
- Strings: `app_name` (`translatable="false"`, EN file only), `welcome_message` (EN + PT).

## Steps
1. Strings base. In `values/strings.xml` mark `app_name` `translatable="false"` and add `welcome_message` ("Welcome to CityFix"). Create `values-pt/strings.xml` with `welcome_message` ("Bem-vindo ao CityFix"), UTF-8. Change `Greeting` to `Greeting(modifier: Modifier = Modifier)` with `Text(text = stringResource(R.string.welcome_message), ...)`; update the call in `MainActivity` and the preview. Commit: `feat(F-001): add EN/PT string resources and localise placeholder`. Verify: `./gradlew assembleDebug` and `./gradlew lint` (no `MissingTranslation` / `ExtraTranslation`); grep the touched Kotlin for string literals in `Text(`; on the emulator set the language to Portuguese, then English; rotate to landscape and back.
2. Light colour scheme. Replace the colour vals in `Color.kt`; in `Theme.kt` delete `DarkColorScheme`, the `darkTheme` parameter, the `isSystemInDarkTheme`/`darkColorScheme`/`dynamicDarkColorScheme` imports; `colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicLightColorScheme(LocalContext.current) else LightColorScheme`; set `dynamicColor = false` with a one-line comment saying why. Commit: `feat(F-001): add CityFix light colour scheme`. Verify: `./gradlew assembleDebug` and `./gradlew lint`; preview and an Android 12+ emulator show the CityFix colours; text on coloured areas readable by eye.

Record the outcome of each slice in `state.md`.

## Conventions
- Strings only through `stringResource` / `R.string`; English default, PT added in the same commit. Plain snake_case keys (`welcome_message`, `ok`, `cancel`), as in the course examples. Names that do not change between languages get `translatable="false"`.
- Icons: `contentDescription = stringResource(...)`, or `null` only for decorative icons next to text.
- 48dp: use Material 3 interactive components (`Button`, `IconButton`, `TextButton`, ...); no custom `clickable` smaller than `48.dp`.
- Inline `.dp` for layout and `.sp` for text; colours and text styles through `MaterialTheme.colorScheme.*` / `MaterialTheme.typography.*`, never hard-coded `Color(...)` in screens.
- No new dependencies.

## Do not touch
`gradle/libs.versions.toml`, `app/build.gradle.kts`, `AndroidManifest.xml`, `google-services.json`, `Type.kt`, `res/values/themes.xml`, anything outside `app/src`, and other tasks' work folders.

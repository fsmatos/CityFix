## Files
| Path | Change | Why |
| --- | --- | --- |
| `app/src/main/res/values/strings.xml` | Add shared strings (EN, default) | Localisation base |
| `app/src/main/res/values-pt/strings.xml` | New, same keys in Portuguese | PT support |
| `app/src/main/java/pt/isec/amov/cityfix/MainActivity.kt` | Greeting uses `stringResource`; drop `name` param | No hard-coded text |
| `app/src/main/java/pt/isec/amov/cityfix/ui/theme/Color.kt` | Replace template tokens with CityFix light/dark palette | Brand and contrast |
| `app/src/main/java/pt/isec/amov/cityfix/ui/theme/Theme.kt` | Explicit light/dark schemes; `dynamicColor` default false | Material 3 theme |
| `app/src/main/java/pt/isec/amov/cityfix/ui/theme/Type.kt` | Full M3 typography scale | Consistent text styles |
| `app/src/main/java/pt/isec/amov/cityfix/ui/theme/Dimens.kt` | New: `MinTouchTarget = 48.dp`, spacing constants | Accessibility rule in one place |
| `app/src/main/res/values-night/themes.xml` | New: `Theme.CityFix` with dark framework parent | No white flash in dark mode |
| `app/src/main/res/values/colors.xml` | Remove unused template colours (after grep) | Cleanup |

## Interfaces
- `CityFixTheme(darkTheme: Boolean = isSystemInDarkTheme(), dynamicColor: Boolean = false, content: @Composable () -> Unit)`.
- `object Dimens { val MinTouchTarget = 48.dp; val SpaceXs/Sm/Md/Lg/Xl }` in `pt.isec.amov.cityfix.ui.theme`.
- Palette vals in `Color.kt` are plain top-level vals; light and dark schemes defined as `private val CityFixLightColors/CityFixDarkColors` in `Theme.kt`.
- Strings (initial set, snake_case, all with PT): `app_name`, `action_ok`, `action_cancel`, `action_retry`, `action_back`, `action_close`, `cd_back`, `cd_close`, `placeholder_welcome`. Group with `<!-- Common actions -->` style comments.

## Steps
1. Strings base. Add keys to EN, create PT file (UTF-8, proper accents, escape apostrophes), change `Greeting` to `Greeting(modifier)` using `stringResource(R.string.placeholder_welcome)`, update preview. Commit: `feat(F-001): add EN/PT string resources and localise placeholder`. Verify: `./gradlew assembleDebug lint` (no `MissingTranslation`); manual: set device language to Portuguese, then English.
2. Colours and typography. Define palette tokens, build `lightColorScheme`/`darkColorScheme` with all on-colours set explicitly, set `dynamicColor = false` default, define the full `Typography`. Compute the contrast ratio of each required pair (WCAG formula) and note the values in `state.md`; adjust colours if any is below 4.5:1. Commit: `feat(F-001): add CityFix Material 3 colour scheme and typography`. Verify: `./gradlew assembleDebug lint`; check previews in light and dark; set the largest font scale and confirm the placeholder is readable; rotate to landscape and back.
3. Dimens and window theme. Add `Dimens.kt`, `values-night/themes.xml`, remove unused colours after confirming no reference with grep. Commit: `feat(F-001): add dimens, dark window theme and clean template colours`. Verify: `./gradlew assembleDebug lint`; launch in dark mode (no white flash); code review that `MinTouchTarget` is 48.dp and used by any interactive element.

Record the outcome of each slice in `state.md`.

## Conventions
- Strings only through `stringResource`/`R.string`; English default, PT in the same commit.
- Key naming: `action_*`, `cd_*` (content descriptions), `label_*`, `msg_*`, `error_*`; screens later use `<screen>_*`.
- Icons: always `contentDescription = stringResource(...)`, or `null` only for purely decorative icons next to text.
- Interactive elements at least `Dimens.MinTouchTarget` (use `Modifier.minimumInteractiveComponentSize()` or `sizeIn`).
- No new dependencies; `sp` for text, `dp` for layout.

## Do not touch
`gradle/libs.versions.toml`, `app/build.gradle.kts`, `AndroidManifest.xml`, `google-services.json`, anything outside `app/src`, and other tasks' work folders.

# F-001: Theme, strings and accessibility base
Type: feature · Task file: docs/tasks/phase-1/F-001-theme-and-strings.md
Labels: none

## Problem
The app is still the Android Studio template: purple placeholder palette, one string (`app_name`), no `values-pt`, and `MainActivity` has a hard-coded `"Hello $name!"`. Every later screen needs a Material 3 theme with the CityFix colours and a localisation base (EN default, PT) to build on, plus agreed accessibility conventions.

## Outcome
- `CityFixTheme` keeps the template trio `Color.kt` / `Theme.kt` / `Type.kt` as taught in the course, with a light-only CityFix palette (primary, secondary, tertiary) shown by default (dynamic colour off).
- `values/strings.xml` (EN, default) and `values-pt/strings.xml` (PT) with the same translatable keys; `app_name` marked `translatable="false"`.
- The placeholder screen has no hard-coded user-visible text.
- Accessibility rules (contentDescription, 48dp through Material 3 components, `sp` for text) written down in the docs for later tasks.

## Acceptance checks
- [ ] No hard-coded user-visible strings in touched code: the placeholder uses `stringResource(R.string.welcome_message)`. Check: grep for string literals in `Text(` / `contentDescription =`, plus lint (`HardcodedText` does not cover Compose, so the grep is the real check).
- [ ] Device language Portuguese: the placeholder shows Portuguese text; English (or any other language): English.
- [ ] `values/strings.xml` and `values-pt/strings.xml` have the same translatable keys: `./gradlew lint` reports no `MissingTranslation`.
- [ ] Touch targets at least 48dp: interactive elements use Material 3 components (`Button`, `IconButton`, ...), which enforce the 48dp minimum; no custom `clickable` smaller than `48.dp`. Check by code review (F-001 adds no interactive element; the rule is recorded for later tasks).
- [ ] contentDescription rule recorded in `docs/architecture/overview.md`: every `Icon`/`Image` has `contentDescription = stringResource(...)`, or `null` only when decorative next to text.
- [ ] Brand colours visible: on an Android 12+ emulator the app shows the CityFix primary colour, not the wallpaper colours. Text on primary/secondary/tertiary is readable (checked by eye in the preview and on the emulator).
- [ ] Landscape: rotating to landscape and back keeps the placeholder intact and unclipped; the manifest does not lock the orientation (confirmed: no `screenOrientation`).
- [ ] `./gradlew assembleDebug` and `./gradlew lint` pass for each slice.

## Approach
Chosen:
- Theme as taught: keep the template structure of `Color.kt` (top-level colour vals), `Theme.kt` (`lightColorScheme(primary, secondary, tertiary)` plus `MaterialTheme(colorScheme, typography, content)`) and `Type.kt` (unchanged, only `bodyLarge`). Screens use `MaterialTheme.colorScheme.*` and `MaterialTheme.typography.*`.
- Light only: remove `DarkColorScheme`, the `darkTheme` parameter and `isSystemInDarkTheme()`. Chosen over "keep the signature and ignore `darkTheme`" because an unused parameter is harder to explain at the defence than a missing one; dark mode is not required by the brief.
- `dynamicColor` parameter kept (template shape) but default `false`, and its branch uses only `dynamicLightColorScheme`. This is the one deviation from the course template: with `true`, Android 12+ devices replace the brand colours with wallpaper colours, so the CityFix palette would never be seen.
- Palette: dark enough primary/secondary/tertiary that the M3 default on-colours (white) stay readable, so no on-colour overrides are needed (proposal in Q1). Other roles (background, surface, containers) keep the M3 baseline defaults, as in the template.
- Strings: EN default plus `values-pt` with the same keys, accessed with `stringResource`. Plain snake_case keys as in the course examples (`welcome_message`). `app_name` is `translatable="false"` (same name in PT, course practice) and is not repeated in the PT file. Only strings with a consumer are added now; shared actions (OK, Cancel, ...) and icon descriptions are added by the first task that uses them (see Q2).
- Dimensions: inline `.dp` / `.sp` in composables, as in the course Compose code. No `Dimens` object, no `dimens.xml`.
- `res/values/themes.xml` and `res/values/colors.xml` stay as they are (light framework parent fits a light-only app; course projects leave `colors.xml`). Only touched if lint fails on them.

Alternatives considered:
- Full custom typography scale or bundled font: not in the course material, no requirement behind it.
- Dark scheme and `values-night/themes.xml`: not a must/should in the brief; dropped.
- Kotlin `Dimens` object with a 48dp constant: not in the course material; M3 components already guarantee 48dp.
- WCAG contrast calculations: not used in the course; readability is checked by eye.
- In-app language picker (`LocaleManager`/AppCompat per-app locales): brief asks for device language only.

## Course alignment
- Theme: ContactsApp_Prof, P6/Aula6, AMOV_SMS and slides 04_JetPackCompose ("Configuring the Material 3 theme") all keep the template trio with `lightColorScheme(primary, secondary, tertiary)` and `Type.kt` with only `bodyLarge`. This plan keeps that shape; deviations: light only (no dark branch) and `dynamicColor = false`.
- Strings: slides 04_UI_StateManagement ("No-Hardcoding Rule": user-visible text in `res/values/strings.xml`, `values-pt` loaded for Portuguese devices, fallback to default). Example AMOV_ResourcesMenusNotify: `values-pt/`, `translatable="false"` on `app_name`, plain snake_case keys. Access with `stringResource` / `R.string`.
- Dimensions: slides teach `dp` for layout and `sp` for text (scales with the accessibility font size); course Compose code uses inline `.dp`.
- Accessibility: slides mention modifiers carrying accessibility labels and `contentDescription` on `Icon` (`null` when decorative next to text); M3 `Button`/`IconButton` enforce 48dp.
- Previews: `@Preview` as in 04_JetPackCompose (no `uiMode` night preview, since there is no dark theme).
- Package layout: P07 (`model`, `viewmodel`, `data`, `utils`, `ui.navigation`, `ui.components`, `ui.screens`, `ui.theme`) already matches the project.
- Brief (`docs/brief/AMov.ProjetoKotlin_PT.pdf`, about lines 132-138): English default plus one extra language by device setting; portrait or landscape; accessibility mechanisms. No dark mode requirement.

## Architecture impact
Only `ui/theme/`, `res/values*/strings.xml` and the placeholder in `MainActivity`. No new files in Kotlin, no new layers, no new dependencies, no version catalog change.

## Failure modes
- Missing PT key: falls back to English silently; guarded by lint `MissingTranslation`.
- `app_name` added to `values-pt` by mistake while marked `translatable="false"`: lint `ExtraTranslation`; keep it only in `values/`.
- Format args differing between EN and PT (`%1$s`): none now; lint flags mismatches when added.
- Apostrophes or accents in PT: escape `'` as `\'`; file saved as UTF-8.
- Device language `pt-BR` / `pt-PT`: both resolve to `values-pt`.
- Large font scale: text uses `sp`; per-screen layouts are verified by the tasks that own them.
- Dynamic colour accidentally passed as `true` by a caller: brand colours replaced on Android 12+; only `MainActivity` calls the theme, with the default.

## Risks and open questions
- Risk: M3 baseline container roles (`primaryContainer`, `secondaryContainer`, ...) are not derived from our three colours, so components that use them (e.g. FAB, filled tonal button) may show the default purple tint. Acceptable now; revisit if a screen looks off (override the container roles then).
- Risk: `./gradlew` builds depend on AGP / compileSdk 37 installed locally; not verified here.
- Q1: Palette. Proposal: primary blue `0xFF1565C0`, secondary teal `0xFF00695C`, tertiary deep orange `0xFFBF360C` (all readable with white text). Confirm or give brand colours. Default if no answer: the proposal.
- Q2: Shared strings. Proposal: add only `welcome_message` now; actions (`ok`, `cancel`, ...) and icon descriptions come with the first screen that uses them, so no unused resources. Alternative: add a small shared set now. Default: proposal.
- Q3: Placeholder screen. Default: keep it as a translated placeholder until navigation exists (F-002 or later).

## Out of scope
Dark mode, navigation, real screens, adaptive/split landscape layouts (P07, per screen by the owning tasks), in-app language picker, custom fonts or typography scale, Firebase, feature-specific strings (added by their own tasks).

## Slices
Each slice is verified by `./gradlew assembleDebug` and `./gradlew lint`.
1. Strings base: `app_name` marked `translatable="false"`, `welcome_message` in EN, new `values-pt/strings.xml`, placeholder uses `stringResource`.
2. Light colour scheme: CityFix palette in `Color.kt`, `Theme.kt` light only with `dynamicColor = false`; `Type.kt` unchanged.

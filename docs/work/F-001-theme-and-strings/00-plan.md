# F-001: Theme, strings and accessibility base
Type: feature · Task file: docs/tasks/phase-1/F-001-theme-and-strings.md
Labels: none

## Problem
The app is still the Android Studio template: purple placeholder palette, default typography, one string (`app_name`), no `values-pt`, and `MainActivity` has a hard-coded `"Hello $name!"`. Every later screen needs a Material 3 theme, a localisation base (EN default, PT) and accessibility conventions (content descriptions, 48dp targets) to build on.

## Outcome
- `CityFixTheme` with a CityFix light and dark colour scheme, full Material 3 typography and shared dimension constants (including the 48dp minimum touch target).
- `values/strings.xml` (EN) and `values-pt/strings.xml` (PT) with the same keys: app name plus a small set of shared strings (common actions, generic content descriptions) that later tasks can reuse.
- The template greeting no longer contains hard-coded user-visible text.
- Window background does not flash white in dark mode.

## Acceptance checks
- [ ] No hard-coded user-visible strings in new or touched code (`MainActivity` greeting uses `stringResource`). Check: grep for string literals in `Text(`/`contentDescription =` plus lint.
- [ ] With device language set to Portuguese, the app name and the placeholder screen show Portuguese text; with English (or any other language) they show English.
- [ ] `values/strings.xml` and `values-pt/strings.xml` have the same keys: `./gradlew lint` reports no `MissingTranslation` (manual check; also compare the key lists by eye in review).
- [ ] Touch targets at least 48dp: `Dimens.MinTouchTarget = 48.dp` exists and the placeholder screen's interactive element (if any) uses it. Check by code review.
- [ ] Light and dark schemes: primary/onPrimary, secondary/onSecondary, error/onError, surface/onSurface pairs reach contrast ratio >= 4.5:1. Check by eye in previews plus a quick WCAG calculation (relative luminance, (L1+0.05)/(L2+0.05)) for each pair; note the ratios in `state.md`.
- [ ] Rotating the device to landscape and back (brief: portrait or landscape): the placeholder stays intact and nothing is clipped; the manifest does not lock the orientation.
- [ ] Dark mode on a device/emulator: no white flash at launch, text readable.
- [ ] Font scale set to the largest value in system settings: placeholder text stays readable and is not clipped.
- [ ] `./gradlew assembleDebug` and `./gradlew lint` pass for each slice.

## Approach
Chosen:
- Keep the existing file layout (`Color.kt`, `Type.kt`, `Theme.kt`) and add `Dimens.kt` in `ui/theme/`. Replace the template purple tokens with a civic palette (blue/teal primary, amber secondary, red error) defined as explicit light and dark schemes.
- Dynamic colour: parameter kept but default `false`, so the brand palette and the verified contrast are what the authors see and defend; it can be enabled later.
- Typography: define the full M3 scale on `FontFamily.Default` (no new font dependency).
- Strings: a minimal shared set now (common actions such as OK/Cancel/Retry/Back, generic icon descriptions), grouped with XML comments. Feature strings are added by the task that owns each screen, EN and PT together.
- Dark-mode window background: add `values-night/themes.xml` with a dark framework parent, because `Theme.CityFix` extends a Light framework theme.
- Accessibility: encode the rules as `Dimens.MinTouchTarget` and document the contentDescription rule in the docs; no wrapper components yet (nothing to wrap).

Alternatives considered:
- Add the `com.google.android.material` library for an XML Material 3 theme: extra dependency, not needed for Compose.
- Bundle a custom font: dependency/asset cost with no requirement behind it.
- Per-app language switcher (`LocaleManager`/AppCompat): not requested; device language is enough for the acceptance check.
- Dynamic colour on by default: contrast not controllable, rejected.

## Architecture impact
Only `ui/theme/` and `res/`. No new layers, no new dependencies, no version catalog change. `MainActivity` is touched only to remove the hard-coded string.

## Failure modes
- Missing PT key: falls back to English silently; guarded by lint `MissingTranslation`.
- Format args differing between EN and PT (`%1$s`): none in the initial set; check by eye when args are added (lint also flags format mismatches).
- Low contrast in dark scheme: checked by eye and a quick calculation noted in `state.md`.
- Fonts scale 2x: text uses `sp`; checked manually with the largest font scale; layouts verified later per screen.
- Device language `pt-BR`/`pt-PT`: both resolve to `values-pt`.

## Risks and open questions
- Risk: lint may flag the unused template colours in `res/values/colors.xml` (`purple_*`, `teal_*`); the plan removes those not referenced. Verify with grep before deleting.
- Risk: `./gradlew` builds depend on AGP 9.4.1 / compileSdk 37 being installed locally; not verified here.
- Q1: Confirm the palette direction (blue/teal civic) or give brand colours. Default if no answer: the proposed palette.
- Q2: Dynamic colour default off: confirm.
- Brief alignment: English is the default language and one extra language (PT) follows the device setting; portrait/landscape must work; accessibility is expected; MVVM with stateless/stateful split is graded (no impact here, theme only).
- Q3: Should the placeholder screen stay (translated) or be removed? Default: keep as a translated placeholder until navigation exists (F-002 or later).

## Out of scope
Navigation, real screens, adaptive/split landscape layouts (course P07, done per screen by the tasks that own them), in-app language picker, custom fonts, Firebase, feature-specific strings (added by their own tasks).

## Slices
Each slice is verified by `./gradlew assembleDebug` and `./gradlew lint`.
1. Strings base: EN `strings.xml`, new `values-pt/strings.xml`, `MainActivity` greeting uses `stringResource`.
2. Colour scheme and typography: new palette in `Color.kt`, explicit schemes in `Theme.kt`, full scale in `Type.kt`; contrast checked by eye and calculation.
3. Dimens and dark window theme: `Dimens.kt` (48dp target, spacing), `values-night/themes.xml`, remove unused template colours.

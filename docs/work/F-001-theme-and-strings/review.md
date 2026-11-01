# F-001: Review
Reviewer: reviewer agent · Date: 2026-10-27
Range: `0dfdb78..HEAD` (commits 7d9e489, b172c4d); `state.md` Base is eff85d0, the commits in between are plan/chore only.
Checked against: 00-plan.md, 01-coding-plan.md, brief (EN default + device-language extra language, portrait/landscape, accessibility), docs/architecture/overview.md, kotlin-conventions skill, CLAUDE.md rules.

## Verdict
Approve, no blocking findings. One should-fix (system bar icons in a light-only theme) is a direct side effect of removing the dark branch and should be fixed before push; the rest is process or nits.

The change matches the coding plan file by file: interfaces as specified (`CityFixTheme(dynamicColor: Boolean = false, content)`, `CityFixBlue`/`CityFixTeal`/`CityFixOrange`, `LightColorScheme` with the template comment block), `Type.kt`, `themes.xml`, `colors.xml`, manifest and Gradle untouched, no new dependencies, no hard-coded user-visible text left in `MainActivity.kt`, `app_name` `translatable="false"` only in `values/`, `welcome_message` in both `values/` and `values-pt/`. The extra removal of the unused `android.app.Activity` import is recorded in `state.md`. Out of scope for this task and not affected: MVVM/stateful-stateless (only a stateless `Greeting`, preview targets it), permissions, anonymous personal-data exposure. Contrast of the palette with white on-colours is fine (about 5.7:1 blue, 6.6:1 teal, 5.6:1 orange, all above 4.5:1). The missing contentDescription rule in overview.md is the docs phase's job and is not a finding.

## Blocking
None.

## Should-fix
1. **Fixed** (commit `fix(F-001): keep light system bar icons with the light-only theme`). Status bar icons unreadable on devices in dark mode (non-blocking, fix before push).
   `MainActivity` calls `enableEdgeToEdge()` with defaults, which use `SystemBarStyle.auto(...)`: the icon colour follows the device's dark-mode setting. The template was consistent because the app also went dark; after this task the app is always light, so on a device with dark mode on the status bar gets light (white) icons over the light M3 `background`, i.e. clock and battery icons nearly invisible. This is an accessibility/contrast issue and hard to explain at the defence ("why is the status bar white-on-white?").
   Fix in `MainActivity.onCreate` (still inside the files of the plan):
   ```kotlin
   enableEdgeToEdge(
       statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
       navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
   )
   ```
   with `import androidx.activity.SystemBarStyle` and `import android.graphics.Color` (framework int colour, not the Compose one). Record it in `state.md` as a deviation ("light-only theme needs light system bar style"). Verify on an emulator with dark mode on: status bar icons dark and readable, in portrait and landscape.

2. Manual acceptance checks not yet done (process, non-blocking for review, blocking for "done").
   `state.md` says the PT/EN device-language check, landscape rotation and the Android 12+ brand-colour check are left for the person. These are the only evidence for three of the 00-plan acceptance checks (brief: extra language by device setting, portrait/landscape). Fix: before `/push-task`, run them on an emulator (Settings > Language = Portugues -> "Bem-vindo ao CityFix"/your chosen text; English -> "Welcome to CityFix"; rotate both ways; Android 12+ with a coloured wallpaper still shows blue primary) and append the result to the `state.md` Log; tick the boxes in 00-plan.md.

## Nit
1. Gendered Portuguese greeting. "Bem-vindo ao CityFix" is masculine; the app addresses any citizen. Fix: `values-pt/strings.xml` -> `<string name="welcome_message">Boas-vindas ao CityFix</string>` (neutral, common in PT-PT UI). Optional, it is a placeholder.
2. **Fixed** (same commit). Inconsistent XML headers. `values-pt/strings.xml` has `<?xml version="1.0" encoding="utf-8"?>`, `values/strings.xml` does not. Fix: add the same declaration to `values/strings.xml` so both files look alike (and the UTF-8 encoding is explicit before accented PT strings arrive; `values-pt` is ASCII today).
3. **Fixed** (same commit). `MainActivity.kt` still has no newline at end of file (pre-existing from the template; the other touched files gained one). Fix: add a trailing newline when the file is next touched.
4. Landscape with a display cutout. With edge-to-edge (forced on targetSdk 35+), the placeholder sits at the top-left of the Scaffold content; when doing the manual landscape check, use an emulator with a cutout (e.g. Pixel with "Display cutout: Corner/Punch hole") to confirm the text is not under the cutout. If it is, pass `contentWindowInsets = WindowInsets.safeDrawing` to `Scaffold`. Likely fine; listed so the check is meaningful.
5. Container roles still purple (already listed as a risk in 00-plan). No action now; when the first screen uses a FAB/tonal button, override `primaryContainer`/`onPrimaryContainer` (and secondary/tertiary) in `LightColorScheme`.

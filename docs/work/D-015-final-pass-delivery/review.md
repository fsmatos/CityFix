# D-015 review: slices 1-3

Range: `7def3e9..HEAD` (6c0fbf0, a3fe6fb, ade184a, cd88a21) on `dev/Diogo`. Reviewed 2026-11-17 against 00-plan, 01-coding-plan, 03-docs-plan, the task file, CLAUDE.md, `docs/architecture/overview.md` and `kotlin-conventions`. Slices 4-6 are out of scope (they wait for the F-015/F-016/F-017 merges).

## Verdict
**Approve, with should-fix items.** The code matches slices 1-3 of the coding plan with no deviation. Nothing blocks. Two should-fix items remain: a stale "debug seed" KDoc in code, and overview passages that 03-docs-plan does not list yet. Both are comment or docs edits.

## Verification
- `./gradlew assembleDebug lint`: BUILD SUCCESSFUL. `app/build/reports/lint-results-debug.txt` has **0 errors, 14 warnings**, which matches the baseline: 7 `NewerVersionAvailable`, 5 `GradleDependency`, 1 `AndroidGradlePluginVersion`, 1 `UseKtx` (Francisco's `ImageCompression.kt`). There is no `RedundantLabel`, `UnusedResources`, `MissingTranslation` or `ExtraTranslation`. Gradle reused up-to-date outputs from the implementer's run at the same HEAD. The sources have not changed since then.
- `app/src` has no references to `DebugSeed`, `SampleCategories`, `placeholder_message`, "Coming soon", `R.color.`, `@color/` or the template colour names. `themes.xml` is a bare `Theme.Material.Light.NoActionBar` parent and uses no colours. `res/values/` now holds only `strings.xml` and `themes.xml`.
- `PlaceholderScreen` has 3 callers: `PendingValidationScreen.kt:23`, `AccountProblemScreen` (`PlaceholderScreen.kt:75`) and `PlaceholderPreview` (`:97`). All 3 pass `messageRes` by name. The signature matches the Interfaces section of the coding plan.
- Manifest: `android:label="@string/app_name"` is still on `<application>` (`:15`), so the launcher name does not change. The Maps key `android:value` is unchanged. Only the comment above it changed.
- `CityFixApplication` is left with just the lazy container. Nothing else needed `onCreate`, and no imports are left unused.
- MVVM, permissions, anonymous privacy, i18n, accessibility, orientation: these slices remove code and do not touch any of them. Both locales lost the same key, so EN/PT parity holds. The `PlaceholderScreen` icon is still decorative next to a visible heading.
- Not checked here (left to Diogo on a device, as the state log says): cold start without the seed, Pending validation / Account problem texts, launcher label.

## Blocking
None.

## Should-fix
1. **Stale "debug seed" KDoc in code.** `app/src/main/java/pt/isec/amov/cityfix/data/FirestoreMapping.kt:147` says:
   ```kotlin
   /** The `categories/{id}` document (debug seed and CategoriesRepository, D-012). */
   ```
   The seed no longer exists. The 00-plan goal is "no stale comments" in the delivered code. Slice 1's check grepped only `DebugSeed|SampleCategories`, so it missed the lower-case wording.
   **Fix:** change it to `/** The \`categories/{id}\` document, written by CategoriesRepository (D-012). */`. This is Francisco's file, so add it to the "tell Francisco" log line, or ask him to make the change. Doing it in slice 4 as a comment-only edit is also fine.
2. **The overview still describes the seed, `SampleCategories` and `placeholder_message` as present, and 03-docs-plan misses several of those places.** The docs plan covers `overview.md` around :306, :502, :858, :871 and :905. It does not cover these:
   - `:21` (`data/` list): "`DebugSeed` (debug builds only, removed before delivery)" and "`SampleCategories` (3 active + 1 inactive), now used by previews and the seed".
   - `:22` (container): "it holds the debug seed call (`DebugSeed` in `onCreate()`)".
   - `:148`: the sample tickets "are now written once into an empty database by the debug seed".
   - `:179` and `:192` (Report form): code list `data/SampleCategories.kt`, and "the sample categories reach Firestore through the debug seed".
   - `:253`: "`PlaceholderScreen.kt` and `placeholder_message` are kept (Q10 not applied)".
   - `:849` ("Debug seed (`DebugSeed.kt`)" bullet in the F-011 section).
   
   **Fix:** add these lines to 03-docs-plan "Docs to update" now, so the docs phase does not miss them:
   - `:21`: drop `DebugSeed`, `SampleTickets` becomes "preview data only", `SampleCategories` is "removed in D-015".
   - `:22`, `:148`, `:192`, `:849`: add "(removed in D-015)" or reword to the past tense.
   - `:179`: mark `SampleCategories.kt` as deleted.
   - `:253`: "`placeholder_message` removed in D-015; `PlaceholderScreen` now requires `messageRes`".
   
   `docs/report/notes.md:382/391/842/875` and decisions 0008/0009/0020 are dated history and can stay as they are. `notes.md:875` ("must be removed before delivery") should get "done in D-015" when the D-015 report section is written.

## Nit
1. **The Maps key comment states something that is not checked yet.** `AndroidManifest.xml:39` now says "No Android-app restriction, so the grader's build shows the map". The console check (R4, manual checklist item 2) has not been done. **Fix:** do the R4 check before delivery and record it in `state.md`. Until then, the comment could say "must have no Android-app restriction".
2. **The name `PlaceholderScreen` no longer fits.** No route is a placeholder now. The composable is a generic message screen used by Pending validation and Account problem, and the defence may ask why it is called "placeholder". **Fix (optional, Diogo's file):** rename it to `MessageScreen` (file `MessageScreen.kt`, preview `MessageScreenPreview`) in a `refactor(D-015)` commit and update the 2 call sites. Or keep the name and add one line to the KDoc: "(name kept from D-009, when it backed the unbuilt routes)".
3. **The slice 1 check pattern was too narrow.** For the later slices, also grep case-insensitively for the prose wording (`rg -i "debug seed|placeholder|coming soon" app/src`), not only for the identifiers. This is what found should-fix 1.

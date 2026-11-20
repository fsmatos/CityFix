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

---

# D-015 review, round 2: phone fix and slices 4-6

Range: `f6a4cb1` (phone prefix fix) and `2744ea4..HEAD` (01a276d, 953f577, d8e5881, 671312b) on `dev/Diogo`. The merge a1c3c6f (F-015/F-016) is out of scope; it was read only to check that D-015 works with it. Reviewed 2026-11-20 against 00-plan, 01-coding-plan (slices 4-6, Interfaces), CLAUDE.md, `.claude/rules/kotlin.md`, `docs/architecture/overview.md` and `kotlin-conventions`.

## Verdict
**Approve, with should-fix items.** The code matches the plan. There is one recorded deviation (`cd_report_ticket` removed), and it is justified. Nothing in the code blocks. The open risk is delivery, not code: the ZIP script has never run (S2). It must run once, before 2026-11-22, or the ZIP must be built by hand from steps 2-7 of slice 6.

## Verification
- `./gradlew assembleDebug lint`: BUILD SUCCESSFUL, **0 errors, 17 warnings**, the baseline: 8 `GradleDependency`, 7 `NewerVersionAvailable`, 1 `AndroidGradlePluginVersion`, 1 `UseKtx`. There is no `UnusedResources`, `MissingTranslation` or `ExtraTranslation`.
- `rg cd_report_ticket app/src`: nothing. `MainTopAppBar` has no `showAddAction` / `onAddClick` and no `filled.Add` import left. The previews compile.
- `git archive ... HEAD -- . ':(exclude).claude' ':(exclude)CLAUDE.md' ':(exclude)docs/work'` (written to the scratchpad, then deleted): exit 0, 298 files, none under the excluded paths. The `-ExcludeAgentFiles` pathspec works.
- `git ls-files`: no tracked `build/` directory, `local.properties`, keystore or `google-services.json` (`.gitignore` lists `app/google-services.json`). The longest tracked path is 85 characters, and no file name has non-ASCII characters. Only `.idea/.gitignore` is tracked from `.idea/`.
- Not run: the script itself (execution policy, see S2), and no device check of the FAB.

## Focus 1: `nationalPhoneDigits` and `toProfile`
- **Correct for the console data seen.** `"+351 917 234 568"`, `"+351917234568"` and `"00351917234568"` all give `"917234568"`. 9-digit app data passes through unchanged. Other values (such as `"+44..."`) come back with the whitespace removed and then fail `isPortuguesePhone`, which is the same as before.
- **Every reader goes through it.** `toProfile()` is the only mapper from `users/{uid}`. It feeds `observeProfile`, `pendingCitizens` (Approvals / `CitizenApprovalScreen`), `OperatorTicketViewModel.toContact` (Contacts) and `ProfileViewModel`. Each screen adds `+351` once, through `setup_phone_display` / `operator_ticket_phone`. Anonymous users never read `users/`, so privacy does not change.
- **Writes.** `ProfileViewModel.save()` sends `phone = state.phone.takeIf { it != profile.phone }`, and `profile.phone` is now the stripped value. As a result:
  - Saving without touching the phone (for example, only the photo or the impact toggle) writes no `phone`. The stored `"+351..."` stays in Firestore, and no code path writes `"+351"`.
  - Editing the phone writes 9 digits (`onPhoneChange` keeps digits only), so the document is normalised.
  - Behaviour change, an improvement: before, the field was loaded as `"+351 9..."` and failed `isPortuguesePhone` (`ProfileViewModel.kt:87`). `save()` then refused with `register_error_phone` (`:170`), so a console-made account could not save its profile at all. That now works. Registration (`FirestoreUsersRepository.kt:172`) and MFA enrolment (`"+351${digits}"`) do not use the function and do not change.

## Focus 2: citizen FAB
- **Visibility.** `session.canReport` is `accessRole == CITIZEN`, so only an active citizen sees it: not anonymous, pending, operator, loading, setup or account-problem users. `!showGate` is redundant, because the gate is shown to anonymous users only, but it is harmless and makes the intent clear. The screens are Dashboard, List and My tickets. It is hidden on Report, Detail, Profile and the auth screens, as Interfaces says. The route `ReportTicketRoute()` with `launchSingleTop` is the same as the old "+".
- **F-016.** The FAB does not touch the notification-permission effects, the snackbar host (M3 places the snackbar above the FAB) or F-015's `ReportTicketRoute(fromImpact = true)` path. There is no conflict.
- **Accessibility.** `ExtendedFloatingActionButton` is 56 dp high (at least 48 dp). Its text label is the merged accessible name, so the `null` icon description is correct. The label is `top_bar_title_report` in both locales. With the "+" gone, removing `cd_report_ticket` is the right call (it would have been `UnusedResources`). The deviation is recorded in the state log.
- **Landscape.** The M3 `Scaffold` places the FAB inside `contentWindowInsets = safeDrawing`, so a side navigation bar or cutout does not cover it. On the dashboard, the "See tickets" button is centred, so the end-aligned FAB does not cover it in either orientation.
- **Last card: covered.** See S1.

## Focus 3: `scripts/make-delivery-zip.ps1`
Read in full (120 lines). It matches slice 6 steps 1-8 and brief VI as the plan quotes it:
- name `AMOV2627.2018013532.2018044613` (the same as the task file);
- `git archive` of a ref, never the working tree;
- `.gradle`, `build`, `app/build`, `local.properties` and keystores are checked twice (in the tree and in the `tar -t` listing);
- `app/google-services.json` and the PDF are copied in from parameters and checked in the listing;
- it writes only to `%TEMP%` and `-OutDir`, runs no `git add`/`commit`/`push`, and reads `local.properties` only for `sdk.dir`.

Errors are loud: `$ErrorActionPreference = 'Stop'` for cmdlets, a `$LASTEXITCODE` check after every native command, `exit 1` with a red message, and a refusal to overwrite an existing ZIP. Windows paths: `Join-Path` / `Resolve-Path` everywhere, `tar.exe` stores forward slashes and the script checks that, and `sdk.dir` un-escaping (`\:`, `\\`) is correct. The findings are S2-S3 and N2-N5.

## Focus 4: `scripts/`
Approved at Gate 1 Q6 (state log 2026-11-17) and allowed by the coding plan's Conventions. The folder holds one file and is not part of the Gradle build. Fine.

## Blocking
None.

## Should-fix
1. **The FAB covers the last ticket card on List and My tickets.** Both `LazyColumn`s use `contentPadding = PaddingValues(16.dp)` (`ListScreen.kt:55`, `MyTicketsScreen.kt:59`). The FAB takes the bottom 72 dp (56 dp + 16 dp margin) at the end side and is about 180 dp wide. When the list is scrolled to the end, the last card ends 16 dp above the bottom, so the FAB covers its lower-right part: the end of the category title, the badge row on narrow phones, and part of the tap area. This follows from the layout, so the "only if the sweep shows it" condition in Interfaces is already met.
   **Fix:** in both files, `contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp)` (16 + 56 + 16). This applies for every role. It is simpler than a conditional, and the extra space at the end does not hurt operators or anonymous users. Both files are Francisco's (F-003, F-014): make it a `fix(D-015)` commit and tell him, or ask him to do it. The Dashboard needs nothing.
2. **The script has never run, and the deadline is 2026-11-22 23:59.** The parser check does not prove the behaviour of `Expand-Archive`, `tar.exe -a` or `gradlew.bat`. Slice 6 step 9 is still open.
   **Fix:** Diogo runs it once from a PowerShell window, today:
   `powershell -ExecutionPolicy Bypass -File D:\Amov\City-Fix\scripts\make-delivery-zip.ps1 -GoogleServices D:\Amov\City-Fix\app\google-services.json -Report <any.pdf> -Ref dev/Diogo -OutDir <temp folder> -SkipBuild`
   - `-ExecutionPolicy` on the command line is Process scope and overrides LocalMachine `AllSigned`. It does not override a Group Policy (MachinePolicy/UserPolicy) setting: check with `Get-ExecutionPolicy -List`. If Group Policy sets it, sign the script or follow steps 2-7 by hand.
   - Then run it again without `-SkipBuild`, delete the test ZIP, and check that `git status` is clean.
   - Record the result (entry count, size, build pass) in `state.md`. If it cannot run, write "manual ZIP" in the delivery checklist now, so nobody counts on the script on the 22nd.
3. **`-Ref main` packages the local `main`, which may be behind `origin/main`.** The final merges into `main` can be pushed from either machine (checklist step 5). If Diogo's local `main` has not been updated, the ZIP silently ships an old commit. `git archive` uses the local ref, and the script prints the commit but does not compare it.
   **Fix:** after the `rev-parse` (line 51), add:
   ```powershell
   & git -C $repo fetch origin
   if ($LASTEXITCODE -ne 0) { Fail 'git fetch failed' }
   $remote = (& git -C $repo rev-parse --verify --quiet "origin/$Ref")
   if ($remote -and $remote -ne (& git -C $repo rev-parse $Ref)) { Fail "$Ref differs from origin/$Ref: pull or push first" }
   ```
   Also print the full SHA (`rev-parse $Ref`, without `--short`) for the state log and the PDF.

## Nit
1. **The `nationalPhoneDigits` KDoc says "anything else is returned trimmed".** The code removes *all* whitespace, including inner spaces, for every input. **Fix:** "Whitespace is removed, then a leading +351 / 00351; other values are returned as they are otherwise." Not handled, and fine to leave as it is: `"351912345678"` without `+`, and hyphens. Console data has not shown these.
2. **Path length in the verify build.** `%TEMP%\AMOV2627.2018013532.2018044613-verify-yyyyMMdd-HHmmss\CityFix\` is about 100 characters before Gradle's `app\build\intermediates\...` and Kotlin incremental caches. That can go past MAX_PATH (260) on Windows. The script would then fail loudly, but as a false negative. **Fix:** use a short folder for verification only, for example `Join-Path $env:TEMP "cfv-$stamp"`, and the same for the stage (`cfs-$stamp`). The ZIP name does not change.
3. **Staging copies are left in `%TEMP%`.** The stage folder (with a copy of `google-services.json` and the PDF) stays after both success and failure. It is not in git, so this is not a secret leak, but it is clutter with an API key in it. **Fix:** after the step-4 checks pass, `Remove-Item -Recurse -Force $stage`, and print its path in `Fail` so it can be removed by hand. Keep the verify folder, as now, because of the Gradle lock.
4. **`gradlew` loses its execute bit.** A ZIP made with `tar.exe` from extracted Windows files has no Unix mode. A grader on macOS/Linux who runs `./gradlew` from a shell gets "permission denied" (Android Studio is not affected). **Fix:** add one line to the PDF's "how to run": "On macOS/Linux: `chmod +x gradlew`."
5. **The verify build needs Java.** `gradlew.bat` fails with "JAVA_HOME is not set" if Java is not on PATH. That happens on machines with only Android Studio's `jbr`. **Fix:** like the `ANDROID_HOME` fallback, if `$env:JAVA_HOME` is empty, try `"$env:ProgramFiles\Android\Android Studio\jbr"` when it exists. Otherwise `Fail` with "set JAVA_HOME to Android Studio's jbr folder".

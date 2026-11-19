# D-015: Final pass: landscape, accessibility, strings, delivery ZIP
Type: refactor (clean-up) + delivery · Task file: docs/tasks/phase-2/D-015-final-pass-delivery.md
Labels: -

## Problem
Brief V asks for English by default plus PT from the device, portrait and landscape without loss, top bars / icons / FABs that change with the context, accessibility, and MVVM with a stateless/stateful split. Brief VI asks for one ZIP named `AMOV2627.2018013532.2018044613`. The ZIP must not contain `.gradle`, `build`, `app/build` or `local.properties`, and it must contain the PDF report. Each format error or leftover folder costs up to 5%. The deadline is 2026-11-22 23:59, with -5% per hour late.

What the code looks like today (`dev/Diogo` at `ab11ae0`, read on 2026-11-17; the brief PDF was read):

**Strings**
- `values/strings.xml` and `values-pt/strings.xml` have the same translatable keys. The only differences are three keys that are `translatable="false"` in `values/` and so need no PT copy: `app_name` (values/strings.xml:3), `setup_phone_prefix` (:227) and `setup_phone_display` (:228). `app_name` is **not** really missing: lint `MissingTranslation` skips non-translatable strings. Adding it to `values-pt` would raise lint's untranslatable-string warning. See Q3.
- Composables have no hard-coded user-visible text. Every string literal in `ui/` belongs to `@Preview` sample data (for example `ListScreen.kt:72-122`, `OperatorTicketScreen.kt:539-713`, `MessageThread.kt:161-181`) or to a `Log` tag or message in `data/`. Lint's `HardcodedText` only checks XML layouts, and this app has none, so the code was grepped by hand instead.
- Leftover: `placeholder_message` ("Coming soon.", values/strings.xml:208, values-pt:210) is only the default of `PlaceholderScreen(messageRes = ...)` (`PlaceholderScreen.kt:35`). The only use left is `PlaceholderPreview` (:98). Runtime screens never show it, but it is placeholder text in the delivery.

**Accessibility**
- No `contentDescription` is set from a literal. Every `contentDescription = null` is on a decorative icon next to a text that says the same thing: `ShowScreen.kt:486` (tile with a label below), `PlaceholderScreen.kt:41`, `OperatorPanelScreen.kt:215` (the chip has its own description, :218), `CategoriesScreen.kt:136` (the FAB text says "Add category"), `AttachmentImage.kt:75` (play overlay), `TicketMap.kt:49`. No change is needed.
- Touch targets: top-bar and message `IconButton`s (48 dp by default). The attachment remove button is `.size(48.dp)` (`AttachmentGallery.kt:93`). Radio rows use `heightIn(min = 48.dp)` (`ReportScreen.kt:470`). The category switch uses `minimumInteractiveComponentSize()` (`CategoriesScreen.kt:190`). The `.size(18.dp)` hits are progress spinners inside buttons (`ChangeCredentialScreen.kt:164`, `OperatorTicketScreen.kt:384`, `RegisterScreen.kt:319`), not targets. No change is needed.
- Contrast: the badge colours are dark on white (`Color.kt`: #1565C0, #00695C, #BF360C, #2E7D32). On-device check only (Accessibility Scanner).

**Rotation**
- Form state lives in ViewModels (`*UiState`). Every `mutableStateOf` in `ui/` uses `rememberSaveable`: menu and dropdown flags, and the camera temp paths (`ReportScreen.kt:106`, `ProfileScreen.kt:71`, `RegisterScreen.kt:86`, `OperatorTicketScreen.kt:86`). Dialog text sits in the UiState (`dialogText`, `openDialog`). The one plain `remember { mutableStateOf }` is `MainActivity.kt:258` (`hasPrecise`), which is read again on every resume. No change is needed. Rotation is still checked on the device.
- Landscape: `ShowScreen`, `CitizenApprovalScreen`, `ReportScreen`, `DashboardScreen` and `MessageThread` split on orientation. Every form uses `FormColumn` (`LoginScreen.kt:192`: `verticalScroll` + `imePadding`). Every list is a `LazyColumn`.

**FAB / top bar**
- The top bar title changes with the destination. Its actions change with role and context: back, "+" report for active citizens (`MainActivity.kt:325`), menu by role, Log in / Log out. The only FAB is `CategoriesScreen.kt:134` (operator). Brief V names "botões flutuantes ... dinâmicos" explicitly. See Q5.

**Clean-up**
- `data/DebugSeed.kt` (whole file, marked "DEBUG ONLY ... before delivery (D-015)") and its call in `CityFixApplication.kt:84-90`, plus imports :4 and :20. The grader builds a debug APK, so `FLAG_DEBUGGABLE` is set and the seed would run if their database were empty.
- `data/SampleCategories.kt`: used only by `DebugSeed` (:38). It is dead once the seed goes.
- `data/SampleTickets.kt`: still used by `ShowScreenPrivacyPreview` (`ShowScreen.kt:638`). Keep it. Its KDoc (:12) says "Phase-1 sample tickets ... (no Firebase yet)", which is stale.
- `LocalTicketsRepository`: **already deleted** by F-011 (decision 0009). There are no uses in `app/src`.
- Stale comments: `MainActivity.kt:527` ("Placeholders: the later tasks replace the content of these destinations") and the `PlaceholderScreen` KDoc (`PlaceholderScreen.kt:26-30`, which lists screens built since then). `AndroidManifest.xml:40` says "Paste the project's key here", but the real key is already there (:43).
- Lint baseline (last report in `app/build/reports/lint-results-debug.txt`, date unknown; D-013 recorded the same numbers): **0 errors, 22 warnings**.
  - 13 warnings are dependency-version notices (`libs.versions.toml`). Leave them: no upgrades 5 days before delivery.
  - `RedundantLabel`: `AndroidManifest.xml:22`.
  - 7 `UnusedResources`: the whole of `values/colors.xml` (template colours; `themes.xml` uses none of them).
  - `UseKtx`: `ImageCompression.kt:87` (Francisco's file).
- Tests: there is no `app/src/test` or `app/src/androidTest`. The "test documents" are Firestore data: the `dev-citizen-1`, `citizen-1` and `citizen-2` stand-ins (decision 0013) and any `debug` collection left by F-010.

**Not on this branch yet**
- F-015 (impact detection), F-016 (notifications) and F-017 (report) have no work folders on `dev/Diogo`. They will bring new strings, a notification permission and new UI. The strings and screen sweep must run again after they reach `main` and `main` is merged into `dev/Diogo`.

**Delivery facts**
- `app/google-services.json` is ignored (`.gitignore:20`, decision 0012 Q7). A `git archive` of `main` will not contain it, so it must be copied in by hand.
- The Maps key is committed in the manifest (`AndroidManifest.xml:43`, F-012 "as in class"). Decision 0014 says it must have **no** Android-app (SHA-1) restriction, or the grader sees grey tiles.
- Firestore (and Storage) run in **test mode until 2026-12-10** (overview "Security rules"). After that date every request is refused. The defence (23-27 Nov) is covered, but a grader running the ZIP later is not. See Q7.
- SMS MFA uses console test numbers. The debug SHA-1 is registered for the authors' keystores only. On the grader's machine (another debug keystore), Firebase falls back to reCAPTCHA verification. See R3.

## Outcome
- The delivered code has no debug seed, no dead sample data, no "Coming soon" text and no stale "placeholder" comments. `./gradlew lint` shows 0 errors, and the warnings drop from 22 to 14 (13 version notices plus `UseKtx` in Francisco's file, unless he fixes it).
- Every user-visible string exists in EN and PT, checked again after F-015/F-016 are merged.
- Every screen of every role has been checked on a device in portrait and landscape, EN and PT, with TalkBack. Small defects are fixed in their own commits. Larger ones are listed for the report's improvements.
- The test accounts (active citizen, pending citizen, operator, plus MFA test numbers) exist in Firebase and are listed in the report.
- `AMOV2627.2018013532.2018044613.zip` is built from `main` after both branches are merged. It contains the project folder with `app/google-services.json` and the PDF report. It has no `.gradle`, `build`, `app/build` or `local.properties`. Its unzipped copy passes `gradlew assembleDebug`. It is uploaded to Nonio before 2026-11-22 23:59.

## Behavior that must not change
- Every screen, role guard, redirect, gate, report, grouping, lifecycle action, upload and the proximity watch behave as today. The clean-up removes only debug-only start-up code and dead files.
- The privacy preview (`ShowScreenPrivacyPreview`) keeps using `SampleTickets`.
- `AccountProblemScreen` and `PendingValidationScreen` show the same texts. They already pass their own `messageRes`.
- The Firestore data the app reads is unchanged. The seed only ever wrote when `stats/summary` did not exist, and it exists in the shared database.

## Acceptance checks
- [ ] `grep -r "DebugSeed\|SampleCategories\|placeholder_message" app/src` finds nothing. `SampleTickets` is used only by `ShowScreen.kt` previews.
- [ ] `./gradlew assembleDebug` passes after each slice. `./gradlew lint` shows 0 errors, and no `RedundantLabel` or `UnusedResources` warnings.
- [ ] After merging F-015/F-016: every `<string>` / `<plurals>` name in `values/` that is not `translatable="false"` also exists in `values-pt/` (key diff empty), and no new `Text("...")` or literal `contentDescription` in `ui/`.
- [ ] Device sweep (table in 01-coding-plan.md, "Manual sweep") done for anonymous, pending citizen, active citizen and operator. Rotating with typed data keeps it (report form, register, login, profile, change e-mail/password, category dialog, request-info / reopen / clarification dialogs, message draft). PT texts fit (no clipping) in both orientations. TalkBack reads every control.
- [ ] Test accounts exist and log in with MFA: one active citizen, one pending citizen, one operator.
- [ ] The ZIP is named exactly `AMOV2627.2018013532.2018044613.zip` and was built from `main` at a recorded commit. Its listing shows no `.gradle/`, `build/`, `app/build/` or `local.properties`. It contains `app/google-services.json` and the PDF.
- [ ] The unzipped copy in a fresh temp folder passes `gradlew assembleDebug`. The APK installs, logs in and shows the map.
- [ ] Uploaded to Nonio before 2026-11-22 23:59. The upload time is recorded in `state.md`.

## Approach
Three tracks. The code track goes first, on `dev/Diogo`. The sweep and the ZIP follow, in the order of the timeline below.

1. **Code clean-up now (slices 1-3).** Remove the seed and the dead sample data, remove the placeholder default and stale comments, and clear the resource/manifest lint warnings. Each slice is one commit with the build green. These do not depend on Francisco's remaining tasks.
2. **Sweep after merge (slice 4, and slice 5 if Q5 says yes).** When Diogo asks for the merges (F-015/F-016 into `main`, then `/merge` into `dev/Diogo`), re-run the string key diff and the hard-coded text grep, then do the device sweep. Every defect fixed in code is its own `fix(D-015): ...` commit, logged in `state.md`. What is not fixed goes to the report's improvements (handed to F-017).
3. **Delivery (manual, with an optional script, slice 6).** After the final merge of both branches into `main` (only when Diogo asks), build the ZIP from `main`. Then copy in `google-services.json` and the PDF, check the exclusions, unzip it in a new temp folder, build it once, and upload it.

Alternatives considered:
- *Keep `DebugSeed` behind `FLAG_DEBUGGABLE`*: rejected. The grader builds debug, so the flag does not protect anything. The file says to delete it. Decisions 0013 and the overview handover say D-015 removes it.
- *Delete `SampleTickets` too and inline a privacy-preview ticket into `ShowScreen`*: rejected. It is Francisco's preview file, it adds churn, and the preview stays valid.
- *Add `app_name` to `values-pt`*: not recommended (Q3). It is `translatable="false"`, so lint would warn, and the brand name is the same in PT.
- *Build the ZIP with `git archive --format=zip` directly*: it cannot add the ignored `google-services.json` or the PDF in one step. It is fine as the first step: archive to a staging folder, copy the two files, then zip.
- *Compress-Archive (Windows PowerShell 5.1)*: older versions wrote `\` path separators, which break on macOS/Linux unzip. Use `tar.exe -a -c -f` (bsdtar, built into Windows 10), which writes `/`. The script lists the entries to check this.

## Architecture impact
- None. No new classes, routes, permissions or dependencies. `data/` loses `DebugSeed.kt` and `SampleCategories.kt`. `CityFixApplication` loses its `onCreate` override. `values/colors.xml` is deleted.
- If Q6 says yes: a new top-level `scripts/` folder holding `make-delivery-zip.ps1`. It is not part of the app module and never holds secrets: the paths to `google-services.json` and the PDF are parameters. Note: the "no new top-level folders without asking" rule in `.claude/rules/kotlin.md` is about the package, but Q6 asks anyway.

## Failure modes
- Removing `onCreate` also removes `super.onCreate()`. That is fine: the default `Application.onCreate` is empty, and the `by lazy` properties are unaffected.
- Deleting `colors.xml` breaks the build if some XML references `@color/...`. Lint says none does, and the build verifies it.
- After F-015/F-016, new PT keys are missing: `MissingTranslation` is a lint **error**, so the slice 4 lint run catches it.
- ZIP:
  - `google-services.json` is forgotten: `assembleDebug` of the unzipped copy fails (google-services plugin). The script fails early if the file is missing.
  - The PDF is not ready by 11-21: the ZIP waits for F-017. Q8 sets a cut-off.
  - The ZIP is built from a branch, not `main`: the script takes `-Ref main` by default and records the commit.
  - The verification build creates `build/`, `.gradle/` and `local.properties` in the verify folder. Those are never zipped, because the ZIP is made from the staging folder before any build. To build without writing `local.properties`, set `ANDROID_HOME` in the environment.
  - Executable bit on `gradlew` lost: harmless on Windows and in Android Studio. A Unix grader may need `chmod +x gradlew` (one line in the report's "how to run").
- Firestore test mode expires on 2026-12-10: the app shows empty lists and errors for anyone running it later (Q7).

## Risks and open questions
- **R1, timeline.** F-015, F-016 and F-017 are not merged. Slices 4-5, the sweep and the ZIP depend on them and on Diogo asking for the merges. Target: merges on 11-20/11-21, ZIP on 11-21, upload by 11-22 midday. Leave the evening as a buffer.
- **R2, Francisco's files.** Ownership was inferred from the work items: the planner has no shell, so `git log` could not be run. Confirm with `git log --format=%an -- <file>`. Francisco's files touched here are `CityFixApplication.kt` (F-010), `DebugSeed.kt` (F-011), `SampleCategories.kt` (F-006, created in Diogo's `data/` folder), `SampleTickets.kt` KDoc (D-003 or F-005: confirm), `AndroidManifest.xml` (shared; F-012 key comment), and `strings.xml` in both locales (shared; F-001). All are removals or comment edits. Tell Francisco before pushing.
- **R3, grader login.** SMS MFA with test numbers on a build signed with the grader's debug key: Firebase cannot use Play Integrity without that SHA-1, so it falls back to a reCAPTCHA page, which normally works on an emulator with Chrome. This cannot be tested on Diogo's PC with the same keystore. Mitigation: the report's "how to run" says a browser check may appear. If possible, try once on Francisco's machine with a fresh debug keystore. Do not add `setAppVerificationDisabledForTesting` (it only works with test numbers and changes D-009's auth code).
- **R4, Maps key.** It must have no SHA-1 restriction (decision 0014). Check in the Google Cloud console (manual).
- **R5, test data.** The stand-in tickets and users (`dev-citizen-1`, `citizen-1`, `citizen-2`) can confuse the defence (Q4).

### Open questions for Gate 1 (recommended answer first)
1. **Order: clean-up slices now, sweep after merges?** Recommended: yes. Slices 1-3 now on `dev/Diogo`. Slice 4 (re-check strings, fix sweep defects) after Diogo asks for F-015/F-016 to be merged into `main` and runs `/merge`.
2. **`SampleTickets`: keep (KDoc updated) or delete?** Recommended: keep. One privacy preview uses it.
3. **`app_name` in `values-pt`?** Recommended: no. It is `translatable="false"`, the name is the same in PT, and adding it causes a lint warning. Correct the task file's statement in the docs phase instead.
4. **Stand-in Firestore data (`dev-citizen-1`, `citizen-1`, `citizen-2`, any `debug` collection): delete before the defence?** Recommended: yes. Diogo deletes them by hand in the console (with Francisco's OK), after both test devices are signed out. Then decrement or reset `stats/summary`, or leave the counters: deleting tickets does not update them, so the dashboard would disagree with the list. Simplest: delete only the stand-in **users** and the `debug` collection, and keep the tickets.
5. **Citizen FAB (brief V "botões flutuantes dinâmicos")?** Recommended: yes, small. Add an `ExtendedFloatingActionButton` "Report an issue" to the `Scaffold` in `MainActivity` for active citizens on Dashboard, List and My tickets, and drop the top-bar "+" (`showAddAction`), so there is one entry point that changes with the role and the screen. It reuses `cd_report_ticket` / `top_bar_title_report`, needs no new strings, and touches only Diogo's files (`MainActivity.kt`, `MainTopAppBar.kt` and its previews). Alternative: keep the "+" and add nothing, and the report argues that the Categories FAB plus the role-based top-bar actions meet V.
6. **Add `scripts/make-delivery-zip.ps1`?** Recommended: yes. It is PowerShell because both authors build on Windows, and it is about 60 lines: `git archive` of `-Ref` (default `main`) into a temp staging folder, copy `-GoogleServices <path>` to `app/` and `-Report <pdf>` to the root, fail if `.gradle`, `build`, `app/build` or `local.properties` exist, `tar.exe -a -c -f` the ZIP, list the entries, expand them into a second temp folder, and run `gradlew.bat assembleDebug` with `ANDROID_HOME` set. No secrets in git: both files are parameters. The script is committed on `dev/Diogo` and reaches `main` with the merge. Alternative: the same steps by hand from the checklist in 01-coding-plan.md.
7. **Firestore/Storage test-mode expiry (2026-12-10).** Recommended: in the console, move the test-mode date of both rules to 2027-03-01 before the upload. This is a change to the deployed rules, so it needs Diogo's explicit OK, and Diogo makes it himself in the console. Claude does not deploy anything. Alternative: leave it and state in the report that the database closes on 2026-12-10.
8. **ZIP layout and contents.** Recommended:
   - The ZIP root holds `CityFix/`, the whole tracked repo from `git archive` (docs, `.claude/`, rules drafts included), plus the PDF at the root: `AMOV2627.2018013532.2018044613_Relatorio.pdf`, the name F-017 suggests.
   - Ask Francisco whether to leave out `.claude/`, `CLAUDE.md` and `docs/work/`. The brief does not forbid them. Excluding them is one `git archive` pathspec each.
   - Cut-off: if the PDF is not final by 2026-11-22 18:00, ZIP the latest export anyway.
9. **Test accounts in git?** Recommended: no passwords in the repo. In `docs/`, list only roles and what each account shows. E-mails, passwords and MFA test numbers with codes go only in the PDF (they are throwaway test credentials made for the grader).
10. **Remaining lint warnings.** Recommended: leave the 13 dependency-version notices (no upgrades 5 days before delivery). Ask Francisco to apply the `UseKtx` fix in `ImageCompression.kt:87` (`bitmap.scale(...)`) in his own branch, or leave it.

## Out of scope
- Dependency upgrades, new features beyond Q5, rules deploys (Q7 is a console date change by Diogo only), F-015/F-016/F-017 work, rewriting `firestore.rules`.
- Fixing large layout problems found in the sweep. Those are listed in the report's improvements instead.
- Merging into `main` or pushing `main`: only when Diogo asks in the conversation.

## Slices
1. `chore(D-015): remove the debug seed and its sample categories`: delete `DebugSeed.kt` and `SampleCategories.kt`, remove the `onCreate` override and the two imports in `CityFixApplication.kt`, refresh the `SampleTickets` KDoc.
2. `chore(D-015): drop the placeholder default text`: `PlaceholderScreen` takes a required `messageRes`, remove `placeholder_message` from both locales, fix the stale KDoc and the `MainActivity.kt:527` comment, and the preview passes a real message.
3. `chore(D-015): clear the manifest and resource lint warnings`: remove the activity `android:label` (`RedundantLabel`), delete `values/colors.xml` (7 `UnusedResources`), fix the stale Maps key comment.
4. `fix(D-015): ...` (after merges, 0..n commits): strings parity and the hard-coded text grep re-run on merged code, and small sweep fixes, one commit each.
5. (Q5) `fix(D-015): show the report action as a floating button for citizens`: FAB in `MainActivity`'s `Scaffold`, remove `showAddAction` from `MainTopAppBar`.
6. (Q6) `chore(D-015): add the delivery ZIP script`: `scripts/make-delivery-zip.ps1`.
Manual (Diogo, not commits): device sweep, test accounts, console date (Q7), stand-in cleanup (Q4), ZIP build from `main`, verification build, Nonio upload.

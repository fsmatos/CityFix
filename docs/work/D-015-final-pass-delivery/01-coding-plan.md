# D-015 coding plan

Owner column: **F** = Francisco's file (inferred from the work items; confirm with `git log --format=%an -- <file>`), **D** = Diogo's file, **S** = shared. Tell Francisco about every F/S change before `/push-task`.

## Files
| Path | Change | Why | Owner |
| --- | --- | --- | --- |
| `app/src/main/java/pt/isec/amov/cityfix/data/DebugSeed.kt` | delete | Debug-only seed, marked for removal in D-015 (F-011 Q5, decision 0013). | F |
| `app/src/main/java/pt/isec/amov/cityfix/data/SampleCategories.kt` | delete | Only `DebugSeed` used it (`DebugSeed.kt:38`). | F (created by F-006 in D's folder) |
| `app/src/main/java/pt/isec/amov/cityfix/CityFixApplication.kt` | remove `onCreate` override (:84-90) and imports `android.content.pm.ApplicationInfo` (:4), `data.DebugSeed` (:20) | The seed call goes. | F |
| `app/src/main/java/pt/isec/amov/cityfix/data/SampleTickets.kt` | KDoc only (:12) | Now it is preview data only (privacy preview in `ShowScreen`), not "phase 1, no Firebase yet". | confirm (D-003?) |
| `app/src/main/java/pt/isec/amov/cityfix/ui/screens/PlaceholderScreen.kt` | `messageRes` becomes a required parameter (no `R.string.placeholder_message` default, :35). Update the KDoc (:26-30): "simple message screen used by Pending validation and Account problem". `PlaceholderPreview` (:98) passes `messageRes = R.string.pending_message`. | No "Coming soon." left in the delivery. | D (D-009) |
| `app/src/main/java/pt/isec/amov/cityfix/MainActivity.kt` | comment at :527 ("Placeholders: the later tasks replace...") removed or reworded ("Profile and credential changes, signed-in users only."); slice 5 adds the FAB | Stale comment; Q5. | D |
| `app/src/main/res/values/strings.xml`, `values-pt/strings.xml` | remove `placeholder_message` (:208 / :210); slice 4 may add keys for merged code | Unused after slice 2. | S |
| `app/src/main/AndroidManifest.xml` | remove `android:label` on `<activity>` (:22); comment :40 becomes "Google Maps key, as in class (P10). No Android-app restriction, so the grader's build shows the map (decision 0014)." | `RedundantLabel`; stale comment. Key value untouched. | S (F-012 key) |
| `app/src/main/res/values/colors.xml` | delete | 7 `UnusedResources` (template colours; `themes.xml` uses none). | S (template) |
| `app/src/main/java/pt/isec/amov/cityfix/ui/navigation/MainTopAppBar.kt` | (slice 5, Q5) remove `showAddAction`, `onAddClick` and the "+" `IconButton` (:125-132); update previews | The FAB replaces the "+" action. | D |
| `scripts/make-delivery-zip.ps1` | (slice 6, Q6) new | Repeatable ZIP build and check. | D |

## Interfaces
- `PlaceholderScreen(@StringRes titleRes: Int, @StringRes messageRes: Int, modifier: Modifier = Modifier, actions: @Composable ColumnScope.() -> Unit = {})`. `messageRes` moves before `modifier` (required parameters first). Both callers use named arguments, so neither call site changes.
- (Q5) `MainTopAppBar(...)` loses `showAddAction: Boolean` and `onAddClick: () -> Unit`. `MainActivity`'s `Scaffold` gains:
  ```kotlin
  floatingActionButton = {
      if (showReportFab) ExtendedFloatingActionButton(
          onClick = { navController.navigate(ReportTicketRoute) { launchSingleTop = true } },
          icon = { Icon(Icons.Filled.Add, contentDescription = null) },
          text = { Text(stringResource(R.string.top_bar_title_report)) },
      )
  }
  ```
  with `val showReportFab = session.canReport && !showGate && screen in setOf(TopBarScreen.DASHBOARD, TopBarScreen.LIST, TopBarScreen.MY_TICKETS)`.
  - The FAB label is the accessible name, so the icon stays `null`.
  - Check that the last list item is not hidden under the FAB: add `contentPadding(bottom = 88.dp)` to the three `LazyColumn`s only if the sweep shows overlap. That would be a further change in `ListScreen.kt` (F-003, F's file) and `MyTicketsScreen.kt` (F-014, F's file), so tell Francisco.
- (Q6) `scripts/make-delivery-zip.ps1 -GoogleServices <path> -Report <pdf> [-Ref main] [-OutDir <dir>] [-SkipBuild]`.

## Steps

### Slice 1: `chore(D-015): remove the debug seed and its sample categories`
1. Delete `data/DebugSeed.kt` and `data/SampleCategories.kt`.
2. In `CityFixApplication.kt`, remove the whole `override fun onCreate()` block and the imports `android.content.pm.ApplicationInfo` and `pt.isec.amov.cityfix.data.DebugSeed`. The class KDoc is unchanged (it never mentions the seed).
3. `SampleTickets.kt:12` KDoc: "Sample tickets near Pr. Republica for the previews (`ShowScreenPrivacyPreview`: a raw ticket with author, follower and exact location). Not written to Firestore. Times are fixed UTC epoch ms."
4. Verify:
   - `rg "DebugSeed|SampleCategories" app/src` finds nothing.
   - `./gradlew assembleDebug` passes. Kotlin also reports any unused import as a warning, so read the build output.
   - Install, cold-start the app: it opens on the dashboard as before, and Logcat has no `DebugSeed` tag.

### Slice 2: `chore(D-015): drop the placeholder default text`
1. `PlaceholderScreen.kt`:
   - Make `messageRes` required (the parameter order is in Interfaces).
   - New KDoc: "A simple message screen: decorative icon, heading, message and optional [actions]. Used by Pending validation and Account problem. Stateless."
   - `PlaceholderPreview` passes `titleRes = R.string.pending_title, messageRes = R.string.pending_message`.
2. Remove `placeholder_message` from `values/strings.xml:208` and `values-pt/strings.xml:210`.
3. `MainActivity.kt:527`: replace the comment with "Profile and credential changes: any signed-in user with a profile."
4. Verify:
   - `rg "placeholder_message" app/src` finds nothing.
   - `assembleDebug` passes.
   - On the device, Account problem (e.g. offline with a missing profile) and Pending validation show the same texts as before.

### Slice 3: `chore(D-015): clear the manifest and resource lint warnings`
1. `AndroidManifest.xml`:
   - Delete `android:label="@string/app_name"` from `<activity>` (:22). The application label (:15) still names the launcher entry.
   - Reword the comment at :40.
   - Do not touch the key value.
2. Delete `app/src/main/res/values/colors.xml`.
3. Verify:
   - `./gradlew lint`: 0 errors, and no `RedundantLabel` or `UnusedResources` in `app/build/reports/lint-results-debug.txt`. Record the new warning count in `state.md` (expected 14).
   - `assembleDebug` passes.
   - The launcher still shows "CityFix".

### Slice 4: `fix(D-015): <defect>` (after Diogo asks for the merges and `/merge` brings F-015/F-016 in; 0..n commits)
1. **Strings parity.** Compare the `name="..."` lists of both files, ignoring `translatable="false"` entries. Any missing PT key: add a PT translation next to the EN group, with the same comment header. `./gradlew lint` must have no `MissingTranslation` / `ExtraTranslation`.
2. **Hard-coded text grep** on the merged `ui/` and on any notification code:
   - `Text\(\s*"`
   - `contentDescription = "`
   - `setContentTitle\("`, `setContentText\("`, `NotificationChannel\([^)]*"`
   - `Toast.makeText\([^)]*"`
   
   Anything user-visible moves to `strings.xml` in both locales. Preview data is fine.
3. **`contentDescription = null` review** on new code: it is only allowed when a visible label says the same thing.
4. **Rotation review** on new code: `remember { mutableStateOf` holding user input becomes ViewModel state or `rememberSaveable`.
5. **Device-sweep defects** (table below) that are small (one file, no new feature): one commit each, `fix(D-015): <what>`. Log each in `state.md`. Larger ones go to "Report notes" in 03-docs-plan.md for F-017.
6. Verify each commit with `assembleDebug`, plus `lint` at the end of the slice.

### Slice 5 (Q5): `fix(D-015): show the report action as a floating button for citizens`
1. `MainActivity.kt`: add `floatingActionButton` to the `Scaffold` (Interfaces). Remove `showAddAction` / `onAddClick` from the `MainTopAppBar` call (:325, :331-333).
2. `MainTopAppBar.kt`: remove the parameters and the "+" `IconButton`, and update the previews.
3. Verify:
   - Active citizen: the FAB shows on Dashboard, List and My tickets. It is hidden on Report, Detail and Profile.
   - Hidden for anonymous, pending and operator users, and while the gate shows.
   - TalkBack reads "Report an issue".
   - In landscape it does not cover the dashboard button or the last list card.
   - `assembleDebug` and `lint` pass.

### Slice 6 (Q6): `chore(D-015): add the delivery ZIP script`
`scripts/make-delivery-zip.ps1`, in plain PowerShell 5.1 (comments explain each step for the defence):
1. Parameters: `-GoogleServices` (required, must exist), `-Report` (required, must be a `.pdf`), `-Ref` (default `main`), `-OutDir` (default the user's Desktop), `-SkipBuild`.
2. `$name = 'AMOV2627.2018013532.2018044613'`. Create `$stage = New-Item (Join-Path $env:TEMP "$name-stage-<timestamp>") -ItemType Directory`. Record `git rev-parse --short $Ref`.
3. `git archive --format=zip --prefix=CityFix/ -o "$stage\src.zip" $Ref`, then `Expand-Archive` into `$stage\out`, then delete `src.zip`. (If Q8 excludes paths: `git archive ... $Ref -- . ':(exclude).claude' ':(exclude)docs/work' ':(exclude)CLAUDE.md'`.)
4. Copy `-GoogleServices` to `$stage\out\CityFix\app\google-services.json`, and `-Report` to `$stage\out\${name}_Relatorio.pdf`.
5. Guard: fail if any of `CityFix\.gradle`, `CityFix\build`, `CityFix\app\build` or `CityFix\local.properties` exists, or if any `*.jks`, `*.keystore` or `local.properties` exists anywhere in the tree.
6. `tar.exe -a -c -f "$OutDir\$name.zip" -C "$stage\out" CityFix "${name}_Relatorio.pdf"`. Then `tar.exe -t -f` it and check:
   - No entry contains `\`.
   - No entry matches `(^|/)(\.gradle|build)/` or `local.properties`.
   - `CityFix/app/google-services.json` and the PDF are present.
7. Unless `-SkipBuild`:
   - `Expand-Archive "$OutDir\$name.zip"` into a new `$env:TEMP\$name-verify-<timestamp>`.
   - Set `$env:ANDROID_HOME` from the repo's `local.properties` `sdk.dir` (read only, never copied).
   - Run `& "$verify\CityFix\gradlew.bat" -p "$verify\CityFix" assembleDebug`. Exit non-zero on failure.
8. Print the ZIP path, size, source commit and the entry count.
9. Verify: run it once against `dev/Diogo` with `-SkipBuild` to test the packaging, then without `-SkipBuild`. Delete the test ZIP afterwards. `git status` must show no new files besides the script: the script writes only to `%TEMP%` and `-OutDir`.

### Manual sweep (Diogo, on device or emulator; not commits)
Do it after slice 4's merge. Use one portrait/landscape pass in EN and one in PT (device language Portuguese). Turn TalkBack on for one pass per role.

| Role | Screens | Check |
| --- | --- | --- |
| Anonymous (inside 10 km) | Gate (outside / denied / no fix), Dashboard, List, Public detail, Login, Forgot password, Register | The coarse map has no author data. Rotate with the e-mail typed in Login and with every Register field filled (incl. photo and location). PT labels fit. |
| Pending citizen | Pending validation, public browsing, Profile, Change e-mail/password | Rotate mid-change (code step). |
| Active citizen | Dashboard (FAB if Q5), Report (category, situation, description, 2 photos, camera, scope), My tickets, Participant detail (thread draft, reopen, clarification dialogs), Profile (photo, impact toggle), F-015 impact suggestion, F-016 notifications permission | Rotate with the form filled and a dialog open: the text is kept. TalkBack reads the radio groups, the attachment remove buttons and the send button. |
| Operator | Operations panel (filters, sort chip), Operator detail (contacts, request info, start work, resolve with proof), Approvals, Citizen approval (map), Categories (FAB, edit dialog, switch) | Rotate with the resolve sheet and its attachments, and with the category dialog half typed. The 48 dp targets on the switch and the edit buttons. |
| All | Top bar title / back / menu per screen; snackbars | Contrast with Accessibility Scanner on Dashboard, List and Detail. |

### Manual delivery checklist (Diogo)
1. **Test accounts** (Firebase console plus the app):
   - one active citizen (registered in the app, then activated by the operator in the app);
   - one pending citizen;
   - one operator on the council domain;
   - each with an MFA test phone number and code in Authentication > Sign-in method > Phone (test numbers).
   
   Log in once with each. Send the e-mails, passwords and test numbers/codes to Francisco for the PDF only (Q9).
2. Maps key: in the Google Cloud console > Credentials, check that the key has no Android-app restriction (R4).
3. (Q7) Console: Firestore rules and Storage rules test-mode date moved past the grading period (Diogo, by hand).
4. (Q4) Console: delete the stand-in users and the `debug` collection, if approved.
5. Diogo asks for the final merges (`dev/Francisco` and `dev/Diogo` into `main`, then push `main`). Then build the ZIP from `main` (script, or steps 2-7 of slice 6 by hand). Write down the `main` commit.
6. Open the unzipped copy's APK on an emulator: log in as the operator, open a ticket, and check that the map tiles show.
7. Upload `AMOV2627.2018013532.2018044613.zip` to Nonio. Download it back and compare the size. Record the time in `state.md`.

## Conventions
- Kotlin rules in `.claude/rules/kotlin.md` and the `kotlin-conventions` skill:
  - strings in both locales;
  - `contentDescription` on every meaningful control;
  - 48 dp targets;
  - no new top-level folders except `scripts/` if Q6 approves it.
- Commits: one per slice, `chore(D-015): ...` / `fix(D-015): ...`, no attribution lines. Untracked `.idea/` files stay out (state.md Start).
- Before each commit: `./gradlew assembleDebug` (the `verify` skill). `lint` at slices 3, 4 and 5.
- Never stage `app/google-services.json`, any PDF made from the ZIP, or the ZIP itself. The script writes outside the repo.

## Do not touch
- `firestore.rules`, `storage.rules`, `firebase.json`, `app/google-services.json`, and the Maps key value in the manifest.
- `gradle/libs.versions.toml` and `app/build.gradle.kts`: no dependency upgrades.
- `ImageCompression.kt` (Francisco's, `UseKtx`), unless he asks.
- `ShowScreen.kt` previews and `SampleTickets` data.
- Anything in F-015/F-016/F-017 beyond the string/accessibility fixes of slice 4. Tell Francisco about those.
- `main`: no merge or push unless Diogo asks in the conversation.

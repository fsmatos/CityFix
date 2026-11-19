# D-015 docs plan

## Docs to update
- `docs/architecture/overview.md`
  - "Container" paragraph (around line 502): the seed sentence becomes "removed by D-015 on <date> (`<sha>`)".
  - Code-mapping table row for the Firebase instances (around line 306): drop "and `DebugSeed`".
  - "Handover (F-011)" and "Handover (F-012)" D-015 items (around lines 858, 871, 905): mark them done or answered:
    - seed removed;
    - stand-in data per Q4;
    - Maps key restriction checked (R4).
  - "Setup" (around lines 516-521): add the ZIP procedure in short (script or manual steps, `ANDROID_HOME` for the verify build) and the test-mode expiry answer (Q7).
  - If Q5 is approved: "MainTopAppBar" / "App shell" sections say that the report entry is a citizen FAB on Dashboard/List/My tickets and that the top bar no longer has "+".
  - New short section "Delivery (D-015)":
    - ZIP name and layout (Q8);
    - source `main` commit;
    - where the PDF and `google-services.json` come from;
    - the verification build;
    - the Nonio upload time.
- `docs/decisions/0013-firestore-reads-and-dashboard-counters.md` Consequences (:50-51): append "(D-015: seed removed; stand-in data <deleted/kept>, Q4)".
- `docs/decisions/0012-firebase-plan-notifications-close-maps.md` Consequences (:35, :37): append "done in D-015". If Q7 changes the expiry, update the "test mode until 2026-12-10" mention, or point to the new date in the overview.
- `docs/tasks/phase-2/D-015-final-pass-delivery.md` (Diogo's file):
  - Correct "today `app_name` is only in `values/`" to "`app_name` is `translatable="false"`; no PT copy needed (Q3)".
  - Change "`LocalTicketsRepository` if unused" to "(already deleted by F-011)".
  - Tick the acceptance checks met.
- `docs/report/notes.md`: new section "D-015: Final pass and delivery" (content in "Report notes" below).
- If Q6 is approved: a short "how to use" header comment inside `scripts/make-delivery-zip.ps1` is the documentation. Link it from the overview "Delivery (D-015)" section. No separate README.

## Decisions to record
- **0023: Delivery package** (short). Record:
  - the ZIP layout and what is included or excluded (Q8);
  - `google-services.json` copied in by hand (from 0012 Q7);
  - building with `tar.exe` for `/` separators;
  - the verification build of the unzipped copy;
  - test accounts in the PDF only (Q9);
  - the Firestore/Storage test-mode date (Q7);
  - the FAB choice, if Q5 is approved (or a line in the overview instead, if it stays small).
  
  Rejected options:
  - zipping the working folder (risk of `build/` and `local.properties`);
  - committing `google-services.json`;
  - `Compress-Archive` on PS 5.1.

## Report notes
For F-017. Diogo hands these to Francisco.
- **Languages:** English default (`values/`), Portuguese from the device setting (`values-pt/`). Every translatable key exists in both; parity was checked by key diff after the final merge. Names that do not change by language are `translatable="false"` (`app_name`, phone prefix). Category names are data typed by operators, so they are not translated (improvement: per-locale category names).
- **Orientation:**
  - Form and dialog state lives in ViewModels (`StateFlow<UiState>`); small UI flags and the camera temp path use `rememberSaveable`. So rotation and process recreation keep typed data.
  - Detail, report, dashboard, approval and the message thread split into two panes in landscape. The forms scroll with `imePadding`.
- **Accessibility:**
  - Every actionable icon has a localised `contentDescription`. Decorative icons are `null` when a visible label says the same thing.
  - Radio groups use `selectableGroup` + `Role.RadioButton`, and switches use a `toggleable` row with `stateDescription`.
  - Touch targets are at least 48 dp (`IconButton`, `minimumInteractiveComponentSize`, `heightIn(min = 48.dp)`).
  - Loading spinners have descriptions, and headings are marked with `semantics { heading() }`.
  - The anonymous gate hides the screen behind it from TalkBack (`clearAndSetSemantics`).
  - Contrast was checked with Accessibility Scanner (list its findings).
- **Dynamic top bar and FAB:**
  - The title follows the destination.
  - The actions follow role and context (back, menu by role, Log in / Log out).
  - Report entry: the "+" for active citizens, or the citizen FAB on Dashboard/List/My tickets (Q5).
  - The operator Categories FAB is hidden until the list loads.
- **Clean-up before delivery:** the debug seed and the phase-1 sample categories were removed. The sample tickets stay for one privacy preview only. Lint has 0 errors; the remaining warnings are dependency-version notices, left on purpose (no upgrades right before delivery).
- **Test accounts (section 9 of the report):**
  - a table with role, e-mail, password, MFA test number and fixed code, and what to try with each account;
  - a note that the grader's build may show a reCAPTCHA browser step during SMS MFA, because the grader's debug key SHA-1 is not registered (R3);
  - on macOS/Linux, `chmod +x gradlew` may be needed.
- **How to run:**
  - open `CityFix/` in Android Studio, which creates `local.properties` (sdk.dir);
  - `google-services.json` is included;
  - the Maps key is in the manifest;
  - the database is in test mode until <date> (Q7).
- **Limits to state:**
  - the security rules are drafts and not deployed, so privacy, MFA and role guards are enforced in the app only;
  - notifications are local, with no server;
  - the operator geofence works in the foreground only;
  - any sweep defect not fixed (list them).
- **Improvement opportunities:**
  - automated UI tests for rotation and TalkBack;
  - a `WindowSizeClass`-based layout for tablets;
  - localised category names;
  - deployed rules;
  - a CI job that builds the ZIP and checks it.

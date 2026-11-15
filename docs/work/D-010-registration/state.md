# D-010: Registration (citizen and operator)
Branch: dev/Diogo · Base: 73370ed
Remote: origin/dev/Diogo

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-14 | Task file `docs/tasks/phase-2/D-010-registration.md` |
| Plan | done | 2026-11-14 | 00-plan, 01-coding-plan, 03-docs-plan; 7 slices (6 map pick optional, 7 rules line conditional on Q1); Q1-Q15 for Gate 1 |
| Gate 1: plan approved | done | 2026-11-14 | Approved by Diogo; Q1-Q15 as recommended; slice 7 (draft rules edit, not deployed) approved; slice 6 (map pick) skipped |
| Code | done | 2026-11-14 | Slices 1-5 and 7 committed (cdbcb8d..1760f28); slice 6 skipped (Gate 1); build + lint green, 22 warnings = baseline, none new; device checks pending (Diogo) |
| Review | done | 2026-11-14 | Round 1: changes requested (B1, S1-S3, N1-N7). Round 2 (5be47fd..HEAD): approved; B1, S1-S3, N1, N2, N4-N6 resolved, N3/N7 deferred, 4 new nits (R2-N1..N4); build + lint 0 errors, 22 warnings = baseline; device checks pending |
| Docs | done | 2026-11-14 | New decision 0016; 0015 (session) amended; overview, report notes, task file updated; N7 and the deferred nits documented; nothing committed |
| Pushed | done | 2026-11-14 | eba8aa2 to origin/dev/Diogo |
| Merged | pending | | |

## Log
Append-only.

- 2026-11-14 Sync: `origin/main` had no commits missing from dev/Diogo; Diogo chose not to run `/merge`.
- 2026-11-14 Start: task file `docs/tasks/phase-2/D-010-registration.md` (target 2026-11-14), base 73370ed.
- 2026-11-14 Plan: planner wrote the three plans. 7 slices: validation helpers, registration data layer, Register screen + Login link + redirect hold, citizen location (F-012 `LocationRepository`), optional photo via `ProfilePhotoWorker` after the commit, optional map pick, conditional rules line. Key finding: sign-up signs the user in, so D-009's redirect would clear the Register screen and cancel the NIF transaction; fix is a saved `holdRedirect` flag in `CityFixApp`. Recommends creating the operator's `users/{uid}` at registration (deviation from the D-009 handover) and dropping the `email_verified` term of the draft `users` create rule (`firestore.rules:190`), which needs Diogo's OK. Risks R1-R9 (incl. two decision records numbered 0015).
- 2026-11-14 Gate 1: approved by Diogo. Q1-Q15: the recommended answers. Q1: Diogo allows editing the draft `firestore.rules` (drop the `email_verified` term of the `users` create rule) in slice 7; commit only, no `firebase deploy`; Francisco to be told. Slice 6 (map pick) skipped: "use my location" only.
- 2026-11-14 Code: implementer ran slices 1, 2, 3, 4, 5 and 7, one commit each; `./gradlew assembleDebug` then `./gradlew lint` before every Kotlin commit. Lint baseline measured before slice 1 on e7cfdf4: 22 warnings (the 22 of the coding plan; the hand-off note said 20). Every slice: 0 errors, 22 warnings, the same list as the baseline, no new warning; no Kotlin compiler warnings. EN and PT key sets equal.
  - cdbcb8d `feat(D-010): add input validation helpers` (slice 1).
  - a3961e8 `feat(D-010): add registration to the users repository` (slice 2).
  - add37e9 `feat(D-010): add the registration screen for citizens and operators` (slice 3).
  - f10ce0f `feat(D-010): georeference the citizen at registration` (slice 4).
  - aba8c6b `feat(D-010): add an optional profile photo uploaded after registration` (slice 5).
  - 1760f28 `docs(D-010): let the draft rules accept the operator profile before verification` (slice 7; rules file only, so build and lint were skipped as the verify skill says; not deployed, no `firebase` command run). Francisco still has to be told (shared file).
  - Slice 6 (map pick) skipped, as decided at Gate 1.
- 2026-11-14 Code deviations (all small, none changes the approved design):
  - Slice 3: `RegisterScreen` also releases the redirect hold in a `DisposableEffect` `onDispose`. Why: if the user leaves the screen while Submit runs (Back), the ViewModel is cleared, the registration is cancelled (the Auth user is deleted in `NonCancellable`) and, without the release, `holdRedirect` would stay true and block every later redirect. Rotation is unaffected: the saveable value is saved before the old composition is disposed.
  - Slice 3: the Register fields use a private `RegisterTextField` / `PasswordField` instead of Login's `EmailField`, because the domain error needs a format argument (`register_error_email_domain` with the domain) and `EmailField` only takes a resource id.
  - Slice 3: no `cd_*` string for the impact switch: the whole row is `toggleable(role = Role.Switch)` with the visible label, and the `Switch` is passive (`onCheckedChange = null`), so TalkBack reads the label; a content description would repeat it. `cd_register_progress` was added for the Submit progress indicator.
  - Slice 3: the phone field shows the `+351 ` prefix (existing `setup_phone_prefix`); `submit()` also returns when `registered` is true (no double submit after success).
  - Slice 4: reuses the generic `report_location_locating` and `report_location_value` (5 decimals) strings; new `register_location_use`, `register_location_unavailable`, `register_location_denied`.
  - Slice 5: the worker compresses into a fixed name `profile_<uid>.jpg` (not a new UUID per attempt), so a retry overwrites instead of leaving files, and keeps the picked source until the final outcome so each retry can compress again; every final case (success, undecodable, 5 failed attempts) deletes the local files. The ViewModel has `isCopyingPhoto` (photo buttons and Submit wait for a gallery copy) and `photoNotice` (new `register_photo_copy_failed`; camera failure reuses `report_camera_failed`). A copy that finishes after Submit is deleted.
  - Process: `core.hooksPath` is not set in this clone (the commit-msg hook did not run); the six commit messages were checked by hand: no attribution lines.
- 2026-11-14 Review: reviewer read `git diff e7cfdf4..HEAD` (cdbcb8d..fab62c6) against the plans, the Gate 1 decisions, the task file, the overview, decisions 0010 / 0015 and kotlin-conventions. Reviewer ran `./gradlew assembleDebug lint`: success, 0 errors, 22 warnings (baseline; none in D-010 files). EN/PT key sets equal (only the existing `translatable="false"` keys differ). Verdict: changes requested.
  - Blocking: B1. Back or Up during Submit cancels `register()`. During sign-up this leaves an orphan Auth user. During the transaction the commit can still land, and the Auth user is then deleted, so `nifs/{ROLE}_{nif}` is reserved for good. The slice 3 deviation above ("the Auth user is deleted in NonCancellable") covers only the second case, and there the delete makes things worse. Fix: `BackHandler` while registering, no Up arrow while `holdRedirect`, and `withContext(NonCancellable)` around `users.register` in the ViewModel.
  - Should-fix: S1, the failure path depends on the session being ANONYMOUS before the hold drops (device check, or a defensive wait). S2, operator domain: no field error while it loads, and no re-read after a failed read. S3, the photo worker retries permission errors and then drops the photo without a message.
  - Nits N1-N7: listed in review.md (the `Coordinates(0.0, 0.0)` fallback, the e-mail regex, a process-death photo edge case, a content description, keyboard options, import order, overview / decision updates in the docs phase).
  - No code edited, nothing committed by the reviewer.
- 2026-11-14 Review fixes (round 1): five `fix(D-010)` commits on dev/Diogo, verify before each (`assembleDebug`, then `lint`: 0 errors, 22 warnings, the same list as the baseline, no new warning; no Kotlin compiler warnings).
  - 29533cb `fix(D-010): finish a started registration and keep the form on failure` (B1 + S1).
    - B1: `BackHandler(enabled = state.isRegistering) {}` in `RegisterScreen`; `canNavigateBack` is also false while `holdRedirect` in `MainActivity`; `users.register(...)` runs under `withContext(NonCancellable)`.
    - The slice 3 deviation is corrected: the `onDispose` release stays, but only as a safety net. Back and Up cannot leave the screen during Submit any more, and a cleared ViewModel no longer cancels the registration. The earlier text ("the account is deleted in NonCancellable") was true only for the transaction phase.
    - S1, fix chosen: a non-saved `RegisterUiState.failed` flag, set on every failed result and cleared on the next Submit. `RegisterScreen` gets `accountSignedIn = role != ANONYMOUS` and holds the redirect while `isRegistering || (failed && accountSignedIn)`. After a failure, the hold lasts until the deleted (or signed-out) account reaches the session, so Account setup can never open over the form. Why this fix and not the reviewer's two options:
      - Waiting in `deleteNewUser` for the auth listener still leaves the session flow and the compose hops to race.
      - An `onRegistered` gate in `CityFixApp` would strand a user on Register after process death (Q11).
      - With this fix, after process death `failed` is false, so the hold drops and the Q11 path (Account setup, then Account problem) is unchanged.
  - 25d6b02 `fix(D-010): read the council domain again when it is missing` (S2): `loadDomain()` runs from init, from `onRoleSelected(OPERATOR)` and from `submit()` while the domain is null and no read is running. An operator `submit()` returns early while the read is running; the "Checking the council's e-mail domain" hint is shown meanwhile.
  - 78c3cbb `fix(D-010): give up a profile photo upload that cannot succeed` (S3 + N3 log).
    - These cases are final at once, with the local files deleted and `Result.failure()`: the signed-in uid is not the photo's uid, Storage answers `ERROR_NOT_AUTHORIZED` / `ERROR_NOT_AUTHENTICATED`, or Firestore answers `PERMISSION_DENIED` / `UNAUTHENTICATED`.
    - Consequence: a user who logs out before the upload runs loses the photo, even if they log in again later (optional photo, as the coordinator asked).
    - A missing source file is now logged (N3).
  - f14bfb1 `fix(D-010): tidy the registration form and its validation`:
    - N1: `requireNotNull(registration.georeferencing)` instead of the (0, 0) fallback.
    - N2: the e-mail regex now allows dots only between characters, on both sides of the @. This also tightens Login and Forgot password, which share it (Q15).
    - N4: new `cd_register_photo_copying` (EN/PT) on the copy progress.
    - N5: `KeyboardCapitalization.Words` on the name field and `autoCorrectEnabled = false` on the e-mail field, through new `RegisterTextField` parameters.
    - N6: the `RegisterScreen` imports are sorted.
  - Left open:
    - N3, the process-death photo edge, is left (rare). The worker now logs the missing file. The docs phase should mention it.
    - N7 is for the docs phase.
    - Add the S3 case (photo dropped after logout or refusal) to the failure-mode table in the docs.
  - Device checks added by the fixes: tap Submit, then Back at once (Back ignored, no Up arrow, result as normal). NIF clash: no flash of Account setup, and the form keeps "already registered". Operator Submit offline on a fresh install, then online: the domain is read again on Submit or on re-selecting Operator.
- 2026-11-14 Review round 2: reviewer read `git diff 5be47fd..HEAD` (29533cb, 25d6b02, 78c3cbb, f14bfb1, f09c041) and ran `./gradlew assembleDebug lint`: success, 0 errors, 22 warnings (baseline). EN/PT parity holds. Verdict: approved.
  - B1, S1, S2, S3, N1, N2, N4, N5 and N6 are resolved.
  - The S1 `failed && role != ANONYMOUS` hold was judged on its own: correct whatever the callback order, it leans the safe way (LOADING counts as signed in), and process death is unchanged (Q11).
  - N3 and N7 are deferred: N7 to the docs phase, N3 now logged by the worker.
  - New nits for the docs phase or a later touch:
    - R2-N1: `AuthResult.user == null` returns without `signOut()`.
    - R2-N2: Submit after a missing domain needs a second tap.
    - R2-N3: a Storage object can be orphaned when the Firestore update is refused, and an unauthenticated answer can be transient.
    - R2-N4: no way to leave the screen while Firebase hangs (intended).
  - Device checks from the fixes are still pending (Diogo).
  - No code edited, nothing committed by the reviewer.
- 2026-11-14 Docs: docs-writer ran `03-docs-plan.md`. It worked from the plans, `state.md`, `review.md` and the current code files; it could not run `git diff` (no shell in the session), so it read the changed files directly.
  - New `docs/decisions/0016-registration.md`. Number 0016 because 0015 is used twice (R8). The clash is noted in 0016, the overview and the report notes; Francisco's records are not renumbered or edited.
  - `docs/decisions/0015-session-role-navigation-mfa.md` (Diogo's): status line plus one Decision bullet for the redirect hold and Register as an auth screen.
  - `docs/architecture/overview.md`:
    - Layers: `viewmodel/`, `data/`, `utils/` and `CityFixApplication`.
    - `RegisterRoute` in the route table; `TopBarScreen.REGISTER` and the controls-by-role row.
    - App shell: `holdRedirect` and `RegisterViewModel` rows, the redirect key, the gate row.
    - Firebase: data-layout line, a D-010 rules note, and the console step for `municipality/main.emailDomain` (test domain with a real inbox; orphan users deleted in the console).
    - Auth and session: repository line, redirect rule, the two `AuthError` rows and the shared `toAuthError()`, the e-mail rule, `isPortugueseMobile`, the gate exceptions.
    - **N7:** the "Handover (F-010)" operator item (around line 505) and the "Handover (D-009)" D-010 item now record the Q1 deviation: both roles' `users/{uid}` are created in the registration transaction; the operator `email_verified` term was dropped from the draft `users` create rule; not deployed; Francisco must be told.
    - New "Registration (D-010)" section: form, validation table, `register()` steps, document fields, operator timing, redirect hold, domain, georeferencing, photo, offline, privacy, known limits, and a failure-mode table. The table covers orphan Auth users (Q11, console cleanup), S3 (photo dropped after a logout or a refused write), N3 (queued photo lost after a process death right after Submit), R2-N3 (`photo.jpg` without `photoPath`; an unauthenticated answer treated as final), R2-N2 (tap Submit again once the domain loads), R2-N4 (Back blocked while Firebase hangs) and R2-N1.
    - New "Handover (D-010)" for D-011, D-012, D-014 and Francisco (rules line, strings, `onCreateAccount`, the stricter e-mail regex), with the suggested amendment line for decision 0010 (not edited).
    - Roles: registration as the way into PENDING_CITIZEN / OPERATOR; R3-N3 now reachable by anyone; Q12.
  - `docs/report/notes.md`: new "D-010" section (technical solutions, options and why, class material vs beyond, limits, verification, improvements, the 0015 housekeeping note).
  - `docs/tasks/phase-2/D-010-registration.md`: status line and a "Status notes" section (Q1 deviation, map pick skipped, pending device checks). No acceptance box is ticked.
  - `CLAUDE.md` is unchanged (nothing there mentions the rule). No code, rules or decision records of Francisco were edited. Nothing was committed.
  - Still open: tell Francisco about the `firestore.rules` edit and the suggested 0010 amendment; Diogo's console step (`municipality/main`) and the device checks.
- 2026-11-14 Push: verify OK (assembleDebug, lint 0 errors / 22 warnings); dev/Diogo pushed to origin at eba8aa2. Not merged into main.
- 2026-11-15 Device check 3a (Claude via adb on the `Iphone_Diogo` emulator, PT locale, debug build of 647e2b3): Register screen errors appear as you type for e-mail `ana@`, phone `12345`, NIF `123456780` (`123456789` accepted), short password, password without a digit, and a confirmation that doesn't match. Submit with errors stays on the form with "Alguns campos precisam da sua atenção." and the citizen location error. Rotation to landscape and back keeps every field, and the passwords also stay because the ViewModel survives. Operator: the hint reads `@gmail.com` from `municipality/main`, and `ana@hotmail.com` is refused naming the domain. Not checked: landscape clipping (emulator screenshots come out blank, so only the UI tree was read), full registration, NIF clash, photo, Back during Submit.
- 2026-11-15 Device check 3b, citizen happy path (emulator restarted with `-gpu swiftshader_indirect` because the screen was blank with host GPU). Claude filled in name, e-mail (`+cidadao1` Gmail alias), phone, NIF `123456789`, and the location via "Use my location" (40.21000, -8.41900). Diogo typed the password, submitted, opened the verification link and entered the SMS code. Account setup opened by itself (e-mail, then phone), and after MFA the app shows "Registo pendente" / "O seu registo aguarda validação" (PENDING waiting screen). Cosmetic nit: the phone field shows two spaces between `+351` and the number.
- 2026-11-15 Device check 3c, NIF clash: after logging out, a second citizen (`+cidadao2` alias, NIF `123456789`) submitted by Diogo stays on the Register form with every field kept and "Este NIF já está registado." under the NIF field; Account setup did not open (B1/S1 behaviour). Auth console check for the deleted `+cidadao2` user still pending.

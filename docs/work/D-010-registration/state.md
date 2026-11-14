# D-010: Registration (citizen and operator)
Branch: dev/Diogo · Base: 73370ed
Remote: not pushed

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-14 | Task file `docs/tasks/phase-2/D-010-registration.md` |
| Plan | done | 2026-11-14 | 00-plan, 01-coding-plan, 03-docs-plan; 7 slices (6 map pick optional, 7 rules line conditional on Q1); Q1-Q15 for Gate 1 |
| Gate 1: plan approved | done | 2026-11-14 | Approved by Diogo; Q1-Q15 as recommended; slice 7 (draft rules edit, not deployed) approved; slice 6 (map pick) skipped |
| Code | done | 2026-11-14 | Slices 1-5 and 7 committed (cdbcb8d..1760f28); slice 6 skipped (Gate 1); build + lint green, 22 warnings = baseline, none new; device checks pending (Diogo) |
| Review | done | 2026-11-14 | Changes requested: 1 blocking (B1 Back/Up during Submit), 3 should-fix, 7 nits; build + lint green (0 errors, 22 warnings = baseline); see review.md |
| Docs | pending | | |
| Pushed | pending | | |
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

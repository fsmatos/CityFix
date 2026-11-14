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

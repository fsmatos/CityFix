# D-011: Profile editing
Branch: dev/Diogo · Base: 7440821
Remote: not pushed

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-15 | Task file `docs/tasks/phase-2/D-011-profile-editing.md` |
| Plan | done | 2026-11-15 | 00-plan, 01-coding-plan, 03-docs-plan; 8 slices (8 = draft rules, conditional on Q4); Q1-Q13 for Gate 1 |
| Gate 1: plan approved | done | 2026-11-15 | Approved by Diogo; Q1-Q13 as recommended; Q4 yes (draft rules edit in slice 8, commit only, not deployed) |
| Code | pending | | |
| Review | pending | | |
| Docs | pending | | |
| Pushed | pending | | |
| Merged | pending | | |

## Log
Append-only.

- 2026-11-15 Sync: `origin/main` had no commits missing from dev/Diogo; Diogo chose not to run `/merge`.
- 2026-11-15 Start: task file `docs/tasks/phase-2/D-011-profile-editing.md` (target 2026-11-15), base 7440821.
- 2026-11-15 Plan: planner wrote the three plans. 8 slices: share the registration fields (`ui/components/AccountFields.kt`), `UsersRepository.updateProfile` / `syncEmail` (phone, photo, citizen impact detection only), Profile screen, re-authentication + credential changes in the auth repository (password + SMS, reusing the MFA challenge), change password, change e-mail via `verifyBeforeUpdateEmail`, keep `users/{uid}.email` in step from `SessionViewModel`, and a conditional draft rules allow-list (Q4). Key finding: the draft rules (`firestore.rules:197-200`) forbid the owner from changing `email`, so the e-mail sync needs slice 8. Risks R1-R10; decision record 0017.
- 2026-11-15 Gate 1: approved by Diogo. Q1-Q13: the recommended answers. Q4: Diogo allows editing the draft `firestore.rules` (owner allow-list: `phone`, `photoPath`, `impactDetection` for citizens, `fcmTokens`, and `email` only to the verified token e-mail, operators within the domain) in slice 8; commit only, no `firebase deploy`; Francisco to be told.
- 2026-11-15 Code slice 1: fd388df `refactor(D-011): share the registration form fields`. Verify: assembleDebug OK; lint 0 errors / 22 warnings (same ids as baseline). Note: the photo notice is now drawn by the caller after `ProfilePhotoPicker`, so its spacing follows `FormColumn` instead of the photo column (same order on screen).
- 2026-11-15 Code slice 2: 10dbbc4 `feat(D-011): write the editable profile fields`. Verify: assembleDebug OK; lint 0 errors / 22 warnings.
- 2026-11-15 Code slice 3: `feat(D-011): add the profile screen` (sha in the next line). Verify: assembleDebug OK; lint 0 errors / 22 warnings; EN/PT key sets equal. Notes: name, NIF, e-mail and georeferencing labels reuse `register_name_label`, `register_nif_label`, `login_email_label`, `register_location_label` (the plan allows reuse); new keys `profile_role_label`, `profile_status_*`, `profile_phone_hint`, `profile_save`, `profile_saved`, `profile_photo_uploading`, `profile_unavailable`, `profile_log_out`, `cd_profile_loading`. The camera launch helper is a private copy in `ProfileScreen.kt` (`launchProfileCamera`), as Register and Report each have their own.
- 2026-11-15 Code slice 3 commit: 35c5dae.
- 2026-11-15 Code slice 4: `feat(D-011): add re-authentication and credential changes to the auth repository` (sha in the next line). Verify: assembleDebug OK; lint 0 errors / 22 warnings. `signIn` now uses the extracted `challengeOf(e)`; behaviour unchanged.

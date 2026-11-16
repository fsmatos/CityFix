# D-011: Profile editing

Owner: Diogo (`dev/Diogo`) · Layer: `ui` / `viewmodel` · Phase 2 · Status: code, review and docs done (2026-11-16); not pushed; some device and console checks open · Target: 2026-11-15
Depends on: D-010

## Goal
Let citizens and operators change their editable profile data.

## Why
- Brief I.2.C: citizen cannot change name, NIF or georeferencing.
- Brief I.3.C: operator cannot change name or NIF.
- Brief I.2.A/F: the impact-detection choice lives in the profile (F-015 reads it).

## What to build
- **Profile screen** reusing the registration form components: locked fields shown read-only; editable: email, phone, photo, impact detection (citizen). Same validation as D-010.
- **Email change:** Firebase requires re-authentication / verification (`verifyBeforeUpdateEmail`); operators must stay in the council domain.
- Change password (re-authentication) and log out.
- `UsersRepository.updateProfile(...)` writes only the allowed fields (the Firestore rules from F-010 should also block the locked ones).

## Files
New `ui/screens/ProfileScreen.kt`, `viewmodel/ProfileViewModel.kt`; `data/UsersRepository.kt`, navigation, `firestore.rules` (locked fields; deploying needs the person's OK), strings EN/PT.

## Acceptance checks
- [ ] Name, NIF (and georeferencing for citizens) cannot be edited in the UI or by a direct write.
- [ ] Turning impact detection off stops F-015's detector.
- [ ] Portrait and landscape both usable.

## Status notes
Work item: `docs/work/D-011-profile-editing/` (plans, `state.md`, `review.md`). Decision: [0017](../../decisions/0017-profile-editing.md). Overview: "Profile (D-011)", "Handover (D-011)".
- **Gate 1 (2026-11-15, Diogo):** Q1-Q13 as recommended. The profile phone is contact data, separate from the SMS factor (Q1); re-authentication is password + SMS (Q2); the e-mail sync lives in the session plus a reload on Profile (Q3); the draft `firestore.rules` get an owner allow-list, committed and **not deployed** (Q4, Q5); one Save (Q6); no stored-photo removal (Q7); unique photo work (Q8); pending citizens may edit (Q9); e-mail and password on their own routes (Q10); single centred column in landscape (Q11).
- **Against "What to build":** the e-mail is not edited in the Profile form but on its own Change e-mail route (password + SMS, then `verifyBeforeUpdateEmail`); the Profile shows it read-only. Change password is a route too.
- **Deviations:** `CredentialChangeUiState.currentEmail` and a few extra strings (state log); after review S2, `ProfilePhotoWorker` also writes `photoUpdatedAt` (the plan's R4 fallback) and the draft allow-list includes it.
- **Review:** round 1 approve with should-fix (S1 e-mail sync loop, S2 stale photo, fixed; N1-N4 fixed; N5, N6 documented); round 2 approve (R2-N1 to R2-N3 documented in decision 0017).
- **Device checks (2026-11-16, emulator):** read-only fields (no keyboard), phone error, Save online and offline, rotation, Profile landscape, camera and gallery photo with the new image after upload (R4), wrong current password, full password change (R1: still signed in), e-mail checks before Firebase, address in use (R3: no error at request time), the link signs this device out at the next reload with no message (R2), login with the new e-mail and password.
- **Open:** console values (`phone`, `impactDetection: false`, `photoPath` + `photoUpdatedAt`, `users/{uid}.email`); Register with a photo after the worker change; two devices on one account; old password refused at login; Change e-mail / Change password in landscape; TalkBack "read only"; Rules Playground for the draft.
- **Why no box is ticked:** (1) the UI part holds, but "by a direct write" holds only in the undeployed draft (test mode), and the Playground was not run; (2) F-015 does not exist, so the detector stopping cannot be shown; checked only through the session/profile value (impact off saved offline, still off after reopening Profile); (3) Profile passed in landscape, the two change screens were not checked in landscape.
- **Handover:** F-015 reads `session.accessRole == AccessRole.CITIZEN && ((session as? Session.SignedIn)?.user as? Citizen)?.impactDetection == true` (overview "Handover (D-011)"). Francisco must be told that `firestore.rules` changed (shared, not deployed).

## References
Brief I.2.C, I.3.C.

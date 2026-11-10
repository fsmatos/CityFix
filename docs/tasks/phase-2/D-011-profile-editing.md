# D-011: Profile editing

Owner: Diogo (`dev/Diogo`) · Layer: `ui` / `viewmodel` · Phase 2 · Status: to do · Target: 2026-11-15
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

## References
Brief I.2.C, I.3.C.

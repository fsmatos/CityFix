# D-010: Registration (citizen and operator)

Owner: Diogo (`dev/Diogo`) · Layer: `data` / `ui` / `viewmodel` · Phase 2 · Status: done; code reviewed and approved, docs done (2026-11-14), device checks passed on the emulator (2026-11-15); pushed to origin/dev/Diogo, not merged into main · Target: 2026-11-14
Depends on: D-009 · Blocks: D-011, D-012, F-015

## Goal
Register citizens (pending until the council validates the NIF) and operators (active at once), with validated input and a NIF unique per role.

## Why
- Brief I.2.A: name, email, phone, NIF, georeferencing, role Citizen, optional photo; NIF unique for the role; formats validated; pending until validated by NIF; choose impact detection.
- Brief I.3.A: name, email, phone, NIF, role Operator, optional photo; email must be in the council's domain; active immediately.

## What to build
- **Register screen** with a role choice. Fields per role; real-time validation like P08's `EditContactViewModel` (email regex, Portuguese phone 9 digits, NIF 9 digits with the check digit, password strength). Operator email domain from `municipality/main`.
- **Georeferencing (citizen):** current location via F-012's location helper, or a pick on the map (if F-012 is not ready yet, a "use my location" button with a stub and finish after).
- **Photo (optional):** gallery/camera as in F-007, compressed, uploaded to Storage (reuse F-013's compression).
- **`UsersRepository.register(...)`:** create the Auth user, then in one Firestore transaction create `nifs/{role}_{nif}` (fails if it exists) and `users/{uid}`; if the transaction fails, delete the Auth user and show "NIF already registered".
- Impact detection switch (citizen).
- After registration go to MFA enrolment (D-009).
- See the overview's "Handover (D-009)": after sign-up the session itself routes to Account setup (verify e-mail, then phone); the operator's `users/{uid}` is created only after the e-mail is verified.

## Files
New `ui/screens/RegisterScreen.kt`, `viewmodel/RegisterViewModel.kt`, `viewmodel/RegisterUiState.kt`, `utils/Validation.kt`; `data/UsersRepository.kt`, navigation, strings EN/PT.

## Acceptance checks
- [x] Invalid email/phone/NIF show errors in real time and block submit.
- [x] Two citizens with the same NIF: the second fails; a citizen and an operator may share one NIF.
- [x] Operator with an email outside the domain is refused.
- [x] New citizen is PENDING; new operator is ACTIVE.
- [x] Rotation keeps the form.

## Status notes (2026-11-14, updated 2026-11-15)
- Work item: `docs/work/D-010-registration/` (plans, `state.md`, `review.md`); decision [0016](../../decisions/0016-registration.md); overview "Registration (D-010)".
- **Deviation from "What to build" (Gate 1 Q1):** the operator's `users/{uid}` is created in the registration transaction, before the e-mail is verified, not after it. The draft `firestore.rules` `users` create rule lost its operator `email_verified` term (not deployed; Francisco to be told).
- The map pick was skipped (Gate 1); only "Use my location" is built.
- Device checks (2026-11-15, emulator, PT locale; details in `state.md`): all five acceptance checks passed, plus e-mail already in use, Back right after Submit ignored, camera photo uploaded (`photoPath` URL), offline Submit ("Sem ligação", form kept) and landscape without clipping. Console checks by Diogo: `users`/`nifs` documents as expected, no Auth user left by the NIF clash or the offline attempt.
- Not run on a device: a gallery photo, a location permission denial, the operator domain re-read after starting offline, the EN locale.

## References
Brief I.2.A, I.3.A; P08 (form validation); decision 0010; phase 1 risk 4.

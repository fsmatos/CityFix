# D-010: Registration (citizen and operator)

Owner: Diogo (`dev/Diogo`) · Layer: `data` / `ui` / `viewmodel` · Phase 2 · Status: to do · Target: 2026-11-14
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

## Files
New `ui/screens/RegisterScreen.kt`, `viewmodel/RegisterViewModel.kt`, `viewmodel/RegisterUiState.kt`, `utils/Validation.kt`; `data/UsersRepository.kt`, navigation, strings EN/PT.

## Acceptance checks
- [ ] Invalid email/phone/NIF show errors in real time and block submit.
- [ ] Two citizens with the same NIF: the second fails; a citizen and an operator may share one NIF.
- [ ] Operator with an email outside the domain is refused.
- [ ] New citizen is PENDING; new operator is ACTIVE.
- [ ] Rotation keeps the form.

## References
Brief I.2.A, I.3.A; P08 (form validation); decision 0010; phase 1 risk 4.

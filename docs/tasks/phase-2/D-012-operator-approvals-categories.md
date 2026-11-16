# D-012: Operator: citizen approval and category management

Owner: Diogo (`dev/Diogo`) · Layer: `ui` / `viewmodel` / `data` · Phase 2 · Status: code, review and docs done 2026-11-16; device checks pending; not pushed · Target: 2026-11-16
Depends on: D-010, F-011

## Goal
Give operators the two admin screens: activate pending citizens and manage the problem categories.

## Why
- Brief I.3.D: view a citizen's registration (name, georeferencing, NIF) and activate it.
- Brief I.3.E: create, activate, deactivate or edit categories; citizens only report on active ones.

## What to build
- **Pending citizens screen:** list of `users` with role CITIZEN and `registrationStatus = PENDING`; detail with name, NIF and location on the map (F-012 component); Activate (and Reject, if you want) updates the status.
- **Categories screen:** list with an active switch, FAB to add, tap to edit the name. `CategoriesRepository` (or methods in the tickets repository; agree with Francisco) over `categories/`. Existing tickets keep their `categoryName` snapshot (decision 0003).
- Only operators reach these routes (D-009 navigation); Firestore rules restrict writes to operators.

## Files
New `ui/screens/PendingCitizensScreen.kt`, `ui/screens/CategoriesScreen.kt`, `viewmodel/OperatorAdminViewModel.kt` (or two), `data/CategoriesRepository.kt`; `data/UsersRepository.kt`, navigation, strings EN/PT.

## Acceptance checks
- [ ] Activating a citizen lets them report at once (no reinstall).
- [ ] A deactivated category disappears from the report form but old tickets still show its name.
- [ ] A citizen cannot open these screens or write categories.

None of the boxes above is ticked: no device check has run yet (see Status notes).

## Status notes
- Work item: `docs/work/D-012-operator-approvals-categories/` (plans, `state.md`, `review.md`). Decision [0020](../../decisions/0020-operator-approvals-and-categories.md); overview "Approvals and categories (D-012)" and "Handover (D-012)".
- **Gate 1 (2026-11-16):** approved by Diogo, Q1-Q12 as recommended: a new `CategoriesRepository` (Q1); no Reject (Q2); detail with name, NIF, e-mail, phone, photo and the exact map (Q3); oldest first (Q4); a confirmation dialog (Q5); `validatedAt` as epoch ms (Q6); names trimmed, at most 40 characters, unique ignoring case (Q7); no delete (Q8); no `SavedStateHandle` (Q9); no rules edit, so slice 6 skipped (Q10); title "Registration" (Q11); pending citizens still not gated (Q12).
- **Deviations from this task file:** three ViewModels (`PendingCitizensViewModel`, `CitizenApprovalViewModel`, `CategoriesViewModel`) instead of `OperatorAdminViewModel`, and an extra route `PendingCitizenRoute(citizenId)` with `CitizenApprovalScreen.kt` for the detail; editing a category name uses an Edit button, not a tap on the row (plan, review N4); no Reject; the Firestore rules were not edited and are not deployed, so "rules restrict writes to operators" holds only in the drafts.
- Code slices 1-5 (`f4fc8b2`, `0e019ba`, `29482f0`, `c778e39`, `15356d0`), review fixes `3d24e4a`, `2fe52a2`. Build and lint green (0 errors / 22 warnings, baseline).
- **Device checks pending (none run):** the full list is in `state.md` and the overview. For the "report at once" check, record how it was done (two emulators/devices, or the fallback).

## References
Brief I.3.D, I.3.E; decisions 0003, 0010, 0020.

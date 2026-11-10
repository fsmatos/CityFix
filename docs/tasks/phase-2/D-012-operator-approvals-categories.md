# D-012: Operator: citizen approval and category management

Owner: Diogo (`dev/Diogo`) · Layer: `ui` / `viewmodel` / `data` · Phase 2 · Status: to do · Target: 2026-11-16
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

## References
Brief I.3.D, I.3.E; decisions 0003, 0010.

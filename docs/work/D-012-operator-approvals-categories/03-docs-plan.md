# D-012 docs plan

## Docs to update
- `docs/architecture/overview.md`:
  - **Layers:** `viewmodel/` gains `PendingCitizensViewModel`, `CitizenApprovalViewModel` and `CategoriesViewModel`, each with UiState and plain factory, route-scoped inside `RequireAccess(OPERATOR)`. `data/` gains `UsersRepository.pendingCitizens` / `activateCitizen` and the new `CategoriesRepository` / `FirestoreCategoriesRepository`. `CityFixApplication` gains `categoriesRepository` `by lazy`.
  - **Navigation (D-004), route table:** `ApprovalsRoute` and `CategoriesRoute` real screens (owner D-012); new row `PendingCitizenRoute(citizenId)` (OPERATOR, D-012), carrying only the uid.
  - **Top bar (D-005):** `TopBarScreen.PENDING_CITIZEN` and its title.
  - **App shell (D-006), state table:** rows for the three ViewModels (back-stack entry); `onActivated` uses the shared snackbar host like the report.
  - **Report form (F-006), "Category source":** operators now manage categories (D-012); a deactivation or rename reaches the open form through the listener; the rename-inside-an-open-form limit (R4).
  - **New section "Approvals and categories (D-012)":** the pending query (one equality filter, client sort by `createdAt`, no index), the three-field activation write (not awaited, idempotent), how the citizen's app switches (session listener + redirect, no new code), the detail fields (Q3) and exact map, the categories repository and why the form keeps `activeCategories()` (Q1), name rules (Q7), no delete / no reject (Q2, Q8), the failure-mode table from `00-plan.md` updated with device results, known limits.
  - **New "Handover (D-012)":**
    - **Francisco:** new `CategoriesRepository` next to his `TicketsRepository` (two readers of `categories`); `toCategoryDocument()` now also used by it (KDoc line changed, if Q1); a renamed category keeps the old name in a form opened before the rename (his `ReportViewModel`, optional fix: refresh `categoryName` from `categories` in `submit()`); shared files `MainActivity.kt`, both `strings.xml` (new `approvals_*`, `citizen_approval_*`, `categories_*` keys; keep both sides) and, if slice 6 ran, `firestore.rules`.
    - **D-013 (Diogo):** the operator menu now has real Approvals and Categories; `OperatorPanelRoute` is the only placeholder left.
    - **F-016 (Francisco):** optional notification on activation (the `registrationStatus` change in the citizen's own listener).
    - **D-015 (Diogo):** the "set `registrationStatus` to ACTIVE by hand" console step is no longer needed; operators activate in the app.
  - **Setup:** update the D-010 console-step note (line "step 7 above is needed only for the operator's validation of citizens (D-012)") to say D-012 now does it in the app.
  - **Firebase (F-010), security rules:** a "D-012 and the rules" line: no edit (or the slice 6 edit), not deployed, so the operator-only writes are enforced by the app.
  - **Roles:** the operator's Approvals and Categories screens.
- `docs/tasks/phase-2/D-012-operator-approvals-categories.md`: status line, a "Status notes" section (Gate 1 answers, deviations such as the two ViewModels and the extra route, device checks run and pending), acceptance boxes ticked only once checked (the two-device check says how it was done).
- `docs/report/notes.md`: a new "D-012" section (see Report notes).
- `CLAUDE.md`: no change expected.

## Decisions to record
- **New `docs/decisions/0020-operator-approvals-and-categories.md`** (Diogo's; 0019 is the last on this branch; check `origin/main` for a newer number before writing). Contents:
  - Context: brief I.3.D, I.3.E; decision 0003 snapshot; rules drafted and not deployed; the session listener already reacts to `registrationStatus`.
  - Decision: pending query and activation write (Q4, Q6); Activate only, with confirmation (Q2, Q5); detail fields (Q3); separate detail route and two ViewModels; `CategoriesRepository` apart from `TicketsRepository` (Q1); name rules, no delete (Q7, Q8); no `SavedStateHandle` (Q9); rules (Q10).
  - Rejected: Reject/REJECTED status; one `OperatorAdminViewModel`; list/detail two-pane with in-ViewModel selection; writes in `TicketsRepository`; category delete; a server timestamp for `validatedAt`.
  - Consequences: operator-only writes enforced only by the app (R1); duplicate names possible offline (R5); rename inside an open form (R4); old tickets keep the old name.
- **Amend `0015-session-role-navigation-mfa.md`** (Diogo's): one line, the D-012 placeholders are real screens; new route `PendingCitizenRoute`.
- **Decision 0010 (Francisco's; not edited):** suggested amendment line in the handover: "Since D-012 operators write `categories/{id}` (`name`, `active`) from the app and validate citizens with `registrationStatus`, `validatedBy`, `validatedAt` (epoch ms); decision 0020."
- **Decision 0013 (Francisco's; not edited):** suggested line: "Operators manage categories through `CategoriesRepository` (D-012, decision 0020); the form still reads `activeCategories()`."

## Report notes
Section "D-012: Approvals and categories" in `docs/report/notes.md`:
- **Technical solutions:**
  - the activation needs no push or restart: the citizen's session listens to `users/{uid}`, so the role changes and the redirect opens the dashboard (reactive design from D-009);
  - the pending list as a live query with one equality filter and a client-side sort (no composite index);
  - category changes that never rewrite tickets (decision 0003 snapshot), and the form following them live;
  - writes not awaited, so admin actions work offline.
- **Options chosen and why:** the Gate 1 answers (Q1-Q12), especially Activate without Reject, the separate repository and the extra detail route.
- **Class material vs beyond:** Navigation type-safe routes with an argument, ViewModel + StateFlow + factory, `LazyColumn` with keys, `AlertDialog`, FAB (P08), Maps Compose marker (Contacts_#3), `Collator` (P07). Beyond class: Firestore `snapshots()` flows on a query, not-awaited offline writes.
- **Limits** (state at the defence): rules not deployed, so only the app stops a citizen writing categories or activating themselves; duplicate names offline; no reject or delete; rename inside an open form.
- **Verification:** build and lint per slice; the device checks run (two devices for the activation), and the open ones.
- **Improvements:** deploy the rules (with the tightened `categories` block); a Reject with a reason and a notification; category delete with a "used by N tickets" check; a list/detail two-pane in landscape; server-side uniqueness for names (a `categoryNames` index document, like `nifs`).

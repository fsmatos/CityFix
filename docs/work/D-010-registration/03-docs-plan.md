# D-010 docs plan

Written after the code, against what was actually built. Docs only; Francisco's records get suggested lines in the overview handover, not edits, unless he agrees.

## Docs to update
- **`docs/architecture/overview.md`:**
  - "Layers (MVVM)": `viewmodel/` adds `RegisterViewModel` + `RegisterUiState` + `RegisterViewModelFactory` (route-scoped, `SavedStateHandle`); `data/` adds `register` / `operatorEmailDomain` on `UsersRepository`, `ProfilePhotoUploader` + `ProfilePhotoWorker`, the shared `toAuthError()` (`data/AuthErrors.kt`); `utils/` adds `Validation.kt` (pure, no Android imports); `CityFixApplication` line: `usersRepository` built from `firestore` + `auth` + `profilePhotoUploader`.
  - "Navigation (D-004)" route table: `RegisterRoute` (anonymous, no guard, opened from Login).
  - "Top bar (D-005)": `TopBarScreen.REGISTER` (no add, no menu, account action NONE).
  - "App shell (D-006)" state table: `holdRedirect` row; redirect now keyed on `(role, holdRedirect)`.
  - "Auth and session (D-009)": the redirect hold (why sign-up would otherwise cancel the transaction; success, failure, rotation, process death), `onAuthScreen` includes Register, the two new `AuthError` rows in the mapping table (`EMAIL_IN_USE`, `WEAK_PASSWORD`), `emailErrorFor` now on `isValidEmail`, the mobile regex moved to `Validation.kt`.
  - New section **"Registration (D-010)"**: form fields per role; validation rules (e-mail regex, domain, phone 2/9 prefix, mobile 9 prefix for MFA, NIF mod-11 with the formula, password rule) and when errors show; `register()` step by step (Auth user, transaction on `nifs/{ROLE}_{nif}` + `users/{uid}`, delete on failure in `NonCancellable`, photo enqueued after commit); document fields written per role; operator document timing (Q1 answer); georeferencing (permission at point of use, map pick if built); photo upload (worker, path `users/{uid}/photo.jpg`, `photoPath` = download URL); offline behaviour; failure-mode table; known limits (orphan Auth users, no retry for the photo, test domain).
  - "Handover (D-009)" D-010 item marked done with the chosen answer for the operator document; new **"Handover (D-010)"**: D-011 (photo change reuses `ProfilePhotoUploader`; `name`, `nif`, `georeferencing` not editable; prefill), D-012 (pending citizens have `registrationStatus: PENDING`, `photoPath` URL, `georeferencing`; activation sets `validatedBy` / `validatedAt`), D-014 (`impactDetection` from `users/{uid}`), Francisco (rules line if Q1 approved; shared strings; `LoginContent` gained `onCreateAccount`).
  - "Firebase (F-010)" data-layout line: first writers of `users`, `nifs`, `users/{uid}/photo.jpg`; first reader of `municipality/main`. "Setup": console step to create `municipality/main` (fields from decision 0010; the domain used for testing, no inbox addresses).
  - "Roles": registration as the entry to PENDING_CITIZEN / OPERATOR; restate R3-N3 consequence (now reachable by self-registration) and the Q12 answer.
- **`docs/report/notes.md`:** new "D-010" section (see Report notes below).
- **Task file `docs/tasks/phase-2/D-010-registration.md`:** status line; tick only the acceptance boxes Diogo checked on a device; list the pending device checks.
- **`CLAUDE.md`:** no change expected. If Q1 is approved, nothing there mentions the rule; leave it.

## Decisions to record
- **New `docs/decisions/0016-registration.md`** (0015 is taken twice; see "Report notes" / R8). Status, Context (brief I.2.A, I.3.A; decision 0010 transaction; D-009 handover), Decision, Rejected, Consequences:
  - one transaction for `nifs/{ROLE}_{nif}` + `users/{uid}` after the Auth user, Auth user deleted on failure (in `NonCancellable`), online only;
  - the operator document timing (Q1 answer and why; the rules-draft line if approved);
  - the redirect hold (Q2) and its process-death behaviour;
  - validation rules (Q5-Q7, Q10, Q15) and the council domain source (Q9);
  - photo via WorkManager after the commit, `photoPath` = download URL (Q3, Q4);
  - georeferencing required, any accuracy (Q8);
  - Rejected: ViewModel orchestration (Q13), inline upload, a batch instead of the transaction, deferring the operator document, a local pending-registration store;
  - Consequences: orphan Auth users (Q11), pending citizens not gated (Q12, links 0015), testing needs an inbox in `emailDomain`, `nifs` readable by any signed-in user (uid only).
- **Amend `0015-session-role-navigation-mfa.md`** (Diogo's): status line + one Decision bullet: the redirect is keyed on `(role, holdRedirect)` and held while a registration runs (D-010, decision 0016); Register is an auth screen for the gate.
- **Decision 0010 (Francisco's, not edited):** suggested amendment in the overview handover: "Amended by D-010: the operator's `users/{uid}` is written in the registration transaction, before verification; the create rule no longer requires `email_verified` for operators, while `isOperator()` still does (decision 0016)." Only if Q1 is answered as recommended. Also: `photoPath` holds the download URL.

## Report notes
For `docs/report/notes.md`, section "D-010":
- **Technical solutions:** NIF check digit (mod 11, weights 9..2) as a pure function; e-mail, phone and domain rules in one pure file shared by Login, Forgot password, Account setup and Register; real-time validation P08-style with `@StringRes` errors; registration = Auth user + Firestore transaction (read-then-create on `nifs/{ROLE}_{nif}`), with compensation (delete the Auth user) under `NonCancellable`; why a transaction and not a batch (uniqueness needs a read; registration is online anyway); keeping the form alive while Firebase signs the new user in (redirect hold, rotation-safe with `rememberSaveable`); `SavedStateHandle` for the form, never for passwords; profile photo through WorkManager after the commit, reusing F-013's compression.
- **Options chosen and why:** operator document at registration vs after verification (Q1); role-prefixed NIF ids so a citizen and an operator may share one NIF; download URL in `photoPath`; council domain read from `municipality/main`, not a constant; impact switch stored only (D-014 uses it).
- **Class material vs beyond:** P08 form validation and `EditContactViewModel`; P09 picker and camera; P10 location; slides 08 WorkManager. Beyond: Firestore transactions, `NonCancellable` clean-up, the redirect hold, the NIF algorithm.
- **Limits:** orphan Auth users if the app dies or the network drops between the two steps; registration needs a network; rules not deployed, so uniqueness and the domain rule rely on the app's transaction and checks (test mode); `nifs` documents reveal which uid owns a NIF to signed-in users; no photo retry UI; testing operators needs a real inbox in the configured domain; pending citizens not gated by the 10 km rule.
- **Improvements:** a "delete my account and start again" action for orphan accounts; prefill the MFA phone with the registration phone; server-side registration (Cloud Function) to make the two steps atomic; stricter NIF prefixes for individuals; password strength meter.
- **Housekeeping found while planning:** two decision records share the number 0015 (`0015-media-preparation-and-uploads.md`, `0015-session-role-navigation-mfa.md`); D-010 uses 0016; the authors should renumber one.

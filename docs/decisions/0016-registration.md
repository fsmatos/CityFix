# 0016: Registration (citizen and operator)

Status: accepted (D-010, Gate 1 on 2026-11-14, approved by Diogo: Q1-Q15 as recommended; slice 7, a draft-rules edit that is not deployed, approved; slice 6, the map pick, skipped). Review round 1 fixes on 2026-11-14 (B1, S1-S3). Review round 2 approved. Amended 2026-11-16 by D-011 ([decision 0019](0019-profile-editing.md)): the form field components moved to `ui/components/AccountFields.kt` (shared with Profile), and the photo uploader uses unique work per uid (`REPLACE`), with the worker also writing `photoUpdatedAt`. Amends [decision 0010](0010-firestore-schema.md) (operator document timing, `photoPath` value) and [decision 0015](0015-session-role-navigation-mfa.md) (redirect hold). Code-level detail: `docs/architecture/overview.md`, "Registration (D-010)". Plan: `docs/work/D-010-registration/00-plan.md`.

**Numbering note (risk R8).** Two records already use the number 0015: `0015-media-preparation-and-uploads.md` (F-013, Francisco) and `0015-session-role-navigation-mfa.md` (D-009, Diogo). This record takes 0016. The clash is for the two authors to settle. D-010 does not renumber either record. **Resolved 2026-11-15 (F-014):** F-013's record was renamed to `0017-media-preparation-and-uploads.md`; D-009 keeps 0015.

**Not verified on a device.** Build and lint pass: 0 errors and 22 warnings, the same as the baseline. The device checks are still pending (Diogo). The list is in `docs/work/D-010-registration/state.md`.

## Context
- **Brief I.2.A (citizen):** name, e-mail, phone, NIF, georeferencing, role Citizen and an optional photo. The NIF is unique for the role. The formats are validated. The account "fica pendente até validação" by NIF. The citizen chooses whether impact detection is on.
- **Brief I.3.A (operator):** name, e-mail, phone, NIF, role Operator and an optional photo. The e-mail must be in the council's domain. The account "fica logo ativo".
- **Decision 0010** names the registration as the one online transaction: `nifs/{ROLE}_{nif}` (field `uid` only) plus `users/{uid}`. The draft `users` create rule required `email_verified == true` for `role: OPERATOR`. Because of that, the F-010 and D-009 handovers said that the operator's `users/{uid}` should be created only after the e-mail is verified.
- **D-009 (decision 0015):** after sign-up the session becomes `NeedsEmailVerification`, and the redirect opens Account setup by itself. Until `users/{uid}` exists, a user who has finished MFA sees Account problem (`ProfileMissing`).
- `createUserWithEmailAndPassword` signs the new user in at once. D-009's redirect would then clear the Register entry and cancel its ViewModel before the NIF transaction runs.
- The rules are drafts and are not deployed (decision 0012 Q5, amended 2026-11-13). The database is in test mode.

## Decision
- **`UsersRepository.register(registration, password)` runs the whole flow in `data/` (Q13).**
  1. Operator only: read `municipality/main.emailDomain` again and check the domain. A mismatch gives `DomainNotAllowed`.
  2. Create the Auth user (`createUserWithEmailAndPassword`).
  3. Run one Firestore transaction. It reads `nifs/{ROLE}_{nif}` and aborts if the document exists (`NifTaken`). Otherwise it writes the NIF document (`uid`) and `users/{uid}`.
  4. If the transaction fails, delete the Auth user inside `withContext(NonCancellable)`. If the delete also fails, sign out.
  5. After the commit, enqueue the optional photo.

  Registration is online only: transactions and sign-up both need the network.
- **The ViewModel cannot cancel a started registration (review B1).** `RegisterViewModel` calls `register` inside `withContext(NonCancellable)`. Back is swallowed by `BackHandler(enabled = isRegistering)`, and the Up arrow is hidden while the redirect is held. So the sign-up and the transaction always run to the end and clean up in order.
- **Both roles get `users/{uid}` in the registration transaction, before the e-mail is verified (Q1).** This is a deviation from the F-010 and D-009 handovers. Reasons:
  - the NIF clash, the "delete the Auth user on failure" rule and the form errors all happen while the user is still on the Register screen;
  - there is one code path for both roles;
  - there is no local store of a half-finished registration.

  It is safe in the app, because the session checks the verified e-mail and the second factor before it reads the profile. An unverified operator only reaches Account setup. It is also safe in the draft rules: `isOperator()` still requires `email_verified == true` on every check, so an operator document written before verification grants nothing.
  - **Draft rules edit (slice 7, `1760f28`).** The `email_verified` term was removed from the operator branch of the `users` **create** rule in `firestore.rules`. The domain term stays, and a comment explains the change. **Not deployed**; no `firebase` command was run. The file is shared, so Francisco must be told.
- **Redirect hold (Q2; amends decision 0015).** `CityFixApp` keeps `holdRedirect` in `rememberSaveable`. The redirect is `LaunchedEffect(role, holdRedirect)` and does nothing while the hold is on. `RegisterScreen` sets the hold through `onBusyChange` while `isRegistering || (failed && accountSignedIn)`, where `accountSignedIn = role != ANONYMOUS` (review S1).
  - On success the hold drops and the role (`SETUP`) differs from `handledRole`, so Account setup opens once.
  - On failure the hold stays until the deleted or signed-out account reaches the session. So Account setup never flashes over the form, whatever order the Firebase callbacks run in.
  - On rotation the hold is restored and the ViewModel keeps running.
  - After process death `failed` is not saved and nothing is registering, so the hold drops (Q11 path below). A `DisposableEffect` `onDispose` also releases the hold, as a safety net.
- **Validation in one pure file, `utils/Validation.kt` (Q5-Q7, Q10, Q15).**
  - E-mail: a regex that allows dots only between characters, on both sides of the `@` (review N2). Login and Forgot password use the same rule (Q15).
  - Operator domain: the part after the last `@`, lowercased, must equal `emailDomain`.
  - Phone: 9 digits starting with 2 or 9 (Q6). The MFA phone keeps "starts with 9" (`isPortugueseMobile`, moved here from `AccountSetupViewModel`).
  - NIF: 9 digits, the first digit not 0, and the mod-11 check digit (weights 9..2; `r = sum % 11`; the check digit is 0 if `r < 2`, else `11 - r`) (Q5).
  - Password: at least 8 characters, with a letter and a digit, plus a confirmation field (Q7).
  - Name: at least 2 characters after trimming.
  - A field shows its error once it has been edited, and Submit shows every error. Submit stays enabled and gates on `isValid` (Q10).
- **Council domain from `municipality/main` (Q9).** `operatorEmailDomain()` reads it, lowercased and without `@`. A missing document or a failed read gives `null`, and operator registration is refused. The read is retried from `init`, from selecting Operator and from Submit (review S2).
- **Photo through WorkManager after the commit (Q3, Q4).** `ProfilePhotoUploader` enqueues `ProfilePhotoWorker` with a network constraint and exponential backoff. The worker compresses the photo with F-013's `ImageCompression`, uploads it to `users/{uid}/photo.jpg` and sets `users/{uid}.photoPath` to the **https download URL** (as F-013 does for ticket media). This amends decision 0010, which left the value open.
- **Georeferencing required for citizens, any accuracy (Q8).** "Use my location" uses F-012's `LocationRepository`. Location permission is asked at that point, and approximate location is accepted. The map pick (slice 6) was skipped.
- **Pending citizens stay ungated (Q12).** They are not added to the 10 km gate (decision 0015, R3-N3).
- **Entry point (Q14):** "Create an account" on the Login screen only.

### Rejected
- **The ViewModel orchestrating `AuthRepository` + `UsersRepository` (Q13):** the "delete the Auth user on failure" rule would live in a ViewModel and depend on its scope.
- **Uploading the photo inline in `register()`:** it blocks the flow on a slow network and breaks the "uploads through WorkManager" rule.
- **A batched write instead of the transaction:** a batch cannot read `nifs` first, so uniqueness would depend on rules that are not deployed.
- **Deferring the operator document until after verification** (the F-010 / D-009 handover): it needs a local store (a second source of truth, which decision 0015 avoided) and two code paths. The NIF clash would then appear after the Auth account exists, on the setup screen, where `delete()` may need a recent login.
- **A local pending-registration store** (DataStore / SharedPreferences): same reason.
- **An `onRegistered` gate in `CityFixApp`** (review S1 option): it would strand a user on Register after process death.
- **Waiting for the auth listener inside `deleteNewUser`** (review S1 option): the session flow and the compose hops can still race.

## Consequences
- **Orphan Auth users (Q11).** If the network drops between the two steps, or the app dies there, an Auth user without a profile may remain. That user sees Account setup and, after MFA, Account problem. The e-mail cannot be used again until the user is deleted **in the console**.
- **Pending citizens are not gated** by the 10 km rule, and anyone can now register (decision 0015, R3-N3).
- **Testing operators needs a real inbox in `emailDomain`**, to verify the e-mail and finish MFA. The console document `municipality/main` must exist.
- **`nifs` documents are readable by any signed-in user** (`get` only, `list` denied). A NIF document reveals only the uid that owns it.
- **The photo is best effort.** It is dropped without a message in these cases: 5 failed attempts; an undecodable file; the user logs out before the upload; the rules refuse the write (review S3). A refused Firestore update after a successful upload leaves `photo.jpg` in Storage without `photoPath`. An "unauthenticated" answer is treated as final, though it may be transient (review R2-N3). A process death right after Submit can delete the queued source file (review N3; the worker logs it).
- **No way to leave the screen while Firebase hangs** (review R2-N4). This is the price of B1's fix. The SDK timeouts bound the wait.
- **Uniqueness and the domain rule rely on the app** while the rules stay undeployed (test mode).
- **Decision 0010 (Francisco's record) is not edited.** The suggested amendment line is in the overview's "Handover (D-010)".

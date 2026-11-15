# D-010: Registration (citizen and operator)
Type: feature · Task file: docs/tasks/phase-2/D-010-registration.md
Labels: -

## Problem
- Accounts exist only when created by hand in the console (D-009 Q9). The brief asks for self-registration:
  - **I.2.A (citizen):** name, e-mail, phone, NIF, georeferencing, role Citizen, optional photo; the NIF is unique for the role; formats validated; the registration "fica pendente até validação" by NIF; the citizen chooses whether impact detection is on.
  - **I.3.A (operator):** name, e-mail, phone, NIF, role Operator, optional photo; the e-mail must be in the council's domain; the account "fica logo ativo".
- D-009 left the pieces around it (overview "Handover (D-009)", decision 0015):
  - after sign-up the session becomes `NeedsEmailVerification` and the redirect opens Account setup (verify e-mail, then SMS enrolment) by itself;
  - until `users/{uid}` exists, a user who finished MFA sees Account problem (`ProfileMissing`);
  - F-010's handover says the operator's `users/{uid}` is created only after the e-mail is verified, because the draft `users` create rule requires `email_verified == true` for `role: OPERATOR` (decision 0010, `firestore.rules:189-191`);
  - the PT mobile regex in `AccountSetupViewModel` should move to `utils/Validation.kt`.
- `UsersRepository` has only `observeUser`. `municipality/main` is not read anywhere (F-012 uses code constants), and the document may not exist in the console yet.
- The Login screen has no way to reach a registration.

## Outcome
- Login gets a **Create an account** button that opens a new **Register** screen (anonymous only; not covered by the 10 km gate, like the other auth screens).
- The Register screen asks for the role first (Citizen / Operator, segmented buttons), then:
  - both roles: name, e-mail, phone (9 digits), NIF (9 digits with the check digit), password and confirmation, optional photo (gallery or camera);
  - citizen only: georeferencing ("Use my location", optionally adjusted on a small map) and an **Impact detection** switch (off by default);
  - operator only: a hint with the council's e-mail domain read from `municipality/main` (for example "Use your @cm-coimbra.pt address").
- Errors appear as the user types (once a field has been edited) and every error is shown on Submit; an invalid form never reaches Firebase.
- Submit:
  1. creates the Firebase Auth user;
  2. runs **one Firestore transaction** that reads `nifs/{ROLE}_{nif}`, aborts if it exists, otherwise writes it (`uid`) and `users/{uid}` (citizen `registrationStatus: PENDING`, operator no status field = active);
  3. if the transaction fails, deletes the Auth user and shows "This NIF is already registered" (or the network message), keeping the form;
  4. on success, the optional photo is compressed and queued for upload to `users/{uid}/photo.jpg` (WorkManager, network constraint), and the app moves to Account setup (verify e-mail, then MFA enrolment, D-009).
- After setup: a citizen lands on the "waiting for validation" screen (PENDING), an operator on the Operations panel (active).
- Rotation (and process death, except the password fields) keeps the form.
- Nothing typed at registration is ever readable by anonymous users (it goes only to `users/{uid}`, `nifs/{id}` holds only the uid, the photo is under `users/{uid}/`).

## Acceptance checks
- [ ] An invalid e-mail, phone (not 9 digits / wrong prefix) or NIF (wrong length or check digit) shows its error while typing, and Submit does not call Firebase (no Auth user appears in the console).
- [ ] Two citizens with the same NIF: the second gets "This NIF is already registered", stays on the form with its values, and has **no** Auth user left in the console. A citizen and an operator with the same NIF both register.
- [ ] An operator e-mail outside `municipality/main.emailDomain` is refused before submit, with the domain shown in the message.
- [ ] A new citizen has `registrationStatus: PENDING` in `users/{uid}` and, after setup, sees the waiting screen; a new operator has no pending status and, after setup, lands on the Operations panel.
- [ ] After a successful registration the app opens Account setup by itself (verification e-mail sent), then SMS enrolment.
- [ ] Rotation at any point of the form (including with the map shown and with a photo chosen) keeps every field, the role, the location and the photo. Rotation while Submit is running does not lose the registration.
- [ ] Citizen: "Use my location" fills the georeferencing (asks for location permission at that point; a denial shows the message and Open settings); the impact switch value is stored in `impactDetection`.
- [ ] Optional photo from gallery and camera: once online, `users/{uid}.photoPath` holds a download URL and `users/{uid}/photo.jpg` is in Storage; registering without a photo leaves `photoPath` absent.
- [ ] Offline, Submit shows "No connection" and creates nothing.
- [ ] EN and PT strings for every new text; content descriptions on the photo, map and switch; 48dp targets; portrait and landscape without clipping.
- [ ] `./gradlew assembleDebug` and `./gradlew lint` pass after each slice; every new lint warning is listed in `state.md`.

## Approach

### Validation (`utils/Validation.kt`, pure Kotlin, no Android imports)
Top-level functions returning `Boolean` (the ViewModel maps them to `@StringRes` errors, as `ReportViewModel` does):
- `isValidEmail(email)`: one `Regex` (`^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$`), on the trimmed value. `emailErrorFor` in `viewmodel/AuthErrorMessages.kt` switches from `android.util.Patterns` to it, so Login, Forgot password and Register share one rule (Q15).
- `isInEmailDomain(email, domain)`: the part after the last `@`, lowercased, equals `domain` (stored lowercase without `@`, decision 0010).
- `isPortuguesePhone(digits)`: 9 digits starting with 2 or 9 (Q6). `isPortugueseMobile(digits)`: 9 digits starting with 9, moved from `AccountSetupViewModel` (D-009 handover), which then calls it.
- `isValidNif(nif)`: 9 digits, first digit not 0, and the mod-11 check digit: `sum = d1*9 + d2*8 + ... + d8*2`, `r = sum % 11`, check digit `= if (r < 2) 0 else 11 - r` (Q5). `123456789` is a valid example for previews and checks.
- `passwordProblem(password)`: an enum `PasswordProblem { TOO_SHORT, NO_LETTER, NO_DIGIT }` or `null` (Q7: at least 8 characters, a letter and a digit; Firebase's own minimum is 6).
- `isValidName(name)`: at least 2 characters after trim.

### Form state (`viewmodel/RegisterUiState.kt`, `RegisterViewModel.kt`, `RegisterViewModelFactory.kt`)
- Follows `ReportViewModel` (P08 `EditContactViewModel`) and the D-009 screen ViewModels: route-scoped, private `MutableStateFlow` + `asStateFlow()`, `update { it.copy(...) }`, one `@StringRes Int?` error per field, computed `isValid`, `submit()` that rebuilds every error and gates on `isValid`.
- **Real-time** (Q10): each `onXChange` stores the value and recomputes that field's error; an empty untouched field shows nothing until Submit. Changing the role clears the role-specific errors; Submit is enabled while not busy and shows all errors plus a summary when invalid.
- **Rotation and process death:** the ViewModel survives rotation; every field except the two passwords is also written to `SavedStateHandle` (role, name, email, phone, NIF, impact switch, latitude/longitude, photo path), as `LoginViewModel` keeps the e-mail and not the password. Factory with the `CreationExtras` overload, like `LoginViewModelFactory`.
- The operator domain is read once in `init` (`usersRepository.operatorEmailDomain()`), kept in the state; `null` (no document, offline on a first install) shows "Operator registration is not available right now" and blocks operator submit.
- `onCleared()` deletes the picked photo file unless the registration succeeded (as `ReportViewModel` does with drafts).

### Data (`data/UsersRepository.kt`, `data/FirestoreUsersRepository.kt`)
- New types next to the interface: `data class Registration(role: RegistrationRole, name, email, phone, nif, georeferencing: Coordinates?, impactDetection: Boolean, photoPath: String?)` (`RegistrationRole { CITIZEN, OPERATOR }`, because the model's `User` subtypes need an id we do not have yet), and `sealed interface RegisterResult { Success; NifTaken; DomainNotAllowed; Failure(error: AuthError) }`.
- `suspend fun register(registration: Registration, password: String): RegisterResult` (task wording; Q13). `FirestoreUsersRepository` gains `auth: FirebaseAuth` and the photo uploader in its constructor (`CityFixApplication` passes them). Steps:
  1. operator only: re-read the domain and check `isInEmailDomain` (defence in depth; the form already checked) -> `DomainNotAllowed`;
  2. `auth.createUserWithEmailAndPassword(email, password).await()`; failure -> `Failure(mapped)` (new `AuthError.EMAIL_IN_USE`, `WEAK_PASSWORD`; network -> `NETWORK`);
  3. `firestore.runTransaction { tx -> if (tx.get(nifRef).exists()) throw NifTakenException; tx.set(nifRef, mapOf("uid" to uid)); tx.set(userRef, userDocument) }.await()`;
  4. transaction failure -> `withContext(NonCancellable) { currentUser.delete().await() }` (best effort; on failure `auth.signOut()`), then `NifTaken` or `Failure(NETWORK/UNKNOWN)`. A `CancellationException` after step 2 also runs the delete in `NonCancellable`, then rethrows;
  5. success: if a photo was chosen, `ProfilePhotoUploader.enqueue(uid, path)`; return `Success`.
- The user document follows decision 0010 and the draft create rule: `role`, `name`, `nif`, `email` (= `auth.currentUser.email`, the token's value), `phone` (9 digits), `createdAt` (epoch ms); citizen also `georeferencing` (`GeoPoint`), `registrationStatus: "PENDING"`, `impactDetection`; both `fcmTokens: []`. No `photoPath` until the upload ends; no `validatedBy` / `validatedAt`.
- `suspend fun operatorEmailDomain(): String?`: `municipality/main` `get().await()` (server or cache), `emailDomain` lowercased, `null` on a missing document or a failed read (Q9).
- The Firebase exception mapping `toAuthError()` moves from `FirebaseAuthRepository` (private) to an `internal` top-level function in `data/` so both repositories use one table; two new cases (`FirebaseAuthUserCollisionException` -> `EMAIL_IN_USE`, `FirebaseAuthWeakPasswordException` -> `WEAK_PASSWORD`), with messages in `AuthErrorMessages.kt`.

### Operator document timing (reconciling the D-009 / F-010 handover; Q1)
- **Recommended: create `users/{uid}` for both roles in the registration transaction**, at once, before the e-mail is verified.
  - Why: the NIF reservation, the "delete the Auth user on failure" rule and the form errors all happen while the user is still on the Register screen; one code path for both roles; no local store of a half-finished registration.
  - Safe in the app: the session checks the verified e-mail and the second factor **before** the profile (`SessionViewModel`), so an unverified operator only reaches Account setup, never an operator screen.
  - Safe in the draft rules if one line changes: `isOperator()` already requires `email_verified == true` on every check (Firestore and Storage), so an operator document written before verification grants nothing. The `email_verified` term in the `users` **create** rule (`firestore.rules:190`) can be dropped while the domain term stays. **This is a change to the shared rules draft and needs Diogo's approval and Francisco's knowledge; it is never deployed** (decision 0012 Q5). Without the change, the draft would refuse the operator create (the database is in test mode, so the app works either way; the draft would just be wrong).
- Alternative (as the handover says): keep the operator's data on the device until the e-mail is verified, then create the documents from Account setup. Rejected as the recommendation: needs a local store (SharedPreferences/DataStore, a second source of truth that decision 0015 avoided), the NIF is reserved only after verification (a NIF clash would then appear on the setup screen, after the Auth account exists, and `delete()` may need a recent login), and two code paths.

### Keeping the Register screen during the sign-up (redirect hold; Q2)
- `createUserWithEmailAndPassword` signs the user in at once. The account flow emits, the session becomes `NeedsEmailVerification` (role `SETUP`), and today's redirect `LaunchedEffect(role)` would navigate to Account setup with `popUpTo(graph)`, clearing the Register entry **and cancelling its ViewModel's coroutine before the transaction runs**; a NIF error could then never be shown, and the Auth user would be left without a profile.
- Fix in `CityFixApp`: `var holdRedirect by rememberSaveable { mutableStateOf(false) }`, set from the Register screen through `onBusyChange(isRegistering)` (a `LaunchedEffect(state.isRegistering)` in `RegisterScreen`); the redirect becomes `LaunchedEffect(role, holdRedirect) { if (holdRedirect) return@LaunchedEffect; ... }`.
  - Success: `isRegistering` false -> hold released -> the role (`SETUP`) differs from `handledRole` (`ANONYMOUS`) -> `goHome()` = Account setup, once.
  - Failure: the Auth user is deleted, the role is `ANONYMOUS` again = `handledRole` -> no navigation; the form stays with the error.
  - Rotation: `rememberSaveable` keeps the hold and the ViewModel keeps running.
  - Process death mid-registration: the new ViewModel is not registering, so the screen releases the hold and the redirect goes to Account setup (and later Account problem, `ProfileMissing`, if the transaction never ran; Q11).
- `TopBarScreen.REGISTER` joins `onAuthScreen` (no gate, no menu), and the account action is `NONE` on the Register screen, so the brief `SETUP` role does not show Log out there.

### Georeferencing (citizen; Q8)
- "Use my location": `LocationRepository.currentLocation()` (F-012). Permission at the point of use with `RequestMultiplePermissions(LOCATION_PERMISSIONS)` as `ReportScreen` does; approximate is accepted (the residence is not used for 10 m grouping). Denied -> the existing `LocationDeniedMessage` with Open settings. Stored as `Coordinates`, shown with 5 decimals; required for citizens.
- Optional slice: a small `GoogleMap` (maps-compose, already in the catalog) with a marker at the fix and `onMapClick` to move it. Only the citizen sees their own point.

### Photo (optional; Q3, Q4)
- Picking as F-007: `PickVisualMedia(ImageOnly)` copied with `AttachmentStorage.copyToInternalStorage` (off the main thread), and `TakePicture` with `FileUtils.createTempImageFile` (FileProvider, no CAMERA permission). Launchers sit in the stateful screen above any orientation branch (F-007 rule). The preview is a Coil `AsyncImage` of the local file.
- Upload: new `data/ProfilePhotoUploader.kt` + `data/ProfilePhotoWorker.kt`, the F-013 pattern (one `OneTimeWorkRequest`, `NetworkType.CONNECTED`, exponential backoff). The worker compresses with `ImageCompression.compressImage` (long side 1600 px, JPEG 80, no EXIF; the thumbnail it also writes is deleted), `putFile` to `users/{uid}/photo.jpg` with `image/jpeg`, `downloadUrl`, then `users/{uid}.update("photoPath", url)` (allowed for the owner by the draft rule), and deletes the local files. Enqueued only after the transaction commits, so `users/{uid}` exists when the update runs.
- `photoPath` holds the https download URL, as F-013 does for ticket media (Q4).

### Navigation and strings
- New `@Serializable data object RegisterRoute` in `Destinations.kt`; `composable<RegisterRoute>` (no `RequireAccess`: anonymous by definition; a signed-in user cannot reach Login, hence not Register); `toTopBarScreen()` and the title.
- `LoginContent` gets `onCreateAccount` ("Create an account" `TextButton`).
- Strings prefixed `register_` in `values/strings.xml` and `values-pt/strings.xml`.

### Alternatives considered
- `AuthRepository.createAccount` + `UsersRepository.createProfile` orchestrated by the ViewModel: more visible, but the "delete the Auth user if the profile fails" rule would live in a ViewModel and depend on its scope. Rejected (Q13).
- Uploading the photo inside `register()` before returning: simpler, but blocks the flow on a slow network and breaks the project rule "upload through WorkManager" (kotlin-conventions).
- A batched write instead of the transaction: queued offline, but cannot read `nifs` first, so uniqueness would depend on rules that are not deployed. Decision 0010 already names the registration as the one online transaction.

## Architecture impact
- `utils/`: new pure `Validation.kt` (no Android imports, so `model/` could use it).
- `data/`: `UsersRepository` grows `register` and `operatorEmailDomain`; `FirestoreUsersRepository` takes `auth` and the photo uploader; new `ProfilePhotoUploader` + `ProfilePhotoWorker`; `toAuthError()` becomes shared; `AuthError` gains `EMAIL_IN_USE`, `WEAK_PASSWORD`.
- `viewmodel/`: new `RegisterViewModel`, `RegisterUiState`, `RegisterViewModelFactory`; `AccountSetupViewModel` and `AuthErrorMessages.kt` use `Validation.kt`.
- `ui/`: new `RegisterScreen.kt` (stateful + stateless, previews portrait/landscape/PT); `LoginScreen.kt` link; `Destinations.kt`, `MainTopAppBar.kt` (`TopBarScreen.REGISTER`), `MainActivity.kt` (route, redirect hold, `onAuthScreen`).
- `CityFixApplication.kt`: `usersRepository` built with `auth` and a `profilePhotoUploader` (`by lazy`).
- Firestore: first real writers of `users/{uid}` and `nifs/{ROLE}_{nif}`; first reader of `municipality/main`. Storage: first writer of `users/{uid}/photo.jpg`.
- `firestore.rules`: one line proposed (Q1), only with Diogo's approval; nothing deployed. `storage.rules` unchanged.
- No `model/` change (`Citizen`, `Operator`, `RegistrationStatus` already have every field).

## Failure modes
| Situation | Behaviour |
| --- | --- |
| Offline at Submit | `createUser` fails -> "No connection"; nothing created. |
| E-mail already used | `EMAIL_IN_USE` -> error on the e-mail field; nothing created. |
| NIF already registered for the role | transaction aborts -> Auth user deleted -> "This NIF is already registered" on the NIF field; form kept (passwords too, the ViewModel is alive). |
| Network lost between Auth creation and the transaction | transaction fails -> delete attempted (also needs network); if it fails, `signOut()`; an orphan Auth user remains (Q11) and the e-mail cannot be reused until it is deleted in the console. Message: "No connection". |
| App killed between Auth creation and the transaction | Auth user without profile: next start -> Account setup -> after MFA, Account problem (`ProfileMissing`). Orphan as above (Q11). |
| Rotation during Submit | ViewModel keeps running; `holdRedirect` restored; the result arrives normally. |
| `municipality/main` missing or unreadable | Operator role shows "not available right now" and cannot submit; citizen unaffected. |
| Location denied / no fix | `LocationDeniedMessage` / "Location unavailable, try again"; citizen cannot submit without a location. |
| Photo upload fails 5 times | Worker gives up; `photoPath` stays absent; the account is fine (photo optional). No retry UI. |
| Photo cannot be decoded (HEIC on an old device) | Worker drops it; no `photoPath`. |

## Risks and open questions
Risks:
- **R1 Redirect cancels the registration.** Covered by the redirect hold; the riskiest piece of the task, and it touches D-009's B1 logic in `MainActivity`. Device check: rotation during Submit, NIF clash, success.
- **R2 Rules draft disagrees with the flow** (operator create before verification). One-line amendment proposed; not deployed either way (Q1).
- **R3 Orphan Auth users** (Q11). Accepted for a prototype, documented, console cleanup.
- **R4 Photo before the user document.** Avoided: the upload is enqueued only after the commit; the Storage rule does not read Firestore for `users/{uid}/photo.jpg` anyway. The path holds the uid, which decision 0010 already allows for profile photos (read by the owner and operators only, never anonymous).
- **R5 Testing the operator path** needs `municipality/main` in the console and a real inbox in its `emailDomain` (to verify the e-mail and finish MFA). Console step for Diogo (Q9).
- **R6 Transaction semantics offline:** Firestore transactions fail without a network (decision 0010); registration is online by design, as is Auth sign-up.
- **R7 Pending citizens are not gated (decision 0015, R3-N3):** from now on anyone can register (password + MFA) and browse public tickets outside 10 km before validation. Q12.
- **R8 Decision numbering:** two records are numbered 0015 (`0015-media-preparation-and-uploads.md`, `0015-session-role-navigation-mfa.md`); D-010's record takes 0016. The clash is for Diogo and Francisco to settle, not D-010.
- **R9 Maps key:** the map-pick slice shows a grey map without the real key in the manifest (F-012 note); it is optional.

Open questions for Gate 1 (each with a recommended answer):
1. **When is the operator's `users/{uid}` created?** Recommended: in the registration transaction, for both roles, and amend the draft `users` create rule to drop `email_verified == true` for operators (domain check kept; `isOperator()` still requires verification everywhere). Needs your approval for the `firestore.rules` edit (shared with Francisco, not deployed). Alternative: defer until verification (local store, two paths).
2. **How is the Register screen kept during sign-up?** Recommended: the `holdRedirect` flag driven by `RegisterUiState.isRegistering`, with the redirect keyed on `(role, holdRedirect)`.
3. **Photo upload.** Recommended: a small `ProfilePhotoWorker` enqueued after the commit (WorkManager, network constraint), reusing `ImageCompression`. Alternatives: inline upload in `register()`; or postpone the photo to D-011 (profile).
4. **`photoPath` value.** Recommended: the https download URL (as F-013), so D-011/D-012 load it with Coil directly. Alternative: the Storage path, resolved on read.
5. **NIF rule.** Recommended: 9 digits, first digit not 0, mod-11 check digit. Alternative: only individuals' prefixes (1, 2, 3, 45), which rejects some valid NIFs a tester might type.
6. **Phone rule for the profile phone.** Recommended: 9 digits starting with 2 or 9 (mobile or landline); the MFA step keeps "starts with 9". Alternative: mobile only.
7. **Password strength and confirmation.** Recommended: at least 8 characters with a letter and a digit, plus a "Confirm password" field. Alternative: Firebase's minimum (6) only.
8. **Georeferencing.** Recommended: required for citizens; "Use my location" (any accuracy) in the main slice; the map pick as a separate, optional slice (drop it if the Maps key is still a placeholder or time is short).
9. **Council domain.** Recommended: `UsersRepository.operatorEmailDomain()` reads `municipality/main`; you create that document in the console (`name`, `emailDomain`, `center`, `radiusKm`, decision 0010). For testing and the defence, set `emailDomain` to a domain where you can receive mail, and say so at the defence. Missing document -> operator registration refused.
10. **When errors show.** Recommended: per field once it has been edited, all on Submit; Submit stays enabled and gates on `isValid` (as the report form, F-006 review S2). Alternative: Submit disabled while invalid.
11. **Orphan Auth users** (app killed or network lost mid-registration). Recommended: accept and document; clean up in the console. Alternative (later task): a "Delete account" action on the Account problem screen.
12. **Pending citizens and the 10 km gate (R3-N3).** Recommended: unchanged (not gated, decision 0015 reading). Alternative: add `PENDING_CITIZEN` to `showGate` now (one line) since registration makes it reachable.
13. **Where `register` lives.** Recommended: `UsersRepository.register(...)` (task wording), with `FirestoreUsersRepository` receiving `auth` and the photo uploader. Alternative: ViewModel orchestrating `AuthRepository` + `UsersRepository`.
14. **Entry point.** Recommended: "Create an account" on the Login screen only. Alternative: also on the 10 km gate.
15. **One e-mail rule.** Recommended: `emailErrorFor` (Login, Forgot password) uses the new `isValidEmail` instead of `android.util.Patterns`. Alternative: keep `Patterns` there and use the regex only in Register.

## Out of scope
- Operator validation of pending citizens (D-012); profile editing and photo change (D-011); impact detection itself (D-014; D-010 only stores the switch).
- Prefilling the MFA phone in Account setup with the registration phone (possible improvement).
- Deleting orphan Auth accounts from the app; re-sending a registration after process death.
- Deploying any rules (decision 0012 Q5); console changes (Diogo does them).
- Automated tests (not required).

## Slices
Each leaves the build green and becomes one commit.
1. **Validation helpers.** `utils/Validation.kt`; `AccountSetupViewModel` uses `isPortugueseMobile`; `emailErrorFor` uses `isValidEmail` (Q15).
2. **Registration data layer.** `Registration`, `RegistrationRole`, `RegisterResult`, `register`, `operatorEmailDomain` in `UsersRepository` / `FirestoreUsersRepository`; shared `toAuthError()` + `EMAIL_IN_USE`, `WEAK_PASSWORD`; `CityFixApplication` wiring. No caller yet (photo path parameter accepted but not uploaded until slice 5).
3. **Register screen, both roles, redirect hold.** Route, Login link, `RegisterUiState` / `ViewModel` / `Factory`, `RegisterScreen` (role, fields, password, impact switch, domain hint, real-time errors, Submit), `TopBarScreen.REGISTER`, `holdRedirect` in `CityFixApp`, strings EN/PT. Operators can register end to end; citizens are blocked by the missing location until slice 4 (`isValid` needs it).
4. **Citizen location.** "Use my location" with the permission at point of use, `LocationDeniedMessage`, saved coordinates.
5. **Optional photo.** Gallery + camera on the form, `ProfilePhotoUploader` + `ProfilePhotoWorker`, enqueue from `register`, file clean-up in `onCleared`.
6. **(Optional, Q8) Map pick.** Small `GoogleMap` with a marker and `onMapClick` to adjust the citizen's point.
7. **(Only if Q1 approved) Rules draft line.** Drop `request.auth.token.email_verified == true` from the operator branch of the `users` create rule in `firestore.rules`, with a comment; not deployed.

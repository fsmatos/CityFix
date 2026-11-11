# D-009: Login, MFA, password recovery and role navigation
Type: feature · Task file: docs/tasks/phase-2/D-009-auth-mfa-role-navigation.md
Labels: -

## Problem
- Every user is anonymous today. `CityFixApp` (in `MainActivity.kt`) has no session, `canExpandCards` is the constant `false` (D-007, decision 0006), and the report route and the top bar's add button are open to everyone. The brief allows reporting only for registered citizens (I.2.E; overview "Top bar (D-005)", "Requirement for the Auth phase").
- F-010 left `CityFixApplication.auth` and `firestore` unused, with the D-009 handover in the overview ("Handover (F-010)"):
  - operators need a **verified** e-mail (`isOperator()` checks `email_verified`);
  - refresh the token after verification;
  - rules are deployed once login works (decision 0011 point 5).
- The brief asks for (summarised from the task file; the PDF was **not checked in this session**, no PDF tools):
  - e-mail/password with MFA and password recovery by e-mail (I.2.B, I.3.B);
  - the session cached on the device after the first login (IV);
  - three profiles with different features (I), with top-bar actions changing by role and screen (V).

## Outcome
- Anonymous users see the same dashboard, list and detail as today, plus a **Log in** action in the top bar.
- Login flow:
  1. E-mail and password.
  2. If the account has a second factor: a code screen (SMS to the enrolled phone; console test numbers in development).
  3. "Forgot password?" sends the Firebase reset e-mail.
- First login of an account without a second factor (every console-created test user, and every D-010 registration):
  1. "verify your e-mail" step (Firebase verification link, then "I have verified");
  2. "add your phone" step: SMS code, then enrol.
  The role features open only after both steps. MFA is **mandatory** for citizens and operators (Q1).
- After login the app goes to the role's home, and the back stack is reset:
  - **active citizen:** the dashboard; the add (report) button is visible; menu entries My tickets and Profile;
  - **pending citizen:** a "waiting for validation" screen with a button to the public dashboard and list, but no report (Q2). When an operator activates the account, the screen changes live, through the `users/{uid}` listener;
  - **operator:** an Operations panel placeholder; menu entries Approvals, Categories and Profile;
  - placeholders are used where the real screen belongs to a later task (F-014, D-011, D-012, D-013).
- **Log out** in the top bar (or the menu) signs out and returns to the anonymous dashboard.
- The session survives closing and reopening the app, offline too:
  - Firebase Auth persists the user;
  - `users/{uid}` comes from Firestore's persistent cache.
- Logged out, or with the wrong role, the report and operator routes cannot be reached: their buttons are hidden, and a guard on each destination redirects.
- `canExpandCards` comes from the session (Q7).

## Acceptance checks
- [ ] A test active citizen and a test operator, each with a test phone number, log in and land on different home screens: the dashboard with the add button, and the Operations panel placeholder.
- [ ] A wrong second-factor code shows "Wrong code" and stays on the code step. The right code logs in.
- [ ] A new console user (no factor) is taken through verify e-mail and add phone. Until the enrolment ends, they cannot reach the report form or the role menu entries.
- [ ] Closing the app (swipe away) and reopening it keeps the session and the role home. The same holds in airplane mode after one online login.
- [ ] "Forgot password?" for a test user's e-mail: the reset e-mail arrives in that inbox.
- [ ] Logged out, the add button and the role menu entries are hidden. The report and operator destinations, if reached through the back stack after logging out, redirect to the dashboard.
- [ ] A pending citizen sees the waiting screen and no add button. After the person sets `registrationStatus` to `ACTIVE` in the console, the app switches to the citizen home without a restart.
- [ ] Rotation on Login, the code step, Forgot password and Account setup keeps the typed values and the current step.
- [ ] EN and PT strings for every new text. TalkBack labels on the new top-bar actions. 48dp targets.
- [ ] `./gradlew assembleDebug` and `./gradlew lint` pass after each slice. Every new lint warning is listed in `state.md`.

## Approach

### Data layer (Diogo's `data/AuthRepository.kt`, `data/UsersRepository.kt`)
- The interfaces are what the ViewModels see (kotlin-conventions: ViewModels behind repository interfaces). The Firebase implementations live next to them, like `TicketsRepository` / `LocalTicketsRepository`:
  - `AuthRepository` / `FirebaseAuthRepository(auth)`;
  - `UsersRepository` / `FirestoreUsersRepository(firestore)`.
  `CityFixApplication` creates both `by lazy` from its `auth`/`firestore` (decision 0009 pattern).
- `AuthRepository` methods:
  - `account: StateFlow<AuthAccount?>`, a plain snapshot `uid, email, isEmailVerified, hasSecondFactor`. An `AuthStateListener` feeds it, and the repository re-emits it after `reload()` / enrol, because the listener does not fire for those;
  - `signIn(email, password): SignInResult`;
  - `sendSecondFactorCode(activity, challenge)` / `resolveSecondFactor(challenge, verificationId, code)`;
  - `sendPasswordReset(email)`;
  - `sendEmailVerification()` / `reloadAccount()` (reload + `getIdToken(true)`, the F-010 handover);
  - `sendEnrolmentCode(activity, phone)` / `enrolSecondFactor(verificationId, code)`;
  - `signOut()`.
  All are `suspend` with `await()`.
- `UsersRepository.observeUser(uid): Flow<UserProfile>`:
  - it uses the `users/{uid}` `snapshots()`, with the cache served first;
  - it maps `role` to `Citizen` / `Operator` (decision 0010; `role` stays out of `model/`);
  - `UserProfile` is `Found(user)`, `Missing` (no document or unknown role) or `Unavailable` (offline with no cache).
  Only the read part is in D-009. `register` is D-010; `updateProfile` is D-011.
- **Deviation from the task wording:** the task names `AuthRepository.currentUser: Flow<User?>`. The session needs more than `User?`: verified e-mail, second factor, a missing or not-cached profile. So the auth flow and the users flow stay separate, and `SessionViewModel` combines them. See Q3.
- **Firebase types stay in `data/`.** The MFA resolver is wrapped in an opaque `SecondFactorChallenge` (`data/`), which carries the resolver and the masked phone. The ViewModel holds it in a private field, never in `UiState` or saved state. After process death the login falls back to the credentials step.

### MFA (factor fixed at F-010 Gate Q7: Identity Platform SMS with test numbers; confirm at this Gate)
Checked on 2026-11-11 against the official docs (https://firebase.google.com/docs/auth/android/multi-factor, https://firebase.google.com/docs/auth/android/phone-auth, https://docs.cloud.google.com/identity-platform/docs/test-phone-numbers). The APIs below are in `firebase-auth` from BoM 34.19.0 (well past the 21.2.0 / BoM 31.4.0 floor for Play Integrity).
- **Sign-in:**
  1. `signInWithEmailAndPassword` throws `FirebaseAuthMultiFactorException`.
  2. `e.resolver` gives the `MultiFactorResolver`; pick the first hint with `factorId == PhoneMultiFactorGenerator.FACTOR_ID` (`PhoneMultiFactorInfo`).
  3. Build `PhoneAuthOptions` with `setMultiFactorHint(hint)`, `setMultiFactorSession(resolver.session)`, `setActivity(activity)`, `setTimeout(60s)`, `requireSmsValidation(true)` and the callbacks, then call `PhoneAuthProvider.verifyPhoneNumber(options)`.
  4. `onCodeSent(verificationId)`, then the user types the code.
  5. `PhoneAuthProvider.getCredential(verificationId, code)` -> `PhoneMultiFactorGenerator.getAssertion(credential)` -> `resolver.resolveSignIn(assertion)`.
  6. A wrong code throws `FirebaseAuthInvalidCredentialsException`, shown as "Wrong code".
- **Enrolment** (signed in, e-mail verified):
  1. `user.multiFactor.session`.
  2. `PhoneAuthOptions` with `setPhoneNumber(e164)`, `setMultiFactorSession(session)`, `setActivity` and the callbacks; `verifyPhoneNumber`; `onCodeSent`.
  3. `getCredential` -> `getAssertion` -> `user.multiFactor.enroll(assertion, displayName)`.
  4. The docs say "re-authenticate the user" first. The user has just logged in, so a `FirebaseAuthRecentLoginRequiredException` is handled by signing out and asking for the login again.
- **Prerequisites (docs):**
  - Identity Platform upgrade;
  - SMS MFA enabled under Sign-in method > Advanced;
  - **e-mail verified** ("MFA requires email verification"), hence the verify step before enrolment;
  - SHA-1 (and SHA-256 for Play Integrity) in the console.
- **Activity:** `setActivity` has been optional since 21.2.0, but without it the reCAPTCHA fallback throws `FirebaseAuthMissingActivityForRecaptchaException`. reCAPTCHA is used when Play Integrity cannot vouch for the app, and the docs list "not distributed through the Play Store", which covers our debug builds. So the stateful screen reads `LocalActivity.current` (activity-compose 1.13.0) and passes it to the ViewModel call. The ViewModel passes it straight to the repository and keeps no reference to it.
  - The docs say the callbacks are bound to that activity and detached when it stops. A rotation during the send can therefore lose `onCodeSent`. The repository wraps the callbacks in `suspendCancellableCoroutine` with a 60 s `withTimeout`, so the step then shows "Could not send the code, try again" instead of hanging.
- **Instant verification / auto-retrieval:** `requireSmsValidation(true)` turns off instant verification. If `onVerificationCompleted` still arrives (auto-retrieval), its `smsCode` only fills the code field. There is one completion path.
- **Test numbers:** Identity Platform, Multi-factor SMS tab, up to 10, E.164 with a fixed 6-digit code. No SMS is sent and no quota is used.
  - `setAppVerificationDisabledForTesting()` is documented for phone sign-in only, not for MFA, so it is **not** used: the real Play Integrity / reCAPTCHA path runs (risk R2).
- Rejected:
  - TOTP: needs an authenticator app at the defence (decision 0011);
  - an own e-mail one-time code: needs a server;
  - optional MFA: the brief asks for MFA (not checked against the PDF, Q1).

### Session (Q3)
- `SessionViewModel(authRepository, usersRepository)` is created in `CityFixApp` **above the `NavHost`** with `SessionViewModelFactory`, like `TicketsViewModel`, so it is activity-scoped and survives rotation. It exposes `session: StateFlow<Session>`:
  - it combines `account` with `flatMapLatest { observeUser(uid) }`;
  - `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Session.Loading)`.
- Sealed type `viewmodel/Session.kt`:
  ```kotlin
  sealed interface Session {
      data object Loading : Session              // first auth + profile read not back yet
      data object Anonymous : Session
      data class NeedsEmailVerification(val email: String) : Session
      data class NeedsSecondFactor(val email: String) : Session
      data object ProfileMissing : Session       // Auth user without users/{uid} (D-010 not finished)
      data object ProfileUnavailable : Session   // offline and never cached
      data class SignedIn(val user: User) : Session   // Citizen or Operator
  }
  ```
  Order of checks: no account -> Anonymous; not verified -> NeedsEmailVerification; no factor -> NeedsSecondFactor; profile missing/unavailable; else SignedIn.
- Pure helpers in the same file:
  - `enum class AccessRole { LOADING, ANONYMOUS, SETUP, ACCOUNT_PROBLEM, PENDING_CITIZEN, CITIZEN, OPERATOR }`;
  - `Session.accessRole`;
  - `Session.canReport` (active citizen only);
  - `Session.canExpandCards` (Q7).
  These are plain `when` expressions, easy to explain at the defence.
- Rejected:
  - reading `FirebaseAuth` in composables (`.claude/rules/kotlin.md`);
  - a `CompositionLocal` for the session (hidden dependency, decision 0007 rejected the same for audience);
  - Hilt (decision 0009).

### Login as destinations (Q6)
- New `@Serializable` routes: `LoginRoute`, `ForgotPasswordRoute(email: String = "")`, `AccountSetupRoute`.
- **The second-factor code is a step of `LoginRoute`, not its own route.** The resolver cannot go into a route (it is not serialisable), and it must stay in the route-scoped `LoginViewModel`. `LoginUiState.step` is `CREDENTIALS` or `SECOND_FACTOR`.
- `MfaScreen.kt` holds a stateless `MfaCodeContent(...)`, reused by the login code step and the enrolment code step.
- Each screen follows the P08 split: a stateful `XxxScreen(viewModel, ...)` and a stateless `XxxContent(state, ...)` with previews (portrait, landscape, PT). Each has its own `XxxViewModel` + `XxxUiState` + factory in `viewmodel/`, created inside its `composable<>` (route-scoped, like `ReportViewModel`, decision 0008).
- Rejected: one big "auth" screen with all states in `CityFixApp`, which is harder to explain and loses the route-scoped ViewModel lifetimes.

### Route guarding (Q4)
- **Redirect on role change.** One `LaunchedEffect(session.accessRole)` in `CityFixApp` navigates to `homeFor(role)` with `popUpTo(graph start) { inclusive = true }`:
  - `ANONYMOUS` -> `DashboardRoute`;
  - `CITIZEN` -> `DashboardRoute`;
  - `PENDING_CITIZEN` -> `PendingValidationRoute`;
  - `OPERATOR` -> `OperatorPanelRoute`;
  - `SETUP` -> `AccountSetupRoute`;
  - `ACCOUNT_PROBLEM` -> `AccountProblemRoute` (ProfileMissing / ProfileUnavailable);
  - `LOADING` never navigates.
  The last role handled is kept in `rememberSaveable`, so rotation or process recreation does not reset the back stack. Only a real change does (login, logout, activation).
- **Destination guard.** A small `RequireAccess(allowed, session, onDenied) { content }` composable wraps the report and every role-only destination. If the role is not allowed, it draws nothing and calls `onDenied`, which navigates home. This covers back-stack leftovers and future deep links.
- **Hidden entry points.**
  - The add button shows only if `session.canReport`.
  - The role menu entries come from `menuItemsFor(role)`.
  - Log in shows for ANONYMOUS; Log out for every signed-in state.
  - Login, Forgot password and Account setup show neither the add button nor the menu.
- `NavHost` keeps `startDestination = DashboardRoute`. While `Session.Loading`, `CityFixApp` shows a centred progress indicator instead of the `NavHost`, so an operator never sees the dashboard flash.
- Rejected:
  - building the graph per role (navigating to an unregistered route throws `IllegalArgumentException`, a crash path);
  - changing `startDestination` with the session (it resets the graph).

### Placeholders (scope)
- A `PlaceholderScreen(titleRes)` in `ui/screens/` says "Coming soon".
- Routes for it:
  - `MyTicketsRoute` (F-014, Francisco);
  - `ProfileRoute` (D-011);
  - `OperatorPanelRoute` (D-013);
  - `ApprovalsRoute`, `CategoriesRoute` (D-012).
  The later tasks replace the content in their `composable<>`.
- `PendingValidationScreen` is real, not a placeholder (Q2).
- Detail audience stays `DetailAudience.PUBLIC` for everyone: `tickets` is still the projected public list (decision 0006 consequence: registered and operator views need an unprojected source, F-011/F-014/D-013).

### Test accounts (Q9)
- Registration is D-010, so the person creates the test accounts in the console (steps in "Console steps" below):
  - Auth users with **real, reachable** e-mails (plus-addressing on one inbox is fine), which the verify and reset e-mails need;
  - a `users/{uid}` document per user, typed by hand with the 0010 fields;
  - optionally `nifs/{ROLE}_{nif}`, so D-010's uniqueness check sees them.
- Test mode (until 2026-12-10) allows these writes and reads.
- The app takes each new user through verification and enrolment on first login, so no MFA set-up happens in the console beyond the test numbers.
- Rejected: a debug-only seeding screen or code. It writes `role: OPERATOR` from the client, which the deployed rules forbid without a verified domain e-mail, and it is code to defend for no feature.

### Console steps for the person (Claude does none of these, and does not touch `google-services.json`)
1. Confirm the project is on **Blaze** with a budget alert (decision 0011). Identity Platform's free tier covers 50k MAU on Blaze; test numbers send no SMS.
2. **Authentication > Settings: upgrade to Firebase Authentication with Identity Platform.**
3. **Authentication > Sign-in method:** Email/Password enabled (F-010). Under **Advanced**, enable **SMS Multi-factor Authentication**.
4. **Test numbers:** in the Multi-factor SMS settings (Google Cloud console > Identity Platform > Identity Providers > tab "Multi-factor SMS" > Edit > "Phone numbers for testing"), add one number per test user in E.164, for example `+351 91x xxx xxx`, with a 6-digit code. Do not use real numbers, and keep the values out of git.
5. **Project settings > Your apps > `pt.isec.amov.cityfix`:** add the debug **SHA-1** and **SHA-256**. `./gradlew signingReport` prints them. Claude may run it with the person's OK; it reads only the debug keystore. Firebase may then offer a new `google-services.json`, and only the person replaces it.
6. **Authentication > Settings > SMS region policy:** allow Portugal only (new projects allow no region). This matters only for real numbers.
7. **Authentication > Users > Add user** (example set):

   | Test user | Role | `users/{uid}` fields to add |
   | --- | --- | --- |
   | citizen-active | CITIZEN | `role`, `name`, `nif`, `email` (same as Auth), `phone`, `createdAt` (number, epoch ms), `georeferencing` (geopoint), `registrationStatus: "ACTIVE"`, `impactDetection: false`, `fcmTokens: []` |
   | citizen-pending | CITIZEN | same, with `registrationStatus: "PENDING"` |
   | operator | OPERATOR | `role`, `name`, `nif`, `email`, `phone`, `createdAt`, `fcmTokens: []` (no citizen fields) |

   The document id is the user's **UID** from the Users tab. The operator's e-mail should be in `municipality/main.emailDomain` if a mailbox there exists. Otherwise accept a mismatch for testing and note it: the console write bypasses the create rule, and `isOperator()` only checks the role and the verified e-mail.
8. Optional: **Authentication > Templates**, set the reset and verification e-mail language to Portuguese.
9. **Rules deploy:** not part of D-009's code. Once the acceptance checks pass, Claude asks the person for permission to deploy `firestore.rules` and `storage.rules` (decision 0011 point 5). Claude never runs `firebase deploy` without that OK (Q10).

## Architecture impact
- New in `data/`:
  - `AuthRepository.kt` (interface + result types + `SecondFactorChallenge`);
  - `FirebaseAuthRepository.kt`;
  - `UsersRepository.kt`;
  - `FirestoreUsersRepository.kt` (read-only mapping `users/{uid}` -> `Citizen`/`Operator`).
- New in `viewmodel/`:
  - `Session.kt`;
  - `SessionViewModel.kt` + factory;
  - `LoginViewModel`/`LoginUiState`;
  - `ForgotPasswordViewModel`/`UiState`;
  - `AccountSetupViewModel`/`UiState`;
  - factories, one per ViewModel, as in the existing code.
- New in `ui/screens/`:
  - `LoginScreen.kt`, `MfaScreen.kt` (stateless code entry), `ForgotPasswordScreen.kt`;
  - `AccountSetupScreen.kt`, `PendingValidationScreen.kt`, `PlaceholderScreen.kt`.
- `ui/navigation/`:
  - new routes in `Destinations.kt`;
  - `MainTopAppBar` gains an account action (Log in / Log out), an overflow menu with role entries and a `showAddAction` flag, and keeps `showExpandToggle`;
  - `TopBarScreen` gains one value per new route (exhaustive titles);
  - a new `RoleAccess.kt` with `homeFor`, `menuItemsFor` and `RequireAccess`.
- `CityFixApplication.kt`: two `by lazy` repositories (`authRepository`, `usersRepository`). The file is shared (F-010 container); tell Francisco (Q11).
- `MainActivity.kt` (`CityFixApp`):
  - `SessionViewModel` above the `NavHost`;
  - the loading gate and the redirect `LaunchedEffect`;
  - `canExpandCards` from the session;
  - the guarded report route;
  - new destinations.
- Strings EN/PT: about 50 new keys, prefixes `login_`, `mfa_`, `forgot_`, `setup_`, `pending_`, `placeholder_`, `menu_`, `top_bar_title_`, `cd_`.
- No change to `model/`, `TicketsRepository`, the rules, Gradle or the manifest. The `INTERNET` permission is already merged from Firebase, and `LocalActivity` is in activity-compose 1.13.0.
- New decision record **0013** (session and role navigation, MFA flow). See 03-docs-plan.

## Failure modes
| Situation | Behaviour |
| --- | --- |
| Wrong e-mail/password (`FirebaseAuthInvalidCredentialsException`, `INVALID_LOGIN_CREDENTIALS` with e-mail enumeration protection) | "E-mail or password is wrong" (one message, no account enumeration) |
| Disabled account (`FirebaseAuthInvalidUserException`) | "This account is disabled" |
| No network at login (`FirebaseNetworkException`) | "No connection. Try again." Login needs the network; the cached session does not |
| Too many attempts (`FirebaseTooManyRequestsException`, also the SMS quota) | "Too many attempts. Wait and try again." |
| Wrong MFA code (`FirebaseAuthInvalidCredentialsException` on `resolveSignIn` / `enroll`) | "Wrong code", stays on the step |
| Code expired / session expired (`FirebaseAuthInvalidCredentialsException` / `FirebaseAuthException` codes) | "The code expired. Send a new one." plus Resend |
| `onCodeSent` lost (rotation detaches the callbacks; 60 s timeout) | "Could not send the code. Try again." |
| reCAPTCHA needed and no activity (`FirebaseAuthMissingActivityForRecaptchaException`) | Cannot happen, because the activity is always passed. Mapped to the generic send error |
| Enrol while the e-mail is not verified | Prevented by the order of the `Session` states. If Firebase still refuses: the generic error, back to the verify step |
| `FirebaseAuthRecentLoginRequiredException` on enrol | Sign out, "Please log in again" |
| Auth user without `users/{uid}`, or an unknown `role` | `ProfileMissing` screen with Log out (D-010 completes registration). Never guess a role |
| Offline with no cached profile | `ProfileUnavailable`: "Your profile could not be loaded offline" with Retry and Log out |
| Profile document changes (activation, role) | The listener emits, the role changes and the redirect runs |
| Process death on the code step | The resolver is lost, so the step returns to credentials with the e-mail kept (`SavedStateHandle`) |
| Logout while a role screen is open | Role becomes ANONYMOUS, the redirect goes to the dashboard and the back stack is cleared |
| Password reset for an unknown e-mail | Same confirmation as for a known one (no enumeration) |

## Risks and open questions

### Risks
- **R1, console set-up blocks testing.** Without the Identity Platform upgrade, SMS MFA enabled, test numbers and the SHA fingerprints, slices 5-6 cannot be checked on a device. Slices 1-4 can be checked with any console user, so do the console steps before slice 5.
- **R2, reCAPTCHA / Play Integrity on the emulator.** Debug builds are not from the Play Store, so verification may open a reCAPTCHA web page (Chrome Custom Tab) even for test numbers, and an emulator without Google Play images may fail.
  - Use an AVD with a Google Play image.
  - If it still fails, the fallback is `setAppVerificationDisabledForTesting(true)` in debug builds only. The docs do not confirm it for MFA, so it needs the person's OK and is recorded as a deviation.
- **R3, rotation during code send.** The docs bind the callbacks to the activity. Handled by the timeout and Resend, accepted for the prototype. Recorded in 0013.
- **R4, email-enumeration protection** changes error codes, giving one generic invalid-credentials error. The mapping does not rely on separate "user not found" / "wrong password".
- **R5, shared files.**
  - `CityFixApplication.kt` (F-010 container), `values*/strings.xml`, and `ui/navigation` routes that Francisco's F-014 will fill.
  - Merge `origin/main` the same day.
  - Tell Francisco the new route names and `Session` API, which F-014 and F-016 need for "my tickets" and the uid.
- **R6, brief not read in this session.** The PDF cannot be opened here. These items rely on the task-file summaries and are **not checked against the brief**: MFA mandatory for both roles (Q1); the pending behaviour (Q2); `canExpandCards` by role (Q7); the FAB wording of brief V (the add action stays in the top bar, D-005); the session cache (IV).
- **R7, Contacts_#3 not extracted.** This session has no unzip tool, so the class example was not re-read. The plan follows the Contacts patterns the codebase already cites: `ContactsApp` container, `EditContactViewModelFactory`, route-scoped edit ViewModel with UiState. Check `EditScreen` / `EditContactViewModel` by hand before slice 3 if you want the exact validation style.
- **R8, size.** Six slices on a target of 11-13. If time runs short, slice 6 (MFA sign-in) cannot be skipped (acceptance). The placeholders (slice 4) are the part to trim.

### Open questions (recommended answer in **bold**)
- **Q1, MFA enrolment: where and how strict.** **In D-009, mandatory for both roles.** After the first login, an account with no factor goes through Account setup: verify the e-mail, then enrol the phone. D-010 only navigates there after registration (its task file already says "go to MFA enrolment (D-009)"). Alternatives: enrolment in D-010 (but test users exist before D-010), or optional MFA (weaker against I.2.B/I.3.B). Not checked against the brief.
  - **Sub-question:** also enforce it server-side with `request.auth.token.firebase.sign_in_second_factor != null` in `isActiveCitizen()`/`isOperator()`. **Recommended: yes, added to the rules at the deploy step, with the person's OK.** It changes `firestore.rules`/`storage.rules`, which needs the person's OK under CLAUDE.md. Answer at this Gate whether D-009 may edit the rule files (no deploy).
- **Q2, pending citizen.** **They can log in (after MFA). Home is a `PendingValidationScreen` ("Your registration is waiting for validation by the council") with a "Browse public issues" button to the dashboard. They have the anonymous features only: no report, no My tickets; Profile is allowed (D-011). Activation switches them live to the citizen home.** Alternative: a banner over the normal dashboard, which is less explicit. Not checked against the brief.
- **Q3, session shape.** **The sealed `Session` above, in an activity-scoped `SessionViewModel` above the `NavHost`. `AuthRepository.account` and `UsersRepository.observeUser` stay separate and are combined in the ViewModel** (deviation from the task's `currentUser: Flow<User?>`, which cannot express verification, second factor or a missing profile). Alternative: keep `currentUser: Flow<User?>` plus separate booleans, which spreads the state over several flows.
- **Q4, route guarding.** **Hide the entry points, plus `RequireAccess` on each guarded destination, plus one redirect on role change (saveable "last role").** Alternatives: per-role graphs (crash on unknown route), or a guard in every ViewModel (Firebase rules do the real enforcement anyway once deployed).
- **Q5, offline / cached session (brief IV).** **Rely on Firebase Auth persistence plus the Firestore persistent cache for `users/{uid}`; no copy in DataStore or SharedPreferences.** Offline after one online login: SignedIn from the cache. Offline with nothing cached: `ProfileUnavailable`, with Retry and Log out. Login itself needs the network. Alternative: cache the role in DataStore, which means a second source of truth.
- **Q6, login: route or state.** **Login, Forgot password and Account setup are routes. The second-factor code is a step inside `LoginRoute`** (the resolver cannot go into a route). `MfaScreen.kt` is the shared stateless code-entry content.
- **Q7, `canExpandCards` by role.** **`true` only for `SignedIn(Operator)`, `false` for anonymous, pending and active citizens.** The list is the public list, and brief I.2.D gives registered citizens the anonymous features, so their cards stay as anonymous ones. The tickets are still projected, so an operator's expanded card shows only the date until D-013 gives operators an unprojected source. Alternative: true for active citizens too, but they would see "Reported on" of other people's tickets, which 0006 keeps hidden from the public. Not checked against the brief.
- **Q8, e-mail verification.** **Required for both roles before enrolment** (Firebase requires it for MFA; the rules require it for operators). The setup step sends `sendEmailVerification()`, and "I have verified" runs `reload()` + `getIdToken(true)`, with Resend and Log out.
- **Q9, seeding test accounts.** **Console only: Auth users with real inboxes, plus `users/{uid}` and `nifs/{ROLE}_{nif}` documents typed by hand, plus MFA test numbers.** No seeding code. D-010 replaces the documents with real registrations later; the test users can stay for the defence.
- **Q10, deploying `firestore.rules` / `storage.rules`.** **Not in D-009's slices.** After the acceptance checks pass, Claude asks the person. With the OK, the person runs or approves `firebase deploy --only firestore:rules,storage` and checks the test users still work (in particular that the seeded operator is allowed, since `isOperator()` needs `email_verified`). It is recorded in state.md and in the overview "Firebase (F-010)" line. Claude never deploys on its own.
- **Q11, `CityFixApplication.kt` edits.** **Add `authRepository` and `usersRepository` there (two lines, the 0009 container pattern) and tell Francisco.** Alternative: create them in `MainActivity` (breaks the single-container rule).
- **Q12, placeholders.** **Five routes with one shared `PlaceholderScreen`, reachable from the role menu, so "operator routes cannot be reached logged out" is testable now.** The later tasks replace the content. Alternative: only `OperatorPanelRoute`, with the rest added by each task.
- **Q13, MFA hint choice.** **Use the first phone hint.** Test users have one factor; enrolling a second factor is not offered (docs suggest it for recovery, a report note).

## Out of scope
- Registration and the `nifs` transaction (D-010).
- Profile editing, change password, e-mail change (D-011).
- My tickets (F-014).
- Approvals and categories (D-012).
- The operator panel and lifecycle (D-013).
- Unprojected ticket sources and `DetailAudience` mapping (F-011/F-014/D-013).
- FCM token storage (F-016).
- Rules changes and the deploy (Q1 sub-question, Q10).
- Enrolling more than one factor.
- TOTP.
- A real FAB.

## Slices
1. **Repositories:** `AuthRepository`/`FirebaseAuthRepository` (account flow, sign-in without MFA, sign-out, reset, verification, reload) and `UsersRepository`/`FirestoreUsersRepository` (`observeUser`), created in `CityFixApplication`. Not used by the UI yet.
2. **Session:** `Session`, `SessionViewModel` + factory above the `NavHost`, the loading gate, `canExpandCards` and `canReport` from the session. The add button is hidden unless `canReport`, and the report destination is guarded. Everyone is still Anonymous, so the add button disappears for now: expected, and recorded.
3. **Login and password reset:** `LoginRoute` (credentials step), `ForgotPasswordRoute`, the top-bar Log in / Log out, the role-change redirect. Console users without MFA can log in at this point (temporary, closed by slice 5).
4. **Role navigation:** `PendingValidationRoute`, the operator home and the placeholder routes, `RoleAccess.kt` (`homeFor`, `menuItemsFor`, `RequireAccess`), the top-bar overflow menu, guards on every role destination, the `ProfileMissing` / `ProfileUnavailable` content.
5. **Account setup:** `AccountSetupRoute` (verify e-mail, then phone, then code, then enrol), `MfaScreen.kt` code content, `Session.NeedsEmailVerification` / `NeedsSecondFactor` routed there. Needs console steps 2-5.
6. **MFA sign-in:** `FirebaseAuthMultiFactorException` -> `SecondFactorChallenge` -> the code step in `LoginRoute` (send, resend, wrong code, resolve).

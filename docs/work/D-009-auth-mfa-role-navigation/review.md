# D-009 review

Reviewed: `git diff c3e4e66..HEAD` (e38f449..6dcbed3, 31 files) against 00-plan, 01-coding-plan, the task file, the brief I text in state.md, the Gate 1 answers (Q1 mandatory MFA, Q7 no expand toggle), decisions 0006/0010/0011, `firestore.rules`, overview "Handover (F-010)" / "Roles", `.claude/rules/kotlin.md` and `kotlin-conventions`. The reviewer re-ran `./gradlew assembleDebug lint`: build OK, lint 0 errors / 18 warnings, the same as the baseline. Nothing was run on a device.

Summary: the data layer, the session machine and the MFA flows match the plan and the Firebase APIs, and the code is plain enough to explain at the defence. One blocking bug: a cold start does not open the role's home screen, so operators and setup users end up on the public dashboard.

## Blocking

### B1. Cold start never goes to the role's home (`MainActivity.kt:156-165`)
`handledRole` is `null` only on a **fresh** start. After process death `rememberSaveable` restores it. Rotation never changes `role`, because `SessionViewModel` survives it. So the `if (handledRole != null) goHome()` guard skips navigation in exactly one case: a cold start with a persisted Firebase session. In that case the NavHost stays on `DashboardRoute`:
- **operator:** sees the public dashboard. The overflow menu has Approvals / Categories / Profile but no entry for `OperatorPanelRoute`, so the operator home cannot be reached without logging out and in again;
- **SETUP** (logged in, e-mail not verified or no factor): sees the dashboard with only Log out. `AccountSetupRoute` cannot be reached, so the user cannot finish enrolment;
- **pending citizen:** sees the dashboard instead of the waiting screen;
- **ACCOUNT_PROBLEM:** sees the dashboard instead of the problem screen with Retry.

This fails the acceptance checks "closing and reopening keeps the session **and the role home**" and "land on different home screens", and it defeats the Q1 setup gate after a restart. (01-coding-plan slice 3 has the same wording, so the bug comes from the plan, not from the implementer.)

Fix: always redirect on the first non-loading value. A restored back stack already has a non-null `handledRole`, so it is not affected. Put the effect inside the Scaffold content, **after** `NavHost(...)`, so the graph is set before `goHome()` runs:
```kotlin
// after NavHost { ... }, inside the Scaffold content (not reached while Loading)
LaunchedEffect(role) {
    if (role != handledRole) {
        // null = fresh start: nothing restored, so go to the role's home too.
        if (handledRole != null || homeFor(role) != DashboardRoute) goHome()
        handledRole = role
    }
}
```
Update the comment at lines 156-158: "null means a fresh start; a restored value means process recreation, whose back stack is kept". Check on the device: cold start as operator (lands on the Operations panel), as an unverified user (lands on Account setup), and after process death on an operator placeholder (stays there, using "Don't keep activities").

## Should-fix

### S1. ImeAction.Next on the login e-mail field does nothing (`LoginScreen.kt:115`, `LoginScreen.kt:222`)
`KeyboardActions(onAny = { onImeAction() })` replaces the default action for **every** IME action. Login passes `onImeAction = {}`, so "Next" no longer moves focus to the password field (a usability and keyboard-accessibility regression). Fix: make the parameter `onImeAction: (() -> Unit)? = null` and pass `keyboardActions = if (onImeAction == null) KeyboardActions.Default else KeyboardActions(onAny = { onImeAction() })`, then drop `onImeAction = {}` in `LoginContent`. Forgot password keeps its `onSend`.

### S2. A failed reload after a successful enrolment reports an error and leaves the account stale (`FirebaseAuthRepository.kt:137-142`)
If `enroll(...)` succeeds and `user.reload()` then throws (for example a network blip), the function returns an error and `_account` still says `hasSecondFactor = false`. The user stays on the code step with an error. "Confirm" again fails, because the code is already used. The factor is enrolled on the server, but the app only sees it after logging out and in again (and only then asks for the SMS code). Fix: once `enroll` has succeeded, treat the reload as best effort:
```kotlin
user.multiFactor.enroll(...).await()
runCatching { user.reload().await() }   // best effort: the factor is already enrolled
_account.value = auth.currentUser?.toAccount()
null
```
If `enrolledFactors` is still empty without the reload (check on the device), keep the reload but return `null` anyway, and log the case in state.md.

### S3. `catch (e: Exception)` swallows coroutine cancellation (`FirebaseAuthRepository.kt:59, 83, 91, 100, 113, 122, 142`)
`await()` throws `CancellationException` when the calling scope is cancelled (for example `viewModelScope` when Login is popped). The catch turns it into `AuthError.UNKNOWN` and returns normally, which breaks structured concurrency (`.claude/rules/kotlin.md`). It is harmless today, but it is a known trap that the examiner may ask about. Fix: add `catch (e: CancellationException) { throw e }` before each `catch (e: Exception)` (import `kotlinx.coroutines.CancellationException`), or wrap the calls in one private `suspend inline fun <T> firebaseCall(block: suspend () -> T): T` that rethrows cancellation, so the rule lives in one place.

## Nit

- **N1. Stale comments.** `MainActivity.kt:252` still says "Phase 1 has no login: every user is anonymous". Change it to "`tickets` is still the projected public list, so every session gets the PUBLIC detail (decision 0006; F-011/F-014/D-013 add the other sources)". `Destinations.kt:5` says "routes for the phase-1 flow". Change it to "app routes; the role routes and guards are in RoleAccess.kt (D-009)".
- **N2. Code step: on-screen Back and Resend stay disabled while a send is in flight** (`MfaScreen.kt:78, 81`). If `onCodeSent` is lost (R3, rotation), the user waits up to 60 s, and only system Back works on Login. `LoginViewModel` already drops stale answers (`challenge !== current`), so pass `enabled = true` for Back on the login step. For setup, `changeNumber` returns while busy, so either keep it that way or add the same stale-result check before enabling it.
- **N3. System Back on the setup code step leaves the app** (`AccountSetupScreen.kt:119`). The back stack has only this entry. Add `BackHandler(enabled = state.step == SetupStep.CODE, onBack = viewModel::changeNumber)` in `AccountSetupScreen`, like the `BackHandler` on Login's code step.
- **N4. The automatic verification e-mail is sent once per back-stack entry, not once per account** (`AccountSetupViewModel.kt:47, 56`). Each new login or cold start of an unverified account sends another e-mail and may hit `TOO_MANY_REQUESTS`. The intro text (`setup_verify_intro`, "We sent a verification link") is also shown when that send failed. This is acceptable for the prototype. Either reword it to "Open the verification link sent to %1$s" or record the behaviour in 0013.
- **N5. Forgot password saved-state key equals the route argument name** (`ForgotPasswordViewModel.kt:25, 60`). The back-stack `SavedStateHandle` already contains the route's `email` argument under the key `"email"`, so `initialEmail` is never used, and that is hard to explain. Rename the key (`"typedEmail"`) or drop `initialEmail` and say in the KDoc that the handle holds the route argument.
- **N6. An invalid phone number shows "Could not send the code"** (`FirebaseAuthRepository.kt:171-176`). `ERROR_INVALID_PHONE_NUMBER` falls into `CODE_NOT_SENT`. Optional: map it to `setup_error_phone`. Test numbers make this rare.
- **N7. Account/profile gap after the second factor.** Between `resolveSignIn` and the first `users/{uid}` snapshot, the session is still `Anonymous` (the `flatMapLatest` keeps the last value). Login stays busy and hides Log in, so nothing is visible, but say so in 0013 when explaining the state machine.

## Checked and fine
- **Firebase calls:**
  - sign-in resolver: first `PhoneMultiFactorInfo` hint, `setMultiFactorHint` + `resolver.session`;
  - enrolment: `multiFactor.session` + `setPhoneNumber`, then `enroll`;
  - `requireSmsValidation(true)`;
  - `suspendCancellableCoroutine` inside `withTimeoutOrNull(60 s)` with `isActive` checks;
  - the Activity is passed to the builder only. It is held by the pending coroutine for at most 60 s and never stored in a ViewModel or a field;
  - the `MultiFactorResolver` stays inside the opaque `SecondFactorChallenge` in a private ViewModel field and never goes into `UiState` or saved state.
- **Session:** the order of checks matches the plan. `flatMapLatest` cancels the `users/{uid}` `snapshots()` listener on logout or account change (the `callbackFlow` removes it). Retry restarts it through `combine`. An unknown role maps to Missing, and an unknown `registrationStatus` maps to PENDING.
- **Guards:** `RequireAccess` wraps every role destination, and `ReportViewModel` is built only inside the guard. Logout clears the back stack (`popUpTo(graph.id) inclusive`). Back-stack leftovers and process recreation are covered (apart from B1). The add button shows only for an active citizen. The menu comes from `menuItemsFor`. Log out appears in every signed-in state.
- **Rules (`firestore.rules`):** reading one's own `users/{uid}` is allowed (`isSignedIn() && request.auth.uid == uid`). `reloadAccount` does `getIdToken(true)` for the `email_verified` claim (F-010 handover). D-009 writes nothing to Firestore. `isActiveCitizen()` / `isOperator()` do not yet check `sign_in_second_factor`, so MFA is enforced only in the client until the Q1 sub-question is applied at the deploy step (Gate Q1, recorded).
- **Personal data:**
  - anonymous users get nothing new: tickets stay projected and `DetailAudience.PUBLIC`;
  - the session holds only the user's own profile;
  - the phone shown is Firebase's masked hint;
  - no test numbers, codes or secrets are in the diff;
  - `google-services.json` is ignored (`.gitignore:20`);
  - no Firebase import outside `data/` and `CityFixApplication.kt`, and `Firebase.firestore` is only in `CityFixApplication.kt`.
- **Strings:** EN and PT have the same keys, apart from `app_name` and the two `translatable="false"` +351 strings. There are no hard-coded user-visible strings (`FACTOR_NAME` is not shown). PT uses "autarquia".
- **Accessibility:** the new icon buttons have content descriptions, M3 components give 48dp targets, fields use `error()` semantics, messages are polite live regions, headings are marked, and the loading indicator is labelled.
- **Layout:** `FormColumn` scrolls with `imePadding()` before `verticalScroll` and caps the width at 480dp. Every new screen has landscape and PT previews.
- **Gate Q7:** `canExpandCards = false` with the reason in a comment, as decided.

## Needs the device or the console (not verifiable here)
- Console steps 2-8 (Identity Platform, SMS MFA, test numbers, SHA-1/SHA-256, test users and their `users/{uid}`).
- Login per role landing on different homes. After the B1 fix, cold start as operator, pending citizen and setup user.
- Wrong code shows "Wrong code" and the right code logs in (sign-in and enrolment). Confirm the `ERROR_INVALID_VERIFICATION_CODE` / `ERROR_SESSION_EXPIRED` strings in Logcat.
- A new user goes through verify e-mail, then phone, then code, then role home. The factor appears in the console's MFA column. The verify and reset e-mails arrive.
- Pending to ACTIVE switches live without a restart.
- Airplane-mode cold start after one online login: same home. A first install offline: Account problem with Retry. A first install online: check that no Account problem screen flashes before the profile arrives.
- Logout from an operator placeholder: dashboard, and Back leaves the app.
- Rotation on Login, the code step, Forgot password and every setup step. Rotation during the send (R3). Whether a reCAPTCHA page appears and whether solving it within 60 s still delivers `onCodeSent` (R2).
- Process death ("Don't keep activities") on Login's code step (back to credentials, e-mail kept) and on the setup code step (step and number kept).
- PT strings on screen, TalkBack on the top-bar actions, password toggle and code field.
- After the rules deploy (Q10, with Diogo's OK): the test users still work, in particular the console-seeded operator (`email_verified`).

## Round 2

Reviewed: `git diff df2373b..HEAD` (8524ea1 B1, b35c684 S1-S3 + N1-N6, 9674e54 state log). The reviewer re-ran `./gradlew assembleDebug lint`: build OK, lint 0 errors / 18 warnings, the same as the baseline (1 AndroidGradlePluginVersion, 4 GradleDependency, 5 NewerVersionAvailable, 1 RedundantLabel, 7 UnusedResources). EN and PT string keys still match (apart from `app_name` and the two `translatable="false"` +351 strings). No Firebase import outside `data/` and `CityFixApplication.kt`.

**Verdict: approved.** No blocking or should-fix findings remain. Two new nits, both optional.

### B1, checked (`MainActivity.kt:346-357`)
The redirect `LaunchedEffect(role)` now sits inside the Scaffold content, after `NavHost(...)` and after the `return@Scaffold` of the Loading branch. So it is never composed while the session is Loading, and the NavHost has already set its graph during composition when the effect body runs. `handledRole` is still `rememberSaveable` in `CityFixApp`, outside the content lambda, so it is not reset when the Loading gate swaps the content.
- **Fresh cold start** (`handledRole == null`): operator -> `OperatorPanelRoute`; setup user (e-mail unverified, or verified with no factor) -> `AccountSetupRoute`; pending citizen -> `PendingValidationRoute`; ProfileMissing/Unavailable -> `AccountProblemRoute`. Anonymous and active citizen have `homeFor == DashboardRoute`, so the role is only recorded and the start destination stays.
- **Rotation:** the activity-scoped `SessionViewModel` keeps the role, and `handledRole` is restored, so `role == handledRole` and nothing navigates.
- **Process death:** `handledRole` is restored to the old role, and the restored back stack is kept unless the role changed meanwhile (then `goHome()`, which is correct).
- **No loop with `RequireAccess`:** `goHome()` always goes to `homeFor(role)`, and each home is allowed for its own role (Dashboard is unguarded, AccountSetup is SETUP, AccountProblem is ACCOUNT_PROBLEM, Pending is PENDING_CITIZEN, OperatorPanel is OPERATOR). When the guard and the redirect both fire, it is the same `navigate` with `popUpTo(graph) inclusive` + `launchSingleTop`, so it is idempotent.

### Round-1 findings
- **S1, fixed.** `EmailField.onImeAction` is nullable and falls back to `KeyboardActions.Default`. Login no longer passes `{}`, so Next moves focus. Forgot password keeps Send.
- **S2, fixed.** After `enroll()` succeeds, `reload()` is best effort, then the account is re-emitted and `null` is returned. Whether `enrolledFactors` is filled without the reload is a device check (logged).
- **S3, fixed.** Every `catch (e: Exception)` in `FirebaseAuthRepository` is preceded by a rethrow of `CancellationException`, including the inner reload catch. `kotlin.coroutines.cancellation.CancellationException` is the same type that kotlinx throws.
- **N1, N3, N4, N5, N6, fixed.**
- **N2, fixed.** Login's Back is always enabled. A send or confirm that finishes after Back is dropped by `challenge !== current`. A `resolveSignIn` that succeeds after Back still signs the user in, and the redirect then runs, which is acceptable. Setup keeps Back disabled while busy, consistent with `changeNumber()`.
- **N7:** left for decision 0013 in the Docs phase, as logged.

### New nits
- **R2-N1.** `MfaScreen.kt:28`: the KDoc line added for `backEnabled` is about 150 characters. Rewrap it to the file's ~100-character width.
- **R2-N2.** `AccountSetupScreen.kt:45`: while the code step is busy, the enabled `BackHandler` calls `changeNumber()`, which returns immediately, so system Back does nothing for up to 60 s. Optional: `BackHandler(enabled = state.step == SetupStep.CODE && !state.isBusy, ...)`. Back then leaves the app during a send, so the current behaviour is also defensible. Pick one and say which at the defence.

### Still needs the device
Everything in "Needs the device or the console" above, plus:
- cold start as operator, setup user, pending citizen and account-problem user;
- process death on an operator placeholder ("Don't keep activities");
- `enrolledFactors` after enrolment without the reload (S2);
- keyboard Next on Login.

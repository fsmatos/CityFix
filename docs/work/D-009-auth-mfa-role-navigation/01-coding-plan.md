# D-009 coding plan

**Prerequisites:**
- `app/google-services.json` is in place (it already is locally, and it is ignored). Claude never reads, edits or stages it.
- Slices 5-6 also need console steps 2-5 from 00-plan: Identity Platform, SMS MFA, test numbers and the SHA fingerprints.
- Before each commit:
  - `git check-ignore -v app/google-services.json` prints the `.gitignore` line;
  - `git status --porcelain` does not list the file;
  - no test phone number or code appears in the diff.

## Files
| Path | Change | Why |
| --- | --- | --- |
| `app/src/main/java/pt/isec/amov/cityfix/data/AuthRepository.kt` | New: interface `AuthRepository`, `AuthAccount`, `SignInResult`, `AuthError`, `CodeSendResult`, `SecondFactorChallenge` | Task; ViewModels depend on the interface |
| `.../data/FirebaseAuthRepository.kt` | New: implementation over `FirebaseAuth` | Firebase stays in `data/` |
| `.../data/UsersRepository.kt` | New: interface `UsersRepository`, `UserProfile` | Task (read part) |
| `.../data/FirestoreUsersRepository.kt` | New: `users/{uid}` snapshot -> `Citizen`/`Operator` | Decision 0010 mapping |
| `.../CityFixApplication.kt` | `authRepository`, `usersRepository` `by lazy` | Decision 0009 container (Q11) |
| `.../viewmodel/Session.kt` | New: `Session`, `AccessRole`, `accessRole`, `canReport`, `canExpandCards` | Q3, Q7 |
| `.../viewmodel/SessionViewModel.kt`, `SessionViewModelFactory.kt` | New | Above the `NavHost` |
| `.../viewmodel/LoginUiState.kt`, `LoginViewModel.kt`, `LoginViewModelFactory.kt` | New (credentials step in slice 3, code step in slice 6) | Login screen |
| `.../viewmodel/ForgotPasswordUiState.kt`, `ForgotPasswordViewModel.kt`, `ForgotPasswordViewModelFactory.kt` | New | Password reset |
| `.../viewmodel/AccountSetupUiState.kt`, `AccountSetupViewModel.kt`, `AccountSetupViewModelFactory.kt` | New | Verify e-mail + enrolment |
| `.../ui/screens/LoginScreen.kt` | New: `LoginScreen(viewModel, onForgotPassword, ...)` + `LoginContent` | Task |
| `.../ui/screens/MfaScreen.kt` | New: stateless `MfaCodeContent` | Task; shared by login and setup |
| `.../ui/screens/ForgotPasswordScreen.kt` | New | Task |
| `.../ui/screens/AccountSetupScreen.kt` | New | Q1, Q8 |
| `.../ui/screens/PendingValidationScreen.kt` | New | Q2 |
| `.../ui/screens/PlaceholderScreen.kt` | New: one generic "Coming soon" screen; also used for ProfileMissing / ProfileUnavailable (title + message + actions) | Q12 |
| `.../ui/navigation/Destinations.kt` | Routes `LoginRoute`, `ForgotPasswordRoute(email)`, `AccountSetupRoute`, `PendingValidationRoute`, `OperatorPanelRoute`, `MyTicketsRoute`, `ProfileRoute`, `ApprovalsRoute`, `CategoriesRoute`, `AccountProblemRoute` | Role navigation |
| `.../ui/navigation/RoleAccess.kt` | New: `homeFor`, `menuItemsFor`, `RequireAccess` | Q4 |
| `.../ui/navigation/MainTopAppBar.kt` | `TopBarScreen` values; `showAddAction`, `accountAction`, `menuItems` + callbacks; previews | Brief V, D-005 requirement |
| `.../MainActivity.kt` | `SessionViewModel`, loading gate, redirect, guards, new destinations, `canExpandCards` from the session | Task |
| `app/src/main/res/values/strings.xml`, `values-pt/strings.xml` | New keys, EN and PT in the same commit | Conventions |

## Interfaces
Shapes, not final code. Names are the contract with Francisco once pushed.

```kotlin
// data/AuthRepository.kt
data class AuthAccount(
    val uid: String,
    val email: String,
    val isEmailVerified: Boolean,
    val hasSecondFactor: Boolean,   // user.multiFactor.enrolledFactors.isNotEmpty()
)

enum class AuthError { INVALID_CREDENTIALS, USER_DISABLED, NETWORK, TOO_MANY_REQUESTS,
    WRONG_CODE, CODE_EXPIRED, CODE_NOT_SENT, RECENT_LOGIN_REQUIRED, UNKNOWN }

/** Opaque to the ViewModels: wraps the MultiFactorResolver and the chosen phone hint. */
class SecondFactorChallenge internal constructor(
    internal val resolver: MultiFactorResolver,
    internal val hint: PhoneMultiFactorInfo,
) { val maskedPhone: String get() = hint.phoneNumber }

sealed interface SignInResult {
    data object Success : SignInResult
    data class SecondFactorRequired(val challenge: SecondFactorChallenge) : SignInResult
    data class Failure(val error: AuthError) : SignInResult
}

sealed interface CodeSendResult {
    data class Sent(val verificationId: String, val autoRetrievedCode: String? = null) : CodeSendResult
    data class Failure(val error: AuthError) : CodeSendResult
}

interface AuthRepository {
    val account: StateFlow<AuthAccount?>
    suspend fun signIn(email: String, password: String): SignInResult
    suspend fun sendSecondFactorCode(activity: Activity, challenge: SecondFactorChallenge): CodeSendResult
    suspend fun resolveSecondFactor(challenge: SecondFactorChallenge, verificationId: String, code: String): AuthError?
    suspend fun sendPasswordReset(email: String): AuthError?
    suspend fun sendEmailVerification(): AuthError?
    suspend fun reloadAccount(): AuthError?          // reload() + getIdToken(true), then re-emit account
    suspend fun sendEnrolmentCode(activity: Activity, phoneE164: String): CodeSendResult
    suspend fun enrolSecondFactor(verificationId: String, code: String): AuthError?   // then re-emit account
    fun signOut()
}
```
- `null` means success for the `AuthError?` returns. Write a KDoc line on each.
- `FirebaseAuthRepository(private val auth: FirebaseAuth)`:
  - `MutableStateFlow(auth.currentUser?.toAccount())`;
  - an `AuthStateListener` registered in `init`, which lives for the process (the repository is a process singleton in `CityFixApplication`, so there is no leak to worry about; say so in the KDoc).
- `verifyPhoneNumber` is wrapped in `suspendCancellableCoroutine` inside `withTimeout(60_000)`:
  - `onCodeSent` -> `Sent`;
  - `onVerificationCompleted(credential)` -> `Sent(verificationId = lastId, autoRetrievedCode = credential.smsCode)`, or ignored if `onCodeSent` came first;
  - `onVerificationFailed` -> `Failure(map(e))`;
  - timeout -> `Failure(CODE_NOT_SENT)`.
  `requireSmsValidation(true)`, `setTimeout(60L, SECONDS)`, `setActivity(activity)`.
- Exception mapping is in one private `fun Exception.toAuthError(): AuthError`, an exhaustive list of `is` checks:
  - `FirebaseAuthInvalidCredentialsException` with `errorCode` `ERROR_INVALID_VERIFICATION_CODE` -> `WRONG_CODE`;
  - `ERROR_SESSION_EXPIRED` -> `CODE_EXPIRED`;
  - other `FirebaseAuthInvalidCredentialsException` -> `INVALID_CREDENTIALS`;
  - `FirebaseAuthInvalidUserException` -> `USER_DISABLED` (or `INVALID_CREDENTIALS` for `ERROR_USER_NOT_FOUND`);
  - `FirebaseAuthRecentLoginRequiredException` -> `RECENT_LOGIN_REQUIRED`;
  - `FirebaseNetworkException` -> `NETWORK`;
  - `FirebaseTooManyRequestsException` -> `TOO_MANY_REQUESTS`;
  - else `UNKNOWN`.
  Check the error-code strings against the `FirebaseAuthException.errorCode` values seen in Logcat during slice 6, and record them in state.md.

```kotlin
// data/UsersRepository.kt
sealed interface UserProfile {
    data class Found(val user: User) : UserProfile
    data object Missing : UserProfile        // no document, or role not CITIZEN/OPERATOR
    data object Unavailable : UserProfile    // read failed (offline, nothing cached)
}
interface UsersRepository {
    fun observeUser(uid: String): Flow<UserProfile>
}
```
- `FirestoreUsersRepository(private val firestore: FirebaseFirestore)`:
  - `firestore.collection("users").document(uid).snapshots()` (Kotlin extension in `firebase-firestore`, BoM 34);
  - `.map { it.toProfile() }`;
  - `.catch { emit(UserProfile.Unavailable) }`.
- Mapping by hand from `getString` / `getGeoPoint` / `getLong` / `getBoolean`, not `toObject`, because `role` must pick the type:
  - `CITIZEN` -> `Citizen(id = uid, name, nif, email, phone, photo = photoPath, georeferencing = Coordinates(lat, lng), registrationStatus = RegistrationStatus.valueOf(...)` with a fallback to `PENDING` for an unknown value, `impactDetection)`;
  - `OPERATOR` -> `Operator(...)`;
  - else `Missing`.
  Check the `Coordinates` constructor in `model/Coordinates.kt` before writing.
- **Must use `app.firestore`**, never `Firebase.firestore` (overview, F-010 review N6).

```kotlin
// viewmodel/Session.kt
sealed interface Session { Loading; Anonymous; NeedsEmailVerification(email); NeedsSecondFactor(email);
    ProfileMissing; ProfileUnavailable; SignedIn(user: User) }        // as in 00-plan
enum class AccessRole { LOADING, ANONYMOUS, SETUP, ACCOUNT_PROBLEM, PENDING_CITIZEN, CITIZEN, OPERATOR }
val Session.accessRole: AccessRole        // exhaustive when
val Session.canReport: Boolean get() = accessRole == AccessRole.CITIZEN
val Session.canExpandCards: Boolean get() = accessRole == AccessRole.OPERATOR   // Q7
val Session.isSignedIn: Boolean           // everything except Loading/Anonymous
```
`AccessRole` is finer than 00-plan's sketch: LOADING and ACCOUNT_PROBLEM are split out, so the redirect has one target per value.

```kotlin
// viewmodel/SessionViewModel.kt
class SessionViewModel(private val auth: AuthRepository, private val users: UsersRepository) : ViewModel() {
    val session: StateFlow<Session> = auth.account
        .flatMapLatest { account -> when {
            account == null -> flowOf(Session.Anonymous)
            !account.isEmailVerified -> flowOf(Session.NeedsEmailVerification(account.email))
            !account.hasSecondFactor -> flowOf(Session.NeedsSecondFactor(account.email))
            else -> users.observeUser(account.uid).map { it.toSession() }
        } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Session.Loading)
    fun signOut() = auth.signOut()
}
```
- `flatMapLatest` needs `@OptIn(ExperimentalCoroutinesApi::class)`. Explain it in a comment: it cancels the old user listener when the account changes, so logout stops the `users/{uid}` listener.

```kotlin
// ui/navigation/RoleAccess.kt
fun homeFor(role: AccessRole): Any                      // route object; LOADING never navigates
data class TopBarMenuItem(@StringRes val label: Int, val route: Any)
fun menuItemsFor(role: AccessRole): List<TopBarMenuItem>
@Composable fun RequireAccess(role: AccessRole, allowed: Set<AccessRole>, onDenied: () -> Unit, content: @Composable () -> Unit)
```
- `RequireAccess`: if `role in allowed`, it draws `content`. Otherwise it runs `LaunchedEffect(role) { onDenied() }` and draws nothing.
- `MainTopAppBar` new parameters (stateless):
  - `showAddAction: Boolean` (replaces `screen != REPORT`; the caller passes `canReport && screen.showsAdd`);
  - `accountAction: AccountAction` (`enum { NONE, LOG_IN, LOG_OUT }`);
  - `menuItems: List<TopBarMenuItem>` (an overflow `IconButton(MoreVert)` + `DropdownMenu`, shown only when not empty, with `rememberSaveable` expanded);
  - `onAccountClick: () -> Unit`, `onMenuItemClick: (TopBarMenuItem) -> Unit`.
  It still imports nothing from `androidx.navigation`; routes are `Any` values passed through.
  - Log in / Log out: an `IconButton` with an icon from `material-icons-core`; check `Icons.AutoMirrored.Filled.ExitToApp` / `Icons.Filled.AccountCircle` exist in core. Its `contentDescription` is "Log in" / "Log out". Log out also appears as the last menu item when the menu is shown, so the bar does not overflow in portrait with three icons. Decide in slice 4, and log it.

Each screen ViewModel follows `ReportViewModel`: `MutableStateFlow<XxxUiState>`, `asStateFlow()`, events as functions, `viewModelScope.launch`, `@StringRes` error ids in the state, `isBusy` blocking double submits. Typed values (e-mail, phone, step) are mirrored into `SavedStateHandle` (via `createSavedStateHandle()` in the factory's `CreationExtras` overload) so they survive process death. Rotation is covered by the ViewModel itself.

## Steps

### Slice 1: repositories
- Write `AuthRepository.kt` and `FirebaseAuthRepository.kt` (everything above except the two MFA send/resolve/enrol bodies, which may return `Failure(UNKNOWN)` / `UNKNOWN` until slices 5-6, each with a `// D-009 slice 5/6` comment), `UsersRepository.kt` and `FirestoreUsersRepository.kt`.
- Add `authRepository` / `usersRepository` to `CityFixApplication` (KDoc: built from `auth` / `firestore`).
- Commit: `feat(D-009): add auth and users repositories`.
- Verify:
  - `assembleDebug` and `lint` pass; new warnings listed (expected none, or `UnusedResources`-style only);
  - `git diff --cached` shows no Firebase import outside `data/` and `CityFixApplication.kt`;
  - grep `Firebase.firestore` in `app/src` finds only `CityFixApplication.kt`.

### Slice 2: session above the NavHost
- `Session.kt`, `SessionViewModel` + factory (`SessionViewModelFactory(app.authRepository, app.usersRepository)`).
- In `CityFixApp`:
  - collect `session`;
  - while `Loading`, show a centred `CircularProgressIndicator` (with `semantics { contentDescription = loading }`) inside the `Scaffold` instead of the `NavHost`;
  - `val canExpandCards = session.canExpandCards` (replace the D-007 comment; keep `canExpandCards && isExpanded`);
  - `MainTopAppBar(showAddAction = session.canReport && screen != TopBarScreen.REPORT, ...)` with the new parameter added to the bar (other new parameters come in slices 3-4);
  - wrap `composable<ReportTicketRoute>` content in `RequireAccess(allowed = setOf(CITIZEN))`, with `onDenied = { navController.navigate(DashboardRoute) { popUpTo<DashboardRoute> { inclusive = true } } }`. Put `RequireAccess` in `RoleAccess.kt` now, with only `RequireAccess` in it.
- Commit: `feat(D-009): add session state and guard the report route`.
- Verify:
  - build and lint pass;
  - on the emulator (logged out): dashboard, list and detail work; no add button anywhere; the toggle stays hidden; rotation keeps the screen;
  - record in state.md that the add button is now hidden for everyone until a citizen logs in (expected).

### Slice 3: login and password reset
- Routes `LoginRoute`, `ForgotPasswordRoute(val email: String = "")`; `TopBarScreen.LOGIN`, `FORGOT_PASSWORD` with titles.
- `LoginViewModel` (credentials step only):
  - e-mail and password fields; `showPassword`;
  - `@StringRes` errors (blank e-mail, invalid format with `android.util.Patterns.EMAIL_ADDRESS`, blank password);
  - `signIn()` calls the repository and maps `AuthError` to strings;
  - `SecondFactorRequired` sets an error "Second factor not supported yet" until slice 6 (comment);
  - `Success` does nothing: the session changes and the redirect navigates.
- `LoginContent`:
  - e-mail `OutlinedTextField` (`KeyboardType.Email`, `ImeAction.Next`);
  - password with a visibility toggle (`cd_show_password` / `cd_hide_password`);
  - Log in button (`enabled = !isBusy`);
  - "Forgot password?" `TextButton`;
  - error text with `semantics { liveRegion = Polite }`;
  - a scrolling `Column` with `imePadding()` before `verticalScroll` (the overview's report-form lesson), `widthIn(max = 480.dp)` centred, the same in landscape.
- `ForgotPasswordViewModel` / `Content`:
  - e-mail prefilled from the route;
  - Send;
  - then always "If an account exists for this e-mail, a reset link was sent." (enumeration-safe);
  - `NETWORK` / `TOO_MANY_REQUESTS` errors shown.
- `MainTopAppBar`: `accountAction` + `onAccountClick`. Log in when ANONYMOUS and the screen is not LOGIN/FORGOT. Log out when `session.isSignedIn` (calls `sessionViewModel.signOut()`).
- Redirect in `CityFixApp`:
  ```kotlin
  var handledRole by rememberSaveable { mutableStateOf<AccessRole?>(null) }
  LaunchedEffect(role) {
      if (role != AccessRole.LOADING && role != handledRole) {
          if (handledRole != null) navController.navigate(homeFor(role)) { popUpTo(0) }
          handledRole = role
      }
  }
  ```
  The first value is only recorded, so a cold start keeps the restored back stack. Check that `popUpTo(0)` vs `popUpTo(navController.graph.id) { inclusive = true }` both compile on navigation 2.9.8, and use the graph-id form. In this slice, `homeFor` returns `DashboardRoute` for every role, and slice 4 adds the rest.
- Strings EN + PT for everything above.
- Commit: `feat(D-009): add login, password reset and log in/out actions`.
- Verify (console user without MFA, any `users/{uid}`):
  - wrong password shows the message;
  - right password lands on the dashboard, Log out is visible;
  - an active citizen doc shows the add button, the report form opens and submits (local repository);
  - Log out returns to the dashboard with no add button;
  - Forgot password: the e-mail arrives (**acceptance check**);
  - rotation keeps typed fields; PT strings checked once;
  - cold start while logged in stays logged in.
  This is a **temporary state**: accounts without MFA are let in until slice 5 adds the `NeedsSecondFactor` gate. Note it in state.md.

### Slice 4: role navigation and placeholders
- Routes `PendingValidationRoute`, `OperatorPanelRoute`, `MyTicketsRoute`, `ProfileRoute`, `ApprovalsRoute`, `CategoriesRoute`, `AccountProblemRoute`, each with a `TopBarScreen` value and a title.
- `homeFor`:
  - ANONYMOUS / CITIZEN -> `DashboardRoute`;
  - PENDING_CITIZEN -> `PendingValidationRoute`;
  - OPERATOR -> `OperatorPanelRoute`;
  - ACCOUNT_PROBLEM -> `AccountProblemRoute`;
  - SETUP -> `AccountSetupRoute` (added in slice 5; until then SETUP cannot occur, because the session has no gate yet).
- `menuItemsFor`:
  - CITIZEN: My tickets, Profile;
  - PENDING_CITIZEN: Profile;
  - OPERATOR: Approvals, Categories, Profile;
  - others: empty.
- Guards (`RequireAccess`):
  - My tickets: CITIZEN;
  - Profile: CITIZEN, PENDING_CITIZEN, OPERATOR;
  - Operator panel, Approvals, Categories: OPERATOR;
  - Pending: PENDING_CITIZEN;
  - Account problem: ACCOUNT_PROBLEM.
  `onDenied` navigates to `homeFor(role)` with the back stack cleared.
- `PendingValidationScreen` (stateless):
  - icon (decorative), heading, explanation, "Browse public issues" -> `DashboardRoute`;
  - a pending citizen on the dashboard sees no add button (`canReport` false).
- `AccountProblemRoute` content: `ProfileMissing` -> "Your account is not set up yet." `ProfileUnavailable` -> "Your profile could not be loaded. Check your connection." Both have Log out; Retry is only for Unavailable (`sessionViewModel.retry()`, which re-subscribes through a `MutableStateFlow` retry counter combined in the flow; keep it small).
- `PlaceholderScreen(titleRes)` for the five later screens.
- Top bar: the overflow menu.
- Commit: `feat(D-009): navigate by role with placeholder destinations`.
- Verify (console users, no MFA yet):
  - active citizen: dashboard + add + menu (My tickets, Profile);
  - pending citizen: the waiting screen, no add; set `ACTIVE` in the console, and the app moves to the dashboard with the add button without a restart (**acceptance check**);
  - operator: the Operations panel placeholder, the menu with three entries, no add button;
  - log out from an operator placeholder: the dashboard, and Back leaves the app (back stack cleared);
  - delete a test user's document (or use a user without one): Account problem;
  - airplane mode + cold start after login: same home (**acceptance check**, cache);
  - rotation on each new screen keeps it (the redirect does not fire).

### Slice 5: account setup (verify e-mail, enrol phone)
- Fill `sendEmailVerification`, `reloadAccount`, `sendEnrolmentCode` and `enrolSecondFactor` in `FirebaseAuthRepository`. After enrol and reload, update `account`.
- `SessionViewModel`: `NeedsEmailVerification` / `NeedsSecondFactor` map to `AccessRole.SETUP`, and `homeFor(SETUP) = AccountSetupRoute` (route + `TopBarScreen.ACCOUNT_SETUP`, guard SETUP only).
- `AccountSetupViewModel`:
  - step from the session: `VERIFY_EMAIL` or `PHONE` / `CODE`;
  - verify step: "Send verification e-mail" (sent once automatically on entry, and on Resend), "I have verified" -> `reloadAccount()`, "Not verified yet" if still false;
  - phone step: a +351 prefix shown, 9 digits validated (Portuguese mobile starting with 9, as D-010 will; keep the regex in the ViewModel for now, D-010 moves it to `utils/Validation.kt`) -> `sendEnrolmentCode(activity, "+351$digits")`;
  - code step: `MfaCodeContent` (6 digits, `KeyboardType.NumberPassword`, `autofill`/`ContentType.SmsOtpCode` if available in Compose `ui` 1.11, otherwise skip), Confirm -> `enrolSecondFactor`, Resend, Change number.
  - On success the session becomes SignedIn and the redirect goes to the role home.
  - `RECENT_LOGIN_REQUIRED` -> `signOut()` and the message on Login.
- `AccountSetupScreen` reads `LocalActivity.current` (`androidx.activity.compose.LocalActivity`) and passes it into `viewModel.sendCode(activity)`. It is never stored.
- `MfaScreen.kt`: `MfaCodeContent(maskedPhone, code, error, isBusy, onCodeChange, onConfirm, onResend, onBack)` + previews (EN, PT, landscape, error).
- Commit: `feat(D-009): verify e-mail and enrol the SMS second factor`.
- Verify (console steps 2-5 done; a fresh console user with a test number):
  - first login -> verify step -> the e-mail arrives -> open the link -> "I have verified" -> phone step -> test number -> code step -> a wrong code shows "Wrong code" -> the right code -> role home;
  - the enrolled factor shows in Authentication > Users (MFA column);
  - before enrolment, the add button and the menu are absent, and Log out works;
  - rotation on each step keeps the step and the typed values; rotation **during** "sending" may give "Could not send the code" (R3); record what happened;
  - record whether a reCAPTCHA page appeared (R2).

### Slice 6: MFA at sign-in
- `FirebaseAuthRepository.signIn` catches `FirebaseAuthMultiFactorException`, takes the first `PhoneMultiFactorInfo` hint, and returns `SecondFactorRequired(SecondFactorChallenge(resolver, hint))`. Fill `sendSecondFactorCode` (`setMultiFactorHint`, `setMultiFactorSession(resolver.session)`) and `resolveSecondFactor` (`resolver.resolveSignIn(assertion)`).
- `LoginViewModel`:
  - `step = SECOND_FACTOR` and `maskedPhone` in the state; `challenge` and `verificationId` in private fields;
  - the code is sent once when the step opens (from the screen with the activity, via `LaunchedEffect(step)` calling `viewModel.sendCode(activity)` once per step, guarded by a `codeRequested` flag in the state);
  - Confirm -> `resolveSecondFactor`; Resend; Back to the credentials step (clears the challenge);
  - after process death the challenge is null, so the step resets to credentials with the e-mail kept.
- `LoginScreen` shows `MfaCodeContent` when `step == SECOND_FACTOR`; system Back on that step goes to credentials (`BackHandler`).
- Commit: `feat(D-009): ask for the SMS code at login`.
- Verify (**acceptance checks**):
  - enrolled citizen and operator: log out, then log in -> code step -> a wrong code is rejected -> the right code -> different homes;
  - close and reopen: the session is kept;
  - logged out, the report and operator routes are unreachable (no buttons; after logging out on an operator screen, Back does not return to it);
  - record the `errorCode` strings seen for the wrong and expired codes.

After slice 6: the person's checks of the full acceptance list, then the rules-deploy question (00-plan Q10). No deploy happens without the explicit OK.

## Conventions
- MVVM as `ReportViewModel`: immutable `UiState` data classes, `StateFlow`, `@StringRes` errors, no text in ViewModels. Stateful `XxxScreen(viewModel)` and stateless, previewable `XxxContent(state, ...)` (`.claude/rules/kotlin.md`).
- Factories like `ReportViewModelFactory` / `EditContactViewModelFactory` (Contacts_#3), with the same unchecked-cast comment. Use the `CreationExtras` overload only where a `SavedStateHandle` is needed.
- Composables never import Firebase. Firebase types (`FirebaseUser`, `MultiFactorResolver`, `PhoneAuthCredential`) stay in `data/`. `Activity` is passed through a ViewModel call and never stored.
- Coroutines: `viewModelScope`, `await()`; no `GlobalScope`, no `Tasks.await` on the main thread.
- Strings: EN default, PT in `values-pt`; PT wording uses "autarquia"/"município" as the brief does; check the existing PT file for terms. Add `cd_` for icon buttons and `top_bar_title_` for titles. No hard-coded text, including in previews.
- Accessibility: `contentDescription` on every icon button; errors as text with `error()` semantics on fields; 48dp via M3 components; live regions for result messages.
- Layout: portrait and landscape both scroll, with `imePadding()` before `verticalScroll`; forms are capped at 480dp width and centred in landscape.
- Navigation: `launchSingleTop = true` on every forward `navigate`; role changes clear the back stack.
- Every new `TopBarScreen` value: add it to `toTopBarScreen()` in `MainActivity` by hand (its `else` will not warn, overview "App shell (D-006)").
- Commits: Conventional Commits with `D-009`, no attribution lines.

## Do not touch
- `app/google-services.json`, keys, `local.properties`.
- `firestore.rules`, `storage.rules`, `firebase.json`: no edits unless the person answers Q1's sub-question with yes (then edit only, as its own commit). **Never `firebase deploy`** or any Firebase CLI command without the person's explicit OK.
- The Firebase console: everything there is the person's (00-plan "Console steps").
- `model/` (no `role` field, decision 0010), `data/TicketsRepository.kt`, `LocalTicketsRepository.kt` (Francisco's; F-011 adds `creatorId`).
- Francisco's screens (`ShowScreen`, `ListScreen`, `DashboardScreen`, `ReportScreen`, `TicketCard`). `DetailAudience` stays PUBLIC at the call site.
- Gradle files and the version catalog (nothing new is needed; if a library turns out to be missing, stop and log it).
- `main`: no push or merge.

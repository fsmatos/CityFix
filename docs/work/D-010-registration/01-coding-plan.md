# D-010 coding plan

Read `00-plan.md` first; the Gate 1 answers (Q1-Q15) may change the steps below. Record every deviation in `state.md`.

## Files
Paths under `app/src/main/java/pt/isec/amov/cityfix/` unless stated.

| Path | Change | Why |
| --- | --- | --- |
| `utils/Validation.kt` | new | Pure validation functions (email, domain, phone, mobile, NIF, password, name). |
| `viewmodel/AccountSetupViewModel.kt` | edit | `PORTUGUESE_MOBILE` regex replaced by `isPortugueseMobile` (D-009 handover). |
| `viewmodel/AuthErrorMessages.kt` | edit | `emailErrorFor` uses `isValidEmail` (Q15); messages for `EMAIL_IN_USE`, `WEAK_PASSWORD`. |
| `data/AuthRepository.kt` | edit | `AuthError.EMAIL_IN_USE`, `AuthError.WEAK_PASSWORD`. |
| `data/AuthErrors.kt` | new | `internal fun Exception.toAuthError()` moved out of `FirebaseAuthRepository` (plus the two new cases). |
| `data/FirebaseAuthRepository.kt` | edit | Uses the shared `toAuthError()`; private copy removed. |
| `data/UsersRepository.kt` | edit | `Registration`, `RegistrationRole`, `RegisterResult`; `register`, `operatorEmailDomain`. |
| `data/FirestoreUsersRepository.kt` | edit | Constructor `(firestore, auth, photoUploader)`; `register` (Auth user, transaction, clean-up), `operatorEmailDomain`. |
| `data/ProfilePhotoUploader.kt` | new (slice 5) | Enqueues `ProfilePhotoWorker` (network constraint, backoff). |
| `data/ProfilePhotoWorker.kt` | new (slice 5) | Compress, upload `users/{uid}/photo.jpg`, set `photoPath`, delete local files. |
| `CityFixApplication.kt` | edit | `usersRepository = FirestoreUsersRepository(firestore, auth, profilePhotoUploader)`; `profilePhotoUploader by lazy`. |
| `viewmodel/RegisterUiState.kt` | new | Immutable form state, errors, computed `isValid`. |
| `viewmodel/RegisterViewModel.kt` | new | Events, real-time validation, `SavedStateHandle`, `submit()`. |
| `viewmodel/RegisterViewModelFactory.kt` | new | `CreationExtras` overload (SavedStateHandle), like `LoginViewModelFactory`. |
| `ui/screens/RegisterScreen.kt` | new | Stateful `RegisterScreen` (launchers, permission, busy report) + stateless `RegisterContent`; previews portrait / landscape / PT, citizen and operator. |
| `ui/screens/LoginScreen.kt` | edit | `onCreateAccount` button in `LoginContent` and `LoginScreen`; previews updated. |
| `ui/navigation/Destinations.kt` | edit | `@Serializable data object RegisterRoute`. |
| `ui/navigation/MainTopAppBar.kt` | edit | `TopBarScreen.REGISTER` + title (no add, no menu, like LOGIN). |
| `MainActivity.kt` | edit | `composable<RegisterRoute>`, `toTopBarScreen()`, `onAuthScreen` includes REGISTER, account action NONE on REGISTER, `holdRedirect` + redirect keyed on `(role, holdRedirect)`, Login's `onCreateAccount`. |
| `app/src/main/res/values/strings.xml`, `values-pt/strings.xml` | edit | `register_*` keys, `auth_error_email_in_use`, `auth_error_weak_password`, `top_bar_title_register`, `cd_register_*`. |
| `firestore.rules` | edit (slice 7, only if Q1 approved) | Operator create without `email_verified`; never deployed. |

## Interfaces

```kotlin
// utils/Validation.kt (no Android imports)
fun isValidEmail(email: String): Boolean
fun isInEmailDomain(email: String, domain: String): Boolean
fun isPortuguesePhone(digits: String): Boolean      // [29][0-9]{8}
fun isPortugueseMobile(digits: String): Boolean     // 9[0-9]{8}
fun isValidNif(nif: String): Boolean                // 9 digits, first != 0, mod-11 check digit
enum class PasswordProblem { TOO_SHORT, NO_LETTER, NO_DIGIT }
fun passwordProblem(password: String): PasswordProblem?   // null = strong enough
fun isValidName(name: String): Boolean
const val MIN_PASSWORD_LENGTH = 8
```

```kotlin
// data/UsersRepository.kt
enum class RegistrationRole { CITIZEN, OPERATOR }

data class Registration(
    val role: RegistrationRole,
    val name: String,
    val email: String,
    val phone: String,               // 9 digits
    val nif: String,                 // 9 digits
    val georeferencing: Coordinates?,// citizen only, required for CITIZEN
    val impactDetection: Boolean,    // citizen only
    val photoPath: String?,          // local file, optional
)

sealed interface RegisterResult {
    data object Success : RegisterResult
    data object NifTaken : RegisterResult
    data object DomainNotAllowed : RegisterResult
    data class Failure(val error: AuthError) : RegisterResult
}

interface UsersRepository {
    fun observeUser(uid: String): Flow<UserProfile>
    /** Auth user, then one transaction for nifs/{ROLE}_{nif} + users/{uid}; deletes the Auth user on failure. Online only. */
    suspend fun register(registration: Registration, password: String): RegisterResult
    /** municipality/main.emailDomain, lowercase without '@'; null when missing or unreadable. */
    suspend fun operatorEmailDomain(): String?
}
```

```kotlin
// viewmodel/RegisterUiState.kt (sketch)
data class RegisterUiState(
    val role: RegistrationRole = RegistrationRole.CITIZEN,
    val name: String = "", val email: String = "", val phone: String = "", val nif: String = "",
    val password: String = "", val confirmPassword: String = "", val showPassword: Boolean = false,
    val location: Coordinates? = null, val isLocating: Boolean = false,
    val locationPermissionDenied: Boolean = false,
    val impactDetection: Boolean = false,
    val photoPath: String? = null,
    val operatorDomain: String? = null, val isLoadingDomain: Boolean = true,
    @StringRes val nameError: Int? = null, /* emailError, phoneError, nifError, passwordError,
    confirmError, locationError */ @StringRes val error: Int? = null,
    val showErrorSummary: Boolean = false,
    val isRegistering: Boolean = false,
    val registered: Boolean = false,
) { val isValid: Boolean get() = /* every field rule for the role, no error */ }
```

```kotlin
// viewmodel/RegisterViewModel.kt events
fun onRoleSelected(role: RegistrationRole)
fun onNameChange(text: String); fun onEmailChange(text: String)
fun onPhoneChange(text: String)  // digits only, max 9
fun onNifChange(text: String)    // digits only, max 9
fun onPasswordChange(text: String); fun onConfirmPasswordChange(text: String); fun onToggleShowPassword()
fun onImpactDetectionChange(enabled: Boolean)
fun onLocationPermissionResult(granted: Boolean); fun refreshLocation()      // slice 4
fun onMapLocationPicked(location: Coordinates)                               // slice 6
fun onPhotoPicked(uri: Uri); fun onPhotoCaptured(path: String); fun onRemovePhoto(); fun onCameraUnavailable() // slice 5
fun submit()
```

```kotlin
// ui/screens/RegisterScreen.kt
@Composable fun RegisterScreen(viewModel: RegisterViewModel, onBusyChange: (Boolean) -> Unit, modifier: Modifier = Modifier)
@Composable fun RegisterContent(state: RegisterUiState, /* one lambda per event */, modifier: Modifier = Modifier)
```

## Steps

### Slice 1: validation helpers
- Write `utils/Validation.kt` with the functions above and short KDoc (rule and source: NIF mod-11, PT numbering plan prefixes 2/9). Pure Kotlin, no `import android.*`.
- `AccountSetupViewModel.sendCode`: `if (!isPortugueseMobile(state.phoneDigits))`; delete `PORTUGUESE_MOBILE` and its comment.
- `AuthErrorMessages.emailErrorFor`: `!isValidEmail(email)` instead of `Patterns`; drop the `android.util.Patterns` import.
- Commit: `feat(D-010): add input validation helpers`.
- Verify: `assembleDebug`, `lint` (same warning count as baseline, 22 at main e345744); hand check in a scratch reading of the code: `isValidNif("123456789") == true`, `"123456780"` false, `"012345678"` false; `isPortuguesePhone("239000000")` true, `"123456789"` false; Login still rejects `ana@` and accepts `ana@example.com` (device).

### Slice 2: registration data layer
- Move `toAuthError()` to `data/AuthErrors.kt` as `internal fun Exception.toAuthError()`; add `FirebaseAuthUserCollisionException -> EMAIL_IN_USE`, `FirebaseAuthWeakPasswordException -> WEAK_PASSWORD` (before the generic `FirebaseAuthInvalidCredentialsException` branch: `WeakPassword` extends it). `FirebaseAuthRepository` calls the shared one.
- `AuthError` + `messageRes()` cases and the two strings (EN/PT).
- `UsersRepository`: types and the two functions above. KDoc: online only; the Auth user is deleted when the profile cannot be written.
- `FirestoreUsersRepository(firestore, auth)` (the uploader parameter comes in slice 5):
  - `operatorEmailDomain()`: `firestore.collection("municipality").document("main").get().await()`, `getString("emailDomain")?.trim()?.lowercase()?.removePrefix("@")?.takeIf { it.isNotEmpty() }`; `catch (CancellationException) { throw }`, `catch (Exception) { null }`.
  - `register(registration, password)`:
    1. normalise: `email.trim().lowercase()`, `name.trim()`;
    2. OPERATOR: `val domain = operatorEmailDomain() ?: return Failure(UNKNOWN)`; `if (!isInEmailDomain(email, domain)) return DomainNotAllowed`; CITIZEN: `georeferencing` must be non-null, else `Failure(UNKNOWN)`;
    3. `val user = try { auth.createUserWithEmailAndPassword(email, password).await().user } catch (CancellationException) { throw } catch (e: Exception) { return Failure(e.toAuthError()) } ?: return Failure(UNKNOWN)`;
    4. `try { firestore.runTransaction { tx -> ... }.await() } catch (e: CancellationException) { withContext(NonCancellable) { deleteNewUser(user) }; throw e } catch (e: Exception) { withContext(NonCancellable) { deleteNewUser(user) }; return if (e.isNifTaken()) NifTaken else Failure(if (e is FirebaseFirestoreException && e.code == UNAVAILABLE) NETWORK else UNKNOWN) }`;
       - inside the transaction: `val nifRef = firestore.collection("nifs").document("${role.name}_${nif}")`; `if (tx.get(nifRef).exists()) throw FirebaseFirestoreException("NIF taken", ABORTED)`; `tx.set(nifRef, mapOf("uid" to uid))`; `tx.set(userRef, userDocument(...))`. Detect "NIF taken" with a private `NifTakenException` thrown from the lambda (the SDK wraps or rethrows it; check `e is NifTakenException || e.cause is NifTakenException`);
       - `userDocument`: `role`, `name`, `nif`, `email` (= `user.email`), `phone`, `createdAt` (`System.currentTimeMillis()`), `fcmTokens` (`emptyList<String>()`); CITIZEN adds `georeferencing` (`GeoPoint`), `registrationStatus` `"PENDING"` (`RegistrationStatus.PENDING.name`), `impactDetection`. No `photoPath`, no `validatedBy`/`validatedAt`. Role strings are the existing `ROLE_CITIZEN` / `ROLE_OPERATOR` constants;
    5. `deleteNewUser(user)`: `try { user.delete().await() } catch (Exception) { auth.signOut() }` (rethrow nothing; already inside NonCancellable);
    6. return `Success`.
- `CityFixApplication.usersRepository = FirestoreUsersRepository(firestore, auth)`.
- Commit: `feat(D-010): add registration to the users repository`.
- Verify: build + lint (no new warning other than an unused-symbol warning, if lint reports one, recorded); grep: no Firebase import outside `data/` and `CityFixApplication.kt`.

### Slice 3: register screen for both roles, redirect hold
- `Destinations.kt`: `RegisterRoute`. `MainTopAppBar.kt`: `TopBarScreen.REGISTER`, title `top_bar_title_register`, treated as LOGIN in the action logic (no add, no menu).
- `RegisterUiState`, `RegisterViewModel(users: UsersRepository, savedState: SavedStateHandle)`, `RegisterViewModelFactory(users)`:
  - `init`: restore saved fields (role by name, name, email, phone, nif, impact, lat/lng when both present, photo path); `launch { val d = users.operatorEmailDomain(); update { copy(operatorDomain = d, isLoadingDomain = false) } }`;
  - each `onXChange`: filter (digits for phone/NIF, `take(9)`), save to `savedState` (never the passwords), recompute that field's error only when the text is not empty (Q10), clear `error`, refresh `showErrorSummary && !isValid`;
  - e-mail error for OPERATOR: `emailErrorFor(email)`, then `register_error_email_domain` (formatted with the domain in the UI) when `!isInEmailDomain`, or `register_error_operator_unavailable` when the domain is `null` after loading;
  - `confirmError` when `confirmPassword != password`; `passwordError` from `passwordProblem` (one string per enum value);
  - `submit()`: return if `isRegistering`; build every error; if `!isValid` -> show errors + summary; else `update { isRegistering = true }` **before** `launch`, then `users.register(...)`: `Success` -> one `update { copy(registered = true, isRegistering = false) }`: `isRegistering = false` releases the redirect hold, and `registered` keeps Submit disabled until the redirect leaves the screen (no double submit); `NifTaken` -> `nifError = register_error_nif_taken`; `DomainNotAllowed` -> email error; `Failure(EMAIL_IN_USE)` -> `emailError`; other `Failure` -> `error = messageRes()`; all failures set `isRegistering = false`.
  - `isValid`: name, e-mail (and domain for OPERATOR, `operatorDomain != null`), phone, NIF, password, confirm, and for CITIZEN `location != null`.
- `RegisterScreen`:
  - `LaunchedEffect(state.isRegistering) { onBusyChange(state.isRegistering) }`;
  - `RegisterContent` inside `FormColumn` (imePadding before verticalScroll, max 480dp): `SingleChoiceSegmentedButtonRow` for the role (labels from strings, 48dp), `OutlinedTextField`s with `supportingText` errors, `KeyboardOptions` (`KeyboardType.Email`, `Phone`, `Number` for the NIF, `Password` with the Show/Hide text button as on Login), operator domain hint, citizen block (location row placeholder until slice 4; `Switch` for impact detection with a `Modifier.toggleable` row and a label), `FormMessage` for `error` and the summary, primary Button "Create account" (disabled while `isRegistering || registered`, with a progress indicator).
  - Previews: citizen empty, citizen with errors, operator with domain hint, landscape (`widthDp = 800, heightDp = 360`), PT locale.
- `LoginScreen.kt`: `onCreateAccount: () -> Unit` on `LoginScreen` and `LoginContent`; a `TextButton(register_create_account)` under "Forgot password?"; previews pass `{}`.
- `MainActivity.kt`:
  - `var holdRedirect by rememberSaveable { mutableStateOf(false) }` next to `handledRole`;
  - `onAuthScreen` adds `TopBarScreen.REGISTER`; `accountAction` is `NONE` when `screen == TopBarScreen.REGISTER` (first branch of the `when`);
  - Login: `onCreateAccount = { navController.navigate(RegisterRoute) { launchSingleTop = true } }`;
  - `composable<RegisterRoute> { val vm: RegisterViewModel = viewModel(factory = RegisterViewModelFactory(app.usersRepository)); RegisterScreen(vm, onBusyChange = { holdRedirect = it }) }` (no `RequireAccess`; comment why);
  - redirect: `LaunchedEffect(role, holdRedirect) { if (holdRedirect) return@LaunchedEffect; if (role != handledRole) { ... unchanged ... } }`, with a comment: sign-up signs in at once; the hold keeps the Register entry (and its ViewModel) alive until the NIF transaction ends; failure deletes the account, so the role is ANONYMOUS again and nothing navigates.
  - `toTopBarScreen()`: `hasRoute<RegisterRoute>() -> TopBarScreen.REGISTER`.
- Strings EN/PT: role labels, field labels, every error, domain hint (`%1$s`), summary, button, `top_bar_title_register`, `register_create_account`, `cd_*` for the switch and progress.
- Commit: `feat(D-010): add the registration screen for citizens and operators`.
- Verify: build + lint; EN/PT key sets equal. Device (Diogo, with `municipality/main` created): operator happy path -> Account setup opens, verification e-mail arrives, `users/{uid}` has role OPERATOR and no `registrationStatus`, `nifs/OPERATOR_<nif>` has the uid; operator outside the domain refused; e-mail already used -> field error; rotation while typing and during Submit; offline Submit -> "No connection", no Auth user.

### Slice 4: citizen georeferencing
- `RegisterViewModel(users, locationRepository, savedState)`; factory gets `app.locationRepository`.
- `onLocationPermissionResult(granted)` (granted -> `refreshLocation()`; denied -> `locationPermissionDenied = true`), `refreshLocation()` (`isLocating`, `locationRepository.currentLocation()`, null -> `locationError = register_location_unavailable`), coordinates saved as two `Double`s.
- `RegisterScreen`: `askLocation = rememberLauncherForActivityResult(RequestMultiplePermissions())` above the content; "Use my location" button: if `hasLocationPermission(context)` -> `refreshLocation()`, else launch `LOCATION_PERMISSIONS`; callback -> `onLocationPermissionResult(hasLocationPermission(context))`. Show the fix with 5 decimals, `LocationDeniedMessage` with Open settings when denied.
- Commit: `feat(D-010): georeference the citizen at registration`.
- Verify: build + lint. Device: `adb emu geo fix -8.4190 40.2100`, Use my location fills the field; deny -> message + Open settings; citizen registration end to end -> `users/{uid}` with `registrationStatus: PENDING`, `georeferencing`, `impactDetection`; after setup the waiting screen; second citizen with the same NIF -> "already registered", no Auth user left; same NIF as the operator of slice 3 -> succeeds.

### Slice 5: optional photo
- `data/ProfilePhotoUploader(context)`: `enqueue(uid, localPath)` -> `OneTimeWorkRequestBuilder<ProfilePhotoWorker>()` with `workDataOf(KEY_UID, KEY_PATH)`, `NetworkType.CONNECTED`, `BackoffPolicy.EXPONENTIAL` 30 s (as `AttachmentUploader.enqueue`).
- `data/ProfilePhotoWorker : CoroutineWorker`: reads the container (`applicationContext as CityFixApplication`); compresses with `ImageCompression.compressImage(path, target, thumb)` into `FileUtils.mediaFile(ctx, "<uuid>.jpg")` (thumbnail deleted at once); undecodable -> delete the source, `Result.failure()`; `storage.reference.child("users/$uid/photo.jpg").putFile(Uri.fromFile(target), storageMetadata { contentType = "image/jpeg" }).await()`, `downloadUrl.await()`, `firestore.collection("users").document(uid).update("photoPath", url.toString()).await()`; delete the files; exceptions -> `Result.retry()` up to 5 attempts (`runAttemptCount`), then delete the files and `Result.failure()`; rethrow `CancellationException`.
- `FirestoreUsersRepository(firestore, auth, photoUploader)`: after a committed transaction, `registration.photoPath?.let { photoUploader.enqueue(uid, it) }`. `CityFixApplication.profilePhotoUploader by lazy`.
- `RegisterViewModel(users, locationRepository, attachmentStorage, savedState)`: `onPhotoPicked(uri)` copies with `attachmentStorage.copyToInternalStorage(listOf(uri))` in `viewModelScope` (replacing and deleting a previous photo), `onPhotoCaptured(path)`, `onRemovePhoto()` (deletes the file), `onCameraUnavailable()` (notice); `onCleared()` deletes the photo file unless `registered`.
- `RegisterScreen`: `PickVisualMedia` (`ImageOnly`) and `TakePicture` launchers with `rememberSaveable` temp path (as `ReportScreen`), placed above the content; `AsyncImage` preview (120dp, `contentDescription = cd_register_photo`), buttons Gallery / Camera / Remove.
- Commit: `feat(D-010): add an optional profile photo uploaded after registration`.
- Verify: build + lint. Device: photo from gallery and from camera; after registration Storage has `users/<uid>/photo.jpg` and `photoPath` is an https URL; airplane mode after Submit -> the job waits, uploads when online; rotation keeps the chosen photo; leaving the form deletes the draft file (Device Explorer `files/images`).

### Slice 6 (optional, Q8): map pick
- In the citizen block, below the fix, a `GoogleMap` (240dp high, `rememberCameraPositionState` at the fix, zoom 16) with a `Marker` and `onMapClick = onMapLocationPicked`; `Modifier.semantics { contentDescription = cd_register_map }`. Shown only when a location exists. Landscape: same column (scrolls).
- `onMapLocationPicked(location)` stores and saves the point, clears `locationError`.
- Commit: `feat(D-010): let the citizen adjust the location on a map`.
- Verify: build + lint; device: tap moves the marker and the stored point; rotation keeps it. Grey map without the real key (known).

### Slice 7 (only if Q1 approved): rules draft
- `firestore.rules` `users` create, operator branch: remove `&& request.auth.token.email_verified == true`; add a comment: "The operator document is written at registration, before verification (D-010); `isOperator()` still requires a verified e-mail for every operator permission." No deploy, no `firebase` command.
- Commit: `docs(D-010): let the draft rules accept the operator profile before verification`.
- Verify: diff shows only that line and the comment; tell Francisco (shared file).

## Conventions
- MVVM as in `.claude/rules/kotlin.md`: ViewModel exposes `StateFlow<RegisterUiState>`; composables never touch Firebase, WorkManager or sensors; `RegisterContent` is stateless and previewable.
- Errors are `@StringRes Int?`, never text in the ViewModel. Enum names persisted as strings (`RegistrationStatus.PENDING.name`, `"CITIZEN"` / `"OPERATOR"` constants already in `FirestoreUsersRepository`).
- Every suspend wrapper with `catch (e: Exception)` first rethrows `CancellationException` (D-009 review S3); clean-up after Auth creation runs in `withContext(NonCancellable)`.
- Firebase only in `data/` and `CityFixApplication.kt`; always the container's `firestore`, `auth`, `storage`.
- Strings in both `values/strings.xml` and `values-pt/strings.xml`; content descriptions; 48dp targets; portrait and landscape.
- No passwords in `SavedStateHandle`, logs or state files. No test e-mails, NIFs of real people, phone numbers or UIDs in code, docs or commits (`123456789` is a synthetic valid NIF; use it or other synthetic ones).
- Commits: Conventional Commits with `D-010`, no attribution lines. Run `verify` (build, then lint) before each commit; check `app/google-services.json` is not staged.

## Do not touch
- `model/` (no field is missing).
- Francisco's files beyond what the table lists: `ImageCompression.kt`, `AttachmentUploader.kt`, `UploadWorker.kt`, `AttachmentStorage.kt`, `FileUtils.kt`, `LocationRepository.kt`, `LocationPermissions.kt`, `LocationDeniedMessage.kt`, `TicketMap.kt`, `ReportScreen.kt` are **reused, not edited**.
- `storage.rules`; `firestore.rules` except slice 7 with approval; `firebase.json`; `app/google-services.json`; Gradle files and the version catalog (every dependency needed is already there: WorkManager, Storage, Coil, maps-compose, play-services-location).
- No `firebase deploy`, no console changes (Diogo's console steps: create `municipality/main`; optional test inboxes).
- D-009's session logic (`Session.kt`, `SessionViewModel.kt`) and `AccountSetupViewModel` beyond the regex swap.

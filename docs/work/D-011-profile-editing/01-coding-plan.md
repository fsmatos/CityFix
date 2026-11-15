# D-011 coding plan

Read `00-plan.md` first. The Gate 1 answers (Q1-Q13) may change the slices marked "if Q...".

## Files
| Path | Change | Why |
| --- | --- | --- |
| `app/src/main/java/pt/isec/amov/cityfix/ui/components/AccountFields.kt` | new | `AccountTextField` (+ `readOnly`), `PasswordField`, `ImpactDetectionRow`, `ProfilePhotoPicker`, moved from Register (slice 1) |
| `app/src/main/java/pt/isec/amov/cityfix/ui/screens/RegisterScreen.kt` | edit | Use the moved components; private copies deleted (slice 1) |
| `app/src/main/java/pt/isec/amov/cityfix/data/UsersRepository.kt` | edit | `ProfileChanges`, `updateProfile`, `syncEmail`; KDoc "profile editing is D-011" updated (slice 2) |
| `app/src/main/java/pt/isec/amov/cityfix/data/FirestoreUsersRepository.kt` | edit | Implementations, no `await()` (slice 2) |
| `app/src/main/java/pt/isec/amov/cityfix/data/ProfilePhotoUploader.kt` | edit | `enqueueUniqueWork(..., REPLACE, ...)` (slice 2, if Q8) |
| `app/src/main/java/pt/isec/amov/cityfix/viewmodel/ProfileUiState.kt` | new | Slice 3 |
| `app/src/main/java/pt/isec/amov/cityfix/viewmodel/ProfileViewModel.kt` | new | Slice 3 (reload on open in slice 7) |
| `app/src/main/java/pt/isec/amov/cityfix/viewmodel/ProfileViewModelFactory.kt` | new | `CreationExtras` overload (slice 3) |
| `app/src/main/java/pt/isec/amov/cityfix/ui/screens/ProfileScreen.kt` | new | Stateful + stateless + previews (slice 3; buttons in 5 and 6) |
| `app/src/main/java/pt/isec/amov/cityfix/data/AuthRepository.kt` | edit | `reauthenticate`, `changePassword`, `requestEmailChange` (slice 4) |
| `app/src/main/java/pt/isec/amov/cityfix/data/FirebaseAuthRepository.kt` | edit | Implementations (slice 4) |
| `app/src/main/java/pt/isec/amov/cityfix/viewmodel/CredentialChangeUiState.kt` | new | `CredentialChange`, `CredentialStep`, state (slice 5; e-mail fields in 6) |
| `app/src/main/java/pt/isec/amov/cityfix/viewmodel/CredentialChangeViewModel.kt` | new | Slice 5, EMAIL path in 6 |
| `app/src/main/java/pt/isec/amov/cityfix/viewmodel/CredentialChangeViewModelFactory.kt` | new | Takes the `CredentialChange` (slice 5) |
| `app/src/main/java/pt/isec/amov/cityfix/ui/screens/ChangeCredentialScreen.kt` | new | FORM / CODE (`MfaCodeContent`) / DONE (slice 5, e-mail form in 6) |
| `app/src/main/java/pt/isec/amov/cityfix/ui/navigation/Destinations.kt` | edit | `ChangePasswordRoute` (5), `ChangeEmailRoute` (6); `ProfileRoute` KDoc (3) |
| `app/src/main/java/pt/isec/amov/cityfix/ui/navigation/MainTopAppBar.kt` | edit | `TopBarScreen.CHANGE_PASSWORD` (5), `CHANGE_EMAIL` (6) + titles |
| `app/src/main/java/pt/isec/amov/cityfix/MainActivity.kt` | edit | Profile destination (3), two new destinations and `toTopBarScreen()` rows (5, 6) |
| `app/src/main/java/pt/isec/amov/cityfix/viewmodel/SessionViewModel.kt` | edit | E-mail sync `onEach` (slice 7) |
| `app/src/main/res/values/strings.xml`, `app/src/main/res/values-pt/strings.xml` | edit | `profile_*`, `credential_*`, `top_bar_title_change_*`, `cd_profile_*` (slices 3, 5, 6) |
| `firestore.rules` | edit | Owner update allow-list + e-mail rule (slice 8, only if Q4) |

## Interfaces

```kotlin
// data/UsersRepository.kt
/** What the Profile screen may change (brief I.2.C, I.3.C). null = unchanged. */
data class ProfileChanges(
    /** 9 digits, without +351. */
    val phone: String? = null,
    /** Citizens only; ignored for an Operator. */
    val impactDetection: Boolean? = null,
    /** A new local photo in filesDir/images; uploaded by ProfilePhotoWorker. */
    val newPhotoPath: String? = null,
)

interface UsersRepository {
    // ... existing observeUser, register, operatorEmailDomain
    /**
     * Writes only phone and (citizen) impactDetection to users/{user.id}, then queues the photo.
     * Never name, nif, georeferencing, role, registrationStatus or email. Not awaited: the write
     * is queued in the local cache (works offline) and the listener shows it at once.
     * Returns false when there is nothing to write.
     */
    fun updateProfile(user: User, changes: ProfileChanges): Boolean

    /** Copies the Auth e-mail into users/{uid}.email (after a verified change). Not awaited. */
    fun syncEmail(uid: String, email: String)
}

// data/AuthRepository.kt
interface AuthRepository {
    // ... existing
    /**
     * Re-authenticates the signed-in user with [password]. An enrolled account answers
     * SecondFactorRequired; finish it with sendSecondFactorCode + resolveSecondFactor.
     */
    suspend fun reauthenticate(password: String): SignInResult

    /** After a recent re-authentication. Null on success. */
    suspend fun changePassword(newPassword: String): AuthError?

    /** Sends the verification link to [newEmail] (verifyBeforeUpdateEmail). Null on success. */
    suspend fun requestEmailChange(newEmail: String): AuthError?
}

// ui/components/AccountFields.kt (internal)
@Composable internal fun AccountTextField(
    value: String, onValueChange: (String) -> Unit, @StringRes labelRes: Int, error: String?,
    enabled: Boolean, keyboardType: KeyboardType, prefix: String? = null,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    autoCorrect: Boolean = true, readOnly: Boolean = false, supportingHint: String? = null,
)
@Composable internal fun PasswordField(/* unchanged signature from RegisterScreen */)
@Composable internal fun ImpactDetectionRow(checked: Boolean, enabled: Boolean, onCheckedChange: (Boolean) -> Unit)
/** [photo] is the Coil model: a File (local pick) or the https photoPath; null = no photo. */
@Composable internal fun ProfilePhotoPicker(
    photo: Any?, isCopying: Boolean, enabled: Boolean, showRemove: Boolean,
    onPickPhoto: () -> Unit, onTakePhoto: () -> Unit, onRemovePhoto: () -> Unit,
)

// viewmodel/ProfileUiState.kt
data class ProfileUiState(
    /** The stored profile (read-only fields, remote photo); null until the first snapshot. */
    val profile: User? = null,
    /** The Auth account's e-mail (the current one). */
    val email: String = "",
    val phone: String = "",
    val impactDetection: Boolean = false,
    /** A photo picked in this visit, not saved yet. */
    val newPhotoPath: String? = null,
    val isCopyingPhoto: Boolean = false,
    @StringRes val photoNotice: Int? = null,
    @StringRes val phoneError: Int? = null,
    /** "Profile saved." after Save; cleared by the next edit. */
    @StringRes val notice: Int? = null,
    /** True after Save with a photo, until photoPath changes. */
    val photoUploadPending: Boolean = false,
    /** The profile could not be read (Unavailable/Missing); the screen shows a message. */
    val unavailable: Boolean = false,
) {
    val isCitizen: Boolean get() = profile is Citizen
    val isValid: Boolean get() = isPortuguesePhone(phone)
    val hasChanges: Boolean get() = profile != null &&
        (phone != profile.phone || newPhotoPath != null ||
            (profile is Citizen && impactDetection != profile.impactDetection))
}

// viewmodel/CredentialChangeUiState.kt
enum class CredentialChange { EMAIL, PASSWORD }
enum class CredentialStep { FORM, CODE, DONE }
data class CredentialChangeUiState(
    val change: CredentialChange,
    val step: CredentialStep = CredentialStep.FORM,
    val currentPassword: String = "",
    val newEmail: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val showPassword: Boolean = false,
    val isOperator: Boolean = false,
    val operatorDomain: String? = null,
    val isLoadingDomain: Boolean = false,
    val maskedPhone: String = "",
    val code: String = "",
    val codeRequested: Boolean = false,
    val isBusy: Boolean = false,
    @StringRes val currentPasswordError: Int? = null,
    @StringRes val newEmailError: Int? = null,
    @StringRes val newPasswordError: Int? = null,
    @StringRes val confirmError: Int? = null,
    @StringRes val codeError: Int? = null,
    @StringRes val error: Int? = null,
    @StringRes val notice: Int? = null,
)

// ui/navigation/Destinations.kt
@Serializable data object ChangePasswordRoute
@Serializable data object ChangeEmailRoute
```

`ProfileViewModel(uid: String, users: UsersRepository, auth: AuthRepository, attachmentStorage: AttachmentStorage, savedState: SavedStateHandle)`; `ProfileViewModelFactory(uid, users, auth, attachmentStorage)`.
`CredentialChangeViewModel(change: CredentialChange, isOperator: Boolean, auth: AuthRepository, users: UsersRepository, savedState: SavedStateHandle)`; `CredentialChangeViewModelFactory(change, isOperator, auth, users)`.

## Steps
Verify every Kotlin slice with `./gradlew assembleDebug`, then `./gradlew lint` (0 errors; compare the warnings with the baseline of 22 and list any new one in `state.md`). Check that the EN and PT key sets are equal. Rules-only slices skip build and lint, as the verify skill says.

### Slice 1: shared account form fields (refactor)
- Create `ui/components/AccountFields.kt` with the four composables, moved verbatim from `RegisterScreen.kt`:
  - `RegisterTextField` becomes `AccountTextField`, plus `readOnly` (passed to `OutlinedTextField(readOnly = ...)`) and an optional `supportingHint` (shown when there is no error; used by the profile phone hint);
  - `ProfilePhoto(state, ...)` becomes `ProfilePhotoPicker(photo, isCopying, enabled, showRemove, ...)`, without the `FormMessage` line. `RegisterContent` draws `FormMessage(state.photoNotice, isError = true)` right after it, so the visible order is the same.
- `RegisterScreen.kt`: delete the private copies and call the moved ones (`photo = state.photoPath?.let { File(it) }`, `showRemove = state.photoPath != null`).
- `CitizenLocation` stays in `RegisterScreen.kt` (the profile shows the location read-only, through `AccountTextField`).
- Verify: build + lint. Open the Register previews (all must render as before). On the emulator: Register citizen and operator in portrait and landscape, errors while typing, the photo pick and the impact row toggle (TalkBack "switch").
- Commit: `refactor(D-011): share the registration form fields`.

### Slice 2: profile writes in the users repository
- `UsersRepository`: `ProfileChanges`, `updateProfile`, `syncEmail` (Interfaces). Update the interface KDoc.
- `FirestoreUsersRepository.updateProfile(user, changes)`:
  - `val fields = buildMap { changes.phone?.let { put("phone", it) }; if (user is Citizen) changes.impactDetection?.let { put("impactDetection", it) } }`;
  - if `fields` is not empty: `firestore.collection(USERS).document(user.id).update(fields).addOnFailureListener { Log.w(TAG, "profile update refused", it) }`, **no `await()`** (comment: offline queue, as F-011);
  - `changes.newPhotoPath?.let { photoUploader.enqueue(user.id, it) }`;
  - return `fields.isNotEmpty() || changes.newPhotoPath != null`.
- `syncEmail(uid, email)`: `update("email", email)` with the same failure log.
- `ProfilePhotoUploader.enqueue` (if Q8): `WorkManager.getInstance(appContext).enqueueUniqueWork("profile_photo_$uid", ExistingWorkPolicy.REPLACE, request)`. Add a KDoc line on the orphan source after a REPLACE (R5).
- No caller yet. Verify: build + lint.
- Commit: `feat(D-011): write the editable profile fields`.

### Slice 3: Profile screen
- `ProfileUiState` (Interfaces). `ProfileViewModel`:
  - `init`:
    - collects `users.observeUser(uid)` in `viewModelScope`. `Found` sets `profile`, and on the first `Found` only (a private `initialised` flag) sets `phone` / `impactDetection` unless `savedState` has them. When `photoUploadPending` is true and `profile.photo` changed, it clears the flag. `Missing` / `Unavailable` set `unavailable = true`;
    - `email = auth.account.value?.email.orEmpty()`, kept up to date by collecting `auth.account`.
  - `onPhoneChange` (digits, max 9; `phoneError = if (digits.isEmpty()) null else phone rule`; saves `KEY_PHONE`), `onImpactDetectionChange` (saves `KEY_IMPACT`), `onPhotoPicked(uri)` / `onPhotoCaptured(path)` / `onRemoveNewPhoto()` / `onCameraUnavailable()`. These are copied from `RegisterViewModel` (copy off the main thread, delete the previous pick, `KEY_PHOTO`). Every edit clears `notice`.
  - `save()`: return if `isCopyingPhoto`, no profile, or `!hasChanges`. If `!isValid`, set the phone error and return. Otherwise build `ProfileChanges` with only the changed values (`impactDetection` only when `isCitizen`) and call `users.updateProfile(profile, changes)`. Then set `notice = R.string.profile_saved`, `photoUploadPending = newPhotoPath != null`, `newPhotoPath = null`, and remove `KEY_PHOTO` / `KEY_PHONE` / `KEY_IMPACT` from `savedState` (the stored values are the baseline now).
  - `onCleared()`: delete `newPhotoPath` if it is set (not saved).
- `ProfileViewModelFactory`: the `CreationExtras` overload, as `RegisterViewModelFactory`.
- `ProfileScreen(viewModel, onLogOut, modifier)` (stateful): the two photo launchers and `tempCameraPath` above the content, copied from `RegisterScreen`. `ProfileContent(state, ...)` (stateless) in `FormColumn`:
  - the photo: `ProfilePhotoPicker(photo = newPhotoPath?.let(::File) ?: profile.photo, showRemove = newPhotoPath != null, ...)`, then `FormMessage(photoNotice)` and, when `photoUploadPending`, `FormMessage(R.string.profile_photo_uploading, isError = false)`;
  - read-only `AccountTextField`s: name, NIF, role (`register_role_citizen` / `register_role_operator`), citizen status (`profile_status_pending` / `profile_status_active`), citizen georeferencing (`report_location_value`), e-mail;
  - editable phone (`+351` prefix, hint `profile_phone_hint`), the citizen `ImpactDetectionRow`, `FormMessage(notice)`, **Save** (enabled when `hasChanges && !isCopyingPhoto`), and **Log out** (`OutlinedButton`);
  - while `profile == null`: a progress indicator (`cd_profile_loading`), or `profile_unavailable` when `unavailable`.
  - Previews: active citizen, pending citizen, operator, landscape (`widthDp = 800, heightDp = 360`), PT, phone error.
- `MainActivity`: inside the existing `RequireAccess` of `composable<ProfileRoute>`, `val user = (session as? Session.SignedIn)?.user ?: return@RequireAccess`, then the ViewModel from the factory (`app.usersRepository`, `app.authRepository`, `app.attachmentStorage`) and `ProfileScreen(viewModel, onLogOut = logOut)`.
- `Destinations.kt`: `ProfileRoute` KDoc ("Profile of the signed-in user (D-011)").
- Strings EN/PT: `profile_name_label`, `profile_nif_label`, `profile_role_label`, `profile_status_label`, `profile_status_pending`, `profile_status_active`, `profile_location_label`, `profile_email_label`, `profile_phone_hint`, `profile_save`, `profile_saved` ("Profile saved. Changes made offline sync when you are online."), `profile_photo_uploading`, `profile_unavailable`, `profile_log_out`, `cd_profile_loading`. Reuse `register_*` labels where the text is the same.
- Verify: build + lint, previews. On the emulator (citizen, pending citizen, operator):
  - the read-only fields take no input and TalkBack says "read only";
  - a phone error shows while typing, and Save then does nothing;
  - Save online and offline (airplane mode), and the value in the console;
  - a photo from the gallery and from the camera, and the new photo after the upload (R4);
  - Impact detection off, Save, then `impactDetection: false` in the console;
  - rotation keeps the typed values;
  - Log out.
- Commit: `feat(D-011): add the profile screen`.

### Slice 4: re-authentication and credential changes in the auth repository
- `FirebaseAuthRepository.reauthenticate(password)`: `val user = auth.currentUser ?: return Failure(UNKNOWN)`, then `user.reauthenticate(EmailAuthProvider.getCredential(user.email.orEmpty(), password)).await()` gives `Success`. Catch `FirebaseAuthMultiFactorException` with the same hint code as `signIn`; extract a private `challengeOf(e)` used by both, so `signIn` is unchanged in behaviour. Then `CancellationException` (rethrow) and `Exception` (`toAuthError()`).
- `changePassword(new)`: `currentUser?.updatePassword(new)?.await()`. `requestEmailChange(new)`: `currentUser?.verifyBeforeUpdateEmail(new.trim().lowercase())?.await()`. Both use the usual catches.
- KDoc on the class: `resolveSecondFactor` also completes a re-authentication (the resolver comes from `reauthenticate`).
- No caller yet. Verify: build + lint.
- Commit: `feat(D-011): add re-authentication and credential changes to the auth repository`.

### Slice 5: Change password screen
- `CredentialChangeUiState` (Interfaces) and `CredentialChangeViewModel`, with the PASSWORD path:
  - field events with errors while typing (`passwordProblem` messages `register_error_password_*`, `register_error_confirm`; a blank current password gives `credential_error_current_blank` on Submit);
  - `submit()`: gate on validity, then `isBusy`, then `auth.reauthenticate(currentPassword)`:
    - `SecondFactorRequired`: store `challenge`, `step = CODE`, `maskedPhone`;
    - `Success`: `performChange()`;
    - `Failure(INVALID_CREDENTIALS)`: `currentPasswordError = credential_error_current_wrong`;
    - other failures: `error = messageRes()`.
  - `sendCode(activity)`, `onCodeChange`, `confirmCode()`, `backToForm()`: modelled on `LoginViewModel` (the `challenge !== current` guard). On success, `performChange()`.
  - `performChange()`: `auth.changePassword(newPassword)`. `null` gives `step = DONE`. `WEAK_PASSWORD` returns to FORM with `newPasswordError = auth_error_weak_password`. `RECENT_LOGIN_REQUIRED` and others return to FORM with `error`. The passwords are not saved.
- `CredentialChangeViewModelFactory(change, isOperator, auth, users)`: the `CreationExtras` overload.
- `ChangeCredentialScreen(viewModel, onDone)`:
  - stateful: `LocalActivity.current`, plus `LaunchedEffect(step)` that sends the code once (`codeRequested`) and `BackHandler` on CODE, as `LoginScreen`;
  - `ChangeCredentialContent(state, ...)` (stateless): FORM uses `PasswordField` ×3, CODE uses `MfaCodeContent(backLabel = credential_back)`, DONE shows `credential_password_changed` and a "Back to profile" button (`onDone`).
  - Previews: FORM with errors, CODE, DONE, landscape, PT.
- Routes and bar: `ChangePasswordRoute`, `TopBarScreen.CHANGE_PASSWORD` with the title `top_bar_title_change_password`, and the `toTopBarScreen()` row.
- `MainActivity`: `composable<ChangePasswordRoute>` inside `RequireAccess(CITIZEN, PENDING_CITIZEN, OPERATOR)`; `isOperator = user is Operator`; `onDone = { navController.navigateUp() }`. Profile gets `onChangePassword` (`navigate(ChangePasswordRoute) { launchSingleTop = true }`) and a **Change password** button.
- Strings EN/PT: `profile_change_password`, `credential_current_password_label`, `credential_new_password_label`, `credential_error_current_blank`, `credential_error_current_wrong`, `credential_code_intro`, `credential_back`, `credential_password_changed`, `credential_back_to_profile`, `top_bar_title_change_password`.
- Verify: build + lint. On the emulator:
  - a wrong current password shows its error and sends no SMS;
  - the right one gives the code (test number), then DONE;
  - log out, then log in with the new password; the old one fails;
  - rotation on FORM and CODE;
  - Back on CODE returns to FORM;
  - airplane mode gives "No connection".
  - Record R1: does the session stay `SignedIn` through `resolveSignIn`?
- Commit: `feat(D-011): change the password from the profile`.

### Slice 6: Change e-mail screen
- `CredentialChangeViewModel`, EMAIL path:
  - `KEY_NEW_EMAIL` in `savedState`;
  - for an operator, `loadDomain()` on init and before Submit while it is missing (the D-010 pattern; `isLoadingDomain`; `register_operator_domain_loading` / `register_error_operator_unavailable`);
  - the new e-mail error, in this order: `emailErrorFor(trimmed)`, then `credential_error_email_same` (equal to `auth.account.value?.email`, ignoring case), then for an operator `register_error_email_domain` (formatted with the domain);
  - `performChange()`: `auth.requestEmailChange(newEmail)`. `null` gives DONE with `credential_email_link_sent` (formatted with the new e-mail). `EMAIL_IN_USE` gives `newEmailError = auth_error_email_in_use` on FORM.
- Content: the FORM step for EMAIL shows the current e-mail read-only, the new e-mail field, the operator domain hint and the current password.
- `ChangeEmailRoute`, `TopBarScreen.CHANGE_EMAIL` with the title `top_bar_title_change_email`, the `toTopBarScreen()` row, and the `MainActivity` destination (same guard). A **Change e-mail** button on Profile.
- Strings EN/PT: `profile_change_email`, `credential_new_email_label`, `credential_error_email_same`, `credential_email_link_sent` ("We sent a link to %1$s. Your e-mail changes when you open it; you may have to log in again with the new address."), `top_bar_title_change_email`.
- Verify: build + lint. On the emulator:
  - invalid, same and (operator) outside-domain addresses are refused before Firebase;
  - a citizen with a new Gmail `+alias` gets the link (record R3 for an address already used);
  - open the link and record R2 (signed out at once, later, or never).
- Commit: `feat(D-011): change the e-mail with a verification link`.

### Slice 7: e-mail sync and reload
- `SessionViewModel`, `else` branch: `users.observeUser(account.uid).onEach { profile -> if (profile is UserProfile.Found && account.email.isNotBlank() && !profile.user.email.equals(account.email, ignoreCase = true)) users.syncEmail(account.uid, account.email) }.map { it.toSession() }`. Add a KDoc comment: the next snapshot matches, so it writes once per change.
- `ProfileViewModel.reloadAccount()`: calls `auth.reloadAccount()` and ignores the result. `ProfileScreen` calls it from `LaunchedEffect(Unit)`, so once per visit and not again after a rotation. Comment: a revoked token here signs the user out, which is the decision 0015 involuntary path.
- Verify: build + lint. On the emulator, after the slice 6 link: reopen Profile or log in again; the Auth e-mail and `users/{uid}.email` (console) match the new address. A second reload writes nothing more (no extra writes in the console's usage, or a log line).
- Commit: `feat(D-011): keep the profile e-mail in step with the account`.

### Slice 8 (only if Q4 is approved): draft rules
- `firestore.rules`, the `users/{uid}` owner update rule:
  ```
  allow update: if isSignedIn()
    && request.auth.uid == uid
    && changedKeys().hasOnly(['phone', 'photoPath', 'impactDetection', 'email', 'fcmTokens'])
    && (!changedKeys().hasAny(['impactDetection']) || resource.data.role == 'CITIZEN')
    && (!changedKeys().hasAny(['email'])
        || (request.resource.data.email == request.auth.token.email
            && request.auth.token.email_verified == true
            && (resource.data.role != 'OPERATOR'
                || request.auth.token.email.lower().split('@')[1] == municipality().emailDomain)));
  ```
  Add a comment: D-011, allow-list (brief I.2.C, I.3.C), `fcmTokens` for F-016, not deployed. Add `photoUpdatedAt` to the list only if R4 forced it.
- Rules only: build and lint skipped. Do not run any `firebase` command. Francisco must be told (shared file).
- Commit: `docs(D-011): let the draft rules accept only the editable profile fields`.

## Conventions
- MVVM as in `kotlin-conventions`:
  - private `MutableStateFlow` + `asStateFlow()`, `update { it.copy(...) }`, `@StringRes` errors, no text in the ViewModels;
  - stateful `XxxScreen` / stateless `XxxContent`, with previews of the stateless one (portrait, landscape, PT).
- Every `catch (e: Exception)` is preceded by a rethrow of `CancellationException` (decision 0015, review S3).
- No Firebase type leaves `data/`; the `Activity` is passed through the call and never stored.
- Passwords never go to `SavedStateHandle`, logs or `state.md`. Test e-mails, numbers and UIDs never go into git.
- Strings in both `values/strings.xml` and `values-pt/strings.xml` (PT uses "ocorrência(s)" where relevant; "Perfil", "Terminar sessão" as in the existing menu strings). Add the new keys at the end of each file (R9).
- 48dp targets; content descriptions on the photo and on every progress indicator; `semantics { error(...) }` on the fields with an error (already in `AccountTextField`).
- Plain code: no new abstractions beyond the four shared field components.
- Commits: Conventional Commits with `D-011`, no attribution lines. `core.hooksPath` is not set in this clone, so check by hand.

## Do not touch
- `google-services.json`, keys, `storage.rules` (the owner overwrite of `photo.jpg` is already allowed), and any `firebase deploy`.
- `firestore.rules`, except slice 8 after Q4 approval.
- Francisco's files and records: `TicketsViewModel.kt`, `FirestoreTicketsRepository.kt`, `UploadWorker.kt`, `AttachmentUploader.kt`, `ImageCompression.kt`, decisions 0010 / 0012 / 0013 / 0014 / 0015-media.
- `ProfilePhotoWorker.kt` (unless R4 needs `photoUpdatedAt`; then one line, recorded as a deviation).
- `model/` (no change needed).
- The registration flow (`RegisterViewModel`, `register()`), apart from slice 1's call-site changes.
- `main` (no merge or push).

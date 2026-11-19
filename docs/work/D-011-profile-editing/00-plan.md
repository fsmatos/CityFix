# D-011: Profile editing
Type: feature · Task file: docs/tasks/phase-2/D-011-profile-editing.md
Labels: -

## Problem
- Brief I.2.C: the citizen may change their profile, except name, NIF and georeferencing. Brief I.3.C: the operator may change their profile, except name and NIF. Brief I.2.A/F: the citizen's impact-detection choice lives in the profile, and F-015 (Francisco, not started) reads it.
- Today `ProfileRoute` is a `PlaceholderScreen`, guarded for CITIZEN, PENDING_CITIZEN and OPERATOR and reached from the top bar's overflow menu (`menuItemsFor`, D-009). Log out exists only in that menu.
- `UsersRepository` can only read (`observeUser`) and register (D-010). `AuthRepository` has no re-authentication, password change or e-mail change. D-009 noted that a password change needs re-authentication (`RECENT_LOGIN_REQUIRED`).
- **MFA makes re-authentication a two-step flow.** Every account has an SMS factor (mandatory, decision 0015). For such a user, `reauthenticateWithCredential(EmailAuthProvider...)` throws `FirebaseAuthMultiFactorException`, just as sign-in does. So a re-authentication is password + SMS code, through the same resolver (`resolver.resolveSignIn`).
- **E-mail change is out of band.** `verifyBeforeUpdateEmail(new)` only sends a link. The Auth e-mail changes when the user opens it, possibly on another device and much later. `users/{uid}.email` was written at registration from the token, and the draft update rule (`firestore.rules:197-200`) forbids the owner from changing `email` at all.
- The registration form pieces (`RegisterTextField`, `PasswordField`, `ImpactDetectionRow`, `ProfilePhoto`, the location display) are `private` in `RegisterScreen.kt`, and `ProfilePhoto` / `CitizenLocation` take `RegisterUiState`. So they cannot be reused as they are.
- The draft `users` update rule already denies `name`, `nif`, `georeferencing`, `role`, `email`, `createdAt` and the validation fields to the owner. But the rules are **not deployed** (decision 0012 Q5), and the database is in test mode until 2026-12-10. So a direct write to a locked field succeeds today.

## Outcome
- **Profile screen** (`ProfileRoute`, from the menu entry "Profile" for an active citizen, a pending citizen and an operator):
  - **Read-only:** name, NIF, role and, for citizens, registration status and georeferencing (5 decimals, only the user's own point), plus the current e-mail.
    - The read-only fields use the same text-field component as Register with `readOnly = true`. TalkBack reads them as "read only", and they cannot be focused for typing.
  - **Editable:** phone (`+351` prefix, the same rule as registration), photo (gallery or camera, preview), and Impact detection (citizens only, the same switch row as Register).
  - One **Save** button. It is enabled when something changed and the form is valid, and it writes only the changed fields. It also works offline: Firestore queues the write, and the photo waits for the network in WorkManager.
  - Buttons **Change e-mail**, **Change password** and **Log out**.
- **Change password** screen: current password, new password and confirmation (the D-010 password rule). Then the SMS code to the enrolled phone (re-authentication). Then "Password changed".
- **Change e-mail** screen: the new e-mail (the D-010 e-mail rule; operators must stay in `municipality/main.emailDomain`) and the current password. Then the SMS code. Then a message: "We sent a link to <new>. Your e-mail changes when you open it. You may have to log in again with the new address."
- **E-mail kept in step:** whenever the signed-in Auth e-mail differs from `users/{uid}.email`, the session writes the new value to Firestore (one field). The Profile screen reloads the account when it opens, so a link opened elsewhere shows up.
- **Impact detection** changes `users/{uid}.impactDetection`. The `users/{uid}` listener already feeds `Session.SignedIn.user` (`Citizen.impactDetection`), from the local cache too, so F-015's detector, keyed on that value, stops as soon as Save is pressed, even offline.
- Draft `firestore.rules` (only if Q4 is approved; never deployed): the owner's update becomes an allow-list. `email` may change only to the token's e-mail (an operator's must also stay in the domain), and `impactDetection` only for citizens.

## Acceptance checks
- [ ] Name, NIF and role (and georeferencing and registration status for citizens) are shown read-only: no keyboard opens on them, and TalkBack says "read only". No code path writes them: `updateProfile` builds its map only from `phone`, `impactDetection` (citizen) and, through the worker, `photoPath`.
- [ ] Direct write: in the draft rules, an owner update that changes `name`, `nif` or `georeferencing` is denied, and so is an e-mail that differs from the token's (Rules Playground or emulator, if Diogo runs it; see Q4/Q5). **Not enforced on the live database while the rules are not deployed**; this is stated in the docs and at the defence.
- [ ] Phone: an invalid number shows the D-010 error while typing, and Save does nothing. A valid change appears in `users/{uid}.phone`. Offline, Save succeeds and the value syncs when the network returns.
- [ ] Photo: a new gallery or camera photo replaces `users/{uid}/photo.jpg`, and `photoPath` is updated once online. The screen then shows the new photo (check that Coil does not keep showing the old one; risk R4).
- [ ] Impact detection: switching it off and pressing Save sets `impactDetection: false`. `Session.SignedIn.user` shows the new value at once, also offline (log or debugger). Once F-015 exists: the detector stops (the sensor listener is unregistered). Until then this box is checked through the session value only, and the state log says so.
- [ ] Change password: a wrong current password shows an error on that field. With the right one the SMS code is asked for, and after it the new password works at the next login and the old one does not.
- [ ] Change e-mail: an invalid e-mail, the same e-mail, or (operator) an address outside the domain is refused before Firebase is called. After password + SMS, the link arrives at the new address. After opening it and logging in again (or reopening Profile), the Auth e-mail and `users/{uid}.email` both hold the new address.
- [ ] Log out on Profile signs out, and the app goes to the anonymous dashboard (the D-009 `logOut` helper).
- [ ] An anonymous user can never reach Profile or the two change screens: no menu entry, and `RequireAccess` sends a deep link or back-stack leftover home.
- [ ] Portrait and landscape on all three screens: nothing clipped, and rotation keeps the typed values. The code step survives rotation; after process death the form step returns with the typed e-mail kept and the passwords cleared.
- [ ] EN and PT strings for every new text; content descriptions on the photo and progress indicators; 48dp targets.
- [ ] `./gradlew assembleDebug` and `./gradlew lint` pass after each slice, and every new lint warning is listed in `state.md` (baseline: 0 errors, 22 warnings).

## Approach

### Reusing the registration components (slice 1, refactor)
- Move `RegisterTextField` (renamed `AccountTextField`), `PasswordField`, `ImpactDetectionRow` and the photo block from `RegisterScreen.kt` to a new `ui/components/AccountFields.kt`, as `internal`.
  - `AccountTextField` gains `readOnly: Boolean = false` (passed to `OutlinedTextField.readOnly`; keyboard `ImeAction.Next` stays).
  - The photo block becomes `ProfilePhotoPicker(photo: Any?, isCopying, enabled, showRemove, onPick, onTake, onRemove)`, where `photo` is what `AsyncImage` loads (a `File` or a URL). The caller draws the notice with `FormMessage`, so `ui/components` does not import from `ui/screens`.
- `RegisterScreen` calls the moved components. **Behaviour that must not change:** the Register screen looks and works exactly as before (the same previews, the same semantics and the same 48dp rows).
- Why not reuse `RegisterContent` itself: it is built around `RegisterUiState` and the role selector, and the profile needs different read-only and editable sets. Shared field components keep each screen small.

### Data (`data/UsersRepository.kt`, `data/FirestoreUsersRepository.kt`, `data/ProfilePhotoUploader.kt`)
- `data class ProfileChanges(val phone: String? = null, val impactDetection: Boolean? = null, val newPhotoPath: String? = null)`. `null` means "unchanged".
- `fun updateProfile(user: User, changes: ProfileChanges): Boolean`:
  - builds a map with only `phone` (digits) and, **only when `user is Citizen`**, `impactDetection`;
  - `users/{uid}.update(map)` **without `await()`**, as F-011 does: the write is queued in the local cache and returns at once, also offline. A server refusal is only logged (`addOnFailureListener`);
  - then `newPhotoPath?.let { photoUploader.enqueue(user.id, it) }`;
  - returns `false` when there is nothing to write.
  - It never writes `name`, `nif`, `georeferencing`, `role`, `registrationStatus` or `email`.
- `fun syncEmail(uid: String, email: String)`: `update("email", email)`, without `await()`. It is called only by the session (below).
- `ProfilePhotoUploader.enqueue` uses `enqueueUniqueWork("profile_photo_<uid>", ExistingWorkPolicy.REPLACE, ...)` (Q8). Two quick photo changes then cannot finish in the wrong order and leave the older photo. Registration enqueues once, so it is not affected.

### Auth (`data/AuthRepository.kt`, `data/FirebaseAuthRepository.kt`)
- `suspend fun reauthenticate(password: String): SignInResult`. It calls `currentUser.reauthenticate(EmailAuthProvider.getCredential(currentUser.email, password))`. `FirebaseAuthMultiFactorException` gives `SecondFactorRequired(SecondFactorChallenge(resolver, hint))`, exactly as `signIn` does, so the existing `sendSecondFactorCode` and `resolveSecondFactor` finish it. Errors go through `toAuthError()`.
- `suspend fun changePassword(newPassword: String): AuthError?`: `updatePassword`. Firebase keeps this device signed in and signs out the others.
- `suspend fun requestEmailChange(newEmail: String): AuthError?`: `verifyBeforeUpdateEmail(newEmail)`.
- Existing `reloadAccount()`: the Profile screen calls it once when it opens (best effort), so an e-mail changed through the link is picked up.

### Profile screen (`viewmodel/Profile{UiState,ViewModel,ViewModelFactory}.kt`, `ui/screens/ProfileScreen.kt`)
- Route-scoped, factory with the `CreationExtras` overload (`SavedStateHandle`), like Register.
- `ProfileViewModel(uid, users, auth, attachmentStorage, savedState)`:
  - collects `users.observeUser(uid)` itself (one more listener on a document the session already listens to; the cache serves both). Every snapshot refreshes the read-only part and the remote photo;
  - the editable values (phone, impact) are taken from the **first** snapshot, unless they were restored from `SavedStateHandle`. Later snapshots do not overwrite what the user is typing;
  - the e-mail shown is `auth.account.value.email`;
  - events: `onPhoneChange` (digits, max 9, error while typing as in D-010), `onImpactDetectionChange`, `onPhotoPicked(uri)` / `onPhotoCaptured(path)` / `onRemoveNewPhoto` (only the photo picked in this visit, not the stored one; Q7), `save()`.
  - `save()` gates on `isValid && hasChanges`. It calls `users.updateProfile(profile, changes)`, then clears the picked photo (now owned by the worker) and shows "Profile saved." (offline: the same message plus "It will sync when you are online"; see Failure modes). The baseline then follows the next snapshot, which arrives from the cache at once.
  - `onCleared` deletes a picked photo that was not saved (as Register does).
- `ProfileScreen` (stateful: launchers, `LaunchedEffect(Unit) { viewModel.reloadAccount() }`) and `ProfileContent` (stateless) inside `FormColumn`: one centred column of at most 480dp in portrait and landscape (Q11), with previews for citizen, pending citizen and operator, landscape and PT.
- Log out takes CityFixApp's `logOut` lambda (D-009) as `onLogOut`.

### Change password and Change e-mail (one ViewModel, two routes)
- New routes `ChangeEmailRoute` and `ChangePasswordRoute` (`data object`s; no route arguments, so no enum-argument question), guarded like `ProfileRoute`. Two new `TopBarScreen` values with titles, mapped in `toTopBarScreen()`.
- `CredentialChangeViewModel(change: CredentialChange, auth, users, savedState)`, where `enum class CredentialChange { EMAIL, PASSWORD }` is chosen by the factory per route. Steps `FORM -> CODE -> DONE`, modelled on `LoginViewModel`:
  - FORM, EMAIL: new e-mail (`isValidEmail`; not equal to the current one, ignoring case; operator: `isInEmailDomain(new, users.operatorEmailDomain())`, read on init with the D-010 "loading / not available" handling) and the current password.
  - FORM, PASSWORD: current password, new password (`passwordProblem`) and confirmation.
  - Submit: `auth.reauthenticate(current)`. `INVALID_CREDENTIALS` sets the current-password field error ("The current password is wrong"). `SecondFactorRequired` goes to CODE. A plain `Success` (an account without a factor, which the session normally prevents) goes straight to the change.
  - CODE: `MfaCodeContent` (D-009). The code is sent once from a `LaunchedEffect` with `LocalActivity.current`; Resend and Back return to FORM. The challenge and the verification id are private fields, never saved (decision 0015).
  - After `resolveSecondFactor` succeeds, the change runs at once (`changePassword` or `requestEmailChange`). `EMAIL_IN_USE` and `WEAK_PASSWORD` go back to FORM with the field error; other errors show the D-009 messages.
  - DONE: a message and a "Back to profile" button (`navigateUp`).
  - Saved state: only the new e-mail. The passwords are never saved (as in Login and Register).

### E-mail sync (`viewmodel/SessionViewModel.kt`)
- In the `else` branch of `flatMapLatest`, add `onEach` on `observeUser`. When the profile is `Found`, `account.email` is not blank and differs from `user.email` (ignoring case), it calls `users.syncEmail(account.uid, account.email)`. The write is not awaited; the next snapshot then has the same e-mail, so the check stops by itself. The session owns both values, so the sync runs whether or not Profile is ever opened (D-012 shows the operator the citizen's contact e-mail).
- The link may revoke this device's refresh token. If it does, the next token refresh or the Profile `reloadAccount()` signs the user out (an involuntary sign-out, already handled by decision 0015). The user logs in with the new e-mail, and the sync runs then. Either path ends with Auth and Firestore equal (device check, R2).

### Phone and the SMS factor (Q1)
- The profile phone is **contact data** (brief I.2.A / I.3.A, `users/{uid}.phone`). The SMS second factor was enrolled separately in Account setup (D-009). D-010 did not prefill it, so the two numbers may already differ.
- Recommended: changing the profile phone does **not** touch the factor. A hint under the field says "Contact number. The number that receives your login codes does not change here." Changing the MFA phone (enrol new, unenrol old, both needing re-authentication) is out of scope. A lost phone stays a console reset (decision 0015 consequence).

### Anonymous users
- Three layers, all existing: no menu entry for ANONYMOUS (`menuItemsFor`), `RequireAccess(CITIZEN, PENDING_CITIZEN, OPERATOR)` on all three routes, and the ViewModels are created only inside the guard with the signed-in uid (`(session as? Session.SignedIn)?.user`). A profile is only ever read from `users/{uid}` for the signed-in uid.

### Alternatives considered
- **An immediate switch for impact detection** (written on toggle, like a setting): this would add a second write path next to Save. Rejected for one Save (Q6).
- **Dialogs on the Profile screen for e-mail and password**: the `MultiFactorResolver` step and rotation make a route-scoped ViewModel simpler, so they are routes.
- **Syncing the e-mail from `ProfileViewModel`**: it would never run if the user does not reopen Profile.
- **Storing a `pendingEmail` in Firestore**: it would be a second source of truth, and Auth has no "pending e-mail" to compare it with.
- **Re-enrolling MFA when the phone changes**: two extra SMS flows plus unenrolment, a lockout risk, and not asked for by the brief.

## Architecture impact
- New: `ui/components/AccountFields.kt`, `ui/screens/ProfileScreen.kt`, `ui/screens/ChangeCredentialScreen.kt`, `viewmodel/Profile{UiState,ViewModel,ViewModelFactory}.kt`, `viewmodel/CredentialChange{UiState,ViewModel,ViewModelFactory}.kt`.
- Changed:
  - `data/UsersRepository.kt` and `FirestoreUsersRepository.kt` (`ProfileChanges`, `updateProfile`, `syncEmail`), `data/ProfilePhotoUploader.kt` (unique work);
  - `data/AuthRepository.kt` and `FirebaseAuthRepository.kt` (`reauthenticate`, `changePassword`, `requestEmailChange`);
  - `viewmodel/SessionViewModel.kt` (the e-mail sync);
  - `ui/screens/RegisterScreen.kt` (calls the moved components);
  - `ui/navigation/Destinations.kt` (two routes, `ProfileRoute` KDoc), `ui/navigation/MainTopAppBar.kt` (two `TopBarScreen` values and titles);
  - `MainActivity.kt` (three destinations, `toTopBarScreen()`);
  - strings EN/PT;
  - `firestore.rules` (draft, only if Q4 is approved).
- No new dependency. No `model/` change: `Citizen.impactDetection`, `User.phone` and `User.photo` already exist.
- Decision record: a new `0019-profile-editing.md`, and amendment lines for 0015 (the session's e-mail sync) and 0016 (the shared field components). See the docs plan.
- F-015 contract: the detector runs only while `session.accessRole == CITIZEN && ((session as? Session.SignedIn)?.user as? Citizen)?.impactDetection == true`, keyed on that value (handover).

## Failure modes
| Situation | Behaviour |
| --- | --- |
| Save offline | `update()` is queued and the session snapshot shows the new values from the cache at once (impact detection takes effect). "Profile saved." The photo waits for the network. If the app is killed before sync, the cached write survives (Firestore persistence) and syncs later |
| Rules refuse the update (only if the drafts are ever deployed) | The local value shows, then the server rejects it and the listener reverts it. The failure is only logged. Accepted while the rules are not deployed |
| Snapshot arrives while the user is editing (the worker set `photoPath`, an operator validated the citizen) | Read-only fields and the remote photo refresh; typed values are kept |
| Photo upload fails 5 times / undecodable / user logs out first | As in D-010: dropped without a message, and the old photo stays |
| Two photo changes in quick succession | `REPLACE` cancels the first worker. Its source file in `filesDir/images` may remain (small orphan; see R5) |
| Coil shows the old photo after the overwrite | R4, device check |
| Re-authentication offline | `NETWORK` "No connection"; the form keeps its values |
| Wrong current password | Error on the current-password field; no SMS sent |
| SMS not received / rotation during send | 60 s timeout, then "Could not send the code" and Resend (D-009 R3) |
| `TOO_MANY_REQUESTS` (too many SMS or passwords) | The D-009 message |
| Process death on the code step | The challenge is lost, so the form step returns with the new e-mail kept and the passwords empty |
| New e-mail already used by another account | Firebase may refuse at once (`EMAIL_IN_USE` on the field). With e-mail enumeration protection it may only fail when the link is opened; the user then stays on the old e-mail (R3) |
| Link never opened | Nothing changes; Auth and Firestore keep the old e-mail |
| Link opened, this device's token revoked | Signed out at the next refresh or the Profile reload; log in with the new e-mail; then the sync writes Firestore |
| "Revert e-mail change" link opened from the old inbox | Auth goes back to the old e-mail. Firebase may also remove the second factor (R6); the session then shows Account setup and the user enrols again. The sync makes Firestore follow |
| Password changed | This device stays signed in; other devices are signed out at their next token refresh (involuntary sign-out, decision 0015) |
| Operator's new e-mail outside the domain | Refused in the form. Auth itself cannot enforce the domain (no Cloud Functions); the draft rules refuse the Firestore sync (Q4), and `isOperator()` cannot check the domain (R7) |
| Anonymous user, deep link or back-stack leftover | `RequireAccess` navigates home; nothing is drawn |

## Risks and open questions
Risks:
- **R1 Re-authentication with MFA not yet seen on a device.** That `reauthenticate` throws `FirebaseAuthMultiFactorException` and that `resolver.resolveSignIn` completes a re-authentication come from the Firebase MFA docs (Android). Check on the emulator with the test numbers. `resolveSignIn` may fire the `AuthStateListener` with the same user; `StateFlow` drops an equal `AuthAccount`, so the session should not restart (check).
- **R2 Behaviour after the e-mail link.** It is not confirmed whether the link revokes this device's session at once, at the next token refresh, or not at all. Each case ends with the sync (see Approach), but what the user sees differs. Record it in the state log.
- **R3 E-mail enumeration protection** may hide `EMAIL_IN_USE` from `verifyBeforeUpdateEmail`. Then the done message is shown although the change can never complete.
- **R4 Coil cache.** If overwriting `users/{uid}/photo.jpg` keeps the same download token, `photoPath` does not change, so the listener does not fire and Coil shows the cached old image. Fallback, if the device check shows it: the worker also writes `photoUpdatedAt` (the profile uses it as the Coil cache key). That is a one-line change in `ProfilePhotoWorker` plus the field in the rules allow-list.
- **R5 Orphan file on REPLACE.** A cancelled `ProfilePhotoWorker` rethrows `CancellationException` and does not delete its source. Rare (two saves within the upload time), so it is accepted.
- **R6 Revert link and MFA.** Firebase documents that reverting an e-mail change can unenrol second factors (a security measure). The app already handles a missing factor (Account setup), but it is worth knowing for the defence.
- **R7 Operator domain is checked by the client only** for the Auth e-mail. A changed operator e-mail outside the domain would still pass `isOperator()` (it checks only `email_verified`). This would matter only with deployed rules; the draft sync rule refuses the Firestore copy.
- **R8 Acceptance "direct write" vs test mode.** Until rules are published, any client can write any field. The check can be shown only in the draft (Playground/emulator), never on the live database.
- **R9 Merge conflicts.** F-014 (Francisco) also edits `MainActivity.kt` and both `strings.xml`. Keep the D-011 edits inside their own `composable<>` blocks and add the strings at the end of each file.
- **R10 Decision numbering** (D-010 R8): 0015 is used twice. This task takes 0019.

Open questions (for Gate 1; recommended answer in brackets):
1. Is the profile phone the MFA factor? Should changing it re-enrol? [No. They are separate (the factor was enrolled in Account setup). Show a hint and leave MFA phone changes out of scope.]
2. Re-authentication asks for the password **and** an SMS code every time (Firebase requires it for MFA accounts). Accept? [Yes.]
3. Keep `users/{uid}.email` in step from `SessionViewModel` (it writes the Auth e-mail when they differ), plus a best-effort `reloadAccount()` when Profile opens? [Yes, both.]
4. Edit the draft `firestore.rules` (commit only, not deployed; Francisco to be told)? The owner's `users` update becomes `hasOnly(['phone', 'photoPath', 'impactDetection', 'email', 'fcmTokens'])`. `email` may change only to `request.auth.token.email` (an operator's also in `municipality().emailDomain`), and `impactDetection` only when `role == 'CITIZEN'`. Today's rule is a deny-list that forbids any `email` change, so the sync would be refused. [Yes, as slice 8. Keep `fcmTokens` for F-016.]
5. Deploy the rules, or run them in the Rules Playground, to show the "direct write" acceptance check? [Do not deploy (decision 0012 Q5). Diogo may run the Playground; otherwise state the check as "draft only" at the defence.]
6. Impact detection saved with the one Save button, or written at once when toggled? [One Save for phone, photo and impact.]
7. Offer removing the stored photo? [No. Only replace it, and discard a photo picked in this visit. Removal would also need a Storage delete.]
8. `ProfilePhotoUploader` uses unique work with `REPLACE` per uid? [Yes.]
9. Pending citizens may edit their profile and see the impact switch? [Yes. The brief does not forbid it, and F-015 runs only for active citizens.]
10. E-mail and password changes on their own routes (one shared ViewModel), not dialogs? [Yes.]
11. Landscape layout: the single centred column (`FormColumn`, max 480dp) as on Register, or a two-pane split (P07)? [Single column. Register passed its landscape check with it.]
12. A Log out button on Profile as well as the menu entry? [Yes, as the task asks; it uses the same `logOut` helper.]
13. Show the georeferencing as coordinates only (no map)? [Yes. D-010 skipped the map pick, and the value is the user's own.]

## Out of scope
- Changing the MFA phone or adding a second factor; MFA recovery.
- Deleting the account.
- Removing the stored photo (Q7).
- Deploying any rules; Cloud Functions.
- F-015's detector itself (Francisco). D-011 only provides the flag and documents the contract.
- Editing locked fields by operators for other users (D-012 validates only).
- A map display of the georeferencing.

## Slices
1. **refactor:** move the registration form fields to `ui/components/AccountFields.kt` (`AccountTextField` with `readOnly`, `PasswordField`, `ImpactDetectionRow`, `ProfilePhotoPicker`); `RegisterScreen` uses them; no behaviour change.
2. **feat:** `UsersRepository.updateProfile` / `syncEmail` (`ProfileChanges`), and unique photo work in `ProfilePhotoUploader`.
3. **feat:** Profile screen: read-only identity, editable phone, photo and impact detection, Save, Log out. Replaces the placeholder; EN/PT.
4. **feat:** `AuthRepository.reauthenticate`, `changePassword`, `requestEmailChange` (data only; no caller yet).
5. **feat:** Change password screen (route, `CredentialChangeViewModel` PASSWORD path, code step), linked from Profile.
6. **feat:** Change e-mail screen (EMAIL path, operator domain check, done message), linked from Profile.
7. **feat:** keep `users/{uid}.email` in step with the Auth e-mail (`SessionViewModel`), and the Profile reload on open.
8. **docs (conditional on Q4):** draft `firestore.rules` owner update allow-list and e-mail rule. Not deployed.

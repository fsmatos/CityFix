# D-010 review

Scope: `git diff e7cfdf4..HEAD` (cdbcb8d..fab62c6) on `dev/Diogo`, checked against `00-plan.md`, `01-coding-plan.md`, the Gate 1 decisions (Q1-Q15 as recommended, slice 6 skipped, slice 7 a local draft-rules edit), the deviations in `state.md`, the task file, `docs/architecture/overview.md`, decisions 0010 / 0015, CLAUDE.md and `kotlin-conventions`.

Build and lint (reviewer run, 2026-11-14): `./gradlew assembleDebug lint` succeeded. Lint: 0 errors, 22 warnings, the same as the baseline. None of the warnings is in a D-010 file.

## Verdict
**Changes requested.** One blocking finding (B1): pressing Back or Up while Submit runs can leave an orphan Auth user, or reserve a NIF for good. The rest of the work follows the plan. Validation is correct (NIF mod-11 checked by hand: `123456789` is valid). The transaction and the delete-on-failure order match the plan. `SavedStateHandle` keeps every field except the passwords. The worker deletes its local files in every final case. The rules diff removes only the operator `email_verified` term and adds a comment. EN/PT key sets are equal (the only differences are `translatable="false"` keys that were already there). No personal data reaches anonymous users: `users/{uid}` is readable by the owner and operators, `nifs` needs a signed-in user, and the photo sits under `users/{uid}/`. No secrets are in the diff.

## Blocking

### B1. Back or Up during Submit cancels `register()` at points where the cleanup cannot work
- `RegisterViewModel.kt:251-252` runs `users.register(...)` in `viewModelScope`. Back, or the top bar's Up arrow (`MainActivity.kt:246`, still active on Register), pops the entry, clears the ViewModel and cancels the call. Nothing stops Back while `isRegistering` is true.
- Cancelled while `createUserWithEmailAndPassword(...).await()` waits (`FirestoreUsersRepository.kt:74-76`): the `CancellationException` is rethrown and no delete runs. The Firebase task still finishes, so the account is created and signed in without a profile (orphan, R3). The released hold then sends the user to Account setup, and after MFA to Account problem.
- Cancelled while `runTransaction(...).await()` waits (`FirestoreUsersRepository.kt:87-94`): cancelling the coroutine does not cancel the Firestore task, so the commit can still land. Line 93 then deletes the Auth user, which leaves `nifs/{ROLE}_{nif}` pointing to a deleted uid and a `users/{uid}` with that person's data. The draft rules make `nifs` create-only (`delete: if false`), so that NIF can never register again for that role. Only a console cleanup fixes it.
- The slice 3 deviation in `state.md` says the account "is deleted in NonCancellable" when the user leaves during Submit. That is true only for the transaction phase, and that is the phase where deleting does harm.
- **Fix:**
  1. `RegisterScreen.kt`: add `BackHandler(enabled = state.isRegistering) {}` next to the `DisposableEffect`. In `MainActivity.kt:246`, pass `canNavigateBack = canNavigateBack && !showGate && !holdRedirect`.
  2. `RegisterViewModel.kt:252`: `val result = withContext(NonCancellable) { users.register(registration, state.password) }`. The sign-up and the transaction then always finish and clean up in order, even if the ViewModel is cleared some other way. Updating `_uiState` after `onCleared` does no harm.
  3. Keep the `onDispose` release as a safety net and correct the deviation text in `state.md`.
  4. Device check: tap Submit, then press Back at once. Expect Back to be ignored until the result arrives.

## Should-fix

### S1. The failure path assumes the session is ANONYMOUS again before the hold drops
- `MainActivity.kt:453-459` and `RegisterViewModel.kt:253-268`: on `NifTaken` or `Failure`, the ViewModel sets `isRegistering = false` as soon as `deleteNewUser` returns. The role reaches the redirect by another route: the `AuthStateListener`, then `SessionViewModel`'s `flatMapLatest` / `stateIn`, then `collectAsStateWithLifecycle`. Firebase does not promise that the listener runs before the `delete()` / `signOut()` continuation resumes.
- If the hold drops while `role` is still `SETUP`, `goHome()` opens Account setup with `popUpTo(graph)`. The form and its "This NIF is already registered" are gone, and the user lands on the dashboard a moment later. The comment at `MainActivity.kt:450-452` assumes the good order.
- It is several frames apart, so it probably works. Nothing has run on a device yet, though, and this is R1, the riskiest part of the task.
- **Fix:**
  1. Run the plan's NIF-clash device check first, and watch for any flash of Account setup.
  2. Defensive option: in `FirestoreUsersRepository.deleteNewUser`, after the delete or sign-out, wait until the auth state is null, for example `withTimeoutOrNull(2_000) { callbackFlow { val l = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }; auth.addAuthStateListener(l); awaitClose { auth.removeAuthStateListener(l) } }.first { it == null } }`.
  3. Or, in `CityFixApp`, skip the redirect while `screen == TopBarScreen.REGISTER && role == AccessRole.SETUP` until Register reports success through a second callback (`onRegistered`). Keep the process-death case (Q11) in mind if you pick this one.

### S2. Operator Submit while the domain is loading or unavailable shows no field error, and the domain is never re-read
- `RegisterViewModel.kt:313` makes `emailError` return `null` while `isLoadingDomain`. `RegisterUiState.isValid` (`RegisterUiState.kt:66`) needs `operatorDomain != null`. A quick Submit therefore shows only "Some fields need attention", with no field marked.
- If the read in `init` (`RegisterViewModel.kt:66-72`) fails (offline on a first install, or before `municipality/main` exists in the console), `operatorDomain` stays `null` for the life of the ViewModel. Coming back online does not help until the user leaves and reopens the screen.
- **Fix:**
  1. Move the read into `private fun loadDomain()` and call it from `init`, from `onRoleSelected(OPERATOR)` and from `submit()` whenever `operatorDomain == null && !isLoadingDomain`.
  2. In `submit()`, for an operator while `isLoadingDomain`, return early. The hint `register_operator_domain_loading` is already shown.

### S3. The profile photo upload retries errors that cannot succeed and then drops the photo silently
- `ProfilePhotoWorker.kt:47-66`: every exception means `Result.retry()`. The draft rules let only `auth.uid == uid` write `users/{uid}/photo.jpg` and update `users/{uid}`.
- If the new user taps Log out on Account setup before the upload runs (likely offline), each attempt fails with a permission error. After 5 attempts the photo is gone without a message.
- That is acceptable for an optional photo, but it should be explained, not discovered.
- **Fix:**
  1. At the start of `doWork`, `if (app.auth.currentUser?.uid != uid) return Result.retry()`, capped by the same `MAX_ATTEMPTS`. Or treat `StorageException.ERROR_NOT_AUTHORIZED` / `FirebaseFirestoreException.Code.PERMISSION_DENIED` as final.
  2. Add the case to the plan's failure-mode table in the docs phase.

## Nit

- **N1** `FirestoreUsersRepository.kt:125`: `registration.georeferencing ?: Coordinates(0.0, 0.0)` would silently write a point in the Gulf of Guinea if the guard at line 69 ever changed. Pass the checked non-null location into `userDocument`, or use `requireNotNull(registration.georeferencing)`.
- **N2** `utils/Validation.kt:11` (`EMAIL`): the regex accepts `a@b..pt` and `.a@b.pt`. Firebase rejects these with `ERROR_INVALID_EMAIL`, which `toAuthError()` maps to `INVALID_CREDENTIALS`, a misleading "wrong e-mail or password" on Register. Tighten the regex to `^[A-Za-z0-9_%+-]+(\.[A-Za-z0-9_%+-]+)*@[A-Za-z0-9-]+(\.[A-Za-z0-9-]+)*\.[A-Za-z]{2,}$`, or map `ERROR_INVALID_EMAIL` to an e-mail field error in `AuthErrors.kt:25`.
- **N3** `RegisterViewModel.kt:224-227`: process death after `photoUploader.enqueue` but before the Success update gives a new ViewModel with `registered = false`. Its `onCleared` then deletes the source the queued worker needs, and the worker ends with `Result.failure()` at `ProfilePhotoWorker.kt:31`. Very rare; mention it in the docs, or have the worker log the missing file.
- **N4** `RegisterScreen.kt:440`: the photo-copy `CircularProgressIndicator` has no content description. Reuse `cd_register_progress`, or add `cd_register_photo_copying` (EN/PT).
- **N5** `RegisterScreen.kt:214-241`: the name field could use `KeyboardOptions(capitalization = KeyboardCapitalization.Words)` and the e-mail field `autoCorrectEnabled = false`. `RegisterTextField` needs a `capitalization` parameter for this.
- **N6** `RegisterScreen.kt:11-12, 33-40`: the imports are out of order (`CircleShape` before `annotation`, `DisposableEffect` after `LaunchedEffect`, `rememberUpdatedState` after `setValue`). Use Optimize imports.
- **N7** Docs phase: the overview "Handover (D-009)" and the F-010 handover (overview line 505) still say the operator document is created only after verification. Record the Q1 deviation there and in the new decision (0016, R8), and tell Francisco about the `firestore.rules` edit, as Gate 1 asked.

## Checked and fine
- `holdRedirect`: saveable, so rotation keeps the hold. After process death the new ViewModel is not registering, so the screen's `LaunchedEffect` releases it. D-009's fresh-start case (`handledRole == null`) is not touched, because Register is reachable only after `handledRole` is set. Success releases the hold in the same update that sets `registered`, which avoids a double submit. `onAuthScreen` covers REGISTER, so the 10 km gate never covers the form.
- The order of create, transaction and delete matches the plan. `NifTakenException` is detected directly and as a cause. `UNAVAILABLE` maps to NETWORK. The photo is enqueued only after the commit. `toAuthError()` checks `WeakPassword` before `InvalidCredentials`.
- Validation: the NIF weights 9..2 and the `r < 2 -> 0` rule are correct; phone `[29]` and mobile `9` follow Q6; the password rule follows Q7. `emailErrorFor` uses the shared regex (Q15).
- `SavedStateHandle` holds role, name, e-mail, phone, NIF, impact switch, both coordinates and the photo path. A restored photo path is used only if the file still exists. Passwords are never saved.
- Worker: fixed target name, so retries overwrite. The thumbnail is deleted at once. The source and target are deleted on success, on an undecodable file and after the last attempt. `CancellationException` is rethrown.
- Rules: only the `email_verified` term of the operator create branch is removed, with a comment, and `storage.rules` is unchanged. Nothing was deployed.
- MVVM: stateless `RegisterContent` with previews (portrait, landscape 800x360, PT). Firebase is used only in `data/` and the worker. Errors are `@StringRes`. The switch row is `toggleable(Role.Switch)` and at least 48dp high. The segmented buttons are at least 48dp. The field errors are exposed through `semantics { error(...) }`.

# D-011 review

Reviewer: reviewer agent · Date: 2026-11-15 · Range: `ed005da..b59a15e` (fd388df..b59a15e, 7 Kotlin slices + 1 draft rules slice)

## Verdict
**Approve with should-fix.** There are no Blocking findings. Fix S1 before push. S2 and S3 depend on the device checks, so do those before the Docs phase.

What was verified:
- `./gradlew assembleDebug lint`: build OK; lint **0 errors / 22 warnings**, the same ids as the baseline (5 GradleDependency, 7 NewerVersionAvailable, 1 AndroidGradlePluginVersion, 1 RedundantLabel, 7 UnusedResources (only the template colours), 1 UseKtx). No D-011 resource is unused.
- EN/PT parity: the key sets are equal. The only keys missing from PT are `app_name`, `setup_phone_prefix` and `setup_phone_display`, which are all `translatable="false"`. Every new string has a PT text, and the PT terms ("Perfil", "Terminar sessão") match the existing menu.
- Slice 1 (Register refactor): `RegisterTextField`, `PasswordField`, `ImpactDetectionRow` and `ProfilePhoto` were moved verbatim to `ui/components/AccountFields.kt`. The only additions are `readOnly` and `supportingHint`, both with defaults that keep the old behaviour. `RegisterContent` passes `File(photoPath)` and `showRemove = photoPath != null`, then draws `FormMessage(photoNotice)` right after the picker, so the visible order is the same. The only difference is spacing (the notice now follows `FormColumn`'s spacing instead of the photo column's 8dp), which the state log records. `CitizenLocation`, the role selector and every Register call site are otherwise unchanged. **No behaviour change.**
- `updateProfile`: the map contains only `phone` and, when `user is Citizen`, `impactDetection`. The photo goes only through the worker, which writes `photoPath`. No path writes `name`, `nif`, `georeferencing`, `role`, `registrationStatus` or `email`. `syncEmail` writes only `email` and is called only by the session.
- Re-authentication: `reauthenticate` uses the same `challengeOf(e)` as `signIn`, and `signIn` behaves as before. The challenge and the verification id are private fields, never saved, and passwords are not saved. Back on CODE drops the challenge, and the `challenge !== current` guard drops late answers. Rotation keeps the step through the ViewModel, and `codeRequested` stops a second SMS. After process death the form comes back with the new e-mail and empty passwords.
- Draft rules: `changedKeys().hasOnly([...])` is a correct allow-list, so name, NIF, georeferencing, role, registration status, `createdAt` and the validation fields cannot change through the owner rule. The operator rule is a separate `allow update` and still allows only `registrationStatus` / `validatedBy` / `validatedAt` on citizens. `impactDetection` is allowed only for `CITIZEN`. `email` must equal the verified token e-mail, and an operator's must stay in `municipality().emailDomain`, using the same expression as the create rule. Not deployed.
- Anonymous users: there is no menu entry, all three routes sit inside `RequireAccess(CITIZEN, PENDING_CITIZEN, OPERATOR)`, and the ViewModels are built only from `Session.SignedIn.user.id`. A profile is read only for the signed-in uid, and the log lines contain no personal data. Nothing is exposed to anonymous users.
- MVVM: each screen has a stateful `XxxScreen` and a stateless `XxxContent` with previews (portrait, landscape and PT). `UiState` holds `@StringRes` only. The repositories are interfaces, and no Firebase type leaves `data/`. The Activity is passed through the call and never stored.

## Blocking
None.

## Should-fix

### S1. The e-mail sync can write a stale e-mail back, and it retries on every refused write
`SessionViewModel.kt`, the `else` branch: the check runs on **every** `users/{uid}` snapshot, with an `account` value that may be old.
- **Two devices.** Device A opens the link, logs in with the new e-mail and syncs `users/{uid}.email = new`. Device B is still signed in with the old cached `AuthAccount`: its refresh token is revoked, but its ID token stays valid for up to an hour. B gets A's snapshot, sees a mismatch and writes the **old** e-mail back. A then sees B's write and writes the new one again, and the two devices keep overwriting each other until B is signed out. The draft rules do not stop this, because B's old token still carries the old verified e-mail, so `request.resource.data.email == request.auth.token.email` holds. Test mode does not stop it either.
- **Refused write.** If the write is ever refused (deployed rules, an operator e-mail outside the domain (R7), or a token claim that does not match), the server rejects it, the listener reverts the cached document, the next snapshot mismatches again, and `syncEmail` runs again. The result is an unbounded write/reject loop, which contradicts the plan's "writes once per change".

Fix: check once per account emission, on the first `Found` snapshot only. A new `account` value comes only from a sign-in or from `reloadAccount()`, and `reloadAccount()` already calls `getIdToken(true)`, so the one write always carries a fresh token.
```kotlin
else -> {
    var emailChecked = false
    users.observeUser(account.uid)
        .onEach { profile ->
            if (!emailChecked && profile is UserProfile.Found) {
                emailChecked = true
                if (account.email.isNotBlank() &&
                    !profile.user.email.equals(account.email, ignoreCase = true)
                ) users.syncEmail(account.uid, account.email)
            }
        }
        .map { it.toSession() }
}
```
Update the KDoc comment ("checked once per account value: a sign-in or a reload, both with a fresh token"). Then add a line to decision 0017 or the 0015 amendment explaining why a later snapshot never triggers the sync.

### S2. The photo after Save: the pending notice may never clear, and the preview falls back to the old photo
`ProfileViewModel.onProfile`, `save()` and `ProfileContent` (R4).
- `ProfilePhotoWorker` overwrites `users/{uid}/photo.jpg` and writes `downloadUrl`. Firebase Storage usually keeps the existing download token when an object is overwritten, so `photoPath` probably gets the **same** URL. Then `user.photo != photoAtSave` never becomes true, `photoUploadPending` and "The new photo is uploaded when you are online" stay on screen for the whole visit, and Coil keeps serving the cached old image (memory and disk cache are keyed by URL).
- `save()` also sets `newPhotoPath = null` at once, so the preview jumps back to the **old** stored photo right after the user saves a new one.

Fix (the R4 fallback the plan already describes; do it now if the device check confirms the same URL, which is the likely case):
- In `ProfilePhotoWorker`, write `update(mapOf("photoPath" to url, "photoUpdatedAt" to System.currentTimeMillis()))`. This is one recorded deviation from "Do not touch".
- Map `photoUpdatedAt` into the model or `UserProfile`, and build the Coil request with `ImageRequest.Builder(ctx).data(url).memoryCacheKey("$url#$t").diskCacheKey("$url#$t")`.
- Clear `photoUploadPending` when `photoUpdatedAt` changes, not when the URL does.
- Add `'photoUpdatedAt'` to the `hasOnly([...])` list in `firestore.rules`.
- In `save()`, keep a `savedLocalPhoto` (display-only, not deleted in `onCleared`) to show until the upload completes.

If the device shows a new URL each time, record that in `state.md` and keep only the preview part.

### S3. The acceptance checks still depend on unverified device behaviour (R1-R4)
The code is consistent with the plan, but these are facts it relies on that nobody has observed yet:
- R1: `reauthenticate` throws `FirebaseAuthMultiFactorException`, `resolveSignIn` completes the re-authentication, and the session stays `SignedIn` (no `AuthStateListener` restart).
- R2: what this device does after the link.
- R3: `EMAIL_IN_USE` under e-mail enumeration protection.
- R4: see S2.

Fix: run the device checks listed in the state log before `/write-docs`, and record each outcome (R1-R4) in the Log, so decision 0017 states observed facts. If R1 fails (for example, the session restarts and the route-scoped ViewModel is lost on CODE), keep D-011 open rather than documenting the flow as working.

## Nit

### N1. Back on CODE while the change is running re-enables the form
After `resolveSecondFactor` succeeds, `performChange()` runs with `step = CODE` and `isBusy = true`, and `challenge` is already null. `BackHandler` / Back calls `backToForm()`, which sets `isBusy = false` on FORM. The user can then press Continue again (a second re-authentication and SMS, and for EMAIL possibly a second link) while the first change is still running, and then DONE overwrites the form. Fix: add a private `changing` flag, set in `performChange()` and cleared at its end, and make `backToForm()` return early while it is set.

### N2. `updateProfile`'s `Boolean` result is ignored
`ProfileViewModel.save()` drops the result, because `hasChanges` already gates the call. Either use it (`if (!users.updateProfile(...)) return`) or make the function return `Unit` and remove "Returns false..." from the KDoc, so the defence does not have to explain an unused contract.

### N3. The Profile photo label says "Photo (optional)"
`ProfilePhotoPicker` always shows `register_photo_label`. On Profile, "(optional)" reads oddly. Fix: add a `@StringRes labelRes: Int = R.string.register_photo_label` parameter, and pass a new `profile_photo_label` ("Photo" / "Fotografia") from Profile.

### N4. `profile_photo_uploading` text
"The new photo is uploaded when you are online." is also shown while online. Fix: "Uploading the new photo. Without a connection it waits until you are online." / "A enviar a nova fotografia. Sem ligação, espera até estar online."

### N5. The draft rules do not check types or formats on the allow-listed fields
The owner can write `phone: "abc"`, `impactDetection: "yes"` or any `photoPath`. `getBoolean("impactDetection")` on a string throws during mapping, so this only harms the owner, and the brief asks for validated formats. Fix: add terms to the owner rule:
```
&& (!changedKeys().hasAny(['phone']) || request.resource.data.phone.matches('^[0-9]{9}$'))
&& (!changedKeys().hasAny(['impactDetection']) || request.resource.data.impactDetection is bool)
&& (!changedKeys().hasAny(['photoPath']) || request.resource.data.photoPath is string)
```
Draft only, so tell Francisco as with slice 8.

### N6. `currentEmail` is taken once in `CredentialChangeViewModel`
If the account changes while the screen is open (for example, a reload from another path), the "same e-mail" rule and the read-only field show the old value. Fix: collect `auth.account` in `init`, as `ProfileViewModel` does.

## Notes for the defence (not findings)
- The plan's rule "only the verified token e-mail" rests on `reloadAccount()` calling `getIdToken(true)`, which is pre-existing D-009 code. Point to it when explaining why the sync write passes the draft rule.
- The rules are a draft and test mode is still on, so the "direct write is denied" check can only be shown in the Rules Playground (R8).
- Two `observeUser(uid)` listeners (the session and Profile) share the Firestore cache, so this does not cost two server reads per change.

## Round 2

Reviewer: reviewer agent · Date: 2026-11-15 · Range: `f085729..HEAD` (f704087, f592488, 3b3dfb1, 8f3257a, 2ca6af5, d0a9663, fc55408)

### Verdict
**Approve.** S1, S2 and N1-N4 are resolved, Register still works, and the change to the rules allow-list is correct. Nothing new is Blocking or Should-fix. S3 (the R1-R4 device checks, plus the new ones the round 1 log added) is still open and must be done before `/write-docs`. N5 and N6 stay with the docs phase, as decided.

What was verified:
- `./gradlew assembleDebug lint`: build OK. Lint reports **0 errors / 22 warnings** with the baseline ids (5 GradleDependency, 7 NewerVersionAvailable, 1 AndroidGradlePluginVersion, 1 RedundantLabel, 7 UnusedResources, 1 UseKtx). The new `profile_photo_label` is used.
- EN/PT: `profile_photo_label` and the reworded `profile_photo_uploading` are in both files, and the texts match round 1's suggestion.
- **S1 resolved.** `SessionViewModel` keeps a local `emailChecked` inside the `flatMapLatest` branch, so only the first `Found` snapshot per branch is compared. A later snapshot (another device's write, or the cache reverting a refused write) never calls `syncEmail`. This removes the ping-pong between devices and the loop of writes and rejections. The KDoc explains why. See R2-N1 for one case the comment over-claims.
- **S2 resolved.** The worker writes `photoPath` and `photoUpdatedAt` in one `update` with a client epoch value, not `serverTimestamp()`, so the local snapshot already carries a non-null value. `UserProfile.Found.photoUpdatedAt` defaults to null, which keeps the other `Found(...)` callers source-compatible, and `model/` is unchanged. `photoUploadPending` now clears when the version changes, including the first upload (null to a value). The Coil memory and disk keys are `"$url#$t"` and fall back to the plain URL when `t` is absent. `savedPhotoPath` keeps the saved photo on screen after Save and is cleared in the same update as the pending flag. It is not deleted in `onCleared`, because the worker owns the file.
- **Register still works.** `RegisterViewModel` and `RegisterScreen` are unchanged. `ProfilePhotoPicker`'s new `labelRes` defaults to `register_photo_label`. The registration upload goes through the same worker, and its extra field is accepted by the rules, as the next item shows.
- **Rules.** `photoUpdatedAt` is added to the owner `changedKeys().hasOnly([...])`. The worker's write is always an `update` on an existing document, so the owner update rule is what applies on both paths:
  - On registration, a `PENDING` citizen or a new operator writes `{photoPath, photoUpdatedAt}`. The rule does not depend on `registrationStatus`, and neither key triggers the `impactDetection` or `email` terms, so the write is allowed.
  - On Profile, the same write is allowed.
  - The `allow create` rule does not need the field: registration never writes `photoUpdatedAt`, and the create rule has no key allow-list anyway.
  - The operator validation rule is still a separate `allow update` limited to `registrationStatus` / `validatedBy` / `validatedAt`.
  - Name, NIF, georeferencing, role and the validation fields remain locked.
  - Nothing is deployed.
- **N1 resolved.** `changing` is set at the start of `performChange()` and cleared in a `finally`, so a failure or a cancellation still clears it. Both the `BackHandler` and the top-bar Back go through `backToForm()`, which returns early while the change runs. On the main dispatcher there is no race.
- **N2 resolved.** `updateProfile` returns `Unit` and the KDoc says why. The only caller is still gated by `hasChanges`.
- **N3 and N4 resolved.** Profile passes `R.string.profile_photo_label` ("Photo" / "Fotografia"), and Register still shows "Photo (optional)".
- **No new exposure.** The only data added is the photo version on the owner's own document. Anonymous users still cannot reach any of the three routes, and the log lines are unchanged.

### Blocking
None.

### Should-fix
None new. S3 from round 1 remains: record the R1-R4 outcomes and the round 1 fix checks in the Log before `/write-docs`.

### Nit

#### R2-N1. "Once per account value" is really once per subscription
`session` uses `SharingStarted.WhileSubscribed(5_000)`. When the app has been in the background for more than 5 s and comes back, `auth.account`, a `StateFlow`, is collected again and emits the same current value. `flatMapLatest` then starts a new branch with `emailChecked = false`, so the check runs again with the same cached `AuthAccount`. `retry()` has the same effect, which the Log already records.

As a result, a second device with a stale account can still write the old e-mail once each time it returns to the foreground, until its ID token expires (up to about an hour) and it is signed out. This does not loop, because each return gives one write. However, the KDoc says "a second device with an old cached account cannot write the old e-mail back", which is stronger than what the code does, and the Log's accepted edge mentions only the cold start.

Fix, either of these:
- (a) Reword the KDoc and the Log's accepted edge to say "once per account emission, including a resubscription after 5 s in the background; at most one stale write per return, until the old token expires".
- (b) Make it truly once per account value with a ViewModel field compared by identity:
  ```kotlin
  private var emailCheckedFor: AuthAccount? = null
  // in the branch:
  if (emailCheckedFor !== account && profile is UserProfile.Found) {
      emailCheckedFor = account
      ...
  }
  ```
  A resubscription re-emits the same instance, so the check is skipped. A sign-in or a reload that changes the account creates a new instance, so the check runs. `MutableStateFlow` does not re-emit an equal value, so a reload that changes nothing triggers no check either way.

(b) is four lines. Choose (a) if you prefer not to change code again before the defence.

#### R2-N2. Add `photoUpdatedAt` to the N5 type checks in the docs phase
When N5 is handled, also add `&& (!changedKeys().hasAny(['photoUpdatedAt']) || request.resource.data.photoUpdatedAt is int)`. `getLong("photoUpdatedAt")` on a string would throw during mapping, the same failure mode as `impactDetection`. This is draft only, so tell Francisco.

#### R2-N3. Mention the two-photo case in the docs
If the user saves photo A and then photo B before A's upload ends, `REPLACE` cancels A's worker, which is expected. If A's worker had already written, its snapshot clears `photoUploadPending` and `savedPhotoPath` while B is still uploading. For a moment the screen shows A, and the notice disappears until B's snapshot shows B. This is harmless and rare. Fix: one line in decision 0017's R4 note. No code change.

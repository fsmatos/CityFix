# D-012 review

Scope: `git diff 1c09646..HEAD` (slices 1-5: `f4fc8b2`, `0e019ba`, `29482f0`, `c778e39`, `15356d0`; state `ad66e76`). Checked against `00-plan.md`, `01-coding-plan.md`, the task file, the draft `firestore.rules` / `storage.rules`, decisions 0003/0010/0015/0019 and `kotlin-conventions`. The brief PDF was not reread (same limit as the plan, R6).

Verify (2026-11-16): `./gradlew assembleDebug` BUILD SUCCESSFUL; `./gradlew lint` 0 errors, 22 warnings (AndroidGradlePluginVersion 1, GradleDependency 5, NewerVersionAvailable 7, RedundantLabel 1, UnusedResources 7 (template colours), UseKtx 1). Same as the baseline; nothing new from D-012.

## Verdict
**Approve with should-fixes.** Nothing blocking. The code follows the approved plan (Q1-Q12 as recommended, slice 6 skipped), and the four recorded deviations are small and explained. Fix S1 and S2 before the device checks. S3 is a one-line accessibility fix.

What was checked and is fine:
- **Access control.** All three destinations are inside `RequireAccess(OPERATOR)`, and their ViewModels (and so the `users` / `categories` listeners) are created inside the guard. `AccessRole.OPERATOR` is only reached through `Session.SignedIn` with an `Operator`, which comes after e-mail verification and MFA (decision 0015). The menu entries come from `menuItemsFor`, unchanged. Category writes exist only in `FirestoreCategoriesRepository` and are called only from `CategoriesViewModel`.
- **Personal data.** `PendingCitizenRoute` carries only the uid. The logs contain no names, NIFs or e-mails. Preview data is synthetic. Anonymous users and citizens cannot reach any screen that shows another citizen's data. The exact map is shown only to operators.
- **Activation write shape.** One `update` with exactly `registrationStatus: "ACTIVE"`, `validatedBy` (the operator's profile id, which is the Auth uid) and `validatedAt` (Long epoch ms). This matches the draft rule `changedKeys().hasOnly(['registrationStatus', 'validatedBy', 'validatedAt']) && validatedBy == request.auth.uid` with `resource.data.role == 'CITIZEN'`.
- **Operator list.** `users` read is `isSignedIn() && (uid == auth.uid || isOperator())`. `isOperator()` does not depend on `resource`, so the `whereEqualTo("registrationStatus", "PENDING")` list query is allowed for an operator and refused for a citizen.
- **Category writes.** Create writes `{name, active: true}` (`toCategoryDocument()`), rename writes `name` only, the switch writes `active` only. All are covered by `categories: allow write: if isOperator()`. Profile photos: `storage.rules` already lets operators read `users/{uid}/photo.jpg`, and `photoPath` holds a download URL (ProfilePhotoWorker), so `AsyncImage(photo)` works.
- **Index.** There is one equality filter and no `orderBy`, so the automatic single-field index is enough. No composite index is needed. The sort by `createdAt` (a Long since D-010) happens in the app.
- **Offline.** No write is awaited, so writes are queued in the persistent cache and the listeners show them at once. This matches F-011/D-011 and the failure-mode table.
- **Rotation.** The editor (including the typed name), the confirm dialog and the lists live in route-scoped ViewModels. The scroll states use `rememberScrollState`. Process death is out of scope (Q9).
- **MVVM.** Each screen has a stateful `XxxScreen(viewModel)` and a stateless `XxxContent(state, ...)`, previews target the stateless one, state is private `MutableStateFlow` + `asStateFlow()`, and the factories are plain.
- **i18n.** There are 38 new keys in both `values/` and `values-pt/` with the same names. The PT wording follows the project's terms ("Registo", "munícipe", "ocorrências"). There is no literal UI text.
- **Orientation.** The detail screen uses two `weight(1f)` scrolling panes in landscape (P07). The lists use one column capped at 600dp and centred. The categories list has bottom padding so the last row clears the FAB.
- `FirestoreMapping.kt` changes only the one KDoc line (Q1). `TicketsRepository`, the report form, `model/`, the rules and `google-services.json` are untouched.

## Blocking
None.

## Should-fix

**S1. The focus request can run before the dialog's text field is attached** (`CategoriesScreen.kt`, `CategoryEditorDialog`).
`LaunchedEffect(Unit) { focusRequester.requestFocus() }` sits in the parent composition, but `AlertDialog` composes its `text` slot in the dialog window's own composition, which attaches later. If the coroutine runs first, `requestFocus()` throws "FocusRequester is not initialized". This is a well-known crash pattern with dialogs, and no device check has been run yet. It is the only `requestFocus()` in the app, so no earlier screen has proven the pattern safe.
Fix: move the effect into the dialog's content, next to the field it focuses:
```kotlin
text = {
    OutlinedTextField(/* ... */ modifier = Modifier.fillMaxWidth().focusRequester(focusRequester) /* ... */)
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
},
```
Then check on the device: open Add, open Edit, and rotate with the dialog open.

**S2. A double tap on "Activate" in the dialog can call `navigateUp()` twice** (`CitizenApprovalViewModel.activate`).
`activate` checks only `canActivate`. That flag stays true until the listener delivers the local write, so a second `onConfirm` in the same frame passes the check. It then writes again, shows a second snackbar and calls `navigateUp()` again. The second `navigateUp()` pops the Approvals list too and lands the operator on the home screen.
Fix: also require the dialog to be open, and close it in the same step:
```kotlin
fun activate(onActivated: () -> Unit) {
    val state = _uiState.value
    if (!state.confirming || !state.canActivate) return
    _uiState.update { it.copy(confirming = false) }
    ...
}
```

**S3. TalkBack reads the category switch as a contradiction** (`CategoryRow`).
The toggle area has `contentDescription = "%1$s active"` ("Pavement active") and also `stateDescription = "Inactive"`. For an inactive category TalkBack says "Pavement active, Inactive, Switch". In PT it says "Pavimento ativa, Inativa".
Fix: make the content description the label only. Either use `category.name` or change `cd_category_active` to "Active: %1$s" / "Ativa: %1$s", and keep `stateDescription` as the on/off value.

## Nit

**N1. Duplicate check can be bypassed while the list is loading** (`CategoriesContent`).
The FAB is shown while `isLoading`. Saving before the first snapshot checks against an empty list, so a duplicate can be created (mainly offline with a cold cache).
Fix: show the FAB only when `!state.isLoading`, or have `onEditorSave` return early while `isLoading`.

**N2. A bad document ends the pending list listener** (`FirestoreUsersRepository.pendingCitizens`).
`it.getLong("createdAt")` throws if a hand-made console document stores `createdAt` as a Timestamp or a string. The `.catch` then emits an empty list and the flow ends, so the screen shows "No registrations are waiting." until it is reopened.
Fix: read the value leniently, for example `(it.get("createdAt") as? Number)?.toLong() ?: 0L`. Mention in the docs that `.catch` ends the listener (the same pattern as the other repositories).

**N3. Draft validation rule is looser than the write** (`firestore.rules`, operator `users` update; needs Diogo's explicit OK, never deployed).
The rule restricts the keys but not their values. An operator could set `registrationStatus` to any string or change an ACTIVE citizen's `validatedAt`. Optional tightening to propose in the docs or at the defence: `&& resource.data.registrationStatus == 'PENDING' && request.resource.data.registrationStatus == 'ACTIVE' && request.resource.data.validatedAt is int`. With that rule, the "two operators activate the same citizen" case in the failure-mode table would be refused for the second operator rather than "last write wins". Update the table if you adopt it.

**N4. Task file says "tap to edit the name"; the code uses an Edit icon button.**
The approved plan chose the icon (00-plan), so this is acceptable. Optionally make the name column `clickable(onClick = onEditClick)` too, so either works. Record the choice in the docs.

**N5. Redundant `semantics(mergeDescendants = true) { }` on `Card(onClick)`** (`PendingCitizenCard`).
A clickable `Card` already merges its children. Remove the modifier, or keep it and say why in a comment.

**N6. "Already active" can flash during the exit transition.**
After Activate, the local write reaches the detail's listener while the screen animates out, so the button can be briefly replaced by "This registration is already active.". This is cosmetic. Mention it if someone notices it at the defence, or keep the button while `confirming` was just closed (not worth more code).

## For the docs phase
- Tell Francisco about the `toCategoryDocument()` KDoc line (Q1) and R4 (a rename while a citizen's form is open keeps the old name in that new ticket).
- State that the server does not enforce "only operators write categories / activate", because the rules are not deployed (R1).
- The device checks listed in `state.md` are all still to do. These need a real device or emulator: the two-device activation, the offline Activate, rotation with each dialog open (after S1), and the category deactivate/rename effect on the report form and on old tickets.

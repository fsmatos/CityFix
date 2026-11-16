# D-013 review

Range: `e4ac7bf..HEAD` (slices 1-7 `ae0bc42`..`bc74b4f`, state log `9167876`). Reviewed 2026-11-16 against `00-plan.md`, `01-coding-plan.md`, the task file, `state.md` deviations, the draft `firestore.rules`, CLAUDE.md and kotlin-conventions.

**Verdict: approve with should-fixes** (one should-fix, no blocking findings).

Verify: `./gradlew assembleDebug` pass; `./gradlew lint` 0 errors / 22 warnings (same as the baseline, no new ones). EN/PT parity: every new key is in both files. The only keys missing from `values-pt` are the existing untranslatable ones (`app_name`, `setup_phone_display`, `setup_phone_prefix`).

## What was checked and holds
- **Personal data:** `observeUser` for other users, `PersonContact` and `responsibleNames` exist only in the operator branch of `composable<TicketDetailRoute>` (`MainActivity.kt:356`, reached only when `user is Operator`). Citizens and anonymous users keep F-014's `TicketDetailScreen` / `ShowScreen(PUBLIC)` with the default empty names map. The panel stays behind `RequireAccess(OPERATOR)`. The "Resolution proof" label is not personal data.
- **Transitions:** the buttons come from `allowedTransitions(operator, now)` (`OperatorTicketScreen.kt:210`, `:280-294`). The repository re-runs `transition()` in `startWork`, `askForClarification` and `resolveTicket` (dry run before any file work, then the real run), so a stale or forged call writes nothing.
- **One batch per transition:** `commitStatusChange` still writes `tickets` status fields, the `publicTickets` status (PUBLIC only), `ticketLocations.active` and `stats/summary` together. Resolve adds `attachments` via `FieldValue.arrayUnion` in the same `update`. Upload enqueued once after the commit with `addPublicThumbnail = false` (decision 0010).
- **No resolve without proof:** `canConfirmResolve` requires a non-empty draft, `transition(RESOLVED)` rejects empty proof, and `resolveTicket` stops when no proof is PENDING after `prepare`. `prepare` deletes or renames an original only on success, so a failed resolve keeps the draft. Offline-safe: the commit is not awaited and WorkManager runs with `NetworkType.CONNECTED`.
- **Double taps:** `confirmDialog` is synchronous and returns when `openDialog` is null. `resolve` sets `isResolving` before `launch`. The camera button ignores a second tap while `tempCameraPath` is set.
- **Rotation:** filter, dialog, dialog text, proof draft and note are in ViewModels; `tempCameraPath` is `rememberSaveable`; the launchers sit above ShowScreen's orientation branch.
- **Cleanup:** cancel and remove delete the local copies. `onCleared` deletes what is left in the draft, which is empty after a successful resolve. Cancellation between commit and state update is harmless (originals already moved by `prepare`).
- **Francisco's files:** the changes are additive. `commitStatusChange(newAttachments = emptyList())` keeps its three callers unchanged. `ShowScreen.responsibleNames` is a defaulted parameter placed before the trailing slot, so existing callers compile unchanged. `toDocument()` changed only from private to internal. The only implementation of `TicketsRepository` is `FirestoreTicketsRepository` (no fakes to update).
- **Model:** `TicketFilter.filteredFor` is pure and uses `effectiveStatus`. `needsOperatorGeofence` matches the plan.
- **Recorded deviations** (kept `PlaceholderScreen`, the arrow icons, no `showMap`, the extra inactive-category string, the four-argument ViewModel in slice 3, reused report strings, `onCleared` without a `submitted` flag, Cancel disabled while busy): each is justified in `state.md` and acceptable.
- **Rules:** not edited (Q9). The known `resolvedAt` mismatch with the drafts is documented (decision 0018 Q7) and unchanged.

## Blocking
None.

## Should-fix
- **S1 The Resolve section outlives the move it belongs to** (`OperatorTicketScreen.kt:223`). The section is drawn whenever `state.resolving` is true, whatever the ticket's status. The flag lives in the ViewModel and the ticket updates live, so the section can stay open with Confirm enabled after the ticket stops being IN_PROGRESS. That happens when another operator resolves it, a citizen moves it to INFO_REQUESTED, or it reaches CLOSED. This breaks the acceptance check "a closed ticket shows no actions". It also hides the remaining `ActionsRow` (for example Start work after a citizen's info request). The repository still rejects the write, so no bad data is stored; the problem is in the UI.
  Fix: in `OperatorTicketContent`, draw the section only while the move is still offered:
  ```kotlin
  val canResolve = TicketStatus.RESOLVED in moves
  if (state.resolving && (canResolve || state.isResolving)) { ResolveSection(...) }
  else if (moves.isNotEmpty()) { ActionsRow(...) }
  ```
  `|| state.isResolving` keeps the spinner on screen while the operator's own batch has already turned the cached ticket RESOLVED. Do not auto-cancel the draft from the screen while `isResolving` is true: those files then belong to the upload. Leftover draft files from a hidden section are deleted by `onCleared`.

## Nit
- **N1 The dialog stays open when its move disappears** (`OperatorTicketScreen.kt:256`). Example: a Start work dialog on a ticket that a second operator has meanwhile moved to IN_PROGRESS. Confirm then shows "This action is no longer available", which is correct but needs an extra tap. Fix: show the dialog only while its target is in `moves` (REQUEST_INFO -> INFO_REQUESTED, START_WORK -> IN_PROGRESS), or let the screen call `viewModel.onDialogDismiss()` from a `LaunchedEffect(moves)` when the target is gone.
- **N2 File deletes on the main thread in `onCleared`** (`OperatorTicketViewModel.kt:217-220`). At most three small files, and it mirrors `ReportViewModel`, but `.claude/rules/kotlin.md` says "no blocking calls on the main thread". Fix: keep it as it is and say so in the KDoc (already partly done), or hand the paths to a short `Dispatchers.IO` job on the application scope, if the app has one. Be ready to explain the choice at the defence.
- **N3 "Private" label is a separate TalkBack item** (`OperatorPanelScreen.kt:144-151`). It is read after the card rather than as part of it. Fix: wrap the card and the label in `Modifier.semantics(mergeDescendants = true) {}`, or add the scope to the card's description.
- **N4 Category dropdown says "All categories" while a filter is still active** (`OperatorPanelScreen.kt:223`, `:230`). This happens when the selected id is not in `categories` yet, for example during the first emission. The list is then filtered by an id the field does not show. Categories cannot be deleted (D-012), so this is only a brief loading state. Fix: show the selected id's ticket `categoryName`, or reset `filter.categoryId` in the ViewModel when an emitted list does not contain it.
- **N5 Process death loses the proof draft and orphans its copies** (`OperatorTicketUiState`, not in `SavedStateHandle`). The plan only requires rotation, so this is a known limit. Add it to the failure-mode notes in the docs phase rather than to the code.
- **N6 Combined resolve error.** `resolve_error_failed` covers both "not allowed any more" and "proof could not be prepared" because `resolveTicket` returns a Boolean. This is acceptable. Optionally, `resolve()` can call `ticket.allowedTransitions(operator, now)` first and show `operator_action_error_not_allowed` when RESOLVED is no longer allowed.

## Still to do (not review findings)
- Device checks D1-D11 (by hand, two devices, airplane-mode resolve).
- Tell Francisco about the additive edits to `TicketsRepository`, `FirestoreTicketsRepository`, `FirestoreMapping` and `ShowScreen` (state log, slices 4-6).

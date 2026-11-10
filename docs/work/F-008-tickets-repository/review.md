# F-008: Tickets repository — review
Reviewed: 2026-11-08 · Range: `b0ff237..HEAD` (`state.md` Base). Code commits 1648351, 934980b, c7ec657, e51ef62; f925736, 51a6df9 and f1b737a are docs/chore only.
Against: 00-plan revision 2 (with "Behavior that must not change" and the Gate 1 decisions of 2026-11-08), 01-coding-plan, 03-docs-plan, the two deviations in `state.md`, the task `docs/tasks/phase-1/F-008-tickets-repository.md`, `.claude/rules/kotlin.md`, `kotlin-conventions`, CLAUDE.md, the F-007 review (S1, limit across picks and camera), and the class project `AM/Praticas/Contacts_#3/Contacts` (`ContactsApp`, `ContactsRepository`, `ContactsViewModel(+Factory)`, `EditContactViewModel(+Factory)`, `MainActivity`).

**Verdict: changes required (one blocker, small fix).** The repository, wiring and save path follow the approved plan and Contacts_#3 closely, the anonymous projection now sits only in `data/`, and build and lint are at the baseline. But slice 4 breaks an F-007 behaviour on the "must not change" list: with the copy now asynchronous, the limit of 3 no longer holds when two picker/camera results arrive back to back (B1). That is the race the F-007 review raised as S1 and F-007 fixed; this refactor reopens it. **Nothing here ran on a device or emulator** (no device attached); see "Unverified at runtime".

## What was checked
- **Build and lint (run by the reviewer, `./gradlew assembleDebug lint --rerun-tasks`, 46/46 tasks executed):** both pass. Lint: 0 errors, 16 warnings (AndroidGradlePluginVersion 1, GradleDependency 2, NewerVersionAvailable 5, RedundantLabel 1, UnusedResources 7, all on the old `purple_*`/`teal_*`/`black`/`white` colours). Same as the baseline. The compiler prints the 7 known `@StringRes` annotation-target warnings in `ReportUiState.kt` (pre-existing, F-007 N6); no new compiler warning, so the `@Suppress("UNCHECKED_CAST")` leaves nothing behind.
- **Acceptance greps:** `TicketsList`, `SampleTickets`, `SampleCategories` appear nowhere in `viewmodel/`. `toAnonymousView()` is called only in `data/LocalTicketsRepository.kt` (two call sites); the other hits are KDoc and the definition in `model/TicketPrivacy.kt`. `ReportSubmission` and `onSubmitResult` are gone; `reportTicket` has one caller (`ReportViewModel.submit`).
- **Do-not-touch list:** `git diff --stat` shows no change under `res/`, `model/`, `ui/navigation/`, other screens, the catalog or Firebase files. `FileUtils.kt` changes in KDoc only.
- **Strings / i18n:** no string added, changed or removed in EN or PT; the snackbar still uses `report_submitted` (resolved in composition, passed into `launch`).
- **Anonymous data:** the projection happens in the repository (`visibleTickets()`, `getTicketById`), as `.claude/rules/kotlin.md` asks. `TicketsViewModel` exposes only what the repository returns. `creatorId`/`uploadedById` = `LOCAL_USER_ID` stay inside the cache. No regression. (D-007's `description`/`statusHistory` change will now land in one place, as intended.)
- **MVVM / stateless-stateful:** ViewModels own their `MutableStateFlow`s and expose `StateFlow`; `ReportScreen` stays the stateful half, `ReportContent` gets state + lambdas only; no Context in either ViewModel (`AttachmentStorage` keeps the application context). No `!!`, no `GlobalScope`.
- **Permissions:** none added; camera path unchanged (`launchCamera` still catches `ActivityNotFoundException`/`SecurityException`).
- **Accessibility / orientation:** no new control. Submit and add buttons gain `isCopyingAttachments` in their `enabled`; see N5 for feedback during a long copy. Launchers and `tempCameraPath` still sit above the orientation branch. Both ViewModels keep their scopes, so rotation keeps state.
- **Concurrency:**
  - `LocalTicketsRepository`: every suspend member does `withContext(Dispatchers.IO) { mutex.withLock { ... } }`; validation in `reportTicket` runs outside the lock (pure), `add` inside. Correct. `getTicketById` is the exception (S1).
  - `submit()`: `submitted` and `isSaving` are set synchronously on the main thread before `launch`, so a double tap on Submit returns at the guard. The `null` path clears both and sets `report_error_not_created`. Correct.
  - `AttachmentStorage`: cancellation during a copy is caught (the blocking copy finishes, `ensureActive()` or `withContext`'s resume throws), copied files are deleted, the exception is rethrown. Prompt cancellation on the resume back to Main is also caught. Between the return and `onPhotosAttached` there is no suspension point, so a file is either deleted or in state, never lost. Correct for cancellation; non-cancellation errors in N6.
  - Pick limit: B1.

## Behaviour that must not change
| Behaviour | Result |
| --- | --- |
| List: PUBLIC + active, newest first then id; samples | Holds (same `publicActive().map { toAnonymousView() }`, moved verbatim) |
| Projection (0006, today's `toAnonymousView()`) | Holds, now only in the repository |
| Stats over every ticket, private included (0005) | Holds (`ticketsList.stats()`) |
| `getTicketById` (ViewModel) / detail reads `tickets` | Holds (unchanged). Repository copy: S1 |
| Every `reportTicket` null case (3 trims, empty attachments, `add` refuses) and the ticket built | Holds, line for line; `null` unlocks and shows `report_error_not_created` |
| D-006: no id reaches navigation; PRIVATE never opened; snackbar then back | Holds (`onSuccess: () -> Unit`) |
| Active categories only, from the first frame; `submit()` rejects an id outside the list | Holds (synchronous `activeCategories()`, `isValid` unchanged) |
| Scopes: `TicketsViewModel` above the NavHost, `ReportViewModel` per route; `onCleared` deletes unless submitted | Holds |
| Rotation keeps state; process death restores samples | Holds by reading |
| EN/PT strings | Holds (no change) |
| F-007: file names, who deletes which file, notices and their order, camera path | Holds (`savedPaths < accepted` -> copy failed, else `accepted < uris` -> limit) |
| F-007: **limit of 3 across picks and camera** | **Broken by slice 4 (B1)** — unapproved |

Behaviour changes found, classified:
- Approved at Gate 1: `TicketsViewModel` loses `reportTicket` and gains `refresh()`; suspend save with `isSaving` + `onSuccess`; one-frame empty state at cold start (this also covers the detail route showing `ticket_not_found` for a frame after process death, see N8); back or rotation mid-save may skip snackbar/refresh (see N7 for the exact visible effect); slice 4 off-main-thread copy with `isCopyingAttachments` (buttons and Submit disabled while copying, `submit()` returns while copying); tickets live as long as the process.
- Unapproved: B1 only.

## Deviations (logged by the implementer)
- **D1 `@Suppress("UNCHECKED_CAST") ... as T` in both factories.** Accepted. It is the plan's stated fallback, it is exactly Contacts_#3's `ContactsViewModelFactory`/`EditContactViewModelFactory` shape, the reason comment is there, and the build has no warning. Easy to explain at the defence.
- **D2 `CityFixApp(app: CityFixApplication)`.** Accepted. Contacts_#3 reads `application as ContactsApp` inside `setContent`; reading it once in `onCreate` and passing it to a private composable is equivalent and keeps the composable free of the Activity. `android:name=".CityFixApplication"` is in the manifest, so the cast cannot fail.

## Alignment with Contacts_#3
Matches: `Application` subclass with `by lazy` repository (`ContactsApp`); repository in `data/` with suspend functions on `Dispatchers.IO` and an in-memory cache (`ContactsRepository`); synchronous by-id read from the cache (`getContactById`); one factory class per ViewModel built in `MainActivity` from the app object; list ViewModel owns `MutableStateFlow(emptyList())`, `init { refresh }`, `refresh()` in `viewModelScope` (`ContactsViewModel.refreshContacts`); form ViewModel per back-stack entry saving in `viewModelScope` with `isSaving` and `onSuccess { refresh(); navigate back }` (`EditContactViewModel.saveContact`).
Differences, all approved and worth one sentence each at the defence: the `TicketsRepository` interface; the `Mutex`; no JSON file; the `submitted` lock before `launch` (Contacts_#3 sets `isSaving` inside `launch`, so it has a double-tap window; CityFix does not); slice 4 copy off the main thread (Contacts_#3 copies in the launcher callback).

## Blocking

**B1. Slice 4 lets the attachment count exceed 3 when two media results arrive back to back (F-007 "limit of 3", unapproved).**
`ReportViewModel.onMediaPicked` (`ReportViewModel.kt:79-90`) computes `accepted = uris.take(remainingAttachments)` from the attachments present *before* the copy, and only adds them after an IO hop. `onPhotosAttached` (`:93-103`) appends without a cap, and `isValid` has no upper bound. The disabled buttons only stop new taps; they do not stop results already on their way. F-007 S1 established the path: a fast tap on both add buttons (or twice on "Add from gallery") launches two activities, and when the host resumes, the activity-result registry delivers both results one after the other on the main thread. Before F-008, `attachPicked` attached synchronously, so the second result saw the updated count (and F-007 added the camera check in `attachCaptured` for the other order). Now:
- gallery result then camera result: the copy is in flight, `attachCaptured` (`ReportScreen.kt:171`) reads the old `remainingAttachments > 0` and attaches the photo; the copy then appends its files -> 4.
- two gallery results: the second `onMediaPicked` reads the same stale `remainingAttachments` and starts a second copy -> up to 3 + 2. Also, whichever copy finishes first sets `isCopyingAttachments = false` while the other is still running, so Submit is re-enabled mid-copy; files that land after a successful submit are attached to a submitted form, kept by `onCleared` (because `submitted`) and owned by no ticket.
The plan's own failure-mode line says "two quick picks must not exceed the limit". Not reproduced on a device (none attached); by reading.
Fix (small, all in `ReportViewModel`):
1. Enforce the limit when attaching, not when picking. In `onMediaPicked`'s coroutine, after the copy: `val fits = savedPaths.take(_uiState.value.remainingAttachments)`; delete `savedPaths.drop(fits.size)` with `File(it).delete()`; call `onPhotosAttached(fits)`; show `onAttachmentLimitReached()` when anything was dropped (keep "copy failed" first, as today). This also covers the camera-during-copy order, because the camera photo is already in state when the copy returns.
2. If `_uiState.value.submitted` is true when the copy returns, delete all `savedPaths` and return.
3. Replace the single flag flip with a private counter (`private var copiesInProgress = 0`; `++` before `launch`, `--` in a `finally`, and set `isCopyingAttachments = copiesInProgress > 0`), so a second copy keeps Submit and the add buttons disabled until both finish.
Then re-run the slice 4 device check "two quick picks never go over 3" with a gallery + camera fast tap as well.

## Should-fix

**S1. `LocalTicketsRepository.getTicketById` reads the cache without the lock, and its comment gives the wrong reason.** `LocalTicketsRepository.kt:43-45`: "publicActive() returns a copy" — the copy is *built by iterating* the shared `mutableListOf` in `TicketsList`, so a `reportTicket` adding on an IO thread at the same moment can throw `ConcurrentModificationException` or miss the new ticket. Nothing calls it today (state.md says so), but it is on the interface the Firebase phase will implement, and the comment would be hard to defend ("why is this one safe?"). Contacts_#3's `getContactById` has the same gap but no Mutex to contrast with. Fix, either: (a) make it `suspend fun getTicketById(id)` with `withContext(Dispatchers.IO) { mutex.withLock { ... } }` and record the deviation from the plan's "synchronous"; or (b) remove it from `TicketsRepository` and `LocalTicketsRepository` until a caller exists (the ViewModel's `getTicketById` reads its own flow and covers the detail screen), recording that the plan's member was dropped. At minimum, correct the comment to say it is only safe on the main thread while no save runs.

**S2. The acceptance checks are still unrun; run them before `/push-task`.** Every emulator item in 00-plan "Acceptance checks" and in the state.md "Not verified at runtime" list is open, and state.md itself says the person must run them before review. After B1 is fixed, run at least: 3 categories without Graffiti; dashboard 3/2/1/3 days; list order; PUBLIC report on top and PRIVATE report with snackbar and not listed (total +1 both); double tap Submit -> one ticket; rotation on all four screens and mid-form; large video keeps the UI responsive with buttons disabled; gallery x2 and gallery + camera fast taps never exceed 3; back mid-copy leaves no new file in `filesDir/images` (Device Explorer). Tick the boxes in 00-plan and log the run in state.md.

## Nit

**N1. `isSaving` is written but never read by the UI.** `submitted` already locks the form and is what `DetailsPane` checks; the guard in `submit()` checks both. It is Contacts_#3's field, so keep it, but say so in its KDoc ("mirrors EditContactUiState.isSaving; reserved for a progress indicator in the Firebase phase") so the question "what does it do?" has an answer.

**N2. `refresh()` loads tickets and stats under two separate lock acquisitions, and two refreshes can overlap.** A report saved between the two loads gives a list without the ticket and stats with it, until the next refresh; two overlapping refreshes may finish in either order. Harmless in memory. Fix if wanted: keep a `private var refreshJob: Job?`, `refreshJob?.cancel()` before launching; or add a single `suspend fun loadDashboard(): Pair<List<Ticket>, DashboardStats>` under one lock. Otherwise note it in the Firebase task, where Firestore listeners replace it.

**N3. `onMediaPicked` and `submit()` rely on the UI for some guards.** `onMediaPicked` does not check `submitted` before starting a copy (a picker opened just before a valid Submit still copies into a submitted form). B1's step 2 covers the outcome; also return early in `onMediaPicked` when `submitted` is already true so nothing is copied.

**N4. Back during the save can orphan the draft files.** If `viewModelScope` is cancelled before `reportTicket` reaches `ticketsList.add` (e.g. while waiting for the Mutex), no ticket is created but `submitted` is already true, so `onCleared` keeps the files. Milliseconds in memory; record it with the accepted "back mid-save" risk in state.md, and in the Firebase task have `onCleared` delete unless a save actually returned an id (a `saved` flag), rather than unless `submitted`.

**N5. No feedback while a long copy runs.** With a large video the add buttons and Submit grey out for seconds with no explanation, and TalkBack only reads "disabled". Better than the old frozen UI, so not a regression, and the plan forbids new strings here. Follow-up task: a `LinearProgressIndicator` in the attachments section while `isCopyingAttachments`, with a `report_attachments_copying` string (EN + PT) as a polite live region.

**N6. A non-cancellation exception in the copy leaves files behind.** `FileUtils.copyUriToInternalStorage` calls `contentResolver.getType` outside its `try`, so a `SecurityException` escapes; `AttachmentStorage` only cleans up on `CancellationException`, and the uncaught exception in `viewModelScope` crashes the app (as the old main-thread call did). Fix: move `getType` inside the `try` in `FileUtils` (returns `null` like any other failure), so `AttachmentStorage` only ever sees cancellation.

**N7. Rotation mid-save: say precisely what the user sees.** The `onSuccess` lambda captured before the rotation calls `navigateUp()` on the old `NavController` and launches the snackbar on a cancelled scope, so on the new screen the form stays open with Submit locked and no snackbar; `refresh()` still runs (same activity-scoped ViewModel), and Back leaves the form keeping the files (correct, the ticket exists). This is within the accepted risk, but state.md says only "may skip snackbar or refresh". Record the locked-form effect, and in the Firebase task replace the callback with a `saved` flag in `ReportUiState` consumed by a `LaunchedEffect` in `ReportScreen`.

**N8. Detail after process death shows "not found" for a frame.** `composable<TicketDetailRoute>` reads `tickets.firstOrNull { ... }`, and `tickets` starts empty, so a restored back stack on the detail screen flashes `ticket_not_found` before the load lands. Same cause as the accepted cold-start empty state; mention it next to that risk in state.md so it is not discovered at the defence.

**N9. `ReportViewModel` depends on the concrete `AttachmentStorage`.** `kotlin-conventions` asks for ViewModels on interfaces so they could be tested with fakes. Approved as a class in the plan, and there are no tests, so leave it; note in decision 0009 that it can become an interface when the WorkManager upload task touches it.

## Unverified at runtime
No device or emulator was attached, so nothing was run. By reading only: list order and projection on screen, dashboard figures, categories dropdown, PUBLIC/PRIVATE report flow and snackbar, double-tap Submit, rotation on every screen and mid-save, responsiveness during a large video copy, the pick limit (B1 fails by reading), cleanup of `filesDir/images` on back mid-copy, cold-start empty frame, and process-death restore.

## Re-review (2026-11-08)
Range: `b539da0` (fix(F-008): address review findings) and `8e8698b` (state.md entry). Only `ReportViewModel.kt`, `TicketsRepository.kt` and `LocalTicketsRepository.kt` changed; no string, screen, `model/` or manifest change.

**Verdict: approve for the device checks.** B1 and S1 are resolved by reading, with no new behaviour change. S2 (run the emulator acceptance checks) stays open and is now the only thing between F-008 and push. Nits N1-N6 and N9 stay open by choice (the implementer recorded that); N7 and N8 are now recorded in state.md, so they are closed.

**Build and lint (reviewer, `./gradlew assembleDebug lint --rerun-tasks`, 46/46 tasks executed):** pass. Lint gives 0 errors and 16 warnings (baseline). The only compiler warnings are the 7 known `@StringRes` ones.

**B1: resolved.** `onMediaPicked` now applies the limit when the copy returns: it reads `remainingAttachments` from the current state, so any photo or file attached while the copy ran has already taken its slot.
- **Two quick gallery picks:** each copy cuts its result to the slots still free when it returns, so the total stays at 3 or below whichever copy finishes first.
- **Gallery then camera:** `attachCaptured` attaches the photo if a slot is free at that moment. The copy then returns, sees the slot taken, deletes its extra files and shows the limit notice.
- **Camera then gallery:** the photo is attached synchronously before `onMediaPicked` runs, so the order is handled as before.
- **Extras:** `savedPaths.drop(fits.size)` are deleted, and `report_attachments_limit` shows when `fits.size < savedPaths.size`. "Copy failed" still takes precedence, as in F-007. With no race (`fits == savedPaths`), the notices are exactly the old ones, including the `remaining == 0` case (nothing copied, limit notice).
- **Already submitted:** every returned copy is deleted and nothing is attached. In practice this only happens when a picker was launched just before a valid Submit, because `submit()` returns while a copy runs.
- **`copiesInProgress`:**
  - It is incremented before `launch` and decremented in `finally`. Both run on the main thread (`viewModelScope` is `Dispatchers.Main.immediate`), so a plain `Int` is safe.
  - `isCopyingAttachments = copiesInProgress > 0` keeps the add buttons (`canAddAttachment`) and Submit (`enabled`, plus the guard in `submit()`) disabled until the last copy ends.
  - `finally` also runs on cancellation and on the early `return@launch`, so the counter cannot get stuck.
- **Cancellation cleanup:** unchanged. `AttachmentStorage` still deletes on `CancellationException`. The new `try/finally` neither swallows nor rethrows anything else.
- **New behaviour change:** none. When there is no race, the visible result is the same as F-007. In a race the camera photo wins the slot over a copy still in flight. That is an outcome of enforcing the limit, not a new behaviour.

**S1: resolved.** `TicketsRepository.getTicketById` is now `suspend`. `LocalTicketsRepository` reads under `withContext(Dispatchers.IO) { mutex.withLock { ... } }`, in the same shape as the loads, and the comment now gives the right reason (the list may be added to on another thread). The deviation from the plan's synchronous signature, and the contrast with `getContactById`, are recorded in state.md and in the KDoc. There is still no caller. `TicketsViewModel.getTicketById` is a different, synchronous read of its own flow, unchanged, so nothing else had to adapt. When the Firebase task gives it a caller, call it from `viewModelScope`.

**Still open**
- S2: no device attached, so nothing was run. Before `/push-task`, run every 00-plan acceptance check. Add these:
  - gallery x2 fast taps: never more than 3;
  - gallery + camera fast taps: never more than 3, with the limit notice;
  - Submit stays disabled until both copies finish;
  - a pick that lands after Submit leaves no new file in `filesDir/images`.
- Nits N1-N6, N9: optional, as in the first review.

# F-008: Tickets repository (local implementation)
Type: refactor · Task file: docs/tasks/phase-1/F-008-tickets-repository.md
Labels: none
Revision 2 (2026-11-08): Gate 1 approved with the person's answers (below). Pattern: class project Contacts_#3 (`ContactsApp`, `ContactsRepository`, `ContactsViewModel(+Factory)`, `EditContactViewModel(+Factory)`, `MainActivity`).

## Problem
The ViewModels own the data. `TicketsViewModel` holds `TicketsList(SampleTickets.all)`, builds tickets and calls `toAnonymousView()`; `ReportViewModel` reads `SampleCategories`. `.claude/rules/kotlin.md` wants the anonymous filter in the repository layer, and `kotlin-conventions` wants ViewModels on repository interfaces. Decisions 0006 (Q6, Q11) and 0008 put this off until Firebase.

## Outcome
As in Contacts_#3: a repository in `data/` returns data through `suspend` functions on `Dispatchers.IO` with an in-memory cache; `CityFixApplication` holds one instance; separate factory classes give it to the ViewModels; each ViewModel owns its `MutableStateFlow`s and fills them in `viewModelScope`; the form ViewModel saves (`suspend`), like `EditContactViewModel.saveContact`, and the list ViewModel refreshes. Unlike Contacts_#3 there is a `TicketsRepository` interface, so the Firebase phase adds an implementation instead of rewiring.

## Acceptance checks
- [ ] No ViewModel references `TicketsList`, `SampleTickets` or `SampleCategories` (grep `viewmodel/`).
- [ ] `toAnonymousView()` is called only in `data/LocalTicketsRepository.kt` (grep).
- [ ] Dashboard -> list -> detail -> report works as before, in portrait and landscape (emulator).
- [ ] PUBLIC report appears at the top of the list; PRIVATE report shows the snackbar and is not in the list; the dashboard total goes up by one in both cases.
- [ ] Double tap on Submit creates one ticket.
- [ ] Picking a large video does not freeze the UI; add buttons are disabled while it copies.
- [ ] `./gradlew assembleDebug` and `./gradlew lint` pass after every slice with no new warnings (including no unchecked-cast warning from the factories).

## Behavior that must not change
- **List:** PUBLIC and active tickets only, newest first then by id (decision 0005). Samples: `sample-1`, `sample-2`.
- **Projection (decision 0006, today's `toAnonymousView()` as is):** `creatorId` blank, `followerIds` and `messages` empty, every `StatusHistory.responsibleId` blank and `justification` null, every `Attachment.uploadedById` blank, location rounded to 2 decimals. `description`/`statusHistory` still pass through (D-007 not done; hidden in `ShowScreen` only).
- **Stats:** over every ticket, private included (decision 0005). Samples: 3 / 2 / 1, average 3 days.
- **`getTicketById`:** only the projected list; unknown, PRIVATE, RESOLVED, CLOSED give `null`. Detail still reads the collected `tickets`.
- **Report null cases:** the repository trims description, category id and name and returns `null` when any is blank, `attachments` is empty, or `add` refuses; otherwise the same ticket as today (`UUID` id, OPEN, one OPEN history entry, `creatorId = "local-citizen"`, (0, 0), attachments `PENDING` + `uploadedById = "local-citizen"`). No 10 m grouping. `null` unlocks the form and shows `report_error_not_created`.
- **D-006 rule:** the new id is never navigated to; the success callback takes no id, so a PRIVATE report cannot be opened. Snackbar "Report submitted." / "Ocorrência enviada.", then back.
- **Async save (new, must hold):** `submit()` sets the `submitted` lock before launching, so a double tap while saving does nothing; Submit stays disabled until a `null` result unlocks it. Rotation during the save keeps the form (route-scoped ViewModel) and the save continues in `viewModelScope`.
- **Categories:** only active ones (`pavement`, `lighting`, `furniture`; `graffiti` hidden, I.3.E), present from the first frame; `submit()` still rejects an id outside the list.
- **Scopes:** one `TicketsViewModel` above the `NavHost`; `ReportViewModel` per route (draft cleared on leave; `onCleared` deletes draft files unless submitted).
- **Rotation:** all state kept; process death restores the samples.
- **F-007:** limit of 3, file names, who deletes which file, notices, `tempCameraPath` above the orientation branch. Slice 4 moves only the gallery copy off the main thread.
- **Strings:** none added or changed, EN and PT.

Known differences (accepted with the Gate 1 answers):
- Tickets live as long as the process, not the activity (Q4).
- `TicketsViewModel` starts with empty values and fills them from `init` after an IO hop, as `ContactsViewModel` does. The dashboard and list may show the zero/empty state for a frame on cold start. Not hidden with a loading state in phase 1.

## Approach
- **Repository** (Contacts_#3 `ContactsRepository`, plus an interface): `suspend fun loadPublicTickets()`, `suspend fun loadStats()`, `fun getTicketById(id)` from the cache (like `getContactById`), `suspend fun reportTicket(...)`, `fun activeCategories()` (synchronous, so the dropdown never flashes "no categories"). `LocalTicketsRepository` keeps the `TicketsList` as its cache, seeded from `SampleTickets`; IO work in `withContext(Dispatchers.IO)` behind a `Mutex` (the IO pool can run a load and a report at once). No JSON persistence (out of scope).
- **App instance:** `CityFixApplication : Application` with `val ticketsRepository by lazy { LocalTicketsRepository() }` (like `ContactsApp`), registered in the manifest.
- **Factories** (Gate 1 answer 1): `TicketsViewModelFactory(repository)` and `ReportViewModelFactory(repository, attachmentStorage)` in `viewmodel/`, implementing `ViewModelProvider.Factory`. `MainActivity`: `val app = application as CityFixApplication`, `viewModel(factory = ...)`. `create` returns `modelClass.cast(...)`, which throws on a wrong class and avoids the unchecked-cast warning (fallback: `@Suppress("UNCHECKED_CAST")` with a reason).
- **ViewModel owns state** (answer 2): `TicketsViewModel` keeps private `MutableStateFlow`s for `tickets` and `stats`, `init { refresh() }`, `fun refresh()` launches in `viewModelScope` (like `refreshContacts`). `getTicketById` reads its own `tickets` value. `reportTicket` leaves `TicketsViewModel`.
- **Suspend save** (answer 3): `ReportViewModel.submit(onSuccess: () -> Unit)` validates as today, sets `submitted` and `isSaving`, launches `repository.reportTicket(...)`, clears `isSaving`; a non-null id calls `onSuccess()`, `null` unlocks with `submitError`. `MainActivity`'s `onSuccess` = `ticketsViewModel.refresh()` + snackbar + `navigateUp()` (like `saveContact { refreshContacts(); popBackStack() }`). `ReportSubmission` and `onSubmitResult` go away.
- **Gallery copy off the main thread** (answer 4): `data/AttachmentStorage(context)` with a `suspend` copy on `Dispatchers.IO`; `ReportViewModel.onMediaPicked(uris)` launches it. Goes beyond Contacts_#3, which still copies on the main thread.

## Architecture impact
- `data/` gains its first repository; ViewModels depend on an interface and get it through factories. Supersedes the "no repository / no factory / no coroutines" parts of 0006 and 0008 (decision 0009).
- I.3.E "active only" filter moves from `ReportViewModel` to the repository (Q5).
- Ticket creation moves from `TicketsViewModel` to `ReportViewModel` + repository; `TicketsViewModel`'s public API loses `reportTicket` and gains `refresh()` (Diogo's file).
- `ReportScreen(viewModel, onSubmit: (ReportSubmission) -> Boolean)` becomes `ReportScreen(viewModel, onSubmitted: () -> Unit)`.

## Failure modes
- Missing `android:name` in the manifest: the cast to `CityFixApplication` crashes at start.
- Calling `viewModel()` without a factory: crash (no no-arg constructor).
- `onSuccess` captures the composition's `navController`; if the activity is recreated mid-save, the old lambda runs. In memory the save takes milliseconds, so accepted as in Contacts_#3; the Firebase task should switch to a "saved" event in the UI state.
- Back pressed mid-save: `viewModelScope` is cancelled; the ticket may already be in the repository with no snackbar and no `refresh()`, so the list is stale until the next refresh. Accepted (milliseconds); recorded.
- Slice 4: leaving mid-copy must delete what copied; two quick picks must not exceed the limit; Submit blocked while copying.

## Decisions at Gate 1 (2026-11-08)
1. Separate factory classes, as Contacts_#3; no `viewModelFactory { initializer }` companions.
2. ViewModels own their `StateFlow`s and load from the repository in `viewModelScope`; the repository returns data through `suspend` functions with an in-memory cache. `toAnonymousView()` stays in the repository.
3. `reportTicket` is `suspend`; the form ViewModel saves in `viewModelScope` with `isSaving` and an `onSuccess` callback (no id).
4. Slice 4 (gallery copy off the main thread) included.
5. Defaults kept: Q4 accept the longer lifetime and record it; Q5 active filter in the repository; Q6 slices that edit Diogo's files only after the person confirms Diogo agrees; Q7 new decision 0009 (0008 marked "superseded in part"; Diogo marks 0006).
- Keep the `TicketsRepository` interface (Firebase reason); Contacts_#3 has none.

## Risks and open questions
- Diogo's files: `TicketsViewModel.kt` (rewritten, API change), `MainActivity.kt`, `AndroidManifest.xml`. D-007 must change only `model/TicketPrivacy.kt`; tell Diogo the call site moved. If D-007 edits `TicketsViewModel.kt` first: conflict; keep this version and apply D-007 in the repository.
- The one-frame empty state on cold start (above): if the person dislikes it, add `isLoading` later (Firebase phase needs it anyway).

## Out of scope
Firebase, Auth, 10 m grouping, location, JSON/disk persistence, D-007's projection changes, a separate categories repository, `ShowScreen` preview data (keeps `SampleTickets`).

## Slices
1. Add `TicketsRepository` and `LocalTicketsRepository` (unused).
2. `CityFixApplication` + manifest; `ReportViewModelFactory`; `ReportViewModel` takes categories from the repository. (`MainActivity` one line: needs Diogo's OK.)
3. `TicketsViewModelFactory`; `TicketsViewModel` owns its flows and loads from the repository; `ReportViewModel.submit(onSuccess)` saves through the suspend `reportTicket`; `ReportScreen` and `MainActivity` rewired. (Diogo's files.)
4. Gallery copy on `Dispatchers.IO` through `data/AttachmentStorage`.

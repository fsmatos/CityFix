# 0009: Tickets repository in phase 1

Status: accepted (F-008, Gate 1 on 2026-11-08, plan revision 2; review fixes on 2026-11-08). Supersedes in part [decision 0006](0006-anonymous-projection-phase-1.md) (Q6, Q11: "no repository yet"; the ViewModel as the place of the projection) and [decision 0008](0008-report-form-state.md) (no factory, no coroutines, the caller creates the ticket). Diogo agreed to the changes to his files and to 0006.

## Context
- The ViewModels owned the data: `TicketsViewModel` held `TicketsList(SampleTickets.all)`, built tickets and called `toAnonymousView()`; `ReportViewModel` read `SampleCategories`. `.claude/rules/kotlin.md` wants the anonymous filter in the repository layer, and `kotlin-conventions` wants ViewModels on repository interfaces. 0006 and 0008 put the repository off until Firebase.
- F-007 copied gallery picks on the main thread (F-007 Gate 1 Q13, an accepted exception to `.claude/rules/kotlin.md`).
- At Gate 1 the person asked to follow the class project Contacts_#3 (P08): `ContactsApp`, `ContactsRepository`, `ContactsViewModel(+Factory)`, `EditContactViewModel(+Factory)`, `MainActivity`.

## Decision
- **Repository now, before Firebase.** `data/TicketsRepository` (interface) and `data/LocalTicketsRepository` (in memory). Suspend `loadPublicTickets()`, `loadStats()`, `getTicketById(id)` and `reportTicket(...)` on `Dispatchers.IO`; synchronous `activeCategories()` so the dropdown has its list on the first frame. The cache is a `TicketsList` seeded from `SampleTickets`. No JSON file (Contacts_#3 has one).
- **Projection in the repository.** `toAnonymousView()` is called only in `LocalTicketsRepository`. D-007's projection change lands there (or in `model/TicketPrivacy.kt`), not in a ViewModel.
- **Interface kept** (Contacts_#3 has none): the Firebase phase adds an implementation instead of rewiring the ViewModels.
- **`Mutex` over the cache:** the IO pool can run a load and a report at once, and `TicketsList` is not thread-safe. `getTicketById` is suspend and locked too (review S1; the plan had it synchronous like `getContactById`). It has no caller yet.
- **Manual DI as in Contacts_#3.** `CityFixApplication` (`android:name` in the manifest) holds `ticketsRepository` and `attachmentStorage` `by lazy`. Separate `TicketsViewModelFactory(repository)` and `ReportViewModelFactory(repository, attachmentStorage)` classes, built in `MainActivity` from `application as CityFixApplication` and passed as `CityFixApp(app)`. `create` uses `@Suppress("UNCHECKED_CAST") ... as T`, the Contacts_#3 shape (`modelClass.cast` gave a compiler warning).
- **ViewModels own their state.** `TicketsViewModel` keeps private `MutableStateFlow`s, loads in `viewModelScope` from `init` and `refresh()`, and has no `reportTicket` (public API change, accepted by the person and by Diogo).
- **Suspend save with a callback.** `ReportViewModel.submit(onSuccess: () -> Unit)` validates as before, sets `submitted` and `isSaving` before `launch` (so a double tap does nothing), and calls `repository.reportTicket`. A non-null id calls `onSuccess()` with no id (D-006 rule: a PRIVATE ticket is never opened); `null` unlocks with `report_error_not_created`. `MainActivity`'s callback is `refresh()` + snackbar + `navigateUp()`, like `saveContact { refreshContacts(); popBackStack() }`. `ReportSubmission` and `onSubmitResult` are gone.
- **Gallery copy off the main thread** (beyond Contacts_#3, which copies in the launcher callback). `data/AttachmentStorage` copies on `Dispatchers.IO` from `ReportViewModel.onMediaPicked` and deletes its copies on cancellation. `isCopyingAttachments` (a `copiesInProgress` counter) disables the add buttons and Submit until every copy ends; the limit of 3 is checked again when each copy returns (review B1). This removes F-007's Q13 exception for the gallery copy. Camera `length`/`delete` stay on the main thread (F-007 Q6).

### Rejected
- `viewModelFactory { initializer }` companions reading `APPLICATION_KEY` (plan revision 1): fine, but not the class pattern.
- Repository-owned `StateFlow`s (revision 1): Contacts_#3 has the ViewModel own them.
- Synchronous `reportTicket` (revision 1): Contacts_#3 saves with a suspend call and `isSaving`.
- Hilt or Koin: extra plugin and annotations for two ViewModels; harder to explain.
- Waiting for Firebase: the anonymous filter would stay outside the repository layer until then.

## Consequences
- **Tickets live as long as the process**, not the activity (Q4). Process death still restores the samples.
- **One empty frame at cold start:** the dashboard and list can show the zero/empty state before the first load lands; after process death on the detail route, "Ticket not found" shows for a frame (review N8). No loading state in phase 1.
- **Callback risks, accepted as in Contacts_#3:** rotation mid-save runs the old `onSuccess` on the old `NavController`, so the form stays open, locked, with no snackbar (review N7); back mid-save may skip the snackbar and `refresh()`. Fix in the Firebase task: a `saved` flag in `ReportUiState` consumed by a `LaunchedEffect`.
- `refresh()` loads tickets and stats under two lock acquisitions (review N2); harmless in memory, replaced by Firestore listeners later.
- `AttachmentStorage` is a concrete class (review N9); it can become an interface when the WorkManager upload task touches it.
- I.3.E "active only" now lives in the repository (`activeCategories()`); `submit()` still rejects an id outside the list.

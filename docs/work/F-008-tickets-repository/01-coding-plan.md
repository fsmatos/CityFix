# F-008: Coding plan (revision 2, Contacts_#3 pattern)

## Files
| Path | Change | Why |
| --- | --- | --- |
| `data/TicketsRepository.kt` | new | Interface (Firebase adds a second implementation) |
| `data/LocalTicketsRepository.kt` | new | Cache over `TicketsList` + samples; projection; creation |
| `data/AttachmentStorage.kt` | new (slice 4) | Suspend gallery copy on `Dispatchers.IO` |
| `CityFixApplication.kt` (package root) | new | One repository (and storage) per app, like `ContactsApp` |
| `app/src/main/AndroidManifest.xml` | `android:name=".CityFixApplication"` | Registers it |
| `viewmodel/TicketsViewModelFactory.kt` | new | Like `ContactsViewModelFactory` |
| `viewmodel/ReportViewModelFactory.kt` | new | Like `EditContactViewModelFactory` |
| `viewmodel/TicketsViewModel.kt` (Diogo) | own flows, `refresh()`, no `reportTicket` | Like `ContactsViewModel` |
| `viewmodel/ReportViewModel.kt` | repository + storage in constructor; `submit(onSuccess)`; `onMediaPicked` | Like `EditContactViewModel.saveContact` |
| `viewmodel/ReportUiState.kt` | `isSaving`, `isCopyingAttachments`; remove `ReportSubmission` | Async save and copy |
| `ui/screens/ReportScreen.kt` | `onSubmitted: () -> Unit`; launchers call `onMediaPicked` | New wiring |
| `MainActivity.kt` (Diogo) | `app`, two factories, `onSubmitted` | New wiring |
| `utils/FileUtils.kt` | KDoc only (slice 4) | "Blocking: call off the main thread" instead of Q13 |

Paths under `app/src/main/java/pt/isec/amov/cityfix/` unless shown.

## Interfaces
```kotlin
// data/TicketsRepository.kt
interface TicketsRepository {
    /** Public active tickets, newest first, projected with toAnonymousView(). */
    suspend fun loadPublicTickets(): List<Ticket>
    /** Over every ticket, private included (decision 0005). */
    suspend fun loadStats(): DashboardStats
    /** Projected visible ticket from the cache, or null. */
    fun getTicketById(id: String): Ticket?
    /** Today's contract; null when a trimmed text is blank, attachments is empty or add refuses. */
    suspend fun reportTicket(
        description: String, categoryId: String, categoryName: String,
        situation: Situation, scope: Scope, attachments: List<Attachment>,
    ): String?
    /** Active categories (I.3.E). Synchronous: in memory, needed for the first frame. */
    fun activeCategories(): List<Category>
}

// data/LocalTicketsRepository.kt
class LocalTicketsRepository(
    initialTickets: List<Ticket> = SampleTickets.all,
    private val categories: List<Category> = SampleCategories.all,
) : TicketsRepository {
    private val ticketsList = TicketsList(initialTickets)   // the cache
    private val mutex = Mutex()
    // suspend members: withContext(Dispatchers.IO) { mutex.withLock { ... } }
    companion object { const val LOCAL_USER_ID = "local-citizen" }
}

// CityFixApplication.kt
class CityFixApplication : Application() {
    val ticketsRepository: TicketsRepository by lazy { LocalTicketsRepository() }
    val attachmentStorage by lazy { AttachmentStorage(this) }   // slice 4
}

// viewmodel/TicketsViewModelFactory.kt
class TicketsViewModelFactory(private val repository: TicketsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        modelClass.cast(TicketsViewModel(repository))   // no unchecked cast
}
// ReportViewModelFactory(repository, attachmentStorage): same shape (storage from slice 4).

// viewmodel/TicketsViewModel.kt
class TicketsViewModel(private val repository: TicketsRepository) : ViewModel() {
    val tickets: StateFlow<List<Ticket>>        // private MutableStateFlow(emptyList())
    val stats: StateFlow<DashboardStats>        // private MutableStateFlow(DashboardStats())
    init { refresh() }
    fun refresh()                               // viewModelScope.launch { load both }
    fun getTicketById(id: String): Ticket?      // from tickets.value, as today
}

// viewmodel/ReportViewModel.kt
class ReportViewModel(
    private val repository: TicketsRepository,
    private val attachmentStorage: AttachmentStorage,   // slice 4
) : ViewModel() {
    fun submit(onSuccess: () -> Unit)
    fun onMediaPicked(uris: List<Uri>)                  // slice 4
}

// ui/screens/ReportScreen.kt
fun ReportScreen(viewModel: ReportViewModel, onSubmitted: () -> Unit, modifier: Modifier = Modifier)

// data/AttachmentStorage.kt (slice 4)
class AttachmentStorage(context: Context) {
    /** Copies on Dispatchers.IO; returns the paths that copied; deletes them if cancelled. */
    suspend fun copyToInternalStorage(uris: List<Uri>): List<String>
}
```

## Steps
1. **Repository.** Both files. Body from today's `TicketsViewModel` (`publicActive().map { it.toAnonymousView() }`, `stats()`, the `reportTicket` checks and ticket build). `getTicketById` = `ticketsList.publicActive().firstOrNull { it.id == id }?.toAnonymousView()`. Unused.
   Commit `refactor(F-008): add tickets repository`. Verify: build + lint.
2. **Application + ReportViewModel categories.** (Needs Diogo's OK for the `MainActivity` line and the manifest.) `CityFixApplication`, manifest attribute, `ReportViewModelFactory(repository)`; `ReportViewModel(repository)` with `categories = repository.activeCategories()`; `MainActivity`: `val app = application as CityFixApplication` and `viewModel(factory = ReportViewModelFactory(app.ticketsRepository))` inside `composable<ReportTicketRoute>`. Report flow unchanged.
   Commit `refactor(F-008): give ReportViewModel categories from the repository`. Verify: build + lint (no unchecked-cast warning); grep `SampleCategories` only in `data/`; emulator: 3 categories, no Graffiti; draft survives rotation, empty after leaving.
3. **Tickets through the repository, suspend save.** (Diogo's files.)
   - `TicketsViewModelFactory`; `TicketsViewModel` as above; remove `reportTicket`, `TicketsList`, `SampleTickets`, `toAnonymousView`, `UUID`.
   - `ReportUiState`: `isSaving: Boolean = false`; remove `ReportSubmission`. `ReportViewModel.submit(onSuccess)`: return if `submitted || isSaving`; same error build and `isValid` gate; set `submitted = true, isSaving = true`; `viewModelScope.launch { val id = repository.reportTicket(...named...); if (id != null) { isSaving = false; onSuccess() } else { submitted = false, isSaving = false, submitError = report_error_not_created } }`. Remove `onSubmitResult`.
   - `ReportScreen`: `onSubmitClick = { viewModel.submit(onSuccess = onSubmitted) }`; KDoc updated.
   - `MainActivity`: `viewModel(factory = TicketsViewModelFactory(app.ticketsRepository))`; `onSubmitted = { ticketsViewModel.refresh(); snackbar; navController.navigateUp() }`; keep the "never navigate to the id" comment.
   Commit `refactor(F-008): load tickets and save reports through the repository`. Verify: build + lint; greps `toAnonymousView`, `TicketsList(`, `SampleTickets` empty in `viewmodel/`; emulator: dashboard 3/2/1/3 days, list order, PUBLIC and PRIVATE report, double tap Submit, rotation on each screen.
4. **Gallery copy off the main thread.** `AttachmentStorage` (`withContext(Dispatchers.IO) { uris.mapNotNull { FileUtils.copyUriToInternalStorage(appContext, it) } }`, deleting copies on `CancellationException`, then rethrow); `CityFixApplication.attachmentStorage`; `ReportViewModelFactory(repository, attachmentStorage)`. `ReportUiState.isCopyingAttachments`; `canAddAttachment` also needs `!isCopyingAttachments`; `submit()` returns while copying; Submit `enabled = !submitted && !isCopyingAttachments`. `onMediaPicked(uris)`: `take(remainingAttachments)`, flag on, copy, `onPhotosAttached`, same notices as today's `attachPicked`, flag off. `ReportScreen`: launchers call `viewModel.onMediaPicked`; delete `attachPicked` and its Q13 comment. Camera path unchanged.
   Commit `refactor(F-008): copy picked media off the main thread`. Verify: build + lint; emulator: large video keeps the UI responsive, buttons disable; two quick picks never exceed 3; back mid-copy leaves no new file in `filesDir/images`.

## Conventions
- Contacts_#3 shapes: `Application` with `by lazy`, factory classes, `viewModelScope.launch`, `onSuccess` callback. Differences to explain at the defence: interface, `Mutex`, `modelClass.cast`, `@StringRes` errors, no JSON file.
- Plain code, KDoc on public members, no `!!`, no `GlobalScope`.
- Repository calls with named arguments (three `String`s).
- `viewModelScope`, `Dispatchers.IO`, `Mutex` come with lifecycle 2.11.0 / coroutines already on the classpath; no catalog change expected (check before adding one).
- No new strings; commit type `refactor` with `F-008`; no attribution lines.

## Do not touch
`model/` (D-007 changes `TicketPrivacy.kt`), other screens, `ShowScreen` previews, `Destinations.kt`, `MainTopAppBar.kt`, strings, version catalog, Firebase files and keys.

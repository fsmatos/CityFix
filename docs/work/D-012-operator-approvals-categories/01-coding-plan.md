# D-012 coding plan

Read `00-plan.md` first. Gate 1 answers (Q1-Q12) may change the parts marked "if Q...".

## Files
| Path | Change | Why |
| --- | --- | --- |
| `app/src/main/java/pt/isec/amov/cityfix/data/UsersRepository.kt` | edit | `pendingCitizens()`, `activateCitizen()`; interface KDoc "validation (D-012)" (slice 1) |
| `app/src/main/java/pt/isec/amov/cityfix/data/FirestoreUsersRepository.kt` | edit | Implementations; reuse private `toProfile()` (slice 1) |
| `app/src/main/java/pt/isec/amov/cityfix/viewmodel/PendingCitizensUiState.kt` | new | `citizens`, `isLoading` (slice 2) |
| `app/src/main/java/pt/isec/amov/cityfix/viewmodel/PendingCitizensViewModel.kt` | new | Collects `pendingCitizens()` (slice 2) |
| `app/src/main/java/pt/isec/amov/cityfix/viewmodel/PendingCitizensViewModelFactory.kt` | new | Plain factory (slice 2) |
| `app/src/main/java/pt/isec/amov/cityfix/ui/screens/PendingCitizensScreen.kt` | new | Stateful + stateless list + previews (slice 2) |
| `app/src/main/java/pt/isec/amov/cityfix/ui/navigation/Destinations.kt` | edit | `PendingCitizenRoute(citizenId)`; KDoc of `ApprovalsRoute` / `CategoriesRoute` no longer "placeholder" (slices 2, 3, 5) |
| `app/src/main/java/pt/isec/amov/cityfix/viewmodel/CitizenApprovalUiState.kt` | new | Slice 3 |
| `app/src/main/java/pt/isec/amov/cityfix/viewmodel/CitizenApprovalViewModel.kt` | new | `observeUser` + `activate` (slice 3) |
| `app/src/main/java/pt/isec/amov/cityfix/viewmodel/CitizenApprovalViewModelFactory.kt` | new | Takes `citizenId`, `operatorId` (slice 3) |
| `app/src/main/java/pt/isec/amov/cityfix/ui/screens/CitizenApprovalScreen.kt` | new | Detail, map, confirm dialog, portrait/landscape, previews (slice 3) |
| `app/src/main/java/pt/isec/amov/cityfix/ui/navigation/MainTopAppBar.kt` | edit | `TopBarScreen.PENDING_CITIZEN` + title (slice 3) |
| `app/src/main/java/pt/isec/amov/cityfix/data/CategoriesRepository.kt` | new | Interface (slice 4) |
| `app/src/main/java/pt/isec/amov/cityfix/data/FirestoreCategoriesRepository.kt` | new | `snapshots()` + not-awaited writes (slice 4) |
| `app/src/main/java/pt/isec/amov/cityfix/data/FirestoreMapping.kt` | edit (comment only, Francisco's) | `toCategoryDocument()` KDoc: "debug seed and CategoriesRepository" (slice 4, if Q1) |
| `app/src/main/java/pt/isec/amov/cityfix/CityFixApplication.kt` | edit | `categoriesRepository` `by lazy` (slice 4) |
| `app/src/main/java/pt/isec/amov/cityfix/viewmodel/CategoriesUiState.kt` | new | `CategoryEditor` + state (slice 5) |
| `app/src/main/java/pt/isec/amov/cityfix/viewmodel/CategoriesViewModel.kt` | new | Slice 5 |
| `app/src/main/java/pt/isec/amov/cityfix/viewmodel/CategoriesViewModelFactory.kt` | new | Slice 5 |
| `app/src/main/java/pt/isec/amov/cityfix/ui/screens/CategoriesScreen.kt` | new | List, switch rows, FAB, dialog, previews (slice 5) |
| `app/src/main/java/pt/isec/amov/cityfix/MainActivity.kt` | edit | Three destinations, `onActivated` snackbar, one `toTopBarScreen()` row (slices 2, 3, 5) |
| `app/src/main/res/values/strings.xml`, `app/src/main/res/values-pt/strings.xml` | edit | `approvals_*`, `citizen_approval_*`, `categories_*`, `top_bar_title_pending_citizen`, `cd_*` (slices 2, 3, 5) |
| `firestore.rules` | edit | `categories` block only, **only if Q10 = tighten**; never deployed (slice 6) |

## Interfaces

```kotlin
// data/UsersRepository.kt (additions)
interface UsersRepository {
    // ... existing observeUser, register, operatorEmailDomain, updateProfile, syncEmail

    /**
     * Citizens whose registration waits for validation (brief I.3.D), oldest first, live.
     * Operators only (the draft rules let only them list users). An error gives an empty list.
     */
    fun pendingCitizens(): Flow<List<Citizen>>

    /**
     * Activates [citizenId]: registrationStatus ACTIVE, validatedBy [operatorId], validatedAt now
     * (epoch ms). Only these three fields (draft validation rule). Not awaited: queued offline.
     */
    fun activateCitizen(citizenId: String, operatorId: String)
}

// data/CategoriesRepository.kt
/** The municipality's categories for the operator (brief I.3.E). The report form reads TicketsRepository.activeCategories(). */
interface CategoriesRepository {
    /** Every category, active and inactive, sorted by name with Collator, live. */
    fun allCategories(): Flow<List<Category>>
    /** New categories/{auto-id} with [name] (already trimmed and checked) and active = true. Not awaited. */
    fun addCategory(name: String)
    /** Changes the name only; tickets keep their categoryName snapshot (decision 0003). Not awaited. */
    fun renameCategory(id: String, name: String)
    /** Not awaited. */
    fun setActive(id: String, active: Boolean)
}

// ui/navigation/Destinations.kt
/** One pending citizen's registration (D-012). Only the uid: no personal data in the back stack. */
@Serializable
data class PendingCitizenRoute(val citizenId: String)

// viewmodel
data class PendingCitizensUiState(
    val citizens: List<Citizen> = emptyList(),
    val isLoading: Boolean = true,
)

data class CitizenApprovalUiState(
    val citizen: Citizen? = null,
    val isLoading: Boolean = true,
    /** No users/{id} document, or not a citizen. */
    val notFound: Boolean = false,
    /** The Activate confirmation dialog is open (Q5). */
    val confirming: Boolean = false,
) {
    val canActivate: Boolean get() = citizen?.registrationStatus == RegistrationStatus.PENDING
}

data class CategoryEditor(
    /** null = a new category. */
    val id: String? = null,
    val name: String = "",
    @StringRes val error: Int? = null,
)

data class CategoriesUiState(
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
    /** The add/edit dialog, null when closed. */
    val editor: CategoryEditor? = null,
)

class PendingCitizensViewModel(users: UsersRepository) : ViewModel()            // uiState: StateFlow<PendingCitizensUiState>
class CitizenApprovalViewModel(citizenId: String, operatorId: String, users: UsersRepository) : ViewModel() {
    fun onActivateClick()
    fun onDismissConfirm()
    fun activate(onActivated: () -> Unit)
}
class CategoriesViewModel(repository: CategoriesRepository) : ViewModel() {
    fun onAddClick()
    fun onEditClick(category: Category)
    fun onEditorNameChange(name: String)
    fun onEditorDismiss()
    fun onEditorSave()
    fun onActiveChange(category: Category, active: Boolean)
}

// ui/screens
@Composable fun PendingCitizensScreen(viewModel: PendingCitizensViewModel, onCitizenClick: (String) -> Unit)
@Composable fun PendingCitizensContent(state: PendingCitizensUiState, onCitizenClick: (String) -> Unit, modifier: Modifier = Modifier)
@Composable fun CitizenApprovalScreen(viewModel: CitizenApprovalViewModel, onActivated: () -> Unit)
@Composable fun CitizenApprovalContent(state: CitizenApprovalUiState, onActivateClick: () -> Unit, onConfirm: () -> Unit, onDismissConfirm: () -> Unit, modifier: Modifier = Modifier)
@Composable fun CategoriesScreen(viewModel: CategoriesViewModel)
@Composable fun CategoriesContent(state: CategoriesUiState, onAddClick: () -> Unit, onEditClick: (Category) -> Unit, onActiveChange: (Category, Boolean) -> Unit, onEditorNameChange: (String) -> Unit, onEditorSave: () -> Unit, onEditorDismiss: () -> Unit, modifier: Modifier = Modifier)
```

`MainActivity` (shape only):

```kotlin
composable<ApprovalsRoute> {
    RequireAccess(role = role, allowed = setOf(AccessRole.OPERATOR), onDenied = goHome) {
        val vm: PendingCitizensViewModel = viewModel(factory = PendingCitizensViewModelFactory(app.usersRepository))
        PendingCitizensScreen(vm, onCitizenClick = { id -> navController.navigate(PendingCitizenRoute(id)) { launchSingleTop = true } })
    }
}
composable<PendingCitizenRoute> { entry ->
    RequireAccess(role = role, allowed = setOf(AccessRole.OPERATOR), onDenied = goHome) {
        val operatorId = session.ticketsUser?.id ?: return@RequireAccess
        val citizenId = entry.toRoute<PendingCitizenRoute>().citizenId
        val vm: CitizenApprovalViewModel = viewModel(factory = CitizenApprovalViewModelFactory(citizenId, operatorId, app.usersRepository))
        CitizenApprovalScreen(vm, onActivated = {
            snackbarScope.launch { snackbarHostState.showSnackbar(citizenActivatedMessage) }
            navController.navigateUp()
        })
    }
}
```

## Steps

### Slice 1: data, pending citizens
- `UsersRepository`: the two members above; class KDoc gains "operator validation (D-012)".
- `FirestoreUsersRepository`:
  - `pendingCitizens()`: `firestore.collection(USERS).whereEqualTo("registrationStatus", RegistrationStatus.PENDING.name).snapshots().map { snap -> snap.documents.sortedBy { it.getLong("createdAt") ?: 0L }.mapNotNull { (it.toProfile() as? UserProfile.Found)?.user as? Citizen } }.catch { Log.w(TAG, "pending citizens failed", it); emit(emptyList()) }`. Import `com.google.firebase.firestore.snapshots` already covers `Query` (same KTX file).
  - `activateCitizen()`: `update(mapOf("registrationStatus" to RegistrationStatus.ACTIVE.name, "validatedBy" to operatorId, "validatedAt" to System.currentTimeMillis()))` + `addOnFailureListener`.
  - A short comment on why one filter is enough (operators have no `registrationStatus`) and why `createdAt` sorts in the app (no composite index).
- Commit: `feat(D-012): list and activate pending citizens in UsersRepository`
- Verify: `./gradlew assembleDebug`, `./gradlew lint`.

### Slice 2: approvals list
- UiState, ViewModel (`init { viewModelScope.launch { users.pendingCitizens().collect { list -> _uiState.update { it.copy(citizens = list, isLoading = false) } } } }`), factory (plain `create(modelClass)`, as `TicketsViewModelFactory`).
- `PendingCitizensScreen` / `PendingCitizensContent`: loading (`CircularProgressIndicator` with `cd_loading`-style description), empty text `approvals_empty`, `LazyColumn(contentPadding = 16.dp, spacedBy(8.dp))`, `items(citizens, key = { it.id })`, `Card(onClick)` with name (`titleMedium`) and `approvals_nif` ("NIF %1$s"), `semantics(mergeDescendants = true)`. Previews: 2 citizens, empty, loading; EN, PT, landscape. Sample citizens in the preview have fake data (`123456789`, synthetic names), never real test data.
- `MainActivity`: replace the placeholder in `composable<ApprovalsRoute>`; `onCitizenClick = {}` until slice 3.
- Strings: `approvals_empty`, `approvals_nif`, `cd_approvals_loading`.
- Commit: `feat(D-012): add the pending registrations list`
- Verify: build, lint; emulator: operator menu -> Registration approvals lists a PENDING test citizen, not an ACTIVE one; empty state with none; rotation.

### Slice 3: citizen detail and activation
- `PendingCitizenRoute` in `Destinations.kt`; `TopBarScreen.PENDING_CITIZEN` + title `top_bar_title_pending_citizen` ("Registration" / "Registo"); `toTopBarScreen()` row.
- `CitizenApprovalViewModel`: collects `users.observeUser(citizenId)`: `Found` with a `Citizen` -> `citizen`, `isLoading = false`; `Found` operator or `Missing` -> `notFound = true`; `Unavailable` -> keep the last citizen, `isLoading = false` (offline with nothing cached shows not found). `activate(onActivated)`: returns unless `canActivate`; `confirming = false`; `users.activateCitizen(citizenId, operatorId)`; `onActivated()`.
- `CitizenApprovalScreen`: collects state, wires events. `CitizenApprovalContent`:
  - photo: Coil `AsyncImage` (circle, 96dp, `cd_citizen_photo`) when `citizen.photo != null`;
  - name as heading (`semantics { heading() }`), then label/value rows for NIF, e-mail, phone (`+351 ` prefix), each row `mergeDescendants`;
  - location: heading "Registered location", `TicketMap(citizen.georeferencing, exact = true, Modifier.fillMaxWidth().height(220.dp).clip(MaterialTheme.shapes.medium).semantics { contentDescription = cd_citizen_map })`, or `citizen_approval_no_location` for (0, 0). No coordinates as text (not needed; Q3 alternative if wanted);
  - Activate `Button` full width when `canActivate`, otherwise `citizen_approval_already_active`;
  - `AlertDialog` when `confirming`: title "Activate this registration?", text "<name> will be able to report issues.", Confirm "Activate", Dismiss "Cancel";
  - landscape: `Row` of two `weight(1f).fillMaxHeight().verticalScroll(...)` panes (details + button | map), orientation from `LocalConfiguration`, scroll states hoisted (F-005 S2 pattern);
  - not found: centred `citizen_approval_not_found`; loading: progress indicator.
  - Previews: pending with location, (0, 0), already active, not found, confirm dialog; EN, PT, landscape. `TicketMap` draws its placeholder in previews.
- `MainActivity`: `composable<PendingCitizenRoute>` as in Interfaces; `citizenActivatedMessage = stringResource(R.string.citizen_approval_activated)` next to the report messages; list's `onCitizenClick` navigates.
- Strings: `top_bar_title_pending_citizen`, `citizen_approval_nif`, `_email`, `_phone`, `_location_title`, `_no_location`, `_activate`, `_confirm_title`, `_confirm_text`, `_confirm`, `_cancel`, `_already_active`, `_not_found`, `_activated` ("Registration activated." / "Registo ativado."), `cd_citizen_photo`, `cd_citizen_map`.
- Commit: `feat(D-012): show a pending registration and activate it`
- Verify: build, lint; emulator: detail fields and map; Activate -> confirm -> snackbar, back to the list, citizen gone; console shows the three fields; **two devices:** the citizen's waiting screen switches to the dashboard with "+" and a report can be sent; rotation with the dialog open; offline Activate.

### Slice 4: data, categories
- `CategoriesRepository` (interface above), `FirestoreCategoriesRepository(firestore)`:
  - `allCategories()`: `firestore.collection(CATEGORIES).snapshots().map { snap -> val collator = Collator.getInstance(); snap.documents.map { it.toCategory() }.sortedWith { a, b -> collator.compare(a.name, b.name) } }.catch { Log.w(...); emit(emptyList()) }`;
  - `addCategory`: `collection(CATEGORIES).document().set(Category(name = name).toCategoryDocument())`;
  - `renameCategory`: `document(id).update("name", name)`; `setActive`: `document(id).update("active", active)`; all with `addOnFailureListener { Log.w(...) }`, none awaited.
- `CityFixApplication`: `val categoriesRepository: CategoriesRepository by lazy { FirestoreCategoriesRepository(firestore) }`.
- `FirestoreMapping.kt`: KDoc line of `toCategoryDocument()` only (if Q1). Tell Francisco.
- Commit: `feat(D-012): add CategoriesRepository over categories`
- Verify: build, lint.

### Slice 5: categories screen
- `CategoriesViewModel`:
  - `init` collects `allCategories()` into `categories`, `isLoading = false`;
  - `onAddClick` -> `editor = CategoryEditor()`; `onEditClick(c)` -> `CategoryEditor(c.id, c.name)`; `onEditorNameChange` updates the name and clears the error; `onEditorDismiss` -> `editor = null`;
  - `onEditorSave`: `val name = editor.name.trim()`; error `categories_error_blank` if blank, `categories_error_too_long` if `name.length > MAX_CATEGORY_NAME_LENGTH` (40), `categories_error_duplicate` if `categories.any { it.id != editor.id && it.name.trim().equals(name, ignoreCase = true) }`; otherwise `addCategory(name)` (id null) or `renameCategory(id, name)` when the name differs from the stored one, then `editor = null`;
  - `onActiveChange(c, active)` -> `repository.setActive(c.id, active)` (the listener updates the row).
- `CategoriesScreen` / `CategoriesContent`: as `00-plan.md`. The edit dialog is a private `CategoryEditorDialog(editor, ...)`; its text field gets `singleLine`, `KeyboardOptions(capitalization = Sentences, imeAction = Done)`, `keyboardActions = onDone { onEditorSave() }`, and requests focus when it opens. Title "New category" / "Edit category". Previews: list with active + inactive, empty, dialog new, dialog with error; EN, PT, landscape.
- `MainActivity`: replace the placeholder in `composable<CategoriesRoute>` (factory with `app.categoriesRepository`).
- Strings: `categories_add`, `categories_inactive`, `categories_active`, `categories_empty`, `categories_dialog_new`, `categories_dialog_edit`, `categories_name_label`, `categories_save`, `categories_cancel`, `categories_error_blank`, `categories_error_too_long` ("At most %1$d characters."), `categories_error_duplicate`, `cd_category_edit` ("Edit %1$s"), `cd_category_active` ("%1$s active").
- Commit: `feat(D-012): add the categories screen`
- Verify: build, lint; emulator: add, rename, toggle; validation errors; the report form (citizen account) no longer offers a deactivated one and shows a renamed one; an old ticket with that category keeps its name in list, card and detail; rotation with the dialog open and text typed; landscape.

### Slice 6 (only if Q10 = tighten): rules
- `firestore.rules`, `categories` block only: `allow read: if true; allow create, update: if isOperator() && request.resource.data.keys().hasOnly(['name', 'active']) && request.resource.data.name is string && request.resource.data.name.size() > 0 && request.resource.data.active is bool; allow delete: if false;` with a comment "D-012, draft, not deployed".
- Commit: `chore(D-012): tighten the draft categories rule`
- Verify: read-through only (no `firebase` command). **Never deploy.**

## Conventions
- MVVM as the rest of the app: private `MutableStateFlow` + `asStateFlow()`, `update { it.copy(...) }`, collection in `viewModelScope`; `collectAsStateWithLifecycle()` in the stateful screen; previews on the stateless `XxxContent`.
- Each ViewModel built by its own `ViewModelProvider.Factory` and created **inside** `RequireAccess(OPERATOR)` with `viewModel(factory = ...)`, so it is scoped to the back-stack entry and its listener ends with it.
- Firestore writes not awaited, failure logged with `Log.w` (F-011, D-011). Reads through `snapshots()` flows with `.catch`.
- `catch (e: Exception)` only after rethrowing `CancellationException` (none expected: no `await()` in D-012).
- No literal UI text; EN in `values/strings.xml`, PT in `values-pt/strings.xml` ("Registo", "Categoria", "ativar"; "ocorrência(s)" when tickets are mentioned). Category names are data and are not translated.
- 48dp targets via M3 components; switch rows `toggleable(role = Role.Switch)`; headings marked; errors as text plus `semantics { error(...) }`.
- Landscape: detail two panes (P07); lists single column, width capped and centred.
- No personal data in routes, logs or state files; preview data is synthetic. Never real test e-mails, phones or UIDs in git.
- Commits: Conventional Commits with `D-012`, no attribution lines.

## Do not touch
- `TicketsRepository.kt`, `FirestoreTicketsRepository.kt`, `ReportViewModel*.kt`, `ReportUiState.kt`, `ReportScreen.kt` (Francisco's; the form keeps `activeCategories()`). `FirestoreMapping.kt` only for the one KDoc line (Q1).
- `model/` (no `validatedBy`), `SessionViewModel.kt`, `Session.kt`, `RoleAccess.kt` (menu entries already exist), `TicketMap.kt` (used as is).
- `DebugSeed.kt`, `SampleCategories.kt`.
- `firestore.rules` except slice 6 with Diogo's OK; `storage.rules`; `google-services.json`; no `firebase deploy`.

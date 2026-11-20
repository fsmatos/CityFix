# D-012: Operator: citizen approval and category management
Type: feature · Task file: docs/tasks/phase-2/D-012-operator-approvals-categories.md
Labels: -

## Problem
- Brief I.3.D: the operator views a pending citizen's registration (name, georeferencing, NIF) and activates it. Brief I.3.E: the operator creates, activates, deactivates and edits categories, and "os munícipes só podem abrir pedidos de suporte sobre os tipos de problemas mantidos ativos pela autarquia" (quoted in decision 0008). The PDF could not be opened in this session (no PDF renderer), so the I.3.D wording comes from the task file.
- Today `ApprovalsRoute` and `CategoriesRoute` are `PlaceholderScreen`s inside `RequireAccess(OPERATOR)` (D-009, decision 0015), reached from the operator's overflow menu (`menuItemsFor`). The only way to activate a citizen is a hand edit in the console (overview "Setup", D-010 console step).
- `UsersRepository` reads one user (`observeUser`), registers (D-010) and edits the own profile (D-011). It cannot list pending citizens or validate one.
- Categories are only read: `TicketsRepository.activeCategories()` (Francisco, F-011) is a live flow over `categories where active == true`, used by the report form. They are written only by `DebugSeed` (`toCategoryDocument()`). There is no create, rename or (de)activate code.
- The draft `firestore.rules` already have what D-012 needs: operators read `users` (get and list), the validation update `changedKeys().hasOnly(['registrationStatus', 'validatedBy', 'validatedAt'])` with `validatedBy == request.auth.uid`, and `categories` write for `isOperator()`. They are **not deployed** (decision 0012 Q5 amended): the test-mode database accepts any write until 2026-12-10, so the guard is the app.

## Outcome
- **Registration approvals** (`ApprovalsRoute`, menu "Registration approvals", operators only):
  - a list of the citizens with `registrationStatus == PENDING`, oldest registration first (Q4), one card per citizen with name and NIF; an empty state ("No registrations are waiting."); live, so a citizen who registers or is activated elsewhere appears or disappears without a refresh;
  - tap -> **citizen detail** (new route `PendingCitizenRoute(citizenId)`, the uid only): name, NIF, e-mail, phone, photo if any (Q3), and the georeferencing on the exact `TicketMap` (marker, zoom 15); "Location not available" for the unset (0, 0);
  - **Activate** opens a confirmation dialog (Q5); on confirm the app writes `registrationStatus: "ACTIVE"`, `validatedBy` (the operator's uid) and `validatedAt` (epoch ms, Q6) in one `update`, not awaited (queued offline); a snackbar "Registration activated." and back to the list. No Reject (Q2).
  - On the citizen's device nothing new is needed: the session's `users/{uid}` listener turns `PENDING_CITIZEN` into `CITIZEN`, the redirect opens the dashboard, "+" appears and `TicketsViewModel.setUser` switches to the citizen's tickets (D-009 code, unchanged).
- **Categories** (`CategoriesRoute`, menu "Categories", operators only):
  - every category, active and inactive, sorted by name with `Collator`; each row shows the name, "Inactive" under it when off, an Edit button and an active `Switch` (the whole row part is `toggleable(role = Role.Switch)`);
  - a FAB "Add category" and Edit open one dialog with a name field; the name is trimmed, required, at most 40 characters and unique ignoring case (Q7); Save writes, Cancel closes;
  - writes are not awaited (queued offline, as F-011/D-011): new document `categories/{auto-id}` with `name` and `active: true`; rename writes `name` only; the switch writes `active` only. No delete (Q8).
  - The report form needs no change: its `activeCategories()` listener drops a deactivated category and shows a renamed one live. Old tickets keep their `categoryName` snapshot (decision 0003): no ticket, `publicTickets` or `ticketLocations` document is touched.
- Both screens work in portrait and landscape, EN + PT, 48dp targets, content descriptions.

## Acceptance checks
- [ ] Approvals lists exactly the PENDING citizens (an ACTIVE citizen and the operators are not listed); an empty database shows the empty state.
- [ ] The detail shows name, NIF, e-mail, phone, photo (when uploaded) and a map with a marker on the registered location; (0, 0) shows "Location not available".
- [ ] Activate -> confirm: `users/{uid}` gets `registrationStatus: "ACTIVE"`, `validatedBy` = operator uid and `validatedAt`; nothing else changes (console). The citizen leaves the list.
- [ ] **Activating a citizen lets them report at once (no reinstall):** with the citizen's app open on the waiting screen (second emulator or device), activation moves it to the dashboard with the "+" button within seconds, and a report can be submitted. (Fallback if only one device: the citizen's app open while the field is changed by the operator account on another device or, last resort, in the console; say which in the state log.)
- [ ] Offline operator: Activate queues the write and shows the snackbar; it reaches the server when online.
- [ ] Categories: add, rename and (de)activate each show at once in the list and in the console.
- [ ] **A deactivated category disappears from the report form, but old tickets still show its name:** a ticket created with category X still shows X in the list, card and detail after X is deactivated and after X is renamed; the form no longer offers X after deactivation and shows the new name after a rename.
- [ ] Validation: an empty name, a name over 40 characters and a duplicate (any case, inactive ones included) show an error in the dialog and write nothing.
- [ ] **A citizen cannot open these screens or write categories:** no menu entries for citizens (pending or active) or anonymous users; a back-stack leftover or deep link to `ApprovalsRoute`, `PendingCitizenRoute` or `CategoriesRoute` is sent home by `RequireAccess`; the ViewModels (and their `users` / `categories` listeners) are created only inside the guard; the only category writes are in `FirestoreCategoriesRepository`, called only from `CategoriesViewModel`. The draft rule `categories: allow write: if isOperator()` denies a citizen; **not enforced on the live database** (rules not deployed), stated in the docs and at the defence.
- [ ] Anonymous users never see a citizen's personal data: none of the three routes is reachable without the OPERATOR role, and the route carries only the uid.
- [ ] Rotation keeps the open dialog and its typed name (categories) and the confirmation dialog (detail); nothing is clipped in landscape on all three screens.
- [ ] EN and PT strings for every new text; `./gradlew assembleDebug` and `./gradlew lint` pass after each slice, new lint warnings listed in `state.md` (baseline 0 errors, 22 warnings).

## Approach

### Pending citizens: data (`data/UsersRepository.kt`, `data/FirestoreUsersRepository.kt`)
- `fun pendingCitizens(): Flow<List<Citizen>>`: `users.whereEqualTo("registrationStatus", "PENDING").snapshots()` (Firestore KTX `Query.snapshots()`, the same API `observeUser` already uses on a document), each document mapped with the existing private `toProfile()` and kept when it is a `Found` `Citizen`, sorted by the document's `createdAt` (oldest first; `createdAt` is a Firestore field, not in the model, so the sort happens in the repository). `.catch { emit(emptyList()) }` with a `Log.w`.
  - One equality filter only: operators have no `registrationStatus`, so it already selects citizens, and `toProfile()` drops anything that is not a citizen. No composite index needed (single-field index).
  - Alternative rejected: `whereEqualTo("role", "CITIZEN")` as well; it adds nothing and two filters on different fields are the only thing that could ask for an index.
- `fun activateCitizen(citizenId: String, operatorId: String)`: one `update(mapOf("registrationStatus" to "ACTIVE", "validatedBy" to operatorId, "validatedAt" to System.currentTimeMillis()))`, not awaited, `addOnFailureListener { Log.w(...) }`, as `updateProfile`. Exactly the three keys of the draft validation rule. Idempotent: a second activation by another operator just rewrites the three fields.
- No `validatedBy` in `model/Citizen` (overview: the model gains it only when the UI needs it; it does not).

### Pending citizens: screens
- **List**: `PendingCitizensViewModel(users)` collects `pendingCitizens()` into `PendingCitizensUiState(citizens, isLoading)` (private `MutableStateFlow` + `asStateFlow()`, as `TicketsViewModel`). `PendingCitizensScreen(viewModel, onCitizenClick)` / `PendingCitizensContent(state, onCitizenClick)`: `LazyColumn` with `key = { it.id }`, M3 `Card(onClick)` with name (title) and "NIF 123456789"; progress indicator while loading, empty-state text. Same layout in both orientations (as `ListScreen`).
- **Detail**: new route `@Serializable data class PendingCitizenRoute(val citizenId: String)` (the id only, like `TicketDetailRoute`). `CitizenApprovalViewModel(citizenId, operatorId, users)` collects `users.observeUser(citizenId)` (already exists) into `CitizenApprovalUiState(citizen: Citizen?, isLoading, notFound, confirming)`. Events `onActivateClick()` (opens the dialog), `onDismissConfirm()`, `activate(onActivated: () -> Unit)` (closes the dialog, calls `users.activateCitizen`, then `onActivated()`).
  - If the snapshot shows the citizen already ACTIVE (another operator), the button is replaced by "This registration is already active." `notFound` for `Missing` or an operator document.
  - `CitizenApprovalScreen` / `CitizenApprovalContent`: portrait one scrolling `Column` (photo, name heading, NIF, e-mail, phone, map 220dp, Activate full width); landscape a `Row` of two `weight(1f)` panes (details + button left, map right), the pattern of `ShowScreen` (P07). Fields are plain labelled `Text`s, not text fields (nothing is editable). The map is the F-012 `TicketMap(location, exact = true)`; one citizen per route, so its remembered camera is right.
  - `onActivated` in `CityFixApp` shows the snackbar on the shared `snackbarHostState` and calls `navigateUp()`, as the report's `onSubmitted`.
- Why a separate route and two ViewModels (not one `OperatorAdminViewModel` as the task suggests): the detail ViewModel reuses `observeUser` and is cleared with its entry; it is the same list -> detail pattern as tickets, which both authors already explain. Alternative considered: one route with a list/detail two-pane in landscape and an in-ViewModel selection; it needs a `BackHandler` and makes the top bar's Up arrow behave differently per orientation. Not worth it for a two-field screen.

### Categories: data (new `data/CategoriesRepository.kt`, `data/FirestoreCategoriesRepository.kt`)
- Interface (Diogo's new files, so Francisco's `TicketsRepository` stays as it is, Q1):
  - `fun allCategories(): Flow<List<Category>>`: every `categories` document via `collection(CATEGORIES).snapshots()`, mapped with the existing `internal toCategory()` (F-011's `FirestoreMapping.kt`, reused, not edited), sorted with `Collator`.
  - `fun addCategory(name: String)`: `collection(CATEGORIES).document().set(Category(name = name).toCategoryDocument())`, reusing F-011's writer; new ids are Firestore auto-ids.
  - `fun renameCategory(id: String, name: String)`: `update("name", name)`.
  - `fun setActive(id: String, active: Boolean)`: `update("active", active)`.
  - All writes not awaited, failure logged (as F-011, D-011), so they work offline and the listener shows them at once.
- `CityFixApplication.categoriesRepository` `by lazy { FirestoreCategoriesRepository(firestore) }`.
- The report form keeps reading `TicketsRepository.activeCategories()`. Two readers of one collection are fine; moving the form to the new repository would touch Francisco's `ReportViewModel`, its factory and `TicketsRepository` for no behaviour change (Q1).

### Categories: screen
- `CategoriesViewModel(repository)` + `CategoriesUiState(categories, isLoading, editor: CategoryEditor?)` with `data class CategoryEditor(val id: String?, val name: String, @StringRes val error: Int?)` (`id == null` = new). Events: `onAddClick`, `onEditClick(category)`, `onEditorNameChange`, `onEditorDismiss`, `onEditorSave`, `onActiveChange(category, active)`.
  - `onEditorSave` trims, checks the rules (Q7) against the current list (excluding the edited category itself), then `addCategory` or `renameCategory` (only when the name changed) and closes the dialog. Errors are `@StringRes` (P08 pattern).
  - The editor lives in the ViewModel state, so it survives rotation; it is lost on process death (Q9).
- `CategoriesScreen` / `CategoriesContent`: `Scaffold`-free content with a `Box` + `LazyColumn` (`key = { it.id }`, bottom padding so the last row clears the FAB) and an `ExtendedFloatingActionButton` "Add category" (P08's FAB). Row: name (`bodyLarge`) and "Inactive" (`bodySmall`) when off, an `IconButton` Edit (`cd_category_edit`, "Edit <name>"), and the switch part `Modifier.toggleable(value, role = Role.Switch)` with `Switch(onCheckedChange = null)` and the state description "Active"/"Inactive". `AlertDialog` with an `OutlinedTextField` (`isError`, `supportingText`, `semantics { error(...) }`), Save and Cancel. Same layout in landscape, content width capped at 600dp and centred so rows are not stretched.

### Navigation and shell (`MainActivity.kt`, `Destinations.kt`, `MainTopAppBar.kt`)
- `composable<ApprovalsRoute>`, `composable<PendingCitizenRoute>` and `composable<CategoriesRoute>`, each inside `RequireAccess(OPERATOR)`; the ViewModels created inside the guard with their factories; the operator id from `session.ticketsUser?.id` (operator) as for the report's creator id.
- New `TopBarScreen.PENDING_CITIZEN` with the title "Registration" / "Registo", mapped in `toTopBarScreen()`.
- `PlaceholderScreen` stays (still used by `OperatorPanelRoute`, D-013).

### Rules
- No edit proposed (Q10). The existing drafts already restrict the three writes to operators.

## Architecture impact
- `data/`: `UsersRepository` gains `pendingCitizens` and `activateCitizen`; new `CategoriesRepository` / `FirestoreCategoriesRepository`; `CityFixApplication.categoriesRepository`.
- `viewmodel/`: three route-scoped ViewModels with UiState and factory each (`PendingCitizens`, `CitizenApproval`, `Categories`), the plain `ViewModelProvider.Factory` (no `SavedStateHandle`, Q9).
- `ui/`: three screens, one route, one `TopBarScreen` value.
- No change to `model/`, `TicketsRepository`, `FirestoreTicketsRepository`, `ReportViewModel`, `SessionViewModel` or the rules.

## Failure modes
| Situation | Behaviour |
| --- | --- |
| Operator offline, Activate | Queued in the cache; the citizen disappears from this device's list at once; the snackbar shows; the citizen's device sees it once both are online |
| Operator offline, category edits | Queued; shown at once in the list; synced later. Two offline operators can create the same name (duplicate check is client-side) |
| Two operators activate the same citizen | Last write wins on the same three fields; harmless. The second sees "already active" if the snapshot arrives first |
| Citizen's device offline at activation | Stays on the waiting screen until it is online and the listener delivers the change |
| Category deactivated while a citizen has the form open with it selected | The listener removes it from `categories`; `isValid` fails and Submit shows "Choose a category." (F-006 rule, unchanged) |
| Category renamed while a citizen has the form open with it selected | The form keeps the old name in its state, so that new ticket stores the old name (Francisco's `ReportViewModel`; flag to him, not fixed here) |
| Category deactivated with active tickets | Tickets unchanged and still listed; no new report can choose it, so no new grouping on it |
| Pending citizen with no or (0, 0) georeferencing (hand-made document) | "Location not available", no map |
| Listener error (rules, if ever deployed, refuse) | Logged; list empty / last list kept |
| Missing Maps key | Grey tiles (F-012 limit) |
| Citizen registered but the photo not uploaded yet | No photo shown; the rest of the detail works |
| A rejected write (only if the rules are ever deployed) | Local value shows, then the listener reverts it; only logged |

## Risks and open questions

### Risks
- **R1 Rules not deployed.** "Only operators write categories / activate" is enforced only by the app (menu, `RequireAccess`, ViewModel scope). Say so at the defence; the draft rules show the server design.
- **R2 Two-device check.** The "report at once" acceptance needs the citizen's app open while the operator activates: two emulators (or one emulator + phone) with two test accounts and their SMS test numbers.
- **R3 Merge conflicts with Francisco** in `MainActivity.kt` (D-013 also replaces `OperatorPanelRoute`), both `strings.xml` files and the overview. Keep both sides.
- **R4 Rename snapshot inside an open form** (failure-mode table): a ticket submitted from a form opened before a rename keeps the old name. Matches decision 0003's idea (name at report time) but is not literally "the name at submit". Francisco's call.
- **R5 Duplicate names offline** (client-side check only).
- **R6 Brief wording.** I.3.D not read in the PDF this session; the fields (name, georeferencing, NIF) are the task file's. Check the PDF before the defence.

### Open questions (Gate 1)
1. **Q1 Categories repository.** Recommended: a new `CategoriesRepository` (Diogo's files) for the operator, reusing F-011's `toCategory()` / `toCategoryDocument()` and the `CATEGORIES` constant without editing them; the report form keeps `TicketsRepository.activeCategories()`. Only one comment changes in Francisco's file: the `toCategoryDocument()` KDoc "debug seed only" becomes "debug seed and CategoriesRepository" (tell Francisco). Alternative: add the write methods to `TicketsRepository` (Francisco's file, more coordination).
2. **Q2 Reject.** Recommended: **no Reject** (the brief asks only to activate; a REJECTED status would need a new enum value, a session state and a citizen screen). A wrong registration stays pending; deleting it is a console task.
3. **Q3 Detail fields.** Recommended: name, NIF and georeferencing (brief) **plus** e-mail, phone and photo: the operator needs contact data to check a doubtful registration, and D-011 keeps `users/{uid}.email` in step for this. Alternative: only the three brief fields.
4. **Q4 List order.** Recommended: oldest registration first (`createdAt`, a queue), sorted in the repository. Alternative: by name with `Collator`.
5. **Q5 Confirmation.** Recommended: a confirmation dialog before Activate (there is no undo in the app). Alternative: activate on tap with the snackbar only.
6. **Q6 `validatedAt` type.** Recommended: epoch ms `Long` from the device clock, like every other time in the app (decision 0003). Alternative: `FieldValue.serverTimestamp()` (a Timestamp, the only one of its type in `users`).
7. **Q7 Category name rules.** Recommended: trimmed, not blank, at most 40 characters, unique ignoring case among all categories (inactive too). Names are not translated (municipality data, as F-006 noted).
8. **Q8 Delete categories.** Recommended: **no delete** (the brief lists create/activate/deactivate/edit; deactivate is the safe "delete", and tickets keep the id).
9. **Q9 Process death.** Recommended: no `SavedStateHandle` (dialog drafts are short; rotation is covered by the ViewModel). Alternative: save the category editor (id + name) as D-010/D-011 do (one more factory overload).
10. **Q10 Rules.** Recommended: **no edit** to `firestore.rules` (the needed rules exist; nothing is deployed). Optional, needs Diogo's explicit OK: tighten `categories` to `keys().hasOnly(['name', 'active'])`, `name is string && name.size() > 0`, `active is bool`, and `delete: if false`. Never deployed either way.
11. **Q11 Top bar on the detail.** Recommended: title "Registration" / "Registo" and the normal operator menu. Fine?
12. **Q12 Pending citizens and the 10 km gate.** Not changed here (D-010 Gate Q12 kept pending citizens ungated). Confirm it stays.

## Out of scope
- Rejecting, deactivating or deleting citizens; editing a citizen's data; showing `validatedBy` in the UI.
- Deleting categories; category icons, ordering or translations.
- Moving the report form to `CategoriesRepository`; refreshing a selected category's name inside the open form (Francisco).
- Notifying the citizen of the activation (F-016 may add it; the screen change is immediate anyway).
- Deploying or (unless Q10) editing the rules; the operations panel (D-013).

## Slices
1. **Data: pending citizens.** `pendingCitizens()` and `activateCitizen()` in `UsersRepository` + `FirestoreUsersRepository`. Build green, no caller yet.
2. **Approvals list.** `PendingCitizens{UiState,ViewModel,ViewModelFactory}`, `PendingCitizensScreen`, wired into `composable<ApprovalsRoute>` (tap does nothing yet), strings EN/PT.
3. **Citizen detail and activation.** `PendingCitizenRoute`, `TopBarScreen.PENDING_CITIZEN`, `CitizenApproval{UiState,ViewModel,ViewModelFactory}`, `CitizenApprovalScreen` (map, confirm dialog), list tap navigates, snackbar + `navigateUp`, strings.
4. **Data: categories.** `CategoriesRepository`, `FirestoreCategoriesRepository`, `CityFixApplication.categoriesRepository`, the one KDoc line in `FirestoreMapping.kt` (Q1).
5. **Categories screen.** `Categories{UiState,ViewModel,ViewModelFactory}`, `CategoriesScreen` (list, switch, FAB, dialog, validation), wired into `composable<CategoriesRoute>`, strings.
6. **Rules (only if Q10 = tighten).** The `categories` block of the draft `firestore.rules`. Not deployed.

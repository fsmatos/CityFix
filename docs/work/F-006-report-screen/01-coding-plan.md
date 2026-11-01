Revision 2 (2026-11-01, follows "Dicas da aula P08 para o CityFix"). Replaces revision 1.
Revision 3 (2026-11-01): attachments are F-007, so the `ReportUiState` KDoc says where they go. The Submit button gets an optional `isValid` colour cue (Q8) and an optional FAB variant (Q6). The steps are otherwise unchanged.

## Files
Paths are relative to `app/src/main/`. The Kotlin package root is `java/pt/isec/amov/cityfix/`.

| Path | Change | Why | Slice |
| --- | --- | --- | --- |
| `java/.../data/SampleCategories.kt` | New: `object SampleCategories { val all: List<Category> }` | Phase-1 category source (doc open point (a), Q4) | 1 |
| `java/.../viewmodel/ReportUiState.kt` | New: `ReportUiState` (+ `isValid`), `ReportSubmission` | Form state in one class (doc idea 1) | 1 |
| `java/.../viewmodel/ReportViewModel.kt` | New: form-only ViewModel | Route-scoped state, validation, submit (doc idea 2) | 1 |
| `java/.../ui/screens/ReportScreen.kt` | New: `ReportScreen` (stateful), `ReportContent` (stateless), `scopeLabelRes`, private sections, previews | The form UI | 2-4 |
| `res/values/strings.xml`, `res/values-pt/strings.xml` | New `<!-- Report form -->` block after `<!-- Ticket detail -->` | EN + PT | 1-3 |

String keys (EN / PT; PT says "ocorrência"). Prefixes `report_`, `scope_` (Diogo uses `top_bar_`, `cd_`). Reused: `situation_*`, `ticket_media_title`.
- Slice 1 (referenced by the ViewModel): `report_error_category` Choose a category. / Escolha uma categoria.; `report_error_situation` Choose a situation. / Escolha uma situação.; `report_error_description` Describe the issue. / Descreva a ocorrência.; `report_error_scope` Choose public or private. / Escolha público ou privado.; `report_error_not_created` The report could not be submitted. Try again. / Não foi possível enviar a ocorrência. Tente novamente.
- Slice 2: `report_required_note` All fields are required. / Todos os campos são obrigatórios.; `report_category_label` Category / Categoria; `report_no_categories` No categories are available. / Não há categorias disponíveis.; `report_situation_title` Situation / Situação; `report_description_label` Description / Descrição; `report_scope_title` Visibility / Alcance; `scope_public` Public / Público; `scope_private` Private / Privado; `report_error_summary` Some required fields are missing. / Faltam preencher campos obrigatórios.; `report_submit` Submit report / Enviar ocorrência.
- Slice 3 (Q2 a): `report_media_pending` Adding a photo or video comes in a later version; it will then be required. / A adição de fotografias ou vídeos chega numa versão posterior; nessa altura será obrigatória.

## Interfaces
```kotlin
// data/SampleCategories.kt
/** Phase-1 categories (no Firebase). Ids match SampleTickets; "graffiti" is inactive to show the I.3.E rule. */
object SampleCategories {
    val all: List<Category> = listOf(
        Category(id = "pavement", name = "Pavement"),
        Category(id = "lighting", name = "Public lighting"),
        Category(id = "furniture", name = "Urban furniture"),
        Category(id = "graffiti", name = "Graffiti", active = false),
    )
}

// viewmodel/ReportUiState.kt
/**
 * Report form (brief I.2.E). Nothing is preselected: null / blank means "not chosen".
 * Errors are string resource ids, so the ViewModel holds no text and they are localised.
 * Attachments come in F-007 (P09): add `attachments` and `@StringRes attachmentsError` after
 * `scope`, and `attachments.isNotEmpty() && attachmentsError == null` to isValid
 * ("deve ser carregada uma imagem/vídeo"). Until then a report has no image/video (I.2.E gap).
 */
data class ReportUiState(
    /** Active categories only (I.3.E). */
    val categories: List<Category> = emptyList(),
    val categoryId: String = "",
    val categoryName: String = "",
    val situation: Situation? = null,
    val description: String = "",
    val scope: Scope? = null,
    @StringRes val categoryError: Int? = null,
    @StringRes val situationError: Int? = null,
    @StringRes val descriptionError: Int? = null,
    @StringRes val scopeError: Int? = null,
    /** Shown after a Submit with missing fields; TalkBack announces it. */
    val showErrorSummary: Boolean = false,
    /** Set when the caller could not create the ticket. */
    @StringRes val submitError: Int? = null,
    /** True after a valid submit; blocks a second one. */
    val submitted: Boolean = false,
) {
    val isValid: Boolean
        get() = categoryId.isNotBlank() && categories.any { it.id == categoryId } &&
            situation != null && scope != null && description.isNotBlank() &&
            categoryError == null && situationError == null &&
            descriptionError == null && scopeError == null
}

/** Validated values, ready for TicketsViewModel.reportTicket(...). Description is trimmed. */
data class ReportSubmission(
    val categoryId: String,
    val categoryName: String,
    val situation: Situation,
    val description: String,
    val scope: Scope,
)

// viewmodel/ReportViewModel.kt
/**
 * Form-only ViewModel for the report screen. Create it per route (viewModel() inside
 * composable<ReportTicketRoute>): it survives rotation and is cleared when the user leaves.
 * Does not create tickets: the caller does it with TicketsViewModel.reportTicket (D-006).
 */
class ReportViewModel : ViewModel() {
    val uiState: StateFlow<ReportUiState>
    fun onCategorySelected(category: Category)        // sets id + name, clears categoryError and submitError
    fun onSituationSelected(situation: Situation)     // clears situationError
    fun onDescriptionChange(text: String)             // descriptionError = if (text.isBlank()) R.string.report_error_description else null
    fun onScopeSelected(scope: Scope)                 // clears scopeError
    /** Sets the error of every missing field. Returns the data when valid and not yet submitted (then locks), else null. */
    fun submit(): ReportSubmission?
    /** Result of reportTicket: false unlocks and sets submitError = R.string.report_error_not_created. */
    fun onSubmitResult(created: Boolean)
}
// init: _uiState = MutableStateFlow(ReportUiState(categories = SampleCategories.all.filter { it.active }))

// ui/screens/ReportScreen.kt
fun scopeLabelRes(scope: Scope): Int = when (scope) {
    Scope.PUBLIC -> R.string.scope_public
    Scope.PRIVATE -> R.string.scope_private
}

/** Stateful half: collects [viewModel] and calls [onSubmit] with valid data; [onSubmit] returns true if the ticket was created. */
@Composable
fun ReportScreen(
    viewModel: ReportViewModel,
    onSubmit: (ReportSubmission) -> Boolean,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()   // collectAsStateWithLifecycle once D-006 adds lifecycle-runtime-compose
    ReportContent(
        state = state,
        onCategorySelected = viewModel::onCategorySelected,
        onSituationSelected = viewModel::onSituationSelected,
        onDescriptionChange = viewModel::onDescriptionChange,
        onScopeSelected = viewModel::onScopeSelected,
        onSubmitClick = { viewModel.submit()?.let { viewModel.onSubmitResult(onSubmit(it)) } },
        modifier = modifier,
    )
}

/** Stateless half: draws [state]; previews target this. */
@Composable
fun ReportContent(
    state: ReportUiState,
    onCategorySelected: (Category) -> Unit,
    onSituationSelected: (Situation) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onScopeSelected: (Scope) -> Unit,
    onSubmitClick: () -> Unit,
    modifier: Modifier = Modifier,
)

// private, same file; modifier is the first optional parameter and goes to the root only
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun CategoryDropdown(categories: List<Category>, selectedName: String, @StringRes error: Int?, onSelect: (Category) -> Unit, modifier: Modifier = Modifier)
@Composable private fun <T> RadioGroupSection(title: String, options: List<T>, selected: T?, label: @Composable (T) -> String, @StringRes error: Int?, onSelect: (T) -> Unit, modifier: Modifier = Modifier)
@Composable private fun DescriptionField(value: String, @StringRes error: Int?, onValueChange: (String) -> Unit, modifier: Modifier = Modifier)
@Composable private fun MediaPendingSection(modifier: Modifier = Modifier)   // slice 3
```

Caller (D-006, not part of this task):
```kotlin
composable<ReportTicketRoute> {
    val reportVm: ReportViewModel = viewModel()      // scoped to this back-stack entry
    ReportScreen(
        viewModel = reportVm,
        onSubmit = { s ->
            val id = vm.reportTicket(s.description, s.categoryId, s.categoryName, s.situation, s.scope)
            if (id != null) {
                // snackbar "submitted"; never navigate to id (a PRIVATE report is not in the anonymous list)
                navController.navigateUp()
            }
            id != null                               // false: the form stays and shows report_error_not_created
        },
        modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
    )
}
```

## Steps
1. **Form state and ViewModel.**
   - Strings: slice-1 keys in both files.
   - `data/SampleCategories.kt`, `viewmodel/ReportUiState.kt`, `viewmodel/ReportViewModel.kt` as above. `@StringRes` from `androidx.annotation` (already on the classpath through core-ktx). The ViewModel follows `TicketsViewModel`'s pattern (private `MutableStateFlow`, `asStateFlow()`, `update { it.copy(...) }`), with no coroutines and no Context.
   - `submit()`:
     - if `submitted`, return `null`;
     - compute the errors for every field (category missing or not in `categories`, situation null, description blank, scope null);
     - if any error: update the state with them plus `showErrorSummary = true`, and return `null`;
     - else set `submitted = true`, `showErrorSummary = false`, `submitError = null`, and return `ReportSubmission(..., description = description.trim(), ...)`.
     - Read `situation`/`scope` into locals so the null checks smart-cast (no `!!`).
   - `onSubmitResult(false)`: `submitted = false`, `submitError = R.string.report_error_not_created`. `onSubmitResult(true)`: no change (stays locked).
   - Any field event also clears `submitError`. `showErrorSummary` turns off once `isValid` is true, so recompute it after each event as `showErrorSummary && !isValid`.
   - Commit: `feat(F-006): add ReportViewModel and form state for the report screen`.
   - Verify:
     - assembleDebug passes; lint 0 errors, 13 warnings (the five strings are referenced, so no `UnusedResources`);
     - `ReportUiState.kt`/`ReportViewModel.kt` have no `import androidx.compose` and no `Context`;
     - `TicketsViewModel.kt` unchanged (`git diff --stat`);
     - by reading: `submit()` on a fresh state returns `null` and sets four errors.
2. **ReportScreen and ReportContent (portrait).**
   - Strings: slice-2 keys.
   - `ReportContent`: `val scroll = rememberScrollState()` (hoisted for slice 4), then `Column(modifier.fillMaxSize().verticalScroll(scroll).imePadding().padding(16.dp), spacedBy(16.dp))` with:
     - `report_required_note`;
     - `CategoryDropdown`, then `RadioGroupSection` for situation, then `DescriptionField`, then `RadioGroupSection` for scope;
     - if `showErrorSummary`, the summary `Text(colorScheme.error, Modifier.semantics { liveRegion = LiveRegionMode.Polite })`;
     - if `submitError != null`, its text (same style, also a live region);
     - `Button(onSubmitClick, enabled = !state.submitted, Modifier.fillMaxWidth())`. It is never `enabled = isValid`: P08 slide 20 keeps the Save FAB clickable, and slide 15 returns early when invalid; here Submit shows the errors instead.
     - Only if Gate 1 turns on the Q8 cue: `colors = ButtonDefaults.buttonColors(containerColor = if (state.isValid) primary else surfaceVariant, contentColor = if (state.isValid) onPrimary else onSurfaceVariant)`.
     - Only if Gate 1 picks the Q6 FAB:
       - wrap `ReportContent`'s root in a `Scaffold(floatingActionButton = { FloatingActionButton(onSubmitClick) { Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.cd_submit_report)) } }, contentWindowInsets = WindowInsets(0))`;
       - put an 80dp `Spacer` at the end of the scrolling content (slide 20), and remove the button;
       - add the string `cd_submit_report` Submit report / Enviar ocorrência;
       - skip the FAB while `submitted`.
   - `CategoryDropdown`:
     - `var expanded by rememberSaveable { mutableStateOf(false) }`;
     - `ExposedDropdownMenuBox(expanded, onExpandedChange = { if (categories.isNotEmpty()) expanded = it })`;
     - `OutlinedTextField(value = selectedName, onValueChange = {}, readOnly = true, enabled = categories.isNotEmpty(), label = report_category_label, trailingIcon = ExposedDropdownMenuDefaults.TrailingIcon(expanded), isError, supportingText = error text or (if empty list) report_no_categories, modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth())`. Confirm the anchor API name in material3 1.4; use the non-deprecated one;
     - `ExposedDropdownMenu(expanded, onDismissRequest = { expanded = false })` with `DropdownMenuItem(text = { Text(category.name) }, onClick = { onSelect(category); expanded = false })` per category.
   - `RadioGroupSection`: `Column(modifier.selectableGroup().semantics { if (error != null) error(errorText) })`:
     - title (`titleMedium`, `heading()`);
     - rows `Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected, onClick, role = Role.RadioButton))` with `RadioButton(selected, onClick = null)` + label;
     - the error `Text(bodySmall, colorScheme.error)` under the rows.
   - `DescriptionField`: `OutlinedTextField(value, onValueChange, label, isError = error != null, supportingText = error?.let { { Text(stringResource(it)) } }, minLines = 3, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), modifier = modifier.fillMaxWidth())`. M3 reads `supportingText` with the field; add `semantics { error(text) }` so TalkBack says the real message instead of the default "Invalid input".
   - `ReportScreen` as in Interfaces.
   - Preview data (private), built as `ReportUiState` values, no ViewModel:
     - `previewCategories` = the three active sample categories;
     - `emptyState`;
     - `filledState` (lighting, BREAKDOWN, a description, PUBLIC);
     - `errorState` (four errors, `showErrorSummary = true`);
     - `noCategoriesState`;
     - `submitFailedState` (filled + `submitError`).
   - Previews (`private`, `CityFixTheme`), portrait: empty, filled and error in EN and PT; no categories in EN and PT; submit failed in EN.
   - Commit: `feat(F-006): add ReportScreen with category dropdown, situation, description and scope`.
   - Verify:
     - build and lint at baseline (no new warning from the `@OptIn` or a deprecated anchor);
     - `ReportContent` has no `ReportViewModel` parameter;
     - `ReportScreen.kt` imports nothing from `data` or `androidx.navigation`;
     - by hand, the previews: order, nothing selected, error texts in EN and PT, disabled dropdown with the message;
     - by hand, the accessibility inspector: `error` semantics, headings, one node per radio row.
3. **Photos and videos note (Q2 a).**
   - String `report_media_pending`.
   - `MediaPendingSection`: `ticket_media_title` heading + the note (`bodyMedium`, `onSurfaceVariant`), nothing clickable, placed between description and scope (brief order). Not part of `isValid` (the KDoc on `ReportUiState` already says so). F-007 replaces this section with the picker/camera row and Coil previews, in the same place.
   - Commit: `feat(F-006): note pending photo and video attachments in ReportScreen`.
   - Verify: build and lint at baseline; the note is visible in the previews.
4. **Landscape.**
   - `isLandscape` from `LocalConfiguration`.
   - Hoist `leftScroll` and `rightScroll`; portrait uses `leftScroll`.
   - Landscape: `Row(modifier.fillMaxSize().padding(16.dp), spacedBy(24.dp))` with two `Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(...).imePadding(), spacedBy(16.dp))`:
     - left: required note, category, situation;
     - right: description, media note, scope, summary, submit error, Submit.
   - Use private `ChoicesPane` / `DetailsPane` composables, called by both orientations.
   - Previews: add the landscape `spec:` (EN) and landscape + `locale = "pt"` to the empty, filled and error previews.
   - Commit: `feat(F-006): split ReportScreen into two panes in landscape`.
   - Verify: build and lint at baseline; landscape previews show two panes, nothing clipped, Submit reachable.

Record each slice's outcome and any deviation in `state.md`.

## Conventions
- MVVM per P08 and `.claude/rules/kotlin.md`: immutable `ReportUiState` via `StateFlow`; events as ViewModel functions; stateful `ReportScreen(viewModel)` + stateless `ReportContent(state, on...)`.
- ViewModel: no Compose, no Context, no coroutines, no text (only `@StringRes` ids). No factory: no-arg constructor.
- The opt-in goes on the private composable (`@OptIn(ExperimentalMaterial3Api::class)`), as in `MainTopAppBar`.
- Exhaustive `when` (no `else`) for `Scope` and `Situation` labels; labels from resources, never `.name`.
- EN + PT strings in the same commit; `report_`/`scope_` prefixes.
- Theme colours and typography only; errors always as text, never colour alone.
- Accessibility: headings; `selectableGroup` + `selectable(role = RadioButton)` rows of at least 48dp; `error` semantics with the real message; polite live region for the summary and the submit error.
- `modifier: Modifier = Modifier` is the first optional parameter, applied once to the root.
- Previews: `private`, `CityFixTheme`, the shared landscape `spec:`, a PT landscape for each main state; previews use `ReportUiState` values, never a ViewModel.
- No new dependency; no manifest change.

## Do not touch
- `viewmodel/TicketsViewModel.kt`, `data/SampleTickets.kt`, all of `model/`, `ui/navigation/` (`MainTopAppBar.kt`, `Destinations.kt`). New files in `viewmodel/` and `data/` only as listed above (Gate 1 / Diogo's doc).
- Shared UI: `ui/components/*` (`situationLabelRes` imported, not edited), `ui/theme/`, the other screens, `MainActivity.kt`.
- Gradle files, `AndroidManifest.xml` (`adjustResize` is already there), `google-services.json`, keys, `local.properties`.
- `docs/tasks/`, other work folders.

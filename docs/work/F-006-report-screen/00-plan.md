# F-006: ReportScreen (provisional)
Type: feature · Task file: docs/tasks/phase-1/F-006-report-screen.md
Labels: none
Revision 2 (2026-11-01): Gate 1 asked the plan to follow the shared doc "Dicas da aula P08 para o CityFix" (Diogo, 2026-10-31, from class P08 ContactsApp) while still following the brief strictly. This replaces revision 1.
Revision 3 (2026-11-01): targeted changes after the alignment review against P08/P09 and the brief:
- attachments go to F-007 (the person's decision), with an "F-007 follow-up" section;
- Q8 and Q6 corrected against P08 slides 15 and 20;
- the quote disclaimer removed.
Everything else is revision 2.

## What changed in revision 3
- **Q2 decided:** attachments are task **F-007 "report attachments"**, right after F-006, following P09. F-006 keeps option (a), the visible note. `ReportUiState` is shaped so F-007 adds `attachments` without reshaping it. The I.2.E gap is closed by F-007. See "F-007 follow-up".
- **Q8 corrected:** P08 slide 20 keeps the Save FAB clickable and only changes its colour with `isValid`, and slide 15 returns early when `!isValid`. The default (always enabled, Submit shows every missing field's error) is therefore close to the slides. A colour cue is now an option.
- **Q6 extended:** weighs the P08 slide-20 FAB (check icon, screen-level `Scaffold`) against the full-width button. The default is still the button, with the reasons given.
- The brief quotes were checked with `pdftotext` by the main session.
- "Out of scope" moves attachments, camera and picker to F-007.

## What changed from revision 1
- Form state moves from `rememberSaveable` in the screen to **`ReportUiState` + `ReportViewModel` in `viewmodel/`** (P08 slides 4, 5, 25). These are two new files in Diogo's folder, proposed by his own doc. `TicketsViewModel` is not modified.
- Errors are **`@StringRes` fields in the state**, shown under each field with `isError` + `supportingText` **while the user edits** (slide 22), and set for untouched fields when Submit is tapped.
- Category uses **`ExposedDropdownMenuBox`** (slide 32), active categories only. Situation and scope stay radio groups (Q5).
- Category source decided (the doc's open point (a)): a new `data/SampleCategories.kt`, filtered to active ones by `ReportViewModel` (Q4).
- Submit: the ViewModel validates and returns the data; D-006 calls `TicketsViewModel.reportTicket(...)`; on `null` the screen **stays** and shows an error (Q3).
- `imePadding()` + `adjustResize` (slides 18, 20): `adjustResize` is **already** in `AndroidManifest.xml`, so there is no manifest change.
- Unchanged: stateless `ReportContent` for previews, landscape two panes, the media note and the I.2.E gap (Q2), no location, double-submit protection, a handover to D-006.

## Problem
Phase 1 needs the form behind `ReportTicketRoute` (D-004), reached from the top-bar "Report an issue" button (D-005). The task says "no attachments or Firebase yet". The brief says more:
- **I.2.E, Abertura:** "indicação da Categoria (definidas pelo município), Situação (Dano, Avaria, Destruição ou Roubo), descrição e anexos de imagens e/ou vídeos, alcance (público/privado). Os dados são de preenchimento obrigatório (deve ser carregada uma imagem/vídeo)."
- **I.3.E:** operators create, activate, deactivate and edit categories; "Os munícipes só podem abrir pedidos de suporte sobre os tipos de problemas mantidos ativos pela autarquia."
- **I.2.F:** impact detection suggests opening a pavement-anomaly ticket (a prefilled form, later).
- **I.2 Obs:** same type/situation within 10 m becomes a group ticket (Option A, decision 0002).
- **V:** Compose; English default + Portuguese by device; portrait and landscape "without loss of function/presentation"; accessibility; MVVM with stateless/stateful segmentation; runtime permissions handled dynamically.
- **II.1 Aberto:** the ticket was submitted successfully and registered.

(The main session checked these quotes against `docs/brief/AMov.ProjetoKotlin_PT.pdf` with `pdftotext`; I.2.E and I.3.E match.)

What exists:
- `Category(id, name, active = true)`, `Situation { DAMAGE, BREAKDOWN, DESTRUCTION, THEFT }`, `Scope { PUBLIC, PRIVATE }` (D-001; the scope enum is called `Scope`). `situationLabelRes` (F-002); no scope label.
- `TicketsViewModel.reportTicket(description, categoryId, categoryName, situation, scope): String?` (D-003). It has no form state and no category list. No `Category` instance exists in the code; sample tickets use ids `pavement`, `lighting`, `furniture` ("Pavement", "Public lighting", "Urban furniture").
- D-006 contract (overview): one shared `TicketsViewModel` above the `NavHost`; never navigate to the id `reportTicket` returns; confirm with a snackbar; handle `null`. D-006 adds `lifecycle-viewmodel-compose` and `lifecycle-runtime-compose`; neither is in the catalog yet.
- `MainActivity` calls `enableEdgeToEdge`; the manifest already has `android:windowSoftInputMode="adjustResize"` on `MainActivity`.
- Material3 comes from the Compose BOM `2026.02.01` (material3 1.4.x). `MainTopAppBar` already uses `@OptIn(ExperimentalMaterial3Api::class)` on the function: that is the project's opt-in convention.

## Outcome
- **`viewmodel/ReportUiState.kt`** (new): `ReportUiState` (categories, the four fields, one `@StringRes` error per field, a `submitError`, a `submitted` lock and a computed `isValid`) and `ReportSubmission` (the validated values).
- **`viewmodel/ReportViewModel.kt`** (new): form-only ViewModel, created per route by D-006, so it survives rotation and is cleared when the user leaves the screen. Events: `onCategorySelected`, `onSituationSelected`, `onDescriptionChange`, `onScopeSelected`, `submit(): ReportSubmission?`, `onSubmitResult(created: Boolean)`.
- **`data/SampleCategories.kt`** (new): the three sample categories reusing the ticket ids, plus one inactive category so the active-only rule is visible.
- **`ui/screens/ReportScreen.kt`** (new):
  - stateful `ReportScreen(viewModel: ReportViewModel, onSubmit: (ReportSubmission) -> Boolean, modifier)` collects the state and wires the events;
  - stateless `ReportContent(state, onCategorySelected, onSituationSelected, onDescriptionChange, onScopeSelected, onSubmitClick, modifier)`, which the previews target.
- Fields in the brief's order, all required, **nothing preselected** (Q9):
  - **Category**: read-only `ExposedDropdownMenuBox` of the active categories; disabled with "No categories are available." when the list is empty;
  - **Situation**: radio group over `Situation.entries`;
  - **Description**: multi-line `OutlinedTextField`;
  - **Photos and videos**: a non-interactive note that attachments come in a later version and will then be required (Q2; delivered by F-007);
  - **Visibility** ("Alcance"): radio group over `Scope.entries`;
  - a full-width **Submit report** button.
- **Errors**: under each field, as text (never colour only), from `@StringRes` ids so they are localised. The description error updates as the user types; the choice errors clear when a choice is made; Submit sets the error of every field still missing and shows a summary that TalkBack announces. If `reportTicket` returns `null`, the screen stays and shows "The report could not be submitted. Try again."
- **Landscape**: two panes (left: category, situation; right: description, media note, visibility, submit), each scrolling, with hoisted scroll states and `imePadding()`.
- **Previews** in EN/PT, portrait/landscape: empty, filled, error, no categories, submit failed.

## Acceptance checks
- [ ] Category, Situation, Description and Visibility are present in that order; situation and visibility options come from `Situation.entries` and `Scope.entries` with localised labels (task check 1, I.2.E).
- [ ] The dropdown lists only active categories: with `SampleCategories` it shows Pavement, Public lighting and Urban furniture, never the inactive one (I.3.E). With no categories the field is disabled and says so.
- [ ] Nothing is preselected. Tapping Submit on the empty form shows four field errors and the summary, and `onSubmit` is not called.
- [ ] Typing then deleting the description shows its error at once; choosing a category, situation or visibility clears that field's error.
- [ ] A whitespace-only description is invalid; the submission carries the trimmed text.
- [ ] When `onSubmit` returns `false`, the screen stays, shows the "could not be submitted" message and Submit works again. When it returns `true`, a second tap does nothing.
- [ ] The "Photos and videos" note is visible and has no button.
- [ ] Landscape previews (EN, PT) show two panes, nothing clipped, each scrolls (task check 2).
- [ ] Every field has a visible label; TalkBack reads the dropdown as a labelled field with its error, each radio option as one item with its state, the section titles as headings, and the error text (task check 3).
- [ ] On an emulator after D-006: rotating with a partly filled form keeps the values and errors (ViewModel); leaving and reopening the form starts empty (ViewModel scoped to the route); the keyboard does not cover the description in landscape.
- [ ] Code checks: `ReportUiState`/`ReportViewModel` import no `androidx.compose` and no `android.content.Context`; `ReportContent` takes no ViewModel; no hard-coded user-visible strings; `TicketsViewModel.kt` unchanged.
- [ ] `./gradlew assembleDebug` passes and `./gradlew lint` keeps the baseline (0 errors, 13 warnings) after every slice.

## Approach
**State in a ViewModel tied to the route (doc ideas 1, 2; Q1).**
- `ReportUiState` is immutable. The ViewModel exposes it as `StateFlow<ReportUiState>` (private `MutableStateFlow` + `asStateFlow()`, the same pattern as `TicketsViewModel`), and events come in as plain functions. No coroutines: everything is synchronous and in memory.
- D-006 creates it inside `composable<ReportTicketRoute>` with `viewModel()`, so it is scoped to that back-stack entry: it survives rotation and is cleared when the route is popped (P08 slides 4, 25). A no-arg constructor means the default factory works, so there is no factory and no repository yet (those are for the Firebase phase, per the doc).
- Process death loses the draft. This is accepted for phase 1, as for `TicketsViewModel`. The alternative is a `SavedStateHandle` constructor parameter, which the default factory also supports (Q1).
- Validation lives in the ViewModel (one owner):
  - `isValid` is computed from the fields and errors (doc idea 1);
  - each event sets its own field's error (doc idea 4);
  - `submit()` sets every missing field's error and returns a `ReportSubmission` only when `isValid` and not `submitted`.

**How the form reaches `reportTicket` without a repository or factory (Q3).** Default: `ReportViewModel` does not know `TicketsViewModel`.
- `ReportScreen` calls `viewModel.submit()`. If it returns data, the screen calls `onSubmit(submission)`, and D-006 implements that with `vm.reportTicket(...)`, returning `id != null`. The screen then passes the result to `viewModel.onSubmitResult(created)`.
- On `true`, D-006 shows a snackbar and navigates up, and the lock stays so a double tap is ignored. On `false`, the ViewModel unlocks and sets `submitError`, so the user stays on the form (doc: "If it returns null ... we must not navigate back").
- Rejected:
  - a ViewModel holding another ViewModel;
  - passing `reportTicket` into the constructor, which needs a factory;
  - moving form state into `TicketsViewModel`, which changes Diogo's file and mixes app-wide and per-screen state.

**Stateful/stateless split.** `ReportScreen(viewModel)` collects with `collectAsState()` (Compose runtime, no new dependency) and passes plain values and lambdas to `ReportContent`. `collectAsStateWithLifecycle` needs `lifecycle-runtime-compose`, which D-006 adds, and F-006 adding it too would make both branches edit the catalog (Q10). `ReportContent` is stateless apart from UI element state: the dropdown's `expanded` flag (`rememberSaveable`) and the two scroll states.

**Category dropdown (doc idea 3; Q5).**
- `ExposedDropdownMenuBox` with a read-only `OutlinedTextField` (label "Category", the selected name or empty, `ExposedDropdownMenuDefaults.TrailingIcon`, `isError` + `supportingText`) and an `ExposedDropdownMenu` with one `DropdownMenuItem` per category.
- It is `@ExperimentalMaterial3Api`, so the private composable carries `@OptIn(ExperimentalMaterial3Api::class)`, as `MainTopAppBar` does.
- In material3 1.4 the anchor modifier is `Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)`; the older `MenuAnchorType` is deprecated. Confirm at implementation: a deprecation warning would change the lint count.
- Category names are municipality data, shown as stored (not translated).

**Situation and scope as radio groups (Q5).** Four situations and two scopes, every option visible at once, each row a `selectable(role = Role.RadioButton)` of at least 48dp inside a `selectableGroup()`, with no experimental API. Rejected:
- a dropdown, which hides options and needs an extra tap;
- `SingleChoiceSegmentedButtonRow`: four PT labels ("Destruição") do not fit a portrait row, and a segmented row with no selection is unusual.

**Errors (doc idea 4).**
- All messages are `@StringRes Int?` in the state (doc difference 1), so they are localised and the ViewModel never holds text.
- Shown with `isError` + `supportingText` on the text fields, and an error `Text` under each radio group with `semantics { error(...) }` on the group.
- Timing:
  - description: recomputed on every change (blank after trim gives the error);
  - choices: cleared on selection;
  - on Submit every untouched field gets its error, so an untouched form shows nothing until Submit.
- A summary line above the button ("Some required fields are missing.") appears after a failed Submit, with a polite live region so TalkBack announces it. It is driven by `showErrorSummary` in the state.

**Submit button always enabled (Q8).** It is disabled only after a successful submit (the `submitted` lock). This is the P08 pattern:
- slide 20 keeps the Save FAB clickable and only changes its `containerColor` with `state.isValid` (`primary` vs `surfaceVariant`);
- slide 15's `saveContact` returns early when `!isValid`.

F-006 goes one step further: instead of a silent return, Submit sets every missing field's error and the announced summary. A disabled button would give no feedback, and TalkBack would read only "disabled".

**Keyboard (doc idea 5).** `adjustResize` is already set in the manifest. With `enableEdgeToEdge`, Compose still needs the IME inset applied, so the scrolling containers use `imePadding()`. D-006 should pass `Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)`, so the navigation-bar inset is not added twice when the keyboard opens (handover Q7).

**Landscape (P07).** As revision 1:
- orientation from `LocalConfiguration`;
- two `weight(1f)` panes written once as private composables and used by both orientations (F-005 pattern);
- the two `rememberScrollState()`s hoisted to the top of `ReportContent`, so the position survives rotation (F-005 review S2).

## Architecture impact
- New `viewmodel/ReportUiState.kt` and `viewmodel/ReportViewModel.kt`. They are in Diogo's folder but proposed by his doc; these are new files, and `TicketsViewModel.kt` is untouched. To be confirmed with Diogo (Q7).
- New `data/SampleCategories.kt` in Diogo's folder, as a new file, to close the doc's open point (a) (Q4).
- New `ui/screens/ReportScreen.kt`.
- Strings EN + PT in a new `<!-- Report form -->` block (`report_*`, `scope_*`). Prefixes avoid Diogo's `top_bar_` and `cd_`.
- No Gradle or manifest change.
- Overview gets a second ViewModel; ADR proposed (03-docs-plan).

## Failure modes
- **No active categories:** the field is disabled with "No categories are available."; Submit shows the category error; nothing is created (I.3.E).
- **Stale category id** (the list changes while the form is open; not possible with static samples): `submit()` checks that the id is in `categories` and sets the category error.
- **Whitespace-only description:** error.
- **Very long description:** no limit (Q11); the pane scrolls.
- **Double tap:** `submitted` makes the second `submit()` return `null`.
- **`reportTicket` returns `null`:** the form stays, unlocks and shows `report_error_not_created`; the user can retry.
- **Rotation:** ViewModel state survives; scroll and dropdown `expanded` are saveable.
- **Process death:** the draft is lost (Q1, accepted).
- **Leaving the screen:** the ViewModel is cleared, and the next visit starts empty. That is intended (doc idea 2).
- **Keyboard in landscape:** `imePadding()` + pane scroll. Possible double inset if D-006 does not consume the `Scaffold` padding (checked on the emulator).
- **Long PT labels / large font:** rows wrap, panes scroll; the dropdown field grows.
- **Anonymous users can open the form in phase 1:** only registered citizens may (I.2.E). This is an Auth-task requirement already in the overview.
- **Lint:** the `@OptIn` adds no warning; a deprecated anchor API would. `ModifierParameter` is avoided by parameter order.

## Risks and open questions
Defaults first; Gate 1 confirms or changes each.
- **Q1. ViewModel scope and persistence.** Default: `ReportViewModel()` with no arguments, created by D-006 with `viewModel()` inside `composable<ReportTicketRoute>`; survives rotation, not process death. Alternative: a `SavedStateHandle` parameter to survive process death too.
- **Q2. The attachment requirement (I.2.E "deve ser carregada uma imagem/vídeo"; doc open point (b)). Decided by the person (2026-11-01): separate task F-007.**
  - F-006 keeps (a): a visible "Photos and videos" note ("Adding a photo or video comes in a later version; it will then be required."), with no button and not part of `isValid`. The wording promises only what F-007 delivers: adding images/videos, plus the "at least one" rule in `isValid`.
  - `ReportUiState` is not given an `attachments` field now; its KDoc says where F-007 adds it and its `isValid` term. Adding an always-empty list now would be dead state that F-006 cannot show or test.
  - **Gap against I.2.E, closed by F-007:** between the F-006 and F-007 commits a report can be submitted with no image/video. It is recorded in `state.md` and the docs as open until F-007, next to decision 0006's consequence ("A phase-1 report has no attachment").
- **Q3. Submit path.** Default: `submit(): ReportSubmission?` in the ViewModel; `ReportScreen(viewModel, onSubmit: (ReportSubmission) -> Boolean)`; D-006 calls `reportTicket`, returns `id != null`, shows the snackbar and navigates up on success; `onSubmitResult(false)` unlocks and shows the error. The snackbar strings are D-006's. Alternatives: `ReportViewModel` takes a reporting function by constructor (needs a factory, Firebase phase); form state in `TicketsViewModel`.
- **Q4. Category source (doc open point (a)).** Default:
  - new `data/SampleCategories.all` with `Category("pavement", "Pavement")`, `Category("lighting", "Public lighting")`, `Category("furniture", "Urban furniture")` and `Category("graffiti", "Graffiti", active = false)`;
  - `ReportViewModel` puts `SampleCategories.all.filter { it.active }` in the state. It is the one owner of the active-only rule in phase 1; the Firebase phase moves it to a repository query.
  - Alternatives: D-006 passes the list; or `TicketsViewModel` exposes it (edits Diogo's file).
- **Q5. Controls.** Default: `ExposedDropdownMenuBox` for category (doc idea 3), radio groups for situation and scope. Alternative: segmented buttons for scope only (two options).
- **Q6. Submit placement.** Brief V asks for "ícones/barras de topo/botões flutuantes que sejam dinâmicos consoante o contexto". `MainTopAppBar` already hides the add button on `REPORT`, which is a context change, so the report screen has no top-bar or FAB action today. Options:
  - **(a) Default: a full-width "Submit report" button at the end of the form.**
    - It sits after the last field, in reading and TalkBack order, and carries a visible text label.
    - It never covers content.
    - In landscape it sits at the end of the right pane.
    - It needs no extra `Scaffold`.
  - **(b) P08 slide 20: a `FloatingActionButton` with a check icon in a screen-level `Scaffold`.** This is the class pattern and a direct match for "botões flutuantes ... dinâmicos". Its costs:
    - it floats over the form, so the scrolling content needs a bottom spacer (as slide 20 does) to keep the last field and its error visible;
    - an icon-only button needs a `contentDescription` (`cd_submit_report`) and has no visible text label;
    - the `Scaffold` is nested inside D-006's `Scaffold` (works, but two sets of insets to explain, and `imePadding` must be applied once);
    - in landscape the FAB sits over the right pane, the one with the description field, and over the keyboard area when it is open.
  - **(c) A top-bar check action** in `MainTopAppBar` (Diogo's file), which also needs the validation result hoisted to D-006.
  
  The default stays (a): the form is long, scrolls in both orientations and has a keyboard, and (a) keeps the label visible without covering a field. If the person wants the P08 FAB, (b) replaces the button in slice 2 and adds `cd_submit_report`. Either way the `isValid` colour cue (Q8) can be applied.
- **Q7. Handovers / coordination with Diogo.**
  - (a) Confirm F-006 may add the three new files in `viewmodel/` and `data/`.
  - (b) The D-006 call (see 01-coding-plan "Caller"), with `viewModel()` per route, `consumeWindowInsets(innerPadding)`, and never navigating to the returned id.
  - (c) The Auth task hides the add button for anonymous users (already in the overview).
  - (d) Impact detection (I.2.F) later prefills through a `ReportViewModel` event (e.g. `prefill(situation, categoryId)`), not added now.
  - (e) The `values-pt/strings.xml` merge with D-005 keeps both sides (the overview merge note).
  - (f) For F-007: `TicketsViewModel.reportTicket(...)` needs an attachments parameter (Diogo's file); agree it before F-007 starts.
- **Q8. Submit enabled state.** Default: always enabled except after a successful submit; tapping when invalid shows every missing field's error and the summary. This is close to P08: slide 20 keeps the Save FAB clickable, and slide 15 returns early when `!isValid`. F-006 adds the errors instead of a silent return.
  - Optional (default: off) visual cue from slide 20: `containerColor = if (state.isValid) primary else surfaceVariant` (with the matching content colour). It is only a hint, never the only signal, so the errors stay.
  - Rejected: `enabled = state.isValid`. Errors on untouched fields would never appear, and TalkBack would read only "disabled".
- **Q9. Preselection.** The doc's example preselects `Situation.DAMAGE` and `Scope.PRIVATE`. Default: **nothing preselected** (`Situation?`, `Scope?` = `null`), because I.2.E says "Os dados são de preenchimento obrigatório": a default value lets the user report without choosing, and a wrong situation breaks the 10 m grouping (same category + situation). Alternative: the doc's defaults. If so, keep `Scope.PRIVATE`, never PUBLIC: a citizen who does not choose is not published.
- **Q10. State collection.** Default: `collectAsState()` now; switch to `collectAsStateWithLifecycle()` when D-006 adds `lifecycle-runtime-compose` (one-line change, noted for D-006). Alternative: F-006 adds that dependency to the catalog (both branches then edit the catalog).
- **Q11. Description length.** Default: no limit (the brief sets none).
- **Q12. Scope wording.** Default: "Visibility" / "Alcance", options Public / Público, Private / Privado, no hints.
- **Q13. Location.** Not captured; a phase-1 ticket stays at (0, 0), so there is no 10 m grouping (decision 0006, Q8). The location task adds it with the runtime permission (brief V).

Other risks:
- The doc content comes from the caller; the brief quotes were checked against the PDF.
- The ExposedDropdownMenuBox anchor API name in material3 1.4 is checked at implementation.
- Rotation, the keyboard and TalkBack can be checked on a device only after D-006.

## Out of scope
- **F-007:** attachments (photo picker, camera with `TakePicture` + `FileProvider`, local copies, Coil previews, the "at least one image/video" rule). See "F-007 follow-up".
- Later phases: upload to Storage, thumbnails and compression (brief V, with WorkManager); location, map, 10 m grouping; runtime permissions (location).
- Firebase, repository in `Application`, ViewModel factories, `suspend`/`Dispatchers.IO`, `AlertDialog` (doc: Firebase phase).
- Not copied from P08 (doc): offline JSON queue, `PENDING_SYNC/SYNCED` (we have `UploadState`), upsert/JSON seed.
- Changing `TicketsViewModel`, `MainTopAppBar`, `MainActivity`, Gradle or the manifest.
- Hiding the form from anonymous users (Auth); impact-detection prefill (I.2.F); operator category management (I.3.E).

## F-007 follow-up (report attachments, P09)
Decided at Gate 1 (Q2): a separate task right after F-006, following class P09. P09 attaches media locally with no Firebase and no runtime permission. The person writes the task file in `docs/tasks/`; the list below is input for it, not part of F-006.
- **Picker:** `ActivityResultContracts.PickVisualMedia` / `PickMultipleVisualMedia(maxItems = 3)` (system photo picker, no permission). Use `PickVisualMedia.ImageAndVideo` because the brief says "imagens e/ou vídeos".
- **Camera:** `ActivityResultContracts.TakePicture` with a `FileProvider`:
  - a `<provider>` in `AndroidManifest.xml`;
  - `res/xml/file_paths.xml`;
  - no `CAMERA` permission while the system camera app is used through the intent. Confirm in F-007, since brief V asks for dynamic camera permission handling if the app ever declares it. Video capture (`CaptureVideo`) is F-007's call.
- **Local copies:** `utils/FileUtils.copyUriToInternalStorage(...)` into `context.filesDir/images` (P09). `utils/` is shared; the function stays plain and testable.
- **Previews:** Coil 3 `AsyncImage`; add `coil-compose` + `coil-network-okhttp` to `gradle/libs.versions.toml` and `app/build.gradle.kts`.
- **Form state:**
  - `ReportUiState` gains `attachments: List<String>` (local paths) or `List<Attachment>`, plus an error;
  - `isValid` gains `attachments.isNotEmpty()` (I.2.E "deve ser carregada uma imagem/vídeo");
  - `ReportViewModel` gains `onPhotosAttached(paths)` / remove (P09 "FixIt Project Bridge 1/3");
  - `ReportSubmission` gains the attachments;
  - the F-006 note becomes the attachments section.
- **Creating the ticket:** `TicketsViewModel.reportTicket(...)` needs an attachments parameter to fill `Ticket.attachments` (`Attachment.file` is already documented as "local URI before upload"; `uploadState = PENDING`). That is Diogo's file, so coordinate with him (or a D task). D-006's `onSubmit` passes it through.
- **Not in F-007:** thumbnails, compression and EXIF stripping (brief V, overview "Media is outside the projection"), and the upload to Storage with WorkManager, all in the Firebase/upload phase. Not copied from P09: slide 28's offline JSON queue and `PENDING_SYNC` (we have `UploadState`).
- After F-007, the I.2.E gap recorded by F-006 is closed, and decision 0006's consequence ("A phase-1 report has no attachment") no longer holds (Diogo's ADR to update).

## Slices
Each slice is one commit and leaves the build green (assembleDebug + lint baseline).
1. **Form state and ViewModel.** `data/SampleCategories.kt`; `viewmodel/ReportUiState.kt` (`ReportUiState` with `isValid`, `ReportSubmission`); `viewmodel/ReportViewModel.kt` (events, per-field `@StringRes` errors, `submit()`, `onSubmitResult()`, double-submit lock); the error strings it references (EN + PT). No UI yet.
2. **ReportScreen and ReportContent (portrait).** Stateful `ReportScreen(viewModel, onSubmit, modifier)`; stateless `ReportContent`: required note, category dropdown, situation radio group, description field, visibility radio group, field errors, summary, submit-failed message, Submit button; `scopeLabelRes`; field strings; portrait previews (EN, PT): empty, filled, error, no categories, submit failed.
3. **Photos and videos note** (Q2 a). Non-interactive section between description and visibility; string; previews show it. Dropped if Q2 is (b).
4. **Landscape.** Two panes with hoisted scroll states and `imePadding()` (also on the portrait column); landscape previews (EN, PT) for empty, filled and error.

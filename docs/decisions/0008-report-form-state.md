# 0008: Report form state

Status: accepted (F-006, Gate 1 on 2026-11-01, plan revision 3; S2 review fix on 2026-11-01). Related: [decision 0006](0006-anonymous-projection-phase-1.md) (its consequence "A phase-1 report has no attachment" stops holding after F-007; Diogo's record, not edited here).

## Context
- **Brief I.2.E, Abertura:** "indicação da Categoria (definidas pelo município), Situação (Dano, Avaria, Destruição ou Roubo), descrição e anexos de imagens e/ou vídeos, alcance (público/privado). Os dados são de preenchimento obrigatório (deve ser carregada uma imagem/vídeo)."
- **Brief I.3.E:** "Os munícipes só podem abrir pedidos de suporte sobre os tipos de problemas mantidos ativos pela autarquia."
- **Brief V:** Compose, portrait and landscape "without loss of function", accessibility, EN + PT, MVVM with stateless/stateful segmentation.
- Phase 1 has no Firebase and no repository. `TicketsViewModel` (D-003) is app-wide, created once above the `NavHost`, and offers `reportTicket(description, categoryId, categoryName, situation, scope): String?`, but no form state and no category list. No `Category` instance existed in the code.
- Revision 1 of the F-006 plan kept the form in the screen with `rememberSaveable`. At Gate 1 the person asked the plan to follow Diogo's shared doc "Dicas da aula P08 para o CityFix" (2026-10-31), based on class P08 (ContactsApp): a `UiState` with a computed `isValid`, a ViewModel tied to the route, errors under each field while typing, `ExposedDropdownMenuBox` for the category, `imePadding()` + `adjustResize`. The doc left two points open: (a) where categories come from in phase 1; (b) how `isValid` changes when attachments arrive (I.2.E).
- An alignment review against P08, P09 and the brief showed that P09 (photo picker, camera, Coil) makes attachments reachable without Firebase. The person moved them to a separate task, F-007, right after F-006.

## Decision
- **Form state in a route-scoped ViewModel (P08 slides 4, 5, 25).**
  - `viewmodel/ReportUiState.kt`: an immutable `ReportUiState` with the four fields, one `@StringRes Int?` error per field, `submitError`, `showErrorSummary`, a `submitted` lock and a computed `isValid`; and `ReportSubmission`, the validated, non-null values.
  - `viewmodel/ReportViewModel.kt`: a no-arg `ReportViewModel` exposing `StateFlow<ReportUiState>`, with events as plain functions and no coroutines.
  - D-006 creates it with `viewModel()` inside `composable<ReportTicketRoute>`. It survives rotation, is cleared when the user leaves, and is lost on process death (Gate 1 Q1).
- **Errors are string resource ids**, so the ViewModel holds no text and the messages are localised. The description error is recomputed while typing, a choice clears its own error, and Submit sets the error of every missing field.
- **`isValid` is the one gate** (P08 slide 15; review S2). `submit()` builds the state with every field's error and returns a `ReportSubmission` only if that state `isValid` and the form is not already `submitted`. F-007 therefore adds the attachment rule in `isValid` (and sets its error in `submit()`), and cannot be bypassed.
- **The ViewModel does not create the ticket (Gate 1 Q3).** `submit()` returns the `ReportSubmission`; the caller (D-006) calls `TicketsViewModel.reportTicket(...)` and returns `id != null` through `onSubmit: (ReportSubmission) -> Boolean`; the screen passes it to `onSubmitResult(created)`. `true` keeps the lock (double taps ignored) and D-006 navigates up with a snackbar; `false` unlocks and shows "The report could not be submitted. Try again.", and the form stays (the doc: "If it returns null ... we must not navigate back"). The caller never navigates to the returned id (decision 0006).
- **Nothing preselected (Gate 1 Q9).** `situation` and `scope` are nullable and start `null`; category and description start blank. Diogo's doc preselects `DAMAGE`/`PRIVATE`; this departs from it because I.2.E makes the data mandatory.
- **Categories from `data/SampleCategories` (Gate 1 Q4; the doc's open point (a)).** Three active categories reusing the sample-ticket ids (`pavement`, `lighting`, `furniture`) plus an inactive `graffiti`, so the rule is visible. `ReportViewModel` keeps only the active ones (I.3.E), and `submit()` rejects an id outside that list.
- **Stateful/stateless split:** `ReportScreen(viewModel, onSubmit, modifier)` collects the state; `ReportContent(state, on...)` is stateless and previewable.
- **Attachments are F-007 (Gate 1 Q2; the doc's open point (b)).** F-006 shows a non-interactive "Photos and videos" note and adds no `attachments` field (it would be dead state). The `ReportUiState` KDoc says where F-007 adds `attachments`, `attachmentsError` and the `isValid` term.

### Rejected
- **`rememberSaveable` in the screen** (plan revision 1). It survives rotation and process death with no ViewModel, but needs a custom `Saver` for the form, puts validation in a composable, and does not follow the class pattern or brief V's stateful/stateless segmentation as clearly. Replaced at Gate 1.
- **Form state in `TicketsViewModel`.** It changes Diogo's file and mixes app-wide data with one screen's draft. Because that ViewModel lives above the `NavHost`, a half-filled draft would also outlive the screen and reappear on the next visit.
- **`ReportViewModel` holding `TicketsViewModel`.** ViewModels should not reference each other; their lifetimes differ, and it couples the form to the in-memory list.
- **A reporting function or repository passed to the `ReportViewModel` constructor** (the slides save through a repository). It needs a `ViewModelProvider.Factory`, which belongs to the Firebase phase (the doc's "later" list).
- **Preselected `DAMAGE`/`PRIVATE`** (the doc's example). A user could report without choosing, and a wrong situation breaks the 10 m grouping (same category + situation). If defaults are ever wanted, keep `PRIVATE`, never `PUBLIC`: a citizen who does not choose is not published.
- **`enabled = isValid` on Submit.** Errors on untouched fields would never appear, and TalkBack would read only "disabled". P08 slide 20 also keeps its Save FAB clickable (only its colour changes with `isValid`). Submit stays enabled until a successful submit and shows the errors instead; the optional colour cue was left off (Gate 1 Q8).
- **`submit()` with its own copy of the rules** (the first implementation). F-007 could add the attachment rule to `isValid` only and a report without media would still pass (review S2).
- **The P08 slide-20 FAB or a top-bar check action for Submit** (Gate 1 Q6). A full-width button at the end of the form keeps a visible label in reading order, never covers a field (the landscape right pane holds the description), and needs no nested `Scaffold` or extra `contentDescription`.
- **`SavedStateHandle`** to survive process death (Q1): possible with the default factory, but more code for little gain in phase 1.

## Consequences
- **Gap against I.2.E until F-007.** Between the F-006 and F-007 commits a report can be submitted with no image or video. F-007 closes it: picker + camera (P09), local copies in `filesDir`, Coil previews, `attachments` + `attachmentsError` in `ReportUiState`, the `isValid` term, `ReportSubmission.attachments`, and an attachments parameter on `TicketsViewModel.reportTicket` (Diogo's file, to agree with him). After F-007, decision 0006's consequence "A phase-1 report has no attachment" no longer holds; Diogo should update 0006 (handover, not edited here).
- **The draft is lost on process death**, and leaving the screen discards it on purpose.
- **Two ViewModels in phase 1:** `TicketsViewModel` above the `NavHost`, `ReportViewModel` per route. D-006 must create each at the right level; the overview's D-006 contract shows both.
- **The Firebase phase** replaces `SampleCategories` with a repository query (active categories of the municipality) injected through a factory, and may move ticket creation into the repository. The UI state, events and screen stay the same.
- **Later features hook into the ViewModel:** impact detection (I.2.F) prefills through a new event; the location task adds coordinates and the 10 m grouping.
- Anonymous users can open the form until the Auth task hides the add button by role (overview, "Top bar (D-005)").

## Note from F-007 (2026-11-06)
The form-state decisions above stand unchanged. Two things are added:
- **The I.2.E gap is closed in code.** `isValid` and `reportTicket` both refuse a report with no attachment. Build, lint and static checks pass; nothing has run on a device or emulator yet.
- **Accepted exception to `.claude/rules/kotlin.md` line 15 (F-007 Gate 1, Q13 and Q6).** The person chose literal P09: the attachment copy runs synchronously on the main thread, in the launcher callback (slide 26), and so do `delete`, `mkdirs` and `length`. Known risk: a large video can freeze the UI or cause an ANR, with no upper bound, because there is no video size limit (Q7). Moving the copy off the main thread is an improvement opportunity in `docs/report/notes.md`.

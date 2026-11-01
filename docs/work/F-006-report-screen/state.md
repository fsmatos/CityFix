# F-006: ReportScreen (provisional)
Branch: dev/Francisco · Base: f2714b5
Remote: not pushed

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-01 | |
| Plan | done | 2026-11-01 | revision 3 (P08 doc + P09 review; attachments to F-007): ReportUiState + route-scoped ReportViewModel in viewmodel/, SampleCategories in data/, ExposedDropdownMenuBox + radio groups, live @StringRes errors, submit returns ReportSubmission (D-006 calls reportTicket, stays on null); media note (I.2.E gap); landscape two panes; 4 slices; Q1-Q13 |
| Gate 1: plan approved | done | 2026-11-01 | approved revision 3 with all defaults (full-width Submit, nothing preselected, media note, F-007 for attachments) |
| Code | pending | 2026-11-01 | |
| Review | | | |
| Docs | | | |
| Pushed | | | |
| Merged | | | |

## Log
Append-only.
- 2026-11-01: fetched origin; origin/main had no commits missing from dev/Francisco. The person chose not to merge (already up to date). Started F-006 from `docs/tasks/phase-1/F-006-report-screen.md`; dependencies D-001 and F-001 are in the branch.
- 2026-11-01: Plan written (00-plan, 01-coding-plan, 03-docs-plan).
  - Read: the task file, the phase-1 index, the overview, decisions 0002, 0006, 0007, `Ticket.kt`, `Category.kt`, `Municipality.kt`, `Situation.kt`, `Scope.kt`, `Attachment.kt`, `TicketsViewModel.kt`, `SampleTickets.kt`, `Destinations.kt`, `MainTopAppBar.kt` (REPORT hides the add button, no submit action), `TicketCard.kt`, `DashboardScreen.kt`, both string files, the D-006 task file, the version catalog, and F-005's 00-plan, 01-coding-plan, state and review.
  - Brief quotes (I.2.E Abertura, I.3.E active categories, I.2.F, I.2 Obs, V, II.1) taken from the caller. This planner session had no shell, so it could not run `pdftotext` itself; the person should check the quotes.
  - Findings: `TicketsViewModel` has `reportTicket(description, categoryId, categoryName, situation, scope)` but no form state and no category list; no `Category` instance exists in the code (sample ids `pavement`, `lighting`, `furniture` only on tickets).
  - Approach: stateful `ReportScreen(categories, onSubmit, modifier)` with `rememberSaveable` (`ReportForm` + `listSaver`, `showErrors`, `submitted`) over a stateless `ReportContent`; category, situation and scope as accessible radio groups, description as `OutlinedTextField`; all required, nothing preselected; pure `reportErrors` validation; per-field error text + `error` semantics + polite live-region summary after the first submit attempt; one-shot submit; non-interactive "Photos and videos" note; landscape two panes with hoisted scroll states and `imePadding()`.
  - Slices: 1. fields + state + validation-gated submit; 2. error state + no-categories; 3. media note; 4. landscape.
  - Known gap recorded: brief I.2.E requires an image/video; phase 1 cannot attach one, so a report can be submitted without it (Q2; also decision 0006's consequence).
  - Open questions with defaults: Q1 state in the screen (rememberSaveable), not a ViewModel; Q2 visible media note, no button; Q3 `onSubmit` callback only, one-shot, D-006 reports/snackbar/navigates up; Q4 caller passes active categories, screen does not filter; Q5 radio group, not dropdown; Q6 submit button in the form, not the top bar; Q7 handovers to Diogo (D-006 call, category source + active-only flow, later Auth and I.2.F prefill); Q8 no description limit; Q9 "Visibility"/"Alcance" with no hints; Q10 no location in this task.
  - Gate 1 pending.
- 2026-11-01: Gate 1: revise. The person asked the plan to follow the shared doc "Dicas da aula P08 para o CityFix" (2026-10-31, claude.ai doc). It asks for: `ReportUiState` in `viewmodel/` with fields, `@StringRes` errors and a computed `isValid`; a `ReportViewModel` for the form, tied to the route (survives rotation, dies on leaving); category picked with `ExposedDropdownMenuBox` from active categories only; errors with `isError` + `supportingText` under the field while typing; `imePadding()` + `adjustResize` so the keyboard does not cover the form; `ReportScreen(viewModel)` stateful + stateless `ReportContent(state, onEvent...)` for previews; submit validates and calls `reportTicket(...)`, and does not navigate back if it returns `null`. Its open points: phase-1 category source; `isValid` must change when attachments arrive (I.2.E). Notes sent back to the planner.
- 2026-11-01: Plan revised (revision 2: 00-plan, 01-coding-plan, 03-docs-plan rewritten to follow the P08 doc and the brief).
  - Also read: `MainActivity.kt` (`enableEdgeToEdge`), `AndroidManifest.xml` (`adjustResize` already set on `MainActivity`, so no manifest change), the version catalog (Compose BOM 2026.02.01, material3 1.4.x; no `lifecycle-viewmodel-compose`/`lifecycle-runtime-compose` yet, D-006 adds them), and `MainTopAppBar.kt` (`@OptIn(ExperimentalMaterial3Api::class)` on the function, the opt-in convention). No shell in this session.
  - Changes from revision 1:
    - form state moves to new `viewmodel/ReportUiState.kt` (fields, `@StringRes` errors, computed `isValid`, `submitted`, `submitError`) and `viewmodel/ReportViewModel.kt` (per-route, no-arg, no coroutines), both new files in Diogo's folder as his doc proposes; `TicketsViewModel` untouched;
    - category source decided: new `data/SampleCategories.kt` (pavement, lighting, furniture + inactive graffiti), filtered to active ones by `ReportViewModel`;
    - category is an `ExposedDropdownMenuBox` (opt-in on the private composable; check the material3 1.4 anchor API); situation and scope stay radio groups;
    - errors are live: description on every change, choices cleared on selection, all missing fields on Submit; shown with `isError` + `supportingText` / error text + `error` semantics; summary in a live region;
    - submit path: `submit(): ReportSubmission?` in the ViewModel; `ReportScreen(viewModel, onSubmit: (ReportSubmission) -> Boolean)`; D-006 calls `reportTicket`, navigates up on success, returns `false` on `null` so the form stays and shows `report_error_not_created`; double-submit lock kept;
    - `collectAsState()` until D-006 adds `lifecycle-runtime-compose`.
  - Kept: stateless `ReportContent` for previews, nothing preselected (now Q9, against the doc's `DAMAGE`/`PRIVATE` example; if the person prefers defaults, PRIVATE rather than PUBLIC), media note and the I.2.E gap (Q2), no location (Q13), landscape two panes with hoisted scroll states and `imePadding()`.
  - Slices: 1. SampleCategories + ReportUiState + ReportViewModel + error strings; 2. ReportScreen/ReportContent portrait with dropdown, radios, errors, previews; 3. media note; 4. landscape.
  - Open questions with defaults: Q1 no `SavedStateHandle`; Q3 submit round trip above; Q4 SampleCategories in `data/`; Q5 dropdown + radios; Q6 button in the form; Q7 Diogo confirms the new files + D-006 call with `viewModel()` per route and `consumeWindowInsets`; Q8 Submit always enabled except after success; Q9 nothing preselected; Q10 `collectAsState` for now; Q11 no length limit; Q12 "Visibility"/"Alcance"; Q13 no location. ADR 0008 recommended in the docs phase.
  - Gate 1 pending.
- 2026-11-01: Alignment review of revision 2 (requested by the person) against the P08 and P09 class PDFs (`AM/Praticas/AMov.P08.pdf`, `AMov.P09.pdf`), Diogo's doc and the brief (checked with pdftotext; the I.2.E/I.3.E quotes in 00-plan match the PDF).
  - Aligned with P08 and the doc: `ReportUiState` with a computed `isValid` (slide 5), a route-scoped ViewModel (slides 4, 25), `isError` + `supportingText` (slide 22), `imePadding` + `adjustResize` (slides 18, 20), the category dropdown (slide 32), rotation check (slide 27), and the doc's do-not-copy list (slide 31).
  - Deviations from the doc or the slides, all justified in the plan: nothing preselected (Q9; the doc preselects DAMAGE/PRIVATE); the ViewModel returns a `ReportSubmission` and D-006 calls `reportTicket` (the slides save inside the ViewModel through a repository); `collectAsState` until D-006 adds the lifecycle dependency (the slides use `collectAsStateWithLifecycle`).
  - Corrections for the plan: Q8 says the slides use `enabled = isValid`, but slide 20 keeps the Save FAB clickable and only changes its colour by `isValid`. Q6 did not consider the slide-20 FAB (brief V mentions floating buttons). Line 24's "quotes not checked" is out of date.
  - P09 (media, Camera, Coil 3) is not in the doc or the plan. It shows local attachments with no Firebase and no runtime permission (PickVisualMedia / PickMultipleVisualMedia, TakePicture + FileProvider, copy into `filesDir`, Coil `AsyncImage`), and its "FixIt Project Bridge 1/3" applies it to the report form (`onPhotosAttached`). This makes the I.2.E attachment requirement reachable in phase 1, so Q2 is open again for the person. Its slide 28 (offline JSON queue, `PENDING_SYNC`) is on the doc's do-not-copy list.
- 2026-11-01: The person decided attachments go to a separate task, F-007 (report attachments, following P09), right after F-006. F-006 keeps Q2 (a), the visible note, and shapes `ReportUiState` so F-007 can add attachments and the "at least one image/video" rule to `isValid`. Notes sent to the planner for revision 3: this decision, the Q8 and Q6 corrections, and the outdated quote disclaimer.
- 2026-11-01: Plan revised (revision 3: targeted edits to 00-plan, 01-coding-plan, 03-docs-plan; everything else as revision 2).
  - Q2: recorded as decided (F-007, P09). F-006 keeps the visible note, whose wording promises only what F-007 delivers. No `attachments` field in `ReportUiState` now, because it would be dead state; its KDoc says where F-007 adds `attachments` + `attachmentsError` and the `isValid` term. The I.2.E gap is now "closed by F-007".
  - New 00-plan section "F-007 follow-up":
    - picker with `PickVisualMedia.ImageAndVideo` / `PickMultipleVisualMedia(3)`;
    - `TakePicture` + `FileProvider` (manifest `<provider>`, `res/xml/file_paths.xml`);
    - `utils/FileUtils.copyUriToInternalStorage` into `filesDir/images`;
    - Coil 3 (`coil-compose`, `coil-network-okhttp`) in the catalog;
    - `ReportUiState`/`ReportViewModel`/`ReportSubmission` attachments and `isValid`;
    - `TicketsViewModel.reportTicket` needs an attachments parameter (Diogo's file; handover Q7f);
    - thumbnails, compression and upload left for the upload phase; slide 28 not copied.
  - Q8 corrected: P08 slide 20 keeps the Save FAB clickable and only changes `containerColor` by `isValid`; slide 15 returns early when invalid. The default (always enabled, Submit shows the errors) is close to that. The slide-20 colour cue is now optional (default off).
  - Q6 extended with the P08 slide-20 FAB (check icon, screen-level `Scaffold`, `cd_submit_report`, bottom spacer). Default still the full-width button: it never covers the form or the landscape description pane, keeps a visible text label in reading order, and avoids a nested `Scaffold`. `MainTopAppBar` already hides add on `REPORT`.
  - Removed the "quotes not checked" disclaimer (checked by the main session with pdftotext). "Out of scope" now names F-007 for attachments, camera and picker.
  - 03-docs-plan: the gap is closed by F-007; the person creates the F-007 task file in `docs/tasks/`; report notes cover Q6/Q8.
  - Slices unchanged (4). Gate 1 pending.
- 2026-11-01: Gate 1 approved by the person (revision 3) with all defaults: Q1 no SavedStateHandle; Q2 (a) media note, attachments in F-007; Q3 ReportSubmission round trip via D-006; Q4 SampleCategories filtered by ReportViewModel; Q5 dropdown + radio groups; Q6 full-width Submit at the end of the form (not the FAB); Q8 always enabled; Q9 nothing preselected; Q10 collectAsState until D-006; Q11 no length limit; Q12 Visibility/Alcance; Q13 no location. Before approving, the person asked why a ReportUiState and a ViewModel are needed (rotation, MVVM stateful/stateless split, route scope vs the app-wide TicketsViewModel; F-007 and Firebase later).

Revision 2 (2026-11-01, follows "Dicas da aula P08 para o CityFix"). Replaces revision 1.
Revision 3 (2026-11-01): the I.2.E gap is closed by F-007; the person creates the F-007 task file; report notes cover Q6 and Q8.

## Docs to update
- `docs/architecture/overview.md`:
  - "Layers": `viewmodel/` now has two ViewModels: `TicketsViewModel` (app-wide, above the `NavHost`) and `ReportViewModel` (form-only, per route).
  - New subsection "Report form (F-006)" after "ViewModel (D-003)":
    - `ReportUiState` (fields, `@StringRes` errors, computed `isValid`, `submitted`, `submitError`) and `ReportSubmission`;
    - `ReportViewModel` events, validation timing (description while typing, choices on selection, every missing field on Submit), the double-submit lock, `onSubmitResult`;
    - scope: created with `viewModel()` inside `composable<ReportTicketRoute>`, survives rotation, cleared on leaving, lost on process death (Gate 1 Q1);
    - category source: `data/SampleCategories`, filtered to active ones by the ViewModel (I.3.E, Q4);
    - nothing preselected (I.2.E, Q9);
    - the P08 origin and the three differences from the slides (`@StringRes` errors, stateful/stateless split, no file/`suspend`).
  - "Screens": `ReportScreen(viewModel, onSubmit, modifier)` / `ReportContent(state, ...)` entry:
    - controls (dropdown with `@OptIn`, radio groups), errors and TalkBack, media note;
    - **gap against I.2.E** (no image/video), open until **F-007** (report attachments, P09) closes it, plus what F-007 adds to `ReportUiState`/`isValid` (00-plan "F-007 follow-up");
    - no location, so no 10 m grouping;
    - layout, `imePadding()` + the manifest's `adjustResize`;
    - previews;
    - **Contract for D-006** (the caller in 01-coding-plan, including `consumeWindowInsets` and the switch to `collectAsStateWithLifecycle`).
  - "Shared components": `scopeLabelRes` next to `statusLabelRes`/`situationLabelRes` (in `ReportScreen.kt`; move to `ui/components/` if another screen needs it).
  - "ViewModel (D-003)": `reportTicket` is called by D-006 with a `ReportSubmission`; never navigate to the returned id; on `null` the form stays.
- **F-007 task file:** the person creates `docs/tasks/phase-1/F-007-report-attachments.md` (report attachments, P09) in `docs/tasks/`. The docs writer does not; 00-plan "F-007 follow-up" is the input. Add F-007 to the phase-1 index only if the person asks (`docs/tasks/` is the person's).
- `docs/tasks/phase-1/F-006-report-screen.md`: not edited by the docs writer. Its "Files" line lists only `ReportScreen.kt`; ask the person whether to add the ViewModel files.

## Decisions to record
- **New ADR `0008-report-form-state.md`** (recommended, because the person and Diogo changed the plan at Gate 1 and both are likely defence questions):
  - context: brief I.2.E/I.3.E/V, the P08 doc, phase 1 without a repository;
  - decision: a route-scoped `ReportViewModel` + `ReportUiState` with `@StringRes` errors and `isValid`; submit returns `ReportSubmission`, and the caller calls `reportTicket` and reports success back; nothing preselected; active categories from `SampleCategories`;
  - rejected: `rememberSaveable` in the screen (revision 1), form state in `TicketsViewModel`, a ViewModel holding another, a constructor-injected reporter (needs a factory), preselected `DAMAGE`/`PRIVATE`, `enabled = isValid`;
  - consequences: draft lost on process death; the I.2.E attachment gap and how `isValid` changes; the Firebase-phase repository/factory replaces `SampleCategories`.
- Decision 0006 ("A phase-1 report has no attachment") is Diogo's and stays unedited; 0008 links it.

## Report notes
Add a "F-006: ReportScreen" section to `docs/report/notes.md`:
- Technical solutions: P08 pattern (route-scoped ViewModel, one immutable UI state with computed `isValid`), `@StringRes` errors for localisation, live per-field validation, double-submit lock and the `onSubmitResult` round trip, `ExposedDropdownMenuBox` with opt-in, accessible radio groups, `imePadding()` + `adjustResize` with edge-to-edge, landscape two panes with hoisted scroll states.
- Options chosen and rejected: as in ADR 0008; radio groups vs segmented buttons; submit button vs top-bar action.
- Submit control: full-width button vs the P08 slide-20 FAB (Gate 1 Q6), and P08's always-clickable save with an `isValid` colour cue (Q8).
- Known gaps: no attachment (I.2.E) until F-007; no location or grouping; anonymous users can open the form until the Auth task; category names not translated; process death loses the draft.
- Improvement opportunities: `SavedStateHandle`; repository + factory in the Firebase phase (doc "later" list); `suspend` submit with a progress state; impact-detection prefill (I.2.F); description limit and counter; rotation and force-close checks for the defence (doc).

# F-008: Docs plan (revision 2)

## Docs to update
- `docs/architecture/overview.md`:
  - "Layers": `data/` holds `TicketsRepository`, `LocalTicketsRepository`, `AttachmentStorage`; remove "repositories come with Firebase". Mention `CityFixApplication` and the two factory classes.
  - "Rules (D-002)" / "ViewModel (D-003)": the repository owns the `TicketsList` cache and calls `toAnonymousView()`; `TicketsViewModel` owns its flows, loads in `viewModelScope` (`init`, `refresh()`), has no `reportTicket`, is built with `TicketsViewModelFactory`; process lifetime (Q4); empty first frame.
  - "Report form (F-006)": categories from `activeCategories()` (filter owner is the repository); `submit(onSuccess)` saves through the suspend `reportTicket` with `isSaving`; `ReportSubmission`/`onSubmitResult` removed; `ReportViewModelFactory`.
  - "Report attachments (F-007)": gallery copy on `Dispatchers.IO` via `AttachmentStorage` with `isCopyingAttachments`; Q13 exception gone for the copy; camera `length`/`delete` still under Q6.
  - "App shell (D-006)": state table (factories, `CityFixApplication`); report flow steps rewritten (callback with no id, `refresh()`, snackbar, `navigateUp()`).
- `docs/tasks/phase-1/D-007-anonymous-projection.md` is Diogo's: not edited; hand over that the call site is now the repository.

## Decisions to record
- New `docs/decisions/0009-tickets-repository.md`: the Gate 1 answers (Contacts_#3 pattern: `Application` + factory classes, ViewModels own state, suspend repository with cache, suspend save with `onSuccess`, gallery copy on IO beyond Contacts_#3); interface kept for Firebase (Contacts_#3 has none); `Mutex`; Q4 lifetime and empty first frame; callback risks (stale `navController`, back mid-save). Supersedes the "no repository / no factory / no coroutines / caller creates the ticket" parts of 0006 (Q6, Q11) and 0008.
- `0008`: one-line status "Superseded in part by 0009". `0006`: same line, by Diogo or with his OK.

## Report notes
`docs/report/notes.md`, new F-008 section:
- Why the repository came before Firebase; Contacts_#3 as the model, and where we differ (interface, `Mutex`, `modelClass.cast`, no JSON file, copy off the main thread).
- Manual DI with `Application` + factory classes; why not Hilt.
- Who owns state: ViewModel `StateFlow`s filled from `suspend` loads; one-frame empty state.
- Async save: `submitted` lock + `isSaving`, `onSuccess` without the id (D-006 rule), known callback risks.
- Improvement opportunities: `FirestoreTicketsRepository`; separate `CategoriesRepository`; a "saved" event in the UI state instead of the callback; `isLoading`; JSON persistence as in Contacts_#3 if offline survival is wanted before Firestore.

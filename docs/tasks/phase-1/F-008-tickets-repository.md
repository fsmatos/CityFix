# F-008: Tickets repository (local implementation)

Owner: Francisco (`dev/Francisco`) · Layer: `data` / `viewmodel` · Phase 1 (bridge to Firebase) · Status: to do
Type: refactor (`/refactor`) · Depends on: F-007, D-007. Coordinate with Diogo: `TicketsViewModel.kt` and `MainActivity.kt` are his files, and decisions 0006 and 0008 change.

## Goal
Put a repository between the ViewModels and the data, as the project rules and P08 expect, before the Firebase phase starts. Behaviour does not change. The Firebase phase then adds a second implementation instead of rewiring the ViewModels.

## Why
- `.claude/rules/kotlin.md`: anonymous users' data must be filtered "in the repository layer, not only in the UI". Today `TicketsViewModel` calls `toAnonymousView()`.
- `kotlin-conventions`: ViewModels depend on repository interfaces. `CLAUDE.md` describes `data/` as repositories, but it holds only `SampleTickets` and `SampleCategories`.
- Class material: P08 introduces `ContactsRepository` (in-memory cache, JSON persistence, IO dispatch) before any Firebase work.
- Decisions 0006 (Q6, Q11) and 0008 deferred the repository until Firebase. This task reverses that deferral.
- F-007 copies attachments on the main thread (Q13, accepted exception). A suspend save in the repository removes the exception while keeping P09's structure.

## What to build
- **`data/TicketsRepository`** interface with today's `TicketsViewModel` API: tickets flow (already projected), stats flow, `getTicketById(id)`, `reportTicket(...)` and the active categories.
- **`data/LocalTicketsRepository`**: wraps `TicketsList`, `SampleTickets` and `SampleCategories`, applies `toAnonymousView()` itself, and creates the ticket (moved from `TicketsViewModel.reportTicket`).
- **Wiring:** one repository instance for the app, given to `TicketsViewModel` through a `ViewModelProvider.Factory` (or a simple app-level container: decide at Gate 1). `TicketsViewModel` keeps its public API, so screens do not change.
- **Optional, decide at Gate 1:** a suspend attachment save on `Dispatchers.IO` (repository or a small `data/` class over `FileUtils`) so `ReportScreen` no longer blocks the main thread. This undoes the Q13 exception from F-007.
- **Decision record:** new decision 0009 that supersedes the "no repository" parts of 0006 and 0008. Update `docs/architecture/overview.md`.

## Files
New `data/TicketsRepository.kt` and `data/LocalTicketsRepository.kt`; `viewmodel/TicketsViewModel.kt` (Diogo's), `MainActivity.kt` (Diogo's, where the ViewModel is created), possibly `ReportViewModel.kt` and `ReportScreen.kt` (optional part); `docs/decisions/0009-*.md`, `docs/architecture/overview.md`.

## Behaviour that must not change
- The list shows the same public active tickets, newest first, projected for anonymous users.
- Dashboard stats count every ticket, private ones included (decision 0005).
- `reportTicket` returns `null` in the same cases (blank fields, no attachments), and a PRIVATE report is not opened (D-006).
- Rotation keeps state; EN and PT unchanged.

## Acceptance checks
- [ ] No ViewModel references `TicketsList`, `SampleTickets` or `SampleCategories` directly.
- [ ] `toAnonymousView()` is called only in the repository.
- [ ] Dashboard -> list -> detail -> report works as before, in portrait and landscape.
- [ ] If the optional part is taken: picking a large video does not freeze the UI.
- [ ] `./gradlew assembleDebug` and `./gradlew lint` pass with no new warnings.

## Out of scope
Firebase, Firestore, Auth, the 10 m grouping and location, persistence to disk.

## Order
After D-007, which changes `toAnonymousView()`: that change then lands in one place. Before any Firebase task.

## References
- Decisions 0005, 0006, 0008; F-007 `00-plan.md` (Q13); P08 (ContactsRepository); `docs/architecture/overview.md`, "ViewModel (D-003)".

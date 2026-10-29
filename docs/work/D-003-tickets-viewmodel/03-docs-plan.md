# D-003: Docs plan

## Docs to update
- `docs/architecture/overview.md`: add a "ViewModel (D-003)" subsection after "Rules (D-002)" that covers:
  - `TicketsViewModel` owns the phase-1 `TicketsList`, seeded from `data/SampleTickets.kt`;
  - it exposes `tickets: StateFlow<List<Ticket>>` (public active, newest first, projected) and `stats: StateFlow<DashboardStats>` (all tickets), plus `getTicketById` (visible tickets only, `null` otherwise) and `reportTicket` (UUID id, OPEN, no location, no grouping yet);
  - `Ticket.toAnonymousView()` (`model/TicketPrivacy.kt`): the fields it blanks and the 2-decimal location;
  - the D-006 contract: create the ViewModel once above the `NavHost` and pass state down. D-006 adds `lifecycle-viewmodel-compose` and `lifecycle-runtime-compose`;
  - the "Layers" bullet for `data/`: in phase 1 it holds only `SampleTickets`, and repositories come with Firebase.
- `docs/tasks/phase-1/README.md`: under "Agree the screen signatures first", add one line with the ViewModel API Francisco wires against: `tickets`, `stats`, `getTicketById(id)` and `reportTicket(description, categoryId, categoryName, situation, scope): String?`. Also say that tickets reach the screens already projected, and that the sample category ids are `pavement`, `lighting` and `furniture` (00-plan Q13).
- Coordination message for Francisco before `/push-task` (not a file):
  - the ViewModel API above;
  - the screens receive projected tickets (blank `creatorId`, coarse location), so F-005 must not try to show the author;
  - a PRIVATE report does not appear in the anonymous list.

## Decisions to record
- `docs/decisions/0006-anonymous-projection-phase-1.md` (new), covering 00-plan Q1-Q4 and Q8:
  - **Context:** phase 1 has no login, so every viewer is anonymous. `publicActive()` filters which tickets, not which fields (0005), and the rule must be enforced below the UI.
  - **Decision:**
    - a pure `Ticket.toAnonymousView()` in `model/` that keeps the `Ticket` type (rejected: a `PublicTicket` class), and the fields it blanks, including `StatusHistory.responsibleId` (the creator's id hides there) and `Attachment.uploadedById`;
    - a 2-decimal location (rejected: 3 decimals, too precise; zeroing, which clashes with "(0, 0) = unset");
    - `getTicketById` searches only the visible list;
    - `stats` stay over every ticket (0005, Q9);
    - the 10 m grouping is deferred until reports carry a location, with no fake location on new tickets.
  - **Consequences:**
    - a field added to `Ticket` later must be checked against the projection;
    - the Firebase phase writes the same projection into a public reduced-field collection (README risk 3);
    - registered and operator views need a separate, unprojected source.
- If Gate 1 changes Q6 (one `UiState`) or Q11 (repository now), record that in 0006 as well.

## Report notes
Add a "D-003: TicketsViewModel" section to `docs/report/notes.md`:
- **MVVM as in P07:**
  - the ViewModel owns the in-memory `TicketsList`;
  - read-only `StateFlow`s out (private `MutableStateFlow` + `asStateFlow()`), events in as functions;
  - state survives rotation because a `ViewModel` outlives the activity's configuration change;
  - no coroutines are launched, because all work is synchronous and in memory.
- **Privacy by projection:** what is hidden from anonymous users and why, and the coarse-location choice (about 1 km). The hidden leak found during planning: the first OPEN history entry's `responsibleId` is the creator's id.
- **Sample data:** fixed UTC epoch values make the dashboard reproducible (3 tickets: 2 active, 1 resolved after 3 days, so active about 67% and average 3 days).
- **One shared instance:** why the ViewModel is created above the `NavHost` (one instance for every screen).
- **Verification:** D-003 has no automated tests (Diogo's decision); it was verified by build, lint, code review, and by running the app after D-006.
- **Improvement opportunities:**
  - a `TicketsRepository` interface with Firestore and in-memory implementations;
  - one `UiState` with loading and error once data is remote;
  - `SavedStateHandle` or Firestore offline persistence for process death;
  - wiring `findNearby` once reports carry a GPS location (follow the existing ticket; skip if the reporter is the creator or already follows it);
  - a public reduced-field Firestore collection fed by the same projection;
  - separate views for registered citizens and operators.

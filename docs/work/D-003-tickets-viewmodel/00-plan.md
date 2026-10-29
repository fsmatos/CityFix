# D-003: TicketsViewModel
Type: feature · Task file: docs/tasks/phase-1/D-003-tickets-viewmodel.md
Labels: none

## Problem
The phase-1 screens (F-003 list, F-004 dashboard, F-005 detail, F-006 report) need tickets and dashboard figures, and D-006 has to wire them through `MainActivity`. No layer exposes that data to the UI yet. `TicketsList` (D-002) holds tickets in memory, but composables must not touch it directly, and the state has to survive rotation. The data also has to be safe for anonymous users. Phase 1 has no login, so every screen is effectively anonymous. `TicketsList.publicActive()` filters which tickets are returned, not which fields (decision 0005, D-002 review S4), so the ticket that reaches the UI must still drop the author data and the exact coordinates (CLAUDE.md non-negotiable rule, decision 0003).

## Outcome
`viewmodel/TicketsViewModel.kt` owns a private `TicketsList` seeded with 3 sample tickets and exposes:
- `tickets: StateFlow<List<Ticket>>`: public active tickets, newest first, each projected for anonymous users;
- `stats: StateFlow<DashboardStats>`: the dashboard figures over all tickets (decision 0005, Q9);
- `getTicketById(id): Ticket?`: a projected ticket from the visible list, or `null`;
- `reportTicket(...)`: creates a new ticket for F-006 with a generated unique id (slice 4, Q7).

Rotation keeps the state because it lives in a `ViewModel`, not in a composable.

## Acceptance checks
- [ ] `tickets` and `stats` are typed `StateFlow` (read-only). The `MutableStateFlow`s and the `TicketsList` are `private`, so no public member exposes `TicketsList` or a mutable flow.
- [ ] `getTicketById("does-not-exist")` returns `null`, so F-005 shows "Ticket not found". Private, resolved and closed ids also return `null` (Q4).
- [ ] ~~Unit test with the 3 sample tickets.~~ **Removed by Diogo's decision (2026-10-29):** D-003 has no automated tests. Verification is `./gradlew assembleDebug` + `./gradlew lint`, plus reading the code at review and running the app once D-006 wires it.
- [ ] There are exactly 3 sample tickets. Each has a distinct, non-blank id, `scope = PUBLIC`, `createdAt > 0`, a real location (not (0, 0)) more than 10 m from the others, and a `currentStatus` equal to its last `statusHistory` entry (decision 0004).
- [ ] With the sample data, `stats.value` is total 3, active 2, resolved 1, average repair time 3 days (259 200 000 ms), and `activeRatio` about 0.667. `tickets.value` holds `sample-1` and then `sample-2` (newest first); `sample-3` is RESOLVED, so it is not listed.
- [ ] Every ticket in `tickets` and every ticket that `getTicketById` returns has a blank `creatorId`, empty `followerIds` and `messages`, a blank `responsibleId` on every history entry, a blank `uploadedById` on every attachment, and a location rounded to 2 decimals (Q1, Q2).
- [ ] `reportTicket(...)` with valid input adds a ticket with a fresh UUID id, `currentStatus = OPEN` and one OPEN history entry, then updates both flows. Blank input returns `null` and changes nothing.
- [ ] No `viewModelScope`, `GlobalScope` or Android imports beyond `androidx.lifecycle.ViewModel`; no new dependency (Q10).
- [ ] `./gradlew assembleDebug` and `./gradlew lint` pass after every slice.

## Approach
The ViewModel follows the P07 ViewModel + `ContactsList` pattern, with no repository yet.

1. **Anonymous projection as a pure rule in `model/`** (`model/TicketPrivacy.kt`). `fun Ticket.toAnonymousView(): Ticket` returns a copy of the ticket with:
   - `creatorId` blank, and `followerIds` and `messages` empty;
   - `responsibleId` blank on every history entry, because the first OPEN entry's `responsibleId` is the creator's id, so it would leak `creatorId` again;
   - `uploadedById` blank on every attachment;
   - the location rounded to 2 decimals (about 1.1 km of latitude and 0.85 km of longitude at Coimbra).

   It keeps the `Ticket` type, so Francisco's agreed signatures (`ListScreen(tickets, ...)`, `ShowScreen(ticket?, ...)`, `TicketCard(ticket, ...)`) do not change.
2. **Sample data in `data/SampleTickets.kt`**: an `object SampleTickets { val all: List<Ticket> }` with 3 tickets near Pr. Republica, Coimbra:
   - `sample-1`: OPEN, newest;
   - `sample-2`: IN_PROGRESS;
   - `sample-3`: RESOLVED 3 days after it was created, so the dashboard shows a real average.

   The times are fixed epoch constants, not `System.currentTimeMillis()`, so the figures are reproducible.
3. **`TicketsViewModel`** (no-arg constructor, so D-006 can use the default `viewModel()` factory):
   - a private `TicketsList(SampleTickets.all)`;
   - two private `MutableStateFlow`s, exposed with `asStateFlow()`;
   - a private `refresh()` that recomputes `publicActive().map { it.toAnonymousView() }` and `stats()`;
   - `getTicketById` searches the current `tickets` value.
4. **`reportTicket(description, categoryId, categoryName, situation, scope): String?`**:
   - builds a `Ticket` with `UUID.randomUUID().toString()`, `createdAt = System.currentTimeMillis()`, `creatorId = LOCAL_USER_ID` and one OPEN `StatusHistory`;
   - calls `ticketsList.add`, then `refresh()`;
   - returns the new id, or `null` when the input is blank or `add` refuses it.

   **10 m grouping (`findNearby`)** belongs exactly here: before creating the ticket, look for an active ticket with the same category and situation within 10 m, and follow it instead. Phase 1 has no location input (F-006 has category, situation, description and scope only), so every report is at (0, 0) and `findNearby` always returns `null` for it (decision 0005). Wiring a follow path now would be dead code, and it would also need a `TicketsList.update`. It is deferred to the task that adds the report location (Q8). The plan for that task: call `findNearby`; if it finds a ticket whose `creatorId` and `followerIds` do not include the reporter, append the reporter to `followerIds` and return that ticket's id.

Alternatives considered:
- A separate `PublicTicket` data class for anonymous views. The type would prove the privacy rule, but it breaks the screen signatures agreed with Francisco and duplicates `Ticket` (Q1).
- Leaving the projection to the screens (F-005's check). The rule says to enforce it below the UI, and the ViewModel is the lowest layer in phase 1.
- One `TicketsUiState(tickets, stats)` flow. This fits the kotlin-conventions wording, but the task asks for "a `StateFlow` of tickets (and stats)", and the list and dashboard are separate screens with no loading or error state in phase 1 (Q6).
- A `TicketsRepository` interface now. It is cleaner for the Firebase phase, but it is extra code with only one in-memory implementation (Q11).
- Injecting a clock and an id generator into the constructor. That would only help tests, and D-003 has none.

## Architecture impact
- New `viewmodel/TicketsViewModel.kt`, the first ViewModel in the app. `viewmodel/.gitkeep` is removed.
- New `data/SampleTickets.kt`: the phase-1 stand-in for the data source. `data/.gitkeep` is removed.
- New `model/TicketPrivacy.kt`: a pure rule (no Android imports) that the Firebase phase can reuse when it writes the public reduced-field collection (README risk 3).
- Dependencies: `ui -> viewmodel -> model`, `viewmodel -> data -> model`. `model/` stays free of Android imports.
- **Contract for D-006:** create the ViewModel once, above the `NavHost` (in `MainActivity`'s content, `val vm: TicketsViewModel = viewModel()`), and pass state and callbacks down. If each destination calls `viewModel()` on its own back-stack entry, each screen gets its own instance, and a reported ticket would not appear in the list. D-006 also needs `lifecycle-viewmodel-compose` (for `viewModel()`) and `lifecycle-runtime-compose` (for `collectAsStateWithLifecycle`). D-003 adds neither.
- **Contract for Francisco (F-003/F-004/F-005/F-006):** screens receive `List<Ticket>`, `DashboardStats` and `Ticket?` exactly as agreed. The tickets they receive already have blank author fields and a coarse location. F-006's submit callback maps to `reportTicket(description, categoryId, categoryName, situation, scope)`.

## Failure modes
- **The ViewModel is scoped per destination in D-006**, so the list and the report screen use different instances and new tickets vanish. Mitigation: the D-006 contract above, also written in `overview.md` (docs plan).
- **Personal data leaks through a field that the projection forgets.** The known paths are `creatorId`, `followerIds`, `messages`, `StatusHistory.responsibleId`, `Attachment.uploadedById` and `location`. A field added to `Ticket` later is copied through unchanged. Mitigation: a KDoc on `toAnonymousView()` lists the fields and says "update this when `Ticket` gains a personal field". The reviewer checks this list against `Ticket.kt`.
- **A PRIVATE report disappears from the list right after submit**, because the anonymous list shows public tickets only. Expected in phase 1 (no login), but it may look like a bug. F-006/D-006 can show a confirmation snackbar.
- **`reportTicket` with a blank category or description** is rejected and returns `null`, so the screen must handle `null`.
- **Process death** (not rotation) loses reported tickets and restores the samples. This is accepted for phase 1; Firestore replaces it (Q12).
- **Wrong epoch constants** in the sample data give a wrong average. Mitigation: the coding plan lists each constant with its UTC date, and the reviewer recomputes the 3-day difference.

## Risks and open questions
Recommendations are in **bold**.

- **Q1. How should tickets be projected for anonymous users?**
  - (a) `Ticket.toAnonymousView(): Ticket`, which returns the same type with personal fields blanked;
  - (b) a separate `PublicTicket` class.

  **(a)**: it keeps Francisco's agreed screen signatures. The function is a pure rule in `model/`, and the Firebase phase reuses it.
- **Q2. How coarse should the location be?**
  - (a) round to 2 decimals, about 1 km;
  - (b) 3 decimals, about 100 m, which still points to a street;
  - (c) zero it, so it is unset.

  **(a)**: phase 1 has no map, and the brief asks for "coarse map zoom" (I.1.C). With (c), later map code would have to treat (0, 0) as "no location" everywhere. The decimals are a named constant, `ANONYMOUS_COORDINATE_DECIMALS = 2`.
- **Q3. What does `tickets` expose?**
  - (a) public active tickets, projected;
  - (b) every ticket.

  **(a)**: phase 1 has no login, so every viewer is anonymous (brief I.1.B, "ocorrências públicas ativas"). A registered-citizen or operator view is a later task, with a separate flow or ViewModel.
- **Q4. What does `getTicketById` search?**
  - (a) only the visible projected list;
  - (b) every ticket.

  **(a)**: with (b), an anonymous user who knows an id could open a private or resolved ticket. The cost is that `sample-3` (RESOLVED) cannot be opened in phase 1. It cannot be reached from the list anyway.
- **Q5. What mix of sample tickets?**
  - (a) 2 active + 1 resolved: the list shows 2, and the dashboard shows a real average;
  - (b) 3 active: the list shows 3, and the dashboard shows the "no repairs yet" placeholder.

  **(a)**: both screens show meaningful values. Option (b) never exercises the average.
- **Q6. Two flows (`tickets`, `stats`) or one `TicketsUiState`?** **Two flows**: this matches the task wording and the screen parameters. One `UiState` can come later if loading or error states appear.
- **Q7. Does D-003 include `reportTicket`?** F-006 is a form with no submit target, and D-006 wires the flow to report. Composables may not touch `TicketsList`, so the only entry point is the ViewModel. **Yes, as slice 4.** If Diogo prefers to keep D-003 to the task text, drop slice 4 and add `reportTicket` in D-006. Return type: **the new id (`String?`, `null` when rejected)**, so D-006 can navigate or show a message.
- **Q8. When does the 10 m grouping (`findNearby`) get wired?** **Defer it to the task that adds the report location** (FusedLocationProvider). The reasons, and the planned follow path, are in Approach step 4. Phase-1 reports are created at (0, 0), which `Ticket.kt` and decision 0003 call "not a real location". **Do not put a fake location** (for example the municipality centre) **on new tickets**: two reports at the same fake point would group with each other when grouping is enabled.
- **Q9. Where does the sample data live?**
  - (a) `data/SampleTickets.kt`;
  - (b) a companion object in the ViewModel.

  **(a)**: `data/` is the data-source layer that Firestore replaces later. It also keeps the ViewModel short.
- **Q10. Do we need dependencies?** `androidx.lifecycle.ViewModel` and `kotlinx.coroutines.flow.MutableStateFlow` are expected on the compile classpath through `activity-compose` and `lifecycle-runtime-ktx` (both depend on them as API dependencies). **Add nothing.** Slice 3's build confirms it. If `ViewModel` does not resolve, add `androidx-lifecycle-viewmodel-ktx` with `version.ref = "lifecycleRuntimeKtx"` in the same slice and record it as a deviation.
- **Q11. Should D-003 add a repository interface?** **Not now.** The kotlin-conventions rule ("ViewModels depend on repository interfaces") applies once Firebase exists. Phase 1 follows the task and README ("`TicketsList` owned by `TicketsViewModel`"). This is recorded as a report note.
- **Q12. Do we need `SavedStateHandle` for process death?** **No.** The task asks for rotation only, a `ViewModel` handles that, and Firestore offline persistence covers the rest later.
- **Q13. Where does F-006's category list come from?** There is no category source in phase 1. **This is out of scope for D-003.** Francisco's F-006 provides a provisional list (id plus name resource). The sample tickets use the ids `pavement`, `lighting` and `furniture`, and F-006 should reuse them.
- **Q14. The task's Files section lists only `TicketsViewModel.kt`.** This plan adds `model/TicketPrivacy.kt` and `data/SampleTickets.kt`. **Accept**: both are small, and keeping them out of the ViewModel puts the privacy rule in `model/` and the data in `data/`, per the architecture.
- Risk: none of the acceptance checks can be seen in the running app until D-006 wires the screens. Until then they are verified by build, lint and code review.

## Out of scope
- Automated tests of any kind (Diogo's decision, 2026-10-29).
- The 10 m grouping follow path, the report location and attachments (Q8). A phase-1 report therefore has no attachment, which breaks the diagram's `1..*` (decision 0003, "creation logic enforces"). That is accepted for the provisional form.
- Registered-citizen and operator views, status transitions, the 5-day reopen window, messages.
- A repository interface, Firebase, `SavedStateHandle`.
- Screens, navigation and Compose dependencies (`lifecycle-viewmodel-compose`, `lifecycle-runtime-compose`): F-003..F-006 and D-006.
- Changes to `TicketsList`, `DashboardStats` or the `Ticket` model.

## Slices
1. `feat(D-003): add anonymous ticket projection`: `model/TicketPrivacy.kt` with `Ticket.toAnonymousView()` and `ANONYMOUS_COORDINATE_DECIMALS`.
2. `feat(D-003): add phase-1 sample tickets`: `data/SampleTickets.kt` (3 tickets); delete `data/.gitkeep`.
3. `feat(D-003): add TicketsViewModel with tickets and stats flows`: `viewmodel/TicketsViewModel.kt` with `tickets`, `stats` and `getTicketById`; delete `viewmodel/.gitkeep`.
4. `feat(D-003): add reportTicket entry point`: `reportTicket(...)` in the ViewModel (only if Q7 = yes).

Each slice is verified with `./gradlew assembleDebug` and `./gradlew lint`, and becomes one commit.

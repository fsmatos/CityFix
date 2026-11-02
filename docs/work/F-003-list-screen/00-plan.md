# F-003: ListScreen
Type: feature · Task file: docs/tasks/phase-1/F-003-list-screen.md
Labels: none

## Problem
Phase 1 needs the screen behind `TicketListRoute` (D-004): the public list of active tickets that an anonymous citizen sees (brief I.1.B: each item shows category, situation and thumbnail). The pieces exist: `TicketsViewModel.tickets` (D-003) already delivers the tickets, and `TicketCard` (F-002) draws one row. What is missing is the screen that lists them, with an empty state, in both orientations.

## Outcome
A stateless composable `ListScreen(tickets, isExpanded, onTicketClick, modifier)` in `ui/screens/ListScreen.kt`:
- draws each ticket with `TicketCard` in a `LazyColumn` keyed by `Ticket.id`, in the order it receives;
- passes the global `isExpanded` toggle through to every card;
- reports a tap as `onTicketClick(ticket.id)`, ready for `navigate(TicketDetailRoute(id))` in D-006;
- shows a centred, localised message (EN + PT) when the list is empty;
- has previews with data, expanded, and empty, in portrait and landscape (plus one PT preview).

D-006 wires it: `ListScreen(tickets = vm.tickets.collectAsStateWithLifecycle().value, isExpanded = expanded, onTicketClick = { navController.navigate(TicketDetailRoute(it)) }, modifier = Modifier.padding(innerPadding))`.

## Acceptance checks
- [ ] With tickets, every ticket appears as one `TicketCard` (thumbnail, category, situation, status), in the order given.
- [ ] Empty list shows the empty-state message, centred, in English; in Portuguese with the device (or preview) language PT.
- [ ] The lazy list uses stable keys: `items(tickets, key = { it.id })` in the code.
- [ ] Tapping a card calls `onTicketClick` with that ticket's id (interactive preview or, after D-006, opening the detail).
- [ ] `isExpanded = true` expands every card (expanded preview).
- [ ] `@Preview` with data and empty, each in portrait and landscape; in landscape the list scrolls and no card is clipped.
- [ ] `./gradlew assembleDebug` and `./gradlew lint` pass with no new warnings (no `MissingTranslation`).
- [ ] `ListScreen.kt` reads no ticket field itself (it passes the `Ticket` to `TicketCard` and uses only `id`); grep for `location|creatorId|followerIds|messages` is empty.

## Approach
**Filtering stays upstream.** The existing code already filters and projects: `TicketsList.publicActive()` (D-002) keeps PUBLIC tickets with `isActive` (OPEN, INFO_REQUESTED, IN_PROGRESS), newest first, and `TicketsViewModel.tickets` (D-003) maps them through `toAnonymousView()`. The overview and `.claude/rules/kotlin.md` say privacy is enforced in the data/model layer, not only the UI. So `ListScreen` shows exactly the list it is given: no filter, no sort, no `isActive` check. Filtering again in the screen would duplicate the rule and could drift from it.

**Layout.** `ListScreen` checks `tickets.isEmpty()`:
- empty: a `Box(modifier.fillMaxSize(), contentAlignment = Center)` with a `Text(stringResource(R.string.ticket_list_empty))`, centred, `bodyLarge`, `onSurfaceVariant`, with horizontal padding so long PT text wraps;
- otherwise: `LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = spacedBy(8.dp)) { items(tickets, key = { it.id }) { ticket -> TicketCard(ticket, isExpanded, onClick = { onTicketClick(ticket.id) }) } }`.

`modifier` is the last parameter with a default and is applied to the root, so D-006 can pass the `Scaffold` inner padding. Same layout in both orientations (see Q3). Ticket ids are unique and non-blank because `TicketsList.add` refuses blank or duplicate ids, so `key = { it.id }` cannot crash on data from the ViewModel.

**No stateful wrapper here.** The agreed signature (phase-1 index, "Step 0") makes `ListScreen` itself the stateless composable; the overview's D-003 contract has D-006 create the single `TicketsViewModel` above the `NavHost` and pass state down. See Q1 for the gap with the `XxxScreen(viewModel)` / `XxxContent` rule.

Alternatives considered:
- Filter `isActive` / `Scope.PUBLIC` in the screen: rejected, it duplicates D-002's rule and the ViewModel already does it.
- Take a `UiState` or the ViewModel as parameter: rejected, it breaks the agreed signature and the D-003 contract (one VM above the `NavHost`).
- `LazyVerticalGrid` with two columns in landscape: possible later (Q3), but the task asks for a `LazyColumn` and the card already fills the width with ellipsis.
- An empty-state icon or illustration: not needed for the checks; text only (Q5).

## Architecture impact
- New file `ui/screens/ListScreen.kt`, the first file in `ui/screens/` (remove `ui/screens/.gitkeep`).
- One new string key, `ticket_list_empty`, in `values/` and `values-pt/`.
- No change to `model/`, `viewmodel/`, `data/`, navigation, Gradle or the manifest. No new dependency.
- Gives D-006 the call it needs for `composable<TicketListRoute>`.

## Failure modes
- Duplicate or blank ids would crash `LazyColumn` ("Key was already used"). Prevented upstream by `TicketsList.add`; preview data uses distinct ids.
- Unprojected tickets (with `creatorId`, exact `location`) reaching the screen: the screen never reads those fields, and the card does not either, so nothing leaks visually; the projection itself is D-003's job.
- Expanded cards show the free-text `description` (see Q2): with the global toggle on, an anonymous user sees text that may hold personal data.
- Long PT empty-state text or large font scale: wraps inside the padded `Box`, no clipping.
- Landscape with few tickets: the list is short and the rest of the screen is empty; acceptable.
- Edge-to-edge: without D-006 passing `innerPadding`, the first card could sit under the status bar. That is D-006's wiring; the previews do not draw system bars.

## Risks and open questions
Each question has a default the plan uses unless the person says otherwise at Gate 1.

- **Q1. Stateful/stateless split.** `.claude/rules/kotlin.md` asks for a stateful `XxxScreen(viewModel)` plus a stateless `XxxContent(...)`. The agreed signature makes `ListScreen(tickets, isExpanded, onTicketClick)` the stateless one, and D-006 collects the flow. *Default:* keep the agreed signature with no stateful wrapper in F-003; D-006 collects `tickets` in the `composable<TicketListRoute>` block. Alternative: rename to `ListContent` and add a stateful `ListScreen(viewModel, ...)` (changes the agreed signature; tell Diogo).
- **Q2. `isExpanded` and anonymous users (F-002 review S1).** The person chose to leave this to the screens. In phase 1 there is no login, so every user is anonymous, and an expanded card shows the description, which brief I.1.B/I.1.C keep out of the anonymous list. `ListScreen` has no role information. *Default:* pass `isExpanded` through unchanged (agreed signature); add a one-line KDoc that expanded cards show the description; log in `state.md` and tell Diogo that D-005/D-006 decide whether the toggle is shown to anonymous users (once roles exist). Alternative: ignore `isExpanded` in the anonymous list and always pass `false` (keeps the brief strictly, but the toggle does nothing on this screen in phase 1).
- **Q3. Landscape layout.** The conventions mention splitting layouts in landscape per screen (P07). *Default:* same full-width `LazyColumn` in both orientations; previews prove it scrolls and does not clip. Alternative: `LazyVerticalGrid(GridCells.Adaptive(minSize = 320.dp))`, which still uses `key = { it.id }` but is not a `LazyColumn` as the task says.
- **Q4. Preview data.** *Default:* a private list in `ListScreen.kt` with about 6 tickets, distinct ids (`"p1"`..`"p6"`), only active statuses (OPEN, INFO_REQUESTED, IN_PROGRESS, as the ViewModel delivers), already in anonymous form (no `creatorId`, no location), fixed `createdAt`, enough to scroll in landscape. Alternative: reuse `SampleTickets.all` through `TicketsList(...).publicActive()` and `toAnonymousView()` (only 2 tickets, and the UI would import `data/` in previews).
- **Q5. Empty-state wording.** *Default:* EN "No active issues to show." / PT "Não há ocorrências ativas para mostrar." ("ocorrência" as in the brief), text only, no icon or call to action. Alternative: add an icon, or a hint to report one (the add button belongs to D-005's top bar).
- **Q6. Click payload.** *Default:* `onTicketClick: (String) -> Unit` receiving `ticket.id`, matching `TicketDetailRoute(ticketId: String)` and the F-002 coding plan note. Alternative: `(Ticket) -> Unit` (would tempt D-006 to put the ticket in the route, which D-004 forbids).

Other risks:
- The brief PDF could not be read in this planning session (`pdftoppm` not installed); the brief I.1.B/I.1.C wording above is taken from the F-002 review, which quoted it. Check I.1.B against the PDF at Gate 1 if in doubt.
- F-004, F-005 and F-006 also add files to `ui/screens/`; only F-003 removes `.gitkeep`, so whichever lands first does it. Same branch, same person, so no conflict.
- Manual Android Studio checks (previews, interactive tap, PT, landscape) can only be done by the person; the implementer verifies build and lint only.

## Out of scope
- Collecting the ViewModel, `NavHost` wiring, `Scaffold`, top bar and the expand toggle (D-005, D-006).
- Role-based hiding of the description for anonymous users (Q2; later, with login).
- Real thumbnails with Coil, pull-to-refresh, search, filters by category/status, sorting options.
- Map view of the tickets, operator lists, the private list of a registered citizen.
- Changes to `TicketCard`/`StatusBadge` (F-002 is done; review nits N1/N4 stay as they are).

## Slices
1. **List with data.** `ListScreen` with the `LazyColumn` branch only (`items(tickets, key = { it.id })`, `TicketCard`, `onTicketClick(ticket.id)`), KDoc, private preview data, previews with data (collapsed portrait + landscape, expanded portrait). Remove `ui/screens/.gitkeep`. Build green.
2. **Empty state.** Add `ticket_list_empty` (EN + PT), the `tickets.isEmpty()` branch with the centred message, empty previews (portrait, landscape, PT). Build and lint green.

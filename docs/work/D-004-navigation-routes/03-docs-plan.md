# D-004: Docs plan

## Docs to update
- `docs/architecture/overview.md`:
  - in "Layers", change the `ui/navigation/` wording to "type-safe `@Serializable` routes (D-004)";
  - add a short "Navigation (D-004)" subsection after "ViewModel (D-003)" covering:
    - the four routes in `ui/navigation/Destinations.kt` and the flow they serve;
    - `TicketDetailRoute` carries only `ticketId: String`, and the detail screen takes the ticket from the collected `tickets`;
    - the build setup: the Kotlin Serialization plugin (`version.ref = "kotlin"`) and `kotlinx-serialization-core` 1.9.0, pinned to match Kotlin 2.2.10;
    - the D-006 contract: add `navigation-compose` (suggested 2.9.8), `startDestination = DashboardRoute`, read the id with `toRoute<TicketDetailRoute>()`, and never navigate to the id returned by `reportTicket`.
- `docs/tasks/phase-1/README.md`: no change, unless Gate 1 moves `navigation-compose` into D-004 or D-005 (00-plan Q1). In that case, note which task adds it.
- No coordination message for Francisco is needed. His screens take callbacks, not routes; D-006 maps callbacks to routes.

## Decisions to record
- None (00-plan Q5). Using type-safe routes is the course default, and the version pin is explained in `overview.md`. If Diogo asks for a record, create `docs/decisions/0007-type-safe-navigation-routes.md` covering:
  - **Context:** string routes vs `@Serializable` routes;
  - **Decision:** the id-only detail route;
  - **Consequences:** `navigation-compose` lives in D-006, and serialization stays on 1.9.0 while Kotlin is 2.2.

## Report notes
Add a "D-004: Navigation routes" section to `docs/report/notes.md`:
- **Type-safe navigation:**
  - `@Serializable` route objects and classes (Navigation Compose 2.8+, as in the course) instead of string routes;
  - the compiler checks argument names and types;
  - arguments are rebuilt with `toRoute<T>()`.
- **Only the id in the route:** the back stack never holds ticket content, so there is no stale or personal data in saved state. The screen reads the current ticket from the shared ViewModel.
- **Build setup:**
  - the Kotlin Serialization compiler plugin generates the serializers;
  - the runtime provides `@Serializable`;
  - under AGP 9 built-in Kotlin it is applied like the Compose compiler plugin;
  - the version is pinned to one built for the project's Kotlin version.
- **Improvement opportunities:**
  - deep links to a ticket (e.g. from an FCM notification) with `TicketDetailRoute`;
  - nested graphs per role (anonymous, citizen, operator) in later phases;
  - bump kotlinx-serialization together with Kotlin 2.3.

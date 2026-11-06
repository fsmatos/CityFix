# Phase 1: model + base UI

Source: Notion "Plano de trabalho – Fase 1 (modelo + UI base)". Phase 1 uses local data only, no Firebase. Suggested deadline: **2026-11-01**.

## Decisions (from the plan)
- Classes in English: `Ticket`, `User`, `Citizen`, `Operator`, `Municipality`, `Category`, `Message`, `StatusHistory`, `Attachment`, `TicketStatus`, `Situation`, `Scope`, `AttachmentType`, `UploadState`, `RegistrationStatus`.
- Keep the diagram inheritance: `User` is the base; `Citizen` and `Operator` specialise it. In the Firebase phase the document always stores a `role` field.
- Packages: `model`, `viewmodel`, `data`, `utils`, `ui.navigation`, `ui.components`, `ui.screens`, `ui.theme` (already created).
- Branches: we use `dev/Francisco` and `dev/Diogo` (not the `feature/*` branches in the original plan). IDs here follow the factory scheme: the plan's `F1` is `F-001`, `D1` is `D-001`.

## Tasks
| ID | Plan ID | Owner | Layer | Task | Depends on |
| --- | --- | --- | --- | --- | --- |
| D-001 | D1 | Diogo | model | Domain classes and enums | - |
| D-002 | D2 | Diogo | model | TicketsList and dashboard stats | D-001 |
| D-003 | D3 | Diogo | viewmodel | TicketsViewModel | D-001, D-002 |
| D-004 | D4 | Diogo | ui.navigation | Navigation routes | - |
| D-005 | D5 | Diogo | ui.navigation | MainTopAppBar | D-004 |
| D-006 | D6 | Diogo | ui.screens | MainActivity with Scaffold and NavHost | D-003, D-004, D-005, F-003, F-004, F-005, F-006 |
| D-007 | - | Diogo | model / ui.navigation | Anonymous projection and expand toggle | D-006 |
| F-001 | F1 | Francisco | ui.theme / res | Theme, strings and accessibility base | - |
| F-002 | F2 | Francisco | ui.components | StatusBadge and TicketCard | D-001, F-001 |
| F-003 | F3 | Francisco | ui.screens | ListScreen | D-001, F-002 |
| F-004 | F4 | Francisco | ui.screens | DashboardScreen | D-001, D-002 |
| F-005 | F5 | Francisco | ui.screens | ShowScreen (ticket detail) | D-001, F-002 |
| F-006 | F6 | Francisco | ui.screens | ReportScreen (provisional) | D-001, F-001 |
| F-007 | - | Francisco | ui.screens / viewmodel / utils | Report attachments (photos and videos, P09) | F-006 |

## Suggested order
1. **D-001 first**, then push `dev/Diogo`. Francisco merges it before the tasks that use the model.
2. In parallel: F-001, D-004, then D-002, D-005.
3. Then F-002, then F-003, F-004, F-005, F-006 and D-003.
4. D-006 last: it wires the screens together.

Agree the screen signatures first (plan "Step 0"). As built, and the code is the source of truth: `ListScreen(tickets, isExpanded, onTicketClick)`, `DashboardScreen(stats, onSeeTicketsClick)`, `ShowScreen(ticket?, audience)`, `ReportScreen(viewModel, onSubmit)`, `TicketCard(ticket, isExpanded, onClick)`.
- ViewModel API (D-003) that the screens are wired against in D-006: `tickets: StateFlow<List<Ticket>>`, `stats: StateFlow<DashboardStats>`, `getTicketById(id): Ticket?` and `reportTicket(description, categoryId, categoryName, situation, scope, attachments): String?` (`null` when description, category id or category name is blank after trimming, or `attachments` is empty; the parameter came with F-007). Tickets reach the screens already projected for anonymous users (blank author fields, location rounded to about 1 km), so screens must not show the author. The sample category ids are `pavement`, `lighting` and `furniture`; F-006's provisional category list should reuse them. D-006 creates one shared `TicketsViewModel` above the `NavHost`, takes the detail ticket from the collected `tickets`, and does not open the id returned by a PRIVATE report (it is not in the anonymous list). Details: `docs/architecture/overview.md`, "ViewModel (D-003)".

## Done when
The app navigates dashboard -> list -> detail -> report, keeps state on rotation, shows EN/PT, and has previews in portrait and landscape. Integrate (merge `origin/main` into your dev branch) at least twice a week.

## Left for later phases
Firebase Auth/MFA, Firestore, WorkManager, permissions, geofence, impact detection and FCM.

## Risks to handle in the Firebase phase
1. `Attachment` does not distinguish a report photo from resolution proof (RESOLVED needs proof): add a field (e.g. `phase`) or link it to `StatusHistory`.
2. `User` has no FCM token, and Firestore does not convert inheritance: always store `role`.
3. Anonymous users must not see coordinates and Firestore cannot hide fields inside a document: use a public collection with reduced fields.
4. NIF uniqueness needs one document per NIF created in a transaction.
5. `Municipality` is more general than the brief (one municipality, Pr. República, 10 km): a single document is enough.
6. Reopening (5 days) has no field of its own: compute it from the `StatusHistory` entry with RESOLVED.

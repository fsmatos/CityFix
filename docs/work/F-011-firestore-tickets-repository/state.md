# F-011: Firestore tickets repository and public projection
Branch: dev/Francisco · Base: a9339c9
Remote: origin/dev/Francisco

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-13 | feature (`/feature-dev`), task file `docs/tasks/phase-2/F-011-firestore-tickets-repository.md` |
| Plan | done | 2026-11-13 | `00-plan.md`, `01-coding-plan.md`, `03-docs-plan.md`; 5 slices; Q1-Q8 open for Gate 1 |
| Gate 1: plan approved | done | 2026-11-13 | approved with defaults Q1-Q8 |
| Code | pending | | |
| Review | pending | | |
| Docs | pending | | |
| Pushed | pending | | |
| Merged | pending | | |

## Log
Append-only.
- 2026-11-13: fetched origin; origin/main had 0 commits missing from dev/Francisco; the person chose not to merge (already up to date). Started F-011 from the task file. dev/Francisco contains F-010 (in origin/main at 72037cc) plus a9339c9 (rules kept as undeployed drafts, not yet in main). Firestore runs in test mode (expires 2026-12-10); the drafted rules are not deployed (decision 0012 Q5, amended 2026-11-13).
- 2026-11-13 (Plan): planner read the task file, decisions 0005-0007 and 0009-0012, the overview (incl. "Handover (F-010)"), `firestore.rules`, the `data/`, `model/` and `viewmodel/` code, `CityFixApplication` and `MainActivity`, and the class material (09_Storage, 08_Coroutines_Flows_Services, P07/P08, T8 `AMOVStorageDemo` `MainActivity.kt`). Class shows `getInstance()`, `set`/`get` with success/failure listeners, manual `getString` mapping and `db.batch()`/`commit()` (demo "clear all"); it does not show snapshot listeners or `callbackFlow`. Plan: `FirestoreTicketsRepository` with snapshot listeners wrapped in `callbackFlow` (beyond class, flagged, Q2 offers callbacks instead), ViewModels keep their own `MutableStateFlow`s; audience-aware reads (anonymous -> `publicTickets` only); `reportTicket` batch writes `tickets` + `ticketLocations` + `publicTickets` (PUBLIC) + `stats/summary` increments and returns without awaiting the commit (offline); dashboard from `stats/summary` counters computed as the difference of `TicketsList.stats()` before/after (Q3); `currentUser: User?` stand-in in `CityFixApplication` plus `TicketsViewModel.setUser` as D-009's seam (Q1); delete `LocalTicketsRepository` (Q4); debug-only `seedIfEmpty` with sample categories and tickets (Q5); `ticketLocations` written now (Q6); `stats` rules draft only with the person's OK (Q7); new decision number to confirm with Diogo (Q8). Slices: 1 mapping, 2 repository (unwired), 3 switch, 4 debug seed, 5 rules draft (conditional). Device check list D1-D11 in `01-coding-plan.md`. Diogo to be told: `TicketsViewModel.kt`/`TicketsViewModelFactory.kt` and `TicketPrivacy.kt` (KDoc) change; D-013/F-014 batches must add `statsIncrements`.
- 2026-11-13: Gate 1: approved with defaults. Q1 `CityFixApplication.currentUser` seam (null = anonymous, edited by hand until D-009; creator id `currentUser?.id ?: "dev-citizen-1"`; `TicketsViewModel.setUser` is D-009's hook); Q2 `callbackFlow` over snapshot listeners (beyond class, accepted); Q3 `stats/summary` counters updated in every ticket batch; Q4 delete `LocalTicketsRepository`; Q5 debug-only seed of sample categories and tickets, removed before delivery; Q6 write `ticketLocations` now (location 0,0 until F-012); Q7 draft rules block for `stats/summary`, not deployed; Q8 decision 0013 after checking Diogo has not used it. Diogo must be told before slice 3 (TicketsViewModel/Factory, TicketPrivacy KDoc, repository API, statsIncrements in D-013/F-014).

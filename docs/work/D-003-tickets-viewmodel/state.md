# D-003: TicketsViewModel
Branch: dev/Diogo · Base: 19e7bc5
Remote: not pushed

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-10-29 | Task file: docs/tasks/phase-1/D-003-tickets-viewmodel.md |
| Plan | done | 2026-10-29 | 00-plan, 01-coding-plan, 03-docs-plan; 4 slices (4th only if Q7), no tests |
| Gate 1: plan approved | done | 2026-10-29 | Approved as written; planner recommendations for Q1-Q14 (Q7 yes: slice 4 included) |
| Code | done | 2026-10-29 | 4 slices: f0272b8, 95f1c86, 168f775, 13cacdf; assembleDebug + lint green after each |
| Review | pending | | |
| Docs | pending | | |
| Pushed | pending | | |
| Merged | pending | | |

## Log
Append-only.
- 2026-10-29 Start: task D-003 started on dev/Diogo at 19e7bc5. `git fetch origin` done; origin/main has no commits missing from dev/Diogo, and Diogo chose to skip the merge. Untracked `.idea/` files left alone (IDE state, not part of the task).
- 2026-10-29 Decision (Diogo): no unit tests. The D-003 acceptance check "Unit test with the 3 sample tickets" is struck out in the task file, and the planner was told to drop the test slice. Deleted `app/src/test/.../model/TicketsListTest.kt` (D-002) and the template `ExampleUnitTest.kt`. Removed the test references from overview.md, decision 0005 (Q1 revised), report notes and factory manifest. Kept `androidTest/ExampleInstrumentedTest.kt` and the Gradle test dependencies (template, unused). assembleDebug + lint green. Committed as c401a5d.
- 2026-10-29 Plan: planner wrote 00-plan, 01-coding-plan, 03-docs-plan. Anonymous projection `Ticket.toAnonymousView()` in `model/TicketPrivacy.kt` (also blanks the first history entry's `responsibleId` and attachment uploader ids), sample data in `data/SampleTickets.kt`, `TicketsViewModel` with `tickets`/`stats` StateFlows, `getTicketById`, `reportTicket`. Grouping deferred. No test slice.
- 2026-10-29 Gate 1: approved by Diogo as written; every open question Q1-Q14 takes the planner's recommendation (Q7 yes, so slice 4 is in scope).
- 2026-10-29 Code: implemented the 4 slices of 01-coding-plan, one commit each, `./gradlew assembleDebug` and `./gradlew lint` green before every commit. Slice 1 `f0272b8` (model/TicketPrivacy.kt, `toAnonymousView()`); slice 2 `95f1c86` (data/SampleTickets.kt, data/.gitkeep removed; epoch constants recomputed from their UTC dates and all match the plan); slice 3 `168f775` (viewmodel/TicketsViewModel.kt with `tickets`, `stats`, `getTicketById`; viewmodel/.gitkeep removed); slice 4 `13cacdf` (`reportTicket`). No new dependency: `androidx.lifecycle.ViewModel` and `MutableStateFlow` resolved through the existing classpath, so the slice-3 fallback was not used. Only Android import is `androidx.lifecycle.ViewModel` in viewmodel/. Minor deviations: the sample-1 message has id `sample-1-msg-1` (the plan gave none; `Message` KDoc asks sample data to set distinct ids); the sample times are named private constants with their UTC date in KDoc rather than inline literals; `coarse()` got a one-line KDoc. No tests (Diogo's decision). Not pushed.

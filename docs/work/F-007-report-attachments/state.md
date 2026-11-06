# F-007: Report attachments (photos and videos)
Branch: dev/Francisco · Base: 1487976
Remote: not pushed

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-06 | |
| Plan | done | 2026-11-06 | Revision 3 (shortened, same design as revision 2); 8 slices; Q1, Q3 and Q13 undecided |
| Gate 1: plan approved | done | 2026-11-06 | approved revision 3 with Q13 literal P09 (synchronous copy, accepted exception to the no-blocking rule), Q1 Francisco in slice 7 after Diogo agrees, Q3 nothing more; other questions take their defaults |
| Code | pending | | |
| Review | pending | | |
| Docs | pending | | |
| Pushed | pending | | |
| Merged | pending | | |

## Log
Append-only.
- 2026-11-06: fetched origin; origin/main had 18 commits missing from dev/Francisco (D-006 and D-005 records). The person chose to merge: clean merge 1487976, no conflicts; assembleDebug and lint pass. Started F-007 from `docs/tasks/phase-1/F-007-report-attachments.md`.
- 2026-11-06 Plan: planner wrote `00-plan.md`, `01-coding-plan.md` and `03-docs-plan.md`; 7 slices, no code changed. Checked: the merged manifest has no `CAMERA`; Coil 3.5+ needs Kotlin 2.4 and Compose foundation 1.11+, so 3.4.0 is pinned (POMs); `PickMultipleVisualMedia` requires `maxItems > 1`, and `ACTION_OPEN_DOCUMENT` ignores the limit; `SavedStateHandle` needs no new dependency. Differences from the spec, to confirm at Gate 1: videos in `filesDir/videos` (Q5), event `onMediaPicked(uris)` instead of `onPhotosAttached(paths)` with `ReportViewModel` as `AndroidViewModel` (Q4), no `coil-network-okhttp` (Q2), `MainActivity.kt` gains one line although the task's file list omits it (Q1). P09 is not in the repository, so it was not checked against the slides. No emulator or build was run during planning.
- 2026-11-06: The person supplied `AMov.P09.pdf` before answering Gate 1. Plan checked against all 28 slides: no conflict that changes the slices. Added "Checked against P09" to 00-plan (deliberate differences), removed the "P09 is not in the repository" risk, and added the slide 22 emulator setup to 01-coding-plan. Gate 1 still pending.
- 2026-11-06: Gate 1: revise. After seeing the differences from P09, the person chose to stay close to P09. Notes sent back to the planner. Q1 and Q3 still unanswered.
- 2026-11-06 Plan revision 2: planner rewrote `00-plan.md`, `01-coding-plan.md` and `03-docs-plan.md` to P09's shape; no code changed, nothing built. Copy from the launcher callback in `ReportScreen`, then `onPhotosAttached(savedPaths)`; `onMediaPicked` dropped. `ReportViewModel` needs no `Application` or Context (each need checked in 00-plan); only slice 6 adds `SavedStateHandle`, and only if Q3 keeps it. `object FileUtils`, everything in `filesDir/images`, new `VID_` prefix recovers the type. Slide 27's grid works in the scrolling form because of its height cap; narrow landscape needs its own scroll. Coil 3.0.4 fits the POMs and `coil-video` exists at that version; `decoderFactory` is a `Builder` member there. Running on Compose `ui` 1.11 is unproven, so 3.4.0 is the fallback, checked in slice 4. Pending camera path in `rememberSaveable`. New Q13 on the main-thread copy, default option (c): `viewModel.viewModelScope` + `Dispatchers.IO`, because rotation would cancel `rememberCoroutineScope` and lose the picks. Q4 and Q5 removed. Decision 0009 dropped; short amendment to 0008. 8 slices.
- 2026-11-06 Plan revision 3: Gate 1 asked for shorter plans; the three files were cut with no change to decisions, slices, defaults or question numbers.
- 2026-11-06: Gate 1: revise. The person said the plans are too long. Sent back to the planner to shorten them with no design change. Q1, Q3 and Q13 still unanswered.
- 2026-11-06: Gate 1: approved. Answers: Q13 (a) literal P09, copy on the main thread in the launcher callback, knowing a large video can freeze the UI; Q1 Francisco in slice 7, only after the person confirms Diogo agreed; Q3 nothing more, so 7 slices and `ReportViewModel` stays no-arg. Q2 and Q6-Q12 take their defaults. Plans updated to match.

# D-007: Review

Reviewed: `git diff 14300c8..HEAD` (21af837, be24b9c, 9811fe5) against `00-plan.md`, `01-coding-plan.md`, the task file, decisions 0006/0007, brief I.1.B/I.1.C, `docs/architecture/overview.md` and `kotlin-conventions`. Gate 1 (2026-11-10): Q1 option A, Q2-Q7 as recommended.

Build: `./gradlew assembleDebug` pass; `./gradlew lint` pass, 0 errors, 16 warnings (baseline), none in the three changed files.

## Summary
The code matches the coding plan line for line: `toAnonymousView()` blanks `description`, empties `statusHistory` and keeps `currentStatus`; `MainTopAppBar` has the required `showExpandToggle`; `CityFixApp` passes `canExpandCards = false` to the bar and `canExpandCards && isExpanded` to `ListScreen`. No file outside the plan changed.

Checks made:
- **Every read of `description`, `statusHistory` and `createdAt` in `app/src`** (grep):
  - `TicketCard` reads `description`/`createdAt` only when `isExpanded`, which is now always false on the device.
  - `ShowScreen` reads them only in the REGISTERED/OPERATOR branches (`DescriptionSection`, `StatusHistorySection`), and `MainActivity` passes `DetailAudience.PUBLIC`.
  - `StatusHistorySection` also handles an empty list.
  - `ReportScreen`/`ReportViewModel` read the form's own `description`, not tickets.
- **Projected tickets reach the UI by one path**, `LocalTicketsRepository.loadPublicTickets()`, then `TicketsViewModel.tickets`, then the list and the detail (the detail uses `tickets.firstOrNull`). `getTicketById` projects too. No other screen reads tickets.
- **Nothing reads the history of a projected ticket:**
  - `stats()` runs on the raw `TicketsList` (`loadStats()`), so the dashboard figures cannot change.
  - `publicActive()` sorts by `createdAt` before the projection.
  - `findNearby()` has no caller yet.
  - The status badge reads `currentStatus`.
- **The KDoc contract is complete.** Every `Ticket` and `Attachment` field is listed as dropped, rounded or kept.
- **i18n:** no new strings. **Orientation:** the bar is unchanged apart from the condition, and the new preview uses `@OrientationPreviews`. **Accessibility:** the toggle is removed, not hidden visually, so TalkBack never reaches an inert button. **MVVM:** the bar stays stateless with no `model` import, and the flag is decided at the stateful call site.
- **Personal data for anonymous users:** none reaches the screens. `createdAt` stays in the projection but is not displayed (Gate 1 Q2). The `CAPTURE_<millis>` file names and EXIF make/model/time are recorded as known limits (Q7), and the Docs phase puts them in 0006.

## Blocking
None.

## Should-fix
- **S1. The device and preview acceptance checks have not been run.** `state.md` says the implementer did not run the emulator checks or the Android Studio previews. These cover acceptance checks 1-4 and 6 in `00-plan.md`: no toggle in portrait/landscape and EN/PT, collapsed cards, the detail of `sample-1`/`sample-2`, a new PUBLIC report with a photo, the dashboard at 3/67%/3 days and then 4 total/3 active, and the three `MainTopAppBar` list previews plus the `TicketCard`/`ListScreen` expanded previews. The code reading above supports all of them, but the task file should only tick what was seen.
  **Fix:** before the Docs phase ticks the task file, run the emulator steps in `01-coding-plan.md` slice 2 "Verify" and open the previews. Record the results in the `state.md` Log. Tick only the checks that pass, and leave the others unticked with a note.

## Nit
- **N1. The flag's comment names "the Auth task" without an id** (`MainActivity.kt`, above `val canExpandCards`). `00-plan.md` "Out of scope" names it D-009.
  **Fix:** write "The Auth task (D-009) sets this by role." so the hook is easy to find from the task list. While editing, change the second "so" to a colon or a full stop ("...thumbnail. No expanded cards (D-007).").
- **N2. `Ticket.currentStatus` KDoc says "Denormalised copy of the last [statusHistory] entry ... Update both together"** (`model/Ticket.kt`). Projected tickets now break this on purpose (status with an empty history). `TicketPrivacy.kt` already says so ("a view, not a valid ticket"), but someone reading `Ticket.kt` alone would not know.
  **Fix (later, not in D-007, since `Ticket.kt` is on the do-not-touch list):** add "Anonymous views (`toAnonymousView()`) keep this and drop the history." to that KDoc the next time `Ticket.kt` changes, or note it in the 0006 amendment next to "a view, not a valid ticket".
- **N3. Defence talking point, no code change.** `isExpanded = canExpandCards && isExpanded` looks redundant once the toggle is hidden, because in this build `isExpanded` can never become true. It is there for when the Auth task (D-009) makes the flag depend on the role. A registered user who expanded the cards and then logs out keeps `isExpanded = true` in `rememberSaveable`, and the `&&` still forces the anonymous cards collapsed (failure mode "Toggle state restored as `true`" in `00-plan.md`).
  **Fix:** none needed. Make sure the 0006 amendment or the report notes give this reason in one sentence, so either author can explain it.

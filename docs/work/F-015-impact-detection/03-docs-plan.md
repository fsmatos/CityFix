# F-015 docs plan

## Docs to update
- `docs/architecture/overview.md`:
  - New "Impact detection (F-015)" section: files, total-acceleration thresholds, cooldown, foreground-only lifecycle, prefill route, emulator test commands.
  - "Handover (F-015)":
    - D-011: saving the toggle updates the session, which stops or starts the detector by itself.
    - D-012/D-015: keep a category with id `pavement` once DebugSeed is removed.
    - Diogo: list the `MainActivity` lines that changed.
- `docs/architecture/overview.md` D-010 handover line "D-014 (Diogo), impact detection": mark it done by F-015.

## Decisions to record
- `docs/decisions/0023-impact-detection.md`:
  - Based on the slides' `PotholeDetector`: `TYPE_ACCELEROMETER`, total acceleration outside 6..14 m/s² (the slide's Z axis alone fires constantly when the phone is upright).
  - 30 s cooldown.
  - Foreground only.
  - Snackbar suggestion, never an automatic report.
  - Prefill via the `ReportTicketRoute(fromImpact)` argument.
  - Gate 1 answers.

## Report notes
- `docs/report/notes.md`, new F-015 section:
  - Technique: total acceleration with the slide's 6..14 m/s² limits, cooldown, sensors demo lifecycle.
  - Why not Z alone: an upright phone reads Z near 0, below 6, so it would always fire.
  - No runtime permission needed.
  - Improvements:
    - foreground service for detection with the app closed (P10 `ProximityService` pattern);
    - only the vertical part of the jolt (project on gravity);
    - movement check with GPS speed;
    - threshold set in the profile;
    - attach the detected location and time to the ticket.
- Replace the old improvement line "Impact-detection prefill ... `prefill(situation, categoryId)`" with what was built.

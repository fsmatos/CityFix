# D-007: Anonymous projection and expand toggle

Owner: Diogo (`dev/Diogo`) · Layer: `model` / `ui.navigation` · Phase 1 · Status: code and docs done (2026-11-10); emulator and preview checks pending (review S1)
Depends on: D-006

## Goal
Enforce the anonymous view below the UI and make the list consistent with the detail. Finish before delivery.

## Why
- Brief I.1.B: the anonymous list shows category, situation and thumbnail. I.1.C: the anonymous detail shows only images/videos and a coarse map.
- `toAnonymousView()` still keeps `description` and `statusHistory`, so they are hidden from anonymous users only in the UI (F-005 review S4, decision 0007).
- With D-006 the expand toggle shows the free-text description, which may contain personal data, and the report date in the anonymous list. The detail hides both.

## What to build
- `toAnonymousView()` blanks `description` and clears `statusHistory`, keeping `currentStatus`.
- Decide what the toggle does for anonymous users. Blanking the description alone is not enough: the expanded card would still show "Reported on". Hiding or forcing off the toggle drops D-006's expanded-state check for anonymous users, so decide it at Gate 1.
- Amend decision 0006.

## Files
`model/TicketPrivacy.kt`, `ui/components/TicketCard.kt` or `ui/navigation/MainTopAppBar.kt` and `MainActivity.kt` (per the toggle decision), `docs/decisions/0006-anonymous-projection-phase-1.md`.

## Acceptance checks
- [ ] An anonymous user cannot read a ticket's description, history or date in the list or the detail. *Pending: supported by code reading in review (toggle hidden, cards forced collapsed, PUBLIC detail), not yet seen on the emulator (portrait/landscape, EN/PT, `sample-1`/`sample-2`, a new PUBLIC report with a photo).*
- [x] The projected ticket holds no description or history. *Checked by reading the code and in review: `toAnonymousView()` sets `description = ""` and `statusHistory = emptyList()` and keeps `currentStatus`.*
- [ ] The dashboard stats are unchanged. *Pending: by code reading `loadStats()` runs `stats()` on the raw list, so the figures cannot change; the emulator comparison (3 issues, 67%, 3 days, "Based on 1 repair"; then 4 total / 3 active after a PUBLIC report) has not been run.*

Also pending (review S1): opening the `MainTopAppBar` list previews (with and without the toggle) and the `TicketCard`/`ListScreen` expanded previews in Android Studio. Build and lint pass (0 errors, 16 warnings, baseline). Decision 0006 amended on 2026-11-10.

## References
- Decisions 0006, 0007; `docs/work/D-006-main-activity-nav/review.md` (S1).

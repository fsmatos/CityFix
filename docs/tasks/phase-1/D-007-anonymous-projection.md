# D-007: Anonymous projection and expand toggle

Owner: Diogo (`dev/Diogo`) · Layer: `model` / `ui.navigation` · Phase 1 · Status: to do
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
- [ ] An anonymous user cannot read a ticket's description, history or date in the list or the detail.
- [ ] The projected ticket holds no description or history.
- [ ] The dashboard stats are unchanged.

## References
- Decisions 0006, 0007; `docs/work/D-006-main-activity-nav/review.md` (S1).

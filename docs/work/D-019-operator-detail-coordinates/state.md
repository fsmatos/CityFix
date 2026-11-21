# D-019: Show exact coordinates and attachment authors in the operator detail
Branch: dev/Diogo · Base: e6b34f4
Remote: not pushed

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-21 | Found in the manual checklist, I.3.E detail |
| Plan | done | 2026-11-21 | 00-plan, 01-coding-plan, 03-docs-plan; one slice |
| Gate 1: plan approved | done | 2026-11-21 | Diogo approved; dot decimal, "Adicionado por", keep responsibleNames |
| Code | done | 2026-11-21 | Slice 1 in 20a82b0; build and lint pass (0 errors) |
| Review | | | |
| Docs | | | |
| Pushed | | | |
| Merged | | | |

## Log
- 2026-11-21: merged origin/main into dev/Diogo first (e6b34f4, no file changes; Diogo chose to merge).
- 2026-11-21: Plan (bug mode). Root cause: never implemented; `ExactLocationSection` draws only the map, and `responsibleNames` reaches only the history, not `MediaSection`/`MediaTile`. Approach: reuse the D-013 people lookup (add `attachments.uploadedById` to the watched ids), pass the same map to the tiles ("Added by <name>"), and a coordinates line (Locale.ROOT, 5 decimals) for OPERATOR only. One slice. Open questions Q1-Q4 in 00-plan for Gate 1. Waiting for Gate 1.
- 2026-11-21: Gate 1 approved by Diogo. Q1 keep `responsibleNames` (comment only); Q2 citizen unchanged; Q3 `40.21000, -8.41900` with dot decimal in both languages; Q4 new string "Added by %1$s" / "Adicionado por %1$s".
- 2026-11-21: Code slice 1 (20a82b0): watched ids include attachments.uploadedById (OperatorTicketScreen); ShowScreen passes responsibleNames to DetailsPane -> MediaSection -> MediaTile ("Added by <name>", maxLines 2, ellipsis); ExactLocationSection showCoordinates for OPERATOR only (Locale.ROOT, 5 decimals, TalkBack "Latitude x, longitude y"); KDoc updates incl. watchPeople; operator preview with uploaders, a proof and a landscape preview; 3 strings EN/PT. assembleDebug OK, lint 0 errors (18 existing warnings, none in touched files). No deviations from the coding plan. Pending: manual acceptance checks on device; tell Francisco ShowScreen.kt changed.

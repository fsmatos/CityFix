# D-019: Show exact coordinates and attachment authors in the operator detail
Branch: dev/Diogo · Base: e6b34f4
Remote: not pushed

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-21 | Found in the manual checklist, I.3.E detail |
| Plan | done | 2026-11-21 | 00-plan, 01-coding-plan, 03-docs-plan; one slice |
| Gate 1: plan approved | done | 2026-11-21 | Diogo approved; dot decimal, "Adicionado por", keep responsibleNames |
| Code | | | |
| Review | | | |
| Docs | | | |
| Pushed | | | |
| Merged | | | |

## Log
- 2026-11-21: merged origin/main into dev/Diogo first (e6b34f4, no file changes; Diogo chose to merge).
- 2026-11-21: Plan (bug mode). Root cause: never implemented; `ExactLocationSection` draws only the map, and `responsibleNames` reaches only the history, not `MediaSection`/`MediaTile`. Approach: reuse the D-013 people lookup (add `attachments.uploadedById` to the watched ids), pass the same map to the tiles ("Added by <name>"), and a coordinates line (Locale.ROOT, 5 decimals) for OPERATOR only. One slice. Open questions Q1-Q4 in 00-plan for Gate 1. Waiting for Gate 1.
- 2026-11-21: Gate 1 approved by Diogo. Q1 keep `responsibleNames` (comment only); Q2 citizen unchanged; Q3 `40.21000, -8.41900` with dot decimal in both languages; Q4 new string "Added by %1$s" / "Adicionado por %1$s".

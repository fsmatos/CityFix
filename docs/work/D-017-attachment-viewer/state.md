# D-017: Open photos and play videos in the ticket detail
Branch: dev/Diogo · Base: 7527f97
Remote: not pushed

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-21 | Bug 8 from Francisco's tests |
| Plan | done | 2026-11-21 | |
| Gate 1: plan approved | done | 2026-11-21 | Diogo approved, anonymous video out of scope |
| Code | done | 2026-11-21 | Slice 1; build and lint green |
| Review | skipped | 2026-11-21 | Small UI fix, deadline; manual check on the phone instead |
| Docs | done | 2026-11-21 | overview.md components list |
| Pushed | pending | | |
| Merged | pending | | |

## Log
- 2026-11-21: plan written. Root cause: `MediaTile` in ShowScreen has no click and the app has no player.
- 2026-11-21: Gate 1 approved by Diogo (anonymous video playback stays out of scope).
- 2026-11-21: slice 1 coded: `AttachmentViewer` (Dialog + Coil photo / VideoView + MediaController), `MediaTile` clickable with `Role.Button` and an open label, viewer state by attachment id in `rememberSaveable`. Lint UseKtx fixed (`toUri()`). Deviation: none. Manual checks on the phone still to do.
- 2026-11-21: docs: `AttachmentViewer` added to the components list in `docs/architecture/overview.md`. Report note in 03-docs-plan.

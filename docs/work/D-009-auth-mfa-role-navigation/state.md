# D-009: Login, MFA, password recovery and role navigation
Branch: dev/Diogo · Base: f0131b1
Remote: origin/dev/Diogo

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-11 | After merging origin/dev/Francisco (F-010) ahead of main |
| Plan | pending | | |
| Gate 1: plan approved | pending | | |
| Code | pending | | |
| Review | pending | | |
| Docs | pending | | |
| Pushed | pending | | |
| Merged | pending | | |

## Log
Append-only.

- 2026-11-11 Sync: origin/main had nothing new for dev/Diogo. F-010 (needed by D-009) was reviewed and pushed on origin/dev/Francisco but not merged into main; Diogo chose to merge origin/dev/Francisco into dev/Diogo now (f0131b1, 16 commits). Conflicts only in `docs/architecture/overview.md` (2 hunks) and `docs/report/notes.md` (1 hunk), resolved by keeping both sides. Decision number clash: F-010 and D-008 both used 0011; D-008's record renumbered to `0012-ticket-lifecycle-rules.md` and its references updated (Francisco's files untouched). assembleDebug and lint pass: 0 errors, 18 warnings (F-010 recorded 17; the extra one is a newer-version warning, all 18 are version or unused-resource warnings). `app/google-services.json` present locally (git-ignored).
- 2026-11-11 Found while merging, not D-009 scope: F-010's handover (overview "Handover (F-010)") says the reopen window, CLOSED-on-read and 24 h warning are computed from a server-written `resolvedAt` field, and that every status change has `responsibleId` = the caller, with participants closing after 5 days. D-008 (decision 0012) computes them from the last RESOLVED history entry and writes the automatic close with `responsibleId = "system"`. To reconcile in F-011 / D-013 with Francisco.
- 2026-11-11 Start: task file `docs/tasks/phase-2/D-009-auth-mfa-role-navigation.md` (target 2026-11-13).

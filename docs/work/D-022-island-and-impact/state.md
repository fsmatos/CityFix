# D-022: Island stuck on the full list, impacts not detected in a car
Branch: dev/Diogo · Base: 4de36cb
Remote: not pushed

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-22 | from chat, no task file |
| Plan | done | 2026-11-22 | 00/01/03 written |
| Gate 1: plan approved | done | 2026-11-22 | approved; Q1 = A, Q2 = yes |
| Code | done | 2026-11-22 | `e21d2fe`, `8e663e9`, `6db0648` |
| Review | done | 2026-11-22 | main session, 0 blocking (review.md) |
| Docs | done | 2026-11-22 | overview, 0023, 0028; new report awaits Francisco |
| Pushed | pending | | |
| Merged | pending | | |

## Log
Append-only.
- 2026-11-22: Island bug reproduced as anonymous on a temporary clean emulator with the unchanged code; citizen not run (needs a login). Car failure not reproduced, cause not proven. Location crash seen once on that emulator (ApiException 20). No code edited.
- 2026-11-22: Gate 1 approved by Diogo. Q1 = A (vertical axis, old limits 14 and 6, no speed gate). Q2 = yes (crash fix).
- 2026-11-22: Code done. Build and lint pass before each code commit (0 errors, 17 warnings).
- 2026-11-22: Emulator tests (API 37, clean temporary AVD). Island, anonymous: list from Dashboard and from Active, detail, tab switches, Back: correct. Impact: service started with run-as (no login), phone still: rest, +3, -3 and 11 sideways -> nothing; +5, -5, +14 -> notification; in front -> snackbar; 30 s cooldown respected. These ran before the crash fix, which does not touch those files. Crash: Play services killed during a request -> ApiException 20 caught, "location could not be found" shown.
- 2026-11-22: Not tested: signed-in citizen and operator (need a login), real phone, car, screen off. Those acceptance checks stay open.
- 2026-11-22: Deviation: review by the main session, not the reviewer agent.
- 2026-11-22: Deviation from 03-docs-plan, Diogo's decision: the report is not replaced. The new docx and PDF are added with the suffix `_NOVO` for Francisco to read and approve (changes listed in review.md). Keep only the approved version before the merge into main and the ZIP.

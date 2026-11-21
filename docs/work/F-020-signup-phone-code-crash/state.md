# F-020: Sign-up "Send Code" crash
Branch: dev/Francisco · Base: 04c7b48
Remote: not pushed

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-21 | bug, task file `docs/tasks/phase-2/F-020-signup-phone-code-crash.md` |
| Plan | done | 2026-11-21 | 00/01/03 plans, 2 slices |
| Gate 1: plan approved | done | 2026-11-21 | approved as planned |
| Code | done | 2026-11-21 | slice 1 `8e20138` + review fixes; build and lint pass; device check pending |
| Review | done | 2026-11-21 | review.md: 0 blocking, 3 should-fix, 3 nits |
| Docs | done | 2026-11-21 | 0026 amendment, overview bullet, report notes |
| Pushed | pending | | |
| Merged | pending | | |

## Log
Append-only.
- 2026-11-21: Started from the person's `/bug-fix` request (task file written from it). Step 0: 0 commits behind `origin/main`; the person chose not to merge. Untracked `.idea/` files and `docs/testing/` left as they were. Plans written by the main session directly, without the planner agent (small fix, context already gathered). Awaiting Gate 1.
- 2026-11-21: Gate 1 approved as planned.
- 2026-11-21: Review (reviewer agent) of 04c7b48..HEAD (8e20138): no blocking findings. Should-fix: PHONE_IN_USE after Resend on code step is silent; record the Linked branch deviation (isBusy=false instead of Unit) in Login/CredentialChange VMs; record build/lint/device results. See review.md.
- 2026-11-21: Slice 1 (`8e20138 fix(F-020): send sign-up phone code without MFA-only option`), coded by the main session directly (not the implementer agent). `assembleDebug` and `lint` pass. Deviation: in Login/CredentialChange the `Linked` branch sets `isBusy = false` instead of the planned `Unit`, so a spinner can never stay on (the branch is not reached on MFA paths).
- 2026-11-21: Review fixes (should-fix 1 and two nits): a PHONE_IN_USE after a code-less link now goes back to the phone step with the message, as in `confirmCode` (Resend on the code step could hide it); `requestCode` builds in one chain with `.apply { if (mfa) requireSmsValidation(true) }`; the "Not reached" comment says why it returns a Failure. Build and lint pass again. Not run on a device yet: the reproduction, the typed-code link, and MFA sign-in for a pre-F-018 account.
- 2026-11-21: Docs (slice 2): amendment line in decision 0026, a "Sign-up phone code" bullet in the overview's auth section, an F-020 section in `docs/report/notes.md`.

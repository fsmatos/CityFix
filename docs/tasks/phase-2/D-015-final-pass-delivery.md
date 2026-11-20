# D-015: Final pass: landscape, accessibility, strings, delivery ZIP

Owner: Diogo (`dev/Diogo`), Francisco checks · Layer: all · Phase 2 · Status: to do · Target: 2026-11-21
Depends on: all coding tasks

## Goal
Check the whole app against brief V and build the ZIP for Nonio.

## Why
- Brief V: English default plus another language; portrait and landscape without loss; dynamic top bars, icons and FABs; accessibility; MVVM with stateless/stateful split.
- Brief VI: one ZIP named `AMOV2627.2018013532.2018044613`, without `.gradle`, `build`, `app/build`, `local.properties`, with the PDF report. Wrong format or leftover folders cost up to 5% each.

## What to build
- **Screen sweep** (every screen, every role): rotate with data entered (state kept), check PT on a device set to Portuguese, TalkBack labels (`contentDescription`, touch targets 48 dp, contrast), FAB/top bar per context. Fix what is small; list the rest in the report's improvements.
- **Strings:** every translatable string in `values/` and `values-pt/` (`app_name` is `translatable="false"`, so no PT copy is needed; Gate 1 Q3); no hard-coded text (`lint` HardcodedText).
- **Clean-up:** remove seed/debug buttons, test documents and `LocalTicketsRepository` (already deleted by F-011); `./gradlew lint` with no errors.
- **Test accounts** in the Firebase project (citizen active, citizen pending, operator) listed in the report.
- **ZIP:** from a clean checkout of `main`: `git archive` or copy, check the excluded folders are absent, add the PDF (F-017), make sure `google-services.json` is included so the project builds; unzip in a temp folder and run `./gradlew assembleDebug` once to prove it builds.

## Acceptance checks
- [ ] Every screen works in portrait and landscape, EN and PT.
- [ ] `lint` clean, `assembleDebug` passes from the unzipped copy.
- [ ] `app/google-services.json` copied into the ZIP by hand (a clean checkout does not have it, decision 0012), and the unzipped copy builds with it.
- [ ] ZIP name and contents match brief VI; PDF inside.
- [ ] Submitted on Nonio before 2026-11-22 23:59.

**Status (2026-11-20):** no box is fully met yet. Code slices 1-6 are done and reviewed (`docs/work/D-015-final-pass-delivery/`): clean-up, strings parity after the F-015/F-016 merge, phone fix, citizen report FAB, ZIP script. `lint` has 0 errors (17 version/`UseKtx` warnings, kept on purpose) and `assembleDebug` passes on `dev/Diogo`, but not yet from an unzipped copy. Device checks covered part of the app only (no TalkBack order, no registered-citizen screens, no landscape FAB). Open: run the ZIP script (never run, execution policy), Q8 with Francisco, Q4/Q7 console changes, R4 Maps key check, test accounts, the PDF (F-017), the ZIP from `main`, Nonio upload. Decision: [0025](../../decisions/0025-delivery-package.md).

## References
Brief V, VI; CLAUDE.md non-negotiable rules (ZIP contents).

# 0025: Delivery package

Status: accepted (D-015, Gate 1 on 2026-11-17, Q5-Q9 as recommended; Q8 part 2 still open). The script exists (`d8e5881`, `2e7862a`) but has not run yet (2026-11-20). Code detail: `docs/architecture/overview.md`, "Delivery (D-015)". The number 0025 was the next free one after F-015 (0023) and F-016 (0024).

## Context
- Brief VI: one ZIP named `AMOV2627.<nr1>.<nr2>`, without `.gradle`, `build`, `app/build` or `local.properties`, with the PDF report. Each format error or leftover folder costs up to 5%; -5% per hour late after 2026-11-22 23:59.
- `app/google-services.json` is ignored by git ([decision 0012](0012-firebase-plan-notifications-close-maps.md) Q7), so a clean checkout cannot build. The Maps key is committed in the manifest ([decision 0014](0014-location-gate-grouping-map.md)).
- Both authors build on Windows 10 (PowerShell 5.1, built-in `tar.exe`).

## Decision
- **Name and layout (Q8).** `AMOV2627.2018013532.2018044613.zip` with `CityFix/` (the tracked files of `main`) and `AMOV2627.2018013532.2018044613_Relatorio.pdf` at the root. The whole tracked repo goes in by default; leaving out `.claude/`, `CLAUDE.md` and `docs/work/` (switch `-ExcludeAgentFiles`) needs Francisco's agreement, not asked yet.
- **Source: `git archive` of `main`** after both dev branches are merged and pushed, never the working folder. With `-Ref main` the script fails if `main` differs from `origin/main`, and it prints the full SHA to record.
- **`google-services.json` and the PDF are parameters**, copied into the staging copy, never committed.
- **Zip with `tar.exe -a`** (bsdtar), which stores `/` separators; the script lists the entries and checks the exclusions a second time.
- **Verification build:** the ZIP is unzipped into a new temp folder and `gradlew.bat assembleDebug` runs there, as the grader would.
- **A PowerShell script, `scripts/make-delivery-zip.ps1` (Q6).** It writes only to `%TEMP%` and the output folder. The same steps can be done by hand if the script cannot run.
- **Test accounts in the PDF only (Q9).** The docs list roles and what each account shows; e-mails, passwords and MFA test numbers with codes are never in git.
- **Test mode (Q7).** Firestore and Storage stay in test mode (no rules deployed, decision 0012 Q5). The expiry, 2026-12-10, is to be moved to 2027-03-01 in the console by Diogo before the upload (not done on 2026-11-20); otherwise the report states the 2026-12-10 date.
- **Report entry as a citizen FAB (Q5).** Recorded in the overview ("App shell (D-006)", "Top bar (D-005)"), not here: a small UI choice.

### Rejected
- Zipping the working folder: risks `build/`, `.gradle/` and `local.properties` getting in.
- Committing `google-services.json`: against decision 0012 Q7 and the "never commit secrets" rule.
- `Compress-Archive` on PowerShell 5.1: older versions write `\` separators, which break unzipping on macOS/Linux.
- `git archive --format=zip` alone: it cannot add the ignored `google-services.json` or the PDF.

## Consequences
- The script has never run (execution policy `AllSigned` on Diogo's machine). It must run once before 2026-11-22, or the ZIP is made by hand (review round 2 S2).
- A `tar.exe` ZIP of Windows files has no Unix mode: on macOS/Linux `chmod +x gradlew` may be needed (stated in the PDF).
- The grader's debug key has another SHA-1: SMS MFA may show a reCAPTCHA step (R3), and the Maps key must have no Android-app restriction (R4, console check still open).
- After the test-mode expiry every Firestore and Storage request is refused, so the app shows empty lists.

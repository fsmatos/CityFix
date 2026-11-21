# F-021: Bugs found in manual testing
Type: bug · Task file: docs/tasks/phase-2/F-021-testing-bug-batch.md
Labels: none

## Problem
Seven issues from the person's manual test on 2026-11-21. Code reading, emulator logcat and a read of `publicTickets` (Firestore REST) give the causes below.

## Root cause (per bug)
1. **SMS at login.** The test account was created before F-018, so its phone is an enrolled MFA factor; Firebase always asks for it (decision 0026 kept this on purpose). New accounts do not get it.
2. **Photo rotation.** `AttachmentViewer` uses a `Dialog` that fits the system bars (`decorFitsSystemWindows` default true); after the rotation the new dialog window is inset by the bars, so it is not edge-to-edge.
3. **Anonymous "In progress".** Not reproduced in data: no `publicTickets` doc is IN_PROGRESS, and the In progress "Pavement" ticket on the emulator (16:34) has no public doc, so it is PRIVATE (hidden from anonymous users by design, decision 0006). Code: the anonymous query and `isActive` already include IN_PROGRESS, and every transition updates the mirror.
4. **Tab returns to old details.** `openTab` uses `saveState`/`restoreState`: Home saves the Active stack (list + details) and the Active tab restores it. On the restored details the screen is outside every tab, so the highlight stays on the last tab (Home).
5. **Send Code closes the app.** Not reproduced: no app crash in either emulator's logcat; F-020's fix passed on the Mi 9T Pro. Likely an older build, or on an emulator the reCAPTCHA browser tab opening (looks like the app closing).
6. **No photo in validation list.** `PendingCitizensScreen` always shows a person icon; only the detail screen loads `citizen.photo`.
7. **Dark-mode message.** The outcome snackbar uses the theme's inverse colours; cause to be confirmed on the emulator (dark mode) before changing colours.

## Approach
1. Ask the person (Q1): leave as is and recreate the test account, or unenrol the old factor once after a successful SMS sign-in (then the account links the phone like a new one).
2. `DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)`; the close button already uses `safeDrawingPadding`.
3. No code change; re-test with a PUBLIC ticket moved to In progress.
4. A tab tap always opens the tab's own page: drop `saveState`/`restoreState` in `openTab` (keep `popUpTo(home)` and `launchSingleTop`). Then the highlight follows.
5. No code change now; the person re-runs on the current build with logcat (Q2). Fix in this task if a stack trace shows up.
6. `PendingCitizenCard`: `AsyncImage` of `citizen.photo` in the thumbnail box (crop), the icon when null.
7. Reproduce in dark mode; then set explicit snackbar colours (or move it so the island/FAB does not cover it), whichever the screenshot shows.

## Acceptance checks
- [ ] (2) Photo open, rotate both ways: still full screen.
- [ ] (4) Active > details > Home > Active: Active list, Active highlighted.
- [ ] (6) Pending list shows the photo; no photo shows the icon.
- [ ] (7) Report created / joined in dark mode: message readable.
- [ ] (3, 5) Re-test results recorded in state.md.
- [ ] `assembleDebug` and `lint` pass.

## Risks and open questions
- Q1: bug 1, recreate the account (no code) or one-time unenrol (small repository + login change, needs a decision 0026 amendment)?
- Q2: bug 5, on which device/build did it happen? Please reproduce with the current build.
- Bug 4 changes the tab behaviour for every role (no more remembered sub-pages per tab).

## Out of scope
Firestore rules, MFA for new accounts.

## Slices
1. `fix(F-021): keep photo viewer full screen after rotation` (2).
2. `fix(F-021): open the tab's own page on tab tap` (4).
3. `fix(F-021): show citizen photo in the pending list` (6).
4. `fix(F-021): readable report message in dark mode` (7).
5. Bug 1 only if Q1 = unenrol: `fix(F-021): unenrol old SMS factor after sign-in`.
6. `docs(F-021): record testing fixes`.

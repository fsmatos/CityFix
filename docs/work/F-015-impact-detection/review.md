# F-015 review

Diff 3805597..b4f0675. Lifecycle start/stop, ViewModel ownership of the sensor, strings EN/PT, the route change (all 5 `ReportTicketRoute` uses: lines 207, 294, 399-400, 546) and the prefill are correct. No anonymous data exposure, no new permission. MVVM rule holds: the effect only calls callbacks, the sensor lives in `utils` + ViewModel.

## Blocking
None.

## Should-fix
1. **Acceptance checks not run** (`00-plan.md` Acceptance checks; `state.md` Code log). Emulator jolt + cooldown, background stop/start in Logcat, setting off / operator never registers, prefill, landscape, TalkBack: all left for the person. Fix: run them (`adb emu sensor set acceleration 0:0:20`, then `0:0:9.8`), tick the boxes in `00-plan.md`, log the result in `state.md` before pushing.
2. **"Report" when the form is already open** (`MainActivity.kt:207`). The snackbar lasts ~10 s. If the user taps "+" during that time and then taps "Report", `launchSingleTop` reuses the open form and its ViewModel (`fromImpact = false`). Nothing is prefilled, and the Q9 rule "no suggestion while reporting" is broken at action time. Fix: before navigating, check `navController.currentDestination?.hasRoute<ReportTicketRoute>() != true` (same check as `toTopBarScreen`).
3. **KDoc and commit wording oversell "vertical"** (`utils/ImpactDetector.kt:15`). The total-acceleration check reacts to a sharp jolt in any direction, for example putting the phone down hard or lifting it quickly (total < 6). The brief I.2.F asks for "eixo vertical". Fix: say "a sharp jolt in any direction (a vertical bump included)". In decision 0023 and the report, say plainly that this deviates from the brief and why (the deviation is already in `03-docs-plan.md`; make sure it gets written).

## Nit
1. `utils/ImpactDetector.kt:34-36`: `stop()` logs "ImpactDetector stop" even when `start` registered nothing (no accelerometer). This can confuse acceptance check 2. Fix: log only inside `accelerometer?.let { }`, the same way `start` does.
2. `MainActivity.kt:202`: `snackbarScope` belongs to the composition, so rotating the screen during the 10 s cancels the suggestion. Older snackbars behave the same way. Fix: none needed in code. Add one line to the report's improvements.
3. `MainActivity.kt:197-211`: when the suggestion is skipped on the report screen (Q9), the 30 s cooldown still starts. This is the intended behaviour. Fix: add a short comment so the authors can explain it at the defence.
4. `01-coding-plan.md` Interfaces still shows `Z_MAX`/`Z_MIN` and `event.values[2]`. The deviation is logged in `state.md`. Fix: add a one-line note under Interfaces pointing to that log entry.

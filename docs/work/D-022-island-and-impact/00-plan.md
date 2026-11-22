# D-022: Island stuck on the full list, impacts not detected in a car
Type: bug · Task file: none (Diogo, chat of 2026-11-22)
Labels: deadline-today

## Problem
1. Citizen and anonymous: the island has the problem D-020 fixed for the operator (wrong tab lit,
   cannot get back to the tab's own screen).
2. Impact detection gave no notification in a real car test. The authors decided to go back to how
   it was before the minimum added in F-022.

## Reproduction
1. Reproduced on 2026-11-22 on a clean emulator (API 37), anonymous, at Pr. Republica:
   Dashboard -> "See active issues": **Active** lights up; tap Dashboard (three times, also after
   tapping Active in between): the list stays, Active stays lit. Citizen: same code path, not run.
2. Reported from the car; not reproduced.

## Root cause
1. `BottomTab.ACTIVE.screens` contains `TopBarScreen.LIST`, but the list also opens from the
   Dashboard. There it is saved in the Dashboard's stack, so a Dashboard tap restores it, and the
   list selects Active again: the "tap the selected tab to pop" rule of D-020 never applies. The
   operator has neither tab, so the D-020 check on the operator did not show it.
2. Not proven. F-022 made detection stricter twice:
   - speed gate: needs GPS speed > 2 m/s from a fix < 10 s old;
   - limits: was total > 14 or < 6 (about +4.2 / -3.8 vertical), now |vertical| > 6.
   Removing only the speed gate still misses vertical jolts between about 4 and 6 m/s².

## Outcome
The lit tab is the one the list was opened from and a tap on it leaves the list. A jolt is
suggested without any minimum speed, with the limits chosen at Gate 1.

## Acceptance checks
- [ ] Anonymous: Dashboard -> list: Dashboard lit; tap it: the dashboard.
- [ ] Anonymous: Active -> See more -> list: Active lit; tap it: the Active screen.
- [ ] Anonymous: list -> detail -> other tab -> back: right tab lit, a second tap gives its root.
- [ ] Citizen: the same three checks, plus Profile.
- [ ] Operator: Panel -> detail -> Profile -> Panel still works as in D-020.
- [ ] Citizen, detection on, phone still: a jolt over the limit -> snackbar in front, notification behind.
- [ ] Car test on a real phone (by the authors).
- [ ] assembleDebug and lint pass.

## Approach
1. Remove `LIST` from `ACTIVE.screens`: like the detail, it keeps the tab it was opened from.
2. Remove the speed gate. Limits, Q1:
   - A (proposed): vertical axis with the old limits (`g + a_v` > 14 or < 6);
   - B: exact old rule, total acceleration > 14 or < 6 (not only vertical, against brief I.2.F);
   - C: only remove the speed gate, keep |vertical| > 6.
   Background service, notification and 30 s cooldown stay in all three.
3. Q2, found while reproducing: `currentLocation()` lets `ApiException` escape and the app crashes
   (seen when Play services disconnected). Return null, which the three callers already handle.

## Architecture impact
None.

## Failure modes
- No speed gate: picking up or dropping the phone can suggest a report (it only suggests).
- App in front shows a snackbar, not a notification (by design since F-022).
- The service still needs precise location; without it nothing is detected.
- Screen off: some phones pause the accelerometer. Not verified.

## Risks and open questions
- Q1 limits: A, B or C.
- Q2 include the crash fix?
- The report (docx and PDF) states 2 m/s and 6 m/s²: both must change before delivery.

## Out of scope
Geofence, notification channels, wake locks, push and merge into main.

## Slices
1. Island. 2. Impact. 3. Location crash (if Q2 = yes). 4. Docs and report.

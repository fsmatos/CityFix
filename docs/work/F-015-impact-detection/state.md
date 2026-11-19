# F-015: Impact detection (accelerometer)
Branch: dev/Francisco · Base: cf9ff1d
Remote: origin/dev/Francisco

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-19 | feature, task file `docs/tasks/phase-2/F-015-impact-detection.md` |
| Plan | done | 2026-11-19 | 3 slices, Q1-Q9 answered (defaults) |
| Gate 1: plan approved | done | 2026-11-19 | approved by the person |
| Code | done | 2026-11-19 | 3 slices; emulator checks left for the person |
| Review | done | 2026-11-19 | review.md: 0 blocking, 3 should-fix, 4 nits |
| Docs | pending | | |
| Pushed | pending | | |
| Merged | pending | | |

## Log
Append-only.
- 2026-11-19: fetched origin; origin/main had 0 commits missing from dev/Francisco. origin/dev/Diogo has 80 commits not in main (D-011 to D-015); the person chose to start from main as it is. Started F-015 from the task file (target was 2026-11-18).
- 2026-11-19 (Plan): wrote 00-plan, 01-coding-plan, 03-docs-plan. Read brief I.2.A/F and V, class 08 (sensor streams, only as flows), P10 (`DisposableEffect` + lifecycle observer, `ProximityService`), Contacts_#3 (no SensorManager anywhere in class material: flagged as beyond class). Approach: `utils/ImpactDetector` (`TYPE_ACCELEROMETER`, low-pass gravity, projection on gravity, 1.5 g threshold, 30 s cooldown) behind `ImpactViewModel`, foreground-only via a lifecycle observer, snackbar "Report" -> `ReportTicketRoute(fromImpact = true)` prefilled with Pavement + DAMAGE. Deviations from the task file proposed at Gate 1: threshold 1.5 g instead of 2.5 g (Q3); the route argument instead of a `prefill()` event. Pavement id `pavement` depends on DebugSeed (Q9). Awaiting Gate 1.
- 2026-11-19: decision number changed to 0023: origin/dev/Diogo already uses 0019-0022 (D-011 to D-014).
- 2026-11-19 (Re-plan): the person added class slides 10/11 "Permissions_Sensor_Navigation" and the `SideEffect_BR_Maps_Sensors_Permissions` demo. They show a `PotholeDetector` (`TYPE_ACCELEROMETER`, `SENSOR_DELAY_GAME`, Z axis outside 6..14 m/s²) and a `DisposableEffect` + lifecycle observer for sensors. Plan now follows them: dropped the low-pass gravity filter and projection and the 1.5 g threshold; no longer "beyond class". Questions now Q1-Q9. P11 (Firebase/FCM) is for F-016.
- 2026-11-19: the person accepted all recommended answers for Q1-Q9. Awaiting Gate 1.
- 2026-11-19 (Gate 1): plan approved.
- 2026-11-19 (Code, slice 1): added `utils/ImpactDetector` (slide PotholeDetector + callback + 30 s cooldown), `ImpactViewModel` and factory; not wired. Build and lint pass, no new lint issue.
- 2026-11-19 (Code, slice 2): `ReportTicketRoute(fromImpact)` data class, passed via the factory; ReportViewModel selects DAMAGE and, while no category is chosen, the `pavement` category. Build and lint pass. The manual emulator check with `ReportTicketRoute(true)` was not run by the implementer (no emulator); left for the person.
- 2026-11-19 (Code, slice 3): added `ui/components/ImpactDetectionEffect` (shape of the demo `Sensors.kt`, including its `registered`-style guard as `started`), wired it in MainActivity with `ImpactViewModel`, the `detectImpacts` flag and a long snackbar with "Report" -> `ReportTicketRoute(fromImpact = true)`; strings EN/PT. Build and lint pass, no new lint issue. Deviation: MainActivity grew ~31 lines (with imports and comments) instead of ~15. Emulator/TalkBack acceptance checks not run by the implementer (no emulator); left for the person.
- 2026-11-19 (Deviation, Q1/Q2): the slide's `z > 14 || z < 6` fires every 30 s when the phone is upright (Z near 0, e.g. the emulator's default `0:9.8:0`). The person chose to use the total acceleration sqrt(x²+y²+z²) with the same 6..14 m/s² limits: orientation-independent, still catches vertical jolts. Constants renamed `MIN`/`MAX`; 03-docs-plan updated.
- 2026-11-19 (Review): reviewed 3805597..b4f0675 against the plans, brief I.2.F and kotlin rules; wrote review.md. No blocking issue. Should-fix: run and tick the acceptance checks; the snackbar "Report" action reuses an already-open form (no prefill); the KDoc says "vertical" but the total-acceleration check reacts to jolts in any direction (brief deviation to explain). 4 nits.
- 2026-11-19 (Review fixes): S2 snackbar "Report" no longer navigates when the form is already open; S3 KDoc says it also reacts to non-vertical jolts (docs plan asks 0023 and the report to say so); nits: `stop()` logs only with an accelerometer, comment on the cooldown when skipped, coding-plan pointer to the deviation, rotation line added to report improvements. S1 (device checks) next.

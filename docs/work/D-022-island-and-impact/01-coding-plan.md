# D-022 coding plan

## Files
| Path | Change | Why |
| --- | --- | --- |
| `ui/navigation/MainBottomBar.kt` | `ACTIVE.screens` = `ACTIVE_TICKETS` only | the list opens from two tabs |
| `utils/ImpactDetector.kt` | no `speed`; limits per Q1 | detect in the car |
| `utils/ImpactMath.kt` | only if Q1 = B: delete | unused with the total rule |
| `data/TrackingService.kt` | remove `lastSpeed`, `lastFixAt`, `FIX_MAX_AGE_MS`, their collector | dead without the gate |
| `data/LocationRepository.kt` | Q2: catch `ApiException` in `currentLocation` | no crash |

## Interfaces
`ImpactDetector(context)`; `start`/`stop` unchanged.

## Steps
1. `fix(D-022): keep the list in the tab it was opened from` - emulator, anonymous sequence.
2. `fix(D-022): detect impacts without a minimum speed` - emulator jolt if a citizen session exists.
3. `fix(D-022): handle location failures without crashing` - build and lint.
4. `docs(D-022): update impact and navigation docs`.
Run verify before each code commit.

## Conventions
Small diff; comments only where the rule is not obvious.

## Do not touch
openTab, OperatorGeofence, the service lifecycle and location request, Firebase rules, main.

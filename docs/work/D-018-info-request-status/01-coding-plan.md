## Files
| Path | Change | Why |
| --- | --- | --- |
| `app/src/main/java/pt/isec/amov/cityfix/model/InfoRequest.kt` | new | Pending request, status before, answered rule |
| `app/src/main/java/pt/isec/amov/cityfix/ui/components/InfoRequestCard.kt` | new | Question, who asked, waiting/answered |
| `app/src/main/java/pt/isec/amov/cityfix/ui/screens/OperatorTicketScreen.kt` | edit | Card; "Resume work" label in ActionsRow and dialog |
| `app/src/main/java/pt/isec/amov/cityfix/ui/screens/TicketDetailScreen.kt` | edit | Card for participants |
| `app/src/main/res/values/strings.xml`, `values-pt/strings.xml` | edit | New strings |

## Interfaces
See 00-plan Approach. `InfoRequestCard(request: InfoRequest, answered: Boolean, modifier)`.

## Steps
1. Slice 1, commit `fix(D-018): show information request status and resume work label`. Verify: assembleDebug + lint, manual checks on the phone.

## Conventions
kotlin-conventions; model stays pure (no Android imports); strings in resources.

## Do not touch
firestore.rules, TicketLifecycle transitions, repositories, notifications.

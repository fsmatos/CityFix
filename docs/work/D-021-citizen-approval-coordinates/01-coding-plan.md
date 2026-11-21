## Files
| Path | Change | Why |
| --- | --- | --- |
| `app/src/main/java/pt/isec/amov/cityfix/utils/CoordinatesFormat.kt` | New: public `formatCoordinate(value: Double): String` | One shared formatter for both operator screens |
| `app/src/main/java/pt/isec/amov/cityfix/ui/screens/ShowScreen.kt` | Remove the private `formatCoordinate` (and its KDoc, ~line 317-318); add `import pt.isec.amov.cityfix.utils.formatCoordinate` | Use the shared formatter; behaviour unchanged |
| `app/src/main/java/pt/isec/amov/cityfix/ui/screens/CitizenApprovalScreen.kt` | In `CitizenLocation` (~line 235), `else` branch after `TicketMap`: add the coordinates `Text`; add the `formatCoordinate` import; update the KDoc | The acceptance line |

## Interfaces
```kotlin
// utils/CoordinatesFormat.kt
package pt.isec.amov.cityfix.utils

import java.util.Locale

/** D-019: 5 decimals (about 1 m) with a dot in every language, so "lat, lng" stays unambiguous. */
fun formatCoordinate(value: Double): String = String.format(Locale.ROOT, "%.5f", value)
```

Addition in `CitizenLocation`, after `TicketMap(...)` inside `else`:
```kotlin
val lat = formatCoordinate(location.latitude)
val lng = formatCoordinate(location.longitude)
val coordinatesDescription = stringResource(R.string.cd_ticket_coordinates, lat, lng)
Text(
    text = stringResource(R.string.ticket_coordinates, lat, lng),
    style = MaterialTheme.typography.bodyMedium,
    modifier = Modifier.semantics { contentDescription = coordinatesDescription },
)
```
`semantics` and `contentDescription` are already imported in `CitizenApprovalScreen.kt`.
KDoc of `CitizenLocation` becomes: "The registered location on the exact map (F-012) with its
coordinates as text (D-021), or "Location not available" for (0, 0)."

## Steps
1. Slice 1: create `utils/CoordinatesFormat.kt`; in `ShowScreen.kt` delete the private function and
   its comment, add the import (keep `java.util.Locale`: still used); in `CitizenApprovalScreen.kt`
   add the import, the `Text` block and the KDoc tweak.
   Commit: `feat(D-021): show coordinates in pending citizen detail`
   Verify: `./gradlew assembleDebug`, `./gradlew lint` (no new warnings); on a device as operator:
   pending citizen detail in portrait and landscape, EN and PT, TalkBack on the line; a (0, 0)
   citizen if one exists; the operator ticket detail line unchanged.

## Conventions
- Strings only from resources (reused `ticket_coordinates`, `cd_ticket_coordinates`); no new strings.
- Keep the helper a plain top-level function like `utils/DateFormatting.kt`.
- Commit message without attribution.

## Do not touch
- Anything else in `ShowScreen.kt` (Francisco's file: `ExactLocationSection`, `showCoordinates`,
  F-018's `onMapTouchChange`).
- `strings.xml` (EN/PT), `TicketMap`, ViewModels, repositories, Firestore/Storage rules.
- `RegisterScreen`, `ReportScreen`, `ProfileScreen` coordinate texts.

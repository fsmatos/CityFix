## Files
| Path | Change | Why |
| --- | --- | --- |
| `app/src/main/java/pt/isec/amov/cityfix/ui/screens/OperatorTicketScreen.kt` | Add `t.attachments.map { it.uploadedById }` to `ids` (line ~117); KDoc "history responsibles, uploaders" | Every uploader's profile is watched, so `responsibleNames` has their names |
| `app/src/main/java/pt/isec/amov/cityfix/viewmodel/OperatorTicketViewModel.kt` | KDoc of `watchPeople` only: mention attachment uploaders | Keep the comment true; no code change |
| `app/src/main/java/pt/isec/amov/cityfix/ui/screens/ShowScreen.kt` | Pass names to `DetailsPane` -> `MediaSection` -> `MediaTile`; "Added by" line; `showCoordinates` in `ExactLocationSection`; KDoc updates; operator preview with uploaders and a proof | The two missing items |
| `app/src/main/res/values/strings.xml` | 3 strings next to `ticket_map_exact_title` / `ticket_media_proof` | EN text |
| `app/src/main/res/values-pt/strings.xml` | Same 3 strings | PT text |

## Interfaces
No public signature changes. Private composables in `ShowScreen.kt`:
```kotlin
private fun DetailsPane(ticket, audience, now, locale, modifier = Modifier, responsibleNames: Map<String, String> = emptyMap())
private fun MediaSection(attachments: List<Attachment>, modifier = Modifier, uploaderNames: Map<String, String> = emptyMap())
private fun MediaTile(attachment, position, onClick, modifier = Modifier, uploaderName: String? = null)
private fun ExactLocationSection(location: Coordinates, showCoordinates: Boolean, modifier = Modifier)
private fun formatCoordinate(value: Double): String = String.format(Locale.ROOT, "%.5f", value)
```
Strings:
| Name | EN | PT |
| --- | --- | --- |
| `ticket_coordinates` | `Coordinates: %1$s, %2$s` | `Coordenadas: %1$s, %2$s` |
| `cd_ticket_coordinates` | `Latitude %1$s, longitude %2$s` | `Latitude %1$s, longitude %2$s` |
| `ticket_media_added_by` | `Added by %1$s` | `Adicionado por %1$s` |

## Steps
### Slice 1: `fix(D-019): show coordinates and attachment authors in the operator detail`
1. `OperatorTicketScreen.kt`: `setOf(t.creatorId) + t.followerIds + t.statusHistory.map { it.responsibleId } + t.attachments.map { it.uploadedById }`. Blank ids are already skipped by `watchPeople`.
2. `ShowScreen.kt`, `ShowScreen`: pass `responsibleNames = responsibleNames` to both `DetailsPane` calls (landscape named arg; portrait add the named arg).
3. `DetailsPane`: new last param `responsibleNames`; `SectionCard { MediaSection(ticket.attachments, uploaderNames = responsibleNames) }`.
4. `MediaSection`: new param `uploaderNames`; in the loop `uploaderName = uploaderNames[attachment.uploadedById]`.
5. `MediaTile`: after the "Resolution proof" text, `if (uploaderName != null) Text(stringResource(R.string.ticket_media_added_by, uploaderName), bodySmall, onSurfaceVariant, TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)`. The column already merges descendants, so TalkBack reads it with the tile. Update the KDoc ("operator view only, D-019").
6. `LocationPane`: `ExactLocationSection(ticket.location, showCoordinates = audience == DetailAudience.OPERATOR)`.
7. `ExactLocationSection`: inside the `else` (location set), after `TicketMap`, when `showCoordinates`:
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
   (`Coordinates` has `latitude` and `longitude`, `model/Coordinates.kt`.) KDoc: "coordinate text only for operators (brief I.3.E, D-019)".
8. Top-of-file KDoc of `ShowScreen`: replace "Never shows ... uploader ids" sentence so it says ids are never shown, and `responsibleNames` (operator only) also names the attachment uploaders; OPERATOR also gets the coordinates text.
9. `ShowScreenOperatorPreview`: attachments with `uploadedById = "c-1"` and one `Attachment(id = "preview-op-proof", phase = AttachmentPhase.RESOLUTION, uploadedById = "op-1")`; add a landscape `@Preview`.
10. Strings in both files.
11. Imports: `androidx.compose.ui.text.style.TextOverflow` (Locale is already imported).

Verify: `./gradlew assembleDebug`, `./gradlew lint`; previews (operator, full, public, privacy) render; then manual run of the acceptance checks in `00-plan.md` on device, portrait and landscape, EN and PT, TalkBack on.

## Conventions
- Strings in `values/` and `values-pt/`; no hard-coded text (the format pattern `"%.5f"` is not user-visible text).
- Stateless composables; no ViewModel or Firebase in `ShowScreen`.
- Plain code, one helper, short KDoc saying "D-019" so the authors can explain it.
- Commit message with the task ID, no attribution.

## Do not touch
- `MainActivity.kt` and `TicketDetailScreen.kt` call sites (PUBLIC and REGISTERED stay unchanged).
- `TicketPrivacy.kt`, repositories, Firestore mapping and rules.
- `AttachmentViewer`, `AttachmentGallery`, `ResolveSection`.
- `state.md` is not committed by the implementer beyond the work-item flow.

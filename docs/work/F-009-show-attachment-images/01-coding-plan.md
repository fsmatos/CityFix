## Files
| Path | Change | Why |
| --- | --- | --- |
| `app/src/main/java/pt/isec/amov/cityfix/ui/components/AttachmentImage.kt` | new | One shared composable that draws a photo or video frame from `Attachment.file` (like `ContactAvatar` in Contacts_#3) |
| `app/src/main/java/pt/isec/amov/cityfix/ui/components/AttachmentGallery.kt` | `AttachmentTile` calls `AttachmentImage`; drop the moved model/badge code and imports | Keeps the model logic in one place |
| `app/src/main/java/pt/isec/amov/cityfix/ui/components/TicketCard.kt` | finish the person's edit: pass the `Attachment?`, use `AttachmentImage` | The card showed only the placeholder |
| `app/src/main/java/pt/isec/amov/cityfix/ui/screens/ShowScreen.kt` | finish the person's edit: `MediaTile` uses `AttachmentImage`; label rule; KDoc | The tile showed only icons |
| `gradle/libs.versions.toml` | (slice 3) `exifinterface = "1.4.2"` and the `androidx-exifinterface` library | ExifInterface for GPS stripping |
| `app/build.gradle.kts` | (slice 3) `implementation(libs.androidx.exifinterface)` | Same |
| `app/src/main/java/pt/isec/amov/cityfix/utils/FileUtils.kt` | (slice 3) `removeImageLocation(path): Boolean` | Blocking helper next to the other file helpers |
| `app/src/main/java/pt/isec/amov/cityfix/data/LocalTicketsRepository.kt` | (slice 3) call it for IMAGE attachments in `reportTicket` | Single point on IO, data layer |

## Interfaces
```kotlin
// ui/components/AttachmentImage.kt
/** Photo, or first video frame with a play badge, cropped to fill [modifier]'s bounds; placeholder when file is blank or fails. */
@Composable
fun AttachmentImage(
    attachment: Attachment,
    contentDescription: String?,
    modifier: Modifier = Modifier,
)

// utils/FileUtils.kt (slice 3)
/** Blocking. Clears the GPS EXIF tags of the image at [path] in place; false if the format cannot be written or IO fails. */
fun removeImageLocation(path: String): Boolean
```
`TicketThumbnail(attachment: Attachment?)` stays private in `TicketCard.kt`.

## Steps
1. **Extract `AttachmentImage`**. Create the file: a `Box(modifier)` that holds, when `attachment.file.isBlank()`, an `Icon(ic_image_placeholder, tint = onSurfaceVariant)` centred, else an `AsyncImage(model, contentDescription, placeholder, error, ContentScale.Crop, Modifier.fillMaxSize())`. The model comes from the `remember(attachment.file, isVideo)` block moved from `AttachmentGallery`. If `attachment.type == VIDEO`, add the play badge moved from `AttachmentGallery` (32dp, `surface` circle, `contentDescription = null`). Use `attachment.type` for the video check, as the gallery does. In `AttachmentTile`, replace the model, `AsyncImage` and badge with `AttachmentImage(attachment, description, Modifier.fillMaxWidth().aspectRatio(1f))`; the remove button stays. Remove the imports that are no longer used.
   Commit `refactor(F-009): extract attachment image component`. Stage only `AttachmentImage.kt` and `AttachmentGallery.kt`; leave the person's two edited files unstaged.
   Verify with `assembleDebug` and `lint`. On a device, the report form gallery looks the same for a photo and for a video.
2. **Card and detail**. `TicketCard`: `TicketThumbnail(ticket.attachments.firstOrNull())`. The thumbnail keeps its 64dp clipped `surface` box. When there is no attachment, it shows the placeholder `Icon` (as before). Otherwise it calls `AttachmentImage(attachment, stringResource(R.string.ticket_thumbnail_description), Modifier.fillMaxSize())`. Remove the unused imports (`CircleShape`, `ContentScale` and `AsyncImage` if they are no longer used). `ShowScreen.MediaTile`: delete `var attachmentFile` and the `when`; the box body becomes `AttachmentImage(attachment, contentDescription = null, Modifier.fillMaxSize())`. Wrap the "Not available yet" `Text` in `if (attachment.file.isBlank())`. Rewrite the KDoc: the tile draws the photo or video frame and falls back to the icon and "Not available yet" when there is no file. Remove the `Icons`/`PlayArrow`/`Icon`/`ContentScale`/`AsyncImage`/`painterResource` imports if they are no longer used. Previews keep blank files, so they show placeholders, which is fine.
   Commit `fix(F-009): show attachment images on ticket card and detail`. This commit includes the person's work-in-progress in these two files, as instructed.
   Verify with `assembleDebug`, `lint`, and the Reproduction steps plus acceptance checks 1-8 on an emulator.
3. **(Q1 = yes) EXIF GPS**. Add the dependency. `FileUtils.removeImageLocation`: `try { val exif = ExifInterface(path); GPS_LOCATION_TAGS.forEach { exif.setAttribute(it, null) }; exif.saveAttributes(); true } catch (_: Exception) { false }`. `GPS_LOCATION_TAGS` is a private list: `TAG_GPS_LATITUDE`, `_LATITUDE_REF`, `_LONGITUDE`, `_LONGITUDE_REF`, `_ALTITUDE`, `_ALTITUDE_REF`, `_TIMESTAMP`, `_DATESTAMP`, `_PROCESSING_METHOD`, `_AREA_INFORMATION` (Q3). In `LocalTicketsRepository.reportTicket`, before building the ticket (already inside `withContext(Dispatchers.IO)`), run `attachments.filter { it.type == AttachmentType.IMAGE }.forEach { FileUtils.removeImageLocation(it.file) }`. The result is ignored (Q2 default), with a comment that says so. Update the class KDoc.
   Commit `fix(F-009): strip photo location before storing ticket`.
   Verify with `assembleDebug`, `lint`, and the Q1 acceptance check. If no device writes GPS, record in `state.md` that the check was not run.

## Conventions
- Stateless component: data in, no state; `remember` only for the derived `ImageRequest`, as overview "Shared components" allows.
- Coil 3 `AsyncImage`, as taught; no new image library.
- Accessibility: the card keeps its existing description. The tile image is decorative (`null`) because the merged tile label already says "Photo"/"Video". The play badge is decorative.
- No new strings. No `!!`. No blocking IO on the main thread: `removeImageLocation` runs only inside the repository's IO context.
- KDoc says why, and is short.

## Do not touch
- `model/` (`Attachment`, `TicketPrivacy.kt`), `SampleTickets.kt`, `ReportViewModel.kt`, `ReportScreen.kt`, `AttachmentStorage.kt`.
- `google-services.json`, Firestore/Storage rules, `.idea/` files (untracked, not ours).
- Do not revert, reformat or separately commit the person's edits; they go into slice 2.

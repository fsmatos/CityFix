# F-007: coding plan
Revision 3 (2026-11-06), with the Gate 1 answers applied; the reasons are in 00-plan. Paths are under `app/src/main/`; Kotlin files under `java/pt/isec/amov/cityfix/`.

## Files
| Path | Change |
| --- | --- |
| `gradle/libs.versions.toml`, `app/build.gradle.kts` | `coil` version, `coil-compose`, `coil-video` |
| `AndroidManifest.xml` | `FileProvider` `<provider>`, as slide 9 |
| `res/xml/file_paths.xml` | New: `<files-path name="images" path="images/" />` |
| `utils/FileUtils.kt` | New `object FileUtils` |
| `viewmodel/ReportUiState.kt` | Attachment fields, `isValid`, `ReportSubmission`, KDoc |
| `viewmodel/ReportViewModel.kt` | New events, `onCleared` |
| `ui/components/AttachmentGallery.kt` | New: staggered grid with remove |
| `ui/screens/ReportScreen.kt` | Launchers, `attachPicked`, `tempCameraPath`, `AttachmentsSection`, previews; remove `MediaPendingSection` |
| `res/values/strings.xml`, `res/values-pt/strings.xml` | New keys; remove `report_media_pending` |
| `viewmodel/TicketsViewModel.kt` | **Diogo's, slice 7 only, after Q1** |
| `MainActivity.kt` | **Diogo's, slice 7 only, after Q1** |

## Interfaces
`FileUtils` (bodies follow slides 11-12):
```kotlin
object FileUtils {
    /** Blocking (Q13). Null on any failure, including a null stream; deletes a partial file. */
    fun copyUriToInternalStorage(context: Context, sourceUri: Uri): String?
    fun createTempImageFile(context: Context): Pair<File, Uri>
    fun isVideoPath(path: String): Boolean
}
```
Type from `contentResolver.getType` (`video/*` is a video, anything else an image); extension from `MimeTypeMap`, fallback `mp4` or `jpg`.

`ReportUiState`, after `scope`:
```kotlin
const val MAX_REPORT_ATTACHMENTS = 3
val attachments: List<Attachment> = emptyList()
@StringRes val attachmentsError: Int? = null
@StringRes val attachmentsNotice: Int? = null
val remainingAttachments: Int   // computed
val canAddAttachment: Boolean   // computed: slots free, not submitted
```
`isValid` gains `attachments.isNotEmpty() && attachmentsError == null`; `ReportSubmission` gains `attachments: List<Attachment>`.

`ReportViewModel`:
```kotlin
fun onPhotosAttached(paths: List<String>)   // appends; clears the error and the notice
fun onAttachmentCopyFailed()
fun onAttachmentLimitReached()
fun onCameraUnavailable()
fun onRemoveAttachment(attachment: Attachment)
override fun onCleared()
```
- `Attachment(id = file name, type from isVideoPath, file = path)`.
- Removing the last attachment sets `attachmentsError`, as deleting the description does.

`AttachmentGallery`:
```kotlin
@Composable
fun AttachmentGallery(
    attachments: List<Attachment>,
    onRemove: (Attachment) -> Unit,
    modifier: Modifier = Modifier,
    removeEnabled: Boolean = true,
)
```
Grid as slide 27, keyed by `id`. Each item is a `Box` with the image, a play badge on videos, and a 48dp `Close` `IconButton` at `TopEnd` on a `surface` background. `placeholder` and `error` are `ic_image_placeholder`. Video model: `ImageRequest.Builder(context).data(path).decoderFactory(VideoFrameDecoder.Factory()).build()`.

`ReportScreen`:
- Contracts are `remember`ed. The camera callback reads `tempCameraPath` into a local and clears it; it attaches on success with a non-empty file, otherwise deletes.
- The camera launch catches `ActivityNotFoundException` and `SecurityException`.
- `ReportContent` and `DetailsPane` gain `onAddFromGalleryClick`, `onTakePhotoClick`, `onRemoveAttachment`, before `onSubmitClick`.
- `AttachmentsSection` replaces `MediaPendingSection`: heading, hint, gallery when not empty, two buttons (`enabled = state.canAddAttachment`), error, notice through `ErrorText`.
- Previews: with attachments, full, notice; add a narrow landscape spec for 2 columns.

Both pickers call this plain helper; an empty list means the picker was cancelled:
```kotlin
// Synchronous on the main thread as P09 slide 26: accepted exception, see 00-plan Q13.
private fun attachPicked(viewModel: ReportViewModel, context: Context, uris: List<Uri>) {
    if (uris.isEmpty()) return
    val accepted = uris.take(viewModel.uiState.value.remainingAttachments)
    val savedPaths = accepted.mapNotNull { FileUtils.copyUriToInternalStorage(context, it) }
    viewModel.onPhotosAttached(savedPaths)
    if (savedPaths.size < accepted.size) viewModel.onAttachmentCopyFailed()
    else if (accepted.size < uris.size) viewModel.onAttachmentLimitReached()
}
```

Slice 7, `TicketsViewModel.reportTicket`: new last parameter `attachments: List<Attachment>` with no default; add `attachments.isEmpty()` to the `return null` check; in `Ticket(...)`:
```kotlin
attachments = attachments.map {
    it.copy(uploadState = UploadState.PENDING, uploadedById = LOCAL_USER_ID)
},
```
`MainActivity.kt` gains `attachments = submission.attachments,` in the call and nothing else. Both change in one commit.

## Strings
| Key | EN | PT |
| --- | --- | --- |
| `report_attachments_hint` | Add up to %1$d photos or videos. | Adicione até %1$d fotografias ou vídeos. |
| `report_attachments_gallery` | Add from gallery | Adicionar da galeria |
| `report_attachments_camera` | Take a photo | Tirar fotografia |
| `report_error_attachments` | Add at least one photo or video. | Adicione pelo menos uma fotografia ou um vídeo. |
| `report_attachments_limit` | You reached the limit, so the extra files were not added. | Atingiu o limite, por isso os ficheiros a mais não foram adicionados. |
| `report_attachment_copy_failed` | A file could not be added. Try again. | Não foi possível adicionar um ficheiro. Tente novamente. |
| `report_camera_failed` | The camera could not be used. | Não foi possível usar a câmara. |
| `cd_attachment_photo` | Photo %1$d | Fotografia %1$d |
| `cd_attachment_video` | Video %1$d | Vídeo %1$d |
| `cd_remove_attachment_photo` | Remove photo %1$d | Remover fotografia %1$d |
| `cd_remove_attachment_video` | Remove video %1$d | Remover vídeo %1$d |

## Steps
Each: `assembleDebug` and `lint`, then one commit. "Verify" points at the 00-plan acceptance checks.
1. **Dependencies and FileProvider.** `build(F-007): add Coil and the camera FileProvider`
   Verify: `:app:dependencies` leaves `kotlin-stdlib` 2.3.20, `runtime` and `ui` 1.11.0, `foundation` 1.10.4 unchanged (stop if any rises); manifest check.
2. **File helpers.** `feat(F-007): add local attachment file helpers`
   Verify: build and lint.
3. **State and events**, without the `isValid` term. `feat(F-007): hold report attachments in the form state`
   Verify: a report can still be submitted.
4. **Attachments section with gallery.** `feat(F-007): pick report photos and videos from the gallery`
   Verify: first Coil run (fallback in 00-plan); grid, limit, copy and rotation checks.
5. **Camera.** `feat(F-007): take a report photo with the camera`
   Verify: camera, cancel and rotation checks; `adb shell am kill pt.isec.amov.cityfix` with the camera open.
6. **Required rule.** `feat(F-007): require at least one report attachment`
   Verify: task check 2.
7. **Ticket creation. Do not start until the person confirms Diogo agreed (Q1).** `feat(F-007): store attachments on the new ticket`
   Verify: detail tiles; a PRIVATE report still confirms.

After slice 7, run all the checks in EN and PT, in both orientations, with TalkBack.

Emulator setup for slices 4 and 5 (P09 slide 22): set the AVD's back camera to VirtualScene and cold-boot it. Drag sample photos and a short video onto the emulator, then index them:
```bash
adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Download/
```
Inspect `/data/data/pt.isec.amov.cityfix/files/images` in Device Explorer (slide 23).

## Conventions
`.claude/skills/kotlin-conventions` and `.claude/rules/kotlin.md`. `attachPicked` keeps its comment, so the review sees the exception is decided. Record any deviation in `state.md`. Commits carry no attribution lines.

## Do not touch
- `model/`, `data/`, `ui/navigation/`, `ShowScreen.kt`, `TicketCard.kt`, `ListScreen.kt`, `DashboardScreen.kt`.
- `backup_rules.xml`, `data_extraction_rules.xml`, `docs/decisions/0006-*`.
- Compose BOM, Kotlin, lifecycle, activity and navigation versions; Firebase and keys.

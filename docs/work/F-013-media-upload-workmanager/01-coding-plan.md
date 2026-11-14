# F-013 coding plan

Defaults of the Q list in `00-plan.md` are assumed. If Gate 1 changes one, adapt the step it touches and record the change in `state.md`.

## Files
| Path | Change | Why |
| --- | --- | --- |
| `gradle/libs.versions.toml` | `work = "2.12.0"`, library `androidx-work-runtime-ktx` (`androidx.work:work-runtime-ktx`); library `coil-network` (`io.coil-kt.coil3:coil-network-okhttp`, `version.ref = "coil"`) | brief IV; Coil loads https URLs (P09) |
| `app/build.gradle.kts` | `implementation(libs.androidx.work.runtime.ktx)`, `implementation(libs.coil.network)` | same |
| `utils/ImageCompression.kt` (new) | `object ImageCompression { compressImage(...); videoThumbnail(...) }` + private `scaleDown`, `writeJpeg` | brief IV/V compression and thumbnails |
| `utils/VideoMetadata.kt` (new, slice 5) | `object VideoMetadata { removeLocation(source, target): Boolean }` | decision 0010 (video location) |
| `utils/FileUtils.kt` | add `mediaFile(context, name): File`; delete `removeImageLocation` and `GPS_TAGS` (Q5) | new file names; EXIF strip replaced by re-encoding |
| `data/AttachmentUploader.kt` (new) | `prepare(attachment, uploadedById)` and `enqueue(documentPath, ticketId, attachment, addPublicThumbnail)` | one entry point for F-013 and D-013 |
| `data/UploadWorker.kt` (new) | `CoroutineWorker`: putFile + downloadUrl, transaction, delete local files, retry / FAILED | brief IV, class 08 slide |
| `data/FirestoreMapping.kt` | `toAttachment()` / `toDocument()` internal; `DocumentSnapshot.attachmentsWith(id, change)`; `toPublicTicketDocument()` keeps only UPLOADED thumbnails | worker update; no local path in `publicTickets` |
| `data/FirestoreTicketsRepository.kt` | constructor `(firestore, uploader)`; participant check before prepare; `prepare` instead of the EXIF loop; `enqueue` after each `commit()` | wiring |
| `CityFixApplication.kt` | `val attachmentUploader by lazy { AttachmentUploader(this) }`; `FirestoreTicketsRepository(firestore, attachmentUploader)` | container (decision 0009) |
| `ui/components/AttachmentImage.kt` | draw `thumbnail` when set (image request, play badge kept for videos) | the list uses the thumbnail |
| `ui/screens/ShowScreen.kt` | `MediaTile`: "Upload pending" / "Upload failed" line | upload-state indicator (Q8) |
| `res/values/strings.xml`, `res/values-pt/strings.xml` | `ticket_media_upload_pending`, `ticket_media_upload_failed` | brief V (EN default + PT) |

## Interfaces
```kotlin
// utils/ImageCompression.kt (blocking: call on Dispatchers.IO)
object ImageCompression {
    const val MAX_SIDE = 1600; const val QUALITY = 80
    const val THUMB_SIDE = 256; const val THUMB_QUALITY = 70
    /** Photo at [source] -> JPEG [target] (<= 1600 px, upright, no EXIF) and JPEG [thumbnail] (<= 256 px). False if it cannot be decoded or written. */
    fun compressImage(source: String, target: File, thumbnail: File): Boolean
    /** A frame of the video at [source] -> JPEG [thumbnail] (<= 256 px). False on failure. */
    fun videoThumbnail(source: String, thumbnail: File): Boolean
}

// utils/VideoMetadata.kt (slice 5, blocking)
object VideoMetadata {
    /** Copies the tracks of [source] into a new MP4 [target] without location metadata (MediaExtractor + MediaMuxer, rotation kept). False on failure. */
    fun removeLocation(source: String, target: File): Boolean
}

// utils/FileUtils.kt
fun mediaFile(context: Context, name: String): File   // filesDir/images/<name>

// data/AttachmentUploader.kt
class AttachmentUploader(context: Context) {
    /** New UUID id, compressed file + thumbnail in filesDir/images, original deleted; PENDING, or FAILED (original kept) when compression/cleaning fails. Runs on Dispatchers.IO. */
    suspend fun prepare(attachment: Attachment, uploadedById: String): Attachment
    /** One OneTimeWorkRequest<UploadWorker>, network CONNECTED, exponential backoff 30 s. Call after the batch that stores [attachment] in [documentPath]. */
    fun enqueue(documentPath: String, ticketId: String, attachment: Attachment, addPublicThumbnail: Boolean)
}

// data/UploadWorker.kt
class UploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result
    companion object { const val KEY_DOCUMENT = "document"; KEY_TICKET_ID; KEY_ATTACHMENT_ID; KEY_IS_VIDEO; KEY_FILE; KEY_THUMBNAIL; KEY_PUBLIC_THUMBNAIL; MAX_ATTEMPTS = 5 }
}

// data/FirestoreMapping.kt
/** The attachments array with [change] applied to the entry with [attachmentId], as documents; null when no entry has that id. */
internal fun DocumentSnapshot.attachmentsWith(attachmentId: String, change: (Attachment) -> Attachment): List<Map<String, Any>>?
internal fun Map<*, *>.toAttachment(): Attachment
internal fun Attachment.toDocument(): Map<String, Any>
```
Storage paths (decision 0010): `tickets/$ticketId/$id.jpg`, `tickets/$ticketId/$id.mp4`, `tickets/$ticketId/${id}_thumb.jpg`. Local names: `filesDir/images/$id.jpg|mp4`, `${id}_thumb.jpg`.

## Steps

### Slice 1: compression helpers
- `ImageCompression.compressImage`:
  1. Read the size with `BitmapFactory.Options().apply { inJustDecodeBounds = true }`.
  2. Start `sample = 1` and double it while `max(w, h) / (sample * 2) >= MAX_SIDE`.
  3. `decodeFile(source, Options().apply { inSampleSize = sample }) ?: return false`.
  4. Rotate when `ExifInterface(source).rotationDegrees != 0` (`Bitmap.createBitmap(bitmap, 0, 0, w, h, Matrix().apply { postRotate(...) }, true)`).
  5. `scaleDown(bitmap, MAX_SIDE)` -> `writeJpeg(target, QUALITY)`; `scaleDown(..., THUMB_SIDE)` -> `writeJpeg(thumbnail, THUMB_QUALITY)`.
  6. Wrap the whole function in `try { ... true } catch (_: Exception) { false }`, like `FileUtils`.
  - `scaleDown` returns the bitmap itself when it already fits, otherwise `Bitmap.createScaledBitmap(bitmap, newW, newH, true)` keeping the aspect ratio.
  - `writeJpeg` is `file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it) }` (T8 demo).
- `videoThumbnail`: `MediaMetadataRetriever().apply { setDataSource(source) }.frameAtTime`, then `scaleDown` + `writeJpeg`, `release()` in `finally`, false on null or exception.
- `FileUtils.mediaFile(context, name) = File(mediaFolder(context), name)`.
- KDoc: say which parts are as in class (T8 `compress`, P10 `decodeFile` / `createScaledBitmap`) and which are beyond class (`inSampleSize`, rotation, `MediaMetadataRetriever`).
- Commit: `feat(F-013): add image compression and thumbnails`.
- Verify: `./gradlew assembleDebug`, `./gradlew lint` (0 errors).

### Slice 2: prepare media at submit
- `AttachmentUploader(context)` keeps `context.applicationContext`.
- `prepare` (in `withContext(Dispatchers.IO)`):
  - `id = UUID.randomUUID().toString()`; `thumb = FileUtils.mediaFile(ctx, "${id}_thumb.jpg")`.
  - IMAGE: `target = mediaFile("$id.jpg")`, `ok = compressImage(attachment.file, target, thumb)`.
  - VIDEO: `target = mediaFile("$id.mp4")`. In slice 2, `ok = File(attachment.file).renameTo(target)`. Slice 5 replaces this with `VideoMetadata.removeLocation`. Then `if (ok) videoThumbnail(target.path, thumb)` (a missing thumbnail alone does not fail the video: `thumbnail = null`).
  - On success, delete the original (`File(attachment.file).delete()` for an image; the rename already moved the video). Return `attachment.copy(id = id, file = target.path, thumbnail = thumb.path or null, uploadState = PENDING, uploadedById = uploadedById)`.
  - On failure, delete the partial target and thumb, and return `attachment.copy(uploadState = FAILED, uploadedById = uploadedById)` (the original path and id kept).
- `FirestoreTicketsRepository(firestore, uploader)`:
  - delete the EXIF `forEach`, and with it `FileUtils.removeImageLocation` and `GPS_TAGS` (Q5);
  - create path: `val prepared = attachments.map { uploader.prepare(it, creatorId) }`, then build the ticket with `prepared`;
  - join path: move the `mine` query to the top of `joinTicket` (before preparing), then prepare and build `reportDocument(..., prepared)`. So `joinTicket` receives the raw attachments and the report fields, not a ready map. Keep the KDoc in step with this.
- `CityFixApplication`: `attachmentUploader by lazy`; pass it to the repository.
- `toPublicTicketDocument()`: `.filter { it.phase == AttachmentPhase.REPORT && it.uploadState == UploadState.UPLOADED }`. Update the KDoc (thumbnails are added by the upload worker).
- `AttachmentImage`:
  - `val thumbnail = attachment.thumbnail`;
  - model = `thumbnail` when it is not null, else the current branch (video decoder or `file`);
  - `remember(attachment.file, thumbnail, isVideo)`;
  - the placeholder test becomes `attachment.file.isBlank() && thumbnail == null`;
  - KDoc: "draws the thumbnail when there is one".
- Commit: `feat(F-013): compress report media and make thumbnails at submit`.
- Verify: build + lint. On the emulator, report a camera photo and a gallery video:
  - `adb shell run-as pt.isec.amov.cityfix ls -l files/images` shows `<uuid>.jpg`, `<uuid>_thumb.jpg`, `<uuid>.mp4`, `<uuid>_thumb.jpg`, and no `CAPTURE_` or `IMG_` file of this report;
  - the card shows the thumbnail;
  - the Firestore doc has UUID ids and local `thumbnailPath`;
  - `publicTickets.thumbnails` is `[]`.

### Slice 3: upload worker (not enqueued yet)
- Version catalog + Gradle: `work-runtime-ktx` 2.12.0.
- `FirestoreMapping`: make `toAttachment` / `toDocument` internal and add `attachmentsWith`:
  - read `getMapList("attachments").map { it.toAttachment() }`;
  - if none has `attachmentId`, return null;
  - otherwise map the matching one through `change`, and everything through `toDocument()`.
- `UploadWorker.doWork()`:
  ```kotlin
  val app = applicationContext as CityFixApplication   // as ProximityService in P10
  val docRef = app.firestore.document(documentPath)
  return try {
      val fileUrl = upload(app.storage, "tickets/$ticketId/$attachmentId.${if (isVideo) "mp4" else "jpg"}", file, if (isVideo) "video/mp4" else "image/jpeg")
      val thumbUrl = thumbnail?.let { upload(app.storage, "tickets/$ticketId/${attachmentId}_thumb.jpg", it, "image/jpeg") }
      app.firestore.runTransaction { tx ->
          val list = tx.get(docRef).attachmentsWith(attachmentId) {
              it.copy(file = fileUrl, thumbnail = thumbUrl, uploadState = UploadState.UPLOADED)
          } ?: throw IllegalStateException("attachment not found")   // batch not on the server yet -> retry
          tx.update(docRef, "attachments", list)
          if (addPublicThumbnail && thumbUrl != null) {
              tx.update(app.firestore.collection(PUBLIC_TICKETS).document(ticketId), "thumbnails", FieldValue.arrayUnion(thumbUrl))
          }
      }.await()
      File(file).delete(); thumbnail?.let { File(it).delete() }
      Result.success()
  } catch (e: Exception) {
      Log.w(TAG, "upload failed (attempt $runAttemptCount)", e)
      if (runAttemptCount + 1 >= MAX_ATTEMPTS) { markFailed(app, docRef); Result.failure() } else Result.retry()
  }
  ```
  - `upload(...)` = `val ref = storage.reference.child(path); ref.putFile(Uri.fromFile(File(local)), storageMetadata { contentType = type }).await(); return ref.downloadUrl.await().toString()`.
  - `markFailed` runs the same kind of transaction with `uploadState = FAILED` (`file` and `thumbnail` unchanged), inside its own `try` that only logs (no network to Firestore -> FAILED is just not written).
  - Leave out the `null` checks on `inputData` beyond `getString(...) ?: return Result.failure()` for the required keys (school project; enqueue always sets them).
- KDoc:
  - as on the class slide: `CoroutineWorker`, `inputData`, `Result.retry()`;
  - beyond class: Storage `putFile` / `downloadUrl`, `runTransaction`, why a transaction (Q1), why FAILED after 5 attempts.
- Commit: `feat(F-013): add the media upload worker`.
- Verify: build + lint (the worker is unused; lint must not flag the class; workers are created by reflection, so there are no unused-class errors).

### Slice 4: enqueue uploads
- `AttachmentUploader.enqueue`:
  ```kotlin
  val request = OneTimeWorkRequestBuilder<UploadWorker>()
      .setInputData(workDataOf(KEY_DOCUMENT to documentPath, KEY_TICKET_ID to ticketId, KEY_ATTACHMENT_ID to attachment.id,
          KEY_IS_VIDEO to (attachment.type == AttachmentType.VIDEO), KEY_FILE to attachment.file,
          KEY_THUMBNAIL to attachment.thumbnail, KEY_PUBLIC_THUMBNAIL to addPublicThumbnail))
      .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
      .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
      .build()
  WorkManager.getInstance(appContext).enqueue(request)
  ```
- `reportTicket`:
  - after `batch.commit()`: `ticket.attachments.filter { it.uploadState == UploadState.PENDING }.forEach { uploader.enqueue("$TICKETS/${ticket.id}", ticket.id, it, scope == Scope.PUBLIC && it.phase == AttachmentPhase.REPORT) }`;
  - in `joinTicket` after its `commit()`: `enqueue("$TICKETS/$ticketId/$REPORTS/$creatorId", ticketId, it, false)`.
- Version catalog + Gradle: `coil-network-okhttp` 3.0.4. No code change: Coil 3 picks the network fetcher up from the classpath.
- Repository KDoc: the uploads are enqueued after the batch; the joiner's media are uploaded into `reports/{uid}` and never go to `publicTickets`.
- Commit: `feat(F-013): upload report media with WorkManager`.
- Verify: build + lint, then device checks D1-D5, D7, D8, D11.

### Slice 5: video location removal (Q4)
- `VideoMetadata.removeLocation(source, target)`:
  1. `MediaExtractor().setDataSource(source)`; `MediaMuxer(target.path, MUXER_OUTPUT_MPEG_4)`.
  2. For each track: `selectTrack(i)`, `muxerIndex[i] = muxer.addTrack(extractor.getTrackFormat(i))`.
  3. `setOrientationHint(rotation)` with the rotation from `MediaMetadataRetriever` (`METADATA_KEY_VIDEO_ROTATION`, 0 when missing).
  4. `muxer.start()`.
  5. Loop over the samples: `readSampleData(buffer, 0)` into a 2 MB `ByteBuffer`; stop when the size is below 0. Then `BufferInfo.set(0, size, sampleTime, if (sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0)`, `writeSampleData(muxerIndex[sampleTrackIndex], buffer, info)`, `advance()`.
  6. `stop()` / `release()` in `finally`. On an exception return false and delete `target`.
- `prepare` (VIDEO): `ok = VideoMetadata.removeLocation(attachment.file, target)`, then delete the original copy on success (it replaces `renameTo`).
- KDoc: beyond class. Why: decision 0010 and CLAUDE.md (public media are readable by anonymous users). MediaMuxer writes no location unless `setLocation` is called. No re-encoding.
- Commit: `feat(F-013): remove video location before upload`.
- Verify: build + lint, device check D6.

### Slice 6: upload-state indicator
- `MediaTile`: after the label,
  - `when (attachment.uploadState) { PENDING, UPLOADING -> R.string.ticket_media_upload_pending; FAILED -> R.string.ticket_media_upload_failed; UPLOADED -> null }`;
  - show it as `bodySmall` / `onSurfaceVariant` text, like `ticket_media_unavailable`, and only when `attachment.file.isNotBlank()` (sample tickets keep "Not available yet");
  - the tile already merges semantics, so TalkBack reads it with the label.
- Strings:
  - EN: "Upload pending", "Upload failed";
  - PT: "Envio pendente", "Falha no envio".
- Add one preview attachment with `uploadState = FAILED` to the existing `ShowScreen` preview, if the preview ticket list allows it without other changes.
- Commit: `feat(F-013): show upload state on ticket media`.
- Verify: build + lint, D9, D10.

### Device checks (emulator)
Preconditions:
- `app/google-services.json` present.
- The Storage bucket exists and its rules allow writes (Q12, the person checks in the console).
- Firestore is in test mode (until 2026-12-10).
- Inside 10 km: `adb emu geo fix -8.4115 40.1925`.
- For D3/D7/D8/D10, set `CityFixApplication.currentUser` locally to `Citizen(DEV_CITIZEN_ID, registrationStatus = RegistrationStatus.ACTIVE)` so the reporter's full tickets (with upload state) are listed. **Do not commit it.**
- Firestore REST in test mode: `curl "https://firestore.googleapis.com/v1/projects/<projectId>/databases/(default)/documents/tickets/<id>"` (`<projectId>` from the console; do not paste it into docs).
- WorkManager state: Android Studio App Inspection > Background Task Inspector, or `adb shell dumpsys jobscheduler | findstr cityfix`.

| # | Check | Expected |
| --- | --- | --- |
| D1 | Camera photo, PUBLIC, online. Before submit: `adb shell run-as pt.isec.amov.cityfix ls -l files/images` | Storage `tickets/<id>/<uuid>.jpg` much smaller than the `CAPTURE_` original, long side <= 1600 px, upright; `<uuid>_thumb.jpg` <= 256 px |
| D2 | REST read of `tickets/<id>` and `publicTickets/<id>` after D1 | entry `uploadState` UPLOADED, `storagePath` / `thumbnailPath` https URLs; `publicTickets.thumbnails` = [thumb URL]; anonymous list card shows the thumbnail |
| D3 | Airplane mode on, report PUBLIC with 2 photos, swipe the app away, airplane mode off, do not open the app | ticket listed at once with "Upload pending"; Background Task Inspector shows 2 ENQUEUED jobs waiting for network; within about 1 min both RUNNING then SUCCEEDED; REST shows UPLOADED |
| D4 | Push a ~50 MB video (`adb push` + pick it), submit online, swipe the app away while the job is RUNNING | job runs again (inspector, or REST after a while) and ends UPLOADED. Note: Settings > Force stop pauses it until the app is opened again |
| D5 | Download the uploaded JPEG from the console; check Details in Windows file properties or `exiftool` | no GPS (no EXIF at all); paths contain only the ticket id and a UUID, no uid, no `CAPTURE_` |
| D6 | A video with location (recorded with the emulator camera with location on, or a pushed geotagged MP4). Before slice 5, check whether the picked copy has location (`exiftool -ee -G1 -a <file>` or `ffprobe`) | after slice 5 the uploaded MP4 has no location atom, plays, keeps its rotation; the `_thumb.jpg` exists |
| D7 | Second report within 10 m, same category and situation, another reporter id (change `reporterId` stand-in) | `tickets/<id>/reports/<uid>.attachments` UPLOADED with URLs; shared ticket `attachments` unchanged; `publicTickets.thumbnails` unchanged |
| D8 | Airplane mode, report, `adb shell run-as pt.isec.amov.cityfix rm files/images/<uuid>.jpg`, network on (optionally set `MAX_ATTEMPTS = 1` locally, not committed) | after the attempts the entry is FAILED and the detail says "Upload failed"; no crash |
| D9 | `currentUser = null` (anonymous) | card and detail show the public thumbnails; no pending/failed lines |
| D10 | Detail with pending and failed media, portrait and landscape, device in Portuguese | lines visible, not clipped; PT strings |
| D11 | After D2: `run-as ... ls files/images` | the uploaded `<uuid>.jpg` / `_thumb.jpg` are gone (Q3) |
| D12 | After each slice | `assembleDebug` and `lint` pass, 0 errors |

## Conventions
- Kotlin conventions skill and `.claude/rules/kotlin.md`:
  - no Firebase or WorkManager calls in composables;
  - strings in EN + PT;
  - `Dispatchers.IO` for file work.
- KDoc on every new class and public function says which part follows class (slide 08, T8 demo, P10) and which part is beyond class, in the F-011/F-012 style.
- Short functions, no defensive checks for tampering (school project). `try`/`catch` only where a failure has a defined meaning (compression -> FAILED, upload -> retry or FAILED).
- Commits: Conventional Commits with `F-013`, no attribution lines.

## Do not touch
- `firestore.rules`, `storage.rules`, `firebase.json` (Q11, Q12; ask first), `app/google-services.json`.
- `model/` (no model change is needed), `TicketsRepository.kt`, `ReportViewModel.kt` / `ReportUiState.kt`, `MainActivity.kt` (Diogo's), `AttachmentGallery.kt` (Q8), `DebugSeed.kt`.
- Do not commit the local `currentUser` stand-in change used for the device checks.

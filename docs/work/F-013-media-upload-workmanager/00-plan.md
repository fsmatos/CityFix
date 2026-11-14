# F-013: Image compression, thumbnails and WorkManager uploads
Type: feature · Task file: docs/tasks/phase-2/F-013-media-upload-workmanager.md
Labels: (none)

## Problem
- Brief IV: images must be compressed on the device before sending, and heavy uploads (photos, videos) to Firebase Storage must be handed to WorkManager and run only when there is a network.
- Brief V: captured images must be optimised, compressed and given thumbnails before they go to the cloud. Brief I.1.B: the public list shows a thumbnail.
- Today nothing is uploaded. `reportTicket` (F-011/F-012) stores every attachment as PENDING with `storagePath` = the local path on the reporter's phone, so other devices and anonymous users get a placeholder, and `publicTickets.thumbnails` is always empty (decision 0013).
- `model/UploadState` exists but nothing changes it.
- Open items owned by F-013 in earlier decisions:
  - remove location metadata from videos before upload (0010 Consequences, overview "Handover (F-010)");
  - neutral file names, so `CAPTURE_<epoch millis>` never reaches Storage (0006);
  - upload the joiner's media in `tickets/{id}/reports/{uid}` too, without touching the shared ticket or `publicTickets` (0014, "Handover (F-012)");
  - one entry point D-013 can reuse for the resolution proof (0011, task file).

## Outcome
- On submit, each photo becomes a JPEG of at most 1600 px on the long side (quality 80), turned the right way up, with no EXIF at all (so no GPS), plus a thumbnail of at most 256 px (quality 70). Each video gets a 256 px thumbnail from a frame. The video itself is not compressed; its location metadata is removed first (slice 5, Q4). Every attachment gets a new random id (UUID), and that id is its file name on the phone and in Storage.
- Then one WorkManager job per attachment (a `CoroutineWorker`, network constraint `CONNECTED`, exponential backoff) uploads the file and its thumbnail to `tickets/{ticketId}/{id}.jpg|mp4` and `tickets/{ticketId}/{id}_thumb.jpg` (decision 0010). It then writes the download URLs and `uploadState = UPLOADED` into the attachment entry of the document that holds it: `tickets/{id}` for the creator's media, `tickets/{id}/reports/{uid}` for a joiner's.
- For the creator's REPORT-phase media of a PUBLIC ticket, the worker also adds the thumbnail URL to `publicTickets/{id}.thumbnails`, so anonymous users see it in the list and the detail.
- After 5 failed attempts with a network, the entry becomes FAILED. An attachment that cannot be compressed or cleaned is stored as FAILED and never sent.
- The ticket card and the detail draw the thumbnail when there is one: the local file before the upload, the download URL after it.
- The detail's photo/video tiles show "Upload pending" or "Upload failed" under the label.
- `AttachmentUploader` has two public functions, `prepare` and `enqueue`. `reportTicket` calls both, and D-013 calls the same two for the resolution proof.

## Acceptance checks
Run on an emulator. The full list (D1-D12) and the preconditions are in `01-coding-plan.md`.
- [ ] Report in airplane mode: the ticket appears at once, and its detail says "Upload pending". Swipe the app away and turn the network on: within about a minute, without opening the app, the files are in Storage and the Firestore entry is UPLOADED with https URLs (console or Firestore REST API).
- [ ] The stored image is much smaller than the camera original (compare `run-as ... ls -l files/images` before submit with the Storage console size), with its long side at most 1600 px. A `_thumb.jpg` of at most 256 px exists, and the list card loads it (PUBLIC ticket: `publicTickets.thumbnails` holds its URL).
- [ ] Killing the app during a long upload (swipe from recents): the work starts again later and finishes.
- [ ] No user id and no capture time in any Storage path (only `tickets/{ticketId}/{uuid}...`). The downloaded JPEG has no GPS EXIF. The downloaded video has no location (after slice 5).
- [ ] A joiner's media end up UPLOADED in `reports/{uid}`. The shared ticket's `attachments` and `publicTickets.thumbnails` do not change.
- [ ] Build and lint pass after every slice (0 errors).

## Approach
Follows class `08_Coroutines_Flows_Services` (WorkManager slides, about lines 1177-1225: `CoroutineWorker`, `inputData.getString`, `Result.success()` / `Result.retry()` with exponential backoff, `Constraints.Builder().setRequiredNetworkType(...)`, `WorkManager.getInstance(context)`). It also follows the T8 `AMOVStorageDemo` (`bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)` to a file, `BitmapFactory.decodeFile`) and P10 Challenge I (`BitmapFactory.decodeFile` + `Bitmap.createScaledBitmap`, and the service reading `(applicationContext as ContactsApp).repository`). P09 already uses `coil-network-okhttp` 3.0.4. Departures are marked **beyond class**.

**Libraries (need the person's OK at Gate 1):**
- `androidx.work:work-runtime-ktx` **2.12.0**: the newest stable on Google Maven (`maven-metadata.xml` checked 2026-11-14; minSdk 24, compileSdk 33+, both fine). The class slides name no version. Since 2.9 the `-ktx` artifact is empty and everything is in `work-runtime`. The task file names `-ktx`, which is harmless, but the person may prefer `work-runtime` (Q10).
- `io.coil-kt.coil3:coil-network-okhttp` **3.0.4**: the P09 class version, the same as our `coil`. Without it Coil 3 cannot load the https download URLs.
- `firebase-storage` is already in the build (F-010, BoM 34.19.0).

**1. Prepare at report time (in `reportTicket`, on `Dispatchers.IO`, before the batch).** This is the chosen option. Alternative: compress inside the worker, or chain `beginWith(compressWorker).then(uploadWorker)` as on the class slide. Rejected because it needs two workers and output data, and compression would wait for the network. Compressing at submit also means the reporter's card shows the small local thumbnail at once, offline too, and the files the worker sends are already final.
- `utils/ImageCompression.kt` (new, `object`):
  - `compressImage(source, target, thumbnail): Boolean` reads the size with `inJustDecodeBounds`, decodes with an `inSampleSize` power of 2 so the bitmap is not huge (**beyond class**; class 02 warns about the memory of a 4000x3000 bitmap). It turns the bitmap by `ExifInterface(source).rotationDegrees` with a `Matrix` (**beyond class**; re-encoding drops EXIF, orientation included, so without this portrait photos would lie sideways). It scales to at most 1600 px with `Bitmap.createScaledBitmap` (P10) and writes JPEG 80 with `bitmap.compress` (T8 demo). Then it scales to 256 px and writes JPEG 70 as the thumbnail. It returns false when the decode fails.
  - `videoThumbnail(source, thumbnail): Boolean` uses `MediaMetadataRetriever.getFrameAtTime()` (**beyond class**; Coil's `VideoFrameDecoder` only draws on screen, it does not write a file), then the same scale and JPEG write.
- `data/AttachmentUploader.kt` (new) holds `suspend fun prepare(attachment, uploadedById): Attachment`. It makes a new `id = UUID.randomUUID().toString()` and writes `filesDir/images/{id}.jpg` + `{id}_thumb.jpg` for a photo, or `{id}.mp4` (location removed, slice 5) + `{id}_thumb.jpg` for a video. Then it deletes the original copy. It returns the attachment with the new id, `file`, `thumbnail`, `uploadedById`, and PENDING, or FAILED when compression or cleaning failed (the original is then kept and never sent).
- `reportTicket` calls `prepare` only after the grouping decision. In the ALREADY_FOLLOWING case nothing is written, and `ReportViewModel` still deletes the original files as today. Concretely, the "already a participant" query moves before preparation.
- The F-009 `FileUtils.removeImageLocation` call goes: the new JPEG has no EXIF at all, which is stronger than clearing the GPS block. Q5 decides whether the function and its `GPS_TAGS` list are deleted.
- `toPublicTicketDocument()` copies only the thumbnails of **UPLOADED** REPORT attachments, so a local path never reaches `publicTickets`. At creation the list is empty, and the worker adds to it.

**2. Upload (`data/UploadWorker.kt`, `CoroutineWorker`, as the slide).**
- `inputData`: `documentPath` (`tickets/{id}` or `tickets/{id}/reports/{uid}`), `ticketId`, `attachmentId`, `isVideo`, `file`, `thumbnail` (local paths), `addPublicThumbnail`.
- It gets `firestore` and `storage` from `applicationContext as CityFixApplication`, as P10's `ProximityService` gets its repository, so the Firestore instance with the offline settings is used.
- `doWork()`:
  1. `putFile(Uri.fromFile(file), storageMetadata { contentType = "image/jpeg" | "video/mp4" }).await()` and then `downloadUrl.await()`, for the file and then the thumbnail (**beyond class**: the T8 demo has the Storage dependency but never calls it; the content type is set because the drafted Storage rules match on it).
  2. One Firestore **transaction** (**beyond class**, see Q1). It reads the document, replaces the matching attachment entry (`storagePath` = file URL, `thumbnailPath` = thumbnail URL, `uploadState = UPLOADED`) and writes the whole `attachments` array back. When `addPublicThumbnail` is set, it also does `update(publicTickets/{id}, "thumbnails", FieldValue.arrayUnion(thumbUrl))`.
  3. It deletes the local file and thumbnail (Q3) and returns `Result.success()`.
  4. On any exception it returns `Result.retry()`. From the 5th attempt (`runAttemptCount >= 4`) it instead writes `uploadState = FAILED` with the same kind of transaction (failure only logged) and returns `Result.failure()`.
  - A missing entry (the creation batch, which is not awaited, has not reached the server yet) throws, so the worker simply retries.
- `AttachmentUploader.enqueue(documentPath, ticketId, attachment, addPublicThumbnail)`:
  - builds `OneTimeWorkRequestBuilder<UploadWorker>()` with `setInputData(workDataOf(...))`, `setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())` (the slide uses UNMETERED; we use CONNECTED as the task says) and `setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)` (the default, written out so it is visible at the defence);
  - then calls `WorkManager.getInstance(context).enqueue(request)` as on the slide. Each attachment is enqueued exactly once, so unique work adds nothing (Q7).
- `reportTicket` enqueues every PENDING prepared attachment right after `batch.commit()`. Creator path: `documentPath = "tickets/{id}"` and `addPublicThumbnail = scope == PUBLIC` (REPORT phase). Join path: `"tickets/{id}/reports/{uid}"`, `addPublicThumbnail = false` (decision 0014).
- WorkManager keeps its queue in its own database, so the jobs survive the app being killed and the phone restarting (slide: "Survives app process kills and device reboots").

**3. UI.**
- `AttachmentImage` draws `attachment.thumbnail` when it is set: an image request, keeping the play badge for videos. Otherwise it draws `file` as today (the video frame decoder for a local video). That covers the card (acceptance: "the list uses it"), the detail, and the anonymous rebuilt thumbnails. The report form gallery has no thumbnails before submit, so it is unchanged.
- `ShowScreen.MediaTile` gets a line "Upload pending" (PENDING or UPLOADING) or "Upload failed" (FAILED) under the label, like the existing "Not available yet" line, EN + PT. The task file says `AttachmentGallery`, but that is the report form's draft gallery, where nothing has an upload state yet (Q8).

**4. Video location (slice 5, Q4).** `utils/VideoMetadata.kt`: `removeLocation(source, target): Boolean` copies the tracks with `MediaExtractor` into a new MP4 with `MediaMuxer`, about 35 lines (**beyond class**). MediaMuxer writes no location unless `setLocation` is called. The rotation is copied with `setOrientationHint` from `MediaMetadataRetriever`. No re-encoding, so it is fast and loses no quality. If it fails (a format MediaMuxer cannot write), the video is FAILED and stays on the phone.

**Alternatives considered for the array entry (Q1):** read-then-`update` without a transaction loses updates when two workers of the same report finish together, and WorkManager runs them in parallel. Storing upload state in a map keyed by attachment id (`update("uploads.$id", ...)`) needs no read but changes the decision 0010 schema and every reader. A transaction is the simplest correct choice, and decision 0010 already reserved one for this worker ("Transactions are used only where the network is required anyway: ... the F-013 upload worker").

## Architecture impact
- New: `utils/ImageCompression.kt`, `utils/VideoMetadata.kt`, `data/AttachmentUploader.kt`, `data/UploadWorker.kt`.
- `CityFixApplication` gets `attachmentUploader by lazy`. `FirestoreTicketsRepository(firestore, attachmentUploader)`.
- `TicketsRepository` and `ReportViewModel` do not change.
- `FirestoreMapping.kt`: `toAttachment()` / `toDocument()` become internal, plus one helper `DocumentSnapshot.attachmentsWith(id, change): List<Map<String, Any>>?` (null when the id is missing). `toPublicTicketDocument()` keeps only UPLOADED thumbnails.
- Schema meaning, an amendment to decision 0010 (Q2): once UPLOADED, `storagePath` / `thumbnailPath` hold the **https download URL**, not the Storage path. While PENDING or FAILED they hold the local path. Field names stay, so the drafted rules and the mapping do not change. Storage objects are still at the 0010 paths.
- Attachment ids become UUIDs at submit. Before submit, the form keeps using the file name as its list key.
- Dependencies: WorkManager, Coil network. WorkManager initialises itself (App Startup), so nothing is added to the manifest.

## Failure modes
- **No network**: the jobs wait (constraint). The ticket is already in the local cache and shown as pending.
- **Network but Firestore/Storage refuses** (bucket missing, rules, a CLOSED ticket if rules were deployed): 5 attempts with backoff (about 30 s, 1, 2, 4 min), then FAILED. Retrying a FAILED item is out of scope.
- **The creation batch has not reached the server when the worker runs**: the transaction reads from the server, does not find the entry, and the worker retries.
- **App swiped away mid-upload**: the job is stopped and run again. `putFile` starts the upload from zero. **Force stop** from Settings cancels the jobs until the app is opened again (Android behaviour; WorkManager reschedules them at the next start).
- **Network lost mid-upload**: the constraint stops the worker, and it runs again later.
- **WorkManager's 10-minute limit per run**: a very large video on a slow network is stopped and retried, and can end FAILED (risk, see below).
- **Photo cannot be decoded** (a broken file, or HEIC below API 28): FAILED, never sent, kept on the phone.
- **Local file gone** (should not happen; clearing app data also clears WorkManager): the exceptions lead to FAILED.
- **Two workers on the same document**: the transaction retries on contention, so no update is lost.

## Risks and open questions
Recommended default first. Gate 1 decides.
- **Q1. How the worker changes one entry of the `attachments` array.** Default: a Firestore transaction (read the document, replace the entry by id, write the array, plus `arrayUnion` on `publicTickets.thumbnails` in the same transaction). Not covered in class (the class taught `set` / `get` / batch); about 10 lines. Decision 0010 already names this worker as a transaction user. Alternatives: (b) plain `get().await()` + `update()` (in class, but two attachments of one report finishing together overwrite each other); (c) one worker per document that uploads its attachments one after another and then does (b) (in class, but the task's "one job per attachment" goes, and D-013's proof on the same ticket could still race); (d) a map keyed by id (schema change).
- **Q2. What the entries hold after upload.** Default: the https **download URL** in `storagePath` / `thumbnailPath` (field names kept). Coil loads it directly (with `coil-network-okhttp`, as P09), and so do anonymous users. `Attachment.file`'s KDoc ("Storage path or URL after it") and decision 0003 already allow it. Amend decision 0010. Alternative: keep the Storage path and fetch a URL each time an image is shown. That needs a Firebase call per image from the UI layer, which `.claude/rules/kotlin.md` forbids, or a custom Coil fetcher. Note: a download URL carries a token, so anyone holding it can read the file whatever the rules say. Only participants and operators see the URLs of PRIVATE tickets (they are only in `tickets` / `reports`).
- **Q3. Local copies after upload.** Default: deleted once UPLOADED (and kept while PENDING or FAILED, because the worker needs them). The reporter's card then loads the URL through Coil's cache. Offline, a thumbnail that was never cached shows the placeholder. Alternative: keep them (phone storage grows with every report).
- **Q4. Video location.** Default: implement slice 5 (MediaExtractor + MediaMuxer remux, beyond class), because decision 0010 says F-013 "must remove location metadata from videos" and CLAUDE.md forbids exact coordinates for anonymous users. The Photo Picker removes location from images on Android 13+. For videos and older Android, no source found to say it does, so it is checked in D6. Alternative: drop slice 5, upload videos as they are and record it as a known limit in the report and decision 0010 (smallest code, but it breaks an accepted decision). Videos are not compressed in either case (the task says so; to be mentioned in the report).
- **Q5. F-009's `removeImageLocation`.** Default: remove the call from `reportTicket` and delete the function and `GPS_TAGS` from `FileUtils` (re-encoding writes no EXIF; less code to defend). `androidx.exifinterface` stays for `rotationDegrees`. Alternative: keep calling it on the original before compressing, as the task text says ("reuse"). This is harmless but has no effect on what is uploaded.
- **Q6. UPLOADING.** Default: not written. The states are PENDING -> UPLOADED or FAILED. Writing UPLOADING costs one more transaction per attempt, and the UI shows it the same as pending. The enum value stays (it is in the class diagram). Alternative: write it at the start of `doWork()`.
- **Q7. Unique work.** Default: plain `WorkManager.getInstance(context).enqueue(request)` as on the slide. Each attachment is enqueued once, right after its batch. Alternative (task file): `enqueueUniqueWork("upload-$id", ExistingWorkPolicy.KEEP, request)`, one more concept to explain.
- **Q8. Where the indicator goes.** Default: the detail's `MediaTile` (`ShowScreen`), where saved tickets show their media. `AttachmentGallery` only shows the form's draft. Alternative: a small badge on `AttachmentImage` (card and detail), which needs a parameter so the form does not show "pending" on every draft photo.
- **Q9. Video size.** Default: no size check in code. The rules draft (100 MB) is not deployed, test-mode Storage accepts it, and the 10-minute run limit is the practical cap. Mention it in the report. Alternative: one line in the worker that marks a video of 100 MB or more FAILED without uploading.
- **Q10. Artifact.** Default: `work-runtime-ktx` 2.12.0 as the task names. Alternative: `work-runtime` 2.12.0 (same classes; `-ktx` has been empty since 2.9).
- **Q11. Rules drafts.** `firestore.rules` has `reports/{uid}`: `allow update: if false`, so the worker's update of a joiner's report would be refused if the drafts were ever deployed. Default: do not edit the drafts in F-013 (CLAUDE.md: ask before changing rules; they are not deployed, decision 0012 Q5). Record the gap in the decision 0010 amendment and the handover. Alternative: with the person's OK, add an attachments-only update for the report's author, like the ticket's.
- **Q12. Storage bucket (precondition, the person checks).** Is the default Storage bucket created in the console, with test-mode rules that allow writes? If it was created in production mode (deny all), every upload ends FAILED. Changing Storage rules needs the person's OK, and we do not deploy `storage.rules`.
- **Risk: WorkManager's 10-minute limit.** A large video on a slow network may never finish in one run. Long-running workers (`setForeground`) are out of scope.
- **Risk: another device sees the local path while PENDING.** A participant on another phone sees a placeholder until the upload finishes (already the case since F-011).
- **Risk: media of joiners in a PUBLIC ticket's folder.** Their Storage files sit under `tickets/{ticketId}/`, which the drafted Storage rules let everyone read when the ticket is PUBLIC. The names are random UUIDs that are only in `reports/{uid}` (author and operators), and the drafts are not deployed. Record it.
- **Risk: Kotlin/AGP compatibility of work 2.12.0** with Kotlin 2.2.10 / AGP 9.4.1: expected fine. Slice 3 builds it first.

## Out of scope
- Retrying a FAILED upload from the UI; a progress bar; a foreground long-running worker for big videos.
- Video compression or transcoding (uploaded as is, location removed).
- A full-size image viewer or video player in the detail (the tiles show thumbnails).
- Full media (not just thumbnails) for anonymous users in the detail: `publicTickets` carries only thumbnails (decision 0010). Note it as an improvement.
- D-013's resolution proof screen (it reuses `prepare` + `enqueue`); profile photos (`users/{uid}/photo.jpg`).
- Deploying or editing `storage.rules` / `firestore.rules` (Q11, Q12).

## Slices
Each one builds, passes lint and becomes one commit.
1. `utils/ImageCompression.kt` (`compressImage`, `videoThumbnail`) and `FileUtils.mediaFile(context, name)`. Nothing calls them yet.
2. Prepare at submit: `AttachmentUploader.prepare`, the container entry, the `reportTicket` reorder (participant check, then prepare), the `removeImageLocation` call removed (Q5), `toPublicTicketDocument()` limited to UPLOADED thumbnails, and `AttachmentImage` drawing the thumbnail. Still no upload.
3. The WorkManager dependency, `UploadWorker`, and the `FirestoreMapping` helper (`attachmentsWith`, internal `toAttachment` / `toDocument`). Not enqueued yet.
4. `AttachmentUploader.enqueue`, wired into the create and join paths, plus `coil-network-okhttp`. Uploads work end to end (D1-D5, D7, D8, D11).
5. Video location removal: `utils/VideoMetadata.kt`, used by `prepare` (Q4; dropped if Gate 1 chooses the alternative) (D6).
6. Upload-state line in `ShowScreen.MediaTile` with EN + PT strings (D9, D10).

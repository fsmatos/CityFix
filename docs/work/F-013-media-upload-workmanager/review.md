# F-013 review

Range: `c52848d..HEAD` (commits 20658b4, cf0e15b, bd3eab4, 32d3cf1, acf0168, 58dc45c). Read against `00-plan`, `01-coding-plan`, `03-docs-plan`, the `state.md` Log (Gate 1 answers override the plan), the task file, brief IV/V, decision 0010, slides 08 (WorkManager), the T8 AMOVStorageDemo and P10.

Build: `./gradlew assembleDebug lint` passes. Lint: 0 errors, 22 warnings. The two new ones were already accepted in the Log: `UseKtx` on `createScaledBitmap` (kept because P10 uses it) and `NewerVersionAvailable` on coil-network-okhttp.

## Verdict
**Approve with should-fix.** 0 blocking, 2 should-fix (one in code, one for the docs phase), 3 nits.

The code matches brief IV and V:
- **Compression and thumbnails.** Photos are compressed on the device: `inSampleSize` to at least 1600 px, then the EXIF rotation, then a downscale to 1600 px, then JPEG 80. A 256 px thumbnail (JPEG 70) is made from the upright bitmap. A video gets a thumbnail from one frame.
- **Background upload.** WorkManager uploads each file only with a connection (`NetworkType.CONNECTED`, exponential backoff 30 s). Each step follows slides 08 (`CoroutineWorker`, `inputData`, `Result.retry()`, `Constraints.Builder`, `enqueue`).
- **Firestore update.** The worker reads the document and writes it back, with no transaction (Gate 1 Q1). It writes to the right document: `tickets/{id}` for the creator, `tickets/{id}/reports/{uid}` for a joiner.
- **Public thumbnails.** A thumbnail goes to `publicTickets.thumbnails` only for the creator's REPORT media on a PUBLIC ticket, never for a joiner's media.
- **Failure and clean-up.** After 5 attempts the entry is marked FAILED. Local copies are deleted after the write succeeds.
- **Storage paths.** Paths are `tickets/{ticketId}/{uuid}[.jpg|.mp4|_thumb.jpg]`, with no user id (decision 0010).
- **Repository order.** `prepare` runs only after the 10 m match and after the already-following check. It runs before the batch, and `enqueue` runs after `commit()`.
- **Anonymous privacy.** Anonymous users still only receive `publicTickets`. `toPublicTicketDocument()` keeps only the UPLOADED thumbnail URLs, so a local path never reaches it. The thumbnails are re-encoded JPEGs with no EXIF. Nothing in `publicTickets` exposes the author or exact coordinates.
- **UI.** `AttachmentImage` draws the thumbnail. `MediaTile` shows "Upload pending" or "Upload failed", in EN and in `values-pt`. Its `mergeDescendants` semantics reads that line aloud. The detail screen gets the line only when the file is not blank. Anonymous attachments are UPLOADED, so they show no line.
- **MVVM.** No ViewModel or UI state changed. The worker reads the app container the same way `ProximityService` does in P10.

Consequences of explicit choices (not findings): there is no transaction (see S1 for how often the overwrite can happen); videos keep their recording metadata; the draft rules still do not allow a joiner to update `reports/{uid}` (Q11); there is no UPLOADING state and the enqueue is plain.

## Blocking
None.

## Should-fix

### S1. Workers for the same document run in parallel, so the accepted overwrite is likely, and the losing media is lost from view
`FirestoreTicketsRepository.reportTicket` (and `joinTicket`) enqueues one `UploadWorker` per attachment, all at the same moment, with the same constraint. WorkManager runs them in parallel (the default executor runs several workers at once). With 2 or 3 photos of similar size, both uploads end close together. Each then runs `docRef.get()` followed by `update("attachments", list)` in `UploadWorker.doWork` (lines 243-256). The last write wins, so the other entry is put back to PENDING with its local path.

Gate 1 accepted this as rare ("finish at the same moment"), with the entry staying "PENDING locally until a later write". In practice it is the normal case for a multi-photo report, and no later write ever fixes it:
- The losing worker has already returned `Result.success()` and deleted its local file and thumbnail (lines 259-260).
- The ticket keeps a PENDING entry pointing at a deleted path, so the tile shows the placeholder and "Upload pending" forever.
- The file sits in Storage with no reference to it.
- If the ticket is PUBLIC, the thumbnail URL is still in `publicTickets.thumbnails`. That differs from what `toPublicTicketDocument()` would build now.

**Fix (still no transaction, still a plain `enqueue`, and simpler than now).** Enqueue one worker per document instead of one per attachment:
- `AttachmentUploader.enqueue(documentPath, ticketId, addPublicThumbnail)` takes only those 3 inputs.
- `doWork` reads the document once and loops over the entries that are PENDING and have a local file. For each, it uploads the file and thumbnail, then runs the same get-then-update for that entry. The `arrayUnion` stays in the same batch.
- Already UPLOADED entries are skipped, so a retry resumes where it stopped.
- `MAX_ATTEMPTS` and FAILED apply to the entries still PENDING at the last attempt.

This removes 4 `inputData` keys (`attachmentId`, `isVideo`, `file`, `thumbnail`), because the document already holds them. Within one document the writes become sequential, so the race goes away. `reportTicket` and `joinTicket` each make one `enqueue` call. D-013 calls `enqueue("tickets/<id>", id, false)` after its batch.

If the person prefers to keep one worker per attachment, then at least:
- correct the Q1 consequence in the Log and in the worker comment ("likely with 2+ media; the losing entry stays PENDING with a deleted local file");
- run D3 (2 photos) and check that both entries end UPLOADED;
- say so in the technical report.

### S2. Stale KDocs that the defence will ask about (docs phase)
- `model/TicketPrivacy.kt` lines 26-28 still say photo EXIF is removed by `FileUtils.removeImageLocation` (F-009), which no longer exists, and that video metadata and Storage paths are "for the media-upload task". **Fix:** "Photos are re-encoded in AttachmentUploader.prepare (F-013), which drops all EXIF; videos are uploaded as recorded and may keep their recording metadata (known limit); Storage paths hold only the ticket id and a UUID (decision 0010)."
- `model/Attachment.kt`: the KDoc on `id` says "the local file name before upload (F-007)". After F-013 it is a UUID set by `prepare`. The KDoc on `file` says "Storage path or URL after it". It is now always the https download URL (Q2). **Fix:** update both lines. `model/` was on the do-not-touch list for code, so do this in the docs phase.
- The task file lists items that Gate 1 changed: reuse `removeImageLocation`, UPLOADING, unique work, the indicator in `AttachmentGallery`. **Fix:** note in the docs phase that the task file was superseded by Gate 1 Q5, Q6, Q7 and Q8, so nobody reads it as missing work.

## Nit

### N1. Long KDoc line in `AttachmentUploader`
Line 3 of the class KDoc ("named by a new UUID; then enqueues one UploadWorker per attachment. One entry point for ...") runs past the line width the other files use. **Fix:** rewrap it at about 100 columns.

### N2. Videos are kept in `filesDir/images`
`FileUtils.mediaFile` (and so `prepare`) puts `<uuid>.mp4` in the folder named `images`. This is not a bug, since it is the same folder `AttachmentStorage` already uses. **Fix:** say "photos and videos" in the `mediaFile` KDoc, so the folder name does not mislead during the defence.

### N3. The worker's `catch (e: Exception)` also catches cancellation
If the connection drops during `putFile`, WorkManager stops the worker. The `CancellationException` then lands in the catch block. It logs "upload failed" and, on the last attempt, tries `markFailed` from a cancelled coroutine, which fails quietly. Nothing breaks, because WorkManager ignores the result of a stopped worker and runs it again later. Only the log line is misleading. **Fix (optional):** add `catch (e: CancellationException) { throw e }` before the general catch, or leave it and mention it if asked.

## Open (not blocking)
- Device checks D1-D11 have not been run. The person must first confirm that the default Storage bucket exists and that its rules are in test mode (Q12). D3 should use at least 2 photos (see S1). D10 covers portrait, landscape and PT for the new lines.
- The acceptance checks in the task file ("airplane mode then network on uploads without opening the app", "app killed during upload resumes", "no uid/GPS in Storage paths or files") depend on D3, D4 and D5.

# 0015: Media preparation and WorkManager uploads

Status: accepted (F-013, Gate 1 on 2026-11-14: Q1 and Q7 follow the class approach, Q4 slice 5 dropped, the other questions as recommended; review fix S1 on 2026-11-14 changed the design to one upload worker per Firestore document). Amends [decision 0010](0010-firestore-schema.md) (what `storagePath` / `thumbnailPath` / `publicTickets.thumbnails` hold, no transaction in the worker, video location). The number 0015 was free on `dev/Francisco` and on `origin/dev/Diogo` at Gate 1 (2026-11-14).

## Context
- Brief IV: images are compressed on the device, and heavy uploads go to WorkManager and run only with a network connection. Brief V: optimise, compress and make thumbnails before sending.
- Until F-013 the ticket's `attachments` held local paths with `uploadState` PENDING forever: other devices showed a placeholder, and `publicTickets.thumbnails` was always empty (decisions 0010, 0013).
- Class material: slides 08 (`CoroutineWorker`, `inputData`, `Result.retry()`, `Constraints.Builder()`, `WorkManager.getInstance(context).enqueue`); T8 AMOVStorageDemo (`bitmap.compress(JPEG, 90, stream)`, plain `get()` then `set`/`update` with listeners, no transactions); P10 Challenge I (`BitmapFactory.decodeFile`, `Bitmap.createScaledBitmap`, reading the app container with `applicationContext as ...App`); P09 (`coil-network-okhttp`); 09_Storage names Firebase Storage but shows no upload code.
- Firestore cannot change one element of an array: to change one attachment entry, the whole `attachments` list is read and written back.

## Decision
1. **Prepare at submit, upload later.** `AttachmentUploader.prepare(attachment, uploadedById)` runs in the repository before the batch (on `Dispatchers.IO`), so the stored entries already point to the final files:
   - every attachment gets a new UUID as its `id` and file name (no `CAPTURE_<millis>` time in names or Storage paths);
   - photo: decoded with `inSampleSize`, turned upright from the EXIF rotation, scaled to a long side of at most **1600 px**, written as **JPEG 80**; thumbnail **256 px, JPEG 70**; the original is deleted. Re-encoding writes a new JPEG with no EXIF at all, so GPS, make, model and capture time are gone;
   - video: moved as recorded to `<uuid>.mp4` (no compression), plus a 256 px thumbnail of one frame (`MediaMetadataRetriever`). A missing thumbnail alone does not fail the video;
   - if a photo cannot be decoded or a video cannot be moved, the entry is stored as FAILED with its original path and the report is still saved.
2. **One worker per Firestore document (review S1).** After `commit()` the repository calls `AttachmentUploader.enqueue(documentPath, ticketId, addPublicThumbnail)` once: `tickets/{id}` for the creator's report (`addPublicThumbnail` = the ticket is PUBLIC), `tickets/{id}/reports/{uid}` for a joiner (always `false`). The request has 3 `inputData` keys, `NetworkType.CONNECTED` and exponential backoff from 30 s.
3. **Plain `enqueue`, as on slides 08 (Gate 1 Q7).** `WorkManager.getInstance(appContext).enqueue(request)`, no unique work.
4. **What the worker does.** `UploadWorker` (`CoroutineWorker`) reads the document (and retries if it does not exist on the server yet). Then, one entry at a time, for every entry that is PENDING **and whose local file exists on this device**: Storage `putFile` + `downloadUrl` for the file and the thumbnail (paths `tickets/{ticketId}/{id}.jpg|.mp4` and `{id}_thumb.jpg`), then a plain `get()` of the document, the entry replaced (`attachmentsWith`), and one batch with `update("attachments", list)` plus, when `addPublicThumbnail`, `FieldValue.arrayUnion(thumbUrl)` on `publicTickets/{ticketId}.thumbnails`. Then it deletes the local copies and goes to the next entry.
5. **No transaction (Gate 1 Q1, the class approach).** The class teaches `get()` then `set`/`update`, not `runTransaction`. One worker per document makes the get-then-update calls for that document run one after the other, so they cannot overwrite each other.
6. **Download URLs in the entries (Q2).** After upload, `storagePath` and `thumbnailPath` hold the https download URL, so Coil loads them directly (as in P09) and the anonymous list reads them from `publicTickets.thumbnails`. While PENDING or FAILED they hold the local path.
7. **Local copies deleted after upload (Q3).** Once the entry is written as UPLOADED, the local file and thumbnail are deleted.
8. **States: PENDING -> UPLOADED, or FAILED (Q6).** UPLOADING is never written (it would cost one more read-and-write per file for a state nobody needs). On an exception the worker returns `Result.retry()`; the next run resumes with the entries still PENDING. After **5 attempts** (`MAX_ATTEMPTS`) this device's entries still PENDING become FAILED, so the detail does not say "pending" forever.
9. **The indicator is in the detail (Q8).** `ShowScreen`'s `MediaTile` shows "Upload pending" (PENDING, also UPLOADING if it were ever written) or "Upload failed" (FAILED) under the media, only when the file is not blank. EN and PT. The report form's `AttachmentGallery` shows only the draft, which is never uploading.
10. **No removal of video metadata (Q4, slice 5 dropped).** The brief asks to hide the author's data on screen (I.1.C) and to compress images; it does not ask to remove file metadata. Photos lose all EXIF anyway when re-encoded. Videos are uploaded as recorded and may carry their recording location (known limit).
11. **`FileUtils.removeImageLocation` removed (Q5).** Re-encoding already drops every EXIF tag, so the F-009 step is gone.
12. **No video size check (Q9).** The 3-attachment limit stays the only limit; the drafted Storage rule (`video/*` under 100 MB) is not deployed.
13. **`work-runtime-ktx` 2.12.0 (Q10)**, the newest stable on 2026-11-14 (the slides name no version), and `coil-network-okhttp` 3.0.4 (same version as the other Coil artifacts, as in P09).

### Rejected
- **Compress inside the worker, or a chain (compress worker -> upload worker).** The ticket would first be stored with the camera original (large file, EXIF with GPS on this device's screens) and the paths would change after the batch. Compressing at submit makes Submit a little slower per photo (not measured yet) and keeps the stored entry final.
- **One worker per attachment** (the plan before review S1). With 2 or 3 photos the workers run in parallel and finish close together, so one get-then-update overwrote the other's entry; the losing entry stayed PENDING pointing to a deleted local file.
- **A Firestore transaction on the entry** (the plan's default for Q1): correct for parallel writers, but not taught in class. Not needed once one worker handles the document.
- **A map keyed by attachment id instead of an array** (an update of one field path per entry): a schema change for every reader and the rules, against decision 0010.
- **Unique work** (`enqueueUniqueWork` per document or attachment, the task file's suggestion): not on the slides; a plain enqueue after each batch is enough.
- **Writing UPLOADING** (the task file's PENDING -> UPLOADING -> UPLOADED): one more write per file with nothing in the UI that needs it.
- **Storage paths or gs:// paths in the entries** instead of download URLs: every reader would need a `getDownloadUrl()` call (and the network) before Coil can draw.
- **Remuxing videos to remove the location atom** (slice 5): a hand-written MP4 rewrite with `MediaExtractor` / `MediaMuxer`, beyond class, for something the brief does not ask.

## Consequences
- **Download URLs are bearer links.** Anyone with the URL can read the file, whatever the Storage rules say. The URLs are stored only in documents the reader can already see (participants and operators; `publicTickets` only for PUBLIC tickets).
- **Videos keep their recording metadata**, which may include the place where they were recorded. For a PUBLIC ticket, anyone who downloads the video can read it.
- **WorkManager stops a worker after about 10 minutes.** A very large video on a slow connection may never finish in one run; the next run starts that file again from the beginning, so it may stay PENDING. (A stopped worker rethrows the cancellation, so a stop does not mark the entry FAILED.)
- **Force stop pauses the uploads.** Swiping the app away or restarting the phone keeps the work; Settings > Force stop pauses the work until the app is opened again (WorkManager then reschedules it).
- **Other devices see the placeholder and "Upload pending"** until the creator's device uploads. If the creator uninstalls the app or loses the local files first, the entry stays PENDING forever (no other device has the file).
- **An entry is uploaded only by the device that has the file.** A worker on another device (for example the operator's, when D-013 adds resolution media to the same ticket) skips those entries and never marks them FAILED.
- **Two workers for the same document on the same device** would race again (plain enqueue, no unique work). Today each document gets one enqueue per batch from each device, so this does not happen.
- **Drafted rules, if ever deployed:** `firestore.rules` refuses every update of `tickets/{id}/reports/{uid}` (`allow update: if false`), so a joiner's upload state would be refused (Q11, rules left alone). `storage.rules` allows only `create`, so a retry that re-uploads a file already in Storage (upload done, Firestore write failed) would be refused. Both are recorded, not fixed: the rules are not deployed (decision 0012 Q5).
- **The Storage bucket must be in test mode** (Q12) for the uploads to work, as the Firestore database is. This must be checked before the device checks and for the grader's test.

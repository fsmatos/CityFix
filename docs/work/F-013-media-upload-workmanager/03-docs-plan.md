# F-013 docs plan

## Docs to update
- `docs/architecture/overview.md`:
  - "Layers (MVVM)": `ImageCompression` and `VideoMetadata` in `utils/`; `AttachmentUploader` and `UploadWorker` in `data/`; `CityFixApplication` holds `attachmentUploader`.
  - New section "Media upload (F-013)":
    - preparation at submit (UUID ids, 1600 px / q80, 256 px / q70 thumbnails, rotation, no EXIF; video frame thumbnail; video location removal);
    - the worker (input keys, network constraint, backoff, transaction on the entry, `arrayUnion` on `publicTickets.thumbnails`, local files deleted, FAILED after 5 attempts);
    - the states PENDING -> UPLOADED / FAILED (UPLOADING not written);
    - what `storagePath` / `thumbnailPath` hold in each state;
    - class material and beyond;
    - the device checks D1-D12 and which ran;
    - known limits.
  - "Firestore repository (F-011)", report batch: the EXIF step is replaced by `prepare`; the participant check now runs before preparation; uploads are enqueued after each `commit()`; `toPublicTicketDocument()` copies only UPLOADED thumbnails.
  - "Location, gate and grouping (F-012)", join: the joiner's media are prepared and enqueued into `reports/{uid}`.
  - "Shared components": `AttachmentImage` draws the thumbnail when set. `ShowScreen` "Screens" entry: the upload-state line.
  - "Media is outside the projection" / F-007 handover privacy bullet: video location and XMP location are now handled (re-encoding drops all photo metadata; videos remuxed), if slice 5 lands.
  - Dependencies list: `work-runtime-ktx` 2.12.0 and `coil-network-okhttp` 3.0.4, with the lint warning count.
  - Mark done: "Handover (F-010)" F-013 item (video location, if slice 5; the limits as recorded), "Handover (F-011)" F-013 item, "Handover (F-012)" F-013 item. Remove the "thumbnails are empty in the anonymous list until F-013" known limit.
  - New "Handover (F-013)":
    - **D-013 (Diogo), resolution proof:** call `app.attachmentUploader.prepare(attachment, operatorId)` for each proof file **before** `transition(...)` (so the RESOLUTION entries carry the UUID ids and local paths). After the RESOLVED batch's `commit()`, call `enqueue("tickets/$id", id, proof, addPublicThumbnail = false)` for each PENDING one. Proof thumbnails do not go to `publicTickets` (decision 0010: REPORT-phase only). D-013 also shows each ticket's `reports/{uid}` media to operators (the URLs work once UPLOADED).
    - **D-009 (Diogo):** nothing new; the reporter id given to `prepare` is the signed-in uid once the stand-in goes.
    - **D-015 (Diogo), delivery:** the Storage bucket must exist and accept writes for the grader's test (test-mode expiry date); the report must say that videos are uploaded without compression.
    - **Rules (if ever deployed):** `firestore.rules` `reports/{uid}` needs an attachments-only update for its author (Q11); the Storage `create`-only rule refuses a retry that re-uploads an existing object.
  - Diogo's files changed by F-013: none, unless Gate 1 changes the "Do not touch" list.
- `docs/decisions/0010-firestore-schema.md`: amendment "by F-013 (date)":
  - after upload, `storagePath` / `thumbnailPath` hold the https download URL (Q2); while PENDING or FAILED they hold the local path;
  - `publicTickets.thumbnails` = download URLs of UPLOADED REPORT-phase thumbnails of the creator's media, added by the worker with `arrayUnion`;
  - the worker's transaction (Q1);
  - video location removed (or the limit, per Q4);
  - the `reports/{uid}` update gap in the drafted rules (Q11).
- `docs/decisions/0006-anonymous-projection-phase-1.md`: close the "CAPTURE_ file names" and "EXIF make, model and capture time" items (UUID names; re-encoding drops all EXIF). Amend the F-009 note: `removeImageLocation` was removed in F-013 (Q5).
- `docs/decisions/0013-firestore-reads-and-dashboard-counters.md`: the "Until F-013, `publicTickets.thumbnails` is empty" consequence is done.
- `docs/report/notes.md`: the items below.
- Tell Diogo (no edit to his docs): the D-013 handover above; the `model/Attachment.kt` KDoc for `id` ("the local file name before upload") is now true only for the draft. After submit it is a UUID (comment only, his file).

## Decisions to record
- **0015: Media preparation and WorkManager uploads** (new). Contents:
  - compress at submit instead of in the worker or a chain (and why);
  - sizes and qualities;
  - UUID names;
  - one job per attachment with plain `enqueue` (Q7);
  - `CONNECTED` + exponential backoff;
  - transaction on the array entry (Q1), with the rejected options (read-then-update, one worker per document, a map keyed by id);
  - download URLs in the entries (Q2);
  - local copies deleted after upload (Q3);
  - video remux or the limit (Q4);
  - no UPLOADING writes (Q6);
  - FAILED after 5 attempts;
  - the indicator in the detail (Q8);
  - no video size check (Q9).
  - Consequences: download URLs are bearer links; the 10-minute run limit; force-stop pauses work; other devices see placeholders while PENDING.
  - Write only what Gate 1 confirms.

## Report notes
- Brief IV / V: photos are compressed on the device at submit (at most 1600 px, JPEG 80, upright, every EXIF tag dropped so no GPS) and get a 256 px thumbnail. Videos get a frame thumbnail and are uploaded without compression (location metadata removed). Typical sizes from D1, to fill in after the device checks (camera original vs uploaded vs thumbnail).
- WorkManager as taught in class 08 (`CoroutineWorker`, `inputData`, `Result.retry()` with exponential backoff, network constraint). One job per attachment, so a failure retries only that file. The jobs survive the app being killed and the phone restarting. Offline reports show "Upload pending" and upload on their own when the network returns.
- Firebase Storage paths have no user id (`tickets/{ticketId}/{uuid}`), and the file names carry no capture time.
- Why a transaction for the upload state: Firestore cannot change one element of an array, and parallel workers would overwrite each other. Transactions need the network, which the worker has by its constraint. Reports themselves stay batch-only, so they work offline.
- Download URLs: Coil loads them directly (as in P09, `coil-network-okhttp`); anonymous users see public thumbnails through `publicTickets.thumbnails`.
- Joiners' media stay in their own report document and never reach `publicTickets` (decision 0014).
- Improvement opportunities:
  - a retry button for FAILED uploads;
  - progress in the UI (`setProgress`);
  - a long-running foreground worker or resumable upload sessions for large videos;
  - video transcoding to a lower bitrate;
  - full media (not only thumbnails) for anonymous users;
  - deploying the Storage rules (`image/*` < 10 MB, `video/*` < 100 MB, no uploads to CLOSED tickets);
  - Cloud Functions to make thumbnails on the server.

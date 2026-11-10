# F-013: Image compression, thumbnails and WorkManager uploads

Owner: Francisco (`dev/Francisco`) · Layer: `data` / `utils` · Phase 2 · Status: to do · Target: 2026-11-15
Depends on: F-011 · Blocks: D-013 (resolution proof upload)

## Goal
Upload ticket photos and videos to Firebase Storage in the background, compressing images and making thumbnails first.

## Why
- Brief IV: images compressed locally; heavy uploads delegated to WorkManager, run only with network.
- Brief V: optimise, compress and create thumbnails before sending.
- `model/UploadState` exists but nothing uses it.

## What to build
- **`utils/ImageCompression`:** downscale (for example max 1600 px), JPEG quality around 80, keep orientation, strip GPS (reuse `FileUtils.removeImageLocation`); thumbnail around 256 px. Video: frame thumbnail with `MediaMetadataRetriever`, file uploaded as is (say so in the report).
- **`data/UploadWorker`** (`CoroutineWorker`): input ticket id + attachment id; uploads file and thumbnail to the Storage paths of decision 0010, writes the download URLs and `uploadState` (PENDING → UPLOADING → UPLOADED / FAILED) in `tickets/{id}` and `publicTickets/{id}`. Constraint `NetworkType.CONNECTED`, exponential backoff, unique work per attachment.
- **Enqueue** from `reportTicket` and from D-013's resolution proof (one public function both call, e.g. `AttachmentUploader.enqueue(ticketId, attachment)`).
- **UI:** an upload-state indicator on attachments (pending / failed) in `AttachmentGallery`.

## Files
New `utils/ImageCompression.kt`, `data/UploadWorker.kt`, `data/AttachmentUploader.kt`; `data/FirestoreTicketsRepository.kt`, `ui/components/AttachmentGallery.kt`, `app/build.gradle.kts` (work-runtime-ktx), strings.

## Acceptance checks
- [ ] Report in airplane mode: the ticket appears at once with "pending"; turning the network on uploads it without opening the app.
- [ ] The stored image is much smaller than the camera original; a thumbnail exists and the list uses it.
- [ ] Killing the app during upload: the work resumes.
- [ ] No user id or GPS data in Storage paths or files.

## References
Brief IV, V; decision 0010; F-007, F-009 (attachments, GPS stripping); lecture `08_Coroutines_Flows_Services.pdf`.

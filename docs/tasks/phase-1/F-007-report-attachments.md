# F-007: Report attachments (photos and videos)

Owner: Francisco (`dev/Francisco`) · Layer: `ui.screens` / `viewmodel` / `utils` · Phase 1 · Status: to do
Depends on: F-006; coordination with Diogo for `TicketsViewModel.reportTicket` (attachments parameter) and D-006 (the `onSubmit` call)

## Goal
Let the citizen attach images and/or videos when opening a ticket, stored locally (no Firebase yet), and make at least one attachment required. Closes the I.2.E gap left by F-006.

## Brief
- I.2.E, Abertura: "descrição e anexos de imagens e/ou vídeos [...] Os dados são de preenchimento obrigatório (deve ser carregada uma imagem/vídeo)."
- V: dynamic permission handling (camera) if one is ever declared; image optimisation, compression and thumbnails before upload (upload phase, not this task).
- Class material: P09 (photo picker, `TakePicture` + `FileProvider`, local copies, Coil 3, "FixIt Project Bridge 1/3").

## What to build
- **Picker:** `ActivityResultContracts.PickMultipleVisualMedia(maxItems = 3)` with `PickVisualMedia.ImageAndVideo` (system photo picker, no permission).
- **Camera:** `ActivityResultContracts.TakePicture` through a `FileProvider` (`<provider>` in `AndroidManifest.xml`, `res/xml/file_paths.xml`). No `CAMERA` permission while the system camera app is used; confirm this. Video capture (`CaptureVideo`) is optional.
- **Local copies:** `utils/FileUtils.copyUriToInternalStorage(...)` into `context.filesDir/images`, so the files survive after the picker's temporary permission ends.
- **Previews:** Coil 3 `AsyncImage` thumbnails in the form, with a remove action; add `coil-compose` (and `coil-network-okhttp` if needed) to the version catalog.
- **Form state:** `ReportUiState` gains the attachments and an `attachmentsError`; `isValid` gains "at least one attachment"; `ReportViewModel` gains `onPhotosAttached(paths)` and remove, and `submit()` sets `attachmentsError`; `ReportSubmission` carries the attachments. The F-006 "Photos and videos" note becomes this section.
- **Creating the ticket:** `TicketsViewModel.reportTicket(...)` takes the attachments and fills `Ticket.attachments` (`Attachment.file` = local path, `uploadState = PENDING`). That file is Diogo's: agree the change with him (or he does it as a D task).

## Files
`ReportScreen.kt`, `ReportUiState.kt`, `ReportViewModel.kt`, `utils/FileUtils.kt` (new), `AndroidManifest.xml`, `res/xml/file_paths.xml` (new), `gradle/libs.versions.toml`, `app/build.gradle.kts`, strings EN + PT, previews.

## Acceptance checks
- [ ] The user can add up to 3 images/videos from the gallery and take a photo with the camera; each shows as a thumbnail and can be removed.
- [ ] Submitting with no attachment shows an error and does not create the ticket.
- [ ] Attached files are copied into the app's private storage and still show after rotation.
- [ ] Portrait and landscape work without clipped content; EN and PT strings.
- [ ] Every control has a label/content description and works with TalkBack.

## Out of scope
Thumbnails/compression/EXIF stripping before upload, upload to Firebase Storage with WorkManager (upload phase). Not copied from P09: the offline JSON queue and `PENDING_SYNC` (we have `UploadState`).

## After this task
Decision 0006's consequence "A phase-1 report has no attachment" no longer holds: Diogo updates it.

## References
- Plan input: `docs/work/F-006-report-screen/00-plan.md`, "F-007 follow-up"; decision 0008.
- Brief: `docs/brief/AMov.ProjetoKotlin_PT.pdf`; plan and order: [phase 1 index](README.md).

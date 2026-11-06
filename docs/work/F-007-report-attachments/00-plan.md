# F-007: Report attachments (photos and videos)
Type: feature · Task file: docs/tasks/phase-1/F-007-report-attachments.md
Labels: none
Revision 3 (2026-11-06), approved at Gate 1 with the person's answers to Q1, Q3 and Q13 applied.

## Problem
Brief I.2.E requires at least one image or video per report. F-006 left a note instead, so tickets are created with no media.

## Outcome
The form gets gallery and camera buttons, thumbnails with remove, a maximum of 3 attachments in total and a required-field error. Files are stored locally, and the ticket holds them (`file` = local path, `uploadState = PENDING`). This closes the I.2.E gap.

## Acceptance checks
Task checks:
- [ ] The user can add up to 3 images/videos from the gallery and take a photo with the camera; each shows as a thumbnail and can be removed.
- [ ] Submitting with no attachment shows an error and does not create the ticket.
- [ ] Attached files are copied into the app's private storage and still show after rotation.
- [ ] Portrait and landscape work without clipped content; EN and PT strings.
- [ ] Every control has a label/content description and works with TalkBack.

Added:
- [ ] A video shows a real frame with a play badge; an undecodable file shows the placeholder.
- [ ] The limit of 3 holds across picks and the camera, and on the `ACTION_OPEN_DOCUMENT` fallback (record as not checked if no API 24-29 AVD).
- [ ] Rotating with the picker or camera open still adds the item (P09 Test Case 3).
- [ ] A failed copy shows the notice and keeps the files that copied.
- [ ] Cancelling the picker or camera leaves no file.
- [ ] Process death with the camera open: the new photo is added; earlier attachments are lost (Q3).
- [ ] Remove deletes the file; leaving without submitting deletes all; after a submit the files stay.
- [ ] A PUBLIC ticket's detail shows one tile per attachment with the right label.
- [ ] In narrow landscape (2 grid columns) the third item and its remove button are reachable.
- [ ] The merged manifest has no `CAMERA` and no `INTERNET`.
- [ ] `ReportViewModel` stays no-arg and imports no `Context`; file work only in click handlers and launcher callbacks, never during composition; no `!!`.
- [ ] `assembleDebug` passes and lint shows 0 errors after every slice; warnings at 14 plus accepted Coil ones.

## Approach
- **Copy (Q13):** launchers in `ReportScreen`; the callback calls `FileUtils.copyUriToInternalStorage` synchronously, then `viewModel.onPhotosAttached(savedPaths)`, as slide 26.
- **Launcher placement:** the launchers and `tempCameraPath` must sit in `ReportScreen`, above the orientation branch. `rememberSaveable` is position-based, so inside a pane a rotation would lose the result.
- **ViewModel:** stays a no-arg plain `ViewModel`, with no Context, no coroutines and no `SavedStateHandle`. `viewModel()` in `MainActivity.kt` does not change.
- **Coil: pin 3.0.4** (P09) with `coil-video`, no `coil-network-okhttp`. 3.5+ needs Kotlin 2.4 and Compose foundation 1.11+, past the project's limits. 3.0.4 is unproven on Compose `ui` 1.11, so slice 4 checks it; on a crash, use 3.4.0, which also fits.
- **Thumbnails:** slide 27's `LazyVerticalStaggeredGrid`; `heightIn(max = 240.dp)` must stay, or the grid crashes inside the scrolling form. `AsyncImage(model = path)` for images; a per-request `VideoFrameDecoder` for videos.
- **Storage:** everything in `filesDir/images`. Names `IMG_<UUID>.<ext>`, `VID_<UUID>.<ext>`, `CAPTURE_<millis>.jpg`. The `VID_` prefix gives the type back from a path.
- **Limit of 3:** the callback copies only `uris.take(remainingAttachments)`, so a file over the limit is never written; a dropped pick shows the limit notice. The picker gets `setMaxItems(remaining)`; the last slot uses `PickVisualMedia`, because `maxItems` must be above 1. `ACTION_OPEN_DOCUMENT` ignores the limit, so the `take` is the real rule.
- **Camera:** `TakePicture` through `FileProvider`, no `CAMERA` permission (none in the merged manifest). If one is ever declared without being granted, the launch throws `SecurityException`. The pending path is in `rememberSaveable`.
- **Cleanup:** remove deletes the file; `onCleared` deletes the draft's files unless `submitted` (Challenge I).
- **Form state:** `attachmentsError` is the required rule and blocks; `attachmentsNotice` only informs.
- **Ticket:** `reportTicket` gains an `attachments` parameter (Q1).

| Event | Attachment list | Pending camera path | Other fields |
| --- | --- | --- | --- |
| Rotation | ViewModel | `rememberSaveable` | ViewModel |
| Process death | Lost; files orphaned (Q3, Q10) | `rememberSaveable` | Lost (Q3) |

### Differences from P09
- Real extensions and a `VID_` prefix, not a fixed `.jpg`: the form takes videos.
- `rememberSaveable` and no `!!`, unlike slide 7: Test Case 3 needs it.
- No `coil-network-okhttp`, unlike slide 16: local files only, no `INTERNET`.
- `coil-video` added, and remove buttons on the grid.
- The limit counts across picks, not per pick.
- `List<Attachment>`, not `List<String>`: the model already has it.

## Architecture impact
- New `utils/FileUtils.kt` (Android-dependent; `model/` must not import it) and `ui/components/AttachmentGallery.kt`.
- First `<provider>` of ours; new dependencies `coil-compose`, `coil-video`. `model/` unchanged.

## Failure modes
Those with special handling:
- **Copy fails:** partial file deleted, the rest attached, notice shown.
- **Camera missing or `SecurityException`:** caught at launch, notice shown.
- **Camera reports success with an empty file:** file deleted, notice shown.
- **`FileProvider` misconfigured:** left to crash on purpose; it is a development error.

## Risks and open questions
Decided by the person at Gate 1:
- **Q13. Where does the copy run? Literal P09, on the main thread, as slide 26.** No coroutine and no "Adding..." state. **Accepted exception to `.claude/rules/kotlin.md` line 15** (no blocking calls on the main thread). Known risk: a large video can freeze the UI or cause an ANR.
- **Q1. `reportTicket` and `MainActivity.kt` are Diogo's files** (exact change: 01-coding-plan). Francisco does it in slice 7, after Diogo agrees. The implementer must not start slice 7 until the person confirms Diogo agreed. F-007 is not done until it lands. Check that D-007 does not change the same lines.
- **Q3. What survives process death? Only the pending camera path,** through `rememberSaveable`. Attachments added earlier and the four other fields are lost, as F-006 Gate 1 Q1 accepted.

Accepted at Gate 1 with their defaults (Q4 and Q5 no longer apply):
- **Q2.** Coil 3.0.4, no network module, 3.4.0 as fallback.
- **Q6.** `delete`, `mkdirs` and `length` on the main thread: accepted, under the same exception as Q13.
- **Q7.** Video size limit: none.
- **Q8.** `CaptureVideo`: not included.
- **Q9.** Photo picker backport entry: not added.
- **Q10.** Orphaned files, and media in Auto Backup: accepted, rules unchanged. Attachments added before a process death are lost and their files orphaned; so are the files of in-memory tickets after a restart.
- **Q11.** `ShowScreen` and `TicketCard`: unchanged, so a new ticket shows "Not available yet".
- **Q12.** `Attachment.file` is an absolute path; Diogo updates the KDoc.

Other risks: local media keeps EXIF and video location metadata until the upload phase, which must strip both.

## Out of scope
WorkManager, Firebase and upload; stored thumbnails, compression and metadata stripping; P09's JSON queue and `PENDING_SYNC`; location; hiding the form from anonymous users; open F-006 review nits.

## Slices
Seven, listed in 01-coding-plan "Steps". The required rule comes after both ways of adding media, so every commit can still submit a report.

# F-009: Show attachment images on ticket card and detail
Type: bug (commit type `fix`) · Task file: none (the person's report, see `state.md`)
Labels: builds on the person's uncommitted edits in `ui/components/TicketCard.kt` and `ui/screens/ShowScreen.kt`

## Problem
After reporting a ticket with a camera photo and scope PUBLIC, the photo shows neither on the ticket card in the list nor on the ticket detail. The card shows the placeholder icon. The detail shows a "Photo" tile with an icon and the text "Not available yet".

## Outcome
The card shows the first attachment's photo, or a frame from the video. The detail shows each photo or video frame in its tile. An attachment with no file (the samples use `file = ""`) still shows the placeholder. If Q1 is approved, location tags are removed from a reported photo's EXIF before the ticket is stored.

## Acceptance checks
- [ ] Report with one camera photo, PUBLIC, submit: the new card shows the photo, cropped to the 64dp square.
- [ ] Open that ticket: the "Photos and videos" tile shows the photo, cropped to the 96dp square, labelled "Photo", with no "Not available yet".
- [ ] Report with a gallery video as the only attachment: the card and the tile show a video frame with the play badge.
- [ ] Report with a photo and a video: the detail shows two tiles. The card shows the first attachment.
- [ ] Sample-3 (`file = ""`) shows the placeholder on the card and "Not available yet" in its tile. No crash, no empty box.
- [ ] A file that cannot be decoded shows the placeholder (Coil `error`), not a blank box.
- [ ] Rotate the list and the detail: the images stay and nothing is clipped. Check PT as well.
- [ ] TalkBack: the card reads as before. A tile reads "Photo" or "Video" once, not twice.
- [ ] The report form's `AttachmentGallery` looks and behaves the same as before (slice 1 is a pure extraction).
- [ ] (Q1) Take a photo on a device whose camera saves location. After submit, `adb shell run-as pt.isec.amov.cityfix` plus `exiftool` (or Android Studio Device Explorer) shows no `GPS*` tags in `files/images/CAPTURE_*.jpg`.
- [ ] `./gradlew assembleDebug` and `./gradlew lint` pass with no new warnings (unused imports, unused variable).

## Reproduction
The planner did not run these steps: no device or emulator was attached in this session. They follow the code at `e4f2811`, without the person's uncommitted edits.
1. Start an emulator (API 34, Pixel profile; the emulator camera's virtual scene is enough). Run `./gradlew installDebug` and open CityFix.
2. On the list, tap Report.
3. Pick a category and a situation, type a description, and tap "Take photo". Shoot, then confirm. The form's gallery shows the photo, because `AttachmentGallery` draws it.
4. Choose scope PUBLIC and tap Submit.
5. Back on the list, find the new card. **Observed:** the placeholder icon. **Expected:** the photo.
6. Tap the card. **Observed:** "Photos and videos" shows one tile with the image icon, "Photo" and "Not available yet". **Expected:** the photo.

With the person's work-in-progress applied, step 5 likely already shows the image. The `AsyncImage` has no size modifier, so how it fills the box is unverified, and step 6 shows the image only for photos.

## Root cause
The data is correct and the UI never reads it.
- `ReportViewModel.attachmentFor` sets `Attachment.file` to the absolute path in `filesDir/images`. `LocalTicketsRepository.reportTicket` keeps the path, and `toAnonymousView()` (`model/TicketPrivacy.kt`) passes it through unchanged.
- `TicketCard.TicketThumbnail()` took no parameter and always drew `ic_image_placeholder`. `Attachment.thumbnail` is never set anywhere, so the card cannot rely on it.
- `ShowScreen.MediaTile` read only `attachment.type`. This was deliberate at F-005 ("Phase 1 has no real files"). F-007 started saving real files, but the two placeholders stayed and nothing moved the drawing to the new data.

Coil 3 already loads these paths: `AttachmentGallery` draws the same `file` value with `AsyncImage` in the report form. The fix is therefore UI-only, plus the privacy step in Q1.

## Approach
Follow the Contacts_#3 pattern: one shared composable, used by both the card and the detail, the same way `ContactAvatar` is used by `ContactCard` and `ShowScreen`.
- New `ui/components/AttachmentImage.kt`, `AttachmentImage(attachment, contentDescription, modifier)`. It is stateless and holds the model logic moved out of `AttachmentGallery`: a photo loads from the path string; a video loads through a `remember`ed `ImageRequest` with `VideoFrameDecoder.Factory()`. It uses `ContentScale.Crop` and fills the box it is given, with `ic_image_placeholder` as placeholder and error. Videos get the decorative play badge. A blank `file` draws the placeholder icon without calling Coil.
- `AttachmentGallery.AttachmentTile` calls `AttachmentImage`, so the model code exists in one place only.
- `TicketCard`: keep the person's change, but pass `ticket.attachments.firstOrNull()` (the whole `Attachment`) to `TicketThumbnail`. The thumbnail calls `AttachmentImage(..., Modifier.fillMaxSize())`, and `null` keeps the placeholder icon. This fixes `file == null` missing the samples' `""`, the missing size, and a first attachment that is a video. Remove the unused `CircleShape` import.
- `MediaTile`: replace the person's `when` (stray `var attachmentFile`, the `;`, and the empty `if` for VIDEO) with `AttachmentImage(attachment, contentDescription = null, Modifier.fillMaxSize())`. The image is decorative because the tile merges semantics and the label says "Photo"/"Video". Show "Not available yet" only when `file` is blank. Update the KDoc.
- (Q1) `FileUtils.removeImageLocation(path)` uses `androidx.exifinterface` to clear the GPS tags and save. `LocalTicketsRepository.reportTicket` calls it for every IMAGE attachment. That function already runs on `Dispatchers.IO`, sits in the data layer (CLAUDE.md: enforce privacy below the UI), and is the one point both the camera and the gallery paths pass through.

Alternatives considered:
- Set `Attachment.thumbnail` and draw it. This needs thumbnail generation, which belongs to the media/compression task and is not needed to show a local file.
- Two separate `AsyncImage` blocks in the card and the tile, as in the person's edits. They would repeat the video logic a third time.
- Strip EXIF in `AttachmentStorage` and in the camera callback. That needs two call sites and the camera path has no IO coroutine. One call in the repository is simpler.

## Architecture impact
- New stateless component `AttachmentImage` in `ui/components/`. `AttachmentGallery` changes internally only.
- `TicketCard` and `ShowScreen` now read `Attachment.file`. Until now the docs stated they never did (overview "Shared components" and "Screens" privacy bullets), so the docs must change.
- (Q1) New dependency `androidx.exifinterface:exifinterface` 1.4.2, the latest stable. The library's minSdk is at or below the AndroidX default of 23, so it works with minSdk 24; confirm when syncing. `FileUtils` gains one blocking function. `LocalTicketsRepository` calls `utils/`, which is already allowed for `data/`.

## Failure modes
- The file is missing or corrupt (deleted, or the process was restarted while the cache reseeds from the samples): Coil shows the `error` painter (placeholder).
- A video with no decodable frame: same placeholder, with the play badge.
- A large camera JPEG in a long list: Coil downsamples to the target size because the 64dp/96dp constraints are known once `fillMaxSize` is set. Without a size, Coil may decode the full resolution, which is another reason for the modifier.
- (Q1) `saveAttributes` fails (HEIC/HEIF or another unsupported write format, or an IO error): the function returns `false`, the report still goes through, and the file keeps its tags. See Q2.

## Risks and open questions
- **Q1 (privacy, decide at Gate 1). Recommended default: strip EXIF GPS from photos in this task, and record video metadata as deferred to the media task.** overview.md ("Media is outside the projection") and decision 0006 say metadata must be stripped *before* `MediaTile` draws real files, and this task is the one that starts drawing them. Pixels on screen do not reveal coordinates. The concern is that the anonymous data path holds files with exact GPS, which the later upload would publish.
  - Photos: one dependency, one roughly 15-line function and one call site. `ExifInterface.saveAttributes()` rewrites JPEG, PNG and WebP in place, so it is cheap.
  - Videos: no small API exists. Removing location atoms (`©xyz`, `udta`) needs a remux with `MediaExtractor` + `MediaMuxer` or Media3 Transformer, which is not minimal. Defer it, and record the deferral in overview.md and decision 0006.
  - The other option is to defer both. The bug fix would then ship drawing real files against the documented rule, and the deferral would have to be stated in the docs.
- **Q2 (if Q1 is yes): what happens when stripping fails?** Default: keep the attachment and the report, and accept a known limitation, recorded in the docs. A camera JPEG is always writable, so only a picked HEIC would hit this. The stricter option is to drop that attachment, or to reject a HEIC at pick time.
- **Q3: which tags?** Default: the GPS location tags only (latitude/longitude/altitude, their refs, and the GPS timestamp, datestamp, processing method and area information). Device make/model and capture time stay.
- Unverified: whether the system Photo Picker already redacts location from the files it returns. Q1 does not depend on it, because the camera path is affected either way.
- Unverified: whether the emulator camera writes GPS EXIF. The Q1 acceptance check may need a physical device. If none is available, record that the check was not run.
- The person's uncommitted edits are the base of slice 2. The implementer must not discard them, and must commit them only as part of slice 2.

## Out of scope
- Thumbnail generation, compression, the `Attachment.thumbnail` field, and upload (media task).
- Stripping video metadata (deferred, see Q1).
- A full-screen viewer or video playback on tap.
- Making sample tickets carry real images.
- D-007 projection changes.

## Slices
1. Extract `AttachmentImage` from `AttachmentGallery` (no behavior change). `refactor(F-009): extract attachment image component`
2. Draw attachments on the card and the detail with `AttachmentImage`, on top of the person's edits. `fix(F-009): show attachment images on ticket card and detail`
3. (Only if Q1 = yes) Strip EXIF GPS from reported photos. `fix(F-009): strip photo location before storing ticket`

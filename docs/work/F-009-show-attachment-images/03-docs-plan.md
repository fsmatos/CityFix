## Docs to update
- `docs/architecture/overview.md`:
  - "Shared components": in the `TicketCard` bullet, replace "placeholder thumbnail" with "first attachment through `AttachmentImage`, placeholder when there is none or the file is blank". Remove "never reads ... " only as far as `Attachment.file` is concerned; the card now reads it.
  - "Shared components": add an `AttachmentImage` bullet (photo path vs video `ImageRequest` with `VideoFrameDecoder`, Crop, placeholder/error, play badge, blank file draws the icon). In the `AttachmentGallery` bullet, say that it uses `AttachmentImage`.
  - "Screens", `ShowScreen`, Privacy bullet: remove `Attachment.file`/`thumbnail` from the "never reads" list and state that it reads `file` to draw it.
  - "Screens", `ShowScreen`, Media bullet: the tile draws the file; "Not available yet" appears only for a blank file.
  - "ViewModel (D-003)", "Media is outside the projection": update to the Q1 outcome. If Q1 is yes, say that photo GPS EXIF is stripped in `LocalTicketsRepository.reportTicket` (F-009) and that video metadata is still not stripped (deferred to the media task). Note that the Firebase repository must keep this step. Also update the matching privacy item under F-007/D-007 (around line 338).
  - `utils/` line in the layer list: `FileUtils` also strips photo location (if Q1 is yes).
- `docs/decisions/0006-anonymous-projection-phase-1.md`: amend the "Media files are not covered" point. Photo GPS is stripped at report time, not at upload compression (F-009). Video metadata is deferred to the media task. Record the Q2 failure behaviour.

## Decisions to record
- No new ADR. The media/privacy timing goes into decision 0006 as an amendment with a date and a link to F-009. If the person decides at Gate 1 to defer all stripping, record the deferral in the same place.

## Report notes
- Bug: the data layer already held real paths since F-007, but two phase-1 placeholders never read them. The lesson: when a later task makes data real, check every consumer that was built against the placeholder.
- A shared `AttachmentImage` follows the Contacts_#3 `ContactAvatar` pattern. One composable is reused by the form gallery, the list card and the detail, and the video-frame logic exists once.
- Privacy: EXIF GPS is stripped with `androidx.exifinterface` at the repository, the single point both the camera and the gallery paths pass through. Video location metadata needs a remux (`MediaMuxer` or Media3 Transformer) and is an improvement opportunity.
- Improvement: real thumbnails (`Attachment.thumbnail`) generated at compression would make the list cheaper than decoding full photos, even though Coil downsamples.

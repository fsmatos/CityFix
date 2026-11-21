# D-017: Open photos and play videos in the ticket detail
Type: bug · Task file: docs/tasks/phase-3/D-017-attachment-viewer.md
Labels: none

## Problem
Videos added as attachments cannot be watched (Francisco's test, item 8).

## Reproduction
1. As a registered citizen, report a ticket with a video (or, as an operator, resolve one with a video proof).
2. Open the ticket detail and tap the video tile.
3. Nothing happens: only the first frame with a play badge is ever shown.

## Root cause
`MediaTile` in `ui/screens/ShowScreen.kt` has no click handler, and the app has no player.
`AttachmentImage` only draws a thumbnail (or the first frame through Coil's `VideoFrameDecoder`).
The same goes for photos: they are never shown larger than 96 dp.

## Outcome
Tapping a tile with a file opens a full-screen viewer dialog: the photo at full size, or the
video in a player with play/pause and seek. Close button and system back dismiss it.

## Acceptance checks
- [ ] Video tile (uploaded, https URL) -> the video plays, with play/pause and seek.
- [ ] Video tile on the reporter's device before upload (local path) -> plays from the file.
- [ ] Photo tile -> the photo is shown at full size (Fit, not cropped).
- [ ] Resolution proof videos play too (operator and citizen detail).
- [ ] Rotation works; back and the close button dismiss the viewer; playback stops on close.
- [ ] A file that cannot be opened (for example another device's local path while the upload is pending) shows "Could not open this file" instead of crashing.
- [ ] Sample tiles without a file stay not clickable; TalkBack reads the tile as a button ("Open video 1").
- [ ] English and Portuguese strings.

## Approach
New stateless component `ui/components/AttachmentViewer.kt`: a full-screen `Dialog`
(`usePlatformDefaultWidth = false`) with
- photos: Coil `AsyncImage` of `attachment.file`, `ContentScale.Fit`;
- videos: `android.widget.VideoView` inside `AndroidView`, with `MediaController` for
  play/pause/seek, `setVideoURI(Uri.parse(file))` (https URL or absolute path), start on
  prepared, `setOnErrorListener` -> error text, `stopPlayback()` in `onRelease`/`DisposableEffect`.
`MediaSection` keeps the opened attachment in `rememberSaveable` (its id) and `MediaTile` gets
`Modifier.clickable(role = Role.Button)` when `file` is not blank.

Alternatives considered:
- Media3 ExoPlayer: better player, but a new dependency (3 artifacts) and more to explain at the defence, the day before the deadline.
- `ACTION_VIEW` intent to an external player: no in-app code, but a local file needs a FileProvider URI and the result depends on what the phone has installed.

## Architecture impact
UI only: one new component, a click on `MediaTile`, new strings. No model, repository, Firestore or rules change.

## Failure modes
- Pending upload on another device: `file` is that device's path -> VideoView error -> error text.
- No network: streaming fails -> error text.
- Rotation: the dialog's state is saved by attachment id and reopens; playback restarts from the beginning (accepted).

## Risks and open questions
- Anonymous users: `publicTickets` stores only REPORT thumbnails (JPEG), so anonymous users still see a frame, not the video. Brief I.1.C mentions videos for the public detail too. Fixing it means storing the video URLs in `publicTickets` (mapping and possibly rules). **Out of scope unless you say so.**

## Out of scope
- The report form gallery (`AttachmentGallery`): the user's own picks before sending.
- Ticket cards in lists.
- Bugs 6 and 7 (pending a decision with Francisco).

## Slices
1. `AttachmentViewer` component + strings (en, pt), click on `MediaTile` -> `fix(D-017): play videos and open photos in the ticket detail`.

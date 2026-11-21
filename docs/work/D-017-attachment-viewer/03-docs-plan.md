## Docs to update
- `docs/architecture/overview.md`: mention the attachment viewer under UI components, if components are listed there.

## Decisions to record
None (UI fix). VideoView over Media3 is noted in 00-plan and the report notes.

## Report notes
Video playback with the platform `VideoView` + `MediaController` in a Compose `AndroidView`: no extra dependency; Media3 would be the improvement (better buffering, keeps position on rotation).

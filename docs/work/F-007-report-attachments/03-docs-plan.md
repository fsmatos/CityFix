# F-007: docs plan
Revision 3 (2026-11-06), with the Gate 1 answers applied.

## Docs to update
- `docs/architecture/overview.md`:
  - "Report form (F-006)" and "Screens": replace the attachments bullets and the I.2.E gap with what was built.
  - "ViewModel (D-003)": the `reportTicket` signature and its empty-list null case; video metadata for the upload task.
  - "App shell (D-006)": Coil version and limits, resolved versions, lint count, `tempCameraPath`.
  - "Layers" and "Shared components": `FileUtils`, `AttachmentGallery`.
- `docs/tasks/phase-1/README.md`: the `reportTicket` signature.
- Handover to Diogo: decision 0006 (no-attachment consequence, video metadata) and the `Attachment.file` KDoc.

## Decisions to record
- No new decision record.
- `docs/decisions/0008-report-form-state.md`: one short note, because two things are left: the I.2.E gap is closed, and the person accepted the main-thread exception to `.claude/rules/kotlin.md` line 15 (Q13, Q6) with its ANR risk. The form-state decisions in 0008 stand unchanged.

## Report notes
Add to `docs/report/notes.md`:
- Differences from P09, and why the copy stays on the main thread as slide 26 (Q13).
- Why no `CAMERA` permission, and when it would fail.
- `maxItems > 1`, and the fallback picker ignoring the limit.
- Position-based `rememberSaveable`; the grid's height cap; pinning Coil.
- Limits: orphaned files, attachments lost on process death, device-local paths, nested scroll in narrow landscape, Auto Backup.
- Improvements: moving the copy off the main thread (a large video can freeze the UI or cause an ANR), compression and metadata stripping, real media in `ShowScreen` and `TicketCard`, video capture, a newer Coil.

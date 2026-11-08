# F-009: Show attachment images on ticket card and detail
Branch: dev/Francisco · Base: 247b368
Remote: not pushed

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-08 | bug fix (`/bug-fix`), no task file: description from the person |
| Plan | done | 2026-11-08 | 00-plan, 01-coding-plan, 03-docs-plan; 3 slices (slice 3 depends on Q1) |
| Gate 1: plan approved | done | 2026-11-08 | approved; Q1 strip photo GPS, defer video; Q2 keep report on failure; Q3 GPS tags only |
| Code | pending | | |
| Review | pending | | |
| Docs | pending | | |
| Pushed | pending | | |
| Merged | pending | | |

## Log
Append-only.
- 2026-11-08: fetched origin; origin/main had 0 commits missing from dev/Francisco; the person chose not to merge. Started F-009 from the person's report: after reporting a ticket with a camera photo (PUBLIC), the photo shows neither on the ticket card nor on the detail. The person has uncommitted work-in-progress in `ui/components/TicketCard.kt` and `ui/screens/ShowScreen.kt` (left uncommitted, not part of the start commit); the fix builds on it. Diagnosis before planning: the file path is kept in `Attachment.file` through `reportTicket` and `toAnonymousView()`; the card read the never-set `thumbnail` and `MediaTile` drew only icons. Contacts_#3 draws `picturePath` with `AsyncImage` in `ContactAvatar`, used by card and detail.
- 2026-11-08: Plan written (bug mode). Root cause: `TicketThumbnail` and `MediaTile` were F-005 placeholders that never read `Attachment.file`. F-007 made the files real and nothing moved the drawing to them. The data path is correct, and `AttachmentGallery` already draws the same paths with Coil. Approach: a shared `ui/components/AttachmentImage` (Contacts_#3 `ContactAvatar` pattern) built from `AttachmentGallery`'s photo/video model logic and used by the gallery, the card and `MediaTile`. Slices: (1) extract the component, (2) card and detail on top of the person's uncommitted edits, (3) if Q1 is approved, strip GPS EXIF with `androidx.exifinterface` 1.4.2 in `LocalTicketsRepository.reportTicket`. Open questions: Q1 strip photo GPS now and defer video metadata (default yes), Q2 keep the report if stripping fails (default yes), Q3 GPS tags only (default yes). The reproduction was not run: no device was attached in the planning session. No code changed and the person's edits were not touched.
- 2026-11-08: Gate 1: approved. Q1 yes: strip photo GPS EXIF in this task (slice 3, androidx.exifinterface), video metadata deferred to the media task and documented. Q2 yes: if stripping fails (e.g. HEIC), keep the attachment and the report; known limitation. Q3 yes: remove only GPS tags, keep make/model and capture time.

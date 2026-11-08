# F-009: Show attachment images on ticket card and detail
Branch: dev/Francisco · Base: 247b368
Remote: not pushed

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-08 | bug fix (`/bug-fix`), no task file: description from the person |
| Plan | pending | | |
| Gate 1: plan approved | pending | | |
| Code | pending | | |
| Review | pending | | |
| Docs | pending | | |
| Pushed | pending | | |
| Merged | pending | | |

## Log
Append-only.
- 2026-11-08: fetched origin; origin/main had 0 commits missing from dev/Francisco; the person chose not to merge. Started F-009 from the person's report: after reporting a ticket with a camera photo (PUBLIC), the photo shows neither on the ticket card nor on the detail. The person has uncommitted work-in-progress in `ui/components/TicketCard.kt` and `ui/screens/ShowScreen.kt` (left uncommitted, not part of the start commit); the fix builds on it. Diagnosis before planning: the file path is kept in `Attachment.file` through `reportTicket` and `toAnonymousView()`; the card read the never-set `thumbnail` and `MediaTile` drew only icons. Contacts_#3 draws `picturePath` with `AsyncImage` in `ContactAvatar`, used by card and detail.

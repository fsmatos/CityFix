# D-003: Review

Reviewer: reviewer agent · Date: 2026-10-29
Diff: `git diff b525154..HEAD` (f0272b8, 95f1c86, 168f775, 13cacdf, cba8e5c). `state.md` records Base 19e7bc5. The two commits in between, c401a5d (test removal) and b525154 (plan), come before the code and are outside this review.
Checked against: 00-plan, 01-coding-plan, task file, brief (I.1.A-C), overview.md, decisions 0002-0005, kotlin-conventions, CLAUDE.md.
Verification: `./gradlew assembleDebug lint` exit 0, and lint reports nothing in `TicketPrivacy.kt`, `SampleTickets.kt` or `TicketsViewModel.kt`. There are no tests, by Diogo's decision of 2026-10-29, and this review does not ask for any.

## Verdict
**Approve with should-fixes.** Nothing blocks. The code matches the coding plan almost line for line, and the deviations recorded in `state.md` are minor and justified. The should-fixes concern the anonymous projection: one free-text field it does not cover yet, and one media path it cannot cover by itself.

## What was checked and holds
- **Anonymous projection** (`model/TicketPrivacy.kt`). I compared it field by field with `Ticket.kt`, `StatusHistory.kt`, `Attachment.kt` and `Message.kt`:
  - it blanks `creatorId`, `followerIds`, `messages` (and with them `Message.authorId`), every `StatusHistory.responsibleId` and every `Attachment.uploadedById`;
  - it rounds `location` to 2 decimals. All three samples become (40.21, -8.42), which is not an exact point;
  - it has no Android imports, so it stays a pure rule.
  
  The one remaining text field is covered in S1.
- **Encapsulation.** `ticketsList`, `_tickets` and `_stats` are `private`, and `tickets` and `stats` are `StateFlow` exposed through `asStateFlow()`. No public member returns `TicketsList` or a `MutableStateFlow`. `ticketsList` is declared before the flows that read it in their initialisers. There is no `viewModelScope`, `GlobalScope` or coroutine. The only Android import is `androidx.lifecycle.ViewModel`, and no dependency was added.
- **`getTicketById`.** It searches `_tickets.value` with `firstOrNull`, so an unknown id returns `null`, and so do private, resolved and closed ids (Q4). The result is always projected.
- **Sample data.** I recomputed every epoch constant with `date -u` and all 7 match their UTC comments. The ids are distinct and non-blank, every ticket has `scope = PUBLIC`, and every `currentStatus` equals the last `statusHistory` entry (decision 0004). Each ticket's `creatorId` matches its first OPEN `responsibleId`. The locations are real, near Pr. Republica, and several hundred metres apart (far more than 10 m).
- **Dashboard figures.** Total 3, active 2 (sample-1 OPEN, sample-2 IN_PROGRESS), resolved 1. The average is 1_791_100_800_000 - 1_790_841_600_000 = 259_200_000 ms (3 days), and `activeRatio` is 2/3. `tickets` lists `sample-1` (Oct 20) and then `sample-2` (Oct 15). `sample-3` is RESOLVED and is excluded.
- **`reportTicket`.** It returns early on blank input, before building anything. The id is `UUID.randomUUID()`, and `TicketsList.add` also rejects a duplicate id. The new ticket has `currentStatus = OPEN` and one OPEN history entry. `refresh()` runs only after a successful `add`, so both flows update. The location stays at (0, 0), as Q8 requires.
- **Plan deviations.** The ones in `state.md` (message id `sample-1-msg-1`, named time constants, KDoc on `coarse()`) are all improvements. I found no other deviations.
- **MVVM, rotation, i18n, a11y, orientation, permissions.** The diff has no UI, resources or permissions. The state lives in a `ViewModel`, so it survives rotation. The sample texts are data, not UI strings, as the plan says.

## Blocking
None.

## Should-fix
- **S1. `StatusHistory.justification` is not blanked by `toAnonymousView()`.**
  - **Problem.** `justification` is free text written by a person: the citizen's reason for reopening, or the operator's info request. It can name the author or contain their contacts ("Sr. X, please call ..."). Brief I.1.C says the anonymous detail shows only images/videos and a coarse map, "ocultando dados pessoais do autor". Every sample entry has `null` here, so nothing leaks today, but the first reopen or info-request task would leak this text silently. The KDoc's field list also misses it, which defeats the "update this when Ticket gains a personal field" safeguard.
  - **Fix.** In `TicketPrivacy.kt`, change the history mapping to `statusHistory.map { it.copy(responsibleId = "", justification = null) }` and add "every StatusHistory.justification" to the KDoc list.
- **S2. Attachments can still carry the exact location, and the projection cannot see it.**
  - **Problem.** Phone photos usually hold GPS EXIF tags, and `Attachment.file`/`thumbnail` will later be Storage paths, which often contain the uploader's uid. `toAnonymousView()` copies both unchanged. The brief requires anonymous users to see the media, so the field cannot be dropped. The protection has to happen when the file is created, and right now nothing records that.
  - **Fix (documentation only, no code in D-003).**
    - Add one sentence to the `toAnonymousView()` KDoc: "Media files are passed through: EXIF location must be stripped at compression (utils/) and Storage paths must not contain user ids."
    - Add the same point to the D-003 docs plan, as a failure mode or overview note, so the media/WorkManager task picks it up.

## Nit
- **N1. A PRIVATE report returns an id that the detail screen cannot open.** `reportTicket(..., Scope.PRIVATE)` returns a non-null id, but `getTicketById(thatId)` returns `null`. If D-006 navigates to the detail screen after submit, the user sees "Ticket not found". Fix: add a line to the `reportTicket` KDoc, "The id may not be visible in [tickets] (PRIVATE scope); do not navigate to it", and repeat it in the D-006 contract in `overview.md` (docs phase).
- **N2. Input normalisation in `reportTicket` is uneven.** `description` is trimmed, but `categoryId` and `categoryName` are not, and a blank `categoryName` is accepted, which gives a list card with an empty category. Fix: `val id = categoryId.trim(); val name = categoryName.trim()`, and include `name.isBlank()` in the early `return null` (update the KDoc to match).
- **N3. All coarse locations collapse onto one grid point.** All three samples project to the same (40.21, -8.42). That is fine for privacy, but a future map draws stacked markers. Fix: none now. Add a one-line note to the map task (or the D-003 docs plan) that the anonymous map should cluster or offset markers, never use exact coordinates.
- **N4. `getTicketById` is a snapshot, not a stream.** If F-005/D-006 call it inside composition, the detail screen does not recompose when the ticket changes. Fix: add one sentence to the D-006 contract: derive the detail ticket from the collected `tickets` value (`tickets.firstOrNull { it.id == id }`) or `remember(id, tickets)`, rather than calling the function once.
- **N5. Category names are an English snapshot.** On a PT device, the sample `categoryName`s ("Public lighting") show in English. Tickets reported through F-006 store whatever locale the reporter used, which follows decision 0003's snapshot design. Fix (F-003/F-005 hand-off, not D-003 code): screens show the string resource for a known `categoryId` (`pavement`, `lighting`, `furniture`) and fall back to `categoryName`.

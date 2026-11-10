# D-007: Anonymous projection and expand toggle
Type: feature · Task file: docs/tasks/phase-1/D-007-anonymous-projection.md
Labels: none

## Problem
- `Ticket.toAnonymousView()` (`model/TicketPrivacy.kt`) still passes `description` and `statusHistory` through. The PUBLIC detail hides them only in the UI (`ShowScreen`'s `when (audience)`), which breaks the rule in `.claude/rules/kotlin.md` and CLAUDE.md that privacy is enforced below the UI (F-005 review S4, decision 0007).
- D-006 wired the list's expand toggle for everyone. In phase 1 every user is anonymous, so an expanded `TicketCard` shows the free-text description, which may hold personal data, and "Reported on <date>". Brief I.1.B lists only category, situation and thumbnail for the anonymous list, and the PUBLIC detail (I.1.C) hides both. So the list and the detail disagree (decision 0007, "List/detail consistency"; D-006 review, "Dependencies").
- Blanking `description` alone does not close the list leak: `createdAt` stays in the projection, so the expanded card would still show "Reported on" (D-006 review).

### What changed since the task file was written
The task file predates F-007, F-008 and F-009. Checked against the code at `0453ba4`:
- `toAnonymousView()` is now called only in `data/LocalTicketsRepository.kt` (`visibleTickets()` and `getTicketById`), not in `TicketsViewModel` (decision 0009). Neither call site needs to change: the change is inside the function.
- `TicketsViewModel.kt` only forwards what the repository returns. It does not change.
- Dashboard stats come from `LocalTicketsRepository.loadStats()` -> `ticketsList.stats()`, which reads the **raw** `TicketsList` (every ticket, private ones included, with full `statusHistory`), never the projected list. Clearing `statusHistory` in the projection therefore cannot change the average repair time or the counts. Expected figures with the samples stay total 3, active 2, resolved 1, average 3 days.
- `TicketsList.publicActive()` sorts newest first **before** the projection is applied, so the order does not depend on any projected field.
- `ShowScreen` (F-005, F-009) reads `description` and `statusHistory` only in the REGISTERED/OPERATOR branches, and "Reported on" sits inside `DescriptionSection`. It is correct before and after the change and needs no edit.
- `TicketCard` (F-009) draws the first attachment's file as the thumbnail; collapsed, it reads only `categoryName`, `situation`, `currentStatus` and the first attachment. Expanded, it adds `description` and `createdAt`.
- Ownership (phase-2 README): `MainActivity.kt` and `ui/navigation/*` are Diogo's; `ui/components/TicketCard.kt`, `ui/screens/ListScreen.kt` and `ui/screens/ShowScreen.kt` are Francisco's; `data/TicketsRepository.kt` is Francisco's from phase 2. The file list below follows that ownership.

## Outcome
- The projected ticket has `description = ""` and `statusHistory = emptyList()`, with `currentStatus` kept, so the status badge still shows.
- An anonymous user cannot read a ticket's description, history or report date anywhere: not in the list (collapsed or expanded), not in the detail.
- The dashboard figures do not change.
- Decision 0006 records the new projection, the toggle choice and the contract F-011 builds `publicTickets` on.

## Acceptance checks
- [ ] On the list, as the phase-1 (anonymous) user, there is no expand/collapse button in the top bar, in portrait and landscape, EN and PT. The cards show category, situation, status badge and thumbnail only, with no description and no "Reported on".
- [ ] The detail of `sample-1` and `sample-2` shows header, media and approximate location only (unchanged from D-006).
- [ ] A new PUBLIC report with a photo appears in the list with its thumbnail and no description; its detail shows no description or date.
- [ ] The dashboard shows the same figures before and after the change: 3 issues, 67% active ("2 of 3 issues"), average 3 days, "Based on 1 repair". After one new PUBLIC report: total 4, active 3.
- [ ] By reading the code: `toAnonymousView()` returns `description = ""` and `statusHistory = emptyList()` and keeps `currentStatus`; its KDoc lists every blanked field and the kept ones.
- [ ] The `MainTopAppBar` previews still render the list bar with the toggle (collapsed and expanded) and add one without it; `TicketCard`/`ListScreen` expanded previews still render (they use raw preview tickets).
- [ ] `./gradlew assembleDebug` and `./gradlew lint` pass, with no new lint warning (baseline 0 errors, 16 warnings).

## Approach

### 1. Projection (`model/TicketPrivacy.kt`)
`toAnonymousView()` also sets `description = ""` and `statusHistory = emptyList()`. `currentStatus` is a separate field (decision 0004), so it survives. Clearing the whole history replaces the per-entry blanking of `responsibleId` and `justification`, which becomes unnecessary; the KDoc says the history is dropped because brief I.1.C shows only media and a coarse map, and that this also removes the creator id in the first OPEN entry.

The KDoc becomes the **public projection contract** (section below), with the kept fields named as well as the dropped ones, so a reviewer can check it against `Ticket`, `Attachment` and `StatusHistory`.

### 2. The expand toggle for anonymous users (Gate 1 decision, Q1)
Options:

- **A. Hide the toggle and force cards collapsed for anonymous users (recommended).** `MainTopAppBar` gets a required `showExpandToggle: Boolean`; the toggle shows only when `screen == LIST && showExpandToggle`. In `CityFixApp`, one local `val canExpandCards = false` (phase 1: every user is anonymous; the Auth task sets it by role) feeds both `showExpandToggle = canExpandCards` and `ListScreen(isExpanded = canExpandCards && isExpanded)`. `isExpanded` and its `rememberSaveable` stay, ready for registered citizens and operators.
  - For: matches brief I.1.B exactly (category, situation, thumbnail); the list and the detail then show the same fields (plus the badge in both); no button that does nothing; only Diogo's files change; `TicketCard` keeps its simple `isExpanded` contract for later audiences.
  - Against: the expanded state can no longer be seen on a device in phase 1, so D-006's "expanded state kept on rotation" check drops out until roles exist (it still renders in the `TicketCard`/`ListScreen` previews). The `rememberSaveable` flag is carried with no visible effect until then.
- **B. Keep the toggle; the expanded anonymous card shows only allowed fields.** `TicketCard` (or `ListScreen`) gets an audience parameter, and the expanded PUBLIC card hides the description and date, or shows something allowed instead (a larger thumbnail, the media count).
  - Against: Francisco's files change, and the toggle needs a new design for a PUBLIC expanded card, which the brief does not ask for. With nothing extra to show, it is a button that does nothing visible.
- **C. Change only the projection, and also zero `createdAt`.** The card already skips the date for `0L`, so no UI change is needed.
  - Against: the toggle then does nothing visible (same problem as B). `createdAt` is needed to order the public list: here the sort runs before the projection, but F-011 orders `publicTickets` in Firestore by that field. And `0L` means "not set" (decision 0003), so the projection would fake missing data.

Recommendation: **A**.

### 3. Docs
Amend decision 0006 (the task requires it) and update the overview, the report notes and the task file in the Docs phase (`03-docs-plan.md`).

### Public projection contract (for F-011)
F-011 writes `publicTickets/{id}` from `toAnonymousView()` (its task: "Keep `toAnonymousView()` as the single function that builds a `publicTickets` document"). After D-007 the projected `Ticket` is:

| Field | In the projection | Why |
| --- | --- | --- |
| `id` | kept | needed to open the detail; a random UUID, not personal |
| `situation`, `categoryId`, `categoryName` | kept | brief I.1.B list fields |
| `scope` | kept (always PUBLIC, `publicActive()` filters) | harmless; F-011 may omit it |
| `currentStatus` | kept | status badge on the card and the detail; activity filter |
| `createdAt` | kept, **not shown** to anonymous users | ordering (Firestore `orderBy`); see Q2 |
| `location` | rounded to 2 decimals (about 1 km); (0, 0) stays (0, 0) | coarse map only (I.1.C) |
| `attachments` | kept, each with `uploadedById = ""`; `id`, `type`, `file`, `thumbnail`, `uploadState` pass through | I.1.B thumbnail, I.1.C media |
| `description` | `""` | **new**: free text, may hold personal data; not in I.1.B/I.1.C |
| `statusHistory` | empty | **new**: not in I.1.C; held the creator id and free-text justifications |
| `creatorId` | `""` | author |
| `followerIds`, `messages` | empty | other citizens, message authors |

Rules F-011 (and D-013) must keep:
- A projected ticket is a **view, not a valid ticket**: it breaks the "at least one history entry" rule (`Ticket` KDoc). Never write it back to `tickets/`, never run lifecycle rules (D-008) on it.
- Dashboard stats cannot come from `publicTickets`: they have no history (no RESOLVED date, so no repair time) and no private tickets. F-011's Gate 1 choice (counters or a stats document) must read from the full data or be maintained on write.
- 10 m grouping (`findNearby`) must run on full tickets: a 1 km grid point is useless at 10 m.
- The public document must be rewritten whenever a field it holds changes: status (D-013), attachments after upload (F-013), and deleted or rewritten when the scope or activity changes. That is F-011/D-013's design; D-007 only defines the content.
- Empty values may be written as missing fields instead of `""`/`[]`; the mapping must then read missing as the model default. Either way the Firestore rules must not let anonymous users read `tickets/`.
- A field added later to `Ticket`, `Attachment` or `StatusHistory` passes through until someone adds it to the projection. The KDoc and decision 0006 say so.

### Alternatives considered for the projection
- A separate `PublicTicket` type: rejected in decision 0006 (duplicates `Ticket`, breaks the agreed screen signatures); nothing here changes that.
- Keeping the history with only status and date (no ids, no justification): brief I.1.C says "apenas" images/videos and map, and the detail would not show it anyway. Rejected.

## Architecture impact
- `model/TicketPrivacy.kt`: two more fields cleared; the per-entry history mapping goes away. Pure Kotlin, no new imports.
- `ui/navigation/MainTopAppBar.kt`: one new required parameter. The bar stays stateless and still imports nothing from `model`, `viewmodel` or navigation.
- `MainActivity.kt`: one local flag that later becomes the role check; the toggle state stays where D-006 put it.
- No change to `data/`, `viewmodel/`, the screens, `TicketCard`, strings or Gradle.
- Decision 0006 amended; no new decision record (the task asks for an amendment).

## Failure modes
- **Status badge disappears:** if `currentStatus` were derived from the history somewhere in the UI. Checked: `TicketCard` and `ShowScreen` read `ticket.currentStatus`. The detail's PUBLIC branch never reads the history.
- **Dashboard figures change:** only if stats were computed from projected tickets. Checked: `loadStats()` uses the raw `TicketsList`. The emulator check compares the figures.
- **Order of the list changes:** no; the sort uses `createdAt` before the projection, and `createdAt` is kept.
- **Toggle state restored as `true`:** cannot expand cards, because `ListScreen` receives `canExpandCards && isExpanded`.
- **A later screen reuses the projected `tickets` for registered users or operators:** they would see no description and no history. Already a consequence in decision 0006 ("registered citizens and operators need a separate, unprojected source"); the amended KDoc repeats it.
- **Previews hide the change:** `ShowScreen`'s `previewTicket` is commented "already in anonymous form" but still has a description and history. It is used for both PUBLIC and REGISTERED previews, so the previews still render correctly; only the comment becomes inaccurate (Q5).

## Risks and open questions
- **Q1. What the toggle does for anonymous users.** Options A/B/C above. **Recommended: A** (hide the toggle, force cards collapsed, keep the state for later roles).
- **Q2. Keep `createdAt` in the projection?** It is not the author's personal data, it is needed to order the public list in Firestore (F-011), and the card and the detail no longer show it to anonymous users. It is then hidden from the anonymous user only in the UI, as the acceptance check "cannot read ... date" allows (it says "in the list or the detail"). **Recommended: keep it**, and say so in the KDoc and decision 0006. Alternative: round it to the day; it adds code for little gain.
- **Q3. Shape of the flag.** **Recommended:** a required `showExpandToggle: Boolean` on `MainTopAppBar` (no default, like `DetailAudience` in `ShowScreen`, so the Auth task must decide it at the call site), and a local `val canExpandCards = false` in `CityFixApp` with a comment pointing to the Auth task. Alternatives: a default `showExpandToggle = true` (hides the decision); passing a role or audience into the bar (the bar would import `model`; no role type exists yet).
- **Q4. Keep `isExpanded` and the toggle code, or delete them?** The phase-1 user can no longer reach them. **Recommended: keep** (the brief's registered and operator lists can use it, and the previews keep it compiling and visible). Deleting would also mean changing Francisco's `TicketCard`/`ListScreen`.
- **Q5. Francisco's files with comments that become inaccurate.** `ShowScreen.kt` (`previewTicket` "Already in anonymous form", the KDoc's "the anonymous projection happens upstream", still true) and `TicketCard.kt` (KDoc "isExpanded also shows the description and date", still true for non-anonymous viewers). **Recommended: do not edit them in D-007**; record the `previewTicket` comment as a handover to Francisco in the overview. Alternative: a one-line comment fix with his OK.
- **Q6. Decision 0007 (Francisco's record) says the projection change is "not yet done".** **Recommended:** amend only 0006 (Diogo's), and ask Francisco to add one status line to 0007 pointing to the amended 0006, or let the Docs phase add that line with his OK.
- **Q7. Other leaks found while checking, outside this task.** None reaches the screens as text, but they are recorded:
  - `Attachment.id`/`file` for a camera photo is `CAPTURE_<epoch millis>.jpg`, so the file name carries the capture time (close to the hidden `createdAt`); gallery copies use `IMG_/VID_<UUID>`. Not personal data, never shown; F-013 should not carry it into Storage paths.
  - Photo EXIF keeps make, model and capture time (F-009 Gate 1); video location metadata and XMP location are not removed (F-009 known limits, media task).
  - The coarse location collapses nearby tickets onto one grid point (decision 0006); a future map must cluster.
  - **Recommended:** no change in D-007; list them in decision 0006's amendment as known limits, with owners (F-013, media task).
- **Risk: the expanded-state rotation check from D-006 can no longer be run on a device in phase 1.** Accepted under option A; the previews cover the rendering.
- **Risk: `ShowScreenPrivacyPreview` and the expanded previews still feed raw tickets with descriptions.** That is intended (they prove the UI layer hides them, or show the non-anonymous card); no change.

## Out of scope
- Role checks and hiding the add button and the report form for anonymous users (Auth task, D-009).
- An unprojected source for registered citizens and operators (F-011/D-009).
- The Firestore `publicTickets` collection, its rules and its write path (F-011; D-007 only defines the content).
- Video metadata and XMP location stripping, Storage path naming (F-013 / media task).
- Changing `TicketCard`, `ListScreen` or `ShowScreen` (Francisco's files).
- `formatDate` taking a locale (F-005 review N7 follow-up).

## Slices
1. **Projection drops description and history.** `model/TicketPrivacy.kt` only: blank `description`, empty `statusHistory`, keep `currentStatus`; KDoc rewritten as the public projection contract. After this slice the collapsed list, the detail and the dashboard look the same; the expanded card shows only "Reported on" (closed by slice 2).
2. **Hide the expand toggle for anonymous users.** `ui/navigation/MainTopAppBar.kt` gets the required `showExpandToggle` parameter, its KDoc and a "list, no toggle" preview; `MainActivity.kt` adds `canExpandCards = false` and passes it to the bar and (with `&&`) to `ListScreen`. One commit, because the new required parameter does not compile without the call site.

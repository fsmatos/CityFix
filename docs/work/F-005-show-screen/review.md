# F-005: ShowScreen — review
Reviewed: 2026-11-01 · Range: `c8b5500..HEAD` (bb3f446, 96d4823, aa03eda, 45fe6f4, 8e1b6f1, plus `state.md` in 488caf6)
Against: 00-plan (revision 2, with the Gate 1 decisions at the top), 01-coding-plan, the deviations in `state.md`, the task file `docs/tasks/phase-1/F-005-show-screen.md`, the brief (extracted with `pdftotext -layout`: I.1.B, I.1.C, I.2.D, I.2.E, I.3.E, V), `docs/architecture/overview.md`, decisions 0004 and 0006, `kotlin-conventions` and `.claude/rules/kotlin.md`.

**Verdict: approve with should-fixes.** Nothing blocks. The code follows the approved plan and the Gate 1 decisions. The PUBLIC view reads nothing the brief hides, and build and lint are at baseline. The four logged deviations (the `showStatus` flag, one `.location` read, the shared `DetailsPane`/`LocationPane`, no `LocalInspectionMode`) are sound and make the code easier to explain. The should-fixes are cheap. S1 (media tiles invisible against the background) and S2 (scroll position lost on rotation) are code fixes. S3 (what REGISTERED means) and S4 (privacy enforced only in the UI) are KDoc and docs work so the brief's rules hold once roles exist.

## What was checked
- **Build and lint (run by the reviewer):** `./gradlew assembleDebug` passes. `./gradlew lint` gives 0 errors and 13 warnings, the baseline: AndroidGradlePluginVersion 1, GradleDependency 1, NewerVersionAvailable 3, RedundantLabel 1, UnusedResources 7 (all `colors.xml`). There is no ModifierParameter, MissingTranslation or UnusedResources warning on a new key.
- **Files touched:** `ui/screens/ShowScreen.kt` (new), `ui/components/CoarseLocationMap.kt` (new), `utils/DateFormatting.kt` (+`formatDateTime`), both `strings.xml`, and `state.md`. `git diff --stat` over `model/`, `viewmodel/`, `data/`, `ui/navigation/`, `utils/Distance.kt`, `ui/theme/`, `StatusBadge.kt`, `TicketCard.kt`, `MainActivity.kt`, the manifest, the Gradle files and `docs/tasks/` is empty. Diogo's folders are untouched, and nothing was added to the catalog or the manifest (slice 6 dropped, as Gate 1 decided).
- **Plan greps (re-run on the final files):**
  - `\.(creatorId|followerIds|messages|responsibleId|justification|uploadedById)\b` in `ShowScreen.kt`: no match.
  - `\.(file|thumbnail|uploadState)\b`: no match. `MediaTile` reads only `attachment.type`.
  - `\.description\b`: lines 225 and 232, both in `DescriptionSection`, which is called only from the REGISTERED/OPERATOR arm of `DetailsPane`.
  - `\.statusHistory\b`: line 175 only, in the REGISTERED/OPERATOR arm of `LocationPane`.
  - `\.location\b`: line 170 only (`CoarseLocationMap(ticket.location)`).
  - `latitude|longitude`: absent from `ShowScreen.kt`, `CoarseLocationMap.kt` and both string files. The component only compares with `Coordinates()`.
  - `currentStatus`: read only at line 201, inside `if (showStatus)`, and `showStatus` is `true` only for REGISTERED/OPERATOR.
  - `sortedBy|reversed|LazyColumn`: no match, so the history appears in stored order (decision 0004).
  - `import android` in `DateFormatting.kt`: no match, so `utils/` stays pure Kotlin.
  - `mutableStateOf|remember(` other than `rememberScrollState`, and `ViewModel`: no match.
- **Privacy for PUBLIC (brief I.1.C, CLAUDE.md):** following the code path for `audience = PUBLIC` shows the category, situation, "Photos and videos" (type tiles or "No photos or videos.") and "Approximate location" (placeholder and caption, or "Location not available"). It shows no description, history, badge, "Reported on", author data or coordinate text. `ShowScreenPrivacyPreview` feeds the raw `sample-2` (author `citizen-2`, a follower, exact (40.2075, -8.4210), a description), and none of these can reach a `Text`: the location goes only into `CoarseLocationMap`, which renders no coordinates.
- **Brief alignment:**
  - I.1.C: met, apart from the accepted map gap (static placeholder, Gate 1 Q7). The header (category + situation) follows the Gate 1 Q2 interpretation.
  - I.2.E (messages) and I.3.E (coordinates, author contacts): out of scope. The exhaustive `when (audience)` is the place to add them. OPERATOR = REGISTERED in phase 1, as planned.
  - V: portrait and landscape, EN default + PT, accessibility headings and merged nodes, no permissions needed. See S1 and S2.
- **Strings:** 13 keys in both files, same names, in the same `<!-- Ticket detail -->` block. PT uses "ocorrência" (D-005 review S1) and pt-PT wording ("registadas", "Fotografia"). The reused `ticket_reported_on` (`%1$s`) and `ticket_no_category` exist in both.
- **Previews:** PUBLIC and REGISTERED each have portrait, landscape, PT and PT landscape. Not-found has portrait, landscape and PT. Empty history has EN and PT. Privacy has EN. `CoarseLocationMap` has location and (0, 0) in EN and PT. All are `private`, wrapped in `CityFixTheme`, and use the shared landscape `spec:`. `PREVIEW_CREATED = 1_792_089_000_000` is 2026-10-15 18:30 UTC, as its KDoc says.
- **ModifierParameter:** every composable (`ShowScreen`, `DetailsPane`, `LocationPane`, `TicketHeader`, `DescriptionSection`, `StatusHistorySection`, `HistoryRow`, `MediaSection`, `MediaTile`, `CoarseLocationMap`) has `modifier: Modifier = Modifier` as its first optional parameter and applies it once, to its root. `ShowScreen` applies it to the root of whichever of its three branches is drawn.
- **Accessibility:** section titles have `heading()`. `HistoryRow` and `MediaTile` use `semantics(mergeDescendants = true)`. The decorative icons have `contentDescription = null` next to a text label, and the map box has `cd_ticket_map`. Nothing is interactive, so the 48dp touch-target rule does not apply.
- **MVVM / stateless:** stateless, data in only; the only remembered state is `rememberScrollState()`. This follows the overview "Screens" phase-1 pattern, where the screen is the stateless half and D-006's `composable<TicketDetailRoute>` is the stateful half.
- **Base:** `state.md` says `Base: ad7ca62`. `ad7ca62..c8b5500` holds only the start and plan commits, so the code range is the same.

## Blocking
None.

## Should-fix

**S1. The media tile box is invisible against the screen background.** `MediaTile` draws its 96dp box with `background(MaterialTheme.colorScheme.surface)`. In `Theme.kt`, `surface` and `background` are both `0xFFFFFBFE`, so on the screen the tile has no visible edge: you see a floating icon above "Photo / Not available yet". The plan said "`surface` box" because it copied `TicketCard`, where the same box sits on the card container and so is visible. Brief V asks for "cuidado da apresentação".
- Fix: in `ShowScreen.kt` `MediaTile`, use `.background(MaterialTheme.colorScheme.surfaceVariant)`, as `CoarseLocationMap` does. Keep the `onSurfaceVariant` icon tint, which has good contrast on `surfaceVariant`. Check the PUBLIC preview.

**S2. The scroll position is lost on rotation.** Each `rememberScrollState()` is called in a different branch (the portrait `Column`, then each landscape pane), and `rememberSaveable` keys its value by call site. After a rotation the new branch finds no saved value, so the page jumps back to the top. The kotlin-conventions say "Support portrait and landscape without losing state", and brief V says rotation must not harm "o funcionamento ... ou os elementos a processar". The overview makes the same promise for the dashboard.
- Fix: hoist the states to the top of `ShowScreen`, after the `null` check, so both orientations use the same call sites:
  ```kotlin
  val detailsScroll = rememberScrollState()
  val locationScroll = rememberScrollState()
  ```
  The portrait `Column` uses `verticalScroll(detailsScroll)`. In landscape, `DetailsPane` uses `detailsScroll` and `LocationPane` uses `locationScroll`. The offset is restored and clamped to the new maximum, so portrait to landscape keeps you near the same place in the details. Re-run lint; nothing else changes.

**S3. "REGISTERED" does not say whose ticket it is, and the brief makes the difference.** I.2.D gives a registered citizen "acesso às funcionalidades do utilizador anónimo", so for someone else's public ticket they get the I.1.C view. The I.2.E detail (with messages) applies to "ocorrências (do utilizador)", the citizen's own tickets. As written, `DetailAudience.REGISTERED` shows the description and the full history of any ticket. If the Auth task maps "logged in as a citizen" to REGISTERED (handover (e) as worded in `state.md`), a citizen would see another author's free-text description, which may hold personal data. This is the kind of question the defence asks.
- Fix (KDoc and handover only, no behaviour change):
  - the KDoc on `DetailAudience` should say that REGISTERED means "the citizen who created or follows this ticket (brief I.2.E)", and that any other registered citizen gets PUBLIC (I.2.D);
  - reword handover (e) in `state.md` and in the docs phase (overview "Screens", ADR 0007) the same way: the Auth task maps the role *and* the creator/follower relation to `DetailAudience`.

**S4. Description and history are hidden from anonymous users only in the UI.** `toAnonymousView()` still passes `description` and `statusHistory` through. So for those two fields the CLAUDE.md non-negotiable rule and `.claude/rules/kotlin.md` ("enforce in the repository layer, not only in the UI") hold only through `ShowScreen`'s `when`. Meanwhile `TicketCard(isExpanded = true)` already shows the same description in the anonymous list. Gate 1 accepted this with handover (b) to Diogo, so it does not block F-005: the screen is correct both before and after his change. But nothing yet tracks it outside this folder.
- Fix (docs phase):
  - write ADR 0007 as 03-docs-plan says, with an explicit "Consequences" item: the projection must blank `description` and clear `statusHistory`;
  - add the same item to the overview "ViewModel (D-003)" section;
  - before `/push-task`, ask the person to raise it with Diogo as a task, because only he can change `model/TicketPrivacy.kt` and decision 0006.

## Nit

**N1. A code comment cites the wrong brief item.** `DetailsPane` says `// Brief I.1.C: anonymous users see category and situation only, no status`. I.1.C does not list category or situation. The choice comes from Gate 1 Q2 (the I.1.B fields). At the defence, a reader holding the PDF will not find it in I.1.C.
- Fix: `// Gate 1 Q2: anonymous users get I.1.B's fields (category, situation); I.1.C lists no status`. Adjust the `ShowScreen` KDoc's "PUBLIC (brief I.1.C): category, situation, ..." the same way.

**N2. The anonymous list and the detail disagree on the status badge.** `TicketCard` shows the `StatusBadge` to anonymous users in the list. Under Gate 1 Q2 the anonymous detail hides it. The same user sees "In progress" on the card and not on the detail, which is hard to explain. It also goes beyond I.1.B ("categoria, situação e thumbnail") in the same way as the expanded description (F-003 Q2).
- Fix: no code change in F-005. Record it in ADR 0007 next to the F-003 Q2 note, so the person decides list and detail together: either the badge also goes from the anonymous card, or the Q2 rationale explains the difference.

**N3. The not-found state has no PT landscape preview.** The coding-plan conventions ask for "a PT landscape for each main state".
- Fix: add `@Preview(showBackground = true, locale = "pt", device = "spec:width=411dp,height=891dp,orientation=landscape")` to `ShowScreenNotFoundPreview`.

**N4. The privacy preview picks the sample by index.** `SampleTickets.all[1]` silently changes meaning if Diogo reorders or adds samples, and the KDoc depends on it being `sample-2`.
- Fix: `SampleTickets.all.first { it.id == "sample-2" }`.

**N5. The ticket title is not a heading.** The section titles are headings, but the category (`headlineSmall`, the page title) is not, so TalkBack's heading navigation skips the top of the page.
- Fix: add `modifier = Modifier.semantics { heading() }` to the category `Text` in `TicketHeader`.

**N6. The map placeholder's spoken label promises a map.** `cd_ticket_map` reads "Map of the approximate area of the issue" over a box that holds only a pin icon. The caption "Approximate area (about 1 km)" is a separate TalkBack stop right after it.
- Fix: put `semantics(mergeDescendants = true) {}` on a wrapper around the box and the caption, so TalkBack reads them as one item. Optionally make the description honest until a real map exists (for example "Approximate area of the issue, map not available yet"), in EN and PT. Either way, record it with the accepted I.1.C gap.

**N7. The two date helpers take the locale differently.** `formatDate(epochMillis)` uses the default locale, while the new `formatDateTime(epochMillis, locale)` takes it as a parameter (F-002 review S3). A PT preview of `TicketCard` and of `ShowScreen` can therefore format dates in different languages.
- Fix: leave `formatDate` alone in F-005 (shared F-002 code). Note it in the docs phase as a follow-up: give `formatDate` a `locale` parameter, and have `TicketCard` pass the `LocalConfiguration` locale.

**N8. Task-file wording (plan Q11) is still open.** The task check "Status history shown in order with timestamps" can now be seen only in REGISTERED previews, never on a device in phase 1.
- Fix: in the docs phase, ask the person whether to reword `docs/tasks/phase-1/F-005-show-screen.md` before ticking it. The reviewer does not edit it.

## Manual checks left to the person
- Open the PUBLIC, REGISTERED and privacy previews in Android Studio. Confirm the content listed above, and the PT dates and labels in the PT previews.
- In the landscape previews, confirm the two panes sit side by side and scroll on their own.
- Use the accessibility inspector (or TalkBack after D-006) to confirm one node per history row and per tile, and headings on the section titles.

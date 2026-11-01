# F-003 review: ListScreen

Reviewed: `git diff 733c2f2..HEAD` (commits 57f1abe, 2b31f47, 59adbb8; task Base in `state.md` is ca22f44, and 733c2f2 is the plan commit). Checked against `00-plan.md`, `01-coding-plan.md`, `03-docs-plan.md`, the task file `docs/tasks/phase-1/F-003-list-screen.md`, the brief (I.1.B, I.1.C, V), `docs/architecture/overview.md` and the kotlin-conventions skill. Date: 2026-10-29.

## Verdict
**Approve with should-fix.** There are no blocking findings. The code matches `01-coding-plan.md` almost line for line: the signature and KDoc, the `LazyColumn` with `items(tickets, key = { it.id })`, the click that passes the id, the empty branch, the string key in EN and PT in the same commit, the private preview data p1..p6 and the previews. I ran `./gradlew lint` again myself. It passes with the same 13 earlier warnings (AndroidGradlePluginVersion, GradleDependency, NewerVersionAvailable x3, RedundantLabel, UnusedResources x7). None are in `ListScreen.kt` and there is no `MissingTranslation`. The should-fix items are about handover and manual checks, not the code.

Context, decided at Gate 1 and not counted against this task:
- Q1: there is no stateful wrapper. `ListScreen` is the stateless half, and D-006 collects `vm.tickets`. Brief V grades "MVVM, com segmentação stateless e stateful", so the stateful half has to exist in D-006's `composable<TicketListRoute>` block. Be ready to point to it at the defence.
- Q2: `isExpanded` is passed straight through, so expanded cards show the free-text `description` to anonymous users, which goes beyond brief I.1.B ("categoria, situação e thumbnail"). The person accepted this knowingly. See S1 for the handover this needs.
- Q3: the `LazyColumn` is the same in landscape.

Checked and fine:
- MVVM/stateless: `ListScreen.kt` has no `remember`, `mutableStateOf`, ViewModel, repository or Firebase. Data comes in and the lambda goes out. `modifier` is the last parameter, defaults to `Modifier`, and is applied to the root of whichever branch is drawn.
- Filtering stays upstream, as planned. The screen does not filter, sort or check `isActive`. `TicketsViewModel.tickets` is `publicActive().map { it.toAnonymousView() }`, so the screen only ever gets PUBLIC, active, projected tickets.
- Personal data: `ListScreen.kt` reads only `ticket.id`. A grep for `location|creatorId|followerIds|messages` finds nothing. The card reads no restricted field (F-002). The only open exposure is the description under Q2.
- Keys: `key = { it.id }`. Ids are distinct in the previews and guaranteed non-blank and unique upstream (`TicketsList.add`).
- Rotation: `LazyColumn` uses `rememberLazyListState()` by default, which is saveable, so the scroll position survives rotation. The list itself lives in the ViewModel.
- i18n: `ticket_list_empty` is in both `values/` and `values-pt/` under the same comment. "ocorrências" matches the brief's wording. The PT empty preview exists.
- Accessibility: the empty state is real text (TalkBack reads it) in `bodyLarge` with `onSurfaceVariant` on the surface, which is enough contrast. The cards are full-width `Card(onClick)` targets well above 48dp. Text sizes come from typography, so font scaling works, and the 32dp horizontal padding lets long PT text wrap.
- Orientation: there are portrait and landscape previews for both the data list and the empty list. The list scrolls, so nothing is clipped at the bottom.
- Permissions: not applicable. The screen needs no runtime permission.
- Course alignment: `LazyColumn`, `items`, `Box`, `Text` and `stringResource` are all P0x material. There is nothing hard to explain.

## Blocking
None.

## Should-fix

**S1. The Q2 handover to Diogo is not recorded anywhere he will read it.**
The Q2 default in `00-plan.md` says: "log in `state.md` and tell Diogo that D-005/D-006 decide whether the toggle is shown to anonymous users". The Gate 1 log line records the decision, but not that Diogo was told. The F-002 review S1 fix (a KDoc line on `TicketCard` and a reworded overview note) was not applied either. The overview "Shared components" bullet still describes the expanded card without the anonymous caveat. In phase 1 every user is anonymous, so if D-005 wires the expand toggle unconditionally, the anonymous list shows descriptions. That is free text and may hold names, phone numbers or exact addresses, which goes against CLAUDE.md ("Anonymous users must never see the author's personal data or exact coordinates").
Fix:
- In the Docs phase, the new overview "Screens" entry for `ListScreen` should say plainly: "`isExpanded = true` shows the description; D-005/D-006 must hide or force off the expand toggle for anonymous users once roles exist (brief I.1.B)". Also add the same caveat to the "Shared components" `TicketCard` bullet.
- Append a `state.md` log line saying when and how Diogo was told, or that the overview note is the handover.
- Optional, still within the Gate 1 decision: put the same sentence in the `ListScreen` KDoc, e.g. "Expanded cards also show the description; anonymous callers should pass false (brief I.1.B)."

**S2. The manual acceptance checks have not been run.**
`state.md` says the Android Studio checks for both slices are pending: previews render, the landscape list scrolls with no clipped card, the tap ripple shows and `onTicketClick` gets the id, and the empty state appears in EN and PT. Four of the eight acceptance checks in `00-plan.md` depend on them, and `03-docs-plan.md` only ticks the task file once they pass.
Fix: run the checks in Android Studio. Use interactive preview mode on `ListScreenPreview` for the tap, and the `locale = "pt"` empty preview for PT. Record the results in the `state.md` log before the Docs phase.

## Nit

**N1. There is no PT preview of the list with data.**
Only the empty state has a `locale = "pt"` preview. The PT labels on the cards are covered by F-002's `TicketCardExpandedPreview`, so this is optional. Adding `@Preview(showBackground = true, locale = "pt")` to `ListScreenExpandedPreview` would let the defence show the whole screen in PT without changing the device language. The date line stays in English in that preview because `formatDate` uses `Locale.getDefault()` (see F-002 review S3).

**N2. The expanded list has no landscape preview.**
`ListScreenExpandedPreview` is portrait only. Expanded cards are the tallest rows, so they are the most likely to look cramped in landscape (891x411dp). Fix: add the same `spec:width=411dp,height=891dp,orientation=landscape` preview line to `ListScreenExpandedPreview`.

**N3. The landscape side insets depend on D-006.**
`contentPadding = PaddingValues(16.dp)` is fixed. In landscape on a phone with a camera cutout or 3-button navigation, the side inset is horizontal. The `Scaffold`'s default `contentWindowInsets` covers system bars but not the display cutout, so a card edge could sit under the notch. This is not F-003's job. Fix: when D-006 wires `Modifier.padding(innerPadding)`, check landscape on a device with a cutout, and add `Modifier.windowInsetsPadding(WindowInsets.displayCutout)` (or set `Scaffold(contentWindowInsets = WindowInsets.safeDrawing)`) if needed. Mention it to Diogo together with S1.

**N4. The plan's forbidden-word grep catches its own KDoc.**
The coding-plan verify step greps for `filter`, which matches "filtering ... happen upstream" in the KDoc the same plan prescribes. The implementer logged this. Nothing in the code needs to change. Fix: in future plans, grep for code patterns (`\.filter\b|sortedBy|isActive`) rather than bare words.

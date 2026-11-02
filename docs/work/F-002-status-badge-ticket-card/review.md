# F-002 review: StatusBadge and TicketCard

Reviewed: `git diff 9ec32fe..HEAD` (commits 5866200, 72283bb, 7bcdcd7) against `00-plan.md`, `01-coding-plan.md`, `03-docs-plan.md`, the task file, the brief (I.1.B, I.1.C, I.2.E, II, III, V), `docs/architecture/overview.md`, `.claude/rules/kotlin.md` and the kotlin-conventions skill. Date: 2026-10-28.

## Verdict
**Approve with should-fix.** No blocking findings. The code matches the approved plan (slices, signatures, strings, Q1-Q5 defaults), is stateless, reads no restricted ticket fields, has EN + PT for every new string with the PT wording from the brief, and has portrait + landscape previews. I re-ran `./gradlew lint` myself: it passes with the same 10 earlier warnings (7 UnusedResources, GradleDependency, NewerVersionAvailable, RedundantLabel), none from F-002, no `MissingTranslation`. The three logged deviations (hand-written vector, extra preview data, landscape `spec:` preview kept) are reasonable and explained. Fix S1-S3 and do the manual checks before Docs/Push.

Checked and fine:
- MVVM/stateless: no `remember`, `mutableStateOf`, ViewModel, repository or Firebase in `ui/components/`; `isExpanded` comes from the caller; `modifier` is the last parameter and is applied to the root.
- Labels: exhaustive `when` without `else` in `statusLabelRes` / `situationLabelRes`; no `.name`; no `@StringRes`, as decided at Gate 1.
- Privacy: `TicketCard.kt` never reads `location`, `creatorId`, `followerIds` or `messages` (see S1 for `description`).
- i18n: 12 keys in both `values/` and `values-pt/`; PT statuses (Aberto, Pedido de informações, Em resolução, Resolvido, Encerrado) and situations (Dano, Avaria, Destruição, Roubo) match the brief; `%1$s` matches in both files.
- Accessibility: `Card(onClick)` is full width and about 88dp tall; the badge always has text, so colour is not the only signal; text uses theme typography (`sp`); the contrast of white on blue, orange, teal and green is above 4.5:1 for the 12sp label.
- Orientation: the card fills the width and the category/description ellipsise; previews exist in both orientations.
- `formatDate` uses `java.text.DateFormat`, which works on minSdk 24 without desugaring, as planned.
- Course alignment: `Surface`, `Card`, `Row`/`Column`, `painterResource`, `stringResource` and `DateFormat` are all standard. Nothing is unnecessarily advanced.

## Blocking
None.

## Should-fix

**S1. The expanded card shows the free-text description, and nothing stops the anonymous list from showing it.**
Brief I.1.B limits the anonymous list to "categoria, situação e thumbnail", and I.1.C limits the anonymous detail to "apenas com imagem(ns) e/ou vídeo(s) e mapa com zoom afastado". `description` is text the citizen typed, so it can contain a name, a phone number or an exact address ("in front of my house, Rua X 12"). That would get round the "no personal data, no exact location" rule (CLAUDE.md, brief I.1.C). The card itself is fine. The gap is that `00-plan.md` and the planned overview note call the card "safe in the anonymous list" without any condition, so F-003, D-005 and D-006 could wire the global expand toggle for anonymous users too.
Fix (docs only, no code change needed now):
- Add a second KDoc line to `TicketCard`: `Anonymous list: always pass isExpanded = false (brief I.1.B/I.1.C: the description is free text and may hold personal data).`
- In the Docs phase, write the overview "Shared components" note as "collapsed card is anonymous-safe; the expanded card adds the description, which anonymous users must not see". Tell Diogo (D-005/D-006) that the expand toggle is hidden or forced off for the anonymous role. Add this to F-003's plan when it is written.
- Record the condition in `state.md` as a clarification of the plan's Outcome line.

**S2. The CLOSED badge and the thumbnail box are almost invisible on the card background.**
The default M3 `Card` container is `surfaceContainerHighest` (baseline light `#E6E0E9`). The CLOSED badge and the thumbnail box both use `surfaceVariant` (`#E7E0EC`), which is practically the same colour. Inside a card, CLOSED therefore shows as plain grey text with no badge shape, so the acceptance check "one badge colour per status" fails visually for CLOSED (the text still carries the meaning, so this is not an accessibility failure). The placeholder box also has no visible edge. The `StatusBadgePreview` hides the problem because it draws the badges on the white preview background, not inside a card.
Fix:
- `StatusBadge`, CLOSED: `containerColor = MaterialTheme.colorScheme.inverseSurface`, `textColor = MaterialTheme.colorScheme.inverseOnSurface` (dark grey with light text: high contrast, still reads as "inactive", and still a theme role).
- `TicketThumbnail`: `.background(MaterialTheme.colorScheme.surface)` instead of `surfaceVariant`, and keep `tint = onSurfaceVariant`.
- Check it in `TicketCardListPreview` (t5 is CLOSED), not only in the badge preview.

**S3. The manual acceptance checks have not been done yet.**
`state.md` lists these as still to do: previews render (including the hand-written vector and the `spec:` landscape device), ellipsis on t2, tap ripple, PT labels, landscape on a device. Four of the acceptance checks depend on them. They have to pass before the Docs phase ticks the task file.
Fix:
- Do the checks in Android Studio and record the result in the `state.md` Log.
- Keep one PT preview permanently so PT can be shown at the defence without changing the device language: add `@Preview(showBackground = true, locale = "pt")` to `TicketCardExpandedPreview`. Note that `formatDate` uses `Locale.getDefault()`, which the preview `locale` does not change, so the date stays in English in that preview while the labels switch to PT. On a device with Portuguese as the language the date is Portuguese. This is expected, not a bug, but you should know it before the defence.

## Nit

**N1. The placeholder announces "Ticket photo" when there is no photo** (Q4 default, approved at Gate 1).
`Card(onClick)` merges its children's semantics, so TalkBack reads "Ticket photo, Pavement, Damage, Open" on every row, even though nothing is shown yet. The overview rule says `contentDescription` should be meaningful. Fix (optional): use `contentDescription = null` while the box only shows the placeholder, and put back `ticket_thumbnail_description` when Coil shows a real thumbnail. Otherwise keep the Q4 decision and mention it in the report.

**N2. The raw colour exception is not documented yet.**
The overview says "Screens read `MaterialTheme.colorScheme` ... never raw colours". `StatusBadge` uses `CityFixGreen` and `Color.White`. The plan approved this and `03-docs-plan.md` covers the palette. Fix: in the Docs phase, word the overview note as an explicit exception ("RESOLVED badge uses `CityFixGreen`/white because the M3 scheme has no success role").

**N3. Prepare to explain the deferred `val` initialisation in `StatusBadge`.**
`val containerColor: Color` and `val textColor: Color` are declared without a value and assigned in each `when` branch. This is valid because the `when` is exhaustive and the compiler checks that each `val` is assigned exactly once. It is plain Kotlin but less common in the course examples. No change is needed; just be ready to explain it. (A `Pair` with destructuring would be harder to explain, so do not switch to that.)

**N4. Label helpers in two files.**
`statusLabelRes` is in `StatusBadge.kt` and `situationLabelRes` is in `TicketCard.kt`. F-005 will import both. That works, but a reader looking for the situation label will not expect it in the card file. This is optional: if F-005 adds more label helpers (scope, attachment type), move them all into `ui/components/Labels.kt` then. No change needed now.

**N5. `state.md` Code row has no slice-3 SHA.**
The row says "slice 3 commit". Fix: replace it with `7bcdcd7`.

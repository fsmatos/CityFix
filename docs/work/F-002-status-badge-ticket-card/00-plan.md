# F-002: StatusBadge and TicketCard
Type: feature · Task file: docs/tasks/phase-1/F-002-status-badge-ticket-card.md
Labels: none

## Problem
F-003 (`ListScreen(tickets, isExpanded, onTicketClick)`) and F-005 (`ShowScreen(ticket?, ...)`) both need to show a ticket's status, and the list needs one row per ticket. `ui/components/` is still empty (only `.gitkeep`), and there are no string resources for the `TicketStatus` / `Situation` values yet. Without shared components each screen would build its own status chip and labels, and the UI rule "labels come from string resources, never from `.name`" would be easy to break.

## Outcome
- `StatusBadge(status)`: a small, non-interactive, coloured label with the localised status name. One colour and one label per `TicketStatus`; the text always carries the meaning (not colour only).
- `TicketCard(ticket, isExpanded, onClick)`: a clickable Material 3 card that always shows a placeholder thumbnail, the category name, the situation and the `StatusBadge` (brief I.1.B: the anonymous list shows "categoria, situação e thumbnail"). When `isExpanded` is true it also shows the description (up to 3 lines) and the report date.
- `isExpanded` is the app-wide "show details" toggle (top bar expand/collapse in D-005, kept with `rememberSaveable` in D-006, passed through `ListScreen` to every card), as in the course ContactsApp. It is not per-card state, so the card stays stateless.
- Label helpers `statusLabelRes(status)` and `situationLabelRes(situation)` that F-005 can reuse for the detail and the status history.
- A date helper `formatDate(epochMillis)` in `utils/` (locale-aware, works on minSdk 24) that F-005 can extend with a date-time variant.
- EN + PT strings for all of the above.
- The card reads only public fields: `categoryName`, `currentStatus`, `description`, `situation`, `createdAt`. It never reads `location`, `creatorId`, `followerIds` or `messages`, so it is safe in the anonymous list.

## Acceptance checks
- [ ] Stateless: `StatusBadge.kt` and `TicketCard.kt` have no ViewModel, no `remember`/`mutableStateOf`, and no Firebase or repository import (check by reading the files / grep).
- [ ] One badge colour and one label per `TicketStatus`: the `StatusBadge` preview shows the five badges, each with its text and a colour, readable by eye. The label `when` is exhaustive with no `else`, so a new enum value fails the build.
- [ ] Labels never come from `.name`: grep `\.name` in `ui/components/` finds nothing.
- [ ] `@Preview`s: all five statuses (badge preview and a card preview with one card per status), card collapsed and expanded, each in portrait and landscape. All render in Android Studio without errors.
- [ ] Collapsed card shows thumbnail, category, situation and status badge (brief I.1.B); expanded adds description and date only.
- [ ] PT labels match the brief: statuses Aberto, Pedido de informações, Em resolução, Resolvido, Encerrado; situations Dano, Avaria, Destruição, Roubo.
- [ ] Device language Portuguese: status and situation labels and the card texts are in Portuguese; English otherwise. `./gradlew lint` reports no `MissingTranslation`.
- [ ] Anonymous-safe: grep `TicketCard.kt` for `location`, `creatorId`, `followerIds`, `messages` finds nothing.
- [ ] Long category name or description does not overflow: text ellipsises (check with a long-text card in the preview, portrait and landscape).
- [ ] Accessibility: the card is a Material 3 `Card(onClick = ...)` (full-width, taller than 48dp); the thumbnail has a `contentDescription` from `strings.xml`; badge text uses theme typography (`sp`).
- [ ] Tapping a card calls `onClick` once (checked in interactive preview mode; real wiring is F-003/D-006).
- [ ] `./gradlew assembleDebug` and `./gradlew lint` pass after each slice.

## Approach
Chosen:
- `StatusBadge`: a `Surface(color, contentColor, shape = MaterialTheme.shapes.small)` wrapping a `Text(stringResource(statusLabelRes(status)), style = MaterialTheme.typography.labelMedium)` with small inline padding. Plain and explainable. Colours chosen by a `when (status)` over `MaterialTheme.colorScheme` roles (see Q1). Not an M3 `AssistChip`/`SuggestionChip` (those are interactive and would add a 48dp touch target and click semantics to something that is not clickable) and not M3 `Badge` (meant for counts on icons, too small for a word).
- `statusLabelRes(status): Int` (returns the `R.string` id as a plain `Int`, no `@StringRes`, as in the course examples) lives in `StatusBadge.kt`; `situationLabelRes(situation): Int` lives in `TicketCard.kt`. Both are exhaustive `when` expressions without `else`. They stay in `ui/components/` because `model/` must not depend on Android `R`.
- `TicketCard`: M3 `Card(onClick = onClick, modifier = modifier.fillMaxWidth())` with a `Row`: thumbnail on the left, a `Column` on the right with the category (`titleMedium`, 1 line, ellipsis), the situation label (`bodySmall`) and the `StatusBadge` (the collapsed content follows brief I.1.B); if `isExpanded`, the description (`bodyMedium`, max 3 lines, ellipsis, skipped if blank) and "Reported on <date>" (skipped if `createdAt == 0L`, the "not set" default). The same layout works in portrait and landscape because the card fills the width and all texts ellipsise; a split layout belongs to the screen (F-003), not the card.
- Thumbnail placeholder: a fixed-size `Box` (64dp, rounded, `surfaceVariant` background) with an `Icon(painterResource(R.drawable.ic_image_placeholder), tint = onSurfaceVariant)`. The drawable is a new vector asset (Material "image" outline, imported with Android Studio's Vector Asset tool). Phase 1 always shows the placeholder; real thumbnails (Coil `AsyncImage` over `attachments.first().thumbnail`) come with the Firebase/media tasks.
- Date: `utils/DateFormatting.kt` with `formatDate(epochMillis: Long): String` using `java.text.DateFormat.getDateInstance(DateFormat.MEDIUM)` (device locale), as recorded in the D-001 report notes. `java.time` would need API 26 or desugaring.
- Previews: private preview functions at the bottom of each file, wrapped in `CityFixTheme`, stacking two `@Preview` annotations (portrait, and landscape with `device = "spec:width=411dp,height=891dp,orientation=landscape"`). Sample tickets are built inline with distinct ids and a fixed `createdAt`; no dependency on D-003 sample data.

Alternatives considered:
- Per-card expansion state (`remember { mutableStateOf(false) }` inside the card): breaks "stateless", is lost on rotation, and does not match the agreed `ListScreen(tickets, isExpanded, ...)` signature.
- Adding `material-icons-core`/`-extended` for `Icons.Outlined.Image`: a new dependency for one icon; the current Compose BOM's `material3` no longer brings the icons artifact transitively. A vector drawable needs no dependency.
- Adding Coil now: nothing to load in phase 1 (attachments are sample data with no real files).
- Status colours as raw `Color(...)` constants per status: breaks the "screens read `MaterialTheme.colorScheme`" rule; only used for one value if Q1 is answered that way.
- Putting label helpers in `model/` as enum properties: would pull Android `R` into the pure model.

## Architecture impact
- New files: `ui/components/StatusBadge.kt`, `ui/components/TicketCard.kt`, `utils/DateFormatting.kt`, `res/drawable/ic_image_placeholder.xml`. `ui/components/.gitkeep` and `utils/.gitkeep` are removed once the folders have a real file.
- Strings added to `values/strings.xml` and `values-pt/strings.xml`.
- Possibly one extra colour val in `ui/theme/Color.kt` (Q1). No new dependency, no Gradle or manifest change, no model change.
- Public API used by F-003 / F-005: `StatusBadge`, `TicketCard`, `statusLabelRes`, `situationLabelRes`, `formatDate` (see `01-coding-plan.md`, Interfaces).

## Failure modes
- New `TicketStatus` or `Situation` value: the exhaustive `when` stops the build until a label (EN + PT) and a colour are added. Intended.
- `categoryName` blank (default `Ticket()`): shows the "No category" string instead of an empty line.
- `description` blank: the description line is skipped in expanded mode.
- `createdAt == 0L`: the date line is skipped instead of showing 1 Jan 1970.
- Very long texts or large font scale: `maxLines` + `TextOverflow.Ellipsis`; the card grows in height, never clips horizontally. The badge text is one line and short.
- Missing PT string: falls back to English; caught by lint `MissingTranslation`.
- PT strings with apostrophes or accents ("Pedido de informações", "Destruição"): UTF-8 file; escape `'` as `\'` if any.
- `%1$s` placeholder mismatch between EN and PT in `ticket_reported_on`: lint `StringFormatMatches` flags it.
- Card used where an anonymous user is: safe, the card does not read restricted fields; the repository layer still has to strip them later (rule in `.claude/rules/kotlin.md`).

## Risks and open questions
- Risk: M3 baseline container roles are not derived from the CityFix palette (noted in F-001); the proposal below avoids container roles so no purple tint appears.
- Risk: the landscape `device = "spec:..."` preview string depends on the Android Studio version; fallback is `@Preview(widthDp = 891, heightDp = 411)`.
- Risk: the vector asset path must be imported with Android Studio (Vector Asset > Clip art "image"); if hand-written, it must be a valid `<vector>` with `android:tint` left out (tint is applied in Compose).
- Q1 Badge colours per status. Proposal (container / text): OPEN `primary` / `onPrimary` (blue); INFO_REQUESTED `tertiary` / `onTertiary` (orange, needs the citizen's attention); IN_PROGRESS `secondary` / `onSecondary` (teal); RESOLVED a new `CityFixGreen = Color(0xFF2E7D32)` in `Color.kt` with white text; CLOSED `surfaceVariant` / `onSurfaceVariant` (grey, inactive). Alternative without a new colour: RESOLVED as an outlined badge (`surface` + `primary` border and text). Default if no answer: the proposal with `CityFixGreen`.
- Q2 What the expanded card shows. The collapsed card is fixed by brief I.1.B (category, situation, thumbnail, plus the status badge). Proposal for expanded: adds only the description (max 3 lines) and "Reported on <date>". Not shown: follower count, attachment count, scope, location (detail screen territory; location and followers are restricted anyway). Default: proposal.
- Q3 Meaning of `isExpanded`. Proposal: the global top-bar toggle (D-005/D-006), same value for every card. Alternative: window-size class (wide layout). Default: global toggle. To confirm with Diogo since D-005/D-006 produce it.
- Q4 Thumbnail placeholder. Proposal: vector drawable icon on a `surfaceVariant` 64dp rounded box, same size collapsed and expanded, `contentDescription` "Ticket photo" (kept when real photos arrive). Alternative: `contentDescription = null` (decorative). Default: proposal.
- Q5 Date format. Proposal: date only, `DateFormat.MEDIUM`, device locale, in `utils/DateFormatting.kt` (F-005 adds a date-time variant for the history). Alternative: private helper inside `TicketCard.kt`. Default: proposal.
- Q6 Label wording (verified against the brief). Statuses EN: Open, Info requested, In progress, Resolved, Closed; PT (brief sections II and III): Aberto, Pedido de informações, Em resolução, Resolvido, Encerrado. Situations EN: Damage, Breakdown, Destruction, Theft; PT (brief I.2.E): Dano, Avaria, Destruição, Roubo. Only the EN wording remains open; default: as listed.

## Out of scope
Real thumbnails (Coil, Storage URLs, upload state), the list itself (F-003), the detail screen and history list (F-005), the expand toggle and its saved state (D-005, D-006), sorting/filtering, landscape split layouts, operator actions, dark theme.

## Slices
Each slice ends with `./gradlew assembleDebug` and `./gradlew lint` green and is one commit.
1. StatusBadge: status label strings (EN + PT), `statusLabelRes`, `StatusBadge` with the Q1 colours (plus `CityFixGreen` in `Color.kt` if kept), preview with all five badges in portrait and landscape. Remove `ui/components/.gitkeep`.
2. TicketCard collapsed: placeholder drawable, thumbnail/category strings, situation strings (EN + PT) and `situationLabelRes`, `TicketCard(ticket, isExpanded, onClick, modifier)` showing thumbnail + category + situation + badge (ignores `isExpanded` for now), previews: one card per status and a long-text card, portrait and landscape.
3. TicketCard expanded: `ticket_reported_on` string, `utils/DateFormatting.kt`, expanded section (description, date), expanded previews in portrait and landscape. Remove `utils/.gitkeep`.
